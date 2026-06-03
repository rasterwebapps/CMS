package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryPeriodicalRequest;
import com.cms.dto.LibraryPeriodicalResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibraryPeriodical;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;
import com.cms.repository.LibraryPeriodicalRepository;

@Service
@Transactional(readOnly = true)
public class LibraryPeriodicalService {

    private final LibraryPeriodicalRepository repository;

    public LibraryPeriodicalService(LibraryPeriodicalRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LibraryPeriodicalResponse create(LibraryPeriodicalRequest request) {
        LibraryPeriodical p = new LibraryPeriodical();
        applyFields(p, request);
        return toResponse(repository.save(p));
    }

    public List<LibraryPeriodicalResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
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
        applyFields(p, request);
        return toResponse(repository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Periodical not found with id: " + id);
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
        p.setCopiesCount(r.copiesCount() != null ? r.copiesCount() : 1);
        p.setSubscriptionStatus(r.subscriptionStatus() != null ? r.subscriptionStatus() : SubscriptionStatus.ACTIVE);
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
            p.getJournalName(),
            p.getJournalType(),
            p.getOrganization(),
            p.getVolumeNumber(),
            p.getIssueNumber(),
            p.getMonthRange(),
            p.getYear(),
            p.getCopiesCount(),
            p.getSubscriptionStatus(),
            p.getReceivedDate(),
            p.getReceivedBy(),
            p.getRemarks(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
