package com.example.bookstore.distributed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * DistributedLockService - Coordinate queue worker across multiple instances
 *
 * ==============================================================================
 * PURPOSE
 * ==============================================================================
 *
 * In a 3-instance deployment (bookom-app-1, app-2, app-3), we need only ONE
 * instance to run the notification queue worker. This service provides:
 *
 *   1. Lock acquisition at startup
 *   2. Lock refresh (heartbeat) to prevent timeout
 *   3. Lock release on graceful shutdown
 *   4. Failover detection if lock holder crashes
 *
 * ==============================================================================
 * ALGORITHM
 * ==============================================================================
 *
 * Lock acquisition (at startup):
 *   - Each instance calls acquireLock()
 *   - Database executes sp_acquire_queue_lock stored procedure
 *   - First instance wins (or instance whose lock expired)
 *   - Returns true/false based on success
 *
 * Lock refresh (every 15 seconds):
 *   - Lock holder calls refreshLock()
 *   - If successful (returned true): continue processing
 *   - If failed (returned false): another instance stole lock, stop processing
 *
 * Lock release (on graceful shutdown):
 *   - Calls releaseLock() before exiting
 *   - Sets lock_holder_id = 'UNOWNED'
 *   - Next instance can immediately acquire
 *
 * ==============================================================================
 * DEPLOYMENT SCENARIOS
 * ==============================================================================
 *
 * Scenario 1: Normal startup (all 3 instances start together)
 *   - Instance 1 starts first, acquires lock, begins processing ✓
 *   - Instance 2 starts, tries to acquire, fails, sleeps ✓
 *   - Instance 3 starts, tries to acquire, fails, sleeps ✓
 *   - Result: Only instance 1 processes notifications
 *
 * Scenario 2: Lock holder crashes
 *   - Instance 1 crashes (no graceful shutdown)
 *   - Lock expires after TTL (30 seconds)
 *   - Instance 2 detects expired lock, acquires it, starts processing ✓
 *   - Result: Automatic failover in <30s
 *
 * Scenario 3: Graceful shutdown
 *   - Instance 1 receives SIGTERM, begins shutdown
 *   - Calls releaseLock()
 *   - Instance 2 (waiting) immediately detects available lock
 *   - Instance 2 acquires lock in next refresh cycle ✓
 *   - Result: Zero downtime switchover
 *
 * ==============================================================================
 * CONFIGURATION
 * ==============================================================================
 *
 * Lock TTL (time-to-live):
 *   - Set in @Value("${distributed.lock.ttl-seconds:30}")
 *   - Instance must refresh within this time or loses lock
 *   - Default: 30 seconds (allows crash detection in <30s)
 *
 * Refresh interval:
 *   - Lock holder refreshes every 15 seconds (= TTL / 2)
 *   - Heartbeat interval: 15 seconds (see HeartbeatService)
 *   - Sync: Lock refresh happens alongside heartbeat
 *
 * ==============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final DataSource dataSource;

    @Value("${distributed.lock.enabled:true}")
    private boolean lockEnabled;

    @Value("${distributed.lock.name:NOTIFICATION_QUEUE_WORKER}")
    private String lockName;

    @Value("${distributed.lock.ttl-seconds:30}")
    private int lockTtlSeconds;

    @Value("${spring.application.name:bookstore-app}")
    private String applicationName;

    /**
     * Get unique instance identifier
     * Can be:
     *   - Hostname (DOCKER_HOSTNAME env variable set by Docker)
     *   - Application name + timestamp
     *   - UUID
     */
    public String getInstanceId() {
        String hostname = System.getenv("DOCKER_HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }

        String containerName = System.getenv("HOSTNAME");
        if (containerName != null && !containerName.isBlank()) {
            return containerName;
        }

        // Fallback: application name + PID
        long pid = ProcessHandle.current().pid();
        return applicationName + "-" + pid;
    }

    /**
     * Try to acquire the distributed lock
     *
     * @return true if lock acquired, false if already held by another instance
     */
    @Transactional
    public boolean acquireLock() {
        if (!lockEnabled) {
            log.debug("Distributed lock disabled, allowing any instance to process");
            return true;
        }

        String instanceId = getInstanceId();
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_acquire_queue_lock(?, ?, ?, ?)}")) {

            stmt.setString(1, lockName);
            stmt.setString(2, instanceId);
            stmt.setInt(3, lockTtlSeconds);
            stmt.registerOutParameter(4, Types.BIT);

            stmt.execute();
            boolean acquired = stmt.getBoolean(4);

            if (acquired) {
                log.info("✓ Distributed lock acquired by instance: {}", instanceId);
            } else {
                log.info("✗ Distributed lock NOT acquired (held by another instance)");
            }

            return acquired;

        } catch (SQLException e) {
            log.error("Error acquiring distributed lock: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Refresh the lock (keep it alive)
     *
     * Called periodically by HeartbeatService every 15 seconds
     *
     * @return true if lock still held by this instance, false if lost
     */
    @Transactional
    public boolean refreshLock() {
        if (!lockEnabled) {
            return true;
        }

        String instanceId = getInstanceId();
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_refresh_queue_lock(?, ?, ?, ?)}")) {

            stmt.setString(1, lockName);
            stmt.setString(2, instanceId);
            stmt.setInt(3, lockTtlSeconds);
            stmt.registerOutParameter(4, Types.BIT);

            stmt.execute();
            boolean refreshed = stmt.getBoolean(4);

            if (!refreshed) {
                log.warn("⚠ Lost distributed lock - another instance acquired it");
            }

            return refreshed;

        } catch (SQLException e) {
            log.error("Error refreshing distributed lock: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Release the lock (graceful shutdown)
     *
     * Called from GracefulShutdownComponent during application shutdown
     */
    @Transactional
    public void releaseLock() {
        if (!lockEnabled) {
            return;
        }

        String instanceId = getInstanceId();
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_release_queue_lock(?, ?)}")) {

            stmt.setString(1, lockName);
            stmt.setString(2, instanceId);

            stmt.execute();
            log.info("✓ Distributed lock released by instance: {}", instanceId);

        } catch (SQLException e) {
            log.error("Error releasing distributed lock: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current lock holder (for monitoring/debugging)
     */
    public Optional<String> getCurrentLockHolder() {
        if (!lockEnabled) {
            return Optional.empty();
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT lock_holder_id, lock_expires_at FROM distributed_lock WHERE lock_name = ?")) {

            stmt.setString(1, lockName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String holder = rs.getString(1);
                    if (!"UNOWNED".equals(holder)) {
                        return Optional.of(holder);
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error querying current lock holder: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }
}
