/**
 * 📱 API Service Module - Bookom Bookstore
 * Cung cấp các hàm tiện ích để gọi API từ Frontend
 * 
 * Usage:
 *   <script src="api-service.js"></script>
 *   const auth = ApiService.getAuth();
 *   const books = await ApiService.searchBooks('keyword');
 */

const ApiService = (() => {
    const API_BASE = '/api';

    // ==========================================
    // 1. UTILITY FUNCTIONS
    // ==========================================

    /**
     * Lấy thông tin xác thực từ localStorage
     */
    const getAuth = () => {
        return {
            userId: localStorage.getItem('userId'),
            token: localStorage.getItem('accessToken'),
            role: localStorage.getItem('userRole')
        };
    };

    /**
     * Tạo header cho HTTP request
     */
    const getHeaders = () => {
        const { userId, token } = getAuth();
        const headers = {
            'Content-Type': 'application/json',
            'X-User-Id': userId || ''
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    };

    /**
     * Format tiền tệ VND
     */
    const formatVND = (value) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND',
            maximumFractionDigits: 0
        }).format(value || 0);
    };

    // ==========================================
    // 2. AUTHENTICATION APIs
    // ==========================================

    const Auth = {
        /**
         * Yêu cầu OTP
         * @param {string} email - Email người dùng
         */
        requestOtp: async (email) => {
            const response = await fetch(`${API_BASE}/auth/otp/request`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email })
            });
            return response.json();
        },

        /**
         * Xác minh OTP
         * @param {string} email
         * @param {string} otp
         */
        verifyOtp: async (email, otp) => {
            const response = await fetch(`${API_BASE}/auth/otp/verify`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, otp })
            });
            return response.json();
        },

        /**
         * Đăng ký tài khoản
         */
        register: async (username, password, avatarUrl, favoriteCategoryIds) => {
            const response = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username,
                    password,
                    avatarUrl,
                    favoriteCategoryIds
                })
            });
            return response.json();
        },

        /**
         * Đăng nhập (JWT)
         * @returns {Object} { tokenType, accessToken, userId, role }
         */
        loginJwt: async (username, password) => {
            const response = await fetch(`${API_BASE}/auth/login-jwt`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            if (!response.ok) throw new Error('Login failed');
            return response.json();
        },

        /**
         * Lấy thông tin profile
         */
        getProfile: async (userId) => {
            const response = await fetch(`${API_BASE}/auth/profile/${userId}`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Cập nhật profile
         */
        updateProfile: async (userId, data) => {
            const response = await fetch(`${API_BASE}/auth/profile/${userId}`, {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify(data)
            });
            return response.json();
        }
    };

    // ==========================================
    // 3. BOOK APIs
    // ==========================================

    const Book = {
        /**
         * Tìm kiếm sách APPROVED cho BUYER
         */
        search: async (query = '', categoryId = null, page = 0, size = 20) => {
            const params = new URLSearchParams({
                q: query,
                page: page,
                size: size
            });
            if (categoryId) params.append('categoryId', categoryId);

            const response = await fetch(`${API_BASE}/books/search?${params}`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Lấy danh sách sách của SELLER (bao gồm PENDING, APPROVED, REJECTED)
         * Dùng cho trang Inventory Management (S03)
         */
        getSellerBooks: async (query = '', categoryId = null, page = 0, size = 500) => {
            const params = new URLSearchParams({
                q: query,
                page: page,
                size: size
            });
            if (categoryId) params.append('categoryId', categoryId);

            const response = await fetch(`${API_BASE}/books/seller/me?${params}`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Lấy chi tiết sách
         */
        getById: async (bookId) => {
            const response = await fetch(`${API_BASE}/books/${bookId}`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Tạo sách mới (seller)
         */
        create: async (bookData) => {
            const response = await fetch(`${API_BASE}/books/seller`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(bookData)
            });
            return response.json();
        },

        /**
         * Cập nhật sách
         */
        update: async (bookId, bookData) => {
            const response = await fetch(`${API_BASE}/books/seller/${bookId}`, {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify(bookData)
            });
            return response.json();
        },
        uploadCover: async (bookId, formData) => {
                    // CẦN LƯU Ý: Khi dùng fetch với FormData, KHÔNG set header Content-Type.
                    // Trình duyệt sẽ tự động set 'multipart/form-data' kèm theo Boundary (ranh giới file).

                    const { userId, token } = getAuth();
                    const headers = {
                        'X-User-Id': userId || ''
                    };
                    if (token) {
                        headers['Authorization'] = `Bearer ${token}`;
                    }

                    const response = await fetch(`${API_BASE}/books/seller/${bookId}/upload-cover`, {
                        method: 'POST',
                        headers: headers, // Dùng bộ header riêng, KHÔNG dùng getHeaders() vì cái đó đang set cứng application/json
                        body: formData
                    });

                    // Nếu Backend trả về text (đường dẫn link) thay vì JSON, thì return dạng text
                    if (!response.ok) throw new Error('Upload ảnh thất bại');

                    // Xử lý cẩn thận: nếu response là text thì lấy text, json thì lấy json
                    const contentType = response.headers.get("content-type");
                    if (contentType && contentType.indexOf("application/json") !== -1) {
                        return response.json();
                    } else {
                        return response.text();
                    }
                },
        /**
         * Xóa sách
         */
        delete: async (bookId) => {
            const response = await fetch(`${API_BASE}/books/seller/${bookId}`, {
                method: 'DELETE',
                headers: getHeaders()
            });
            return response.json();
        }
    };

    // ==========================================
    // 4. CART APIs
    // ==========================================

    const Cart = {
        /**
         * Lấy giỏ hàng
         */
        get: async (buyerId = null) => {
            const id = buyerId || getAuth().userId;
            const response = await fetch(`${API_BASE}/carts/buyer/${id}`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Thêm item vào giỏ
         */
        addItem: async (buyerId = null, itemData) => {
            const id = buyerId || getAuth().userId;
            const response = await fetch(`${API_BASE}/carts/buyer/${id}/items`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(itemData)
            });
            return response.json();
        },

        /**
         * Cập nhật số lượng item
         */
        updateItem: async (buyerId = null, itemId, quantity) => {
            const id = buyerId || getAuth().userId;
            const response = await fetch(
                `${API_BASE}/carts/buyer/${id}/items/${itemId}?quantity=${quantity}`,
                {
                    method: 'PATCH',
                    headers: getHeaders()
                }
            );
            return response.json();
        },

        /**
         * Xóa item
         */
        removeItem: async (buyerId = null, itemId) => {
            const id = buyerId || getAuth().userId;
            const response = await fetch(
                `${API_BASE}/carts/buyer/${id}/items/${itemId}`,
                {
                    method: 'DELETE',
                    headers: getHeaders()
                }
            );
            return response.json();
        }
    };

    // ==========================================
    // 5. ORDER APIs
    // ==========================================

    const Order = {
        /**
         * Checkout từ giỏ hàng
         */
        checkout: async (shippingAddress) => {
            const response = await fetch(`${API_BASE}/orders/me/checkout`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ shippingAddress })
            });
            return response.json();
        },

        /**
         * Lấy danh sách đơn hàng của buyer
         */
        getBuyerOrders: async () => {
            const response = await fetch(`${API_BASE}/orders/me`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Lấy chi tiết đơn hàng
         */
        getDetail: async (orderId) => {
            const response = await fetch(`${API_BASE}/orders/me/${orderId}`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Lấy sub-orders của seller
         */
        getSellerOrders: async (sellerId = null) => {
            const url = sellerId
                ? `${API_BASE}/orders/seller/${sellerId}/sub-orders`
                : `${API_BASE}/orders/seller/me/sub-orders`;
            const response = await fetch(url, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Cập nhật trạng thái sub-order
         */
        updateSubOrderStatus: async (subOrderId, status) => {
            const response = await fetch(
                `${API_BASE}/orders/sub-orders/${subOrderId}/status?status=${status}`,
                {
                    method: 'PATCH',
                    headers: getHeaders()
                }
            );
            return response.json();
        }
    };

    // ==========================================
    // 6. SELLER SHOP APIs
    // ==========================================

    const SellerShop = {
        /**
         * Lấy thông tin shop của seller
         */
        getMyShop: async () => {
            const response = await fetch(`${API_BASE}/seller/me/shop`, {
                headers: getHeaders()
            });
            return response.json();
        },

        /**
         * Tạo shop mới
         */
        create: async (shopData) => {
            const response = await fetch(`${API_BASE}/seller/me/shop`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(shopData)
            });
            return response.json();
        },

        /**
         * Cập nhật shop
         */
        update: async (shopData) => {
            const response = await fetch(`${API_BASE}/seller/me/shop`, {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify(shopData)
            });
            return response.json();
        },

        /**
         * Lấy thông tin shop công khai
         */
        getPublicShop: async (slug) => {
            const response = await fetch(`${API_BASE}/shops/${slug}`, {
                headers: getHeaders()
            });
            return response.json();
        }
    };

    // ==========================================
    // 7. CATEGORY APIs
    // ==========================================

    const Category = {
        /**
         * Lấy danh sách category
         */
        getAll: async () => {
            const response = await fetch(`${API_BASE}/categories`, {
                headers: getHeaders()
            });
            return response.json();
        }
    };

    // ==========================================
    // PUBLIC API
    // ==========================================

    return {
        // Utility
        getAuth,
        getHeaders,
        formatVND,

        // API Groups
        Auth,
        Book,
        Cart,
        Order,
        SellerShop,
        Category,

        // Helper: Kiểm tra role
        isAuthenticated: () => !!getAuth().userId,
        isSeller: () => getAuth().role === 'SELLER',
        isBuyer: () => getAuth().role === 'BUYER',
        isAdmin: () => getAuth().role === 'ADMIN',

        // Helper: Login store
        storeAuth: (authData) => {
            localStorage.setItem('userId', authData.userId);
            localStorage.setItem('accessToken', authData.accessToken);
            localStorage.setItem('userRole', authData.role);
        },

        // Helper: Logout
        logout: () => {
            localStorage.removeItem('userId');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('userRole');
            window.location.href = '/';
        }
    };
})();

// ==========================================
// Export for Browser
// ==========================================
window.ApiService = ApiService;
