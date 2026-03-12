package com.revpay.client;

import com.revpay.model.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "transaction-service")
public interface TransactionClient {

    @GetMapping("/api/transactions/analytics")
    org.springframework.http.ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics();

    @GetMapping("/api/transactions/all")
    org.springframework.http.ResponseEntity<ApiResponse<Page<Object>>> getAllTransactions(@RequestParam("page") int page, @RequestParam("size") int size);
}
