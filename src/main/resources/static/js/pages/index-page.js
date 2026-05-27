document.addEventListener('DOMContentLoaded', () => {
    if (!window.ApiService) {
        console.error('ApiService not available');
        return;
    }

    const booksGrid = document.getElementById('books-grid');
    const homeCategoryGrid = document.getElementById('home-category-grid');
    const sidebarCategoryList = document.getElementById('sidebar-category-list');
    const paginationNav = document.getElementById('books-pagination');
    const searchForm = document.getElementById('index-search-form');
    const searchInput = document.getElementById('index-search-input');
    const sortSelect = document.getElementById('sort-select');
    const priceMinInput = document.getElementById('price-min-input');
    const priceMaxInput = document.getElementById('price-max-input');
    const applyPriceFilterBtn = document.getElementById('apply-price-filter-btn');
    const publisherListEl = document.getElementById('publisher-filter-list');
    const ratingFilterList = document.getElementById('rating-filter-list');

    if (!booksGrid) {
        return;
    }

    const state = {
        page: 0,
        size: 12,
        q: '',
        categoryId: null,
        minPrice: null,
        maxPrice: null,
        sort: 'latest'
    };

    const formatPriceVnd = (value) => {
        if (value === null || value === undefined || Number.isNaN(Number(value))) {
            return 'Lien he';
        }
        return new Intl.NumberFormat('vi-VN').format(Number(value)) + ' đ';
    };

    const escapeHtml = (text) => {
        const div = document.createElement('div');
        div.textContent = text ?? '';
        return div.innerHTML;
    };

    const applyWishlistButtonState = (button, isSaved) => {
        if (!button) {
            return;
        }

        button.dataset.wishlistSaved = isSaved ? 'true' : 'false';
        button.setAttribute('aria-pressed', isSaved ? 'true' : 'false');
        button.setAttribute('title', isSaved ? 'Xoa khoi wishlist' : 'Them vao wishlist');
        button.className = isSaved
            ? 'bg-red-500 text-white p-2.5 rounded-full shadow-md transition-colors'
            : 'bg-white text-brand-dark p-2.5 rounded-full shadow-md hover:bg-brand-brown hover:text-white transition-colors';
        button.innerHTML = isSaved
            ? '<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path></svg>'
            : '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>';
    };

    const createWishlistButton = (book, isSaved) => {
        const title = escapeHtml(book.title || 'Chua co ten sach');
        const author = escapeHtml(book.author || 'Chua co tac gia');
        const imageUrl = escapeHtml(book.imageUrl || '');
        const categoryName = escapeHtml(book.category?.name || 'Sach');

        return `
            <button type="button" title="${isSaved ? 'Xoa khoi wishlist' : 'Them vao wishlist'}" aria-pressed="${isSaved ? 'true' : 'false'}" data-toggle-wishlist="true" data-book-id="${book.id}" data-book-title="${title}" data-book-author="${author}" data-book-price="${book.price ?? ''}" data-book-image="${imageUrl}" data-book-category="${categoryName}" class="${isSaved ? 'bg-red-500 text-white p-2.5 rounded-full shadow-md transition-colors' : 'bg-white text-brand-dark p-2.5 rounded-full shadow-md hover:bg-brand-brown hover:text-white transition-colors'}">
                ${isSaved ? '<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path></svg>' : '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>'}
            </button>
        `;
    };

    const getWishlistBookPayload = (button) => ({
        id: Number(button.getAttribute('data-book-id')),
        title: button.getAttribute('data-book-title') || '',
        author: button.getAttribute('data-book-author') || '',
        price: Number(button.getAttribute('data-book-price') || 0),
        imageUrl: button.getAttribute('data-book-image') || '',
        categoryName: button.getAttribute('data-book-category') || 'Sach'
    });

    const setGridBusy = (isBusy) => {
        booksGrid.setAttribute('aria-busy', isBusy ? 'true' : 'false');
    };

    const renderBooksLoading = () => {
        setGridBusy(true);
        const skeletonCard = `
            <div class="bg-white border border-brand-border rounded-xl p-4 h-full animate-pulse">
                <div class="w-full h-60 bg-brand-bg border border-brand-border rounded-lg mb-4"></div>
                <div class="h-3 bg-gray-200 rounded w-1/3 mb-2"></div>
                <div class="h-4 bg-gray-200 rounded w-4/5 mb-3"></div>
                <div class="h-3 bg-gray-200 rounded w-1/2 mb-6"></div>
                <div class="h-4 bg-gray-200 rounded w-2/3"></div>
            </div>
        `;
        booksGrid.innerHTML = Array.from({ length: 8 }).map(() => skeletonCard).join('');
    };

    const renderBooksEmpty = () => {
        setGridBusy(false);
        const hasFilters = Boolean(state.q || state.categoryId || state.minPrice !== null || state.maxPrice !== null);
        const actionLabel = hasFilters ? 'Xoa bo loc' : 'Thu lai';
        const actionId = hasFilters ? 'books-reset-btn' : 'books-retry-btn';

        booksGrid.innerHTML = `
            <div class="col-span-full">
                <div class="bg-white border border-brand-border rounded-2xl p-10 text-center shadow-soft">
                    <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-brand-bg flex items-center justify-center text-brand-brown">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
                    </div>
                    <h3 class="text-lg font-bold text-brand-dark mb-2">Khong tim thay sach phu hop</h3>
                    <p class="text-sm text-gray-500 mb-6">Thu dieu chinh bo loc hoac thay doi tu khoa tim kiem.</p>
                    <button type="button" id="${actionId}" class="bg-brand-brown text-white font-bold px-6 py-2.5 rounded-lg hover:bg-brand-dark transition-colors">
                        ${actionLabel}
                    </button>
                </div>
            </div>
        `;

        const actionButton = document.getElementById(actionId);
        actionButton?.addEventListener('click', () => {
            if (hasFilters) {
                clearFilters();
            } else {
                loadBooks();
            }
        });
    };

    const renderBooksError = (message = 'Khong tai duoc danh sach sach. Vui long thu lai sau.') => {
        setGridBusy(false);
        booksGrid.innerHTML = `
            <div class="col-span-full">
                <div class="bg-red-50 border border-red-200 text-red-700 rounded-2xl px-6 py-6 text-center">
                    <div class="font-bold mb-2">${escapeHtml(message)}</div>
                    <button id="books-retry-btn" class="mt-2 bg-red-600 text-white px-5 py-2 rounded-lg font-bold hover:bg-red-700 transition-colors">Thu lai</button>
                </div>
            </div>
        `;
        document.getElementById('books-retry-btn')?.addEventListener('click', loadBooks);
    };

    const renderCategoryLoading = () => {
        if (homeCategoryGrid) {
            const item = `
                <div class="w-36 h-36 bg-brand-bg border-2 border-brand-border rounded-lg flex flex-col items-center justify-center animate-pulse">
                    <div class="w-10 h-10 rounded-full bg-gray-200 mb-3"></div>
                    <div class="h-3 w-16 bg-gray-200 rounded"></div>
                </div>
            `;
            homeCategoryGrid.innerHTML = Array.from({ length: 6 }).map(() => item).join('');
        }

        if (sidebarCategoryList) {
            const item = `
                <li class="animate-pulse">
                    <div class="h-3 bg-gray-200 rounded w-3/4"></div>
                </li>
            `;
            sidebarCategoryList.innerHTML = Array.from({ length: 6 }).map(() => item).join('');
        }
    };

    const renderCategoryError = () => {
        if (homeCategoryGrid) {
            homeCategoryGrid.innerHTML = `
                <div class="w-full text-center text-sm font-semibold text-gray-500">Khong tai duoc danh muc.</div>
            `;
        }
        if (sidebarCategoryList) {
            sidebarCategoryList.innerHTML = '<li class="text-sm text-gray-500">Khong tai duoc danh muc.</li>';
        }
    };

    const renderPublisherList = (publishers = []) => {
        if (!publisherListEl) return;
        if (!Array.isArray(publishers) || publishers.length === 0) {
            publisherListEl.innerHTML = '<li class="text-sm text-gray-500">Không có dữ liệu NXB.</li>';
            return;
        }

        publisherListEl.innerHTML = publishers.map((p) => `
            <li>
                <label class="flex items-center gap-3 cursor-pointer group">
                    <input type="checkbox" class="publisher-filter-checkbox" data-publisher="${encodeURIComponent(p)}">
                    <span class="group-hover:text-brand-brown transition-colors">${p}</span>
                </label>
            </li>
        `).join('');
    };

    const loadPublishers = async () => {
        try {
            // Gather publisher list from server-side via book search (first page) to avoid adding new API
            const resp = await ApiService.Book.search('', null, 0, 200);
            const books = Array.isArray(resp?.content) ? resp.content : [];
            const pubs = Array.from(new Set(books.map(b => b.publisher).filter(Boolean))).slice(0, 20);
            renderPublisherList(pubs);
        } catch (err) {
            console.warn('Không tải được danh sách NXB', err);
            renderPublisherList([]);
        }
    };

    const clearFilters = () => {
        state.q = '';
        state.categoryId = null;
        state.minPrice = null;
        state.maxPrice = null;
        state.page = 0;
        if (searchInput) searchInput.value = '';
        if (priceMinInput) priceMinInput.value = '';
        if (priceMaxInput) priceMaxInput.value = '';
        loadBooks();
    };

    const applySort = (books) => {
        const sorted = [...books];
        if (state.sort === 'priceAsc') {
            sorted.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        } else if (state.sort === 'priceDesc') {
            sorted.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        } else if (state.sort === 'titleAsc') {
            sorted.sort((a, b) => String(a.title ?? '').localeCompare(String(b.title ?? '')));
        }
        return sorted;
    };

    const applyPriceFilter = (books) => {
        return books.filter((book) => {
            const price = Number(book.price ?? 0);
            if (state.minPrice !== null && price < state.minPrice) {
                return false;
            }
            if (state.maxPrice !== null && price > state.maxPrice) {
                return false;
            }
            return true;
        });
    };

    const applyPublisherFilter = (books) => {
        if (!state.publishers || state.publishers.length === 0) return books;
        return books.filter((b) => state.publishers.includes(b.publisher));
    };

    const applyRatingFilter = (books) => {
        // Backend stores reviews and ratings; frontend will filter by book.averageRating if present
        if (!state.minRating) return books;
        return books.filter((b) => (Number(b.averageRating || 0) >= Number(state.minRating)));
    };

    const setCategory = (categoryId) => {
        state.categoryId = categoryId;
        state.page = 0;
        loadBooks();
    };

    const applyAdvancedFiltersFromUi = () => {
        // Publishers
        const checkedPublishers = Array.from(document.querySelectorAll('.publisher-filter-checkbox:checked')).map(cb => decodeURIComponent(cb.getAttribute('data-publisher')));
        state.publishers = checkedPublishers.length > 0 ? checkedPublishers : null;

        // Ratings
        const checkedRatings = Array.from(document.querySelectorAll('.rating-filter-checkbox:checked')).map(cb => Number(cb.getAttribute('data-min-rating')));
        state.minRating = checkedRatings.length > 0 ? Math.max(...checkedRatings) : null;

        state.page = 0;
        loadBooks();
    };

    const renderCategories = (categories) => {
        if (homeCategoryGrid) {
            homeCategoryGrid.innerHTML = categories.map((category) => `
                <button type="button" data-category-id="${category.id}" class="home-category-btn w-36 h-36 bg-brand-bg border-2 border-brand-brown rounded-lg flex flex-col items-center justify-center text-brand-dark hover:bg-brand-brown hover:text-white transition-colors duration-300 shadow-sm group">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mb-3 text-brand-brown group-hover:text-white transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
                    <span class="font-bold text-sm">${escapeHtml(category.name)}</span>
                </button>
            `).join('');
        }

        if (sidebarCategoryList) {
            sidebarCategoryList.innerHTML = categories.map((category) => `
                <li>
                    <button type="button" data-category-id="${category.id}" class="sidebar-category-btn w-full text-left flex items-center justify-between group">
                        <span class="group-hover:text-brand-brown transition-colors">${escapeHtml(category.name)}</span>
                    </button>
                </li>
            `).join('');
        }
    };

    const bindCategoryEvents = () => {
        document.querySelectorAll('.home-category-btn, .sidebar-category-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                const categoryId = Number(btn.getAttribute('data-category-id'));
                if (!Number.isNaN(categoryId)) {
                    setCategory(categoryId);
                }
            });
        });
    };

    const loadCategories = async () => {
        renderCategoryLoading();
        try {
            const categories = await ApiService.Category.getAll();
            if (Array.isArray(categories) && categories.length > 0) {
                renderCategories(categories);
                bindCategoryEvents();
            } else {
                renderCategoryError();
            }
        } catch (error) {
            console.error('Load categories failed:', error);
            renderCategoryError();
        }
    };

    const buildCard = (book) => {
        const title = escapeHtml(book.title || 'Chua co ten sach');
        const author = escapeHtml(book.author || 'Chua co tac gia');
        const categoryName = escapeHtml(book.category?.name || 'Sach');
        const imageUrl = book.imageUrl ? escapeHtml(book.imageUrl) : '';
        const detailUrl = '/book/' + book.id;
        const price = formatPriceVnd(book.price);
        const isSaved = ApiService.Wishlist.isSaved(book.id);

        const coverHtml = imageUrl
            ? `<img src="${imageUrl}" alt="${title}" class="book-cover w-full h-full object-cover" loading="lazy">`
            : `<div class="book-cover w-3/4 h-5/6 bg-brand-brown border-[5px] border-white shadow-md flex items-center justify-center text-white text-center font-bold px-3 leading-snug">${title}</div>`;

        return `
            <a href="${detailUrl}" data-detail-url="${detailUrl}" class="product-card group cursor-pointer h-full">
                <div class="product-card-inner bg-white border border-brand-border rounded-xl p-4 h-full flex flex-col relative">
                    <div class="relative w-full h-60 bg-brand-bg border border-brand-border rounded-lg mb-4 flex items-center justify-center overflow-hidden">
                        ${coverHtml}
                        <div class="quick-action-group absolute bottom-3 w-full flex justify-center gap-3 px-4">
                            ${createWishlistButton(book, isSaved)}
                            <button type="button" title="Them vao gio" data-add-to-cart="true" data-book-id="${book.id}" class="bg-white text-brand-dark p-2.5 rounded-full shadow-md hover:bg-brand-brown hover:text-white transition-colors">
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
</svg>
                            </button>
                        </div>
                    </div>
                    <div class="grow flex flex-col">
                        <div class="text-xs text-gray-500 mb-1.5 font-medium uppercase tracking-wider">${categoryName}</div>
                        <h3 class="text-lg font-bold text-brand-dark leading-tight mb-2 line-clamp-2 group-hover:text-brand-brown transition-colors">${title}</h3>
                        <div class="text-sm text-gray-600 mb-3">${author}</div>
                        <div class="mt-auto border-t border-brand-border pt-3 flex items-end justify-between">
                            <div class="text-brand-dark font-medium">Gia: <span class="text-brand-orange text-xl font-bold">${price}</span></div>
                        </div>
                    </div>
                </div>
            </a>
        `;
    };

    const showError = () => {
        renderBooksError();
    };

    const addToCart = async (bookId, quantity = 1) => {
        const { userId, role } = ApiService.getAuth();
        if (!userId || role !== 'BUYER') {
            alert('Vui long dang nhap tai khoan BUYER de them vao gio hang.');
            window.location.href = '/main/auth';
            return;
        }

        try {
            await ApiService.Cart.addItem(userId, {
                bookId: Number(bookId),
                quantity
            });
            alert('Da them san pham vao gio hang.');
        } catch (error) {
            const message = error?.message || 'Them vao gio hang that bai.';
            alert(message);
        }
    };

    const renderPagination = (currentPage, totalPages) => {
        if (!paginationNav) {
            return;
        }

        if (!totalPages || totalPages <= 1) {
            paginationNav.innerHTML = '';
            return;
        }

        let html = '';
        html += `<button type="button" data-page="${Math.max(0, currentPage - 1)}" class="page-btn relative inline-flex items-center px-4 py-2 rounded-md text-sm font-medium text-gray-500 hover:bg-brand-bg transition-colors ${currentPage === 0 ? 'opacity-50 cursor-not-allowed' : ''}" ${currentPage === 0 ? 'disabled' : ''}>Prev</button>`;

        const start = Math.max(0, currentPage - 2);
        const end = Math.min(totalPages - 1, currentPage + 2);
        for (let i = start; i <= end; i++) {
            const active = i === currentPage;
            html += `<button type="button" data-page="${i}" class="page-btn relative inline-flex items-center px-4 py-2 rounded-md text-sm mx-1 ${active ? 'font-bold bg-brand-brown text-white' : 'font-medium text-brand-dark hover:bg-brand-bg transition-colors'}">${i + 1}</button>`;
        }

        html += `<button type="button" data-page="${Math.min(totalPages - 1, currentPage + 1)}" class="page-btn relative inline-flex items-center px-4 py-2 rounded-md text-sm font-medium text-gray-500 hover:bg-brand-bg transition-colors ${currentPage >= totalPages - 1 ? 'opacity-50 cursor-not-allowed' : ''}" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>Next</button>`;
        paginationNav.innerHTML = html;

        paginationNav.querySelectorAll('.page-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                const nextPage = Number(btn.getAttribute('data-page'));
                if (!Number.isNaN(nextPage) && nextPage !== state.page) {
                    state.page = nextPage;
                    loadBooks();
                }
            });
        });
    };

    const loadBooks = async () => {
        renderBooksLoading();
        if (paginationNav) {
            paginationNav.innerHTML = '';
        }

        try {
            const response = await ApiService.Book.search(
                state.q,
                state.categoryId,
                state.page,
                state.size
            );

            const serverBooks = Array.isArray(response?.content) ? response.content : [];
            let books = serverBooks.slice();
            // client-side advanced filters
            books = applyPriceFilter(books);
            books = applyPublisherFilter(books);
            books = applyRatingFilter(books);
            books = applySort(books);

            if (books.length === 0) {
                renderBooksEmpty();
                renderPagination(0, 0);
                return;
            }

            setGridBusy(false);
            booksGrid.innerHTML = books.map(buildCard).join('');
            renderPagination(response?.number ?? state.page, response?.totalPages ?? 0);
        } catch (error) {
            showError();
            console.error('Load books failed:', error);
        }
    };

    booksGrid.addEventListener('click', (event) => {
        const card = event.target.closest('.product-card[data-detail-url]');
        if (card && !event.target.closest('button, a, input, label, select, textarea')) {
            window.location.href = card.getAttribute('data-detail-url');
            return;
        }

        const wishlistButton = event.target.closest('button[data-toggle-wishlist="true"]');
        if (wishlistButton) {
            event.preventDefault();
            event.stopPropagation();

            const buyerId = ApiService.getAuth().userId;
            const role = ApiService.getAuth().role;
            if (!buyerId || role !== 'BUYER') {
                alert('Vui long dang nhap tai khoan BUYER de luu sach yeu thich.');
                window.location.href = '/main/auth';
                return;
            }

            (async () => {
                const book = getWishlistBookPayload(wishlistButton);
                const result = await ApiService.Wishlist.toggle(book, buyerId);
                applyWishlistButtonState(wishlistButton, result.saved);
                alert(result.saved ? 'Da luu vao Wishlist.' : 'Da xoa khoi Wishlist.');
            })().catch((error) => {
                alert(error?.message || 'Khong the cap nhat Wishlist.');
            });
            return;
        }

        const targetButton = event.target.closest('button[data-add-to-cart="true"]');
        if (!targetButton) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        const bookId = targetButton.getAttribute('data-book-id');
        if (bookId) {
            addToCart(bookId, 1);
        }
    });

    searchForm?.addEventListener('submit', (event) => {
        event.preventDefault();
        state.q = (searchInput?.value || '').trim();
        state.page = 0;
        loadBooks();
    });

    sortSelect?.addEventListener('change', () => {
        state.sort = sortSelect.value;
        loadBooks();
    });

    applyPriceFilterBtn?.addEventListener('click', () => {
        const min = Number(priceMinInput?.value);
        const max = Number(priceMaxInput?.value);
        state.minPrice = Number.isNaN(min) || min < 0 ? null : min;
        state.maxPrice = Number.isNaN(max) || max < 0 ? null : max;
        state.page = 0;
        loadBooks();
    });

    (async () => {
        await ApiService.Wishlist.bootstrap().catch(() => {});
        loadCategories();
        await loadBooks();
    })();

    // Header category nav: scroll to category section and prepare for in-place filtering
    const navCategoryLink = document.getElementById('nav-category-link');
    if (navCategoryLink) {
        navCategoryLink.addEventListener('click', (e) => {
            e.preventDefault();
            const section = document.getElementById('danh-muc-sach');
            if (section) section.scrollIntoView({ behavior: 'smooth', block: 'start' });
            // After scrolling, ensure category buttons are bound so user can click to filter
            setTimeout(() => {
                try {
                    bindCategoryEvents();
                    const firstBtn = homeCategoryGrid?.querySelector('[data-category-id]');
                    if (firstBtn) firstBtn.focus();
                } catch (err) {
                    console.error('Category nav handler failed:', err);
                }
            }, 500);
        });
    }

    const token = localStorage.getItem('accessToken');
    const role = localStorage.getItem('userRole');
    const accountLink = document.getElementById('header-account-link');
    const accountText = document.getElementById('header-account-text');

    if (token && accountLink) {
        let targetUrl = '/buyer/dashboard';
        if (role === 'ADMIN') targetUrl = '/admin';
        if (role === 'SELLER') targetUrl = '/seller/dashboard';

        accountLink.href = targetUrl;

        if (accountText) {
            accountText.textContent = 'Quan ly';
        }
    }

    // ===== 3D Tilt Effect for Product Cards =====
    const cards = document.querySelectorAll('.product-card');

    cards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const centerX = rect.width / 2;
            const centerY = rect.height / 2;

            const rotateX = ((y - centerY) / centerY) * -6; // Max rotation 6 degrees
            const rotateY = ((x - centerX) / centerX) * 6;  // Max rotation 6 degrees

            card.style.setProperty('--rotateX', `${rotateY}deg`);
            card.style.setProperty('--rotateY', `${rotateX}deg`);
        });

        card.addEventListener('mouseleave', () => {
            card.style.setProperty('--rotateX', '0deg');
            card.style.setProperty('--rotateY', '0deg');
        });
    });

    // Navigate to detail page on card click (excluding buttons/links)
    booksGrid?.addEventListener('click', (event) => {
        const card = event.target.closest('.product-card[data-detail-url]');
        if (!card) {
            return;
        }

        if (event.target.closest('button, a, input, label, select, textarea')) {
            return;
        }

        const detailUrl = card.getAttribute('data-detail-url');
        if (detailUrl) {
            window.location.href = detailUrl;
        }
    });
});
