-- ============================================================================
-- V25: Tạo bảng user_activity_log
-- ============================================================================
-- Cần cho ML feature: browsing_frequency_per_week
-- Ghi lại mỗi lần user truy cập trang web (page view, search, product view)
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user_activity_log]') AND type = 'U')
BEGIN
    CREATE TABLE user_activity_log (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        activity_type VARCHAR(50) NOT NULL,  -- PAGE_VIEW, PRODUCT_VIEW, SEARCH, LOGIN
        page_url VARCHAR(500) NULL,
        product_id BIGINT NULL,
        ip_address VARCHAR(50) NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT FK_user_activity_log_user FOREIGN KEY (user_id) REFERENCES users(id)
    );

    CREATE INDEX IX_user_activity_log_user_created ON user_activity_log(user_id, created_at);
    CREATE INDEX IX_user_activity_log_created ON user_activity_log(created_at);

    PRINT 'Đã tạo bảng user_activity_log thành công';
END
ELSE
BEGIN
    PRINT 'Bảng user_activity_log đã tồn tại, bỏ qua';
END
GO

-- ============================================================================
-- END V25
-- ============================================================================
