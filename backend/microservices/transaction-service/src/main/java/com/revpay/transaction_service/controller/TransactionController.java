package com.revpay.transaction_service.controller;

import com.revpay.transaction_service.dto.*;
import com.revpay.transaction_service.entity.Transaction;
import com.revpay.transaction_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Management", description = "Endpoints for money transfers and history")
public class TransactionController {

    private final TransactionService transactionService;
    private final com.revpay.transaction_service.service.TransactionExportService exportService;

    public TransactionController(TransactionService transactionService,
            com.revpay.transaction_service.service.TransactionExportService exportService) {
        super();
        this.transactionService = transactionService;
        this.exportService = exportService;
    }

    @PostMapping("/transfer/{senderId}")
    @Operation(summary = "P2P Money Transfer")
    public ResponseEntity<ApiResponse<TransactionDto>> transfer(
            @PathVariable Long senderId,
            @Valid @RequestBody TransferRequest request) {
        TransactionDto result = transactionService.transferMoney(senderId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Transfer successful"));
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get transaction history for user")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TransactionDto> history = transactionService.getHistoryPaged(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history, "History retrieved"));
    }

    @GetMapping("/filter/{userId}")
    @Operation(summary = "Filter transactions for user")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> filter(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        // Simple in-memory filter for now, or just return history if no params
        List<TransactionDto> history = transactionService.getHistory(userId);
        if (type != null && !type.isEmpty()) {
            history = history.stream().filter(t -> t.getType().equalsIgnoreCase(type)).toList();
        }
        if (status != null && !status.isEmpty()) {
            history = history.stream().filter(t -> t.getStatus().equalsIgnoreCase(status)).toList();
        }
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), history.size());
        List<TransactionDto> pageContent = start <= end ? history.subList(start, end) : java.util.Collections.emptyList();
        Page<TransactionDto> page = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, history.size());
        
        return ResponseEntity.ok(ApiResponse.success(page, "Filtered history retrieved"));
    }

    @GetMapping("/export/csv/{userId}")
    @Operation(summary = "Export user transaction history to CSV")
    public ResponseEntity<byte[]> exportHistoryToCsv(@PathVariable Long userId) {
        List<TransactionDto> history = transactionService.getHistory(userId);
        byte[] csvData = exportService.exportToCsv(history);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=transactions_" + userId + ".csv");
        headers.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/export/pdf/{userId}")
    @Operation(summary = "Export user transaction history to PDF")
    public ResponseEntity<byte[]> exportHistoryToPdf(@PathVariable Long userId) {
        List<TransactionDto> history = transactionService.getHistory(userId);
        byte[] pdfData = exportService.exportToPdf(history);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=transactions_" + userId + ".pdf");
        headers.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }

    @PostMapping("/internal/record")
    @Operation(summary = "Internal: Record a transaction from another service")
    public ResponseEntity<TransactionDto> recordInternal(@RequestParam Long userId, 
                                                       @RequestParam java.math.BigDecimal amount, 
                                                       @RequestParam String type, 
                                                       @RequestParam(required = false) String description,
                                                       @RequestParam(required = false) Long receiverId) {
        Transaction.TransactionType txnType = Transaction.TransactionType.valueOf(type);
        TransactionDto dto = transactionService.recordTransaction(userId, amount, txnType, description, receiverId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Internal/Admin: Get transaction analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        Map<String, Object> stats = transactionService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Stats retrieved"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAll(pageable), "All transactions retrieved"));
    }

    @PostMapping("/request/{requesterId}")
    public ResponseEntity<TransactionDto> createRequest(@PathVariable Long requesterId, @RequestParam String targetEmail, @RequestParam java.math.BigDecimal amount) {
        return ResponseEntity.ok(transactionService.createRequest(requesterId, targetEmail, amount));
    }

    @GetMapping("/requests/pending/{userId}")
    public ResponseEntity<List<TransactionDto>> getPendingRequests(@PathVariable Long userId, @RequestParam boolean incoming) {
        return ResponseEntity.ok(transactionService.getPendingRequests(userId, incoming));
    }

    @PostMapping("/request/accept/{txnId}")
    public ResponseEntity<TransactionDto> acceptRequest(@RequestParam Long userId, @PathVariable Long txnId) {
        return ResponseEntity.ok(transactionService.acceptRequest(userId, txnId));
    }

    @PostMapping("/request/decline/{txnId}")
    public ResponseEntity<Void> declineRequest(@RequestParam Long userId, @PathVariable Long txnId) {
        transactionService.declineRequest(userId, txnId);
        return ResponseEntity.ok().build();
    }
}
