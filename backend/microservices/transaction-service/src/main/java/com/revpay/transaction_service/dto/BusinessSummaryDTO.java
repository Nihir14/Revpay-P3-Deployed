package com.revpay.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSummaryDTO {
    private BigDecimal totalReceived;
    private BigDecimal totalSent;
    private BigDecimal pendingAmount;
    private int totalTransactionCount;
    private String currency;
}
