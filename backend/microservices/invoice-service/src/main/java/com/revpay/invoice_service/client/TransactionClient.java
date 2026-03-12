package com.revpay.invoice_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "transaction-service")
public interface TransactionClient {
    @PostMapping("/api/transactions/internal/record")
    void recordTransaction(@RequestParam("userId") Long userId, 
                          @RequestParam("amount") BigDecimal amount, 
                          @RequestParam("type") String type, 
                          @RequestParam(value = "description", required = false) String description,
                          @RequestParam(value = "receiverId", required = false) Long receiverId);
}
