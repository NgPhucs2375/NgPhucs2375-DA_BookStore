-- ============================================================================
-- V22: Add instance_id to distributed_lock
-- ============================================================================
-- Purpose:
--   Align the database schema with the current DistributedLock entity, which
--   maps instance_id as a required column.
--
-- Notes:
--   - Safe for existing databases that were created before instance_id existed.
--   - Backfills existing rows from lock_holder_id so historical records remain
--     meaningful on SQL Server.
--   - Keeps the migration idempotent where practical for drifted environments.
-- ============================================================================

IF COL_LENGTH('dbo.distributed_lock', 'instance_id') IS NULL
BEGIN
    ALTER TABLE distributed_lock
        ADD instance_id NVARCHAR(255) NULL;
END
GO

UPDATE distributed_lock
SET instance_id = COALESCE(NULLIF(LTRIM(RTRIM(lock_holder_id)), ''), 'UNOWNED')
WHERE instance_id IS NULL OR LTRIM(RTRIM(instance_id)) = '';
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.distributed_lock')
      AND name = 'instance_id'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE distributed_lock
        ALTER COLUMN instance_id NVARCHAR(255) NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_distributed_lock_instance_id'
      AND object_id = OBJECT_ID(N'dbo.distributed_lock')
)
BEGIN
    CREATE INDEX IX_distributed_lock_instance_id
        ON distributed_lock(instance_id);
END
GO

-- ============================================================================
-- END V22
-- ============================================================================