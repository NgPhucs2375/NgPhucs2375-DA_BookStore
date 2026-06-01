package com.example.bookstore.distributed;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "distributed_lock")
public class DistributedLock {

    @Id
    @Column(name = "lock_name", length = 100)
    private String lockName;

    @Column(name = "instance_id", length = 255, nullable = false)
    private String instanceId = "";

    @Column(name = "lock_holder_id", length = 255, nullable = false)
    private String lockHolderId = "UNOWNED";

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt = LocalDateTime.now();

    @Column(name = "lock_expires_at", nullable = false)
    private LocalDateTime lockExpiresAt = LocalDateTime.now();

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt = LocalDateTime.now();

    // Constructors
    public DistributedLock() {}

    public DistributedLock(String lockName) {
        this.lockName = lockName;
        this.instanceId = "";
    }

    // Getters and Setters
    public String getLockName() { return lockName; }
    public void setLockName(String lockName) { this.lockName = lockName; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getLockHolderId() { return lockHolderId; }
    public void setLockHolderId(String lockHolderId) { this.lockHolderId = lockHolderId; }
    public LocalDateTime getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(LocalDateTime acquiredAt) { this.acquiredAt = acquiredAt; }
    public LocalDateTime getLockExpiresAt() { return lockExpiresAt; }
    public void setLockExpiresAt(LocalDateTime lockExpiresAt) { this.lockExpiresAt = lockExpiresAt; }
    public LocalDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }
}
