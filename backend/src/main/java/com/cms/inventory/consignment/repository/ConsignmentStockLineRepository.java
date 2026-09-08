package com.cms.inventory.consignment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.consignment.model.ConsignmentStockLine;

@Repository
public interface ConsignmentStockLineRepository extends JpaRepository<ConsignmentStockLine, Long>, JpaSpecificationExecutor<ConsignmentStockLine> {

    Optional<ConsignmentStockLine> findByAgreementIdAndProductId(Long agreementId, Long productId);
}
