package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.QuotationRequestSupplier;

@Repository
public interface QuotationRequestSupplierRepository extends JpaRepository<QuotationRequestSupplier, Long> {

    List<QuotationRequestSupplier> findByQuotationRequestIdOrderByIdAsc(Long quotationRequestId);

    boolean existsByQuotationRequestIdAndSupplierId(Long quotationRequestId, Long supplierId);

    void deleteByQuotationRequestIdAndSupplierId(Long quotationRequestId, Long supplierId);
}
