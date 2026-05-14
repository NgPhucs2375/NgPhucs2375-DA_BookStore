-- V17: Add payment_status, refund_amount, refund_reason columns to sub_orders
-- Mục đích: Hỗ trợ hoàn tiền khi hủy đơn đã thanh toán

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'payment_status')
BEGIN
    ALTER TABLE sub_orders ADD payment_status NVARCHAR(20) NULL CONSTRAINT DF_sub_orders_payment_status DEFAULT 'UNPAID';
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'refund_amount')
BEGIN
    ALTER TABLE sub_orders ADD refund_amount FLOAT NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'refund_reason')
BEGIN
    ALTER TABLE sub_orders ADD refund_reason NVARCHAR(500) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'refunded_at')
BEGIN
    ALTER TABLE sub_orders ADD refunded_at DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'confirmed_at')
BEGIN
    ALTER TABLE sub_orders ADD confirmed_at DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'shipped_at')
BEGIN
    ALTER TABLE sub_orders ADD shipped_at DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'completed_at')
BEGIN
    ALTER TABLE sub_orders ADD completed_at DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'cancelled_at')
BEGIN
    ALTER TABLE sub_orders ADD cancelled_at DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('sub_orders') AND name = 'cancelled_by')
BEGIN
    ALTER TABLE sub_orders ADD cancelled_by NVARCHAR(20) NULL; -- BUYER, SELLER, SYSTEM, ADMIN
END;
