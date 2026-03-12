package com.revpay.controller;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.BusinessProfile;
import com.revpay.repository.BusinessProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Secures the entire controller
@Tag(name = "Admin Operations", description = "Endpoints for platform administration, auditing, and oversight")
public class AdminController {

        private final BusinessProfileRepository businessProfileRepository;
        private final com.revpay.repository.UserRepository userRepository;
        private final com.revpay.client.LoanClient loanClient;
        private final com.revpay.client.TransactionClient transactionClient;
        private final com.revpay.client.WalletClient walletClient;

        // --- DTOs ---
        public record BusinessProfileDTO(Long profileId, Long userId, String ownerName, String ownerEmail,
                        String businessName, String businessType,
                        String taxId, String address, boolean verified) {
        }

        public record UserAdminDTO(Long userId, String email, String fullName, String phoneNumber, String role,
                        boolean isActive, boolean isVerified, java.time.LocalDateTime createdAt) {
        }

        public record UserStatusRequest(boolean active) {
        }

        public record LoanApproveRequest(Long loanId, boolean approved, BigDecimal approvedInterestRate, String rejectionReason) {
        }

        // --- USER MANAGEMENT ---
        @GetMapping("/users")
        @Operation(summary = "Get all users", description = "Retrieves a paginated list of all registered users on the platform.")
        public ResponseEntity<ApiResponse<Page<UserAdminDTO>>> getAllUsers(
                        @PageableDefault(size = 20) Pageable pageable) {
                log.info("Admin fetching all users page: {}", pageable.getPageNumber());
                Page<com.revpay.model.entity.User> users = userRepository.findAll(pageable);
                Page<UserAdminDTO> dtos = users.map(u -> new UserAdminDTO(
                                u.getUserId(),
                                u.getEmail(),
                                u.getFullName(),
                                u.getPhoneNumber(),
                                u.getRole().name(),
                                u.isActive(),
                                u.isVerified(),
                                u.getCreatedAt()));

                return ResponseEntity.ok(ApiResponse.success(dtos, "Users retrieved successfully"));
        }

        @PutMapping("/users/{userId}/status")
        @Operation(summary = "Update User Status", description = "Activates or deactivates a user account, preventing login if deactivated.")
        public ResponseEntity<ApiResponse<String>> updateUserStatus(@PathVariable Long userId,
                        @RequestBody UserStatusRequest request) {
                
                com.revpay.model.entity.User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
                user.setActive(request.active());
                userRepository.save(user);

                return ResponseEntity
                                .ok(ApiResponse.success("Status updated",
                                                "User account status has been updated successfully."));
        }

        // --- BUSINESS MANAGEMENT ---

        @GetMapping("/businesses")
        @Operation(summary = "Get all business profiles", description = "Retrieves a paginated list of all registered business profiles.")
        public ResponseEntity<ApiResponse<Page<BusinessProfileDTO>>> getAllBusinessProfiles(
                        @PageableDefault(size = 10) Pageable pageable) {
                log.info("Admin fetching all business profiles page: {}", pageable.getPageNumber());

                Page<BusinessProfile> profiles = businessProfileRepository.findAll(pageable);
                Page<BusinessProfileDTO> dtos = profiles.map(p -> new BusinessProfileDTO(
                                p.getProfileId(),
                                p.getUser().getUserId(),
                                p.getUser().getFullName(),
                                p.getUser().getEmail(),
                                p.getBusinessName(),
                                p.getBusinessType(),
                                p.getTaxId(),
                                p.getAddress(),
                                p.isVerified()));

                return ResponseEntity.ok(ApiResponse.success(dtos, "Business profiles retrieved successfully"));
        }

        @org.springframework.transaction.annotation.Transactional
        @PostMapping("/businesses/{id}/verify")
        @Operation(summary = "Verify a business", description = "Marks a business profile as verified after manual administrative review.")
        public ResponseEntity<ApiResponse<String>> verifyBusiness(@PathVariable Long id) {
                log.info("Admin initiating verification for business profile ID: {}", id);

                BusinessProfile profile = businessProfileRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Business profile not found with ID: " + id));

                if (profile.isVerified()) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("VAL_002", "Business is already verified."));
                }

                profile.setVerified(true);
                businessProfileRepository.save(profile);

                // Activate User as well
                com.revpay.model.entity.User user = profile.getUser();
                user.setVerified(true);
                userRepository.save(user);

                return ResponseEntity
                                .ok(ApiResponse.success("Verification complete",
                                                "Business account verified successfully."));
        }

        @org.springframework.transaction.annotation.Transactional
        @PutMapping("/businesses/{id}/suspend")
        @Operation(summary = "Suspend a business", description = "Revokes verification status from a business profile.")
        public ResponseEntity<ApiResponse<String>> suspendBusiness(@PathVariable Long id) {
                log.info("Admin suspending business profile ID: {}", id);

                BusinessProfile profile = businessProfileRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Business profile not found with ID: " + id));

                profile.setVerified(false);
                businessProfileRepository.save(profile);

                return ResponseEntity
                                .ok(ApiResponse.success("Suspension complete",
                                                "Business account suspended successfully."));
        }

        // --- LOAN MANAGEMENT ---
        @PostMapping("/loans/approve")
        @Operation(summary = "Approve/Reject loan application")
        public ResponseEntity<ApiResponse<Object>> approveLoan(@RequestBody LoanApproveRequest request) {
                log.info("Admin deciding on loan ID: {} | Approved: {}", request.loanId(), request.approved());
                
                if (request.approved()) {
                         return loanClient.approveLoan(request.loanId(), request.approvedInterestRate());
                } else {
                         // TODO: Implement rejection in loan-service if needed
                         return ResponseEntity.ok(ApiResponse.success(null, "Loan application rejected."));
                }
        }

        @GetMapping("/loans/all")
        @Operation(summary = "Admin: Get all loan applications across platform")
        public ResponseEntity<ApiResponse<Page<Object>>> getAllLoans(
                        @PageableDefault(size = 10) Pageable pageable) {
                return loanClient.getAllLoans(pageable.getPageNumber(), pageable.getPageSize());
        }

        @GetMapping("/transactions")
        @Operation(summary = "Admin: Get system-wide transactions")
        public ResponseEntity<ApiResponse<Page<Object>>> getAllTransactions(
                        @PageableDefault(size = 20) Pageable pageable) {
                return transactionClient.getAllTransactions(pageable.getPageNumber(), pageable.getPageSize());
        }

        // --- SYSTEM LEDGER & INVOICES ---
        // These are now handled by respective microservices (Transaction, Invoice)
        
        @GetMapping("/analytics")
        @Operation(summary = "System Analytics", description = "Get platform-wide metrics regarding users, businesses, and transaction volume.")
        public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAnalytics() {
                long totalUsers = userRepository.count();
                long totalBusinesses = businessProfileRepository.count();
                long verifiedBusinesses = businessProfileRepository.countByIsVerifiedTrue();
                
                // Fetch transaction stats from transaction-service
                long totalTxns = 0;
                BigDecimal totalVol = BigDecimal.ZERO;
                try {
                        ApiResponse<Map<String, Object>> txnStats = transactionClient.getAnalytics().getBody();
                        if (txnStats != null && txnStats.getData() != null) {
                                totalTxns = ((Number) txnStats.getData().getOrDefault("totalTransactions", 0)).longValue();
                                Object vol = txnStats.getData().get("totalVolume");
                                if (vol instanceof Number) {
                                        totalVol = BigDecimal.valueOf(((Number) vol).doubleValue());
                                } else if (vol instanceof String) {
                                        totalVol = new BigDecimal((String) vol);
                                }
                        }
                } catch (Exception e) {
                        log.warn("Failed to fetch transaction analytics: {}", e.getMessage());
                }

                BigDecimal adminBalance = BigDecimal.ZERO;
                try {
                        // Admin is userId 1
                        ApiResponse<Map<String, Object>> res = walletClient.getWallet(1L);
                        if (res != null && res.getData() != null) {
                            Object bal = res.getData().get("balance");
                            if (bal instanceof Number) {
                                adminBalance = BigDecimal.valueOf(((Number) bal).doubleValue());
                            } else if (bal instanceof String) {
                                adminBalance = new BigDecimal((String) bal);
                            }
                        }
                } catch (Exception e) {
                        log.warn("Failed to fetch admin wallet balance: {}", e.getMessage());
                }

                java.util.Map<String, Object> analytics = java.util.Map.of(
                        "totalUsers", totalUsers,
                        "totalBusinesses", totalBusinesses,
                        "activeBusinesses", verifiedBusinesses,
                        "totalTransactions", totalTxns,
                        "totalVolume", totalVol,
                        "adminWalletBalance", adminBalance
                );

                return ResponseEntity.ok(ApiResponse.success(analytics, "System metrics retrieved"));
        }
}