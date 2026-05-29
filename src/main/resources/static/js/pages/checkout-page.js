(() => {
    if (!window.ApiService) return;

    // --- DOM ELEMENTS ---
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
    const saveAddrBtn = document.getElementById('save-address-btn');

    // Thêm các Element cho dropdown địa chỉ
    const provinceSelect = document.getElementById('province');
    const districtSelect = document.getElementById('district');
    const wardSelect = document.getElementById('ward');

    if (!itemsEl) return;

    let currentCart = null;
    let appliedCoupon = null;

    // --- HELPER FUNCTIONS ---
    const formatVnd = (value) => ApiService.formatVND(Math.max(0, Number(value) || 0));

    const escapeHtml = (value) => String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');

    const formatAddressSummary = (address) => [
        address?.recipientName,
        address?.recipientPhone,
        address?.addressLine,
        address?.ward,
        address?.district,
        address?.province
    ].filter((part) => part && String(part).trim()).join(' • ');

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

    // --- RENDER CART LOGIC ---
    const renderItems = (cart) => {
        const items = Array.isArray(cart?.items) ? cart.items : [];
        if (items.length === 0) {
            itemsEl.innerHTML = `<div class="text-center text-gray-500 font-semibold py-6">Giỏ hàng rỗng.</div>`;
            if (sellerBreakdownEl) sellerBreakdownEl.innerHTML = '';
            return;
        }

        const grouped = new Map();
        items.forEach((it) => {
            const key = it.sellerId || 'unknown';
            if (!grouped.has(key)) grouped.set(key, { sellerName: it.sellerName || 'Shop', rows: [] });
            grouped.get(key).rows.push(it);
        });

        if (sellerBreakdownEl) {
            sellerBreakdownEl.innerHTML = Array.from(grouped.values()).map((shop) => {
                const subtotal = shop.rows.reduce((s, r) => s + Number(r.lineTotal || 0), 0);
                return `<div class="border border-brand-accent/40 rounded px-3 py-2 bg-white">
                    <div class="flex justify-between items-center text-sm font-bold mb-1"><span>${shop.sellerName}</span><span>${formatVnd(subtotal)}</span></div>
                    </div>`;
            }).join('');
        }

        itemsEl.innerHTML = Array.from(grouped.values()).map((shop) => {
            const rows = shop.rows.map((item) => `
                <div class="flex gap-4 mb-4">
                    <div class="relative w-16 aspect-[3/4] bg-[#2c3e50] border border-gray-200 rounded shadow-sm shrink-0 flex items-center justify-center">
                        <span class="text-white font-bold text-[7px] text-center px-1">BOOK</span>
                        <span class="absolute -top-2 -right-2 bg-brand-orange text-white text-[10px] w-5 h-5 rounded-full flex items-center justify-center font-bold">x${item.quantity}</span>
                    </div>
                    <div class="flex flex-col grow justify-center">
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
            voucherDiscount = Number(appliedCoupon.discount) || 0;
        }

        const total = Math.max(0, subtotal + shippingFee - shopDiscount - voucherDiscount);

        if (badgeEl) badgeEl.textContent = `${totalItems} Sản Phẩm`;
        if (subtotalLabelEl) subtotalLabelEl.textContent = `Tạm tính (${totalItems} sản phẩm)`;
        if (subtotalEl) subtotalEl.textContent = formatVnd(subtotal);
        if (shippingFeeEl) shippingFeeEl.textContent = formatVnd(shippingFee);
        if (shopDiscountEl) shopDiscountEl.textContent = `-${formatVnd(shopDiscount)}`;
        if (voucherDiscountEl) voucherDiscountEl.textContent = `-${formatVnd(voucherDiscount)}`;

        // Hiển thị giá gốc (trước giảm) khi có coupon - không thay đổi layout, chỉ thêm dòng
        const originalBeforeDiscountEl = document.getElementById('checkout-original-before-discount');
        if (appliedCoupon && voucherDiscount > 0) {
            const originalAmount = subtotal + shippingFee;
            if (originalBeforeDiscountEl) {
                originalBeforeDiscountEl.classList.remove('hidden');
                const originalAmountEl = originalBeforeDiscountEl.querySelector('.original-amount');
                if (originalAmountEl) originalAmountEl.textContent = formatVnd(originalAmount);
            }
        } else {
            if (originalBeforeDiscountEl) {
                originalBeforeDiscountEl.classList.add('hidden');
            }
        }

        if (totalEl) totalEl.textContent = formatVnd(total);
        if (placeOrderBtn) {
            placeOrderBtn.disabled = totalItems === 0;
            placeOrderBtn.classList.toggle('opacity-60', totalItems === 0);
            placeOrderBtn.classList.toggle('cursor-not-allowed', totalItems === 0);
        }

        if (estimatedDeliveryEl) estimatedDeliveryEl.textContent = computeEstimatedDeliveryText();
    };


    // Helper: extract unique seller IDs from current cart
    const getSellerIdsFromCart = () => {
        if (!currentCart || !Array.isArray(currentCart.items)) return '';
        const sellerIds = [...new Set(currentCart.items.map(item => item.sellerId).filter(id => id != null))];
        return sellerIds.join(',');
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

    // --- API TỈNH/THÀNH PHỐ LOGIC ---
    const loadLocations = () => {
        if (!provinceSelect || !districtSelect || !wardSelect) return;

        // Tải danh sách Tỉnh/Thành
        fetch('https://provinces.open-api.vn/api/p/')
            .then(res => res.json())
            .then(data => {
                let html = '<option value="" disabled selected>Chọn Tỉnh / Thành Phố</option>';
                data.forEach(p => {
                    html += `<option value="${p.name}" data-code="${p.code}">${p.name}</option>`;
                });
                provinceSelect.innerHTML = html;
            }).catch(console.error);

        // Bắt sự kiện đổi Tỉnh -> Load Quận
        provinceSelect.addEventListener('change', function() {
            const code = this.options[this.selectedIndex].getAttribute('data-code');
            if(!code) return;
            wardSelect.innerHTML = '<option value="" disabled selected>Chọn Phường / Xã</option>';
            districtSelect.innerHTML = '<option value="" disabled selected>Đang tải...</option>';

            fetch(`https://provinces.open-api.vn/api/p/${code}?depth=2`)
                .then(res => res.json())
                .then(data => {
                    let html = '<option value="" disabled selected>Chọn Quận / Huyện</option>';
                    data.districts.forEach(d => {
                        html += `<option value="${d.name}" data-code="${d.code}">${d.name}</option>`;
                    });
                    districtSelect.innerHTML = html;
                }).catch(console.error);
        });

        // Bắt sự kiện đổi Quận -> Load Phường
        districtSelect.addEventListener('change', function() {
            const code = this.options[this.selectedIndex].getAttribute('data-code');
            if(!code) return;
            wardSelect.innerHTML = '<option value="" disabled selected>Đang tải...</option>';

            fetch(`https://provinces.open-api.vn/api/d/${code}?depth=2`)
                .then(res => res.json())
                .then(data => {
                    let html = '<option value="" disabled selected>Chọn Phường / Xã</option>';
                    data.wards.forEach(w => {
                        html += `<option value="${w.name}">${w.name}</option>`;
                    });
                    wardSelect.innerHTML = html;
                }).catch(console.error);
        });
    };

    // --- ADDRESS LOGIC ---
    const deriveAddress = () => {
        // Kiểm tra xem người dùng có chọn địa chỉ đã lưu (Radio button) không
        const selected = document.querySelector('input[name="address"]:checked');
        if (selected) {
            const addressLine = selected.getAttribute('data-address-summary') || '';
            const addrId = selected.getAttribute('data-address-id') || null;
            if (addressLine) {
                return { addressLine, addressId: addrId ? Number(addrId) : null };
            }
        }

        // Nếu không có địa chỉ lưu sẵn, lấy từ các ô nhập tay
        const recipientName = (fullnameInput?.value || '').trim();
        const recipientPhone = (phoneInput?.value || '').trim();
        const p = provinceSelect?.value || '';
        const d = districtSelect?.value || '';
        const w = wardSelect?.value || '';
        const manualAddress = (addressDetailInput?.value || '').trim();

        // Format gửi cho Backend tạo Order: Tên • SĐT • Tòa nhà • Phường • Quận • Tỉnh
        const addressLine = [recipientName, recipientPhone, manualAddress, w, d, p].filter(Boolean).join(' • ');
        return { addressLine, addressId: null };
    };

    const fetchAddresses = async () => {
        try {
            const headers = getHeadersForProfile();
            const res = await fetch('/buyer/profile/api/addresses', { headers });
            if (!res.ok) return;
            const addresses = await res.json();

            if (!Array.isArray(addresses) || !addresses.length) {
                addressesContainer.innerHTML = '<div class="rounded-xl border border-dashed border-brand-accent bg-brand-cream/30 px-4 py-6 text-sm text-gray-500 font-medium">Bạn chưa có địa chỉ nào. Hãy lưu địa chỉ ở khung bên dưới để tiếp tục thanh toán.</div>';
                return;
            }

            const checkedId = document.querySelector('input[name="address"]:checked')?.getAttribute('data-address-id');
            const defaultAddress = addresses.find((addr) => addr.isDefault) || addresses[0];

            addressesContainer.innerHTML = addresses.map((addr, idx) => `
                <div class="relative">
                    <input type="radio" name="address" id="address_addr_${addr.id || idx}" data-address-id="${addr.id || ''}" data-address-summary="${escapeHtml(formatAddressSummary(addr))}" class="radio-card-input" ${(String(addr.id) === String(checkedId) || (!checkedId && defaultAddress && defaultAddress.id === addr.id)) ? 'checked' : ''}>
                    <label for="address_addr_${addr.id || idx}" class="radio-card-label bg-white p-4 rounded-xl">
                        <div class="radio-circle mt-1 mr-4"></div>
                        <div class="grow">
                            <div class="flex items-center gap-3 mb-1">
                                <h3 class="font-bold text-brand-dark text-base">${addr.recipientName || ''}</h3>
                                <span class="text-gray-400 font-medium text-sm">|</span>
                                <span class="font-bold text-gray-600 text-sm">${addr.recipientPhone || ''}</span>
                                ${addr.isDefault ? '<span class="bg-brand-orange text-white text-[10px] font-black px-2 py-0.5 rounded ml-auto uppercase shadow-sm">Mặc Định</span>' : ''}
                            </div>
                            <div class="text-sm text-gray-600 font-medium leading-relaxed">${[addr.addressLine, addr.ward, addr.district, addr.province].filter(Boolean).join(', ')}</div>
                            <div class="text-[11px] text-gray-400 mt-1">${addr.addressType === 'OFFICE' ? 'Văn phòng' : 'Nhà riêng'}</div>
                        </div>
                    </label>
                </div>
            `).join('');

        } catch (e) {
            console.error(e);
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

    const setDefaultAddress = async (addressId) => {
        try {
            const headers = getHeadersForProfile();
            const res = await fetch(`/buyer/profile/api/addresses/${addressId}/set-default`, { method: 'POST', headers });
            if (!res.ok) throw new Error('Không thể đặt mặc định');
            return await res.text();
        } catch (e) { throw e; }
    };

    // --- EVENT LISTENERS ---

    // Nút Lưu Địa Chỉ
    saveAddrBtn?.addEventListener('click', async () => {
        try {
            // Lấy loại địa chỉ
            let addrType = 'HOME';
            const addrTypeInputs = document.getElementsByName('addr_type');
            if (addrTypeInputs && addrTypeInputs[1] && addrTypeInputs[1].checked) addrType = 'OFFICE';

            const payload = {
                recipientName: fullnameInput?.value || '',
                recipientPhone: phoneInput?.value || '',
                province: provinceSelect?.value || '',
                district: districtSelect?.value || '',
                ward: wardSelect?.value || '',
                addressLine: addressDetailInput?.value || '',
                addressType: addrType,
                isDefault: saveDefaultCheckbox?.checked || false
            };

            // Xác thực dữ liệu
            if (!payload.recipientName || !payload.recipientPhone || !payload.province || !payload.district || !payload.ward || !payload.addressLine) {
                alert('Vui lòng điền và chọn đầy đủ thông tin địa chỉ!');
                return;
            }

            const created = await createAddress(payload);
            if (created?.id && payload.isDefault) {
                await setDefaultAddress(created.id);
            }
            await fetchAddresses();

            // Xóa form sau khi lưu
            fullnameInput.value = ''; phoneInput.value = ''; addressDetailInput.value = '';
            provinceSelect.selectedIndex = 0; districtSelect.innerHTML = '<option value="" disabled selected>Chọn Quận / Huyện</option>'; wardSelect.innerHTML = '<option value="" disabled selected>Chọn Phường / Xã</option>';
            saveDefaultCheckbox.checked = false;

            alert('Đã lưu địa chỉ thành công.');
        } catch (err) {
            console.error(err);
            alert(err?.message || 'Lưu địa chỉ thất bại');
        }
    });

    // Khi người dùng click chọn 1 địa chỉ đã lưu
    addressesContainer?.addEventListener('change', (event) => {
        const radio = event.target.closest('input[name="address"]');
        if (!radio) return;
        // Chúng ta không cần auto-fill form khi chọn radio button, vì logic deriveAddress đã ưu tiên lấy radio
    });

    // Cập nhật phí vận chuyển khi chọn
    document.querySelectorAll('input[name="shipping"]').forEach((radio) => {
        radio.addEventListener('change', () => {
            if (currentCart) updateSummary(currentCart);
        });
    });

    // Logic Mã giảm giá - gửi kèm sellerIds để chống cross-seller
    applyCouponBtn?.addEventListener('click', async () => {
        const code = (couponInput?.value || '').trim().toUpperCase();
        if (!code) return alert('Nhập mã khuyến mãi');

        const subtotal = Number(currentCart?.totalAmount || 0);
        const sellerIds = getSellerIdsFromCart();

        try {
            let url = `/api/coupons/${code}/validate?orderAmount=${subtotal}`;
            if (sellerIds) {
                url += `&sellerIds=${sellerIds}`;
            }
            const res = await fetch(url);
            const data = await res.json();

            if (!data.valid) {
                return alert(data.error || 'Mã không hợp lệ');
            }

            appliedCoupon = data;
            appliedCouponText.textContent = `${data.code} (Giảm ${formatVnd(data.discount)})`;
            appliedCouponWrap.classList.remove('hidden');
            clearCouponBtn?.classList.remove('hidden');
            updateSummary(currentCart || {});
        } catch (err) {
            alert('Không thể kiểm tra mã giảm giá');
        }
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

    // --- THANH TOÁN (PLACE ORDER) ---
    placeOrderBtn?.addEventListener('click', async () => {
        try {
            const { addressLine, addressId } = deriveAddress();

            // Check nếu các trường trống
            if (!addressLine || addressLine.split('•').length < 3) {
                alert('Vui lòng chọn địa chỉ đã lưu hoặc điền đầy đủ form địa chỉ mới.');
                return;
            }

            const paymentMethod = document.querySelector('input[name="payment"]:checked')?.id;
            const saveDefault = saveDefaultCheckbox?.checked;

            // Nếu người dùng nhập form mới và ấn "Lưu mặc định"
            if (saveDefault && !addressId) {
                let addrType = 'HOME';
                const addrTypeInputs = document.getElementsByName('addr_type');
                if (addrTypeInputs && addrTypeInputs[1] && addrTypeInputs[1].checked) addrType = 'OFFICE';

                const payload = {
                    recipientName: fullnameInput?.value || '',
                    recipientPhone: phoneInput?.value || '',
                    province: provinceSelect?.value || '',
                    district: districtSelect?.value || '',
                    ward: wardSelect?.value || '',
                    addressLine: addressDetailInput?.value || addressLine,
                    addressType: addrType
                };

                if (payload.province && payload.district) {
                    const created = await createAddress(payload);
                    if (created?.id) await setDefaultAddress(created.id);
                }
            }

            // Gọi API tạo Order
            const response = await ApiService.Order.checkout(addressLine, appliedCoupon?.code);
            const orderId = response?.orderId;
            localStorage.setItem('lastOrderId', String(orderId || ''));

            // Chuyển hướng VNPay hoặc Trả tiền mặt (COD)
            if (paymentMethod === 'pay_vnpay') {
                await handleVNPayPayment(orderId);
            } else {
                window.location.href = orderId ? `/main/order-success?orderId=${orderId}` : '/main/order-success';
            }
        } catch (error) {
            const message = error?.message || 'Đặt hàng thất bại.';
            alert(message);
        }
    });

    const handleVNPayPayment = async (orderId) => {
        try {
            if (!orderId) throw new Error('Không có mã đơn hàng');

            const paymentRequest = {
                orderId: orderId,
                paymentMethod: 'VNPAY',
                returnUrl: `${window.location.origin}/main/payment-result`
            };

            const response = await fetch('/api/payment/vnpay/init', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${ApiService.getAuth().token || ''}`
                },
                body: JSON.stringify(paymentRequest)
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Không thể khởi tạo thanh toán VNPay');
            }

            const paymentData = await response.json();
            if (paymentData.paymentUrl) {
                window.location.href = paymentData.paymentUrl;
            } else {
                throw new Error('Không nhận được URL thanh toán từ VNPay');
            }
        } catch (error) {
            console.error('VNPay payment error:', error);
            alert('Lỗi thanh toán VNPay: ' + (error?.message || 'Vui lòng thử lại'));
        }
    };

    // --- BOOTSTRAP ---
    loadLocations();
    fetchAddresses();
    fetchCart().catch((error) => {
        const message = error?.message || 'Không thể tải dữ liệu thanh toán.';
        itemsEl.innerHTML = `<div class="text-center text-red-600 font-semibold py-6">${message}</div>`;
        updateSummary({ totalItems: 0, totalAmount: 0 });
    });

})();