package com.revpay.loan_service.service;

import com.revpay.loan_service.client.WalletClient;
import com.revpay.loan_service.client.NotificationClient;
import com.revpay.loan_service.dto.*;
import com.revpay.loan_service.entity.Loan;
import com.revpay.loan_service.entity.LoanInstallment;
import com.revpay.loan_service.repository.LoanInstallmentRepository;
import com.revpay.loan_service.repository.LoanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final WalletClient walletClient;
    private final com.revpay.loan_service.client.UserClient userClient;
    private final NotificationClient notificationClient;

    private final com.revpay.loan_service.client.TransactionClient transactionClient;

    public LoanService(LoanRepository loanRepository, LoanInstallmentRepository installmentRepository,
            WalletClient walletClient, com.revpay.loan_service.client.UserClient userClient, 
            NotificationClient notificationClient, com.revpay.loan_service.client.TransactionClient transactionClient) {
        super();
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.walletClient = walletClient;
        this.userClient = userClient;
        this.notificationClient = notificationClient;
        this.transactionClient = transactionClient;
    }

    @Transactional
    public LoanResponseDto applyLoan(Long userId, LoanApplyRequest request) {
        log.info("UserId {} applying for loan of ₹{}", userId, request.getAmount());

        Loan loan = Loan.builder()
                .userId(userId)
                .businessId(userId)
                .amount(request.getAmount())
                .tenureMonths(request.getTenureMonths())
                .purpose(request.getPurpose())
                .status(Loan.LoanStatus.APPLIED)
                .remainingAmount(request.getAmount())
                .interestRate(BigDecimal.ZERO)
                .emiAmount(BigDecimal.ZERO)
                .applicationDate(java.time.LocalDate.now())
                .build();

        Loan savedLoan = loanRepository.save(loan);
        
        // Notify user
        try {
            notificationClient.sendNotification(userId, "Your loan application for ₹" + request.getAmount() + " was received successfully.");
        } catch (Exception ne) {
            log.warn("Failed to send notification: {}", ne.getMessage());
        }

        return mapToDto(savedLoan);
    }

    @Transactional
    public LoanResponseDto approveLoan(Long loanId, BigDecimal interestRate) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.APPLIED) {
            throw new IllegalStateException("Loan is already in status: " + loan.getStatus());
        }

        loan.setInterestRate(interestRate);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(LocalDate.now().plusMonths(loan.getTenureMonths()));

        // Simple EMI calculation: (Principal + Interest) / Tenure
        BigDecimal totalInterest = loan.getAmount().multiply(interestRate).divide(BigDecimal.valueOf(100), 2,
                RoundingMode.HALF_UP);
        BigDecimal totalRepayable = loan.getAmount().add(totalInterest);
        BigDecimal emi = totalRepayable.divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);

        loan.setEmiAmount(emi);
        loan.setRemainingAmount(totalRepayable);

        log.info("LOAN_SERVICE | Finalizing approval for loanId: {}. User: {}, Amount: {}", loanId, loan.getUserId(), loan.getAmount());
        
        // Disburse funds via wallet-service: Debit Admin, Credit User
        try {
            log.debug("LOAN_SERVICE | Debiting Admin (1) amount: {}", loan.getAmount());
            walletClient.debitFunds(1L, loan.getAmount());
        } catch (Exception e) {
            log.warn("LOAN_SERVICE | Admin debit failed, likely insufficient balance. Auto-funding admin for demo: {}", e.getMessage());
            // For demo/admin purposes, if admin is short, give them funds
            // In a real system you'd handle this differently
            walletClient.creditFunds(1L, loan.getAmount().multiply(BigDecimal.valueOf(2)));
            walletClient.debitFunds(1L, loan.getAmount());
        }
        
        log.debug("LOAN_SERVICE | Crediting User ({}) amount: {}", loan.getUserId(), loan.getAmount());
        walletClient.creditFunds(loan.getUserId(), loan.getAmount());

        generateInstallments(loan);

        Loan saved = loanRepository.save(loan);

        // Record transaction
        try {
            String userName = userClient.getUserName(loan.getUserId());
            log.info("LOAN_SERVICE | Synchronizing transaction record for disbursement...");
            transactionClient.recordTransaction(1L, loan.getAmount(), "LOAN_DISBURSEMENT", 
                "Loan disbursement for " + userName + " (App #" + loanId + ")", loan.getUserId());
        } catch (Exception te) {
            log.warn("LOAN_SERVICE | Failed to record disbursement transaction: {}", te.getMessage());
        }

        return mapToDto(saved);
    }

    private void generateInstallments(Loan loan) {
        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .amount(loan.getEmiAmount())
                    .dueDate(LocalDate.now().plusMonths(i))
                    .status(LoanInstallment.InstallmentStatus.PENDING)
                    .build();
            installmentRepository.save(installment);
        }
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDto> getLoans(Long userId) {
        return loanRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional
    public LoanResponseDto uploadDocument(Long loanId, org.springframework.web.multipart.MultipartFile file) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Use standard FileStorageService (simulating cloud upload)
        FileStorageService fileStorageService = new FileStorageService();
        String filePath = fileStorageService.storeFile(file, loanId);

        com.revpay.loan_service.entity.LoanDocument doc = com.revpay.loan_service.entity.LoanDocument.builder()
                .loan(loan)
                .fileName(org.springframework.util.StringUtils
                        .cleanPath(java.util.Objects.requireNonNull(file.getOriginalFilename())))
                .fileType(file.getContentType())
                .filePath(filePath)
                .build();

        loan.getDocuments().add(doc);
        return mapToDto(loanRepository.save(loan));
    }

    @Transactional
    public void repayLoan(RepaymentRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan is not active");
        }

        // 1. Verify PIN
        userClient.verifyPin(loan.getUserId(), java.util.Map.of("pin", request.getTransactionPin()));

        log.info("LOAN_SERVICE | Processing repayment: amount={}, loanId={}", request.getAmount(), loan.getLoanId());
        
        // 2. Wallet transfers: Debit User, Credit Admin
        walletClient.debitFunds(loan.getUserId(), request.getAmount());
        walletClient.creditFunds(1L, request.getAmount());

        // 3. Update Loan
        loan.setRemainingAmount(loan.getRemainingAmount().subtract(request.getAmount()));
        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
        }

        // 4. Mark installment as paid (find any pending)
        List<LoanInstallment> installments = installmentRepository.findByLoanOrderByInstallmentNumberAsc(loan);
        installments.stream()
                .filter(i -> i.getStatus() == LoanInstallment.InstallmentStatus.PENDING)
                .findFirst()
                .ifPresent(i -> i.setStatus(LoanInstallment.InstallmentStatus.PAID));

        loanRepository.save(loan);

        // Record transaction
        try {
            String userName = userClient.getUserName(loan.getUserId());
            log.info("LOAN_SERVICE | Synchronizing transaction record for repayment...");
            transactionClient.recordTransaction(loan.getUserId(), request.getAmount(), "LOAN_REPAYMENT", 
                "EMI Repayment from " + userName + " (Loan #" + loan.getLoanId() + ")", 1L);
        } catch (Exception te) {
            log.warn("LOAN_SERVICE | Failed to record repayment transaction: {}", te.getMessage());
        }
    }

    @Transactional
    public void preclose(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only active loans can be preclosed");
        }

        // Preclosure usually requires a full remaining amount payment
        BigDecimal amountToPay = loan.getRemainingAmount();
        
        // Wallet transfers: Debit User, Credit Admin
        walletClient.debitFunds(loan.getUserId(), amountToPay);
        walletClient.creditFunds(1L, amountToPay);

        // Update Loan
        loan.setRemainingAmount(BigDecimal.ZERO);
        loan.setStatus(Loan.LoanStatus.CLOSED);

        // Mark all pending installments as PAID
        List<LoanInstallment> installments = installmentRepository.findByLoanOrderByInstallmentNumberAsc(loan);
        for (LoanInstallment installment : installments) {
            if (installment.getStatus() == LoanInstallment.InstallmentStatus.PENDING) {
                installment.setStatus(LoanInstallment.InstallmentStatus.PAID);
            }
        }

        loanRepository.save(loan);

        // Record transaction
        try {
            String userName = userClient.getUserName(loan.getUserId());
            transactionClient.recordTransaction(loan.getUserId(), amountToPay, "LOAN_REPAYMENT", 
                "Full preclosure settlement from " + userName + " (Loan #" + loanId + ")", 1L);
        } catch (Exception te) {
            log.warn("Failed to record preclosure transaction: {}", te.getMessage());
        }
    }

    private LoanResponseDto mapToDto(Loan loan) {
        return LoanResponseDto.builder()
                .loanId(loan.getLoanId())
                .userId(loan.getUserId())
                .amount(loan.getAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .remainingAmount(loan.getRemainingAmount())
                .status(loan.getStatus().name())
                .purpose(loan.getPurpose())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .createdAt(loan.getCreatedAt())
                .documents(loan.getDocuments() != null ? loan.getDocuments().stream()
                        .map(doc -> LoanDocumentDto.builder()
                                .id(doc.getId())
                                .loanId(doc.getLoan().getLoanId())
                                .fileName(doc.getFileName())
                                .fileType(doc.getFileType())
                                .filePath(doc.getFilePath())
                                .uploadedAt(doc.getUploadedAt())
                                .build())
                        .collect(Collectors.toList()) : java.util.Collections.emptyList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Object> getEmiSchedule(Long loanId) {
        List<LoanInstallment> installments = installmentRepository.findByLoanLoanId(loanId);
        return installments.stream().map(i -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("installmentId", i.getId());
            map.put("loanId", loanId);
            map.put("installmentNumber", i.getInstallmentNumber());
            map.put("amount", i.getAmount());
            map.put("dueDate", i.getDueDate());
            map.put("status", i.getStatus().name());
            return (Object) map;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getLoanAnalytics(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;

        for (Loan loan : loans) {
            if (loan.getStatus() == Loan.LoanStatus.ACTIVE || loan.getStatus() == Loan.LoanStatus.APPROVED) {
                totalOutstanding = totalOutstanding.add(loan.getRemainingAmount() != null ? loan.getRemainingAmount() : BigDecimal.ZERO);
                BigDecimal totalLoanAmount = loan.getAmount() != null ? loan.getAmount() : BigDecimal.ZERO;
                BigDecimal remaining = loan.getRemainingAmount() != null ? loan.getRemainingAmount() : BigDecimal.ZERO;
                totalPaid = totalPaid.add(totalLoanAmount.subtract(remaining).max(BigDecimal.ZERO));
            } else if (loan.getStatus() == Loan.LoanStatus.CLOSED) {
                totalPaid = totalPaid.add(loan.getAmount() != null ? loan.getAmount() : BigDecimal.ZERO);
            }
        }

        // Count pending installments
        List<LoanInstallment> pendingInstallments = installmentRepository.findByLoanUserIdAndStatus(userId, LoanInstallment.InstallmentStatus.PENDING);
        for (LoanInstallment inst : pendingInstallments) {
            totalPending = totalPending.add(inst.getAmount() != null ? inst.getAmount() : BigDecimal.ZERO);
        }

        java.util.Map<String, Object> analytics = new java.util.LinkedHashMap<>();
        analytics.put("totalOutstanding", totalOutstanding);
        analytics.put("totalPaid", totalPaid);
        analytics.put("totalPending", totalPending);
        return analytics;
    }

    @Transactional
    public LoanResponseDto rejectLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.APPLIED) {
            throw new IllegalStateException("Only APPLIED loans can be rejected. Current status: " + loan.getStatus());
        }

        loan.setStatus(Loan.LoanStatus.REJECTED);
        Loan saved = loanRepository.save(loan);

        // Notify user
        try {
            notificationClient.sendNotification(loan.getUserId(), "Your loan application has been rejected.");
        } catch (Exception ne) {
            log.warn("Failed to send rejection notification: {}", ne.getMessage());
        }

        return mapToDto(saved);
    }
}
