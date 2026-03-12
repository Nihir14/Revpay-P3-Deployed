package com.revpay.transaction_service.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletClientFallback implements WalletClient {

    public WalletClientFallback() {
        super();
    }

    @Override
    public ResponseEntity<Void> debitFunds(Long userId, BigDecimal amount) {
        throw new RuntimeException("Wallet Service is currently unavailable. Debit failed.");
    }

    @Override
    public ResponseEntity<Void> creditFunds(Long userId, BigDecimal amount) {
        throw new RuntimeException("Wallet Service is currently unavailable. Credit failed.");
    }
}
