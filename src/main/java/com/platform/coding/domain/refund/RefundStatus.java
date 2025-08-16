package com.platform.coding.domain.refund;

public enum RefundStatus {
    REQUESTED,  // 환불 요청됨
    PROCESSING, // 환불 처리 중
    COMPLETED,  // 환불 완료
    REJECTED    // 환불 거절
}
