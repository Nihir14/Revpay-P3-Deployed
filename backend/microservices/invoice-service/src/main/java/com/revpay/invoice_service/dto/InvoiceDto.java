package com.revpay.invoice_service.dto;

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
public class InvoiceDto {
    private Long id;
    private Long businessId;
    private String businessName;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private String status;
    private LocalDateTime createdAt;
    private Long linkedTransactionId;
    private java.util.List<InvoiceLineItemDto> items;
}
