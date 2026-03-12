package com.revpay.invoice_service.service;

import com.revpay.invoice_service.client.NotificationClient;
import com.revpay.invoice_service.client.TransactionClient;
import com.revpay.invoice_service.client.UserClient;
import com.revpay.invoice_service.client.WalletClient;
import com.revpay.invoice_service.dto.BusinessProfileDto;
import com.revpay.invoice_service.dto.CreateInvoiceRequest;
import com.revpay.invoice_service.dto.InvoiceDto;
import com.revpay.invoice_service.entity.BusinessProfile;
import com.revpay.invoice_service.entity.Invoice;
import com.revpay.invoice_service.repository.BusinessProfileRepository;
import com.revpay.invoice_service.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private WalletClient walletClient;

    @Mock
    private TransactionClient transactionClient;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void createOrUpdateProfile_Success() {
        BusinessProfile profile = new BusinessProfile();
        profile.setBusinessName("Test Business");

        BusinessProfile savedProfile = new BusinessProfile();
        savedProfile.setProfileId(1L);
        savedProfile.setUserId(2L);
        savedProfile.setBusinessName("Test Business");

        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(savedProfile);

        BusinessProfileDto result = invoiceService.createOrUpdateProfile(2L, profile);

        assertNotNull(result);
        assertEquals(1L, result.getProfileId());
        assertEquals(2L, result.getUserId());
        assertEquals("Test Business", result.getBusinessName());
    }

    @Test
    void getProfile_Success() {
        BusinessProfile profile = new BusinessProfile();
        profile.setProfileId(1L);
        profile.setUserId(2L);
        when(businessProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));

        BusinessProfileDto result = invoiceService.getProfile(2L);

        assertNotNull(result);
        assertEquals(1L, result.getProfileId());
    }

    @Test
    void createInvoice_ExistingProfile_Success() {
        BusinessProfile profile = new BusinessProfile();
        profile.setProfileId(1L);
        profile.setUserId(2L);
        when(businessProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(profile));

        Invoice savedInvoice = new Invoice();
        savedInvoice.setId(10L);
        savedInvoice.setBusinessProfile(profile);
        savedInvoice.setTotalAmount(new BigDecimal("100.00"));
        savedInvoice.setStatus(Invoice.InvoiceStatus.SENT);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerName("Customer");
        request.setCustomerEmail("cust@test.com");
        request.setItems(Collections.emptyList());

        InvoiceDto result = invoiceService.createInvoice(2L, request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("SENT", result.getStatus());
        verify(notificationClient, times(1)).sendNotification(eq(2L), anyString());
    }

    @Test
    void payInvoice_Success() {
        BusinessProfile profile = new BusinessProfile();
        profile.setProfileId(1L);
        profile.setUserId(2L);

        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setBusinessProfile(profile);
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setStatus(Invoice.InvoiceStatus.SENT);

        when(invoiceRepository.findById(anyLong())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceDto result = invoiceService.payInvoice(3L, 10L, "1234");

        assertNotNull(result);
        assertEquals("PAID", result.getStatus());
        verify(userClient, times(1)).verifyPin(eq(3L), any());
        verify(walletClient, times(1)).debitFunds(eq(3L), eq(new BigDecimal("100.00")));
        verify(walletClient, times(1)).creditFunds(eq(2L), eq(new BigDecimal("100.00")));
        verify(transactionClient, times(1)).recordTransaction(eq(3L), eq(new BigDecimal("100.00")), anyString(),
                anyString(), eq(2L));
        verify(notificationClient, times(1)).sendNotification(eq(2L), anyString());
    }
}
