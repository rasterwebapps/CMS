package com.cms.inventory.receiving.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.receiving.model.SupplierReturn;

@Repository
public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, Long>, JpaSpecificationExecutor<SupplierReturn> {
}
