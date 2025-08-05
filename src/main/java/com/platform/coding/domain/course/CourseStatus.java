package com.platform.coding.domain.course;

public enum CourseStatus {
    DRAFT, // 초안 (관리자만 볼 수 있으며, 사용자에게 노출되지 않음)
    PUBLISHED, // 게시됨 (사용자가 보고 구매할 수 있는 상태)
    ARCHIVED // 보관됨 (더 이상 판매하지 않지만, 기존 구매자는 수강 가능)
}
