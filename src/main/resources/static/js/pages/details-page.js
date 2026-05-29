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

    // ==================== REVIEW SYSTEM ====================
    
    const bookId = Number(addButton.getAttribute('data-book-id'));
    let currentReviewPage = 0;
    const reviewSize = 10;

    const fetchReviewStats = async () => {
        try {
            const response = await fetch(`/api/reviews/book/${bookId}/stats`);
            if (response.ok) {
                const stats = await response.json();
                document.getElementById('avg-rating-text').textContent = stats.averageRating.toFixed(1);
                document.getElementById('total-reviews-text').textContent = stats.totalReviews;
                
                // Update stars in summary
                const starsContainer = document.getElementById('avg-stars-container');
                starsContainer.innerHTML = '';
                const fullStars = Math.floor(stats.averageRating);
                for (let i = 0; i < 5; i++) {
                    const star = document.createElement('span');
                    star.textContent = '★';
                    if (i < fullStars) {
                        star.className = 'text-yellow-400';
                    } else {
                        star.className = 'text-gray-300';
                    }
                    starsContainer.appendChild(star);
                }
            }
        } catch (error) {
            console.error('Error fetching review stats:', error);
        }
    };

    const fetchReviews = async (page = 0, append = false) => {
        const container = document.getElementById('reviews-list-container');
        const loadMoreBtnContainer = document.getElementById('load-more-reviews-container');
        
        try {
            const response = await fetch(`/api/reviews/book/${bookId}?page=${page}&size=${reviewSize}`);
            if (response.ok) {
                const data = await response.json();
                const reviews = data.content;
                
                if (!append) {
                    container.innerHTML = '';
                } else {
                    document.getElementById('reviews-loading')?.remove();
                }

                if (reviews.length === 0 && !append) {
                    container.innerHTML = '<div class="py-10 text-center text-gray-400 italic">Chưa có đánh giá nào cho sản phẩm này.</div>';
                    loadMoreBtnContainer.classList.add('hidden');
                    return;
                }

                reviews.forEach(review => {
                    const reviewEl = document.createElement('div');
                    reviewEl.className = 'py-6 border-b border-brand-border flex gap-4';
                    
                    const initials = review.user.username.substring(0, 1).toUpperCase();
                    const dateStr = new Date(review.createdAt).toLocaleString('vi-VN');
                    
                    reviewEl.innerHTML = `
                        <div class="w-12 h-12 rounded-full bg-brand-hero border border-brand-border flex-shrink-0 flex items-center justify-center font-bold text-brand-brown">${initials}</div>
                        <div class="flex-grow">
                            <div class="flex items-center gap-2 mb-1">
                                <span class="font-bold text-brand-dark text-sm">${review.user.username}</span>
                                <span class="bg-green-100 text-green-700 text-[10px] font-bold px-2 py-0.5 rounded flex items-center gap-1">
                                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg> Đã mua hàng
                                </span>
                            </div>
                            <div class="flex text-yellow-400 text-xs mb-2">${'★'.repeat(review.rating)}${'<span class="text-gray-300">★</span>'.repeat(5 - review.rating)}</div>
                            <div class="text-xs text-gray-400 mb-3">${dateStr}</div>
                            <p class="text-sm text-brand-dark leading-relaxed">${review.comment || 'Người dùng không để lại bình luận.'}</p>
                        </div>
                    `;
                    container.appendChild(reviewEl);
                });

                if (data.last) {
                    loadMoreBtnContainer.classList.add('hidden');
                } else {
                    loadMoreBtnContainer.classList.remove('hidden');
                }
            }
        } catch (error) {
            console.error('Error fetching reviews:', error);
            if (!append) {
                container.innerHTML = '<div class="py-10 text-center text-red-400">Không thể tải đánh giá.</div>';
            }
        }
    };

    const checkReviewEligibility = async () => {
        const { userId, role } = ApiService.getAuth();
        if (userId && role === 'BUYER') {
            // Check if user has purchased this book - In a real app, we'd have an API for this
            // For now, let's just show the form if they are logged in as BUYER
            // The backend will enforce the "has purchased" rule anyway.
            document.getElementById('review-form-container').classList.remove('hidden');
        }
    };

    // Star rating input logic
    const stars = document.querySelectorAll('#star-rating-input .star');
    const ratingInput = document.getElementById('selected-rating');

    stars.forEach(star => {
        star.addEventListener('mouseover', () => {
            const val = parseInt(star.dataset.value);
            stars.forEach((s, i) => {
                if (i < val) s.classList.add('text-yellow-400');
                else s.classList.remove('text-yellow-400');
            });
        });

        star.addEventListener('click', () => {
            const val = parseInt(star.dataset.value);
            ratingInput.value = val;
            stars.forEach((s, i) => {
                if (i < val) {
                    s.classList.add('text-yellow-400');
                    s.classList.add('is-selected');
                } else {
                    s.classList.remove('text-yellow-400');
                    s.classList.remove('is-selected');
                }
            });
        });
    });

    document.getElementById('star-rating-input').addEventListener('mouseleave', () => {
        const currentVal = parseInt(ratingInput.value);
        stars.forEach((s, i) => {
            if (i < currentVal) s.classList.add('text-yellow-400');
            else s.classList.remove('text-yellow-400');
        });
    });

    // Review form submission
    document.getElementById('review-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const rating = parseInt(ratingInput.value);
        const comment = document.getElementById('review-comment').value;

        try {
            const response = await ApiService.fetchWithAuth('/api/reviews', {
                method: 'POST',
                body: JSON.stringify({
                    bookId: bookId,
                    rating: rating,
                    comment: comment
                })
            });

            if (response.ok) {
                alert('Cảm ơn bạn đã đánh giá sản phẩm!');
                document.getElementById('review-form-container').classList.add('hidden');
                // Refresh reviews
                currentReviewPage = 0;
                await fetchReviewStats();
                await fetchReviews(0);
            } else {
                const errorText = await response.text();
                alert(errorText || 'Gửi đánh giá thất bại.');
            }
        } catch (error) {
            alert(error.message || 'Đã có lỗi xảy ra.');
        }
    });

    document.getElementById('load-more-reviews-btn').addEventListener('click', () => {
        currentReviewPage++;
        fetchReviews(currentReviewPage, true);
    });

    // Initialize Review Section
    fetchReviewStats();
    fetchReviews(0);
    checkReviewEligibility();
});
