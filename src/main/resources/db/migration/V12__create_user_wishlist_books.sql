IF OBJECT_ID('user_wishlist_books', 'U') IS NULL
BEGIN
CREATE TABLE user_wishlist_books (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_user_wishlist_books PRIMARY KEY (user_id, book_id),
    CONSTRAINT FK_user_wishlist_books_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_wishlist_books_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);
END;