package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.QuotationRequestLine;
import com.cms.inventory.procurement.model.enums.QuotationRequestLineStatus;

@Repository
public interface QuotationRequestLineRepository extends JpaRepository<QuotationRequestLine, Long> {

    List<QuotationRequestLine> findByQuotationRequestIdOrderByIdAsc(Long quotationRequestId);

    boolean existsByQuotationRequestIdAndStatus(Long quotationRequestId, QuotationRequestLineStatus status);

    boolean existsByPurchaseRequisitionItemIdAndStatusNot(Long purchaseRequisitionItemId, QuotationRequestLineStatus status);

    /**
     * {@code AWARDED} lines not yet picked up into a Purchase Order — the pool {@code
     * QuotationRequestService.convertAwardedLines} groups by winning supplier.
     */
    @Query("SELECT l FROM QuotationRequestLine l WHERE l.quotationRequest.id = :quotationRequestId AND l.status = 'AWARDED'")
    List<QuotationRequestLine> findAwardedNotYetOrdered(@Param("quotationRequestId") Long quotationRequestId);
}
