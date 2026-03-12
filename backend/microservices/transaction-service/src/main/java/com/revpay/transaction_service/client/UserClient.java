package com.revpay.transaction_service.client;

import com.revpay.transaction_service.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/api/v1/users/resolve")
    ApiResponse<Long> resolveUser(@RequestParam("identifier") String identifier);
}
