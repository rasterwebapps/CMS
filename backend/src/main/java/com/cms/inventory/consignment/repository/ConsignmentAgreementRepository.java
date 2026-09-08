package com.cms.inventory.consignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.consignment.model.ConsignmentAgreement;

@Repository
public interface ConsignmentAgreementRepository extends JpaRepository<ConsignmentAgreement, Long>, JpaSpecificationExecutor<ConsignmentAgreement> {
}
