package com.revpay.wallet_service.exception;

import com.revpay.wallet_service.dto.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException e) {
        log.warn("Insufficient balance: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
        log.error("Feign client error: status={}, message={}", e.status(), e.getMessage());
        
        String message = "External service communication error";
        String body = e.contentUTF8();
        
        if (e.status() == 400 || e.status() == 422) {
            if (body != null) {
                if (body.toLowerCase().contains("insufficient balance") || body.toLowerCase().contains("insufficient funds")) {
                    message = "Insufficient balance in wallet!";
                } else if (body.contains("message\":\"")) {
                    try {
                        int start = body.indexOf("message\":\"") + 10;
                        int end = body.indexOf("\"", start);
                        if (start > 9 && end > start) {
                            message = body.substring(start, end);
                        }
                    } catch (Exception ex) {
                        message = body;
                    }
                } else {
                    message = body;
                }
            }
        }
        
        return ResponseEntity.status(e.status() > 0 ? e.status() : 500)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        if (e.getClass().getName().contains("NoFallbackAvailableException") && e.getCause() instanceof FeignException) {
            return handleFeignException((FeignException) e.getCause());
        }
        log.warn("Runtime error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred."));
    }
}
