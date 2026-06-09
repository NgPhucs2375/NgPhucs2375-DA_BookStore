-- ============================================================================
-- V16: Create Coupon System
-- ============================================================================

-- 1. Kiểm tra và tạo bảng coupons nếu chưa tồn tại
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[coupons]') AND type in (N'U'))
BEGIN
CREATE TABLE coupons (
                         id BIGINT IDENTITY(1,1) PRIMARY KEY,
                         code VARCHAR(50) NOT NULL,
                         description VARCHAR(255),
                         discount_type VARCHAR(20) NOT NULL,
                         discount_value INT NOT NULL,
                         min_order_value INT,
                         max_discount_amount FLOAT,
                         usage_limit INT,
                         usage_count INT NOT NULL DEFAULT 0,
                         start_date TIMESTAMP,
                         end_date TIMESTAMP,
                         is_active BIT NOT NULL DEFAULT 1,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP,
                         CONSTRAINT UQ_coupon_code UNIQUE (code)
);
END;
GO -- KẾT THÚC BLOCK 1

-- 2. Kiểm tra và tạo bảng coupon_usages nếu chưa tồn tại
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[coupon_usages]') AND type in (N'U'))
BEGIN
CREATE TABLE coupon_usages (
                               id BIGINT IDENTITY(1,1) PRIMARY KEY,
                               coupon_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               order_id BIGINT NOT NULL,
                               used_at TIMESTAMP NOT NULL,
                               CONSTRAINT FK_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
                               CONSTRAINT FK_coupon_usages_user FOREIGN KEY (user_id) REFERENCES users(id),
                               CONSTRAINT FK_coupon_usages_order FOREIGN KEY (order_id) REFERENCES orders_master(id)
);
END;
GO -- KẾT THÚC BLOCK 2

-- 3. Kiểm tra và thêm cột vào bảng orders_master nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[orders_master]') AND name = 'coupon_code')
BEGIN
ALTER TABLE orders_master ADD coupon_code VARCHAR(50) NULL;
END;
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[orders_master]') AND name = 'discount_amount')
BEGIN
ALTER TABLE orders_master ADD discount_amount FLOAT NOT NULL DEFAULT 0;
END;
GO