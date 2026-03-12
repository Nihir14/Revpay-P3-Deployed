package com.revpay.invoice_service.service;

import com.revpay.invoice_service.client.NotificationClient;
import com.revpay.invoice_service.client.UserClient;
import com.revpay.invoice_service.dto.*;
import com.revpay.invoice_service.entity.BusinessProfile;
import com.revpay.invoice_service.entity.Invoice;
import com.revpay.invoice_service.repository.BusinessProfileRepository;
import com.revpay.invoice_service.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserClient userClient;
    private final NotificationClient notificationClient;
    private final com.revpay.invoice_service.client.WalletClient walletClient;
    private final com.revpay.invoice_service.client.TransactionClient transactionClient;

    public InvoiceService(InvoiceRepository invoiceRepository, BusinessProfileRepository businessProfileRepository, UserClient userClient, NotificationClient notificationClient, com.revpay.invoice_service.client.WalletClient walletClient, com.revpay.invoice_service.client.TransactionClient transactionClient) {
        super();
        this.invoiceRepository = invoiceRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.userClient = userClient;
        this.notificationClient = notificationClient;
        this.walletClient = walletClient;
        this.transactionClient = transactionClient;
    }

    @Transactional
    public BusinessProfileDto createOrUpdateProfile(Long userId, BusinessProfile profile) {
        profile.setUserId(userId);
        BusinessProfile saved = businessProfileRepository.save(profile);
        return mapToProfileDto(saved);
    }

    @Transactional(readOnly = true)
    public BusinessProfileDto getProfile(Long userId) {
        return businessProfileRepository.findByUserId(userId)
                .map(this::mapToProfileDto)
                .orElseThrow(() -> new RuntimeException("Business profile not found"));
    }

    @Transactional
    public InvoiceDto createInvoice(Long userId, CreateInvoiceRequest request) {
        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Syncing business profile for user {} from user-service", userId);
                    ApiResponse<BusinessProfileDto> response = userClient.getBusinessProfile(userId);
                    if (response != null && response.getData() != null) {
                        BusinessProfileDto dto = response.getData();
                        BusinessProfile newProfile = BusinessProfile.builder()
                                .userId(userId)
                                .businessName(dto.getBusinessName())
                                .businessType(dto.getBusinessType())
                                .taxId(dto.getTaxId())
                                .address(dto.getAddress())
                                .isVerified(dto.isVerified())
                                .build();
                        return businessProfileRepository.save(newProfile);
                    }
                    throw new RuntimeException("Business profile required to create invoice and could not be synced");
                });

        Invoice invoice = Invoice.builder()
                .businessProfile(profile)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .dueDate(request.getDueDate())
                .status(Invoice.InvoiceStatus.SENT)
                .build();

        java.util.List<com.revpay.invoice_service.entity.InvoiceLineItem> lineItems = request.getItems().stream()
                .map(dto -> {
                    com.revpay.invoice_service.entity.InvoiceLineItem item = com.revpay.invoice_service.entity.InvoiceLineItem
                            .builder()
                            .invoice(invoice)
                            .description(dto.getDescription())
                            .quantity(dto.getQuantity())
                            .unitPrice(dto.getUnitPrice())
                            .build();
                    return item;
                }).collect(Collectors.toList());

        invoice.setItems(lineItems);

        // Calculate total amount from items if not provided
        java.math.BigDecimal calculatedTotal = lineItems.stream()
                .map(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        invoice.setTotalAmount(request.getTotalAmount() != null ? request.getTotalAmount() : calculatedTotal);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Notify user
        try {
            notificationClient.sendNotification(userId, "New invoice created for " + request.getCustomerName() + " (Amount: ₹" + invoice.getTotalAmount() + ")");
        } catch (Exception ne) {
            log.warn("Failed to send notification: {}", ne.getMessage());
        }

        return mapToInvoiceDto(savedInvoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoices(Long userId) {
        return invoiceRepository.findByBusinessProfileUserId(userId).stream()
                .map(this::mapToInvoiceDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getPendingInvoices(String email) {
        return invoiceRepository.findByCustomerEmail(email).stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.SENT)
                .map(this::mapToInvoiceDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceDto payInvoice(Long userId, Long invoiceId, String pin) {
        log.info("User {} paying invoice {} with PIN", userId, invoiceId);

        // 0. Verify PIN
        userClient.verifyPin(userId, new com.revpay.invoice_service.dto.VerifyPinRequest(pin));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice.getStatus() != Invoice.InvoiceStatus.SENT) {
            throw new RuntimeException("Invoice is not in SENT status");
        }

        // 1. Debit user (This happens in another service, so it commits immediately there)
        walletClient.debitFunds(userId, invoice.getTotalAmount());

        try {
            // 2. Credit business
            walletClient.creditFunds(invoice.getBusinessProfile().getUserId(), invoice.getTotalAmount());

            // 3. Update status
            invoice.setStatus(Invoice.InvoiceStatus.PAID);

            // 4. Record transaction
            try {
                transactionClient.recordTransaction(userId, invoice.getTotalAmount(), "INVOICE_PAYMENT",
                        "Invoice #" + invoiceId + " payment to " + invoice.getBusinessProfile().getBusinessName(),
                        invoice.getBusinessProfile().getUserId());
            } catch (Exception te) {
                log.warn("Failed to record transaction: {}", te.getMessage());
            }

            // 5. Notify business
            try {
                notificationClient.sendNotification(invoice.getBusinessProfile().getUserId(), "Invoice #" + invoiceId + " was paid by " + invoice.getCustomerName());
            } catch (Exception ne) {
                log.warn("Failed to notify business: {}", ne.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to complete invoice payment, initiating refund for user {}: {}", userId, e.getMessage());
            // Compensating transaction to refund the debit if the credit fails
            try {
                walletClient.creditFunds(userId, invoice.getTotalAmount());
                log.info("Refunded ₹{} to user {}", invoice.getTotalAmount(), userId);
            } catch (Exception rollbackEx) {
                log.error("CRITICAL: Failed to refund user {} after invoice payment failed! Amount: {}", userId, invoice.getTotalAmount(), rollbackEx);
            }
            throw new RuntimeException("Invoice payment failed during processing. Your account has been refunded. " + e.getMessage());
        }

        return mapToInvoiceDto(invoiceRepository.save(invoice));
    }

    private InvoiceDto mapToInvoiceDto(Invoice i) {
        return InvoiceDto.builder()
                .id(i.getId())
                .businessId(i.getBusinessProfile().getProfileId())
                .businessName(i.getBusinessProfile().getBusinessName())
                .customerName(i.getCustomerName())
                .customerEmail(i.getCustomerEmail())
                .totalAmount(i.getTotalAmount())
                .dueDate(i.getDueDate())
                .status(i.getStatus().name())
                .createdAt(i.getCreatedAt())
                .linkedTransactionId(i.getLinkedTransactionId())
                .items(i.getItems() != null ? i.getItems().stream().map(item -> InvoiceLineItemDto.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build()).collect(Collectors.toList()) : java.util.Collections.emptyList())
                .build();
    }

    private BusinessProfileDto mapToProfileDto(BusinessProfile p) {
        return BusinessProfileDto.builder()
                .profileId(p.getProfileId())
                .userId(p.getUserId())
                .businessName(p.getBusinessName())
                .businessType(p.getBusinessType())
                .taxId(p.getTaxId())
                .address(p.getAddress())
                .isVerified(p.isVerified())
                .build();
    }
}