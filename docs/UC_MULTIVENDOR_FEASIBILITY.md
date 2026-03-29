# UC Feasibility Note - Multi-vendor BookStore

Cap nhat ngay: 2026-03-29

## Ghi chu quyet dinh
- File PROJECT_SUMMARY.md da duoc xoa co chu dich boi owner du an.
- Tai lieu nay thay the vai tro note danh gia UC va huong trien khai tiep theo.

## Muc do truong thanh hien tai
- Tong quan: Project dang o muc "Nen tang backend da co, chua hoan thien nghiep vu end-to-end".
- Danh gia nhanh: ~70% cho phase Backend Multi-vendor Foundation.

Da co:
- Entity/Enum cho multi-vendor (role, seller, approval, cart/order/suborder).
- API checkout tach don theo seller.
- Seeder tao admin + seller mau + sach approved.
- Flyway migration script khoi tao schema moi.

Chua day du:
- Chua co Spring Security/JWT va phan quyen runtime.
- Chua co API day du cho cart CRUD buyer.
- Chua co quy trinh duyet sach admin hoan chinh.
- Chua co luong khoa/mo tai khoan user.
- Chua co dashboard doanh thu thuc su dua tren du lieu don hang da thanh toan.
- Frontend template chua ket noi day du API moi.

## Ma tran UC va kha nang trien khai tren project hien tai

### Nhom BUYER

| UC | Co the trien khai bang project hien tai? | Hien trang | Ket luan |
|---|---|---|---|
| B01 Dang ky/Dang nhap/Quan ly ho so | Mot phan | Co API dang ky, dang nhap co ban; chua co profile API update, chua co auth token | Kha thi cao, can bo sung security + profile API |
| B02 Tim kiem/Loc sach | Mot phan | Co list sach, paging; chua co filter backend theo ten/tac gia/danh muc dung nghia UC | Kha thi cao, can them query/filter endpoint |
| B03 Xem chi tiet sach & mo ta AI | Co | Co trang detail + du lieu description da duoc seeder bo sung AI cho mot phan sach | Co the trien khai ngay |
| B04 Quan ly gio hang | Chua day du | Da co entity Cart/CartItem, chua co cart controller CRUD day du | Kha thi cao, can bo sung API |
| B05 Dat hang & thanh toan tach don theo seller | Co mot phan lon | Da co checkout tach SubOrder theo seller; chua co cong thanh toan that | Co the demo backend ngay, thanh toan that can them |
| B06 Theo doi trang thai don ca nhan | Co mot phan | Da co endpoint lay don buyer; chua co policy phan quyen va UI day du | Kha thi cao |

### Nhom SELLER

| UC | Co the trien khai bang project hien tai? | Hien trang | Ket luan |
|---|---|---|---|
| S01 Dang ky/Cap nhat Shop Profile | Mot phan | Model User da co shopName/shopAddress; chua co API cap nhat profile seller | Kha thi cao |
| S02 Dang ban sach moi cho duyet | Mot phan | Book da co approvalStatus PENDING; chua rang buoc endpoint cho seller role | Kha thi cao |
| S03 Quan ly kho sach | Mot phan | Co endpoint update Book chung; chua tach ownership theo seller va hide/show | Kha thi trung binh-cao |
| S04 Xu ly don hang | Co mot phan lon | Co API update status SubOrder; chua kiem tra seller so huu suborder | Kha thi cao sau khi bo sung auth |
| S05 Dashboard doanh thu shop | Mot phan | Hien co panel API thong ke mang tinh tong hop/mau, chua chuan theo doanh thu thuc cua SubOrder COMPLETED | Kha thi cao |

### Nhom ADMIN

| UC | Co the trien khai bang project hien tai? | Hien trang | Ket luan |
|---|---|---|---|
| A01 Dashboard tong quan san | Mot phan | Co panel summary; nhieu chi so dang tinh tu books, chua from order lifecycle that | Kha thi cao |
| A02 Quan ly user khoa/mo | Chua | Chua co field lock/active va API khoa/mo tai khoan | Kha thi cao (can mo rong User) |
| A03 Kiem duyet noi dung sach | Mot phan | Da co approvalStatus; chua co API approve/reject va ly do tu choi | Kha thi cao |
| A04 Xem toan bo don hang | Chua day du | Co Order/SubOrder model va repository; chua co admin endpoint tong hop don he thong | Kha thi cao |

## De xuat lo trinh tiep theo (uu tien cao -> thap)

1. Security truoc
- Them Spring Security + JWT.
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
- Muc do san sang cao nhat hien nay la B03, B05 (backend), S04 (backend).
- Cac UC con lai can bo sung security, API chuyen biet theo role, va ket noi frontend.
