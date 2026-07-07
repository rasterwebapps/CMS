package com.cms.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibraryPeriodicalRepository;

/**
 * Books and journals share a single accession-number register (one running
 * ledger across both, matching how a physical library's Accession Register
 * works) even though each lives in its own table. This centralizes the
 * cross-table uniqueness check and next-number generation so both
 * LibraryBookService and LibraryPeriodicalService stay in sync.
 */
@Service
public class LibraryAccessionRegistryService {

    private final LibraryBookRepository bookRepository;
    private final LibraryPeriodicalRepository periodicalRepository;

    public LibraryAccessionRegistryService(LibraryBookRepository bookRepository,
                                            LibraryPeriodicalRepository periodicalRepository) {
        this.bookRepository = bookRepository;
        this.periodicalRepository = periodicalRepository;
    }

    /**
     * @param excludeBookId       when checking an edit to an existing book, its own id (else null)
     * @param excludePeriodicalId when checking an edit to an existing periodical, its own id (else null)
     */
    public boolean exists(String accessionNumber, Long excludeBookId, Long excludePeriodicalId) {
        boolean bookExists = excludeBookId != null
            ? bookRepository.existsByAccessionNumberAndIdNot(accessionNumber, excludeBookId)
            : bookRepository.existsByAccessionNumber(accessionNumber);
        if (bookExists) return true;

        return excludePeriodicalId != null
            ? periodicalRepository.existsByAccessionNumberAndIdNot(accessionNumber, excludePeriodicalId)
            : periodicalRepository.existsByAccessionNumber(accessionNumber);
    }

    /**
     * If the caller supplied an accession number, use it (after trim).
     * Otherwise generate the next year-prefixed number: {YEAR}-{N+1}
     * where N is the highest numeric suffix across all existing books AND journals.
     */
    public String resolveAccessionNumber(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        int year = LocalDate.now().getYear();
        return year + "-" + nextSequence();
    }

    private long nextSequence() {
        long maxBook = bookRepository.findAll().stream()
            .mapToLong(b -> parseNumericSuffix(b.getAccessionNumber()))
            .max()
            .orElse(0L);
        long maxPeriodical = periodicalRepository.findAll().stream()
            .mapToLong(p -> parseNumericSuffix(p.getAccessionNumber()))
            .max()
            .orElse(0L);
        return Math.max(maxBook, maxPeriodical) + 1L;
    }

    private static long parseNumericSuffix(String accessionNumber) {
        if (accessionNumber == null) return 0L;
        // Handles both "3001" and "2026-3001"
        int dash = accessionNumber.lastIndexOf('-');
        String numeric = dash >= 0 ? accessionNumber.substring(dash + 1) : accessionNumber;
        try {
            return Long.parseLong(numeric.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
