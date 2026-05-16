# Multi-vendor E-commerce Platform - Technical Documentation

## 1. Tổng Quan & Ý Tưởng

Dự án này là một nền tảng thương mại điện tử đa nhà bán hàng cho sách, nơi một hệ thống chung phục vụ nhiều vai trò: `BUYER`, `SELLER`, và `ADMIN`.

### Mục tiêu cốt lõi

- Cho phép người mua duyệt sách, tìm kiếm, thêm vào giỏ, đặt hàng và theo dõi trạng thái đơn hàng.
- Cho phép người bán quản lý gian hàng, danh mục hàng hóa, tồn kho và xử lý các sub-order thuộc về mình.
- Cho phép quản trị viên duyệt sách, theo dõi hệ thống và thao tác vận hành.
- Hỗ trợ thông báo thời gian thực, gợi ý sản phẩm, hồ sơ người dùng, địa chỉ giao hàng và OTP đăng ký.

### Bài toán hệ thống giải quyết

- Hợp nhất nhiều người bán trong cùng một luồng mua sắm, nhưng vẫn giữ được tính sở hữu theo từng shop.
- Chia một đơn hàng lớn thành nhiều sub-order theo từng seller để dễ xử lý fulfillment.
- Tách rõ dữ liệu, quyền truy cập và luồng nghiệp vụ cho buyer/seller/admin.
- Hỗ trợ chạy trên hạ tầng có khả năng mở rộng, có health check, graceful shutdown và cơ chế lock phân tán cho worker nền.

### Ý tưởng sản phẩm

- Nền tảng hoạt động như một monolith Spring Boot, nhưng đã được tổ chức theo các khối chức năng rõ ràng để có thể tiến hóa dần lên kiến trúc phân tán hơn.
- UI được triển khai bằng Thymeleaf kết hợp static assets, trong khi API JSON phục vụ phần tương tác động và các luồng nghiệp vụ.

## 2. Kiến Trúc Hệ Thống

### Luồng kiến trúc tổng thể

1. Trình duyệt gọi các trang Thymeleaf hoặc REST API.
2. Spring MVC route request đến controller tương ứng.
3. Controller gọi service chứa logic nghiệp vụ.
4. Service thao tác qua repository/JPA entity.
5. Dữ liệu được lưu trong SQL Server, schema được quản lý bằng Flyway.
6. Với thông báo thời gian thực, notification được lưu DB trước, sau đó queue worker đẩy ra SSE.

### Các lớp chính

| Lớp | Vai trò |
|---|---|
| Controller | Nhận HTTP request, validate đầu vào, điều hướng sang service hoặc view |
| Service | Chứa nghiệp vụ chính: auth, cart, order, shop, notification, profile |
| Repository | Truy cập dữ liệu qua Spring Data JPA và custom query |
| Entity | Mô hình dữ liệu lõi: User, Book, Order, SubOrder, SellerShop, Notification |
| DTO | Tách dữ liệu request/response khỏi entity trong các API quan trọng |
| Infrastructure | JWT, SSE, distributed lock, graceful shutdown, heartbeat, seeding |

### Design patterns và kỹ thuật nổi bật

- Layered Architecture / MVC: phân tầng rõ giữa UI, API, service, persistence.
- Repository Pattern: mọi truy xuất dữ liệu đều đi qua repository.
- DTO Pattern: đa số API dùng request/response DTO để kiểm soát dữ liệu trao đổi.
- Builder Pattern: được dùng rộng rãi cho entity và DTO response.
- Filter Chain: JWT authentication filter gắn vào Spring Security chain.
- Scheduled Job: recommendation precompute, heartbeat, queue worker chạy định kỳ.
- Observer-like realtime delivery: SSE service phát sự kiện cho client đang subscribe.
- Strategy-like fallback: recommendation có cache precompute và engine fallback theo author/category.
- Template Fragment Composition: UI Thymeleaf tái sử dụng layout/fragments.

### Core framework và thư viện

| Nhóm | Công nghệ |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.2.4 |
| Web | Spring MVC, Spring Web |
| Persistence | Spring Data JPA, Hibernate |
| Database migration | Flyway |
| Security | Spring Security, JWT custom filter, BCrypt |
| View | Thymeleaf |
| Validation | Spring Validation, Jakarta Validation |
| Email | Spring Mail |
| Observability | Spring Boot Actuator, health endpoints |
| File/image handling | Apache Tika |
| HTML sanitization | OWASP Java HTML Sanitizer |
| JSON | Jackson |
| Test DB | H2 |
| DB target | Microsoft SQL Server |
| Build | Maven |

### Kiến trúc realtime và distributed

- Notification được lưu vào bảng `notifications` trước, sau đó đưa vào bảng `notification_delivery` để worker xử lý.
- `NotificationSseService` giữ danh sách SSE connection theo `userId` trong bộ nhớ của từng instance.
- `NotificationDeliveryQueue` là worker nền quét DB định kỳ và bắn sự kiện SSE.
- `DistributedLockService` dùng stored procedure và bảng lock để đảm bảo chỉ một instance làm queue worker.
- `HeartbeatService` vừa gửi heartbeat cho client, vừa refresh distributed lock.
- `ApplicationStartupListener` và `GracefulShutdownComponent` quản lý vòng đời worker khi app khởi động/tắt.

## 3. Quy Trình Nghiệp Vụ

### 3.1 Đăng ký và đăng nhập

1. Người dùng gọi OTP request.
2. OTP được lưu tạm trong bộ nhớ, đồng thời gửi mail nếu SMTP đã cấu hình.
3. Người dùng xác thực OTP.
4. Đăng ký buyer/seller/admin được thực hiện sau khi OTP hợp lệ.
5. Đăng nhập tạo JWT token ở endpoint `login-jwt`.

### 3.2 Quản lý shop của seller

1. Seller tạo shop qua `/api/seller/me/shop`.
2. Shop được gắn với đúng seller theo `sellerId`.
3. Shop có `slug` duy nhất và `approvalStatus`.
4. Seller có thể cập nhật/xóa shop của chính mình.
5. Khách có thể xem shop public qua slug nếu shop đang `APPROVED`.

### 3.3 Duyệt và khám phá sách

1. Buyer truy cập danh sách sách đã duyệt.
2. Tìm kiếm theo keyword, category, author, giá và năm xuất bản.
3. Hệ thống cung cấp suggestion, best-seller và trending books.
4. Trang chi tiết sách kéo theo gợi ý `bought together` và `similar books`.

### 3.4 Giỏ hàng và checkout

1. Buyer thêm sách vào giỏ, hệ thống kiểm tra sách đã duyệt và còn tồn kho.
2. Khi checkout, hệ thống gom cart items theo seller.
3. Một `Order` gốc được tạo, bên trong sinh ra nhiều `SubOrder` theo từng seller.
4. Mỗi sub-order chứa nhiều `OrderItem`.
5. Sau checkout, giỏ hàng được làm rỗng.

### 3.5 Xử lý đơn hàng multi-vendor

1. Buyer xem order của mình hoặc filter theo thời gian/giá/trạng thái.
2. Seller xem sub-orders thuộc shop của mình.
3. Seller cập nhật trạng thái sub-order: `PENDING_PAYMENT`, `PROCESSING`, `SHIPPING`, `COMPLETED`, `CANCELLED`.
4. Buyer chỉ được hủy đơn khi toàn bộ sub-order còn ở trạng thái cho phép hủy.

### 3.6 Thông báo thời gian thực

1. Notification được tạo và lưu DB.
2. Delivery task được enqueue vào bảng delivery.
3. Queue worker quét task pending và gửi qua SSE.
4. Nếu gửi lỗi, task được retry theo backoff.
5. Nếu quá số lần thử, task chuyển sang `DROPPED`.

### 3.7 Hồ sơ, địa chỉ, bảo mật tài khoản

1. Buyer cập nhật profile, địa chỉ giao hàng và mật khẩu.
2. Hệ thống ghi nhận security events cho các hành động nhạy cảm.
3. Người dùng có thể đặt địa chỉ mặc định.

### 3.8 Gợi ý sản phẩm

1. Job nền precompute danh sách mua cùng nhau và sách tương tự.
2. Cache snapshot được swap atomically.
3. Khi cache thiếu dữ liệu, engine fallback dựa trên cùng tác giả hoặc cùng category.

## 4. Tình Trạng Hiện Tại & Đánh Giá

### Trạng thái module

| Module | Trạng thái | Nhận định |
|---|---|---|
| Auth + OTP + JWT | Hoàn thiện mức nền tảng | Có đăng ký, xác thực OTP, phát token JWT |
| Catalog book/search/discovery | Hoàn thiện | Có search, suggestions, trending, best-seller |
| Cart | Hoàn thiện | Kiểm tra tồn kho và chỉ cho sách đã duyệt |
| Order + sub-order | Hoàn thiện | Đã chia theo seller và có filter/status |
| Seller shop | Gần hoàn thiện | Có CRUD shop, nhưng luồng duyệt còn yếu |
| Notifications + SSE | Hoàn thiện về chức năng, còn rủi ro kiến trúc | Có queue, retry, unread count, realtime |
| Buyer profile/address/security | Hoàn thiện | Có CRUD địa chỉ, đổi mật khẩu, log security event |
| Recommendation | Hoàn thiện mức cơ bản | Có cache precompute và fallback |
| Health/ops/seeding | Hoàn thiện | Có healthcheck, graceful shutdown, seeder |
| Payment gateway | Chưa có | Không thấy integration cổng thanh toán |
| Shipping/fulfillment tracking | Chưa có | Chưa có module vận đơn, phí ship, tracking |
| Returns/refunds/coupons | Chưa có | Không thấy workflow/DB tương ứng |

### Vấn đề hiện tại

- `AdminBookController` kiểm tra `CURRENT_USER_ROLE`, nhưng `JwtAuthenticationFilter` chỉ set `CURRENT_USER_ID`; role không được gắn vào request attribute.
- `SellerShopService.changeStatus()` cho seller tự đổi `approvalStatus` của chính shop mình, có thể tự kích hoạt `APPROVED`.
- `BuyerProfileController` có fallback lấy user đầu tiên nếu không có auth, rất nguy hiểm cho môi trường thật.
- Một số endpoint vẫn dựa trên `X-User-Id` hoặc request attribute thay vì một cơ chế auth thống nhất.
- Nhiều API trả thẳng entity JPA thay vì DTO, làm tăng rủi ro lộ dữ liệu và phụ thuộc lazy loading.
- `NotificationSseService` lưu connection trong bộ nhớ cục bộ của từng instance nên không cluster-safe.
- `BookService.uploadAndVerifyCoverImage()` lưu file trực tiếp vào `src/main/resources/static/images/covers/`, phù hợp dev nhưng không tốt cho production.

### Điểm thiếu sót

- Chưa có payment transaction/entity/gateway.
- Chưa có shipping provider, order tracking, cancellation/refund flow đầy đủ.
- Chưa có admin workflow chuẩn để duyệt shop bằng quyền admin.
- Chưa có centralized object storage cho ảnh sách/shop.
- Chưa có cơ chế auth thống nhất xuyên suốt UI/API.
- Chưa có background cleanup rõ ràng cho notification delivery logs nếu bảng tăng lớn.

### Rủi ro

- Security: JWT secret có default value dev, nếu không override sẽ yếu về bảo mật.
- Security: `@CrossOrigin("*")` và nhiều route public có thể mở rộng bề mặt tấn công nếu không siết lại.
- Security: các lỗ hổng IDOR có thể xuất hiện nếu request header/attribute bị tin quá mức.
- Scalability: SSE connection map là in-memory, cần sticky sessions hoặc shared pub/sub nếu scale nhiều instance.
- Performance: nhiều truy vấn dùng entity graph lớn hoặc `findAll()` rồi filter trong memory dễ gây bottleneck.
- Performance: order/cart/profile/recommendation có nguy cơ N+1 nếu fetch association không tối ưu.
- Reliability: worker notifications phụ thuộc vào DB lock/stored procedure; nếu DB ops không đồng bộ sẽ ảnh hưởng realtime.

### Cần cải tiến & phát triển tiếp

- Chuẩn hóa authentication/authorization dựa trên Spring Security context thay vì request header tùy biến.
- Đổi toàn bộ public API sang DTO response để giảm coupling với entity.
- Tách storage ảnh ra object storage hoặc service riêng.
- Bổ sung payment, shipping và refund workflow.
- Thêm admin moderation thật cho shop/book approval.
- Tối ưu query bằng fetch join, projection, paging chuẩn cho các màn admin/seller.
- Thêm audit log và metrics cho notification queue, SSE, recommendation job.
- Nếu scale nhiều instance, cần shared notification fan-out layer thay cho map SSE trong bộ nhớ.

## 5. Chi Tiết Cấu Trúc Code & Vai Trò File

### Controllers

| File tiêu biểu | Vai trò |
|---|---|
| `src/main/java/com/example/bookstore/controller/AuthController.java` | Đăng ký, đăng nhập, OTP, profile auth cơ bản |
| `src/main/java/com/example/bookstore/controller/BookController.java` | API danh mục sách, search, discovery, CRUD seller book |
| `src/main/java/com/example/bookstore/controller/OrderController.java` | Checkout, danh sách đơn, filter, hủy đơn, update sub-order |
| `src/main/java/com/example/bookstore/controller/CartController.java` | Quản lý giỏ hàng buyer |
| `src/main/java/com/example/bookstore/controller/SellerShopController.java` | CRUD shop của seller và public shop detail |
| `src/main/java/com/example/bookstore/controller/NotificationController.java` | Danh sách notification, unread count, mark read, SSE subscribe |
| `src/main/java/com/example/bookstore/controller/BuyerProfileController.java` | Thymeleaf page + API cho hồ sơ, địa chỉ, đổi mật khẩu |
| `src/main/java/com/example/bookstore/controller/AdminBookController.java` | Duyệt sách pending và đổi trạng thái |
| `src/main/java/com/example/bookstore/controller/PanelController.java` | Dashboard API tổng hợp cho admin/seller |
| `src/main/java/com/example/bookstore/controller/PanelPageController.java` | Điều hướng view admin/seller dashboard |
| `src/main/java/com/example/bookstore/controller/MainPageController.java` | Điều hướng trang public Thymeleaf |
| `src/main/java/com/example/bookstore/controller/PageController.java` | Trang chi tiết sách và recommendation |
| `src/main/java/com/example/bookstore/controller/HealthCheckController.java` | Health, liveness, readiness, queue-worker status |

### Services

| File tiêu biểu | Vai trò |
|---|---|
| `src/main/java/com/example/bookstore/service/AuthService.java` | Business logic đăng ký, login, profile update |
| `src/main/java/com/example/bookstore/service/AuthOtpService.java` | Sinh, verify và consume OTP |
| `src/main/java/com/example/bookstore/service/BookService.java` | CRUD sách, duyệt sách, upload cover, chống IDOR |
| `src/main/java/com/example/bookstore/service/CartService.java` | Quản lý cart item, tính tổng tiền, kiểm tra tồn kho |
| `src/main/java/com/example/bookstore/service/OrderService.java` | Checkout, tách sub-order, filter, cancel, update status |
| `src/main/java/com/example/bookstore/service/SellerShopService.java` | Quản lý shop seller, slug, status |
| `src/main/java/com/example/bookstore/service/BuyerProfileService.java` | Hồ sơ, địa chỉ, đổi mật khẩu, security event |
| `src/main/java/com/example/bookstore/service/NotificationService.java` | Tạo/lấy/đánh dấu notification, broadcast, unread count |
| `src/main/java/com/example/bookstore/service/WishlistService.java` | Thêm/xóa/xem wishlist cho buyer |
| `src/main/java/com/example/bookstore/service/DatabaseSeederService.java` | Seed category, user, book từ CSV và AI enrichment |
| `src/main/java/com/example/bookstore/service/MailService.java` | Gửi mail OTP |
| `src/main/java/com/example/bookstore/service/GeminiService.java` | Gọi Gemini để sinh mô tả sách |
| `src/main/java/com/example/bookstore/service/recommendation/RecommendationService.java` | Lấy sách tương tự và mua cùng nhau |
| `src/main/java/com/example/bookstore/service/recommendation/RecommendationJob.java` | Precompute recommendation cache theo lịch |
| `src/main/java/com/example/bookstore/service/recommendation/RecommendationFallbackEngine.java` | Fallback recommendation theo author/category |
| `src/main/java/com/example/bookstore/distributed/DistributedLockService.java` | Quản lý lock phân tán cho queue worker |
| `src/main/java/com/example/bookstore/sse/NotificationSseService.java` | Quản lý SSE connection trong bộ nhớ |
| `src/main/java/com/example/bookstore/sse/NotificationDeliveryQueue.java` | Queue worker DB-backed cho notification |
| `src/main/java/com/example/bookstore/sse/HeartbeatService.java` | Gửi heartbeat và refresh lock |
| `src/main/java/com/example/bookstore/lifecycle/ApplicationStartupListener.java` | Khởi tạo lock và worker khi app ready |
| `src/main/java/com/example/bookstore/lifecycle/GracefulShutdownComponent.java` | Giải phóng lock và shutdown an toàn |

### Repositories

| File tiêu biểu | Vai trò |
|---|---|
| `src/main/java/com/example/bookstore/repository/UserRepository.java` | Tra cứu user, role, username |
| `src/main/java/com/example/bookstore/repository/BookRepository.java` | Search, discovery, seller inventory, best-seller/trending |
| `src/main/java/com/example/bookstore/repository/OrderRepository.java` | Query order của buyer theo thời gian/giá/trang |
| `src/main/java/com/example/bookstore/repository/SubOrderRepository.java` | Query sub-order của seller, filter nâng cao |
| `src/main/java/com/example/bookstore/repository/OrderItemRepository.java` | Phục vụ recommendation và pair mining |
| `src/main/java/com/example/bookstore/repository/CartRepository.java` | Tìm cart theo buyer |
| `src/main/java/com/example/bookstore/repository/CartItemRepository.java` | Tìm cart item theo cart/book |
| `src/main/java/com/example/bookstore/repository/SellerShopRepository.java` | Tìm shop theo sellerId hoặc slug |
| `src/main/java/com/example/bookstore/repository/NotificationRepository.java` | Tìm/lọc/đánh dấu notification read |
| `src/main/java/com/example/bookstore/repository/NotificationDeliveryRepository.java` | Queue polling, retry, audit delivery |
| `src/main/java/com/example/bookstore/repository/UserAddressRepository.java` | Quản lý địa chỉ buyer |
| `src/main/java/com/example/bookstore/repository/UserSecurityEventRepository.java` | Audit profile/password/address events |
| `src/main/java/com/example/bookstore/repository/CategoryRepository.java` | Quản lý category |

### Entities & enums

| File tiêu biểu | Vai trò |
|---|---|
| `src/main/java/com/example/bookstore/model/User.java` | User đa vai trò, wishlist, favorites, cart, address |
| `src/main/java/com/example/bookstore/model/Book.java` | Sản phẩm cốt lõi của platform |
| `src/main/java/com/example/bookstore/model/Category.java` | Phân loại sách |
| `src/main/java/com/example/bookstore/model/Cart.java` | Giỏ hàng của buyer |
| `src/main/java/com/example/bookstore/model/CartItem.java` | Dòng hàng trong cart |
| `src/main/java/com/example/bookstore/model/Order.java` | Đơn hàng gốc |
| `src/main/java/com/example/bookstore/model/SubOrder.java` | Đơn con theo từng seller |
| `src/main/java/com/example/bookstore/model/OrderItem.java` | Dòng hàng trong sub-order |
| `src/main/java/com/example/bookstore/model/SellerShop.java` | Hồ sơ gian hàng seller |
| `src/main/java/com/example/bookstore/model/Notification.java` | Notification nghiệp vụ |
| `src/main/java/com/example/bookstore/model/NotificationDelivery.java` | Log delivery + retry cho realtime notification |
| `src/main/java/com/example/bookstore/model/UserAddress.java` | Địa chỉ giao hàng |
| `src/main/java/com/example/bookstore/model/UserSecurityEvent.java` | Audit bảo mật user |
| `src/main/java/com/example/bookstore/model/enums/UserRole.java` | BUYER / SELLER / ADMIN |
| `src/main/java/com/example/bookstore/model/enums/OrderStatus.java` | Trạng thái đơn hàng |
| `src/main/java/com/example/bookstore/model/enums/ApprovalStatus.java` | Trạng thái duyệt |
| `src/main/java/com/example/bookstore/model/enums/NotificationType.java` | Loại notification |
| `src/main/java/com/example/bookstore/model/enums/NotificationPriority.java` | Mức ưu tiên notification |

### DTOs

| File tiêu biểu | Vai trò |
|---|---|
| `src/main/java/com/example/bookstore/dto/AuthRegisterRequest.java` | Request đăng ký |
| `src/main/java/com/example/bookstore/dto/AuthLoginRequest.java` | Request đăng nhập |
| `src/main/java/com/example/bookstore/dto/CheckoutRequest.java` | Checkout từ cart |
| `src/main/java/com/example/bookstore/dto/CheckoutResponse.java` | Kết quả checkout |
| `src/main/java/com/example/bookstore/dto/CartResponse.java` | Response giỏ hàng |
| `src/main/java/com/example/bookstore/dto/OrderSummaryResponse.java` | Tóm tắt đơn buyer |
| `src/main/java/com/example/bookstore/dto/SubOrderSummaryResponse.java` | Tóm tắt sub-order seller |
| `src/main/java/com/example/bookstore/dto/NotificationListResponse.java` | Danh sách notification có paging |
| `src/main/java/com/example/bookstore/dto/UserProfileDTO.java` | Profile buyer và form mapping |
| `src/main/java/com/example/bookstore/dto/UserAddressDTO.java` | DTO địa chỉ |
| `src/main/java/com/example/bookstore/dto/SellerShopUpsertRequest.java` | Request tạo/cập nhật shop |
| `src/main/java/com/example/bookstore/dto/SellerShopResponse.java` | Response shop |
| `src/main/java/com/example/bookstore/dto/WishlistItemResponse.java` | Item wishlist |
| `src/main/java/com/example/bookstore/dto/SeedRequest.java` | Cấu hình seed dữ liệu |

### Config, security và hạ tầng

| File tiêu biểu | Vai trò |
|---|---|
| `src/main/java/com/example/bookstore/security/SecurityConfig.java` | Cấu hình Spring Security, CORS, password encoder |
| `src/main/java/com/example/bookstore/security/JwtAuthenticationFilter.java` | Parse JWT và set authentication context |
| `src/main/java/com/example/bookstore/security/JwtTokenProvider.java` | Tạo/verify JWT |
| `src/main/java/com/example/bookstore/security/WebConfig.java` | Map static image directory ra `/images/covers/**` |
| `src/main/java/com/example/bookstore/security/SecurityUtils.java` | Sanitizer HTML để giảm XSS |
| `src/main/java/com/example/bookstore/config/RecommendationConfig.java` | Tham số recommendation algorithm |
| `src/main/java/com/example/bookstore/controller/DatabaseSeederController.java` | Trigger seed dữ liệu qua API |
| `src/main/java/com/example/bookstore/controller/HealthCheckController.java` | Health/liveness/readiness/queue status |

### Resources và giao diện

| File/thư mục | Vai trò |
|---|---|
| `src/main/resources/application.properties` | Cấu hình datasource, Flyway, SMTP, Gemini, pool, lock |
| `src/main/resources/db/migration/` | Toàn bộ migration schema và stored procedure cho lock |
| `src/main/resources/templates/main/` | Trang public: home, search, cart, checkout, order |
| `src/main/resources/templates/buyer/` | Dashboard và profile buyer |
| `src/main/resources/templates/seller/` | Dashboard, inventory, orders, analytics, shop |
| `src/main/resources/templates/admin/` | Admin dashboard và quản trị |
| `src/main/resources/templates/fragments/` | Layout và fragment tái sử dụng |
| `src/main/resources/static/js/` | JS tích hợp API cho các page |
| `src/main/resources/static/images/covers/` | Ảnh cover sách được upload |

## Kết Luận Ngắn

Hiện tại hệ thống đã có nền tảng multi-vendor khá rõ: auth, catalog, cart, order split theo seller, shop management, notification realtime, profile và recommendation. Tuy nhiên để đạt mức production-ready thật sự, ưu tiên lớn nhất là chuẩn hóa auth/role, bổ sung payment/shipping, và xử lý kiến trúc SSE đa instance để tránh phụ thuộc vào trạng thái bộ nhớ cục bộ.