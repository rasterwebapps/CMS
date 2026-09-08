package com.cms.inventory.issue.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.issue.dto.LoanableItemIssueCreateRequest;
import com.cms.inventory.issue.dto.LoanableItemIssueResponse;
import com.cms.inventory.issue.dto.LoanableItemIssueReturnRequest;
import com.cms.inventory.issue.model.LoanableItemIssue;
import com.cms.inventory.issue.model.enums.LoanableItemIssueStatus;
import com.cms.inventory.issue.repository.LoanableItemIssueRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns the Loanable Item Issue workflow — Phase 4's ("Requests, Issues & Returns") fourth and
 * final slice, closing the phase. A generic "borrow and return" record for any {@link Product}
 * flagged {@code isLoanable} — deliberately standalone, not integrated with {@code
 * StockLedger}/{@code StockBalance} (see the {@link LoanableItemIssue} class docs for why). Issue
 * and return are each a single direct action; "overdue" is computed at read time, never stored.
 * See the "Loanable Item Issue slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class LoanableItemIssueService {

    private final LoanableItemIssueRepository issueRepository;
    private final ProductRepository productRepository;
    private final InventoryLocationRepository locationRepository;

    public LoanableItemIssueService(LoanableItemIssueRepository issueRepository,
                                     ProductRepository productRepository,
                                     InventoryLocationRepository locationRepository) {
        this.issueRepository = issueRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public LoanableItemIssueResponse create(LoanableItemIssueCreateRequest request, String issuedBy) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));
        if (!Boolean.TRUE.equals(product.getIsLoanable())) {
            throw new IllegalArgumentException("'" + product.getProductName() + "' is not flagged as loanable");
        }
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : LocalDate.now();
        if (request.expectedReturnDate().isBefore(issueDate)) {
            throw new IllegalArgumentException("Expected return date cannot be before the issue date");
        }

        LoanableItemIssue issue = new LoanableItemIssue();
        issue.setProduct(product);
        issue.setLocation(location);
        issue.setBorrowerName(request.borrowerName().trim());
        issue.setBorrowerContact(trim(request.borrowerContact()));
        issue.setStatus(LoanableItemIssueStatus.ISSUED);
        issue.setIssueDate(issueDate);
        issue.setExpectedReturnDate(request.expectedReturnDate());
        issue.setConditionOnIssue(trim(request.conditionOnIssue()));
        issue.setNotes(trim(request.notes()));
        issue.setIssuedBy(issuedBy);
        issue.setIssuedAt(Instant.now());
        issue.setUpdatedAt(Instant.now());
        return toResponse(issueRepository.save(issue));
    }

    public Page<LoanableItemIssueResponse> findPage(Long locationId, String status, Boolean overdueOnly, Pageable pageable) {
        Specification<LoanableItemIssue> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            if (Boolean.TRUE.equals(overdueOnly)) {
                predicate = cb.and(predicate,
                    cb.equal(root.get("status"), LoanableItemIssueStatus.ISSUED),
                    cb.lessThan(root.get("expectedReturnDate"), LocalDate.now()));
            }
            return predicate;
        };
        return issueRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public LoanableItemIssueResponse findById(Long id) {
        return toResponse(requireIssue(id));
    }

    @Transactional
    public LoanableItemIssueResponse markReturned(Long id, LoanableItemIssueReturnRequest request, String returnedBy) {
        LoanableItemIssue issue = requireIssue(id);
        if (issue.getStatus() != LoanableItemIssueStatus.ISSUED) {
            throw new IllegalArgumentException("This item has already been returned");
        }
        issue.setStatus(LoanableItemIssueStatus.RETURNED);
        issue.setActualReturnDate(LocalDate.now());
        issue.setConditionOnReturn(trim(request != null ? request.conditionOnReturn() : null));
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            issue.setNotes(issue.getNotes() != null ? issue.getNotes() + " | " + request.notes().trim() : request.notes().trim());
        }
        issue.setReturnedBy(returnedBy);
        issue.setReturnedAt(Instant.now());
        issue.setUpdatedAt(Instant.now());
        return toResponse(issueRepository.save(issue));
    }

    private LoanableItemIssue requireIssue(Long id) {
        return issueRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Loanable item issue not found with id: " + id));
    }

    private LoanableItemIssueStatus parseStatus(String value) {
        try {
            return LoanableItemIssueStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private LoanableItemIssueResponse toResponse(LoanableItemIssue issue) {
        Product product = issue.getProduct();
        InventoryLocation location = issue.getLocation();
        boolean overdue = issue.getStatus() == LoanableItemIssueStatus.ISSUED
            && issue.getExpectedReturnDate().isBefore(LocalDate.now());
        return new LoanableItemIssueResponse(
            issue.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            location.getId(), location.getVirtualName(),
            issue.getBorrowerName(), issue.getBorrowerContact(),
            issue.getStatus().name(), issue.getIssueDate(), issue.getExpectedReturnDate(), issue.getActualReturnDate(),
            overdue, issue.getConditionOnIssue(), issue.getConditionOnReturn(), issue.getNotes(),
            issue.getIssuedBy(), issue.getIssuedAt(), issue.getReturnedBy(), issue.getReturnedAt());
    }
}
