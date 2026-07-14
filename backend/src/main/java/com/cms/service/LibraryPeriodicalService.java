package com.cms.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryPeriodicalRequest;
import com.cms.dto.LibraryPeriodicalResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibraryPeriodical;
import com.cms.model.enums.BookStatus;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;
import com.cms.repository.LibraryPeriodicalRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class LibraryPeriodicalService {

    private final LibraryPeriodicalRepository repository;
    private final LibraryAccessionRegistryService accessionRegistry;

    public LibraryPeriodicalService(LibraryPeriodicalRepository repository, LibraryAccessionRegistryService accessionRegistry) {
        this.repository = repository;
        this.accessionRegistry = accessionRegistry;
    }

    @Transactional
    public LibraryPeriodicalResponse create(LibraryPeriodicalRequest request) {
        String accessionNumber = accessionRegistry.resolveAccessionNumber(request.accessionNumber());
        if (accessionRegistry.exists(accessionNumber, null, null)) {
            throw new IllegalArgumentException(
                "An item with accession number '" + accessionNumber + "' already exists");
        }

        String barcode = accessionRegistry.resolveBarcode(request.barcode(), accessionNumber);
        if (accessionRegistry.existsBarcode(barcode, null, null)) {
            throw new IllegalArgumentException(
                "An item with barcode '" + barcode + "' already exists");
        }

        LibraryPeriodical p = new LibraryPeriodical();
        applyFields(p, request);
        p.setAccessionNumber(accessionNumber);
        p.setBarcode(barcode);
        return toResponse(repository.save(p));
    }

    public boolean accessionNumberExists(String accessionNumber, Long excludeId) {
        return accessionRegistry.exists(accessionNumber, null, excludeId);
    }

    public boolean barcodeExists(String barcode, Long excludeId) {
        return accessionRegistry.existsBarcode(barcode, null, excludeId);
    }

    public List<LibraryPeriodicalResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public Page<LibraryPeriodicalResponse> findPage(String search, SubscriptionStatus subscriptionStatus, JournalType journalType, Pageable pageable) {
        return repository.findAll(buildSpec(search, subscriptionStatus, journalType), pageable).map(this::toResponse);
    }

    /** Unpaged, filtered, sorted — used by the Export endpoint (respects the same filters as findPage). */
    public List<LibraryPeriodicalResponse> findAllMatching(String search, SubscriptionStatus subscriptionStatus, JournalType journalType, Sort sort) {
        return repository.findAll(buildSpec(search, subscriptionStatus, journalType), sort).stream()
            .map(this::toResponse).toList();
    }

    private Specification<LibraryPeriodical> buildSpec(String search, SubscriptionStatus subscriptionStatus, JournalType journalType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String p = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("journalName")), p),
                    cb.like(cb.lower(root.get("organization")), p),
                    cb.like(cb.lower(root.get("accessionNumber")), p)
                ));
            }
            if (subscriptionStatus != null) predicates.add(cb.equal(root.get("subscriptionStatus"), subscriptionStatus));
            if (journalType != null) predicates.add(cb.equal(root.get("journalType"), journalType));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<LibraryPeriodicalResponse> findByStatus(SubscriptionStatus status) {
        return repository.findBySubscriptionStatus(status).stream().map(this::toResponse).toList();
    }

    public List<LibraryPeriodicalResponse> findByType(JournalType type) {
        return repository.findByJournalType(type).stream().map(this::toResponse).toList();
    }

    public LibraryPeriodicalResponse findById(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public LibraryPeriodicalResponse update(Long id, LibraryPeriodicalRequest request) {
        LibraryPeriodical p = require(id);

        String accessionNumber = request.accessionNumber() != null && !request.accessionNumber().isBlank()
            ? request.accessionNumber().trim()
            : p.getAccessionNumber();

        if (accessionNumber != null
                && !accessionNumber.equals(p.getAccessionNumber())
                && accessionRegistry.exists(accessionNumber, null, id)) {
            throw new IllegalArgumentException(
                "An item with accession number '" + accessionNumber + "' already exists");
        }

        // Barcode auto-defaults from accession number only at creation; a blank barcode on
        // update keeps whatever value the item already has rather than resetting it.
        String barcode = request.barcode() != null && !request.barcode().isBlank()
            ? request.barcode().trim()
            : p.getBarcode();

        if (barcode != null && !barcode.equals(p.getBarcode())
                && accessionRegistry.existsBarcode(barcode, null, id)) {
            throw new IllegalArgumentException(
                "An item with barcode '" + barcode + "' already exists");
        }

        applyFields(p, request);
        p.setAccessionNumber(accessionNumber);
        p.setBarcode(barcode);
        return toResponse(repository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        LibraryPeriodical p = require(id);
        if (p.getStatus() == BookStatus.ISSUED) {
            throw new IllegalStateException("Cannot delete a journal copy that is currently issued");
        }
        repository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private LibraryPeriodical require(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Periodical not found with id: " + id));
    }

    private void applyFields(LibraryPeriodical p, LibraryPeriodicalRequest r) {
        p.setJournalName(trim(r.journalName()));
        p.setJournalType(r.journalType() != null ? r.journalType() : JournalType.NATIONAL);
        p.setOrganization(trim(r.organization()));
        p.setVolumeNumber(trim(r.volumeNumber()));
        p.setIssueNumber(trim(r.issueNumber()));
        p.setMonthRange(trim(r.monthRange()));
        p.setYear(r.year());
        p.setSubscriptionStatus(r.subscriptionStatus() != null ? r.subscriptionStatus() : SubscriptionStatus.ACTIVE);
        if (r.status() != null) p.setStatus(r.status());
        p.setReceivedDate(r.receivedDate());
        p.setReceivedBy(trim(r.receivedBy()));
        p.setRemarks(trim(r.remarks()));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private LibraryPeriodicalResponse toResponse(LibraryPeriodical p) {
        return new LibraryPeriodicalResponse(
            p.getId(),
            p.getAccessionNumber(),
            p.getBarcode(),
            p.getJournalName(),
            p.getJournalType(),
            p.getOrganization(),
            p.getVolumeNumber(),
            p.getIssueNumber(),
            p.getMonthRange(),
            p.getYear(),
            p.getSubscriptionStatus(),
            p.getStatus(),
            p.getReceivedDate(),
            p.getReceivedBy(),
            p.getRemarks(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
