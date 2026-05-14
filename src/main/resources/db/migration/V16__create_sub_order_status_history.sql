-- V16: Create sub_order_status_history table for logging status changes
-- Mục đích: Lưu lịch sử thay đổi trạng thái sub_order để đối chiếu khi có tranh chấp

IF OBJECT_ID('sub_order_status_history', 'U') IS NULL
BEGIN
CREATE TABLE sub_order_status_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sub_order_id BIGINT NOT NULL,
    from_status NVARCHAR(30) NULL,
    to_status NVARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT NULL,
    changed_by_role NVARCHAR(20) NULL, -- BUYER, SELLER, SYSTEM, ADMIN
    note NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT FK_status_history_sub_order FOREIGN KEY (sub_order_id) REFERENCES sub_orders(id),
    CONSTRAINT FK_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);

-- Index để truy vấn lịch sử nhanh
CREATE INDEX IX_status_history_sub_order_id ON sub_order_status_history(sub_order_id);
CREATE INDEX IX_status_history_created_at ON sub_order_status_history(created_at DESC);
END;
