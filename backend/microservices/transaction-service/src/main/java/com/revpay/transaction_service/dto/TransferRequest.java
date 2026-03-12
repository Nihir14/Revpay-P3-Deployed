package com.revpay.transaction_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    private Long receiverId;
    private String receiverIdentifier;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum amount is ₹1")
    private BigDecimal amount;

    private String description;

    @NotNull(message = "Transaction PIN is required")
    private String transactionPin;
}
