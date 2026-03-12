package com.revpay.invoice_service.controller;

import com.revpay.invoice_service.dto.*;
import com.revpay.invoice_service.entity.BusinessProfile;
import com.revpay.invoice_service.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Management", description = "Endpoints for business profiles and invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        super();
        this.invoiceService = invoiceService;
    }

    @PostMapping("/profile/{userId}")
    @Operation(summary = "Create or update business profile")
    public ResponseEntity<ApiResponse<BusinessProfileDto>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody BusinessProfile profile) {
        return ResponseEntity
                .ok(ApiResponse.success(invoiceService.createOrUpdateProfile(userId, profile), "Profile updated"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get business profile")
    public ResponseEntity<ApiResponse<BusinessProfileDto>> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getProfile(userId), "Profile retrieved"));
    }

    @PostMapping("/{userId}")
    @Operation(summary = "Create new invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(
            @PathVariable Long userId,
            @Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.createInvoice(userId, request), "Invoice created"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all invoices for business")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoices(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoices(userId), "Invoices retrieved"));
    }

    @GetMapping("/customer/pending")
    @Operation(summary = "Get pending invoices for customer")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getPendingInvoices(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getPendingInvoices(email), "Pending invoices retrieved"));
    }

    @PostMapping("/pay/{userId}/{invoiceId}")
    @Operation(summary = "Pay an invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> payInvoice(@PathVariable Long userId, @PathVariable Long invoiceId, @RequestParam String pin) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.payInvoice(userId, invoiceId, pin), "Invoice paid successfully"));
    }
}
