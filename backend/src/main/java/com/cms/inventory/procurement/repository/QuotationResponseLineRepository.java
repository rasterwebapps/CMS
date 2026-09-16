package com.cms.inventory.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.QuotationResponseLine;

@Repository
public interface QuotationResponseLineRepository extends JpaRepository<QuotationResponseLine, Long> {

    List<QuotationResponseLine> findByQuotationRequestLineIdOrderByIdAsc(Long quotationRequestLineId);

    Optional<QuotationResponseLine> findByQuotationRequestLineIdAndSupplierId(Long quotationRequestLineId, Long supplierId);
}
