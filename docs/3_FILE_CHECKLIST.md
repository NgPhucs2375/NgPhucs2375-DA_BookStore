# 🎯 3 FILE CÒN LẠI - CHECKLIST TRIỂN KHAI

**Sprint hiện tại**: May 13, 2026 | **Ưu tiên**: CAO | **Effort**: 6-8 giờ

---

## 📝 TEMPLATE 1: Checkout_Page.html

**Trạng thái**: ⏳ Chưa bắt đầu  
**Độ ưu tiên**: 🔴 CAO (Nhiều mock data)  
**Effort**: 2-3 giờ

### 🔍 Để tìm Mock Data:
```bash
Tìm: "fallback" hoặc "cart-item" hoặc giá hardcoded (đ)
```

### ✅ Checklist Cập nhật:

- [ ] **Bước 1**: Đọc file Checkout_Page.html
- [ ] **Bước 2**: Tìm `<div id="cart-items-fallback">` hoặc tương tự
- [ ] **Bước 3**: Xoá mock data (items với giá, tên sách hardcoded)
- [ ] **Bước 4**: Thêm 3 divs:
  ```html
  <div id="checkout-skeleton" class="hidden"><!-- Skeleton HTML --></div>
  <div id="checkout-empty-state" class="hidden"><!-- Empty message --></div>
  <div id="checkout-items-live"></div>
  ```
- [ ] **Bước 5**: Thêm script import:
  ```html
  <script src="/js/ui-enhancements.js"></script>
  ```
- [ ] **Bước 6**: Cập nhật JavaScript để:
  - Show skeleton loader khi fetch
  - Render real data từ API
  - Hide skeleton khi done
  - Show toast notifications
- [ ] **Bước 7**: Test trên browser (DevTools throttle slow network)
- [ ] **Bước 8**: Test trên mobile

### 📋 Template Code Pattern:

```html
<!-- XOÁĐI: Hardcoded mock items -->
<!-- Thay bằng: -->

<div id="checkout-skeleton" class="space-y-4 hidden">
  <div class="bg-white border border-brand-accent rounded-xl shadow-sm overflow-hidden">
    <div class="p-5 space-y-4">
      <div class="flex gap-4">
        <div class="skeleton h-24 w-20 rounded flex-shrink-0"></div>
        <div class="flex-1 space-y-2">
          <div class="skeleton h-4 w-3/4 rounded"></div>
          <div class="skeleton h-4 w-1/2 rounded"></div>
          <div class="skeleton h-6 w-1/4 rounded"></div>
        </div>
      </div>
    </div>
  </div>
</div>

<div id="checkout-items-live" class="space-y-6"></div>

<script>
  document.addEventListener('DOMContentLoaded', async () => {
    const skeleton = document.getElementById('checkout-skeleton');
    const container = document.getElementById('checkout-items-live');
    
    skeleton.classList.remove('hidden');
    
    try {
      const { userId } = ApiService.getAuth();
      const cart = await ApiService.Cart.get(userId);
      
      if (cart && cart.items.length > 0) {
        renderCheckoutItems(cart.items, container);
        UIEnhancements.ToastService.info('Giỏ hàng đã tải');
      } else {
        container.innerHTML = '<p class="text-center text-gray-500">Giỏ hàng trống</p>';
      }
    } catch (error) {
      UIEnhancements.ToastService.error('Lỗi tải giỏ hàng');
    } finally {
      skeleton.classList.add('hidden');
    }
  });
</script>
```

---

## 📝 TEMPLATE 2: Order_Success.html

**Trạng thái**: ⏳ Chưa bắt đầu  
**Độ ưu tiên**: 🟡 TRUNG (Recommended products)  
**Effort**: 2-3 giờ

### 🔍 Để tìm Mock Data:
```bash
Tìm: "recommended" hoặc "suggested" hoặc sách names như "Rừng Na Uy", "Thiên Tài"
```

### ✅ Checklist Cập nhật:

- [ ] **Bước 1**: Đọc file Order_Success.html
- [ ] **Bước 2**: Tìm section "Recommended for You" với hardcoded books
- [ ] **Bước 3**: Xoá mock recommendation products
- [ ] **Bước 4**: Thêm 3 divs:
  ```html
  <div id="recommended-skeleton" class="hidden"><!-- Skeleton grid --></div>
  <div id="recommended-empty" class="hidden"><!-- Empty state --></div>
  <div id="recommended-live" class="grid grid-cols-2 md:grid-cols-4 gap-4"></div>
  ```
- [ ] **Bước 5**: Import ui-enhancements script
- [ ] **Bước 6**: JavaScript để fetch recommended books:
  ```javascript
  const recommendations = await ApiService.Book.getRecommended(userId);
  renderRecommendations(recommendations, container);
  ```
- [ ] **Bước 7**: Test trên desktop
- [ ] **Bước 8**: Test trên mobile

### 📋 Template Code Pattern:

```html
<!-- PHẦN RECOMMENDED -->
<section class="mt-8">
  <h2 class="text-2xl font-bold mb-6">Sách Được Gợi Ý Cho Bạn</h2>
  
  <!-- Skeleton loading -->
  <div id="recommended-skeleton" class="grid grid-cols-2 md:grid-cols-4 gap-4 hidden">
    <div class="skeleton h-48 rounded"></div>
    <div class="skeleton h-48 rounded"></div>
    <div class="skeleton h-48 rounded"></div>
    <div class="skeleton h-48 rounded"></div>
  </div>
  
  <!-- Real recommendations -->
  <div id="recommended-live" class="grid grid-cols-2 md:grid-cols-4 gap-4"></div>
</section>

<script>
  const fetchRecommendations = async () => {
    const skeleton = document.getElementById('recommended-skeleton');
    const container = document.getElementById('recommended-live');
    
    skeleton.classList.remove('hidden');
    
    try {
      const books = await ApiService.Book.getRecommended(10);
      renderBookGrid(books, container);
    } catch (error) {
      UIEnhancements.ToastService.error('Không thể tải sách gợi ý');
    } finally {
      skeleton.classList.add('hidden');
    }
  };
  
  fetchRecommendations();
</script>
```

---

## 📝 TEMPLATE 3: Flash_Sale.html (hoặc Discovery_Page.html)

**Trạng thái**: ⏳ Chưa bắt đầu  
**Độ ưu tiên**: 🟡 TRUNG (Sale countdown + mock items)  
**Effort**: 2-3 giờ

### 🔍 Để tìm Mock Data:
```bash
Tìm: "flash-sale" hoặc "countdown" hoặc "discount" + giá hardcoded
```

### ✅ Checklist Cập nhật:

- [ ] **Bước 1**: Đọc file Flash_Sale.html
- [ ] **Bước 2**: Tìm section "Flash Sale" với hardcoded products
- [ ] **Bước 3**: Xoá mock sale items
- [ ] **Bước 4**: Thêm 3 divs:
  ```html
  <div id="sale-skeleton" class="hidden"><!-- Skeleton grid --></div>
  <div id="sale-empty" class="hidden"><!-- No sales message --></div>
  <div id="sale-items-live" class="grid grid-cols-2 md:grid-cols-5 gap-4"></div>
  ```
- [ ] **Bước 5**: Import ui-enhancements script
- [ ] **Bước 6**: JavaScript để:
  - Fetch real flash sale items từ API
  - Render products
  - Update prices với AnimateNumber
  - Show countdown timer
- [ ] **Bước 7**: Test countdown trên slow network
- [ ] **Bước 8**: Test trên mobile

### 📋 Template Code Pattern:

```html
<!-- FLASH SALE SECTION -->
<section class="bg-gradient-to-r from-red-500 to-orange-500 text-white py-8 px-6 rounded-xl mb-8">
  <div class="flex justify-between items-center mb-6">
    <h2 class="text-3xl font-black">⚡ FLASH SALE</h2>
    <div id="countdown" class="text-2xl font-bold">00:59:45</div>
  </div>
  
  <!-- Skeleton loading -->
  <div id="sale-skeleton" class="grid grid-cols-2 md:grid-cols-5 gap-4 hidden">
    <div class="skeleton h-56 rounded bg-white/20"></div>
    <div class="skeleton h-56 rounded bg-white/20"></div>
    <div class="skeleton h-56 rounded bg-white/20"></div>
    <div class="skeleton h-56 rounded bg-white/20"></div>
    <div class="skeleton h-56 rounded bg-white/20"></div>
  </div>
  
  <!-- Real sale items -->
  <div id="sale-items-live" class="grid grid-cols-2 md:grid-cols-5 gap-4"></div>
</section>

<script>
  const fetchFlashSale = async () => {
    const skeleton = document.getElementById('sale-skeleton');
    const container = document.getElementById('sale-items-live');
    
    skeleton.classList.remove('hidden');
    
    try {
      const saleItems = await ApiService.Book.getFlashSale();
      
      saleItems.forEach(item => {
        // Animate price changes
        UIEnhancements.AnimateNumber(
          document.getElementById(`sale-price-${item.id}`),
          item.salePrice,
          300,
          (v) => 'đ' + v.toLocaleString('vi-VN')
        );
      });
      
      renderFlashSaleItems(saleItems, container);
      UIEnhancements.ToastService.info('Flash sale đã tải');
      
    } catch (error) {
      UIEnhancements.ToastService.error('Lỗi tải flash sale');
    } finally {
      skeleton.classList.add('hidden');
    }
  };
  
  // Countdown timer
  const startCountdown = () => {
    let timeLeft = 3600; // 1 hour
    setInterval(() => {
      const hours = Math.floor(timeLeft / 3600);
      const mins = Math.floor((timeLeft % 3600) / 60);
      const secs = timeLeft % 60;
      
      document.getElementById('countdown').textContent = 
        `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
      
      timeLeft--;
    }, 1000);
  };
  
  fetchFlashSale();
  startCountdown();
</script>
```

---

## 📊 TỔNG HỢP EFFORT

| File | Mock Data | Loại | Giờ | Ngày |
|------|-----------|------|-----|------|
| Checkout_Page.html | Cart items | Cập nhật | 2-3h | 1 ngày |
| Order_Success.html | Recommended | Cập nhật | 2-3h | 1 ngày |
| Flash_Sale.html | Sale items | Cập nhật | 2-3h | 1 ngày |
| **TỔNG** | - | - | **6-9h** | **3 ngày** |

**Timeline**: 1 sprint (3-5 ngày)

---

## ✅ HOÀN THÀNH 1 FILE = 1 STEP

### Khi Hoàn thành Checkout_Page.html:
```
✅ Checkout page không có mock data
✅ Skeleton loaders hoạt động
✅ Toast notifications hoạt động
✅ Real data từ API hiển thị
✅ Test pass trên desktop
✅ Test pass trên mobile
```

### Khi Hoàn thành Order_Success.html:
```
✅ Recommended products từ API
✅ Real order receipt data
✅ Toast notifications
✅ Skeleton loaders
✅ Empty state (nếu không có recommendations)
```

### Khi Hoàn thành Flash_Sale.html:
```
✅ Real flash sale items từ API
✅ Countdown timer hoạt động
✅ Animated price updates
✅ Skeleton loaders
✅ Toast on load/error
```

---

## 🔗 DEPENDENCIES

Tất cả 3 files cần:
- ✅ `ui-enhancements.js` (đã tạo)
- ✅ `api-service.js` (đã có)
- ✅ Tailwind CSS classes (đã có)
- ✅ Backend APIs (đã hoạt động)

**Không cần**: 
- ❌ Thay đổi API
- ❌ Thay đổi database
- ❌ Cài dependencies mới

---

## 🚀 TIẾP THEO

**Sau khi hoàn thành 3 file này**:
1. Deploy lên staging environment
2. QA test toàn bộ flow
3. Performance testing (slow network)
4. Mobile testing (iOS + Android)
5. Production release

**Rồi bắt đầu**:
- Sprint 1 Real-time Chat System (8-12 tuần tiếp)

---

**Status**: Ready to Deploy | **Version**: 1.0 | **Updated**: May 13, 2026

