package com.revpay.loan_service.client;

import com.revpay.loan_service.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/api/v1/auth/verify-pin")
    ResponseEntity<ApiResponse<Boolean>> verifyPin(@RequestParam("userId") Long userId, @RequestBody Map<String, String> body);

    @GetMapping("/api/v1/users/{userId}/name")
    String getUserName(@PathVariable("userId") Long userId);
}
