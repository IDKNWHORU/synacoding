package com.platform.coding.domain.payment;

public enum OrderStatus {
    PENDING, // 주문 생성되었으나 결제 미완료
    COMPLETED, // 결제 완료
    FAILED, // 결제 실패
    CANCELD // 주문 취소
}
