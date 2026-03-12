package com.revpay.loan_service.service;

import com.revpay.loan_service.client.NotificationClient;
import com.revpay.loan_service.client.TransactionClient;
import com.revpay.loan_service.client.UserClient;
import com.revpay.loan_service.client.WalletClient;
import com.revpay.loan_service.dto.LoanApplyRequest;
import com.revpay.loan_service.dto.LoanResponseDto;
import com.revpay.loan_service.entity.Loan;
import com.revpay.loan_service.repository.LoanInstallmentRepository;
import com.revpay.loan_service.repository.LoanRepository;
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
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private UserClient userClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private TransactionClient transactionClient;

    @InjectMocks
    private LoanService loanService;

    @Test
    void applyLoan_Success() {
        LoanApplyRequest request = new LoanApplyRequest();
        request.setAmount(new BigDecimal("10000.00"));
        request.setTenureMonths(12);
        request.setPurpose("Education");

        Loan savedLoan = Loan.builder()
                .loanId(1L)
                .userId(2L)
                .amount(new BigDecimal("10000.00"))
                .status(Loan.LoanStatus.APPLIED)
                .build();

        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

        LoanResponseDto result = loanService.applyLoan(2L, request);

        assertNotNull(result);
        assertEquals(1L, result.getLoanId());
        assertEquals("APPLIED", result.getStatus());
        verify(notificationClient, times(1)).sendNotification(eq(2L), anyString());
    }

    @Test
    void approveLoan_Success() {
        Loan loan = Loan.builder()
                .loanId(1L)
                .userId(10L)
                .amount(new BigDecimal("10000.00"))
                .tenureMonths(12)
                .status(Loan.LoanStatus.APPLIED)
                .build();

        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(userClient.getUserName(anyLong())).thenReturn("Test User");

        LoanResponseDto result = loanService.approveLoan(1L, new BigDecimal("10.0"));

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(walletClient, times(1)).debitFunds(eq(1L), any(BigDecimal.class));
        verify(walletClient, times(1)).creditFunds(eq(10L), any(BigDecimal.class));
        verify(installmentRepository, times(12)).save(any());
        verify(transactionClient, times(1)).recordTransaction(eq(1L), any(), anyString(), anyString(), eq(10L));
    }

    @Test
    void rejectLoan_Success() {
        Loan loan = Loan.builder()
                .loanId(1L)
                .userId(10L)
                .amount(new BigDecimal("10000.00"))
                .status(Loan.LoanStatus.APPLIED)
                .build();

        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        LoanResponseDto result = loanService.rejectLoan(1L);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        verify(notificationClient, times(1)).sendNotification(eq(10L), anyString());
    }
}
