package com.cms.inventory.consignment.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.consignment.model.ConsignmentStockLine;

@Repository
public interface ConsignmentStockLineRepository extends JpaRepository<ConsignmentStockLine, Long>, JpaSpecificationExecutor<ConsignmentStockLine> {

    Optional<ConsignmentStockLine> findByAgreementIdAndProductId(Long agreementId, Long productId);

    /** Total value still owed to suppliers for received-but-not-yet-consumed consignment stock
     *  across every line — the Inventory Dashboard's "outstanding liability" figure. See the
     *  "Inventory Dashboard slice" decision-log entry. */
    @Query("SELECT COALESCE(SUM((l.receivedQty - l.consumedQty) * l.consignmentPrice), 0) FROM ConsignmentStockLine l")
    BigDecimal sumOutstandingLiabilityValue();
}
