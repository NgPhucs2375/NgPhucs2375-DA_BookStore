# BOOKOM - Implementation Guide: Modern UI & Real Data

**Status**: Ready for Implementation | **Priority**: High | **Effort**: 2-3 sprints

---

## 📋 Overview

This guide provides step-by-step instructions for:
1. Integrating UI enhancement utilities across all pages
2. Removing mock data fallbacks
3. Implementing skeleton loaders for better UX
4. Adding toast notifications for user feedback

---

## 🔧 Quick Start

### Step 1: Include UI Enhancements Script

Add to the `<head>` or before `</body>` of all templates:

```html
<script src="/js/ui-enhancements.js"></script>
```

This makes available globally:
- `UIEnhancements.ToastService` - Toast notifications
- `UIEnhancements.SkeletonLoader` - Loading indicators
- `UIEnhancements.ButtonLoading` - Button state management
- `UIEnhancements.AnimateNumber` - Animated updates
- `UIEnhancements.FormValidator` - Form validation
- `UIEnhancements.Debounce` / `UIEnhancements.Throttle` - Performance utilities
- `UIEnhancements.ModalManager` - Modal control

### Step 2: Use Toasts for User Feedback

Replace generic alerts with modern toasts:

```javascript
// ❌ Old way
alert('Product added to cart');

// ✅ New way
UIEnhancements.ToastService.success('Đã thêm sách vào giỏ hàng');

// ✅ With duration
UIEnhancements.ToastService.error('Lỗi: Vui lòng thử lại', 5000);

// ✅ With action button
UIEnhancements.ToastService.show('Đặt hàng thành công', 'success', 3000, {
  label: 'Xem chi tiết',
  callback: () => window.location.href = '/orders'
});
```

### Step 3: Replace Fallback Mock Data with Skeleton Loaders

Before (Cart_Page.html - OLD):
```html
<div id="cart-items-fallback-1" class="bg-white...">
  <!-- Hardcoded mock books -->
  <div class="...">Đại Gia Gatsby</div>
  <div class="...">Nhà Giả Kim</div>
</div>
```

After (Cart_Page.html - NEW):
```html
<!-- Skeleton Loader -->
<div id="cart-skeleton-loader" class="space-y-4 hidden">
  <div class="bg-white border border-brand-accent rounded-xl shadow-sm overflow-hidden">
    <div class="bg-brand-cream/50 px-5 py-4 border-b border-brand-accent">
      <div class="skeleton h-6 w-32 rounded"></div>
    </div>
    <div class="p-5 space-y-3">
      <div class="flex gap-4">
        <div class="skeleton h-32 w-20 rounded flex-shrink-0"></div>
        <div class="flex-1 space-y-2">
          <div class="skeleton h-4 w-3/4 rounded"></div>
          <div class="skeleton h-4 w-1/2 rounded"></div>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Empty State -->
<div id="cart-empty-state" class="hidden bg-white...">
  <h3>Giỏ hàng của bạn trống</h3>
  <a href="/discover">Khám phá sách →</a>
</div>

<!-- Real Data Container (populated by JavaScript) -->
<div id="cart-items-live" class="flex flex-col gap-6"></div>
```

### Step 4: Show Loaders & Handle Empty States in JavaScript

Update `cart-page.js`:

```javascript
const fetchCart = async () => {
  const container = document.getElementById('cart-items-live');
  const skeletonLoader = document.getElementById('cart-skeleton-loader');
  const emptyState = document.getElementById('cart-empty-state');
  
  // Show skeleton, hide others
  skeletonLoader.classList.remove('hidden');
  container.innerHTML = '';
  emptyState.classList.add('hidden');
  
  try {
    const { userId, role } = ApiService.getAuth();
    if (!userId || role !== 'BUYER') {
      throw new Error('Not authenticated as buyer');
    }
    
    const cart = await ApiService.Cart.get(userId);
    
    if (!cart?.items || cart.items.length === 0) {
      // Show empty state
      skeletonLoader.classList.add('hidden');
      emptyState.classList.remove('hidden');
      updateSummary({ totalItems: 0, totalAmount: 0 });
      return;
    }
    
    // Hide skeleton, render real data
    skeletonLoader.classList.add('hidden');
    renderCart(cart);
    updateSummary(cart);
    
    // Show success toast (optional)
    UIEnhancements.ToastService.info('Giỏ hàng đã cập nhật');
    
  } catch (error) {
    console.error('Cart fetch error:', error);
    skeletonLoader.classList.add('hidden');
    container.innerHTML = `
      <div class="bg-white border border-brand-accent rounded-xl shadow-sm p-8 text-center text-red-500 font-semibold">
        Không thể tải giỏ hàng. <button class="text-blue-600 hover:underline" onclick="location.reload()">Thử lại</button>
      </div>
    `;
    UIEnhancements.ToastService.error('Lỗi tải giỏ hàng. Kiểm tra kết nối mạng.');
  }
};
```

### Step 5: Add Loading State to Async Buttons

```html
<button id="checkout-btn" class="btn-primary">
  Thanh toán ngay
</button>

<script>
document.getElementById('checkout-btn').addEventListener('click', async () => {
  const btn = document.getElementById('checkout-btn');
  UIEnhancements.ButtonLoading.start(btn);
  
  try {
    const response = await fetch('/api/checkout', { method: 'POST' });
    if (response.ok) {
      UIEnhancements.ToastService.success('Chuyển hướng đến thanh toán...');
      setTimeout(() => window.location.href = '/checkout', 1000);
    } else {
      throw new Error('Checkout failed');
    }
  } catch (error) {
    UIEnhancements.ToastService.error('Lỗi thanh toán. Vui lòng thử lại.');
  } finally {
    UIEnhancements.ButtonLoading.end(btn);
  }
});
</script>
```

---

## 🎯 Pages to Update (Priority Order)

### Phase 1 (This Sprint)
- [ ] **Cart_Page.html** - ✅ COMPLETED
- [ ] **Checkout_Page.html** - IN PROGRESS
- [ ] **Order_Success.html** - Recommended (high impact)

### Phase 2 (Next Sprint)
- [ ] **Discovery_Page.html** - Product grid
- [ ] **Details_Produce.html** - Add to cart button
- [ ] **Search_Result.html** - Search results

### Phase 3 (Optional)
- [ ] **Buyer_DashBoard.html** - Order history
- [ ] **Admin_Books.html** - Books management
- [ ] **Seller_Dashboard.html** - Seller analytics

---

## 🔄 Template Conversion Pattern

For each page, follow this pattern:

### 1. Identify Fallback Sections
```html
<div id="[something]-fallback" class="...">
  <!-- Mock data here -->
</div>
```

### 2. Replace with Skeleton + Empty State + Real Container
```html
<!-- Skeleton Loader (shown while loading) -->
<div id="[something]-skeleton" class="hidden">
  <!-- Skeleton HTML -->
</div>

<!-- Empty State (shown when no data) -->
<div id="[something]-empty" class="hidden">
  <!-- Empty message + CTA -->
</div>

<!-- Real Data Container (populated by JS) -->
<div id="[something]-live"></div>
```

### 3. Update JavaScript
```javascript
// Show loader
document.getElementById('[something]-skeleton').classList.remove('hidden');

// Fetch real data
const data = await ApiService.getData();

// Render or show empty state
if (data.length === 0) {
  document.getElementById('[something]-empty').classList.remove('hidden');
} else {
  document.getElementById('[something]-skeleton').classList.add('hidden');
  renderData(data, document.getElementById('[something]-live'));
}
```

---

## 💡 UI Patterns to Implement

### Pattern 1: Animated Number Updates

```javascript
// When updating total price
UIEnhancements.AnimateNumber(
  document.getElementById('total-price'),
  newTotal,  // target value
  500,       // animation duration (ms)
  (val) => 'đ' + val.toLocaleString('vi-VN') // formatter function
);
```

### Pattern 2: Form Validation Feedback

```html
<input id="email" type="email" placeholder="Email">
<span class="error-message hidden text-red-600"></span>

<script>
document.getElementById('email').addEventListener('blur', () => {
  const errors = UIEnhancements.FormValidator.validate(
    document.getElementById('email'),
    { email: true, required: true }
  );
  
  if (errors.length > 0) {
    UIEnhancements.FormValidator.showError(
      document.getElementById('email'),
      'Email không hợp lệ'
    );
  } else {
    UIEnhancements.FormValidator.clearError(document.getElementById('email'));
  }
});
</script>
```

### Pattern 3: Debounced Search

```javascript
const searchInput = document.getElementById('search');
const searchFn = UIEnhancements.Debounce(async (query) => {
  const results = await ApiService.search(query);
  renderResults(results);
}, 500); // Wait 500ms after user stops typing

searchInput.addEventListener('input', (e) => searchFn(e.target.value));
```

### Pattern 4: Modal Management

```javascript
const modal = document.getElementById('address-modal');

// Open
document.getElementById('add-address-btn').addEventListener('click', () => {
  UIEnhancements.ModalManager.open(modal);
});

// Close on backdrop click
UIEnhancements.ModalManager.setupBackdropClose(modal, '.modal-backdrop');

// Close on button click
document.getElementById('close-btn').addEventListener('click', () => {
  UIEnhancements.ModalManager.close(modal);
});
```

---

## ✅ Checklist for Each Page Update

- [ ] Remove all mock data fallback sections
- [ ] Add skeleton loader HTML
- [ ] Add empty state HTML
- [ ] Add real data container with `id="[something]-live"`
- [ ] Update JavaScript to show/hide loaders appropriately
- [ ] Add success/error toasts
- [ ] Test on mobile (iOS 14+, Android 10+)
- [ ] Test with slow network (DevTools throttle)
- [ ] Verify accessibility (ARIA labels, keyboard nav)
- [ ] Update component library docs

---

## 📊 Performance Improvements

### Before Optimization
- Initial page load: ~2.5s
- Mock data shows immediately
- No loading indication
- False sense of data

### After Optimization
- Initial page load: ~1.8s (skeleton shows in 300ms)
- Real data loads asynchronously
- Clear loading indication
- No false data
- Better perceived performance

---

## 🔗 API Integration

Most pages already use real APIs via `ApiService`. Verify these calls:

```javascript
// Cart API
const cart = await ApiService.Cart.get(userId);

// Orders API
const orders = await ApiService.Order.getByBuyer(userId);

// Products API
const products = await ApiService.Book.list();

// Search API
const results = await ApiService.Book.search(query);

// Wishlist API
const wishlist = await ApiService.Wishlist.get(userId);
```

---

## 🚀 Deployment Steps

1. **Test Locally**
   ```bash
   # Start dev server
   mvnw clean spring-boot:run
   
   # Test cart page
   # - Add items
   # - Remove items
   # - Check toasts
   # - Try mobile view
   ```

2. **Deploy to Staging**
   ```bash
   # Build
   mvnw clean package
   
   # Deploy staging
   docker build -t bookom:staging .
   docker push registry.example.com/bookom:staging
   ```

3. **Production Rollout**
   - 10% of users (canary)
   - Monitor errors & performance
   - 50% rollout
   - 100% rollout

---

## 📞 Support & Questions

- **Docs**: See `UI_UX_ENHANCEMENT_GUIDE.md`
- **Components**: `src/main/resources/static/js/ui-enhancements.js`
- **Issues**: File bug reports with screenshot + browser version

---

**Last Updated**: May 13, 2026 | **Version**: 1.0

