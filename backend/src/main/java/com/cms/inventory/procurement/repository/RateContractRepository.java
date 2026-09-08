package com.cms.inventory.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.RateContract;

@Repository
public interface RateContractRepository extends JpaRepository<RateContract, Long>, JpaSpecificationExecutor<RateContract> {
}
