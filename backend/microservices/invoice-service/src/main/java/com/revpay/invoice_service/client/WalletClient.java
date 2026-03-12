package com.revpay.invoice_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "wallet-service")
public interface WalletClient {
    @PostMapping("/api/wallets/user/{userId}/debit")
    void debitFunds(@PathVariable("userId") Long userId, @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/wallets/user/{userId}/credit")
    void creditFunds(@PathVariable("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}
