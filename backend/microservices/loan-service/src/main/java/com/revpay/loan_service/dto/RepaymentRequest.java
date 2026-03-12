package com.revpay.loan_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RepaymentRequest {
    private Long loanId;
    private BigDecimal amount;
    private String transactionPin;
    private boolean isFullForeclosure;
    private String idempotencyKey;
}
