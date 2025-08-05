package com.platform.coding.domain.enrollment;

public enum EnrollmentStatus {
    PENDING_PAYMENT,    // 결제 대기 중 (주문 생성 후 결제 전)
    IN_PROGRESS,        // 수강 중
    COMPLETED,          // 수강 완료
    CANCELLED,          // 사용자 또는 관리자에 의해 취소됨
    REFUND_REQUESTED,   // 환불 요청됨
    REFUNDED            // 환불 완료됨
}
