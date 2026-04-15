document.addEventListener('DOMContentLoaded', async () => {
    // 1. Lấy Elements theo ID chuẩn
    const el = {
        title: document.getElementById('book-title'),
        author: document.getElementById('book-author'),
        categoryId: document.getElementById('book-category'),
        description: document.getElementById('book-description'),
        price: document.getElementById('book-price'),
        stock: document.getElementById('book-stock'),
        saveBtn: document.getElementById('btn-save-book'),
        pageTitle: document.getElementById('page-title'),
        skuDisplay: document.getElementById('sku-display'), // THÊM DẤU PHẨY Ở ĐÂY
        image: document.getElementById('book-image')
    };

    // MẠNG XỬ LÝ PREVIEW ẢNH KHI CHỌN FILE
    const imagePreview = document.getElementById('image-preview');
    const imagePlaceholder = document.getElementById('image-placeholder');

    if (el.image) {
        el.image.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    if (imagePreview) {
                        imagePreview.src = e.target.result;
                        imagePreview.classList.remove('hidden');
                    }
                    if (imagePlaceholder) {
                        imagePlaceholder.classList.add('hidden');
                    }
                }
                reader.readAsDataURL(file);
            }
        });
    }

    const urlParams = new URLSearchParams(window.location.search);
    const bookId = urlParams.get('id');

    // 2. Load danh mục từ API
    async function initCategories() {
        try {
            const categories = await ApiService.Category.getAll();
            if (el.categoryId && categories.length > 0) {
                el.categoryId.innerHTML = '<option value="">-- Chọn danh mục --</option>' +
                    categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
            }
        } catch (e) {
            console.error("❌ Không load được danh mục");
        }
    }

    // 3. Xử lý chế độ SỬA hoặc THÊM
    async function initForm() {
        await initCategories();

        if (bookId) {
            el.pageTitle.textContent = "Chỉnh sửa sách";
            el.skuDisplay.textContent = `Mã sách (ID): ${bookId}`;
            try {
                const book = await ApiService.Book.getById(bookId);
                // Đổ data text vào
                el.title.value = book.title || '';
                el.author.value = book.author || '';
                el.price.value = book.price || 0;
                el.stock.value = book.stock || 0;
                el.categoryId.value = book.categoryId || '';
                el.description.value = book.description || '';

                // Đổ ảnh cũ vào thẻ <img> để preview (không được gán vào thẻ input file)
                if (book.imageUrl && imagePreview && imagePlaceholder) {
                    imagePreview.src = book.imageUrl;
                    imagePreview.classList.remove('hidden');
                    imagePlaceholder.classList.add('hidden');
                }
            } catch (error) {
                alert('❌ Lỗi: Không lấy được thông tin sách');
            }
        } else {
            el.pageTitle.textContent = "Thêm sách mới";
            el.skuDisplay.textContent = "Hệ thống sẽ tự cấp ID";
        }
    }

    // 4. Sự kiện Lưu bằng FormData
    if (el.saveBtn) {
        el.saveBtn.addEventListener('click', async (e) => {
            e.preventDefault();

            // Validate cơ bản
            if (!el.title.value.trim() || !el.price.value || el.stock.value === '') {
                alert("⚠️ Vui lòng nhập đủ: Tên sách, Giá và Tồn kho!");
                return;
            }

            const originalBtnText = el.saveBtn.textContent;
            el.saveBtn.textContent = "⌛ Đang xử lý...";
            el.saveBtn.disabled = true;

            try {
                // CHUẨN FORMDATA DÀNH CHO UPLOAD FILE
                const formData = new FormData();
                formData.append('title', el.title.value.trim());
                formData.append('author', el.author.value.trim() || "Chưa rõ");
                formData.append('price', parseFloat(el.price.value));
                formData.append('stock', parseInt(el.stock.value));

                if (el.categoryId.value) {
                    formData.append('categoryId', parseInt(el.categoryId.value));
                }
                formData.append('description', el.description.value.trim());

                // Xử lý ném file ảnh vào FormData
                const imageFile = el.image.files[0];
                if (imageFile) {
                    // Nếu có chọn ảnh mới thì đính kèm vào
                    formData.append('file', imageFile);
                } else if (!bookId) {
                    // Nếu là Thêm Mới mà không có ảnh thì chặn lại ngay
                    alert("⚠️ Vui lòng chọn ảnh bìa cho sách mới!");
                    el.saveBtn.textContent = originalBtnText;
                    el.saveBtn.disabled = false;
                    return;
                }

                // Call API
                if (bookId) {
                    await ApiService.Book.update(bookId, formData);
                    alert("✅ Cập nhật sách thành công!");
                } else {
                    await ApiService.Book.create(formData);
                    alert("✅ Thêm sách mới thành công!");
                }

                // Trở về trang quản lý kho
                window.location.href = '/seller/inventory';

            } catch (err) {
                alert("❌ Lỗi: " + (err.response?.data?.message || "Không thể lưu sách"));
                console.error(err);
            } finally {
                el.saveBtn.textContent = originalBtnText;
                el.saveBtn.disabled = false;
            }
        });
    }

    initForm();
});