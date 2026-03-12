package com.revpay.wallet_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@FeignClient(name = "transaction-service")
public interface TransactionClient {
    @PostMapping("/api/transactions/internal/record")
    void recordTransaction(@RequestParam("userId") Long userId, 
                          @RequestParam("amount") BigDecimal amount, 
                          @RequestParam("type") String type, 
                          @RequestParam("description") String description);

    @PostMapping("/api/transactions/request/{requesterId}")
    void createRequest(@PathVariable("requesterId") Long requesterId, @RequestParam("targetEmail") String targetEmail, @RequestParam("amount") BigDecimal amount);

    @GetMapping("/api/transactions/requests/pending/{userId}")
    java.util.List<Object> getPendingRequests(@PathVariable("userId") Long userId, @RequestParam("incoming") boolean incoming);

    @PostMapping("/api/transactions/request/accept/{txnId}")
    void acceptRequest(@RequestParam("userId") Long userId, @PathVariable("txnId") Long txnId);

    @PostMapping("/api/transactions/request/decline/{txnId}")
    void declineRequest(@RequestParam("userId") Long userId, @PathVariable("txnId") Long txnId);
}
