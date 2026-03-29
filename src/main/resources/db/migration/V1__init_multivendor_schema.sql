-- Multi-vendor schema initialization for SQL Server

-- users: add role and seller profile fields
IF COL_LENGTH('users', 'role') IS NULL
BEGIN
    ALTER TABLE users ADD role VARCHAR(20) NOT NULL CONSTRAINT DF_users_role DEFAULT 'BUYER';
END;

IF COL_LENGTH('users', 'shop_name') IS NULL
BEGIN
    ALTER TABLE users ADD shop_name NVARCHAR(255) NULL;
END;

IF COL_LENGTH('users', 'shop_address') IS NULL
BEGIN
    ALTER TABLE users ADD shop_address NVARCHAR(500) NULL;
END;

-- books: add seller and approval status
IF COL_LENGTH('books', 'seller_id') IS NULL
BEGIN
    ALTER TABLE books ADD seller_id BIGINT NULL;
END;

IF COL_LENGTH('books', 'approval_status') IS NULL
BEGIN
    ALTER TABLE books ADD approval_status VARCHAR(20) NOT NULL CONSTRAINT DF_books_approval_status DEFAULT 'PENDING';
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_books_seller_id_users'
)
BEGIN
    ALTER TABLE books
    ADD CONSTRAINT FK_books_seller_id_users
    FOREIGN KEY (seller_id) REFERENCES users(id);
END;

-- carts
IF OBJECT_ID('carts', 'U') IS NULL
BEGIN
    CREATE TABLE carts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        buyer_id BIGINT NOT NULL,
        CONSTRAINT UK_carts_buyer UNIQUE (buyer_id),
        CONSTRAINT FK_carts_buyer_users FOREIGN KEY (buyer_id) REFERENCES users(id)
    );
END;

-- cart_items
IF OBJECT_ID('cart_items', 'U') IS NULL
BEGIN
    CREATE TABLE cart_items (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        cart_id BIGINT NOT NULL,
        book_id BIGINT NOT NULL,
        quantity INT NOT NULL,
        CONSTRAINT FK_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
        CONSTRAINT FK_cart_items_book FOREIGN KEY (book_id) REFERENCES books(id)
    );
END;

-- orders master
IF OBJECT_ID('orders_master', 'U') IS NULL
BEGIN
    CREATE TABLE orders_master (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        buyer_id BIGINT NOT NULL,
        total_amount FLOAT NOT NULL,
        shipping_address NVARCHAR(500) NOT NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT FK_orders_master_buyer FOREIGN KEY (buyer_id) REFERENCES users(id)
    );
END;

-- sub_orders
IF OBJECT_ID('sub_orders', 'U') IS NULL
BEGIN
    CREATE TABLE sub_orders (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        order_id BIGINT NOT NULL,
        seller_id BIGINT NOT NULL,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_sub_orders_status DEFAULT 'PENDING_PAYMENT',
        sub_total FLOAT NOT NULL,
        CONSTRAINT FK_sub_orders_order FOREIGN KEY (order_id) REFERENCES orders_master(id),
        CONSTRAINT FK_sub_orders_seller FOREIGN KEY (seller_id) REFERENCES users(id)
    );
END;

-- order_items
IF OBJECT_ID('order_items', 'U') IS NULL
BEGIN
    CREATE TABLE order_items (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        sub_order_id BIGINT NOT NULL,
        book_id BIGINT NOT NULL,
        unit_price FLOAT NOT NULL,
        quantity INT NOT NULL,
        CONSTRAINT FK_order_items_sub_order FOREIGN KEY (sub_order_id) REFERENCES sub_orders(id),
        CONSTRAINT FK_order_items_book FOREIGN KEY (book_id) REFERENCES books(id)
    );
END;
