(() => {
    if (!window.ApiService) {
        return;
    }

    const liveContainer = document.getElementById('cart-items-live');
    const fallback1 = document.getElementById('cart-items-fallback-1');
    const fallback2 = document.getElementById('cart-items-fallback-2');
    const subtotalEl = document.getElementById('cart-summary-subtotal');
    const shippingEl = document.getElementById('cart-summary-shipping');
    const shipDiscountEl = document.getElementById('cart-summary-ship-discount');
    const voucherEl = document.getElementById('cart-summary-voucher');
    const totalEl = document.getElementById('cart-summary-total');
    const itemsLabelEl = document.getElementById('cart-summary-items-label');
    const checkoutBtn = document.getElementById('cart-checkout-btn');

    if (!liveContainer) {
        return;
    }

    const formatVnd = (value) => ApiService.formatVND(Math.max(0, Number(value) || 0));

    const buildGrouped = (items) => {
        const grouped = new Map();
        items.forEach((item) => {
            const key = item.sellerId || 'unknown';
            if (!grouped.has(key)) {
                grouped.set(key, {
                    sellerName: item.sellerName || 'Nha sach',
                    rows: []
                });
            }
            grouped.get(key).rows.push(item);
        });
        return Array.from(grouped.values());
    };

    const renderCart = (cart) => {
        const items = Array.isArray(cart?.items) ? cart.items : [];
        const grouped = buildGrouped(items);

        if (fallback1) fallback1.classList.add('hidden');
        if (fallback2) fallback2.classList.add('hidden');

        if (items.length === 0) {
            liveContainer.innerHTML = `
                <div class="bg-white border border-brand-accent rounded-xl shadow-sm p-8 text-center text-gray-500 font-semibold">
                    Gio hang cua ban dang trong. Hay quay lai trang san pham de mua sam.
                </div>
            `;
            return;
        }

        liveContainer.innerHTML = grouped.map((shop) => {
            const rows = shop.rows.map((item) => {
                return `
                    <div class="p-5 flex flex-col md:flex-row items-start md:items-center gap-4 md:gap-0 border-b border-brand-accent/50 hover:bg-brand-cream/30 transition" data-item-id="${item.itemId}">
                        <div class="flex items-start gap-4 w-full md:w-5/12">
                            <div class="mt-4"><input type="checkbox" class="cart-checkbox row-checkbox" checked></div>
                            <div class="w-20 aspect-[3/4] bg-[#2c3e50] border-2 border-white shadow-md flex-shrink-0 flex items-center justify-center text-white text-center font-bold text-[8px] uppercase p-1">
                                BOOK
                            </div>
                            <div class="flex flex-col justify-center gap-1">
                                <a href="/book/${item.bookId}" class="font-bold text-brand-dark text-sm leading-snug line-clamp-2 hover:text-brand-biscuit transition">${item.title || 'Khong co ten'}</a>
                                <span class="text-xs text-gray-500">Tac gia: ${item.author || 'Dang cap nhat'}</span>
                            </div>
                        </div>
                        <div class="w-full md:w-2/12 flex md:flex-col justify-between md:justify-center items-center md:text-center text-sm ml-8 md:ml-0">
                            <span class="md:hidden text-gray-500">Don gia:</span>
                            <div class="font-bold text-brand-dark">${formatVnd(item.unitPrice)}</div>
                        </div>
                        <div class="w-full md:w-2/12 flex justify-between md:justify-center items-center ml-8 md:ml-0">
                            <span class="md:hidden text-gray-500 text-sm">So luong:</span>
                            <div class="flex items-center border border-brand-accent rounded overflow-hidden shadow-sm">
                                <button type="button" data-action="decrease" data-item-id="${item.itemId}" data-current-qty="${item.quantity}" class="w-8 h-8 bg-brand-cream text-gray-600 hover:bg-brand-biscuit hover:text-white transition font-bold outline-none border-r border-brand-accent flex items-center justify-center">-</button>
                                <input type="number" min="1" value="${item.quantity}" data-item-id="${item.itemId}" class="w-12 h-8 text-center text-sm font-bold text-brand-dark outline-none appearance-none cart-qty-input">
                                <button type="button" data-action="increase" data-item-id="${item.itemId}" data-current-qty="${item.quantity}" class="w-8 h-8 bg-brand-cream text-gray-600 hover:bg-brand-biscuit hover:text-white transition font-bold outline-none border-l border-brand-accent flex items-center justify-center">+</button>
                            </div>
                        </div>
                        <div class="w-full md:w-2/12 flex justify-between md:justify-center items-center ml-8 md:ml-0">
                            <span class="md:hidden text-gray-500 text-sm">Thanh tien:</span>
                            <div class="font-black text-brand-orange text-base">${formatVnd(item.lineTotal)}</div>
                        </div>
                        <div class="w-full md:w-1/12 flex justify-end md:justify-center items-center">
                            <button type="button" data-action="remove" data-item-id="${item.itemId}" class="text-gray-400 hover:text-red-500 transition group" title="Xoa san pham">
                                <svg class="w-5 h-5 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            </button>
                        </div>
                    </div>
                `;
            }).join('');

            return `
                <div class="bg-white border border-brand-accent rounded-xl shadow-sm overflow-hidden">
                    <div class="bg-brand-cream/50 px-5 py-4 border-b border-brand-accent flex items-center gap-4">
                        <input type="checkbox" class="cart-checkbox shop-checkbox" checked>
                        <h3 class="font-bold text-brand-dark text-base">${shop.sellerName}</h3>
                    </div>
                    ${rows}
                </div>
            `;
        }).join('');
    };

    const updateSummary = (cart) => {
        const totalItems = Number(cart?.totalItems || 0);
        const subtotal = Number(cart?.totalAmount || 0);
        const shippingFee = totalItems > 0 ? 35000 : 0;
        const shipDiscount = subtotal >= 250000 ? 15000 : 0;
        const voucher = 0;
        const total = Math.max(0, subtotal + shippingFee - shipDiscount - voucher);

        if (itemsLabelEl) itemsLabelEl.textContent = `Tong tien hang (${totalItems} san pham)`;
        if (subtotalEl) subtotalEl.textContent = formatVnd(subtotal);
        if (shippingEl) shippingEl.textContent = formatVnd(shippingFee);
        if (shipDiscountEl) shipDiscountEl.textContent = `-${formatVnd(shipDiscount)}`;
        if (voucherEl) voucherEl.textContent = `-${formatVnd(voucher)}`;
        if (totalEl) totalEl.textContent = formatVnd(total);
        if (checkoutBtn) {
            checkoutBtn.classList.toggle('pointer-events-none', totalItems === 0);
            checkoutBtn.classList.toggle('opacity-60', totalItems === 0);
        }
    };

    const fetchCart = async () => {
        const { userId, role } = ApiService.getAuth();
        if (!userId || role !== 'BUYER') {
            liveContainer.innerHTML = `
                <div class="bg-white border border-brand-accent rounded-xl shadow-sm p-8 text-center text-gray-500 font-semibold">
                    Vui long dang nhap tai khoan BUYER de xem gio hang.
                </div>
            `;
            if (fallback1) fallback1.classList.add('hidden');
            if (fallback2) fallback2.classList.add('hidden');
            updateSummary({ totalItems: 0, totalAmount: 0 });
            return null;
        }

        const cart = await ApiService.Cart.get(userId);
        renderCart(cart);
        updateSummary(cart);
        return cart;
    };

    const patchQuantity = async (itemId, quantity) => {
        const { userId } = ApiService.getAuth();
        await ApiService.Cart.updateItem(userId, itemId, quantity);
    };

    const removeItem = async (itemId) => {
        const { userId } = ApiService.getAuth();
        await ApiService.Cart.removeItem(userId, itemId);
    };

    liveContainer.addEventListener('click', async (event) => {
        const button = event.target.closest('button[data-action]');
        if (!button) {
            return;
        }

        const action = button.getAttribute('data-action');
        const itemId = Number(button.getAttribute('data-item-id'));
        if (!itemId) {
            return;
        }

        try {
            if (action === 'remove') {
                await removeItem(itemId);
            } else {
                const current = Number(button.getAttribute('data-current-qty') || 1);
                const nextQty = action === 'increase' ? current + 1 : Math.max(1, current - 1);
                await patchQuantity(itemId, nextQty);
            }
            await fetchCart();
        } catch (error) {
            const message = error?.message || 'Cap nhat gio hang that bai.';
            alert(message);
        }
    });

    liveContainer.addEventListener('change', async (event) => {
        const input = event.target.closest('.cart-qty-input');
        if (!input) {
            return;
        }

        const itemId = Number(input.getAttribute('data-item-id'));
        const nextQty = Math.max(1, Number(input.value || 1));
        input.value = String(nextQty);
        try {
            await patchQuantity(itemId, nextQty);
            await fetchCart();
        } catch (error) {
            const message = error?.message || 'Cap nhat gio hang that bai.';
            alert(message);
        }
    });

    document.getElementById('selectAll')?.addEventListener('change', (e) => {
        const checked = e.target.checked;
        document.querySelectorAll('.shop-checkbox, .row-checkbox').forEach((cb) => {
            cb.checked = checked;
        });
    });

    fetchCart().catch((error) => {
        const message = error?.message || 'Khong the tai gio hang.';
        liveContainer.innerHTML = `
            <div class="bg-white border border-red-200 text-red-600 rounded-xl shadow-sm p-8 text-center font-semibold">
                ${message}
            </div>
        `;
        if (fallback1) fallback1.classList.add('hidden');
        if (fallback2) fallback2.classList.add('hidden');
        updateSummary({ totalItems: 0, totalAmount: 0 });
    });
})();
