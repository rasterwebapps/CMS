package com.cms.inventory.issue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.issue.model.StockIssueRequest;

@Repository
public interface StockIssueRequestRepository extends JpaRepository<StockIssueRequest, Long>, JpaSpecificationExecutor<StockIssueRequest> {
}
