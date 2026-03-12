package com.revpay.transaction_service.service;

import com.revpay.transaction_service.client.NotificationClient;
import com.revpay.transaction_service.client.UserClient;
import com.revpay.transaction_service.client.WalletClient;
import com.revpay.transaction_service.dto.ApiResponse;
import com.revpay.transaction_service.dto.TransactionDto;
import com.revpay.transaction_service.dto.TransferRequest;
import com.revpay.transaction_service.entity.Transaction;
import com.revpay.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private UserClient userClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void transferMoney_Success() {
        TransferRequest request = new TransferRequest();
        request.setReceiverId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.SEND)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionDto result = transactionService.transferMoney(1L, request);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(walletClient, times(1)).debitFunds(eq(1L), eq(new BigDecimal("100.00")));
        verify(walletClient, times(1)).creditFunds(eq(2L), eq(new BigDecimal("100.00")));
        verify(notificationClient, times(1)).sendNotification(eq(2L), anyString());
    }

    @Test
    void recordTransaction_Success() {
        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(new BigDecimal("50.00"))
                .type(Transaction.TransactionType.ADD_FUNDS)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionDto result = transactionService.recordTransaction(1L, new BigDecimal("50.00"),
                Transaction.TransactionType.ADD_FUNDS, "Add funds", null);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(notificationClient, times(1)).sendNotification(eq(1L), anyString());
    }

    @Test
    void createRequest_Success() {
        ApiResponse<Long> response = new ApiResponse<>();
        response.setData(2L);
        when(userClient.resolveUser(anyString())).thenReturn(response);

        Transaction requestTxn = Transaction.builder()
                .transactionId(10L)
                .senderId(2L)
                .receiverId(1L)
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.REQUEST)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(requestTxn);

        TransactionDto result = transactionService.createRequest(1L, "target@example.com", new BigDecimal("500.00"));

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("REQUEST", result.getType());
        verify(notificationClient, times(1)).sendNotification(eq(2L), anyString());
    }

    @Test
    void acceptRequest_Success() {
        Transaction requestTxn = Transaction.builder()
                .transactionId(10L)
                .senderId(2L)
                .receiverId(1L)
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.REQUEST)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(requestTxn));

        Transaction completedTxn = Transaction.builder()
                .transactionId(10L)
                .senderId(2L)
                .receiverId(1L)
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.REQUEST)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(completedTxn);

        TransactionDto result = transactionService.acceptRequest(2L, 10L);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(walletClient, times(1)).debitFunds(eq(2L), eq(new BigDecimal("500.00")));
        verify(walletClient, times(1)).creditFunds(eq(1L), eq(new BigDecimal("500.00")));
        verify(notificationClient, times(1)).sendNotification(eq(1L), anyString());
    }
}
