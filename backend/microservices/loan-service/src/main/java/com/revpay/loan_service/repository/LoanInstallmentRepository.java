package com.revpay.loan_service.repository;

import com.revpay.loan_service.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {
    List<LoanInstallment> findByLoanLoanId(Long loanId);

    List<LoanInstallment> findByLoanUserIdAndStatus(Long userId, LoanInstallment.InstallmentStatus status);

    List<LoanInstallment> findByLoanOrderByInstallmentNumberAsc(com.revpay.loan_service.entity.Loan loan);
}
