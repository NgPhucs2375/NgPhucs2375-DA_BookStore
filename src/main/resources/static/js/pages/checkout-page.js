(() => {
    if (!window.ApiService) {
        return;
    }

    const badgeEl = document.getElementById('checkout-items-badge');
    const itemsEl = document.getElementById('checkout-cart-items');
    const subtotalLabelEl = document.getElementById('checkout-subtotal-label');
    const subtotalEl = document.getElementById('checkout-subtotal');
    const shippingFeeEl = document.getElementById('checkout-shipping-fee');
    const shopDiscountEl = document.getElementById('checkout-shop-discount');
    const voucherDiscountEl = document.getElementById('checkout-voucher-discount');
    const totalEl = document.getElementById('checkout-total');
    const placeOrderBtn = document.getElementById('checkout-place-order-btn');

    if (!itemsEl) {
        return;
    }

    let currentCart = null;

    const formatVnd = (value) => ApiService.formatVND(Math.max(0, Number(value) || 0));

    const getShippingFee = () => {
        const expressChecked = document.getElementById('ship_express')?.checked;
        return expressChecked ? 50000 : 35000;
    };

    const renderItems = (cart) => {
        const items = Array.isArray(cart?.items) ? cart.items : [];

        if (items.length === 0) {
            itemsEl.innerHTML = `
                <div class="text-center text-gray-500 font-semibold py-6">
                    Gio hang rong. Vui long quay lai trang gio hang de them san pham.
                </div>
            `;
            return;
        }

        itemsEl.innerHTML = items.map((item) => `
            <div class="flex gap-4 mb-5">
                <div class="relative w-16 aspect-[3/4] bg-[#2c3e50] border border-gray-200 rounded shadow-sm flex-shrink-0 flex items-center justify-center">
                    <span class="text-white font-bold text-[7px] text-center px-1">BOOK</span>
                    <span class="absolute -top-2 -right-2 bg-brand-orange text-white text-[10px] w-5 h-5 rounded-full flex items-center justify-center font-bold">x${item.quantity}</span>
                </div>
                <div class="flex flex-col flex-grow justify-center">
                    <h4 class="font-bold text-brand-dark text-xs leading-snug mb-1 line-clamp-2">${item.title || 'Khong co ten'}</h4>
                    <p class="text-[10px] text-gray-500 font-medium mb-1">Tac gia: ${item.author || 'Dang cap nhat'}</p>
                    <div class="font-bold text-brand-dark text-sm">${formatVnd(item.lineTotal)}</div>
                </div>
            </div>
        `).join('');
    };

    const updateSummary = (cart) => {
        const totalItems = Number(cart?.totalItems || 0);
        const subtotal = Number(cart?.totalAmount || 0);
        const shippingFee = totalItems > 0 ? getShippingFee() : 0;
        const shopDiscount = 0;
        const voucherDiscount = 0;
        const total = Math.max(0, subtotal + shippingFee - shopDiscount - voucherDiscount);

        if (badgeEl) badgeEl.textContent = `${totalItems} San Pham`;
        if (subtotalLabelEl) subtotalLabelEl.textContent = `Tam tinh (${totalItems} san pham)`;
        if (subtotalEl) subtotalEl.textContent = formatVnd(subtotal);
        if (shippingFeeEl) shippingFeeEl.textContent = formatVnd(shippingFee);
        if (shopDiscountEl) shopDiscountEl.textContent = `-${formatVnd(shopDiscount)}`;
        if (voucherDiscountEl) voucherDiscountEl.textContent = `-${formatVnd(voucherDiscount)}`;
        if (totalEl) totalEl.textContent = formatVnd(total);
        if (placeOrderBtn) {
            placeOrderBtn.disabled = totalItems === 0;
            placeOrderBtn.classList.toggle('opacity-60', totalItems === 0);
            placeOrderBtn.classList.toggle('cursor-not-allowed', totalItems === 0);
        }
    };

    const deriveAddress = () => {
        const manual = (document.getElementById('address_detail')?.value || '').trim();
        if (manual) {
            return manual;
        }

        const selected = document.querySelector('input[name="address"]:checked');
        if (!selected) {
            return '';
        }

        const label = document.querySelector(`label[for="${selected.id}"]`);
        const textNode = label?.querySelector('.text-sm.text-gray-600.font-medium.leading-relaxed');
        return (textNode?.textContent || '').replace(/\s+/g, ' ').trim();
    };

    const fetchCart = async () => {
        const { userId, role } = ApiService.getAuth();
        if (!userId || role !== 'BUYER') {
            itemsEl.innerHTML = `
                <div class="text-center text-gray-500 font-semibold py-6">
                    Vui long dang nhap tai khoan BUYER de thanh toan.
                </div>
            `;
            updateSummary({ totalItems: 0, totalAmount: 0 });
            return;
        }

        const cart = await ApiService.Cart.get(userId);
        currentCart = cart;
        renderItems(cart);
        updateSummary(cart);
    };

    document.querySelectorAll('input[name="shipping"]').forEach((radio) => {
        radio.addEventListener('change', () => {
            if (currentCart) {
                updateSummary(currentCart);
            }
        });
    });

    placeOrderBtn?.addEventListener('click', async () => {
        try {
            const shippingAddress = deriveAddress();
            if (!shippingAddress) {
                alert('Vui long nhap hoac chon dia chi giao hang.');
                return;
            }

            const response = await ApiService.Order.checkout(shippingAddress);
            const orderId = response?.orderId;
            localStorage.setItem('lastOrderId', String(orderId || ''));
            window.location.href = orderId ? `/main/order-success?orderId=${orderId}` : '/main/order-success';
        } catch (error) {
            const message = error?.message || 'Dat hang that bai.';
            alert(message);
        }
    });

    fetchCart().catch((error) => {
        const message = error?.message || 'Khong the tai du lieu thanh toan.';
        itemsEl.innerHTML = `
            <div class="text-center text-red-600 font-semibold py-6">${message}</div>
        `;
        updateSummary({ totalItems: 0, totalAmount: 0 });
    });
})();
