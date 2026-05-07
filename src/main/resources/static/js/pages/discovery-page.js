document.addEventListener('DOMContentLoaded', () => {
    if (!window.ApiService) {
        console.error('ApiService not available');
        return;
    }

    const bookGrid = document.getElementById('book-grid');
    const paginationContainer = document.querySelector('.mt-16.text-center');
    if (!bookGrid) {
        return;
    }

    const pageSize = 20;
    let currentPage = 0;

    const formatVND = (price) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0);
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

    const setGridBusy = (isBusy) => {
        bookGrid.setAttribute('aria-busy', isBusy ? 'true' : 'false');
    };

    const renderBooksLoading = () => {
        setGridBusy(true);
        const skeleton = `
            <div class="bg-white border border-brand-accent rounded-2xl p-4 animate-pulse">
                <div class="w-full aspect-[3/4] bg-brand-cream rounded-xl mb-4"></div>
                <div class="h-3 bg-gray-200 rounded w-2/3 mb-2"></div>
                <div class="h-3 bg-gray-200 rounded w-1/2 mb-4"></div>
                <div class="h-4 bg-gray-200 rounded w-1/3"></div>
            </div>
        `;
        bookGrid.innerHTML = Array.from({ length: 10 }).map(() => skeleton).join('');
        renderPaginationLoading();
    };

    const renderBooksEmpty = () => {
        setGridBusy(false);
        bookGrid.innerHTML = `
            <div class="col-span-full">
                <div class="bg-white border border-brand-accent rounded-2xl p-10 text-center shadow-soft">
                    <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-brand-cream flex items-center justify-center text-brand-biscuit">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
                    </div>
                    <h3 class="text-lg font-bold text-brand-dark mb-2">Chua co sach de hien thi</h3>
                    <p class="text-sm text-gray-500 mb-6">Cua hang dang cap nhat them san pham moi.</p>
                    <button type="button" id="discovery-retry-btn" class="bg-brand-biscuit text-white font-bold px-6 py-2.5 rounded-full hover:bg-brand-dark transition-colors">Thu lai</button>
                </div>
            </div>
        `;
        document.getElementById('discovery-retry-btn')?.addEventListener('click', () => fetchBooks(currentPage));
    };

    const renderBooksError = (message = 'Loi ket noi may chu. Vui long thu lai.') => {
        setGridBusy(false);
        bookGrid.innerHTML = `
            <div class="col-span-full">
                <div class="bg-red-50 border border-red-200 text-red-700 rounded-2xl px-6 py-6 text-center">
                    <div class="font-bold mb-2">${message}</div>
                    <button id="discovery-retry-btn" class="mt-2 bg-red-600 text-white px-5 py-2 rounded-full font-bold hover:bg-red-700 transition-colors">Thu lai</button>
                </div>
            </div>
        `;
        document.getElementById('discovery-retry-btn')?.addEventListener('click', () => fetchBooks(currentPage));
    };

    const renderPaginationLoading = () => {
        if (!paginationContainer) {
            return;
        }
        paginationContainer.innerHTML = `
            <div class="flex justify-center items-center gap-4 animate-pulse">
                <div class="h-10 w-28 bg-brand-cream rounded-full"></div>
                <div class="h-4 w-24 bg-brand-cream rounded-full"></div>
                <div class="h-10 w-28 bg-brand-cream rounded-full"></div>
            </div>
        `;
    };

    const renderPagination = (current, total) => {
        if (!paginationContainer) {
            return;
        }

        if (!total || total <= 1) {
            paginationContainer.innerHTML = '';
            return;
        }

        let paginationHtml = `<div class="flex justify-center items-center gap-4">`;

        if (current > 0) {
            paginationHtml += `<button onclick="window.changePage(${current - 1})" class="bg-white border-2 border-brand-biscuit text-brand-biscuit font-bold px-6 py-2 rounded-full hover:bg-brand-biscuit hover:text-white transition-colors">Trang truoc</button>`;
        }

        paginationHtml += `<span class="font-bold text-brand-dark">Trang ${current + 1} / ${total}</span>`;

        if (current < total - 1) {
            paginationHtml += `<button onclick="window.changePage(${current + 1})" class="bg-white border-2 border-brand-biscuit text-brand-biscuit font-bold px-6 py-2 rounded-full hover:bg-brand-biscuit hover:text-white transition-colors">Trang sau</button>`;
        }

        paginationHtml += `</div>`;
        paginationContainer.innerHTML = paginationHtml;
    };

    const fetchBooks = async (page) => {
        renderBooksLoading();
        try {
            const response = await ApiService.Book.search('', null, page, pageSize);
            const books = Array.isArray(response?.content) ? response.content : [];
            const totalPages = response?.totalPages ?? 0;

            if (books.length === 0) {
                renderBooksEmpty();
                renderPagination(0, 0);
                return;
            }

            setGridBusy(false);
            bookGrid.innerHTML = books.map((book) => {
                const coverUrl = book.imageUrl ? book.imageUrl : 'https://via.placeholder.com/150x200?text=No+Cover';
                return `
                    <a href="/book/${book.id}" class="product-card group relative h-full flex flex-col bg-white rounded-2xl p-4 shadow-sm border border-brand-accent cursor-pointer">
                        <div class="relative w-full aspect-[3/4] bg-brand-cream rounded-xl mb-4 flex items-center justify-center overflow-hidden border border-brand-accent">
                            <img src="${coverUrl}" alt="${book.title}" class="book-cover w-full h-full object-cover shadow-md transition-transform duration-500" onerror="this.src='https://via.placeholder.com/150x200?text=Error'"/>
                            <button type="button" data-add-to-cart="true" data-book-id="${book.id}" class="cart-btn-hover absolute bottom-3 w-10 h-10 bg-brand-biscuit text-white rounded-full shadow-lg flex items-center justify-center hover:bg-brand-dark transition-colors">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                            </button>
                        </div>
                        <div class="flex flex-col flex-grow">
                            <h3 class="text-sm font-bold text-brand-dark leading-snug mb-1 line-clamp-2 group-hover:text-brand-biscuit transition-colors">${book.title}</h3>
                            <div class="text-xs text-gray-500 font-medium mb-3">${book.author}</div>
                            <div class="mt-auto flex justify-between items-end">
                                <div><span class="text-brand-orange font-black text-lg">${formatVND(book.price)}</span></div>
                            </div>
                        </div>
                    </a>
                `;
            }).join('');

            renderPagination(page, totalPages);
        } catch (error) {
            console.error('Loi tai sach:', error);
            renderBooksError();
            renderPagination(0, 0);
        }
    };

    bookGrid.addEventListener('click', (event) => {
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

    window.changePage = (newPage) => {
        currentPage = newPage;
        fetchBooks(currentPage);
        document.getElementById('sach-moi-nhat')?.scrollIntoView({ behavior: 'smooth' });
    };

    fetchBooks(currentPage);
});
