package com.revpay.client;

import com.revpay.model.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "loan-service")
public interface LoanClient {

    @PostMapping("/api/loans/approve/{loanId}")
    ResponseEntity<ApiResponse<Object>> approveLoan(@PathVariable("loanId") Long loanId, @RequestParam("interestRate") BigDecimal interestRate);
    
    // Fallback for getting all loans if needed by admin
    @GetMapping("/api/loans/all")
    ResponseEntity<ApiResponse<Page<Object>>> getAllLoans(@RequestParam("page") int page, @RequestParam("size") int size);
}
