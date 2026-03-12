package com.revpay.transaction_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "wallet-service", fallback = WalletClientFallback.class)
public interface WalletClient {

    @PostMapping("/api/wallets/user/{userId}/debit")
    ResponseEntity<Void> debitFunds(@PathVariable("userId") Long userId, @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/wallets/user/{userId}/credit")
    ResponseEntity<Void> creditFunds(@PathVariable("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}
