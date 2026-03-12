package com.revpay.transaction_service.repository;

import com.revpay.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
        List<Transaction> findBySenderIdOrReceiverIdOrderByTimestampDesc(Long senderId, Long receiverId);

        org.springframework.data.domain.Page<Transaction> findBySenderIdOrReceiverIdOrderByTimestampDesc(Long senderId, Long receiverId, org.springframework.data.domain.Pageable pageable);

        List<Transaction> findBySenderIdAndTypeAndStatus(Long senderId, Transaction.TransactionType type,
                        Transaction.TransactionStatus status);

        List<Transaction> findByReceiverIdAndTypeAndStatus(Long receiverId, Transaction.TransactionType type,
                        Transaction.TransactionStatus status);

        List<Transaction> findByReceiverIdAndStatusAndTypeIn(Long receiverId, Transaction.TransactionStatus status,
                        java.util.Collection<Transaction.TransactionType> types);
}
