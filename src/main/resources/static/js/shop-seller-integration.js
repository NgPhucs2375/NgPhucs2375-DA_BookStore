/**
 * 🏬 Shop Seller Management Integration
 * File: shop-seller-integration.js
 * 
 * Để sử dụng, thêm vào Shop_Seller.html:
 * <script src="/js/api-service.js"></script>
 * <script src="/js/shop-seller-integration.js"></script>
 */

(function () {
    // ==========================================
    // 1. CHECK AUTHENTICATION
    // ==========================================

    if (!ApiService.isAuthenticated() || !ApiService.isSeller()) {
        alert('Chỉ seller mới có quyền truy cập');
        window.location.href = '/';
        return;
    }

    // ==========================================
    // 2. FORM ELEMENTS REFERENCES
    // ==========================================

    const shopNameInput = document.getElementById('shop-name') || 
                         document.querySelector('input[name="shopName"]');
    const shopDescriptionInput = document.getElementById('shop-description') || 
                                document.querySelector('textarea[name="description"]');
    const shopPhoneInput = document.getElementById('shop-phone') || 
                         document.querySelector('input[name="phoneNumber"]');
    const shopStatusDisplay = document.getElementById('shop-status') || 
                             document.querySelector('[data-shop-status]');
    const shopSlugDisplay = document.getElementById('shop-slug') || 
                           document.querySelector('[data-shop-slug]');

    const saveShopBtn = document.getElementById('save-shop-btn') || 
                       document.querySelector('button[data-action="save-shop"]');
    const editShopBtn = document.getElementById('edit-shop-btn') || 
                       document.querySelector('button[data-action="edit-shop"]');
    const cancelEditBtn = document.getElementById('cancel-edit-btn') || 
                         document.querySelector('button[data-action="cancel-edit"]');

    // ==========================================
    // 3. STATE MANAGEMENT
    // ==========================================

    let currentShopData = null;
    let isEditMode = false;

    // ==========================================
    // 4. UTILITY FUNCTIONS
    // ==========================================

    const setEditMode = (mode) => {
        isEditMode = mode;

        // Toggle input states
        if (shopNameInput) shopNameInput.disabled = !mode;
        if (shopDescriptionInput) shopDescriptionInput.disabled = !mode;
        if (shopPhoneInput) shopPhoneInput.disabled = !mode;

        // Toggle button visibility
        if (saveShopBtn) saveShopBtn.style.display = mode ? 'inline-block' : 'none';
        if (editShopBtn) editShopBtn.style.display = mode ? 'none' : 'inline-block';
        if (cancelEditBtn) cancelEditBtn.style.display = mode ? 'inline-block' : 'none';

        // Visual feedback
        const inputClass = 'opacity-100 bg-white cursor-text';
        const disabledClass = 'opacity-60 bg-gray-100 cursor-not-allowed';
        [shopNameInput, shopDescriptionInput, shopPhoneInput].forEach(el => {
            if (el) {
                el.classList.toggle(inputClass, mode);
                el.classList.toggle(disabledClass, !mode);
            }
        });
    };

    const showSpinner = (element) => {
        if (!element) return;
        const originalText = element.textContent;
        element.innerHTML = '⏳ Đang xử lý...';
        element.disabled = true;
        return () => {
            element.textContent = originalText;
            element.disabled = false;
        };
    };

    // ==========================================
    // 5. FETCH & RENDER FUNCTIONS
    // ==========================================

    const loadShopInfo = async () => {
        try {
            const shop = await ApiService.SellerShop.getMyShop();
            currentShopData = shop;

            // Populate form
            if (shopNameInput) shopNameInput.value = shop.shopName || '';
            if (shopDescriptionInput) shopDescriptionInput.value = shop.description || '';
            if (shopPhoneInput) shopPhoneInput.value = shop.phoneNumber || '';
            if (shopStatusDisplay) shopStatusDisplay.textContent = shop.status || 'N/A';
            if (shopSlugDisplay) shopSlugDisplay.textContent = shop.slug || 'N/A';

            setEditMode(false);
            console.log('✓ Thông tin shop tải thành công');
            return shop;

        } catch (error) {
            console.error('❌ Lỗi tải shop:', error);
            if (error.status === 404) {
                // Shop chưa tồn tại, cho phép tạo mới
                alert('Bạn chưa có shop. Hãy tạo shop mới.');
                setEditMode(true);
                currentShopData = null;
            } else {
                alert('❌ Lỗi tải thông tin shop');
            }
            return null;
        }
    };

    // ==========================================
    // 6. ACTION HANDLERS
    // ==========================================

    window.enterEditMode = () => {
        setEditMode(true);
    };

    window.cancelEdit = () => {
        if (currentShopData) {
            // Restore original values
            if (shopNameInput) shopNameInput.value = currentShopData.shopName || '';
            if (shopDescriptionInput) shopDescriptionInput.value = currentShopData.description || '';
            if (shopPhoneInput) shopPhoneInput.value = currentShopData.phoneNumber || '';
        }
        setEditMode(false);
    };

    window.saveShop = async () => {
        try {
            // Validate input
            const shopName = (shopNameInput?.value || '').trim();
            const description = (shopDescriptionInput?.value || '').trim();
            const phoneNumber = (shopPhoneInput?.value || '').trim();

            if (!shopName) {
                alert('❌ Tên shop không được để trống');
                shopNameInput?.focus();
                return;
            }

            if (shopName.length < 3) {
                alert('❌ Tên shop phải từ 3 ký tự trở lên');
                return;
            }

            if (phoneNumber && !/^\d{10,11}$/.test(phoneNumber.replace(/\s/g, ''))) {
                alert('❌ Số điện thoại không hợp lệ');
                return;
            }

            // Show loading
            const revert = showSpinner(saveShopBtn);

            // Prepare data
            const shopData = {
                shopName,
                description,
                phoneNumber
            };

            // Call API
            let response;
            if (currentShopData?.id) {
                // Update existing shop
                response = await ApiService.SellerShop.update(shopData);
            } else {
                // Create new shop
                response = await ApiService.SellerShop.create(shopData);
            }

            revert();

            // Success
            currentShopData = response;
            alert('✓ Lưu thông tin shop thành công!');
            setEditMode(false);

            // Reload data to show updated info
            await loadShopInfo();

        } catch (error) {
            console.error('❌ Lỗi lưu shop:', error);
            const revert = showSpinner(saveShopBtn);
            revert();

            const message = error.response?.data?.message || error.message || 'Thử lại sau';
            alert('❌ Lỗi: ' + message);
        }
    };

    // ==========================================
    // 7. EVENT LISTENERS
    // ==========================================

    if (editShopBtn) {
        editShopBtn.addEventListener('click', window.enterEditMode);
    }

    if (cancelEditBtn) {
        cancelEditBtn.addEventListener('click', window.cancelEdit);
    }

    if (saveShopBtn) {
        saveShopBtn.addEventListener('click', window.saveShop);
    }

    // Allow Enter key to save
    [shopNameInput, shopDescriptionInput, shopPhoneInput].forEach(el => {
        if (el) {
            el.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && isEditMode && !el.matches('textarea')) {
                    window.saveShop();
                }
            });
        }
    });

    // ==========================================
    // 8. INITIALIZATION
    // ==========================================

    console.log('🏬 Shop Seller Management loaded');
    loadShopInfo();

})();
