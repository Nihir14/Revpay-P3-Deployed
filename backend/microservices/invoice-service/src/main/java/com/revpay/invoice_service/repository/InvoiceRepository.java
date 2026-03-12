package com.revpay.invoice_service.repository;

import com.revpay.invoice_service.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByBusinessProfileUserId(Long userId);

    List<Invoice> findByCustomerEmail(String email);
}
