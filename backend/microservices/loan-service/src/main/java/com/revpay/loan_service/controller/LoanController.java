package com.revpay.loan_service.controller;

import com.revpay.loan_service.dto.*;
import com.revpay.loan_service.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loan Management", description = "Endpoints for business loan applications and management")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        super();
        this.loanService = loanService;
    }

    @PostMapping("/apply/{userId}")
    @Operation(summary = "Apply for a new business loan")
    public ResponseEntity<ApiResponse<LoanResponseDto>> applyLoan(
            @PathVariable Long userId,
            @Valid @RequestBody LoanApplyRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success(loanService.applyLoan(userId, request), "Loan applied successfully"));
    }

    @PostMapping("/approve/{loanId}")
    @Operation(summary = "Admin: Approve a loan")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approveLoan(
            @PathVariable Long loanId,
            @RequestParam BigDecimal interestRate) {
        log.info("LOAN_SERVICE | Incoming approveLoan request for ID: {} with rate: {}", loanId, interestRate);
        return ResponseEntity.ok(ApiResponse.success(loanService.approveLoan(loanId, interestRate), "Loan approved"));
    }

    @PostMapping("/approve")
    @Operation(summary = "Admin: Process loan application (Approve/Reject)")
    public ResponseEntity<ApiResponse<LoanResponseDto>> processLoan(@RequestBody LoanApproveRequest request) {
        log.info("LOAN_SERVICE | Incoming processLoan request: {}", request);
        if (request.approved()) {
            return ResponseEntity.ok(ApiResponse.success(
                loanService.approveLoan(request.loanId(), request.approvedInterestRate()), 
                "Loan approved successfully"));
        } else {
            return ResponseEntity.ok(ApiResponse.success(
                loanService.rejectLoan(request.loanId()), 
                "Loan rejected successfully"));
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all loans for a user")
    public ResponseEntity<ApiResponse<List<LoanResponseDto>>> getLoans(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoans(userId), "Loans retrieved"));
    }

    @GetMapping("/all")
    @Operation(summary = "Admin: Get all loan applications")
    public ResponseEntity<ApiResponse<Page<LoanResponseDto>>> getAllLoans(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getAllLoans(pageable), "All loans retrieved"));
    }

    @PostMapping("/repay")
    @Operation(summary = "Repay a loan EMI")
    public ResponseEntity<ApiResponse<String>> repayLoan(@Valid @RequestBody RepaymentRequest request) {
        loanService.repayLoan(request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", "EMI Repayment complete"));
    }

    @PostMapping("/preclose/{loanId}")
    @Operation(summary = "Preclose a loan fully")
    public ResponseEntity<ApiResponse<String>> precloseLoan(@PathVariable Long loanId) {
        loanService.preclose(loanId);
        return ResponseEntity.ok(ApiResponse.success("Loan preclosed successfully", "Loan Settlement complete"));
    }

    @PostMapping(value = "/{loanId}/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a KYC or income document for a loan")
    public ResponseEntity<ApiResponse<LoanResponseDto>> uploadDocument(
            @PathVariable Long loanId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        LoanResponseDto updatedLoan = loanService.uploadDocument(loanId, file);
        return ResponseEntity.ok(ApiResponse.success(updatedLoan, "Document uploaded successfully"));
    }

    @GetMapping("/emi/{loanId}")
    @Operation(summary = "Get EMI schedule for a loan")
    public ResponseEntity<ApiResponse<List<Object>>> getEmiSchedule(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEmiSchedule(loanId), "EMI schedule retrieved"));
    }

    @GetMapping("/analytics/{userId}")
    @Operation(summary = "Get loan analytics for a user")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getLoanAnalytics(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoanAnalytics(userId), "Loan analytics retrieved"));
    }

    @PostMapping("/reject/{loanId}")
    @Operation(summary = "Admin: Reject a loan")
    public ResponseEntity<ApiResponse<LoanResponseDto>> rejectLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.rejectLoan(loanId), "Loan rejected"));
    }
}
