-- Seed initial vouchers for Nhã Nam Official
DECLARE @seller_id BIGINT = (SELECT id FROM users WHERE username = 'nhanam_official');

IF @seller_id IS NOT NULL
BEGIN
    -- Voucher 1: Giảm 10k cho đơn từ 100k
    IF NOT EXISTS (SELECT 1 FROM vouchers WHERE voucher_code = 'NHANAM10')
    BEGIN
        INSERT INTO vouchers (voucher_code, name, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, usage_limit, used_count, remaining_quantity, seller_id, status)
        VALUES ('NHANAM10', N'Ưu đãi Nhã Nam 10k', 'FIXED_AMOUNT', 10000, 100000, 10000, 
                '2024-01-01', '2026-12-31', 100, 0, 100, @seller_id, 'ACTIVE');
    END

    -- Voucher 2: Giảm 15% cho đơn từ 200k
    IF NOT EXISTS (SELECT 1 FROM vouchers WHERE voucher_code = 'NHANAM15')
    BEGIN
        INSERT INTO vouchers (voucher_code, name, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, usage_limit, used_count, remaining_quantity, seller_id, status)
        VALUES ('NHANAM15', N'Ưu đãi Nhã Nam 15%', 'PERCENTAGE', 15, 200000, 50000, 
                '2024-01-01', '2026-12-31', 50, 0, 50, @seller_id, 'ACTIVE');
    END
END
