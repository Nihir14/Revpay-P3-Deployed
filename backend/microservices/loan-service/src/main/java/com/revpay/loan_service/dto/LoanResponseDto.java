package com.revpay.loan_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDto {
    private Long loanId;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal remainingAmount;
    private String status;
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private java.util.List<LoanDocumentDto> documents;
}
