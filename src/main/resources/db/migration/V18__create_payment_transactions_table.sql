-- V18__create_payment_transactions_table.sql
-- Create payment transactions table for VNPay integration

CREATE TABLE payment_transactions (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    order_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_code VARCHAR(100),
    payment_url VARCHAR(1000),
    response_code VARCHAR(1000),
    response_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    expired_at TIMESTAMP,
    failure_reason VARCHAR(500),
    CONSTRAINT fk_payment_transactions_order_id 
        FOREIGN KEY (order_id) 
        REFERENCES orders_master(id) ON DELETE CASCADE,
    CONSTRAINT uk_payment_transaction_code 
        UNIQUE (transaction_code),
    CONSTRAINT idx_payment_order_status 
        UNIQUE NONCLUSTERED (order_id, status)
);

-- Index for performance
CREATE INDEX idx_payment_status ON payment_transactions(status);
CREATE INDEX idx_payment_created_at ON payment_transactions(created_at DESC);
