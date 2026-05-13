# Seller Analytics Dashboard Tasks

Tài liệu này mô tả chi tiết các đầu việc cần làm để hoàn thiện dashboard doanh thu và hàng tồn cho seller theo UI đã thống nhất.

## 1. Chuẩn hóa API seller analytics

### Mục tiêu
Tạo một API thống nhất, trả về dữ liệu analytics thật cho dashboard seller thay vì dữ liệu giả lập hoặc tổng hợp từ sách.

### Cần làm
- Xác định một endpoint chính cho dashboard seller, ví dụ `/api/seller/analytics`.
- Trả về cấu trúc dữ liệu ổn định cho toàn bộ UI:
  - KPI chính
  - Doanh thu theo thời gian
  - Doanh thu theo danh mục
  - Top sản phẩm bán chạy
  - Top sản phẩm tồn kho ít
  - Giao dịch gần đây
  - Cảnh báo tồn kho
- Đồng bộ tên field giữa backend và frontend để tránh mapping thủ công.
- Xem lại quyền truy cập seller để chỉ seller hợp lệ mới lấy được dữ liệu của chính họ.

### Tiêu chí hoàn thành
- API trả về JSON rõ ràng, không phụ thuộc vào dữ liệu giả.
- Frontend chỉ cần gọi một hoặc vài endpoint chuẩn, không phải tự ghép dữ liệu từ nhiều nguồn không đồng nhất.

### File có thể tác động
- `src/main/java/com/example/bookstore/controller/PanelController.java`
- `src/main/java/com/example/bookstore/controller/PanelPageController.java`
- `src/main/java/com/example/bookstore/service/OrderService.java`
- `src/main/java/com/example/bookstore/repository/OrderRepository.java`
- `src/main/java/com/example/bookstore/repository/SubOrderRepository.java`
- `src/main/java/com/example/bookstore/repository/OrderItemRepository.java`

## 2. Tổng hợp doanh thu theo thời gian

### Mục tiêu
Hiển thị biểu đồ doanh thu theo ngày, tuần hoặc tháng để seller nhìn được xu hướng tăng giảm.

### Cần làm
- Lấy dữ liệu doanh thu theo mốc thời gian từ đơn hàng đã hoàn tất hoặc trạng thái đủ điều kiện tính doanh thu.
- Hỗ trợ lọc theo khoảng thời gian:
  - 7 ngày gần nhất
  - 30 ngày gần nhất
  - Tháng hiện tại
  - Khoảng ngày tùy chọn
- Tính số đơn, số sản phẩm bán ra và doanh thu thuần theo từng mốc.
- Quy định rõ trạng thái nào được tính vào doanh thu.

### Tiêu chí hoàn thành
- Biểu đồ line/bar có dữ liệu thật theo thời gian.
- Khi đổi khoảng thời gian, dữ liệu đổi đúng theo filter.

### File có thể tác động
- `src/main/java/com/example/bookstore/service/OrderService.java`
- `src/main/java/com/example/bookstore/controller/PanelController.java`
- `src/main/resources/static/js/panel-data.js`
- `src/main/resources/static/js/seller-dashboard-integration.js`

## 3. Top 5 sản phẩm bán chạy

### Mục tiêu
Hiển thị 5 sản phẩm bán tốt nhất trong kỳ theo số lượng bán và doanh thu.

### Cần làm
- Tổng hợp số lượng bán ra theo từng sản phẩm.
- Tính doanh thu của từng sản phẩm trong khoảng thời gian đang lọc.
- Sắp xếp giảm dần theo số lượng bán hoặc doanh thu, rồi lấy top 5.
- Trả về đủ thông tin để hiển thị:
  - Tên sản phẩm
  - Ảnh nhỏ nếu có
  - Số lượng bán
  - Doanh thu thuần
  - Tỷ lệ so với sản phẩm đứng đầu để render progress bar

### Tiêu chí hoàn thành
- Dashboard hiển thị top 5 rõ ràng, dễ so sánh.
- Dữ liệu phản ánh đúng doanh số bán hàng.

### File có thể tác động
- `src/main/java/com/example/bookstore/repository/OrderItemRepository.java`
- `src/main/java/com/example/bookstore/service/OrderService.java`
- `src/main/java/com/example/bookstore/controller/PanelController.java`
- `src/main/resources/static/js/panel-data.js`
- `src/main/resources/templates/seller/Seller_Analytics.html`

## 4. Top 5 sản phẩm tồn kho ít

### Mục tiêu
Hiển thị các sản phẩm đang có tồn kho thấp nhất để seller chủ động nhập hàng.

### Cần làm
- Lấy danh sách sản phẩm theo số lượng tồn kho tăng dần.
- Kết hợp với số lượng đã bán trong kỳ để seller thấy tốc độ tiêu thụ.
- Đánh dấu cảnh báo bằng màu đỏ/cam nếu tồn kho dưới ngưỡng an toàn.
- Lấy top 5 sản phẩm tồn kho ít nhất, ưu tiên các sản phẩm cần nhập trước.

### Tiêu chí hoàn thành
- Dashboard hiển thị top tồn kho ít nhất đúng theo dữ liệu thật.
- Có cảnh báo trực quan khi hàng dưới ngưỡng.

### File có thể tác động
- `src/main/java/com/example/bookstore/repository/BookRepository.java`
- `src/main/java/com/example/bookstore/service/BookService.java`
- `src/main/java/com/example/bookstore/controller/PanelController.java`
- `src/main/resources/static/js/panel-data.js`
- `src/main/resources/templates/seller/Seller_Analytics.html`

## 5. Dựng KPI và bộ lọc thời gian

### Mục tiêu
Tạo các thẻ KPI ở đầu dashboard và bộ lọc thời gian để xem dữ liệu theo kỳ mong muốn.

### Cần làm
- Xác định các KPI chính:
  - Tổng doanh thu
  - Số đơn hoàn thành
  - Giá trị đơn hàng trung bình (AOV)
  - Tỷ lệ hoàn thành đơn
  - Số lượng sản phẩm đã bán
- Thêm bộ lọc thời gian ở header hoặc vùng điều khiển.
- Khi thay đổi bộ lọc, tất cả KPI và biểu đồ phải cập nhật đồng bộ.

### Tiêu chí hoàn thành
- KPI hiển thị số liệu thật và thay đổi theo thời gian lọc.
- Bộ lọc hoạt động nhất quán trên toàn dashboard.

### File có thể tác động
- `src/main/resources/templates/seller/Seller_Dashboard.html`
- `src/main/resources/templates/seller/Seller_Analytics.html`
- `src/main/resources/static/js/seller-dashboard-integration.js`
- `src/main/resources/static/js/panel-data.js`

## 6. Vẽ biểu đồ doanh thu và danh mục

### Mục tiêu
Tạo phần visual analytics để seller đọc xu hướng nhanh hơn.

### Cần làm
- Biểu đồ doanh thu theo thời gian: line hoặc bar.
- Biểu đồ phân bố doanh thu theo danh mục: donut hoặc horizontal bar.
- Đồng bộ style màu sắc, legend và tooltip theo UI của shop.
- Đảm bảo biểu đồ không phụ thuộc vào dữ liệu giả lập.

### Tiêu chí hoàn thành
- Biểu đồ render đúng dữ liệu thật.
- Tooltip và nhãn dễ đọc trên desktop và mobile.

### File có thể tác động
- `src/main/resources/templates/seller/Seller_Analytics.html`
- `src/main/resources/static/js/panel-data.js`
- `src/main/resources/static/js/seller-dashboard-integration.js`
- `src/main/resources/templates/fragments/seller_layout.html`

## 7. Bảng giao dịch gần đây

### Mục tiêu
Hiển thị danh sách giao dịch gần nhất để seller theo dõi doanh số thực tế.

### Cần làm
- Tạo bảng giao dịch gần đây, không hiển thị trạng thái đơn chi tiết.
- Chỉ giữ các cột cần thiết:
  - Mã giao dịch
  - Ngày
  - Khách hàng
  - Sản phẩm
  - Số lượng
  - Thành tiền
  - Hình thức thanh toán
- Thêm tìm kiếm và nếu cần thì nút export.
- Cho phép click để mở chi tiết giao dịch/sản phẩm nếu có nhu cầu.

### Tiêu chí hoàn thành
- Bảng hiển thị dữ liệu thật, sắp xếp theo thời gian mới nhất.
- Không còn phụ thuộc vào bảng trạng thái đơn của seller dashboard cũ.

### File có thể tác động
- `src/main/java/com/example/bookstore/controller/PanelController.java`
- `src/main/resources/static/js/panel-data.js`
- `src/main/resources/static/js/seller-dashboard-integration.js`
- `src/main/resources/templates/seller/Seller_Analytics.html`
- `src/main/resources/templates/seller/Seller_Dashboard.html`

## 8. Cảnh báo tồn kho và test

### Mục tiêu
Thêm cảnh báo hành động nhanh và đảm bảo dashboard không bị vỡ khi triển khai.

### Cần làm
- Hiển thị cảnh báo khi sản phẩm trong top tồn kho ít chạm ngưỡng an toàn.
- Có thể gom cảnh báo thành widget nhỏ ở cuối dashboard.
- Viết test cho các phần quan trọng:
  - API analytics trả đúng schema
  - Tính doanh thu theo thời gian
  - Top sản phẩm bán chạy
  - Top tồn kho ít
  - Bảng giao dịch gần đây
- Kiểm tra lại template và JS để không còn gọi nhầm dữ liệu mock.

### Tiêu chí hoàn thành
- Cảnh báo tồn kho xuất hiện đúng khi có dữ liệu cần nhập hàng.
- Các test cốt lõi pass và bảo vệ được dashboard analytics.

### File có thể tác động
- `src/test/java/**`
- `src/main/resources/static/js/panel-data.js`
- `src/main/resources/static/js/seller-dashboard-integration.js`
- `src/main/resources/templates/seller/Seller_Analytics.html`

## Thứ tự ưu tiên đề xuất

1. Chuẩn hóa API seller analytics.
2. Tổng hợp doanh thu theo thời gian.
3. Top 5 sản phẩm bán chạy.
4. Top 5 sản phẩm tồn kho ít.
5. Dựng KPI và bộ lọc thời gian.
6. Vẽ biểu đồ doanh thu và danh mục.
7. Bảng giao dịch gần đây.
8. Cảnh báo tồn kho và test.
