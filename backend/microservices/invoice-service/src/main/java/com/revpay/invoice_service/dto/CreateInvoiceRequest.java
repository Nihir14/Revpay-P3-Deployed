package com.revpay.invoice_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateInvoiceRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    @NotNull(message = "Total amount is required")
    @Min(value = 1, message = "Amount must be at least ₹1")
    private BigDecimal totalAmount;

    private LocalDate dueDate;

    @NotNull(message = "Items list cannot be null")
    private java.util.List<InvoiceLineItemDto> items;
}
