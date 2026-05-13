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

    /**
     * Parse JSON or text response safely
     */
    const parseResponse = async (response) => {
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return response.json();
        }
        const text = await response.text();
        return text;
    };

    /**
     * Handle API response with error normalization
     */
    const handleResponse = async (response) => {
        const data = await parseResponse(response);
        if (!response.ok) {
            const message = typeof data === 'string' && data.trim()
                ? data
                : 'Request failed';
            const error = new Error(message);
            error.data = data;
            throw error;
        }
        return data;
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
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
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
        search: async (query = '', categoryId = null, page = 0, size = 20, filters = {}) => {
            const params = new URLSearchParams();
            params.set('q', query || '');
            params.set('page', page);
            params.set('size', size);
            if (categoryId) params.append('categoryId', categoryId);

            const appendIfPresent = (key, value) => {
                if (value !== null && value !== undefined && `${value}`.trim() !== '') {
                    params.append(key, value);
                }
            };

            appendIfPresent('author', filters.author);
            appendIfPresent('minPrice', filters.minPrice);
            appendIfPresent('maxPrice', filters.maxPrice);
            appendIfPresent('publishYearFrom', filters.publishYearFrom);
            appendIfPresent('publishYearTo', filters.publishYearTo);
            appendIfPresent('sort', filters.sort);

            const response = await fetch(`${API_BASE}/books/search?${params}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
        },

        /**
         * Gợi ý tìm kiếm nhanh theo tiêu đề/tác giả
         */
        suggestions: async (query = '', size = 8) => {
            const params = new URLSearchParams();
            params.set('q', query || '');
            params.set('size', size);

            const response = await fetch(`${API_BASE}/books/suggestions?${params}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
        },

        /**
         * Sách bán chạy nhất
         */
        bestSellers: async (size = 8) => {
            const response = await fetch(`${API_BASE}/books/discovery/best-sellers?size=${encodeURIComponent(size)}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
        },

        /**
         * Sách đang trending theo đơn gần đây
         */
        trending: async (size = 8, days = 30) => {
            const params = new URLSearchParams();
            params.set('size', size);
            params.set('days', days);
            const response = await fetch(`${API_BASE}/books/discovery/trending?${params}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
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
            return handleResponse(response);
        },

        /**
         * Lấy chi tiết sách
         */
        getById: async (bookId) => {
            const response = await fetch(`${API_BASE}/books/${bookId}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
        }
    };

    const WISHLIST_STORAGE_PREFIX = 'bookom:wishlist:';

    const getWishlistKey = (userId = getAuth().userId) => `${WISHLIST_STORAGE_PREFIX}${userId || 'guest'}`;

    const readWishlist = (userId = getAuth().userId) => {
        try {
            const raw = localStorage.getItem(getWishlistKey(userId));
            if (!raw) {
                return [];
            }

            const parsed = JSON.parse(raw);
            return Array.isArray(parsed)
                ? parsed.filter((item) => item && item.id !== undefined && item.id !== null)
                : [];
        } catch (error) {
            return [];
        }
    };

    const writeWishlist = (userId, items) => {
        localStorage.setItem(getWishlistKey(userId), JSON.stringify(items));
    };

    const normalizeWishlistBook = (book) => {
        const bookId = Number(book?.id);
        return {
            id: bookId,
            title: book?.title || '',
            author: book?.author || '',
            price: Number(book?.price || 0),
            imageUrl: book?.imageUrl || '',
            categoryName: book?.categoryName || book?.category?.name || 'Sach',
            stockQuantity: Number(book?.stockQuantity || 0),
            shopName: book?.shopName || book?.seller?.shopName || '',
            savedAt: Date.now()
        };
    };

    const normalizeWishlistItems = (items) => {
        if (!Array.isArray(items)) {
            return [];
        }

        return items
            .filter((item) => item && item.id !== undefined && item.id !== null)
            .map((item) => ({
                id: Number(item.id),
                title: item.title || '',
                author: item.author || '',
                price: Number(item.price || 0),
                imageUrl: item.imageUrl || '',
                categoryName: item.categoryName || 'Sach',
                stockQuantity: Number(item.stockQuantity || 0),
                shopName: item.shopName || '',
                savedAt: Number(item.savedAt || Date.now())
            }));
    };

    const syncWishlistCache = (userId, items) => {
        writeWishlist(userId, normalizeWishlistItems(items));
    };

    const Wishlist = {
        bootstrap: async (buyerId = null) => {
            return Wishlist.getItems(buyerId);
        },

        getItems: async (buyerId = null) => {
            const id = buyerId || getAuth().userId;
            if (!id) {
                return readWishlist(id).slice().sort((a, b) => Number(b.savedAt || 0) - Number(a.savedAt || 0));
            }

            try {
                const response = await fetch(`${API_BASE}/wishlist/me`, {
                    headers: getHeaders()
                });
                const items = await handleResponse(response);
                syncWishlistCache(id, items);
                return readWishlist(id).slice().sort((a, b) => Number(b.savedAt || 0) - Number(a.savedAt || 0));
            } catch (error) {
                return readWishlist(id).slice().sort((a, b) => Number(b.savedAt || 0) - Number(a.savedAt || 0));
            }
        },

        count: (buyerId = null) => readWishlist(buyerId).length,

        isSaved: (bookId, buyerId = null) => {
            const id = Number(bookId);
            if (!id) {
                return false;
            }
            return readWishlist(buyerId).some((item) => Number(item.id) === id);
        },

        toggle: async (book, buyerId = null) => {
            const id = buyerId || getAuth().userId;
            if (!id) {
                throw new Error('Vui long dang nhap de luu vao Wishlist.');
            }

            const snapshot = normalizeWishlistBook(book);
            if (!snapshot.id) {
                throw new Error('Khong the luu sach nay.');
            }

            try {
                const response = await fetch(`${API_BASE}/wishlist/me/${snapshot.id}`, {
                    method: 'POST',
                    headers: getHeaders()
                });
                const data = await handleResponse(response);
                syncWishlistCache(id, data?.items || []);
                return {
                    saved: !!data?.saved,
                    count: Number(data?.count || 0),
                    items: readWishlist(id)
                };
            } catch (error) {
                const items = readWishlist(id);
                const index = items.findIndex((item) => Number(item.id) === snapshot.id);
                let saved = true;

                if (index >= 0) {
                    items.splice(index, 1);
                    saved = false;
                } else {
                    items.unshift(snapshot);
                }

                writeWishlist(id, items);
                return { saved, count: items.length, items };
            }
        },

        remove: async (bookId, buyerId = null) => {
            const id = buyerId || getAuth().userId;
            if (!id) {
                return [];
            }

            try {
                const response = await fetch(`${API_BASE}/wishlist/me/${Number(bookId)}`, {
                    method: 'DELETE',
                    headers: getHeaders()
                });
                const data = await handleResponse(response);
                syncWishlistCache(id, data?.items || []);
                return readWishlist(id);
            } catch (error) {
                const items = readWishlist(id).filter((item) => Number(item.id) !== Number(bookId));
                writeWishlist(id, items);
                return items;
            }
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
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
        },

        /**
         * Lấy danh sách đơn hàng của buyer
         */
        getBuyerOrders: async () => {
            const response = await fetch(`${API_BASE}/orders/me`, {
                headers: getHeaders()
            });
            return handleResponse(response);
        },

        /**
         * Lấy danh sách đơn hàng dạng summary cho buyer
         */
        getBuyerOrderSummaries: async () => {
            const response = await fetch(`${API_BASE}/orders/me/filter/summary`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({})
            });
            return handleResponse(response);
        },

        /**
         * Lấy chi tiết đơn hàng
         */
        getDetail: async (orderId) => {
            const response = await fetch(`${API_BASE}/orders/me/${orderId}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
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
            return handleResponse(response);
        },

        /**
         * Lấy analytics doanh thu và tồn kho của seller
         */
        getSellerAnalytics: async (days = 30, sellerId = null) => {
            const url = sellerId
                ? `${API_BASE}/orders/seller/${sellerId}/analytics?days=${days}`
                : `${API_BASE}/orders/seller/me/analytics?days=${days}`;
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
            return handleResponse(response);
        },

        /**
         * Hủy đơn hàng của buyer
         */
        cancelBuyerOrder: async (orderId) => {
            const response = await fetch(`${API_BASE}/orders/me/${orderId}/cancel`, {
                method: 'PATCH',
                headers: getHeaders()
            });
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
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
            return handleResponse(response);
        },

        /**
         * Lấy thông tin shop công khai
         */
        getPublicShop: async (slug) => {
            const response = await fetch(`${API_BASE}/shops/${slug}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
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
            return handleResponse(response);
        }
    };

    // ==========================================
    // 8. VOUCHER APIs
    // ==========================================

    const Voucher = {
        getSellerVouchers: async (query = '', status = '') => {
            const { userId } = getAuth();
            const params = new URLSearchParams({ sellerId: userId });
            if (query) params.append('query', query);
            if (status && status !== 'ALL') params.append('status', status);

            const response = await fetch(`${API_BASE}/vouchers/seller/${userId}?${params}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
        },

        create: async (voucherData) => {
            const { userId } = getAuth();
            const response = await fetch(`${API_BASE}/vouchers?sellerId=${userId}`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(voucherData)
            });
            return handleResponse(response);
        },

        delete: async (voucherId) => {
            const { userId } = getAuth();
            const response = await fetch(`${API_BASE}/vouchers/${voucherId}?sellerId=${userId}`, {
                method: 'DELETE',
                headers: getHeaders()
            });
            return handleResponse(response);
        },

        validate: async (code, amount) => {
            const { userId } = getAuth();
            const params = new URLSearchParams({
                code: code,
                userId: userId,
                amount: amount
            });
            const response = await fetch(`${API_BASE}/vouchers/validate?${params}`, {
                headers: getHeaders()
            });
            return handleResponse(response);
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
        Wishlist,
        Order,
        SellerShop,
        Category,
        Voucher,

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
