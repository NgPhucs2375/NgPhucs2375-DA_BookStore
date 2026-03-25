# BOOKSTORE PROJECT SUMMARY

Cap nhat: 2026-03-25

## 1. Tong quan du an
BookStore la ung dung web thuong mai dien tu ban sach xay dung bang Spring Boot + Thymeleaf.
Du an co 3 lop chuc nang chinh:
- Public/Main pages (khach truy cap)
- Buyer dashboard
- Admin/Seller panel

Backend cung cap REST API cho sach, auth, category va bo API dashboard/panel.
Frontend su dung Thymeleaf templates + TailwindCSS (CDN) + JS thuong.

## 2. Cong nghe va phu thuoc chinh
- Java 17
- Spring Boot 4.0.3
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-thymeleaf
- spring-boot-starter-validation
- MSSQL JDBC (runtime)
- MySQL connector (runtime)
- BCrypt (org.mindrot:jbcrypt)
- Lombok

File cau hinh: src/main/resources/application.properties
Mac dinh dang tro den SQL Server localhost:1433, database BookstoreDB.

## 3. Cau truc backend
### 3.1 Models
- Book: id, title, author, description, price, stockQuantity, imageUrl, publisher, publishYear, category
- Category: id, name, description, books
- User: id, username, passwordHash

### 3.2 Repository
- BookRepository
- CategoryRepository
- UserRepository

### 3.3 Service
- BookService
- CategoryService
- AuthService (dang ky/dang nhap + BCrypt hash/check)

### 3.4 Controller chinh
- MainPageController: route cho cac trang main + buyer
- PanelPageController: route cho admin/seller pages
- LegacyRouteController: redirect URL cu -> URL moi
- BookController: REST /api/books
- CategoryController: REST /api/categories
- AuthController: REST /api/auth
- PanelController: REST /api/panel (dashboard, users, shops, books, seller analytics/orders)

## 4. API tom tat
### 4.1 Book API
Base: /api/books
- GET /api/books?page=&size=
- GET /api/books/{id}
- POST /api/books
- PUT /api/books/{id}
- DELETE /api/books/{id}

### 4.2 Category API
Base: /api/categories
- GET /api/categories
- POST /api/categories

### 4.3 Auth API
Base: /api/auth
- POST /api/auth/register
- POST /api/auth/login

### 4.4 Panel API
Base: /api/panel
- GET /summary
- GET /books
- GET /users
- GET /shops
- GET /seller/orders
- GET /seller/analytics

## 5. Frontend va migration trang
### 5.1 Route moi (clean route)
Main:
- /
- /main/discovery
- /main/auth
- /main/cart
- /main/contact
- /main/checkout
- /main/order-details
- /main/order-success
- /main/search
- /main/flash-sale
- /buyer/dashboard

Admin:
- /admin
- /admin/users
- /admin/books
- /admin/shops

Seller:
- /seller/dashboard
- /seller/orders
- /seller/inventory
- /seller/analytics
- /seller/shop
- /seller/product-detail

### 5.2 Legacy redirect da co
Da co map redirect cho duong dan cu dang:
- /Main/*.html
- /Admin/*.html
- /Seller/*.html
- /Buyer/Buyer_DashBoard.html

Controller thuc hien: src/main/java/com/example/bookstore/controller/LegacyRouteController.java

### 5.3 Tinh trang migration hien tai
- So template HTML: 92
- So static HTML: 12
- static/Seller: da rong
- static/Admin: da rong
- static/Main: van con 11 file HTML (giu de tuong thich/doi chieu)

Nhan xet:
- Seller static pages con lai da migrate xong sang templates.
- Main static pages van ton tai trong static/Main (nen quyet dinh giu hay xoa theo ke hoach rollout).

## 6. Shared UI architecture
Da co fragments dung chung cho panel:
- templates/fragments/admin_layout.html
- templates/fragments/seller_layout.html
- templates/fragments/panel_head.html

JS dashboard dung chung:
- static/js/panel-data.js

Panel-data.js goi API /api/panel de render metric, table va chart.

## 7. Seed du lieu va AI
DataSeeder (src/main/java/com/example/bookstore/DataSeeder.java):
- Xoa du lieu sach cu
- Nap toi da 300 sach tu Books.csv
- Tao gia/stock random
- Goi Gemini API de sinh mo ta cho 10 sach dau (neu co GOOGLE_AI_KEY)

Luu y:
- Can set bien moi truong GOOGLE_AI_KEY neu muon chay phan AI description.

## 8. Build va van hanh
### 8.1 Build xac nhan
Trang thai hien tai: BUILD_OK voi lenh compile skip test.

### 8.2 Lenh chay co ban
- Windows build: .\\mvnw.cmd -DskipTests compile
- Run app: .\\mvnw.cmd spring-boot:run

### 8.3 URL mac dinh
- App: http://localhost:8080
- Book API: http://localhost:8080/api/books

## 9. Rủi ro/ton dong ky thuat
- Van con static/Main HTML (co the gay confusion neu duy tri lau).
- Dang dung @CrossOrigin("*") cho API -> can gioi han origin khi len production.
- Auth hien tai tra string message, chua co JWT/session auth flow day du.
- Config DB trong application.properties dang de user/password local.

## 10. De xuat buoc tiep theo
1. Chot chien luoc migration cuoi cung cho static/Main (giu redirect + xoa static files theo dot).
2. Tao README chuan cho setup local (DB, env, run, test data).
3. Chuan hoa response API (DTO + error format) va bo sung auth/authorization.
4. Tach environment config (dev/stage/prod), an thong tin nhay cam.
5. Bo sung test cho service/controller quan trong.
