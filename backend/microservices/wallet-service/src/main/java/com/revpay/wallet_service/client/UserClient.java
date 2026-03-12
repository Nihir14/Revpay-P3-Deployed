package com.revpay.wallet_service.client;

import com.revpay.wallet_service.dto.ApiResponse;
import com.revpay.wallet_service.dto.VerifyPinRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserClient {
    @PostMapping("/api/v1/auth/verify-pin")
    void verifyPin(@RequestParam("userId") Long userId, @RequestBody VerifyPinRequest request);
}
