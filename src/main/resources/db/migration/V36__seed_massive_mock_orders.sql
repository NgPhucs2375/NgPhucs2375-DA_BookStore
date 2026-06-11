-- ============================================================================
-- V36: Seed Massive Mock Orders for Recommendation Engine (Bản Potterhead)
-- Mục đích: Bơm 130 đơn hàng (Status: COMPLETED) với các tổ hợp sách đa dạng.
-- Tích hợp Pattern mua chung siêu mạnh của series HARRY POTTER (ID 58 -> 64)
-- ============================================================================

DECLARE @BuyerId BIGINT;
DECLARE @SellerId BIGINT;

-- 1. Tìm hoặc tạo user để đóng vai Buyer và Seller
SELECT TOP 1 @BuyerId = id FROM users WHERE role = 'BUYER';
IF @BuyerId IS NULL
BEGIN
INSERT INTO users (username, password_hash, role, is_active, created_at)
VALUES ('hacker_mu_hong_buyer', 'hash', 'BUYER', 1, SYSUTCDATETIME());
SET @BuyerId = SCOPE_IDENTITY();
END

SELECT TOP 1 @SellerId = id FROM users WHERE role = 'SELLER';
IF @SellerId IS NULL
BEGIN
INSERT INTO users (username, password_hash, role, is_active, created_at)
VALUES ('tech_united_seller', 'hash', 'SELLER', 1, SYSUTCDATETIME());
SET @SellerId = SCOPE_IDENTITY();
END

-- 2. Tự động lấy 10 ID sách ĐANG TỒN TẠI ngẫu nhiên để làm data nền (Tránh lỗi FK)
DECLARE @B1 BIGINT, @B2 BIGINT, @B3 BIGINT, @B4 BIGINT, @B5 BIGINT;
DECLARE @B6 BIGINT, @B7 BIGINT, @B8 BIGINT, @B9 BIGINT, @B10 BIGINT;

WITH RankedBooks AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id ASC) as rn FROM books WHERE id < 50
)
SELECT
    @B1 = MAX(CASE WHEN rn = 1 THEN id END),
    @B2 = MAX(CASE WHEN rn = 2 THEN id END),
    @B3 = MAX(CASE WHEN rn = 3 THEN id END),
    @B4 = MAX(CASE WHEN rn = 4 THEN id END),
    @B5 = MAX(CASE WHEN rn = 5 THEN id END),
    @B6 = MAX(CASE WHEN rn = 6 THEN id END),
    @B7 = MAX(CASE WHEN rn = 7 THEN id END),
    @B8 = MAX(CASE WHEN rn = 8 THEN id END),
    @B9 = MAX(CASE WHEN rn = 9 THEN id END),
    @B10 = MAX(CASE WHEN rn = 10 THEN id END)
FROM RankedBooks;

DECLARE @OrderId BIGINT, @SubOrderId BIGINT;
DECLARE @Counter INT;

-- =========================================================
-- PATTERN 1: Combo 3 sách đầu tiên (@B1 + @B2 + @B3)
-- Số lượng: 20 đơn hàng
-- =========================================================
SET @Counter = 0;
WHILE @Counter < 20
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 450000, 15000, N'HUIT - TP.HCM', SYSUTCDATETIME());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total, version)
VALUES (@OrderId, @SellerId, 'COMPLETED', 450000, 0);
SET @SubOrderId = SCOPE_IDENTITY();

INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, @B1, 150000, 1),
       (@SubOrderId, @B2, 150000, 1),
       (@SubOrderId, @B3, 150000, 1);

SET @Counter = @Counter + 1;
END

-- =========================================================
-- PATTERN 2: Combo tiếp theo (@B4 + @B5 + @B6)
-- Số lượng: 15 đơn hàng
-- =========================================================
SET @Counter = 0;
WHILE @Counter < 15
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 500000, 20000, N'HUIT - TP.HCM', SYSUTCDATETIME());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total, version)
VALUES (@OrderId, @SellerId, 'COMPLETED', 500000, 0);
SET @SubOrderId = SCOPE_IDENTITY();

INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, @B4, 180000, 1),
       (@SubOrderId, @B5, 170000, 1),
       (@SubOrderId, @B6, 150000, 1);

SET @Counter = @Counter + 1;
END

-- =========================================================
-- PATTERN 3: Dữ liệu nhiễu (Noise Data) - Mua ngẫu nhiên
-- Số lượng: 20 đơn hàng
-- =========================================================
SET @Counter = 0;
WHILE @Counter < 20
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 350000, 15000, N'HUIT - TP.HCM', SYSUTCDATETIME());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total, version)
VALUES (@OrderId, @SellerId, 'COMPLETED', 350000, 0);
SET @SubOrderId = SCOPE_IDENTITY();

    IF @Counter % 2 = 0
BEGIN
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, @B1, 120000, 1), (@SubOrderId, @B5, 110000, 1), (@SubOrderId, @B9, 120000, 1);
END
ELSE
BEGIN
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, @B2, 150000, 1), (@SubOrderId, @B10, 200000, 1);
END

    SET @Counter = @Counter + 1;
END

-- =========================================================
-- PATTERN 4: 🔥 HỘI FAN CỨNG HARRY POTTER 🔥
-- Mua combo phần 1, 2, 3 (ID 58, 59, 60)
-- Số lượng: 35 đơn hàng -> Sẽ tạo ra Confidence và Lift cao ngất ngưởng
-- =========================================================
-- Kiểm tra xem ID 58 có tồn tại không trước khi chèn để chống lỗi FK
IF EXISTS (SELECT 1 FROM books WHERE id = 58)
BEGIN
    SET @Counter = 0;
    WHILE @Counter < 35
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 600000, 0, N'Hogwarts School', SYSUTCDATETIME());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total, version)
VALUES (@OrderId, @SellerId, 'COMPLETED', 600000, 0);
SET @SubOrderId = SCOPE_IDENTITY();

        -- ID 58: Sorcerer's Stone | 59: Chamber of Secrets | 60: Prisoner of Azkaban
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, 58, 200000, 1),
       (@SubOrderId, 59, 200000, 1),
       (@SubOrderId, 60, 200000, 1);

SET @Counter = @Counter + 1;
END
    PRINT 'Đã bơm 35 đơn hàng Combo Harry Potter (Tập 1,2,3)!';
END
ELSE
BEGIN
    PRINT 'Không tìm thấy Harry Potter (ID 58) trong Database. Bỏ qua pattern này.';
END

-- =========================================================
-- PATTERN 5: 🔥 HỘI SƯU TẦM HARRY POTTER 🔥
-- Mua combo phần 4, 5, 6, 7 (ID 61, 62, 63, 64)
-- Số lượng: 20 đơn hàng
-- =========================================================
IF EXISTS (SELECT 1 FROM books WHERE id = 61)
BEGIN
    SET @Counter = 0;
    WHILE @Counter < 20
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 800000, 0, N'Hogwarts School', SYSUTCDATETIME());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total, version)
VALUES (@OrderId, @SellerId, 'COMPLETED', 800000, 0);
SET @SubOrderId = SCOPE_IDENTITY();

        -- ID 61 -> 64
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, 61, 200000, 1),
       (@SubOrderId, 62, 200000, 1),
       (@SubOrderId, 63, 200000, 1),
       (@SubOrderId, 64, 200000, 1);

SET @Counter = @Counter + 1;
END
    PRINT 'Đã bơm 20 đơn hàng Combo Harry Potter (Tập 4,5,6,7)!';
END

PRINT '✅ Hoàn tất quá trình giả lập dữ liệu mua hàng siêu cấp cho Technical United!';
GO

-- ============================================================================
-- TRUY VẤN KIỂM TRA TẬP LUẬT FP-GROWTH ĐÃ HỌC ĐƯỢC
-- Hiển thị trực quan: Tên sách A -> Tên sách B kèm theo các chỉ số toán học
-- ============================================================================

SELECT TOP 50
    r.rule_id AS [Mã Luật],
    b1.title AS [Sách Tiền Đề (Khách vừa chọn)],
    b2.title AS [Sách Hệ Quả (Gợi ý mua kèm)],
    r.support AS [Độ Hỗ Trợ (Support)],
    r.confidence AS [Độ Tin Cậy (Confidence)],
    r.lift AS [Độ Nâng (Lift)],
    r.updated_at AS [Thời gian học]
FROM association_rules r
    JOIN books b1 ON r.book_id_a = b1.id
    JOIN books b2 ON r.book_id_b = b2.id
-- Chỉ lấy những luật thực sự mạnh (Confidence > 30% và Lift > 1.0)
WHERE r.confidence >= 0.3 AND r.lift > 1.0
ORDER BY r.confidence DESC, r.lift DESC;


-- =========================================================
-- PATTERN 6: Trend mới tự chế (VD: Sách lập trình)
-- Mua Sách 100 + 200 + 300
-- =========================================================
-- Nhớ check xem sách có tồn tại không để né lỗi Foreign Key nhé
IF EXISTS (SELECT 1 FROM books WHERE id = 100)
BEGIN
    SET @Counter = 0;
    WHILE @Counter < 25  -- Bơm 25 đơn hàng cho chắc cốp
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 500000, 15000, N'HUIT - TP.HCM', SYSUTCDATETIME());
SET @OrderId = SCOPE_IDENTITY();

INSERT INTO sub_orders (order_id, seller_id, status, sub_total, version)
VALUES (@OrderId, @SellerId, 'COMPLETED', 500000, 0);
SET @SubOrderId = SCOPE_IDENTITY();

        -- ĐỔI SỐ ID SÁCH Ở ĐÂY 👇
INSERT INTO order_items (sub_order_id, book_id, unit_price, quantity)
VALUES (@SubOrderId, 100, 150000, 1),
       (@SubOrderId, 200, 200000, 1),
       (@SubOrderId, 300, 150000, 1);

SET @Counter = @Counter + 1;
END
    PRINT 'Đã bơm 25 đơn hàng cho combo 100-200-300!';
END