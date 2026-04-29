/**
 * 👤 Buyer Dashboard Integration
 * File: buyer-dashboard-integration.js
 * 
 * Để sử dụng, thêm vào Buyer_DashBoard.html:
 * <script src="/js/api-service.js"></script>
 * <script src="/js/buyer-dashboard-integration.js"></script>
 */

(function () {
    if (!window.ApiService || !ApiService.isAuthenticated()) {
        window.location.href = '/';
        return;
    }

    if (!ApiService.isBuyer()) {
        window.location.href = '/';
        return;
    }

    const headerNameEl = document.getElementById('buyer-header-name');
    const headerCartCountEl = document.getElementById('header-cart-count');
    const sidebarNameEl = document.getElementById('sidebar-name');
    const sidebarXuCountEl = document.getElementById('sidebar-xu-count');
    const greetingEl = document.getElementById('buyer-greeting');
    const ordersCountEl = document.getElementById('buyer-orders-count');
    const favoriteCountEl = document.getElementById('buyer-favorite-count');
    const voucherCountEl = document.getElementById('buyer-voucher-count');
    const recentOrdersEl = document.getElementById('order-list');
    const profileAvatarEl = document.getElementById('profile-avatar-preview');
    const sidebarAvatarEl = document.getElementById('sidebar-avatar');
    const headerAvatarEl = document.getElementById('header-avatar-preview');
    const firstNameInput = document.getElementById('prof_ten');
    const lastNameInput = document.getElementById('prof_ho');
    const emailInput = document.getElementById('prof_email');
    const phoneInput = document.getElementById('prof_phone');

    const defaultAvatar = 'https://i.pravatar.cc/150?img=11';

    const setText = (element, value) => {
        if (element) {
            element.textContent = value;
        }
    };

    const setAvatar = (element, url) => {
        if (element) {
            element.style.backgroundImage = `url('${url || defaultAvatar}')`;
        }
    };

    const toDisplayName = (username) => {
        const raw = String(username || '').split('@')[0].replace(/[._-]+/g, ' ').trim();
        if (!raw) return 'Khách hàng';

        return raw
            .split(/\s+/)
            .filter(Boolean)
            .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    };

    const splitDisplayName = (displayName) => {
        const parts = String(displayName || '').trim().split(/\s+/).filter(Boolean);
        if (parts.length === 0) {
            return { firstName: 'Khách', lastName: 'Hàng' };
        }

        if (parts.length === 1) {
            return { firstName: parts[0], lastName: '' };
        }

        return {
            firstName: parts[parts.length - 1],
            lastName: parts.slice(0, -1).join(' ')
        };
    };

    const formatDate = (value) => {
        if (!value) return '-';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '-';
        return date.toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    };

    const renderRecentOrders = (orders) => {
        if (!recentOrdersEl) return;

        if (!orders.length) {
            recentOrdersEl.innerHTML = `
                <div class="bg-white border border-brand-accent rounded-2xl p-6 shadow-sm text-gray-500">
                    Bạn chưa có đơn hàng nào.
                </div>
            `;
            return;
        }

        recentOrdersEl.innerHTML = orders.slice(0, 3).map((order) => `
            <div class="bg-white border border-brand-accent rounded-2xl shadow-sm overflow-hidden">
                <div class="bg-gray-50 border-b border-brand-accent px-6 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div class="flex items-center gap-3">
                        <span class="bg-brand-orange text-white text-[10px] font-black px-2 py-0.5 rounded uppercase">Đơn hàng</span>
                        <span class="font-bold text-brand-dark text-sm">Mã #${order.id}</span>
                    </div>
                    <div class="flex items-center gap-2 text-sm font-bold text-gray-500 uppercase tracking-wide text-xs">
                        ${formatDate(order.createdAt)}
                    </div>
                </div>
                <div class="px-6 py-5 flex items-start gap-4">
                    <div class="w-20 aspect-[3/4] bg-[#2c3e50] border border-gray-200 shadow-sm flex-shrink-0 flex items-center justify-center text-white text-center font-bold text-[8px] uppercase p-1">BOOKOM</div>
                    <div class="flex flex-col flex-grow">
                        <h4 class="font-bold text-brand-dark text-sm line-clamp-2 mb-1">Đơn hàng của bạn</h4>
                        <span class="text-xs text-gray-500 mb-2">${order.shippingAddress || 'Địa chỉ giao hàng đang cập nhật'}</span>
                        <span class="text-xs font-bold text-brand-dark">x1</span>
                    </div>
                    <div class="flex flex-col items-end gap-1">
                        <span class="text-brand-orange font-bold text-sm">${ApiService.formatVND(order.totalAmount || 0)}</span>
                    </div>
                </div>
            </div>
        `).join('');
    };

    const loadProfile = async () => {
        const userId = ApiService.getAuth().userId;
        const profile = await ApiService.Auth.getProfile(userId);
        const displayName = toDisplayName(profile.username);
        const nameParts = splitDisplayName(displayName);

        setText(headerNameEl, displayName);
        setText(sidebarNameEl, displayName);
        setText(greetingEl, `Xin chào, ${displayName}!`);
        setText(favoriteCountEl, String((profile.favoriteCategoryIds || []).length));
        setText(voucherCountEl, '0');

        if (emailInput) emailInput.value = profile.username || '';
        if (firstNameInput) firstNameInput.value = nameParts.firstName;
        if (lastNameInput) lastNameInput.value = nameParts.lastName;
        if (phoneInput) phoneInput.value = '';

        setAvatar(profileAvatarEl, profile.avatarUrl);
        setAvatar(sidebarAvatarEl, profile.avatarUrl);
        setAvatar(headerAvatarEl, profile.avatarUrl);
    };

    const loadCart = async () => {
        const cart = await ApiService.Cart.get();
        setText(headerCartCountEl, String(cart.totalItems || 0));
        setText(sidebarXuCountEl, '0');
    };

    const loadOrders = async () => {
        const orders = await ApiService.Order.getBuyerOrders();
        const list = Array.isArray(orders) ? orders : [];
        setText(ordersCountEl, String(list.length));
        renderRecentOrders(list);
    };

    Promise.allSettled([loadProfile(), loadCart(), loadOrders()]).then((results) => {
        results.forEach((result) => {
            if (result.status === 'rejected') {
                console.error('Buyer dashboard load failed:', result.reason);
            }
        });
    });
})();
