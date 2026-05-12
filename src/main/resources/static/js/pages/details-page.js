document.addEventListener('DOMContentLoaded', () => {
    if (!window.ApiService) {
        console.error('ApiService not available');
        return;
    }

    const addButton = document.getElementById('detail-add-to-cart-btn');
    const wishlistButton = document.getElementById('detail-toggle-wishlist-btn');
    const qtyInput = document.getElementById('detail-qty-input');
    if (!addButton || !qtyInput) {
        return;
    }

    const applyWishlistButtonState = (button, isSaved) => {
        if (!button) {
            return;
        }

        button.dataset.wishlistSaved = isSaved ? 'true' : 'false';
        button.setAttribute('aria-pressed', isSaved ? 'true' : 'false');
        button.className = isSaved
            ? 'flex-1 border border-red-500 bg-red-500 text-white text-lg font-bold py-3.5 px-6 rounded-lg shadow-sm flex items-center justify-center gap-2'
            : 'flex-1 border border-brand-orange text-brand-orange text-lg font-bold py-3.5 px-6 rounded-lg hover:bg-brand-orange-light transition shadow-sm flex items-center justify-center gap-2';
        button.innerHTML = isSaved
            ? '<svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path></svg><span>Da luu</span>'
            : '<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg><span>Luu yeu thich</span>';
    };

    const getWishlistBookPayload = () => ({
        id: Number(wishlistButton?.getAttribute('data-book-id')),
        title: wishlistButton?.getAttribute('data-book-title') || '',
        author: wishlistButton?.getAttribute('data-book-author') || '',
        price: Number(wishlistButton?.getAttribute('data-book-price') || 0),
        imageUrl: wishlistButton?.getAttribute('data-book-image') || '',
        categoryName: wishlistButton?.getAttribute('data-book-category') || 'Sach'
    });

    const ensureBuyer = (message) => {
        const { userId, role } = ApiService.getAuth();
        if (!userId || role !== 'BUYER') {
            alert(message);
            window.location.href = '/main/auth';
            return null;
        }
        return userId;
    };

    (async () => {
        await ApiService.Wishlist.bootstrap().catch(() => {});
        if (wishlistButton) {
            applyWishlistButtonState(wishlistButton, ApiService.Wishlist.isSaved(wishlistButton.getAttribute('data-book-id')));
        }
    })();

    addButton.addEventListener('click', async () => {
        const userId = ensureBuyer('Vui long dang nhap tai khoan BUYER de them vao gio hang.');
        if (!userId) {
            return;
        }

        const bookId = Number(addButton.getAttribute('data-book-id'));
        const quantity = Math.max(1, Number(qtyInput.value || 1));

        try {
            await ApiService.Cart.addItem(userId, {
                bookId,
                quantity
            });
            alert('Da them san pham vao gio hang.');
        } catch (error) {
            const message = error?.message || 'Them vao gio hang that bai.';
            alert(message);
        }
    });

    wishlistButton?.addEventListener('click', () => {
        const userId = ensureBuyer('Vui long dang nhap tai khoan BUYER de luu sach yeu thich.');
        if (!userId) {
            return;
        }

        (async () => {
            const result = await ApiService.Wishlist.toggle(getWishlistBookPayload(), userId);
            applyWishlistButtonState(wishlistButton, result.saved);
            alert(result.saved ? 'Da luu vao Wishlist.' : 'Da xoa khoi Wishlist.');
        })().catch((error) => {
            alert(error?.message || 'Khong the cap nhat Wishlist.');
        });
    });
});
