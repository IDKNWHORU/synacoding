package com.platform.coding.domain.submission;

public enum SubmissionStatus {
    PENDING, // 과제가 할당되었으나 아직 제출되지 않음 (이 상태는 별도 테이블이 없다면 사용하기 어려울 수 있음)
    SUBMITTED, // 제출됨 (채점 대기)
    GRADING, // 체점 중
    GRADED // 체점 완료
}
