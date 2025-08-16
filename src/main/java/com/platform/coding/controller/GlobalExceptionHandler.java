package com.platform.coding.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Spring Security의 @PreAuthorize 등에서 권한 부족으로 발생하는 예외를 처리합니다.
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        ErrorResponse response = new ErrorResponse("ACCESS_DENIED", "요청에 대한 접근 권한이 없습니다.");
        // 권한이 없음을 의미하는 403 Forbidden을 반환합니다.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 우리가 직접 발생시킨 비즈니스 예외 (예: "존재하지 않는 강의입니다.") 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse response = new ErrorResponse("INVALID_INPUT", e.getMessage());
        // 클라이언트의 요청이 잘못되었음을 의미하는 400 Bad Request를 반환합니다.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // 부적절한 상태에서 요청이 들어왔을 때 (예: "완강하지 않은 강의에 리뷰 작성") 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        ErrorResponse response = new ErrorResponse("INVALID_STATE", e.getMessage());
        // 클라이언트의 요청이 현재 리소스의 상태와 맞지 않음을 의미하는 400 Bad Request를 반환
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 유효성 검증 실패 시 첫 번째 에러 메시지를 응답으로 사용
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponse response = new ErrorResponse("VALIDATION_FAILED", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 서버 내부 오류이므로 500 Internal Error를 반환한다.
        // 실제 운영 환경에서는 로그를 더 상세히 남겨야 함.
        ErrorResponse response = new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
        log.error("e: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
