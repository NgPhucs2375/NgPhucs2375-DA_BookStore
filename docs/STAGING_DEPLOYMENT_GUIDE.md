# Staging Deployment Guide

Tài liệu này hướng dẫn triển khai BookStore lên môi trường staging bằng Docker Compose ngay trong workspace hiện tại.

## 1. Mục tiêu staging

Staging ở repo này gồm 4 thành phần:

- `bookom-mssql`: SQL Server dùng cho dữ liệu ứng dụng và Flyway migrations
- `bookom-app-1`, `bookom-app-2`, `bookom-app-3`: 3 bản sao Spring Boot
- `bookom-nginx-lb`: reverse proxy/load balancer ở cổng `80`

Luồng chạy chuẩn:

1. Build jar Spring Boot vào `target/BookStore-0.0.1-SNAPSHOT.jar`
2. Docker Compose khởi động SQL Server
3. Tạo database `BookstoreDB` trong SQL Server
4. Ba app container nối vào SQL Server qua mạng nội bộ Docker
5. Nginx nhận traffic từ `http://localhost` và forward sang các app

## 2. Điều kiện trước khi deploy

- Docker Desktop phải đang chạy
- Port `80` không được bị chiếm bởi ứng dụng khác
- Maven wrapper có thể build thành công
- Không cần SQL Server cài ngoài máy nếu dùng compose hiện tại

## 3. Triển khai từng bước

### Bước 1: Build artifact

Chạy ở thư mục gốc repo:

```bash
mvnw.cmd -DskipTests package
```

Kết quả mong đợi:

- File `target/BookStore-0.0.1-SNAPSHOT.jar` xuất hiện
- Build kết thúc không có lỗi

### Bước 2: Chỉnh secret staging nếu cần

Compose đang dùng mật khẩu mặc định cho SQL Server là `BookomStaging!123`.

Nếu muốn đổi, set biến môi trường trước khi chạy compose:

```bash
set MSSQL_SA_PASSWORD=YourStrongPassword123!
```

### Bước 3: Tạo database ứng dụng

Sau khi SQL Server container đã lên, tạo database `BookstoreDB` một lần:

```bash
docker exec bookom-mssql /opt/mssql-tools18/bin/sqlcmd ^
  -S localhost -U sa -P "BookomStaging!123" -C ^
  -Q "IF DB_ID('BookstoreDB') IS NULL CREATE DATABASE BookstoreDB;"
```

Nếu container không có `sqlcmd` ở đường dẫn này, kiểm tra trong log SQL Server hoặc dùng image tools phù hợp với môi trường của bạn.

### Bước 4: Khởi động staging

```bash
docker compose up -d
```

Compose sẽ tự:

- Tạo network nội bộ
- Tạo volume `mssql-data`
- Chạy SQL Server
- Chờ SQL Server sẵn sàng
- Khởi động 3 app Spring Boot
- Khởi động Nginx ở cổng `80`

### Bước 5: Kiểm tra trạng thái container

```bash
docker compose ps
```

Bạn cần thấy các service ở trạng thái `Up`.

### Bước 6: Kiểm tra log khởi động

```bash
docker compose logs --tail=50 bookom-mssql
docker compose logs --tail=50 bookom-app-1
docker compose logs --tail=50 nginx-lb
```

Trong log app cần thấy:

- Tomcat khởi động trên port `8080`
- Flyway chạy migration
- Không có lỗi `Connection refused` tới `localhost:1433`

### Bước 7: Test staging qua browser hoặc curl

```bash
curl -I http://localhost
```

Kỳ vọng:

- Không còn `502 Bad Gateway`
- Nginx trả về response từ backend

### Bước 8: Test notification endpoint

Khi đã có token hợp lệ, test một API notification ví dụ:

```bash
curl -X GET http://localhost/api/notifications/me \
  -H "Authorization: Bearer <token>"
```

Hoặc mở SSE endpoint trên browser sau khi đăng nhập:

```text
http://localhost/api/notifications/me/subscribe
```

## 4. Cách dừng staging

```bash
docker compose down
```

Nếu muốn xóa luôn dữ liệu SQL Server staging:

```bash
docker compose down -v
```

## 5. Troubleshooting nhanh

### 502 Bad Gateway

Nguyên nhân thường gặp:

- App chưa lên xong do SQL Server chưa sẵn sàng
- Sai mật khẩu `MSSQL_SA_PASSWORD`
- Port `80` đang bị ứng dụng khác chiếm

Lệnh kiểm tra:

```bash
docker compose logs --tail=100 bookom-app-1
```

### App báo `Connection refused localhost:1433`

Điều này nghĩa là app đang dùng cấu hình cũ, chưa nhận được `SPRING_DATASOURCE_URL` từ compose.

Kiểm tra lại:

- `src/main/resources/application.properties`
- `docker-compose.yml`

### SSE bị rớt sớm

Kiểm tra `nginx.conf`.

Guide hiện tại đã tăng timeout lên `3600s` để giữ kết nối SSE lâu hơn.

## 6. Lệnh gỡ lỗi hữu ích

```bash
docker compose restart bookom-app-1
docker compose restart nginx-lb
docker compose logs -f bookom-app-1
```

## 7. Kết luận

Nếu `docker compose ps` cho thấy 4 service đều `Up` và `curl -I http://localhost` không trả về `502`, staging đã sẵn sàng để demo hoặc test UAT.