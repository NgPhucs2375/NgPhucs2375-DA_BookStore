-- Insert initial data for categories
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Kinh tế')
    INSERT INTO categories (name, description) VALUES (N'Kinh tế', N'Sách về kinh tế, đầu tư, quản trị kinh doanh');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Văn học')
    INSERT INTO categories (name, description) VALUES (N'Văn học', N'Sách văn học trong nước và nước ngoài');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Kỹ năng sống')
    INSERT INTO categories (name, description) VALUES (N'Kỹ năng sống', N'Sách phát triển bản thân, kỹ năng mềm');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Thiếu nhi')
    INSERT INTO categories (name, description) VALUES (N'Thiếu nhi', N'Sách dành cho trẻ em');
IF NOT EXISTS (SELECT 1 FROM categories WHERE name = N'Ngoại ngữ')
    INSERT INTO categories (name, description) VALUES (N'Ngoại ngữ', N'Sách học tiếng Anh, Nhật, Trung...');

-- Insert initial data for users (Password: password123)
-- Role: BUYER, SELLER, ADMIN
IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')
    INSERT INTO users (username, password_hash, role, first_name, last_name, email, phone) 
    VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DM9m99P17Ieu', 'ADMIN', N'Hệ thống', N'Admin', 'admin@bookom.com', '0123456789');

IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'nhanam_official')
    INSERT INTO users (username, password_hash, role, shop_name, shop_address, first_name, last_name, email, phone) 
    VALUES ('nhanam_official', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DM9m99P17Ieu', 'SELLER', N'Nhã Nam Official', N'Hà Nội, Việt Nam', N'Nhã Nam', N'Official', 'nhanam@bookom.com', '0987654321');

IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'test_buyer')
    INSERT INTO users (username, password_hash, role, first_name, last_name, email, phone) 
    VALUES ('test_buyer', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DM9m99P17Ieu', 'BUYER', N'Người dùng', N'Thử nghiệm', 'buyer@test.com', '0909090909');

-- Insert initial data for seller_shops (Linked to nhanam_official)
DECLARE @nhanam_id BIGINT = (SELECT id FROM users WHERE username = 'nhanam_official');
IF @nhanam_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seller_shops WHERE seller_id = @nhanam_id)
BEGIN
    INSERT INTO seller_shops (seller_id, slug, shop_name, description, logo_url, banner_url, contact_email, contact_phone, address, city, province, approval_status)
    VALUES (@nhanam_id, 'nhanam-official', N'Nhã Nam Official', N'Nhã Nam - Bởi vì sách là thế giới', 
            'https://nhanam.com.vn/wp-content/uploads/2023/10/logo-nha-nam.png', 
            'https://nhanam.com.vn/wp-content/uploads/2023/10/banner.jpg', 
            'nhanam@bookom.com', '0987654321', N'Hà Nội, Việt Nam', N'Hà Nội', N'Hà Nội', 'APPROVED');
END

-- Insert initial data for books (linked to nhanam_official and categories)
DECLARE @seller_id BIGINT = (SELECT id FROM users WHERE username = 'nhanam_official');
DECLARE @cat_kinhte BIGINT = (SELECT id FROM categories WHERE name = N'Kinh tế');
DECLARE @cat_vanhoc BIGINT = (SELECT id FROM categories WHERE name = N'Văn học');
DECLARE @cat_kynang BIGINT = (SELECT id FROM categories WHERE name = N'Kỹ năng sống');

IF @seller_id IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM books WHERE title = N'Nhà Giả Kim' AND seller_id = @seller_id)
        INSERT INTO books (title, author, description, price, stock_quantity, image_url, publisher, publish_year, category_id, seller_id, approval_status) VALUES 
        (N'Nhà Giả Kim', N'Paulo Coelho', N'Một trong những cuốn sách bán chạy nhất thế giới', 79000, 100, 'https://salt.tikicdn.com/cache/w1200/ts/product/45/3d/e4/da13248288593d14878a1599540b171c.jpg', N'NXB Hội Nhà Văn', '2020', @cat_vanhoc, @seller_id, 'APPROVED');

    IF NOT EXISTS (SELECT 1 FROM books WHERE title = N'Đắc Nhân Tâm' AND seller_id = @seller_id)
        INSERT INTO books (title, author, description, price, stock_quantity, image_url, publisher, publish_year, category_id, seller_id, approval_status) VALUES 
        (N'Đắc Nhân Tâm', N'Dale Carnegie', N'Cuốn sách về kỹ năng giao tiếp kinh điển', 86000, 50, 'https://salt.tikicdn.com/cache/w1200/ts/product/f4/64/00/5248c82f09923a1a98075f850e7b8a53.jpg', N'NXB Tổng hợp TP.HCM', '2021', @cat_kynang, @seller_id, 'APPROVED');

    IF NOT EXISTS (SELECT 1 FROM books WHERE title = N'Chiến Tranh Tiền Tệ' AND seller_id = @seller_id)
        INSERT INTO books (title, author, description, price, stock_quantity, image_url, publisher, publish_year, category_id, seller_id, approval_status) VALUES 
        (N'Chiến Tranh Tiền Tệ', N'Song Hongbing', N'Góc nhìn về lịch sử tài chính thế giới', 155000, 30, 'https://salt.tikicdn.com/cache/w1200/ts/product/13/2b/2d/7a164d12c96c4a8618d360f5835978f8.jpg', N'NXB Trẻ', '2019', @cat_kinhte, @seller_id, 'APPROVED');

    IF NOT EXISTS (SELECT 1 FROM books WHERE title = N'Tôi Thấy Hoa Vàng Trên Cỏ Xanh' AND seller_id = @seller_id)
        INSERT INTO books (title, author, description, price, stock_quantity, image_url, publisher, publish_year, category_id, seller_id, approval_status) VALUES 
        (N'Tôi Thấy Hoa Vàng Trên Cỏ Xanh', N'Nguyễn Nhật Ánh', N'Tác phẩm nổi tiếng của nhà văn Nguyễn Nhật Ánh', 125000, 200, 'https://salt.tikicdn.com/cache/w1200/ts/product/70/4e/74/4e9087a32c253676834033a303867d51.jpg', N'NXB Trẻ', '2010', @cat_vanhoc, @seller_id, 'APPROVED');
END
