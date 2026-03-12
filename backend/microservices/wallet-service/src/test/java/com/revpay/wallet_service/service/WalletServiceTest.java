package com.revpay.wallet_service.service;

import com.revpay.wallet_service.client.TransactionClient;
import com.revpay.wallet_service.client.UserClient;
import com.revpay.wallet_service.dto.AddFundsRequest;
import com.revpay.wallet_service.dto.WalletDto;
import com.revpay.wallet_service.entity.Wallet;
import com.revpay.wallet_service.exception.InsufficientBalanceException;
import com.revpay.wallet_service.repository.PaymentMethodRepository;
import com.revpay.wallet_service.repository.WalletRepository;
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
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private WalletService walletService;

    @Test
    void getWalletByUserId_ExistingWallet_ReturnsWalletDto() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        when(walletRepository.findByUserId(anyLong())).thenReturn(Optional.of(wallet));

        WalletDto result = walletService.getWalletByUserId(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getBalance());
        assertEquals("INR", result.getCurrency());
    }

    @Test
    void getWalletByUserId_NewWallet_CreatesAndReturnsWalletDto() {
        when(walletRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        
        Wallet newWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .build();
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        WalletDto result = walletService.getWalletByUserId(2L);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void addFunds_Success() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .build();
        
        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setDescription("Deposit");

        WalletDto result = walletService.addFunds(1L, request);

        assertEquals(new BigDecimal("150.00"), result.getBalance());
        verify(transactionClient, times(1)).recordTransaction(eq(1L), eq(new BigDecimal("50.00")), eq("ADD_FUNDS"), eq("Deposit"));
    }

    @Test
    void debitFunds_Success() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .build();

        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(wallet));

        assertDoesNotThrow(() -> walletService.debitFunds(1L, new BigDecimal("50.00")));
        
        assertEquals(new BigDecimal("50.00"), wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void debitFunds_InsufficientBalance_ThrowsException() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("40.00"))
                .build();

        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> walletService.debitFunds(1L, new BigDecimal("50.00")));
    }
}
