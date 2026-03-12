package com.revpay.wallet_service.service;

import jakarta.annotation.PostConstruct;

import com.revpay.wallet_service.dto.AddFundsRequest;
import com.revpay.wallet_service.dto.PaymentMethodDto;
import com.revpay.wallet_service.dto.WalletDto;
import com.revpay.wallet_service.entity.PaymentMethod;
import com.revpay.wallet_service.entity.Wallet;
import com.revpay.wallet_service.exception.InsufficientBalanceException;
import com.revpay.wallet_service.exception.ResourceNotFoundException;
import com.revpay.wallet_service.repository.PaymentMethodRepository;
import com.revpay.wallet_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final com.revpay.wallet_service.client.TransactionClient transactionClient;
    private final com.revpay.wallet_service.client.UserClient userClient;

    public WalletService(WalletRepository walletRepository,
                         PaymentMethodRepository paymentMethodRepository,
                         com.revpay.wallet_service.client.TransactionClient transactionClient,
                         com.revpay.wallet_service.client.UserClient userClient) {
        super();
        this.walletRepository = walletRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionClient = transactionClient;
        this.userClient = userClient;
    }

    @PostConstruct
    public void initAdminWallet() {
        try {
            Long adminId = 1L;
            if (walletRepository.findByUserId(adminId).isEmpty()) {
                log.info("LOAN_SERVICE | Initializing Admin wallet with ₹1,000,000");
                Wallet wallet = Wallet.builder()
                        .userId(adminId)
                        .balance(new BigDecimal("1000000.00"))
                        .currency("INR")
                        .build();
                walletRepository.save(wallet);
            }
        } catch (Exception e) {
            log.warn("LOAN_SERVICE | Could not initialize admin wallet: {}", e.getMessage());
        }
    }

    @Transactional
    public WalletDto getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createWallet(userId));
        return mapToDto(wallet);
    }

    @Transactional
    public Wallet createWallet(Long userId) {
        log.info("Creating new wallet for userId: {}", userId);
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .build();
        return walletRepository.save(wallet);
    }

    @Transactional
    public WalletDto addFunds(Long userId, AddFundsRequest request) {
        log.info("Adding ₹{} to wallet of userId: {}", request.getAmount(), userId);
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);

        // Record entry in transaction service
        try {
            transactionClient.recordTransaction(userId, request.getAmount(), "ADD_FUNDS", request.getDescription());
        } catch (Exception e) {
            log.error("Failed to sync transaction to transaction-service: {}", e.getMessage());
        }

        return mapToDto(savedWallet);
    }

    @Transactional
    public void debitFunds(Long userId, BigDecimal amount) {
        log.info("Debiting ₹{} from wallet of userId: {}", amount, userId);
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in wallet!");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void creditFunds(Long userId, BigDecimal amount) {
        log.info("Crediting ₹{} to wallet of userId: {}", amount, userId);
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createWallet(userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    // --- Request Money Management ---
    @Transactional
    public void requestMoney(Long userId, com.revpay.wallet_service.dto.RequestMoneyDTO request) {
        log.info("Requesting money: {} to {} amount {}", userId, request.getTargetEmail(), request.getAmount());
        transactionClient.createRequest(userId, request.getTargetEmail(), request.getAmount());
    }

    @Transactional(readOnly = true)
    public List<Object> getIncomingRequests(Long userId) {
        return transactionClient.getPendingRequests(userId, true);
    }

    @Transactional(readOnly = true)
    public List<Object> getOutgoingRequests(Long userId) {
        return transactionClient.getPendingRequests(userId, false);
    }

    @Transactional
    public void acceptRequest(Long userId, Long txnId, String pin) {
        userClient.verifyPin(userId, new com.revpay.wallet_service.dto.VerifyPinRequest(pin));
        transactionClient.acceptRequest(userId, txnId);
    }

    @Transactional
    public void declineRequest(Long userId, Long txnId) {
        transactionClient.declineRequest(userId, txnId);
    }

    // --- Card Management ---

    @Transactional
    public PaymentMethodDto addPaymentMethod(Long userId, PaymentMethod paymentMethod) {
        paymentMethod.setUserId(userId);
        if (paymentMethodRepository.findByUserId(userId).isEmpty()) {
            paymentMethod.setDefault(true);
        }
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return mapToPaymentMethodDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodDto> getPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserId(userId).stream()
                .map(this::mapToPaymentMethodDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePaymentMethod(Long userId, Long methodId) {
        PaymentMethod method = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));

        if (!method.getUserId().equals(userId)) {
            throw new IllegalStateException("Unauthorized to delete this payment method");
        }

        paymentMethodRepository.delete(method);
    }

    @Transactional
    public void setDefaultPaymentMethod(Long userId, Long methodId) {
        List<PaymentMethod> methods = paymentMethodRepository.findByUserId(userId);
        boolean found = false;
        for (PaymentMethod pm : methods) {
            if (pm.getId().equals(methodId)) {
                pm.setDefault(true);
                found = true;
            } else {
                pm.setDefault(false);
            }
        }
        if (!found) {
            throw new ResourceNotFoundException("Payment method not found for this user");
        }
        paymentMethodRepository.saveAll(methods);
    }

    private WalletDto mapToDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .lastUpdated(wallet.getLastUpdated())
                .build();
    }

    private PaymentMethodDto mapToPaymentMethodDto(PaymentMethod pm) {
        return PaymentMethodDto.builder()
                .id(pm.getId())
                .cardNumber(maskCardNumber(pm.getCardNumber()))
                .expiryDate(pm.getExpiryDate())
                .isDefault(pm.isDefault())
                .cardType(pm.getCardType())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4)
            return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}