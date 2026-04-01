# UC Feasibility Note - Multi-vendor BookStore

Cap nhat ngay: 2026-04-01

## Ghi chu quyet dinh
- File PROJECT_SUMMARY.md da duoc xoa co chu dich boi owner du an.
- Tai lieu nay thay the vai tro note danh gia UC va huong trien khai tiep theo.

## Muc do truong thanh hien tai
- Tong quan: Project dang o muc "Nen tang backend da co, chua hoan thien nghiep vu end-to-end".
- Danh gia nhanh: ~78% cho phase Backend Multi-vendor Foundation + Buyer storefront.

Da co:
- Entity/Enum cho multi-vendor (role, seller, approval, cart/order/suborder).
- API checkout tach don theo seller.
- Seeder tao admin + seller mau + sach approved.
- Flyway migration script khoi tao schema moi.
- Spring Security + JwtAuthenticationFilter cho carts/orders.
- Auth validation (regex + do dai input) cho login/register.
- Frontend da bind API books o trang index/discovery, click vao card di den chi tiet /book/{id}.

Chua day du:
- Security coverage chua full tat ca endpoint seller/admin/books.
- Chua co quy trinh duyet sach admin hoan chinh.
- Chua co luong khoa/mo tai khoan user.
- Chua co dashboard doanh thu thuc su dua tren du lieu don hang da thanh toan.
- Checkout/cart frontend chua ket noi day du voi user context/JWT.

## Ma tran UC va kha nang trien khai tren project hien tai

### Nhom BUYER

| UC | Co the trien khai bang project hien tai? | Hien trang | Ket luan |
|---|---|---|---|
| B01 Dang ky/Dang nhap/Quan ly ho so | Co phan lon | Co register/login/login-jwt, profile API, input validation regex; van can chot flow token tren client | Kha thi cao, uu tien dong bo frontend auth |
| B02 Tim kiem/Loc sach | Co phan lon | Co list sach, paging, search approved-only + filter theo tu khoa/danh muc | Kha thi cao |
| B03 Xem chi tiet sach & mo ta AI | Co | Co trang detail + du lieu description da duoc seeder bo sung AI cho mot phan sach | Co the trien khai ngay |
| B04 Quan ly gio hang | Co phan lon | Da co CartController + CartService voi CRUD chinh va validate ownership/stock | Kha thi cao, can bo sung UI end-to-end |
| B05 Dat hang & thanh toan tach don theo seller | Co mot phan lon | Da co checkout tach SubOrder theo seller; chua co cong thanh toan that | Co the demo backend ngay, thanh toan that can them |
| B06 Theo doi trang thai don ca nhan | Co phan lon | Da co endpoint /api/orders/me + role guard runtime cho carts/orders; UI buyer order tracking chua full | Kha thi cao |

### Nhom SELLER

| UC | Co the trien khai bang project hien tai? | Hien trang | Ket luan |
|---|---|---|---|
| S01 Dang ky/Cap nhat Shop Profile | Co phan lon | Da co profile API, co cap nhat shop info cho SELLER/ADMIN; can bo sung UX seller profile rieng | Kha thi cao |
| S02 Dang ban sach moi cho duyet | Mot phan | Book da co approvalStatus PENDING; chua rang buoc endpoint cho seller role | Kha thi cao |
| S03 Quan ly kho sach | Mot phan | Co endpoint update Book chung; chua tach ownership theo seller va hide/show | Kha thi trung binh-cao |
| S04 Xu ly don hang | Co phan lon | Co API update status SubOrder + da kiem tra ownership seller tren suborder | Kha thi cao |
| S05 Dashboard doanh thu shop | Mot phan | Hien co panel API thong ke mang tinh tong hop/mau, chua chuan theo doanh thu thuc cua SubOrder COMPLETED | Kha thi cao |

### Nhom ADMIN

| UC | Co the trien khai bang project hien tai? | Hien trang | Ket luan |
|---|---|---|---|
| A01 Dashboard tong quan san | Mot phan | Co panel summary; nhieu chi so dang tinh tu books, chua from order lifecycle that | Kha thi cao |
| A02 Quan ly user khoa/mo | Chua | Chua co field lock/active va API khoa/mo tai khoan | Kha thi cao (can mo rong User) |
| A03 Kiem duyet noi dung sach | Mot phan | Da co approvalStatus; chua co API approve/reject va ly do tu choi | Kha thi cao |
| A04 Xem toan bo don hang | Chua day du | Co Order/SubOrder model va repository; chua co admin endpoint tong hop don he thong | Kha thi cao |

## De xuat lo trinh tiep theo (uu tien cao -> thap)

1. Security hardening
- Mo rong coverage Spring Security + JWT cho toan bo endpoint seller/admin/books.
- Rang buoc role cho endpoint:
  - BUYER: cart, checkout, order cua chinh minh.
  - SELLER: suborder cua chinh seller.
  - ADMIN: duyet sach, quan ly user, xem tong don.

2. Hoan thien BUYER flow
- Bo sung CartController: add/update/remove/list.
- Bo sung Book filter API theo title/author/category + approved only.
- Ket noi Checkout_Page va Cart_Page vao API that.

3. Hoan thien SELLER flow
- SellerBookController:
  - tao sach moi => PENDING,
  - cap nhat gia/ton kho,
  - an/hien sach (them field isVisible).
- SellerOrderController:
  - list suborder theo seller,
  - cap nhat status voi validate transition.

4. Hoan thien ADMIN flow
- AdminModerationController:
  - list sach PENDING,
  - approve/reject + reason.
- AdminUserController:
  - lock/unlock user (them field active va lockedReason).
- AdminOrderController:
  - xem all order/suborder + filter status/date.

5. Bao dam du lieu va van hanh
- Chuan hoa Flyway strategy cho moi truong da co schema.
- Viet integration test cho:
  - checkout tach seller,
  - seller update status,
  - admin approve/reject.

## Ket luan tong hop
- Tat ca UC ban liet ke deu co the trien khai duoc tren nen project hien tai.
- Muc do san sang cao nhat hien nay la B03, B04, B05 (backend), B06 (backend), S04 (backend).
- Cac UC con lai can uu tien: security coverage full, admin moderation/user management, va dong bo checkout UI voi user context/JWT.

## Delta moi nhat (2026-04-01)
- Da xac nhan luong storefront:
  - main/index goi API books va render card dong.
  - main/discovery goi API books va card click den /book/{id}.
  - /book/{id} render dung Details_Produce theo du lieu sach.
- Da cap nhat auth input hardening:
  - DTO validation regex cho login/register.
  - global validation handler tra loi ro rang cho client.
