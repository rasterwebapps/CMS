package com.cms.inventory.gatepass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.gatepass.model.GatePass;

@Repository
public interface GatePassRepository extends JpaRepository<GatePass, Long>, JpaSpecificationExecutor<GatePass> {
}
