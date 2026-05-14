-- V18: Add stock tracking columns to order_items
-- Mục đích: Theo dõi việc trừ/hoàn stock khi xác nhận/hủy đơn

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('order_items') AND name = 'stock_deducted')
BEGIN
    ALTER TABLE order_items ADD stock_deducted BIT NOT NULL CONSTRAINT DF_order_items_stock_deducted DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('order_items') AND name = 'stock_deducted_at')
BEGIN
    ALTER TABLE order_items ADD stock_deducted_at DATETIME2 NULL;
END;
