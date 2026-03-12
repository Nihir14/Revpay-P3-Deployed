package com.revpay.wallet_service.controller;

import com.revpay.wallet_service.dto.*;
import com.revpay.wallet_service.entity.PaymentMethod;
import com.revpay.wallet_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallet Management", description = "Endpoints for managing user wallets and payment methods")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        super();
        this.walletService = walletService;
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user ID")
    public ResponseEntity<ApiResponse<WalletDto>> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByUserId(userId), "Wallet retrieved"));
    }

    @PostMapping("/user/{userId}/add-funds")
    @Operation(summary = "Add funds to wallet")
    public ResponseEntity<ApiResponse<WalletDto>> addFunds(
            @PathVariable Long userId,
            @Valid @RequestBody AddFundsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(walletService.addFunds(userId, request), "Funds added"));
    }

    @PostMapping("/user/{userId}/debit")
    @Operation(summary = "Internal: Debit funds (for transactions)")
    public ResponseEntity<Void> debitFunds(@PathVariable Long userId, @RequestParam BigDecimal amount) {
        walletService.debitFunds(userId, amount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}/credit")
    @Operation(summary = "Internal: Credit funds (for transactions)")
    public ResponseEntity<Void> creditFunds(@PathVariable Long userId, @RequestParam BigDecimal amount) {
        walletService.creditFunds(userId, amount);
        return ResponseEntity.ok().build();
    }

    // --- Request Money Management ---

    @PostMapping("/user/{userId}/request")
    @Operation(summary = "Request money from another user")
    public ResponseEntity<ApiResponse<Void>> requestMoney(@PathVariable Long userId, @RequestBody com.revpay.wallet_service.dto.RequestMoneyDTO request) {
        walletService.requestMoney(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Money request sent"));
    }

    @GetMapping("/user/{userId}/requests/incoming")
    @Operation(summary = "Get incoming money requests")
    public ResponseEntity<ApiResponse<List<Object>>> getIncomingRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getIncomingRequests(userId), "Incoming requests retrieved"));
    }

    @GetMapping("/user/{userId}/requests/outgoing")
    @Operation(summary = "Get outgoing money requests")
    public ResponseEntity<ApiResponse<List<Object>>> getOutgoingRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getOutgoingRequests(userId), "Outgoing requests retrieved"));
    }

    @PostMapping("/user/{userId}/request/accept/{txnId}")
    @Operation(summary = "Accept a money request")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(@PathVariable Long userId, @PathVariable Long txnId, @RequestBody Map<String, String> payload) {
        String pin = payload.get("pin");
        walletService.acceptRequest(userId, txnId, pin);
        return ResponseEntity.ok(ApiResponse.success(null, "Request accepted"));
    }

    @PostMapping("/user/{userId}/request/decline/{txnId}")
    @Operation(summary = "Decline a money request")
    public ResponseEntity<ApiResponse<Void>> declineRequest(@PathVariable Long userId, @PathVariable Long txnId) {
        walletService.declineRequest(userId, txnId);
        return ResponseEntity.ok(ApiResponse.success(null, "Request declined"));
    }

    // --- Card Management ---

    @GetMapping("/user/{userId}/cards")
    @Operation(summary = "Get all payment methods for user")
    public ResponseEntity<ApiResponse<List<PaymentMethodDto>>> getCards(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getPaymentMethods(userId), "Cards retrieved"));
    }

    @PostMapping("/user/{userId}/cards")
    @Operation(summary = "Add new payment method")
    public ResponseEntity<ApiResponse<PaymentMethodDto>> addCard(
            @PathVariable Long userId,
            @Valid @RequestBody PaymentMethod paymentMethod) {
        return ResponseEntity
                .ok(ApiResponse.success(walletService.addPaymentMethod(userId, paymentMethod), "Card added"));
    }

    @DeleteMapping("/user/{userId}/cards/{cardId}")
    @Operation(summary = "Remove payment method")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Long userId, @PathVariable Long cardId) {
        walletService.deletePaymentMethod(userId, cardId);
        return ResponseEntity.ok(ApiResponse.success(null, "Card removed"));
    }

    @PostMapping("/user/{userId}/cards/default/{cardId}")
    @Operation(summary = "Set default payment method")
    public ResponseEntity<ApiResponse<Void>> setDefaultCard(@PathVariable Long userId, @PathVariable Long cardId) {
        walletService.setDefaultPaymentMethod(userId, cardId);
        return ResponseEntity.ok(ApiResponse.success(null, "Default card updated"));
    }
}
