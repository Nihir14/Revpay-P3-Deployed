package com.revpay.transaction_service.controller;

import com.revpay.transaction_service.dto.ApiResponse;
import com.revpay.transaction_service.dto.BusinessSummaryDTO;
import com.revpay.transaction_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
@RequestMapping("/api/v1/business/analytics")
@RequiredArgsConstructor
@Tag(name = "Business Analytics", description = "Endpoints for business financial summary and reports")
public class BusinessAnalyticsController {

    private final TransactionService transactionService;

    @GetMapping("/{businessId}/summary")
    @Operation(summary = "Get business transaction summary", description = "Retrieves a high-level overview of transaction volumes and revenue.")
    public ResponseEntity<ApiResponse<BusinessSummaryDTO>> getSummary(@PathVariable Long businessId) {
        log.debug("API_REQUEST | Summary for Business ID: {}", businessId);
        BusinessSummaryDTO summary = transactionService.getBusinessSummary(businessId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Business summary retrieved successfully"));
    }
}
