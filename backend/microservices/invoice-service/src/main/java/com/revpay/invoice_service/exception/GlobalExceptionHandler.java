package com.revpay.invoice_service.exception;

import com.revpay.invoice_service.dto.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
        log.error("Feign client error: status={}, message={}", e.status(), e.getMessage());
        
        String message = "Service communication error";
        String body = e.contentUTF8();
        
        if (e.status() == 400 || e.status() == 422 || e.status() == 402) { // 402 is Payment Required, sometimes used for balance
            if (body != null) {
                if (body.toLowerCase().contains("insufficient balance") || body.toLowerCase().contains("insufficient funds")) {
                    message = "Insufficient balance in your wallet to pay this invoice.";
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
        } else if (e.status() == 401) {
            message = "Authentication failed with internal service.";
        } else if (e.status() == 404) {
            message = "Requested information not found.";
        }
        
        return ResponseEntity.status(e.status() > 0 ? e.status() : 500)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        if (e.getClass().getName().contains("NoFallbackAvailableException") && e.getCause() instanceof FeignException) {
            return handleFeignException((FeignException) e.getCause());
        }
        log.warn("Business error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected system error: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
