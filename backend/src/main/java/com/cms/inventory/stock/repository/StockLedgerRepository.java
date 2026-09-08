package com.cms.inventory.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.StockLedger;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {
}
