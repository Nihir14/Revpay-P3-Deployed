package com.revpay.invoice_service.client;

import com.revpay.invoice_service.dto.ApiResponse;
import com.revpay.invoice_service.dto.BusinessProfileDto;
import com.revpay.invoice_service.dto.VerifyPinRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/api/v1/business/profile/user/{userId}")
    ApiResponse<BusinessProfileDto> getBusinessProfile(@PathVariable("userId") Long userId);

    @PostMapping("/api/v1/auth/verify-pin")
    void verifyPin(@RequestParam("userId") Long userId, @RequestBody VerifyPinRequest request);

    @GetMapping("/api/v1/users/resolve")
    ApiResponse<Long> resolveUser(@RequestParam("identifier") String identifier);
}