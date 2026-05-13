(() => {
    if (!window.ApiService) return;

    const badgeEl = document.getElementById('checkout-items-badge');
    const itemsEl = document.getElementById('checkout-cart-items');
    const subtotalLabelEl = document.getElementById('checkout-subtotal-label');
    const subtotalEl = document.getElementById('checkout-subtotal');
    const shippingFeeEl = document.getElementById('checkout-shipping-fee');
    const shopDiscountEl = document.getElementById('checkout-shop-discount');
    const voucherDiscountEl = document.getElementById('checkout-voucher-discount');
    const totalEl = document.getElementById('checkout-total');
    const placeOrderBtn = document.getElementById('checkout-place-order-btn');
    const sellerBreakdownEl = document.getElementById('checkout-seller-breakdown');

    const couponInput = document.getElementById('checkout-coupon-code');
    const applyCouponBtn = document.getElementById('apply-coupon-btn');
    const clearCouponBtn = document.getElementById('clear-coupon-btn');
    const appliedCouponWrap = document.getElementById('checkout-applied-coupon');
    const appliedCouponText = document.getElementById('checkout-applied-coupon-text');
    const removeAppliedCouponBtn = document.getElementById('remove-applied-coupon');

    const addressesContainer = document.getElementById('checkout-addresses-list');
    const fullnameInput = document.getElementById('fullname');
    const phoneInput = document.getElementById('phone');
    const addressDetailInput = document.getElementById('address_detail');
    const saveDefaultCheckbox = document.getElementById('save-default-checkbox');

    const estimatedDeliveryEl = document.getElementById('checkout-estimated-delivery');

    if (!itemsEl) return;

    let currentCart = null;
    let appliedCoupon = null;

    const COUPONS = {
        'BOOKOM15K': { type: 'fixed', amount: 15000, desc: 'BOOKOM15K (Giảm 15.000đ)' },
        'SAVE10': { type: 'percent', amount: 10, desc: 'SAVE10 (Giảm 10%)' }
    };

    const formatVnd = (value) => ApiService.formatVND(Math.max(0, Number(value) || 0));

    const getHeadersForProfile = () => {
        const auth = ApiService.getAuth();
        const headers = { 'Content-Type': 'application/json', 'X-User-Id': auth.userId || '' };
        if (auth.token) headers['Authorization'] = `Bearer ${auth.token}`;
        return headers;
    };

    const getShippingFee = () => document.getElementById('ship_express')?.checked ? 50000 : 35000;

    const computeEstimatedDeliveryText = () => {
        const now = new Date();
        const isExpress = document.getElementById('ship_express')?.checked;
        const days = isExpress ? 1 : 3;
        const eta = new Date(now.getFullYear(), now.getMonth(), now.getDate() + days);
        return `${isExpress ? 'Nhanh' : 'Tiêu chuẩn'} — Ước tính giao: ${eta.toLocaleDateString('vi-VN')}`;
    };

    const renderItems = (cart) => {
        const items = Array.isArray(cart?.items) ? cart.items : [];
        if (items.length === 0) {
            itemsEl.innerHTML = `
                <div class="text-center text-gray-500 font-semibold py-6">Giỏ hàng rỗng.</div>
            `;
            if (sellerBreakdownEl) sellerBreakdownEl.innerHTML = '';
            return;
        }

        // group by seller
        const grouped = new Map();
        items.forEach((it) => {
            const key = it.sellerId || 'unknown';
            if (!grouped.has(key)) grouped.set(key, { sellerName: it.sellerName || 'Shop', rows: [] });
            grouped.get(key).rows.push(it);
        });

        // render seller breakdown on the summary sidebar
        if (sellerBreakdownEl) {
            sellerBreakdownEl.innerHTML = Array.from(grouped.values()).map((shop) => {
                const subtotal = shop.rows.reduce((s, r) => s + Number(r.lineTotal || 0), 0);
                return `<div class="border border-brand-accent/40 rounded px-3 py-2 bg-white">
                    <div class="flex justify-between items-center text-sm font-bold mb-1"><span>${shop.sellerName}</span><span>${formatVnd(subtotal)}</span></div>
                    </div>`;
            }).join('');
        }

        // render cart items list (compact)
        itemsEl.innerHTML = Array.from(grouped.values()).map((shop) => {
            const rows = shop.rows.map((item) => `
                <div class="flex gap-4 mb-4">
                    <div class="relative w-16 aspect-[3/4] bg-[#2c3e50] border border-gray-200 rounded shadow-sm flex-shrink-0 flex items-center justify-center">
                        <span class="text-white font-bold text-[7px] text-center px-1">BOOK</span>
                        <span class="absolute -top-2 -right-2 bg-brand-orange text-white text-[10px] w-5 h-5 rounded-full flex items-center justify-center font-bold">x${item.quantity}</span>
                    </div>
                    <div class="flex flex-col flex-grow justify-center">
                        <h4 class="font-bold text-brand-dark text-xs leading-snug mb-1 line-clamp-2">${item.title || 'Không rõ'}</h4>
                        <p class="text-[10px] text-gray-500 font-medium mb-1">Tác giả: ${item.author || '-'}</p>
                        <div class="font-bold text-brand-dark text-sm">${formatVnd(item.lineTotal)}</div>
                    </div>
                </div>
            `).join('');

            return `<div class="mb-4"><div class="font-bold text-sm mb-2">${shop.sellerName}</div>${rows}</div>`;
        }).join('');
    };

    const updateSummary = (cart) => {
        const totalItems = Number(cart?.totalItems || 0);
        const subtotal = Number(cart?.totalAmount || 0);
        const shippingFee = totalItems > 0 ? getShippingFee() : 0;
        const shopDiscount = 0;

        let voucherDiscount = 0;
        if (appliedCoupon) {
            if (appliedCoupon.type === 'fixed') voucherDiscount = appliedCoupon.amount;
            if (appliedCoupon.type === 'percent') voucherDiscount = Math.round(subtotal * (appliedCoupon.amount / 100));
        }

        const total = Math.max(0, subtotal + shippingFee - shopDiscount - voucherDiscount);

        if (badgeEl) badgeEl.textContent = `${totalItems} Sản Phẩm`;
        if (subtotalLabelEl) subtotalLabelEl.textContent = `Tạm tính (${totalItems} sản phẩm)`;
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

        if (estimatedDeliveryEl) estimatedDeliveryEl.textContent = computeEstimatedDeliveryText();
    };

    const deriveAddress = () => {
        const manual = (addressDetailInput?.value || '').trim();
        if (manual) {
            return { addressLine: manual, addressId: null };
        }

        const selected = document.querySelector('input[name="address"]:checked');
        if (!selected) return { addressLine: '', addressId: null };

        const addrId = selected.getAttribute('data-address-id') || null;
        const label = document.querySelector(`label[for="${selected.id}"]`);
        const textNode = label?.querySelector('.text-sm.text-gray-600.font-medium.leading-relaxed');
        const addressLine = (textNode?.textContent || '').replace(/\s+/g, ' ').trim();
        return { addressLine, addressId: addrId ? Number(addrId) : null };
    };

    const fetchAddresses = async () => {
        try {
            const headers = getHeadersForProfile();
            const res = await fetch('/buyer/profile/api/addresses', { headers });
            if (!res.ok) return;
            const addresses = await res.json();
            if (!Array.isArray(addresses) || !addresses.length) return;

            // render addresses
            addressesContainer.innerHTML = addresses.map((addr, idx) => `
                <div class="relative">
                    <input type="radio" name="address" id="address_addr_${addr.id || idx}" data-address-id="${addr.id || ''}" class="radio-card-input" ${addr.isDefault ? 'checked' : ''}>
                    <label for="address_addr_${addr.id || idx}" class="radio-card-label bg-white p-4 rounded-xl">
                        <div class="radio-circle mt-1 mr-4"></div>
                        <div class="flex-grow">
                            <div class="flex items-center gap-3 mb-1">
                                <h3 class="font-bold text-brand-dark text-base">${addr.recipientName || ''}</h3>
                                <span class="text-gray-400 font-medium text-sm">|</span>
                                <span class="font-bold text-gray-600 text-sm">${addr.recipientPhone || ''}</span>
                                ${addr.isDefault ? '<span class="bg-brand-orange text-white text-[10px] font-black px-2 py-0.5 rounded ml-auto uppercase shadow-sm">Mặc Định</span>' : ''}
                            </div>
                            <div class="text-sm text-gray-600 font-medium leading-relaxed">${(addr.addressLine || '')}</div>
                        </div>
                    </label>
                </div>
            `).join('');
        } catch (e) {
            // ignore
        }
    };

    const createAddress = async (payload) => {
        try {
            const headers = getHeadersForProfile();
            const res = await fetch('/buyer/profile/api/addresses/create', {
                method: 'POST', headers, body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('Không thể lưu địa chỉ');
            return await res.json();
        } catch (e) { throw e; }
    };

    // Save address button (explicit save from form)
    const saveAddrBtn = document.getElementById('save-address-btn');
    saveAddrBtn?.addEventListener('click', async () => {
        try {
            const payload = {
                recipientName: fullnameInput?.value || '',
                recipientPhone: phoneInput?.value || '',
                addressLine: addressDetailInput?.value || '',
                addressType: 'OTHER',
                isDefault: document.getElementById('save-default-checkbox')?.checked || false
            };
            const created = await createAddress(payload);
            if (created?.id && payload.isDefault) {
                await setDefaultAddress(created.id);
            }
            await fetchAddresses();
            alert('Đã lưu địa chỉ.');
        } catch (err) {
            console.error(err);
            alert(err?.message || 'Lưu địa chỉ thất bại');
        }
    });

    const setDefaultAddress = async (addressId) => {
        try {
            const headers = getHeadersForProfile();
            const res = await fetch(`/buyer/profile/api/addresses/${addressId}/set-default`, { method: 'POST', headers });
            if (!res.ok) throw new Error('Không thể đặt mặc định');
            return await res.text();
        } catch (e) { throw e; }
    };

    const fetchCart = async () => {
        const { userId, role } = ApiService.getAuth();
        if (!userId || role !== 'BUYER') {
            itemsEl.innerHTML = `<div class="text-center text-gray-500 font-semibold py-6">Vui lòng đăng nhập tài khoản BUYER để thanh toán.</div>`;
            updateSummary({ totalItems: 0, totalAmount: 0 });
            return;
        }

        const cart = await ApiService.Cart.get(userId);
        currentCart = cart;
        renderItems(cart);
        updateSummary(cart);
    };

    // coupon handlers
    applyCouponBtn?.addEventListener('click', (e) => {
        const code = (couponInput?.value || '').trim().toUpperCase();
        if (!code) return alert('Nhập mã khuyến mãi');
        const rule = COUPONS[code];
        if (!rule) return alert('Mã không hợp lệ hoặc đã hết hạn');
        appliedCoupon = { ...rule, code };
        appliedCouponText.textContent = rule.desc;
        appliedCouponWrap.classList.remove('hidden');
        clearCouponBtn?.classList.remove('hidden');
        updateSummary(currentCart || {});
    });

    removeAppliedCouponBtn?.addEventListener('click', () => {
        appliedCoupon = null;
        appliedCouponWrap.classList.add('hidden');
        couponInput.value = '';
        clearCouponBtn?.classList.add('hidden');
        updateSummary(currentCart || {});
    });

    clearCouponBtn?.addEventListener('click', () => {
        appliedCoupon = null;
        couponInput.value = '';
        appliedCouponWrap.classList.add('hidden');
        clearCouponBtn.classList.add('hidden');
        updateSummary(currentCart || {});
    });

    document.querySelectorAll('input[name="shipping"]').forEach((radio) => {
        radio.addEventListener('change', () => {
            if (currentCart) updateSummary(currentCart);
        });
    });

    const placeOrder = async (shippingAddress, voucherCode) => {
        const response = await fetch('/api/orders/me/checkout', {
            method: 'POST',
            headers: ApiService.getHeaders(),
            body: JSON.stringify({ shippingAddress, voucherCode })
        });
        return ApiService.handleResponse(response);
    };

    placeOrderBtn?.addEventListener('click', async () => {
        try {
            const { addressLine } = deriveAddress();
            if (!addressLine) return alert('Vui lòng chọn địa chỉ nhận hàng.');

            placeOrderBtn.disabled = true;
            placeOrderBtn.textContent = 'Đang xử lý...';

            const voucherCode = appliedCoupon ? appliedCoupon.code : null;
            const { orderId } = await placeOrder(addressLine, voucherCode);
            
            window.location.href = orderId ? `/main/order-success?orderId=${orderId}` : '/main/order-success';
        } catch (error) {
            const message = error?.message || 'Đặt hàng thất bại.';
            alert(message);
            placeOrderBtn.disabled = false;
            placeOrderBtn.textContent = 'Đặt Hàng';
        }
    });

    // bootstrap
    fetchAddresses();
    fetchCart().catch((error) => {
        const message = error?.message || 'Không thể tải dữ liệu thanh toán.';
        itemsEl.innerHTML = `<div class="text-center text-red-600 font-semibold py-6">${message}</div>`;
        updateSummary({ totalItems: 0, totalAmount: 0 });
    });

})();
