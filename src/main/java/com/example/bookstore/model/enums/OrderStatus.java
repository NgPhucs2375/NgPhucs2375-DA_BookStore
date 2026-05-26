package com.example.bookstore.model.enums;

public enum OrderStatus {
    PENDING_PAYMENT,//
    PROCESSING,// đã xác nhận
    COMFIRMED,
    SHIPPING, // ĐANG GIAO
    COMPLETED,// ĐÃ HOÀN THÀNH
    CANCELLED// ĐÃ HỦY
}
