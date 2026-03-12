package com.revpay.client;

import com.revpay.model.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "wallet-service")
public interface WalletClient {
    @GetMapping("/api/wallets/user/{userId}")
    ApiResponse<Map<String, Object>> getWallet(@PathVariable("userId") Long userId);
}
