-- Create vouchers table
IF OBJECT_ID('vouchers', 'U') IS NULL
BEGIN
    CREATE TABLE vouchers (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        voucher_code NVARCHAR(50) NOT NULL UNIQUE,
        name NVARCHAR(100) NOT NULL,
        discount_type NVARCHAR(20) NOT NULL,
        discount_value FLOAT NOT NULL,
        min_order_amount FLOAT NOT NULL,
        max_discount_amount FLOAT NULL,
        start_date DATETIME2 NOT NULL,
        end_date DATETIME2 NOT NULL,
        usage_limit INT NOT NULL,
        used_count INT NOT NULL DEFAULT 0,
        remaining_quantity INT NOT NULL,
        seller_id BIGINT NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        created_at DATETIME2 DEFAULT GETDATE(),
        updated_at DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_vouchers_seller FOREIGN KEY (seller_id) REFERENCES users(id)
    );
END;

-- Create voucher_usage table
IF OBJECT_ID('voucher_usage', 'U') IS NULL
BEGIN
    CREATE TABLE voucher_usage (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        voucher_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        order_id BIGINT NULL,
        discount_amount FLOAT NOT NULL,
        used_at DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_voucher_usage_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
        CONSTRAINT FK_voucher_usage_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT FK_voucher_usage_order FOREIGN KEY (order_id) REFERENCES orders_master(id)
    );
END;
