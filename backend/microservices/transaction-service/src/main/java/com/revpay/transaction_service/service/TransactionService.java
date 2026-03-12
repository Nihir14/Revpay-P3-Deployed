package com.revpay.transaction_service.service;

import com.revpay.transaction_service.client.NotificationClient;
import com.revpay.transaction_service.client.WalletClient;
import com.revpay.transaction_service.dto.TransactionDto;
import com.revpay.transaction_service.dto.TransferRequest;
import com.revpay.transaction_service.entity.Transaction;
import com.revpay.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletClient walletClient;
    private final com.revpay.transaction_service.client.UserClient userClient;
    private final NotificationClient notificationClient;

    public TransactionService(TransactionRepository transactionRepository, 
                          WalletClient walletClient,
                          com.revpay.transaction_service.client.UserClient userClient,
                          NotificationClient notificationClient) {
        super();
        this.transactionRepository = transactionRepository;
        this.walletClient = walletClient;
        this.userClient = userClient;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public TransactionDto transferMoney(Long senderId, TransferRequest request) {
        Long receiverId = request.getReceiverId();
        
        // Resolve receiver ID if only identifier (email/phone) is provided
        if (receiverId == null && request.getReceiverIdentifier() != null) {
            log.info("Resolving receiver info for: {}", request.getReceiverIdentifier());
            receiverId = userClient.resolveUser(request.getReceiverIdentifier()).getData();
        }
        
        if (receiverId == null) {
            throw new IllegalArgumentException("Receiver could not be identified");
        }
        
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send money to yourself");
        }

        log.info("Initiating transfer from {} to {} for amount ₹{}", senderId, receiverId,
                request.getAmount());

        // 1. Create PENDING transaction
        Transaction transaction = Transaction.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.SEND)
                .status(Transaction.TransactionStatus.PENDING)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .description(request.getDescription())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // 2. Call Wallet Service to debit sender
            walletClient.debitFunds(senderId, request.getAmount());

            // 3. Call Wallet Service to credit receiver
            walletClient.creditFunds(receiverId, request.getAmount());

            // 4. Update status to COMPLETED
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            
            // 5. Send Notification
            try {
                notificationClient.sendNotification(receiverId, "Received ₹" + request.getAmount() + " from user " + senderId);
            } catch (Exception ne) {
                log.warn("Failed to send notification: {}", ne.getMessage());
            }
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            throw new RuntimeException("Transaction failed: " + e.getMessage());
        }

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getHistory(Long userId) {
        List<Transaction> txns = transactionRepository.findBySenderIdOrReceiverIdOrderByTimestampDesc(userId, userId);
        log.info("Found {} transactions for userId: {}", txns.size(), userId);
        return txns.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getHistoryPaged(Long userId, Pageable pageable) {
        Page<Transaction> txns = transactionRepository.findBySenderIdOrReceiverIdOrderByTimestampDesc(userId, userId, pageable);
        log.info("Found {} transactions in current page for userId: {}", txns.getNumberOfElements(), userId);
        return txns.map(this::mapToDto);
    }

    private TransactionDto mapToDto(Transaction t) {
        return TransactionDto.builder()
                .transactionId(t.getTransactionId())
                .senderId(t.getSenderId())
                .receiverId(t.getReceiverId())
                .amount(t.getAmount())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .transactionRef(t.getTransactionRef())
                .timestamp(t.getTimestamp())
                .build();
    }

    @Transactional
    public TransactionDto recordTransaction(Long userId, java.math.BigDecimal amount, Transaction.TransactionType type, String description, Long receiverId) {
        log.info("TRANSACTION_SERVICE | Internal record hit: userId={}, amount={}, type={}, receiverId={}", userId, amount, type, receiverId);
        
        Transaction.TransactionBuilder builder = Transaction.builder()
                .amount(amount)
                .type(type)
                .status(Transaction.TransactionStatus.COMPLETED)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .description(description);

        // Logic: ADD_FUNDS only has a receiver (userId).
        // WITHDRAW only has a sender (userId).
        // Everyone else (SEND, INVOICE_PAYMENT, LOAN_DISBURSEMENT, LOAN_REPAYMENT) has both if provided.
        if (type == Transaction.TransactionType.ADD_FUNDS) {
            builder.receiverId(userId);
        } else if (type == Transaction.TransactionType.WITHDRAW) {
            builder.senderId(userId);
        } else {
            builder.senderId(userId);
            if (receiverId != null) {
                builder.receiverId(receiverId);
            }
        }

        Transaction saved = transactionRepository.save(builder.build());
        
        // Notify users
        try {
            String senderMsg;
            String receiverMsg;
            
            switch (type) {
                case ADD_FUNDS -> {
                    senderMsg = "Money added: ₹" + amount;
                    receiverMsg = null;
                }
                case LOAN_DISBURSEMENT -> {
                    senderMsg = "Loan disbursed to user: ₹" + amount;
                    receiverMsg = "Loan amount credited: ₹" + amount;
                }
                case LOAN_REPAYMENT -> {
                    senderMsg = "Loan repayment made: ₹" + amount;
                    receiverMsg = "Loan repayment received: ₹" + amount;
                }
                case INVOICE_PAYMENT -> {
                    senderMsg = "Invoice payment made: ₹" + amount;
                    receiverMsg = "Invoice payment received: ₹" + amount;
                }
                default -> {
                    senderMsg = "Transaction processed: ₹" + amount;
                    receiverMsg = "Money received: ₹" + amount;
                }
            }

            if (description != null && !description.isEmpty()) {
                senderMsg += " (" + description + ")";
                if (receiverMsg != null) receiverMsg += " (" + description + ")";
            }

            // Send to sender if exists
            if (saved.getSenderId() != null) {
                notificationClient.sendNotification(saved.getSenderId(), senderMsg);
            } else if (type == Transaction.TransactionType.ADD_FUNDS) {
                 notificationClient.sendNotification(saved.getReceiverId(), senderMsg);
            }
            
            // Send to receiver if exists and different
            if (saved.getReceiverId() != null && !saved.getReceiverId().equals(saved.getSenderId()) && type != Transaction.TransactionType.ADD_FUNDS) {
                notificationClient.sendNotification(saved.getReceiverId(), receiverMsg);
            }
        } catch (Exception ne) {
            log.warn("Failed to send notification: {}", ne.getMessage());
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStats() {
        long count = transactionRepository.count();
        java.math.BigDecimal volume = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        return Map.of(
            "totalTransactions", count,
            "totalVolume", volume
        );
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getAll(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional
    public TransactionDto createRequest(Long requesterId, String targetEmail, java.math.BigDecimal amount) {
        log.info("Creating money request from {} to email {} for ₹{}", requesterId, targetEmail, amount);
        
        Long targetId = userClient.resolveUser(targetEmail).getData();
        if (targetId == null) {
            throw new IllegalArgumentException("Target user not found");
        }
        
        Transaction request = Transaction.builder()
                .senderId(targetId) // The person who will PAY
                .receiverId(requesterId) // The person who CAUSE the request
                .amount(amount)
                .type(Transaction.TransactionType.REQUEST)
                .status(Transaction.TransactionStatus.PENDING)
                .transactionRef("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .description("Money request from " + requesterId)
                .build();
                
        Transaction savedRequest = transactionRepository.save(request);
        
        // Send Notification
        try {
            notificationClient.sendNotification(targetId, "You have a new money request of ₹" + amount + " from user " + requesterId);
        } catch (Exception ne) {
            log.warn("Failed to send notification: {}", ne.getMessage());
        }

        return mapToDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getPendingRequests(Long userId, boolean incoming) {
        List<Transaction> all = transactionRepository.findBySenderIdOrReceiverIdOrderByTimestampDesc(userId, userId);
        return all.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.REQUEST)
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                .filter(t -> incoming ? t.getSenderId().equals(userId) : t.getReceiverId().equals(userId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDto acceptRequest(Long userId, Long txnId) {
        log.info("User {} accepting request transaction {}", userId, txnId);
        Transaction transaction = transactionRepository.findById(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getType() != Transaction.TransactionType.REQUEST) {
            throw new IllegalArgumentException("Only REQUEST type transactions can be accepted");
        }

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Request is not in PENDING status");
        }

        if (!transaction.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to accept this request");
        }

        // 1. Debit sender (the person who was requested)
        walletClient.debitFunds(transaction.getSenderId(), transaction.getAmount());

        // 2. Credit receiver (the person who made the request)
        walletClient.creditFunds(transaction.getReceiverId(), transaction.getAmount());

        // 3. Update status
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        
        // 4. Send Confirmation Notification to requester
        try {
            notificationClient.sendNotification(transaction.getReceiverId(), 
                "Your money request for ₹" + transaction.getAmount() + " was accepted and paid by user " + userId);
        } catch (Exception ne) {
            log.warn("Failed to notify requester: {}", ne.getMessage());
        }

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public com.revpay.transaction_service.dto.BusinessSummaryDTO getBusinessSummary(Long businessId) {
        log.info("ANALYTICS_SUMMARY | Request for Business ID: {}", businessId);

        List<Transaction.TransactionType> revenueTypes = List.of(
                Transaction.TransactionType.SEND,
                Transaction.TransactionType.INVOICE_PAYMENT
        );

        List<Transaction> completed = transactionRepository.findByReceiverIdAndStatusAndTypeIn(
                businessId, Transaction.TransactionStatus.COMPLETED, revenueTypes);

        List<Transaction> pending = transactionRepository.findByReceiverIdAndStatusAndTypeIn(
                businessId, Transaction.TransactionStatus.PENDING, revenueTypes);

        java.math.BigDecimal totalReceived = completed.stream()
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal pendingAmount = pending.stream()
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return com.revpay.transaction_service.dto.BusinessSummaryDTO.builder()
                .totalReceived(totalReceived)
                .totalSent(java.math.BigDecimal.ZERO)
                .pendingAmount(pendingAmount)
                .totalTransactionCount(completed.size())
                .currency("INR")
                .build();
    }

    public void declineRequest(Long userId, Long txnId) {
        log.info("User {} declining request transaction {}", userId, txnId);
        Transaction transaction = transactionRepository.findById(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!transaction.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to decline this request");
        }

        transaction.setStatus(Transaction.TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }
}
