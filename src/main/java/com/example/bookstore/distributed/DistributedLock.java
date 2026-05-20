package com.example.bookstore.distributed;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "distributed_lock")
public class DistributedLock {

    @Id
    @Column(name = "lock_name", length = 100)
    private String lockName;

    @Column(name = "lock_holder_id", length = 255, nullable = false)
    private String lockHolderId = "UNOWNED";

    @Column(name = "lock_expires_at", nullable = false)
    private LocalDateTime lockExpiresAt = LocalDateTime.now();

    // Constructors
    public DistributedLock() {}

    public DistributedLock(String lockName) {
        this.lockName = lockName;
    }

    // Getters and Setters
    public String getLockName() { return lockName; }
    public void setLockName(String lockName) { this.lockName = lockName; }
    public String getLockHolderId() { return lockHolderId; }
    public void setLockHolderId(String lockHolderId) { this.lockHolderId = lockHolderId; }
    public LocalDateTime getLockExpiresAt() { return lockExpiresAt; }
    public void setLockExpiresAt(LocalDateTime lockExpiresAt) { this.lockExpiresAt = lockExpiresAt; }
}