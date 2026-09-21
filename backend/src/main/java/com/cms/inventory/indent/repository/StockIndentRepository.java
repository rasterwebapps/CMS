package com.cms.inventory.indent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.indent.model.StockIndent;

@Repository
public interface StockIndentRepository extends JpaRepository<StockIndent, Long>, JpaSpecificationExecutor<StockIndent> {
}
