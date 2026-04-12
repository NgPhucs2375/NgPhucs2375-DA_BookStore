/**
 * 👤 Buyer Dashboard Integration
 * File: buyer-dashboard-integration.js
 * 
 * Để sử dụng, thêm vào Buyer_DashBoard.html:
 * <script src="/js/api-service.js"></script>
 * <script src="/js/buyer-dashboard-integration.js"></script>
 */

(function () {
    // ==========================================
    // 1. CHECK AUTHENTICATION
    // ==========================================

    if (!ApiService.isAuthenticated()) {
        alert('Vui lòng đăng nhập');
        window.location.href = '/main/index';
        return;
    }

    if (!ApiService.isBuyer()) {
        alert('Chỉ buyer mới có quyền truy cập');
        window.location.href = '/main/index';
        return;
    }

    // ==========================================
    // 2. ELEMENT REFERENCES
    // ==========================================

    const buyerNameEl = document.getElementById('buyer-name') || 
                       document.querySelector('[data-buyer-name]');
    const buyerEmailEl = document.getElementById('buyer-email') || 
                        document.querySelector('[data-buyer-email]');
    const buyerAvatarEl = document.getElementById('buyer-avatar') || 
                         document.querySelector('[data-buyer-avatar]');

    const ordersListEl = document.getElementById('buyer-orders-list') || 
                        document.querySelector('[data-orders-list]');
    const totalOrdersEl = document.getElementById('total-orders') || 
                         document.querySelector('[data-total-orders]');
    const totalSpendEl = document.getElementById('total-spend') || 
                        document.querySelector('[data-total-spend]');

    const filterAllBtn = document.querySelector('button[data-filter="all"]');
    const filterPendingBtn = document.querySelector('button[data-filter="pending"]');
    const filterDeliveredBtn = document.querySelector('button[data-filter="delivered"]');
    const filterCancelledBtn = document.querySelector('button[data-filter="cancelled"]');

    const searchOrdersInput = document.getElementById('search-orders') || 
                            document.querySelector('input[data-search="orders"]');

    // ==========================================
    // 3. STATE MANAGEMENT
    // ==========================================

    let allOrders = [];
    let currentFilter = 'all';
    let currentSearchQuery = '';

    // ==========================================
    // 4. UTILITY FUNCTIONS
    // ==========================================

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    };

    const getStatusIcon = (status) => {
        const icons = {
            'PENDING': '⏳',
            'CONFIRMED': '✓',
            'PACKED': '📦',
            'SHIPPED': '🚚',
            'DELIVERED': '🏠',
            'CANCELLED': '✗'
        };
        return icons[status] || '?';
    };

    const getStatusLabel = (status) => {
        const labels = {
            'PENDING': 'Chờ xác nhận',
            'CONFIRMED': 'Đã xác nhận',
            'PACKED': 'Đã đóng gói',
            'SHIPPED': 'Đang giao',
            'DELIVERED': 'Đã giao',
            'CANCELLED': 'Đã hủy'
        };
        return labels[status] || status;
    };

    const getStatusBadgeClass = (status) => {
        const classes = {
            'PENDING': 'bg-yellow-100 text-yellow-800',
            'CONFIRMED': 'bg-blue-100 text-blue-800',
            'PACKED': 'bg-purple-100 text-purple-800',
            'SHIPPED': 'bg-cyan-100 text-cyan-800',
            'DELIVERED': 'bg-green-100 text-green-800',
            'CANCELLED': 'bg-red-100 text-red-800'
        };
        return classes[status] || 'bg-gray-100 text-gray-800';
    };

    const filterOrders = () => {
        let filtered = allOrders;

        // Filter by status
        if (currentFilter !== 'all') {
            filtered = filtered.filter(o => {
                if (currentFilter === 'pending') {
                    return ['PENDING', 'CONFIRMED', 'PACKED', 'SHIPPED'].includes(o.status);
                } else if (currentFilter === 'delivered') {
                    return o.status === 'DELIVERED';
                } else if (currentFilter === 'cancelled') {
                    return o.status === 'CANCELLED';
                }
                return true;
            });
        }

        // Filter by search query
        if (currentSearchQuery) {
            const query = currentSearchQuery.toLowerCase();
            filtered = filtered.filter(o => {
                return (
                    String(o.id).includes(query) ||
                    (o.buyerName && o.buyerName.toLowerCase().includes(query))
                );
            });
        }

        return filtered;
    };

    // ==========================================
    // 5. FETCH & RENDER FUNCTIONS
    // ==========================================

    const loadBuyerProfile = async () => {
        try {
            const userId = ApiService.getAuth().userId;
            const profile = await ApiService.Auth.getProfile(userId);

            if (buyerNameEl) buyerNameEl.textContent = profile.username || 'Khách hàng';
            if (buyerEmailEl) buyerEmailEl.textContent = profile.email || 'N/A';
            if (buyerAvatarEl && profile.avatarUrl) {
                buyerAvatarEl.src = profile.avatarUrl;
            }

        } catch (error) {
            console.error('❌ Lỗi tải profile:', error);
        }
    };

    const loadBuyerOrders = async () => {
        try {
            const orders = await ApiService.Order.getBuyerOrders();
            allOrders = Array.isArray(orders) ? orders : [];

            // Update stats
            const totalSpend = allOrders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
            if (totalOrdersEl) totalOrdersEl.textContent = allOrders.length;
            if (totalSpendEl) totalSpendEl.textContent = ApiService.formatVND(totalSpend);

            renderOrdersList();

        } catch (error) {
            console.error('❌ Lỗi tải đơn hàng:', error);
            if (ordersListEl) {
                ordersListEl.innerHTML = `
                    <div class="text-center py-6 text-red-600">
                        ❌ Không thể tải danh sách đơn hàng
                    </div>
                `;
            }
        }
    };

    const renderOrdersList = () => {
        if (!ordersListEl) return;

        const filtered = filterOrders();

        if (filtered.length === 0) {
            ordersListEl.innerHTML = `
                <div class="text-center py-6 text-gray-500">
                    ${currentSearchQuery ? '❌ Không tìm thấy đơn hàng' : '📭 Bạn chưa có đơn hàng nào'}
                </div>
            `;
            return;
        }

        ordersListEl.innerHTML = filtered.map(order => `
            <div class="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition mb-4">
                <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                    <div class="flex-grow">
                        <div class="flex items-center gap-2 mb-2">
                            <span class="text-2xl">${getStatusIcon(order.status)}</span>
                            <div>
                                <p class="font-bold text-lg">Đơn hàng #${order.id}</p>
                                <p class="text-xs text-gray-500">${formatDate(order.createdAt)}</p>
                            </div>
                        </div>
                        <p class="text-sm text-gray-600">
                            ${(order.items || []).length || 1} sản phẩm
                        </p>
                    </div>

                    <div class="flex flex-col md:items-end gap-2">
                        <span class="px-3 py-1 rounded-full text-sm font-bold ${getStatusBadgeClass(order.status)}">
                            ${getStatusLabel(order.status)}
                        </span>
                        <span class="text-xl font-black text-brand-orange">
                            ${ApiService.formatVND(order.totalAmount || 0)}
                        </span>
                    </div>

                    <div class="flex gap-2">
                        <button 
                            onclick="window.viewOrderDetail(${order.id})"
                            class="px-4 py-2 bg-brand-orange text-white rounded-lg hover:bg-brand-dark transition font-bold text-sm"
                        >
                            Xem chi tiết
                        </button>
                        ${order.status === 'PENDING' ? `
                            <button 
                                onclick="window.cancelOrder(${order.id})"
                                class="px-4 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition font-bold text-sm"
                            >
                                Hủy
                            </button>
                        ` : ''}
                    </div>
                </div>
            </div>
        `).join('');
    };

    // ==========================================
    // 6. ACTION HANDLERS
    // ==========================================

    window.viewOrderDetail = (orderId) => {
        window.location.href = `/main/order-details?orderId=${orderId}`;
    };

    window.cancelOrder = async (orderId) => {
        if (!confirm('⚠️ Bạn chắc chắn muốn hủy đơn hàng này?')) {
            return;
        }

        try {
            await ApiService.Order.updateSubOrderStatus(orderId, 'CANCELLED');
            alert('✓ Hủy đơn hàng thành công');
            await loadBuyerOrders();
        } catch (error) {
            alert('❌ Lỗi hủy đơn hàng: ' + error.message);
        }
    };

    // ==========================================
    // 7. FILTER & SEARCH
    // ==========================================

    const updateFilterButtons = () => {
        [filterAllBtn, filterPendingBtn, filterDeliveredBtn, filterCancelledBtn].forEach(btn => {
            if (btn) {
                const filter = btn.getAttribute('data-filter');
                btn.classList.toggle('bg-brand-orange text-white', filter === currentFilter);
                btn.classList.toggle('bg-gray-200 text-gray-800', filter !== currentFilter);
            }
        });
    };

    if (filterAllBtn) filterAllBtn.addEventListener('click', () => {
        currentFilter = 'all';
        updateFilterButtons();
        renderOrdersList();
    });

    if (filterPendingBtn) filterPendingBtn.addEventListener('click', () => {
        currentFilter = 'pending';
        updateFilterButtons();
        renderOrdersList();
    });

    if (filterDeliveredBtn) filterDeliveredBtn.addEventListener('click', () => {
        currentFilter = 'delivered';
        updateFilterButtons();
        renderOrdersList();
    });

    if (filterCancelledBtn) filterCancelledBtn.addEventListener('click', () => {
        currentFilter = 'cancelled';
        updateFilterButtons();
        renderOrdersList();
    });

    if (searchOrdersInput) {
        let searchTimeout;
        searchOrdersInput.addEventListener('input', (e) => {
            currentSearchQuery = e.target.value.trim();
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                renderOrdersList();
            }, 300);
        });
    }

    // ==========================================
    // 8. INITIALIZATION
    // ==========================================

    console.log('👤 Buyer Dashboard loaded');
    Promise.all([loadBuyerProfile(), loadBuyerOrders()]);

})();
