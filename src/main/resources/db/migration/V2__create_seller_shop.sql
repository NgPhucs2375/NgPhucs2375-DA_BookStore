IF OBJECT_ID('seller_shops', 'U') IS NULL
BEGIN
CREATE TABLE seller_shops (
                      id BIGINT IDENTITY(1,1) PRIMARY KEY,
                      -- seller_id : FK sang user(id) và unique để đảm bảo 1 seller - 1 shop
                      seller_id BIGINT NOT NULL UNIQUE,
                      -- slug: Unique để dùng cho URL_Friendly
                      slug VARCHAR(255) NOT NULL UNIQUE,
                      shop_name VARCHAR(255) NOT NULL,
                      description TEXT ,
                      logo_url VARCHAR(500) NULL,
                      banner_url VARCHAR(500) NULL,
                      contact_email VARCHAR(150),
                      contact_phone VARCHAR(20),
                      address VARCHAR(500),
                      city VARCHAR(100),
                      province VARCHAR(100) NULL,
                      -- approval_status: Trạng thái duyệt shop, mặc định là 'PENDING'
                      -- Các giá trị có thể là: PENDING, APPROVED, REJECTED
                      approval_status VARCHAR(50) DEFAULT 'PENDING',
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                      -- Khoa ngoaosi
                      CONSTRAINT FK_SellerShops_Users FOREIGN KEY (seller_id) REFERENCES users(id)
                      ON DELETE CASCADE

);
END;