-- ============================================================================
-- V38: Seed Mock Orders to Generate Association Rules for FP-Growth
-- ============================================================================
-- Script này tự động sinh ra 260 đơn hàng để tạo "lịch sử mua sắm",
-- giúp hệ thống Machine Learning (FP-Growth) có dữ liệu để tính toán
-- ra các bộ luật (Association Rules) như Support, Confidence, Lift.
-- ============================================================================

DECLARE @BuyerId BIGINT;
DECLARE @SellerId BIGINT;

-- 1. Tìm 1 Buyer (Người mua) và 1 Seller (Người bán) bất kỳ có trong hệ thống
SELECT TOP 1 @BuyerId = id FROM users WHERE role = 'BUYER' OR role = 'USER';
IF @BuyerId IS NULL SELECT TOP 1 @BuyerId = id FROM users;

SELECT TOP 1 @SellerId = id FROM users WHERE role = 'SELLER';
IF @SellerId IS NULL SELECT TOP 1 @SellerId = id FROM users;

IF @BuyerId IS NOT NULL AND @SellerId IS NOT NULL
BEGIN
    -- 2. Lấy ngẫu nhiên 7 cuốn sách đầu tiên trong Database
    DECLARE @B1 BIGINT, @B2 BIGINT, @B3 BIGINT, @B4 BIGINT, @B5 BIGINT, @B6 BIGINT, @B7 BIGINT;

SELECT TOP 1 @B1 = id FROM books ORDER BY id;
SELECT TOP 1 @B2 = id FROM books WHERE id NOT IN (@B1) ORDER BY id;
SELECT TOP 1 @B3 = id FROM books WHERE id NOT IN (@B1, @B2) ORDER BY id;
SELECT TOP 1 @B4 = id FROM books WHERE id NOT IN (@B1, @B2, @B3) ORDER BY id;
SELECT TOP 1 @B5 = id FROM books WHERE id NOT IN (@B1, @B2, @B3, @B4) ORDER BY id;
SELECT TOP 1 @B6 = id FROM books WHERE id NOT IN (@B1, @B2, @B3, @B4, @B5) ORDER BY id;
SELECT TOP 1 @B7 = id FROM books WHERE id NOT IN (@B1, @B2, @B3, @B4, @B5, @B6) ORDER BY id;

-- Kiểm tra xem trong DB có đủ ít nhất 7 quyển sách hay không
IF @B7 IS NOT NULL
BEGIN
        DECLARE @i INT = 1;
        DECLARE @OrderId BIGINT;
        DECLARE @SubOrderId BIGINT;

        -- PAIR 1: Sách 1 & Sách 2 (Mua chung 100 lần) -> Luật cực kỳ mạnh
        WHILE @i <= 100
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_address, created_at)
VALUES (@BuyerId, 300000, 'Mock Address 1', GETDATE());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total)
VALUES (@OrderId, @SellerId, 'DELIVERED', 300000);
SET @SubOrderId = SCOPE_IDENTITY();

INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B1, 150000, 1);
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B2, 150000, 1);

SET @i = @i + 1;
END

        -- PAIR 2: Sách 3 & Sách 4 & Sách 5 (Mua chung 80 lần)
        SET @i = 1;
        WHILE @i <= 80
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_address, created_at)
VALUES (@BuyerId, 450000, 'Mock Address 2', GETDATE());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total)
VALUES (@OrderId, @SellerId, 'DELIVERED', 450000);
SET @SubOrderId = SCOPE_IDENTITY();

INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B3, 150000, 1);
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B4, 150000, 1);
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B5, 150000, 1);

SET @i = @i + 1;
END

        -- PAIR 3: Sách 6 & Sách 7 (Mua chung 50 lần)
        SET @i = 1;
        WHILE @i <= 50
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_address, created_at)
VALUES (@BuyerId, 250000, 'Mock Address 3', GETDATE());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total)
VALUES (@OrderId, @SellerId, 'DELIVERED', 250000);
SET @SubOrderId = SCOPE_IDENTITY();

INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B6, 125000, 1);
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B7, 125000, 1);

SET @i = @i + 1;
END

        -- NOISE: Mua đơn lẻ Sách 1 thêm 30 lần (Để làm nhiễu dữ liệu cho thực tế)
        SET @i = 1;
        WHILE @i <= 30
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_address, created_at)
VALUES (@BuyerId, 100000, 'Mock Address 4', GETDATE());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total)
VALUES (@OrderId, @SellerId, 'DELIVERED', 100000);
SET @SubOrderId = SCOPE_IDENTITY();

INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity) VALUES (@SubOrderId, @B1, 100000, 1);

SET @i = @i + 1;
END

        PRINT 'V38: Đã giả lập thành công 260 đơn hàng mua chung cho FP-Growth mining.';
END
ELSE
BEGIN
        PRINT 'V38: Không đủ số lượng sách (ít nhất 7) trong DB để giả lập luật.';
END
END
ELSE
BEGIN
    PRINT 'V38: Không tìm thấy Buyer hoặc Seller nào trong DB.';
END