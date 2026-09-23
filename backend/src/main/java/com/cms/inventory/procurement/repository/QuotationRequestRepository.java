package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.QuotationRequest;

@Repository
public interface QuotationRequestRepository
        extends JpaRepository<QuotationRequest, Long>, JpaSpecificationExecutor<QuotationRequest> {

    /** Oldest-first — the order QuotationRequestService.regenerateQuotationNumbers renumbers by. */
    List<QuotationRequest> findAllByOrderByRequestDateAscIdAsc();
}
