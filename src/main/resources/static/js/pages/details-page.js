document.addEventListener('DOMContentLoaded', () => {
    if (!window.ApiService) {
        console.error('ApiService not available');
        return;
    }

    const addButton = document.getElementById('detail-add-to-cart-btn');
    const qtyInput = document.getElementById('detail-qty-input');
    if (!addButton || !qtyInput) {
        return;
    }

    addButton.addEventListener('click', async () => {
        const { userId, role } = ApiService.getAuth();
        if (!userId || role !== 'BUYER') {
            alert('Vui long dang nhap tai khoan BUYER de them vao gio hang.');
            window.location.href = '/main/auth';
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
});
