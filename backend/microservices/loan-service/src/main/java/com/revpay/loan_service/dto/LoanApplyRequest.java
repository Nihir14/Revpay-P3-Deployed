package com.revpay.loan_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanApplyRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum loan amount is ₹1000")
    private BigDecimal amount;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    private Integer tenureMonths;

    private String purpose;
    private String loanType;
    private String idempotencyKey;
    private String currency;
}
