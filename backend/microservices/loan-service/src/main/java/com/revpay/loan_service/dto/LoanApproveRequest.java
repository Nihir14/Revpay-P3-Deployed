package com.revpay.loan_service.dto;

import java.math.BigDecimal;

public record LoanApproveRequest(
    Long loanId,
    boolean approved,
    BigDecimal approvedInterestRate,
    String rejectionReason
) {}
