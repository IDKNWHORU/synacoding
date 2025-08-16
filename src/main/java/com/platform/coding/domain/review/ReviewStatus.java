package com.platform.coding.domain.review;

public enum ReviewStatus {
    PENDING_APPROVAL,   // 승인 대기
    PUBLISHED,          // 게시됨
    REPORTED,           // 신고됨
    HIDDEN              // 숨김 처리 (관리자에 의해)
}
