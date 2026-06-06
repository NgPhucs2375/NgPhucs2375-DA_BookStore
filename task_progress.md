# Task Progress: Chuyển Mock Data → Dữ Liệu Thật trong Shop_Seller.html

## Todo List

### Phase 1: Cập nhật Model/Entity
- [x] Phân tích codebase hiện tại
- [ ] Thêm trường `followerCount`, `rating`, `ratingCount` vào SellerShop entity
- [ ] Thêm trường `soldCount`, `averageRating` vào Book entity (hoặc tính từ OrderItem/Review)

### Phase 2: Cập nhật Controller (MainPageController)
- [ ] Cập nhật `GET /shop/{slug}` để truyền thêm: categories, vouchers, stats (follower, rating, join date)
- [ ] Thêm API endpoint hỗ trợ sort, filter, phân trang cho sách của shop

### Phase 3: Cập nhật Template Shop_Seller.html
- [ ] Thay thế mock stats (followers, rating, join date) bằng Thymeleaf dynamic
- [ ] Thay thế mock vouchers bằng Thymeleaf loop
- [ ] Thay thế mock categories sidebar bằng Thymeleaf loop
- [ ] Thay thế 12 sản phẩm mock bằng Thymeleaf loop với dữ liệu thật
- [ ] Thay thế mock pagination bằng Thymeleaf dynamic pagination
- [ ] Thay thế mock banner quảng cáo bằng dữ liệu động
- [ ] Cập nhật header badges (cart/wishlist count) từ session
- [ ] Thêm chức năng sort/filter bằng JavaScript + API

### Phase 4: Kiểm tra và hoàn thiện
- [ ] Verify tất cả các thay đổi
- [ ] Đảm bảo không có lỗi Thymeleaf
