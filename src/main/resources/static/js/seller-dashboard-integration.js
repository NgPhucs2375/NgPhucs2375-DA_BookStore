/**
 * 🏪 Seller Dashboard Integration
 * File: seller-dashboard-integration.js
 * 
 * Để sử dụng, thêm vào Seller_Dashboard.html:
 * <script src="/js/api-service.js"></script>
 * <script src="/js/seller-dashboard-integration.js"></script>
 */

(function () {
    if (!ApiService.isAuthenticated()) {
        alert('Vui lòng đăng nhập để truy cập');
        window.location.href = '/';
        return;
    }

    if (!ApiService.isSeller()) {
        alert('Chỉ seller mới có quyền truy cập trang này');
        window.location.href = '/';
        return;
    }

    const sellerShopNameEl = document.getElementById('seller-shop-name');
    const sellerRevenueEl = document.getElementById('seller-metric-revenue');
    const sellerCompletedEl = document.getElementById('seller-metric-completed');
    const sellerAovEl = document.getElementById('seller-metric-aov');
    const sellerRateEl = document.getElementById('seller-metric-rate');
    const sellerUnitsEl = document.getElementById('seller-metric-units');
    const refreshButtonEl = document.getElementById('seller-refresh-btn');
    const periodFilterEl = document.getElementById('seller-period-filter');

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    };

    const loadShopName = async () => {
        try {
            const shop = await ApiService.SellerShop.getMyShop();
            if (sellerShopNameEl) {
                sellerShopNameEl.textContent = shop && shop.shopName ? shop.shopName : 'Chưa có tên shop';
            }
        } catch (error) {
            if (sellerShopNameEl) {
                sellerShopNameEl.textContent = 'Seller dashboard';
            }
        }
    };

    const renderSummary = (analytics) => {
        if (sellerRevenueEl) sellerRevenueEl.textContent = ApiService.formatVND(analytics.totalRevenue || 0);
        if (sellerCompletedEl) sellerCompletedEl.textContent = String(analytics.completedOrders || 0);
        if (sellerAovEl) sellerAovEl.textContent = ApiService.formatVND(analytics.averageOrderValue || 0);
        if (sellerRateEl) sellerRateEl.textContent = `${Math.round(analytics.completionRate || 0)}%`;
        if (sellerUnitsEl) sellerUnitsEl.textContent = String(analytics.soldUnits || 0);
    };

    const loadAnalytics = async () => {
        const days = periodFilterEl ? Number(periodFilterEl.value || 30) : 30;
        const analytics = await ApiService.Order.getSellerAnalytics(days);
        renderSummary(analytics || {});
    };

    const loadDashboardData = async () => {
        try {
            await Promise.all([loadShopName(), loadAnalytics()]);
        } catch (error) {
            console.error('❌ Lỗi tải dashboard seller:', error);
            alert('❌ Không thể tải dữ liệu dashboard');
        }
    };

    if (refreshButtonEl) {
        refreshButtonEl.addEventListener('click', function () {
            loadDashboardData();
        });
    }

    if (periodFilterEl) {
        periodFilterEl.addEventListener('change', function () {
            loadDashboardData();
        });
    }

    window.refreshDashboard = loadDashboardData;

    console.log('📊 Seller summary dashboard loaded successfully');
    loadDashboardData();
})();
