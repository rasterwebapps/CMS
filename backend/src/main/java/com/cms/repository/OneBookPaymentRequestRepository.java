package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.OneBookPaymentRequest;

public interface OneBookPaymentRequestRepository extends JpaRepository<OneBookPaymentRequest, Long> {

    Optional<OneBookPaymentRequest> findByInvoiceNumber(String invoiceNumber);

    List<OneBookPaymentRequest> findByEntityIdAndPaymentTypeOrderByCreatedAtDesc(Long entityId, String paymentType);

    Optional<OneBookPaymentRequest> findTopByEntityIdAndPaymentTypeOrderByCreatedAtDesc(Long entityId, String paymentType);
}
