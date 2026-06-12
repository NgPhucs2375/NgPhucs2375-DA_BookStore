SET NOCOUNT ON;

IF (SELECT COUNT(*) FROM books) >= 10
BEGIN
    DECLARE @BuyerId BIGINT;
    DECLARE @SellerId BIGINT;

    -- TÌM HOẶC TẠO USER
SELECT TOP 1 @BuyerId = id FROM users WHERE role = 'BUYER';
IF @BuyerId IS NULL
BEGIN
INSERT INTO users (username, password_hash, role, is_active, created_at)
VALUES ('hacker_mu_hong_buyer', 'hash', 'BUYER', 1, GETDATE());
SET @BuyerId = SCOPE_IDENTITY();
END

SELECT TOP 1 @SellerId = id FROM users WHERE role = 'SELLER';
IF @SellerId IS NULL
BEGIN
INSERT INTO users (username, password_hash, role, is_active, created_at)
VALUES ('tech_united_seller', 'hash', 'SELLER', 1, GETDATE());
SET @SellerId = SCOPE_IDENTITY();
END

    -- CÁCH LẤY ID CỔ ĐIỂN CHỐNG LỖI FLYWAY (100% CÓ DATA)
    DECLARE @B1 BIGINT, @B2 BIGINT, @B3 BIGINT, @B4 BIGINT, @B5 BIGINT;
    DECLARE @B6 BIGINT, @B7 BIGINT, @B8 BIGINT, @B9 BIGINT, @B10 BIGINT;

SELECT TOP 1 @B1 = id FROM books ORDER BY id ASC;
SELECT TOP 1 @B2 = id FROM books WHERE id > @B1 ORDER BY id ASC;
SELECT TOP 1 @B3 = id FROM books WHERE id > @B2 ORDER BY id ASC;
SELECT TOP 1 @B4 = id FROM books WHERE id > @B3 ORDER BY id ASC;
SELECT TOP 1 @B5 = id FROM books WHERE id > @B4 ORDER BY id ASC;
SELECT TOP 1 @B6 = id FROM books WHERE id > @B5 ORDER BY id ASC;
SELECT TOP 1 @B7 = id FROM books WHERE id > @B6 ORDER BY id ASC;
SELECT TOP 1 @B8 = id FROM books WHERE id > @B7 ORDER BY id ASC;
SELECT TOP 1 @B9 = id FROM books WHERE id > @B8 ORDER BY id ASC;
SELECT TOP 1 @B10 = id FROM books WHERE id > @B9 ORDER BY id ASC;

DECLARE @OrderId BIGINT, @SubOrderId BIGINT;
    DECLARE @Counter INT;

    -- =========================================================
    -- PATTERN 1 (@B1 + @B2 + @B3)
    -- =========================================================
    SET @Counter = 0;
    WHILE @Counter < 20
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 450000, 15000, N'HUIT', GETDATE());
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
    -- PATTERN 2 (@B4 + @B5 + @B6)
    -- =========================================================
    SET @Counter = 0;
    WHILE @Counter < 15
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 500000, 20000, N'HUIT', GETDATE());
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
    -- PATTERN 3 (Data nhiễu)
    -- =========================================================
    SET @Counter = 0;
    WHILE @Counter < 20
BEGIN
INSERT INTO orders_master (buyer_id, total_amount, shipping_fee, shipping_address, created_at)
VALUES (@BuyerId, 350000, 15000, N'HUIT', GETDATE());
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

    PRINT 'DONE SCRIPT!';
END