package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryBookRequest;
import com.cms.dto.LibraryBookResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibraryBook;
import com.cms.model.enums.BookStatus;
import com.cms.repository.LibraryBookRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class LibraryBookService {

    private final LibraryBookRepository bookRepository;

    public LibraryBookService(LibraryBookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public LibraryBookResponse create(LibraryBookRequest request) {
        String accessionNumber = resolveAccessionNumber(request.accessionNumber());

        if (bookRepository.existsByAccessionNumber(accessionNumber)) {
            throw new IllegalArgumentException(
                "A book with accession number '" + accessionNumber + "' already exists");
        }

        LibraryBook book = new LibraryBook();
        applyFields(book, request);
        book.setAccessionNumber(accessionNumber);

        return toResponse(bookRepository.save(book));
    }

    public List<LibraryBookResponse> findAll() {
        return bookRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Page<LibraryBookResponse> findPage(String search, BookStatus status, String category, Pageable pageable) {
        Specification<LibraryBook> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String p = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), p),
                    cb.like(cb.lower(root.get("authors")), p),
                    cb.like(cb.lower(root.get("accessionNumber")), p),
                    cb.like(cb.lower(root.get("subjectCategory")), p)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (category != null && !category.isBlank()) predicates.add(cb.equal(root.get("subjectCategory"), category));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return bookRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public List<LibraryBookResponse> findByStatus(BookStatus status) {
        return bookRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    public LibraryBookResponse findById(Long id) {
        return toResponse(require(id));
    }

    public boolean accessionNumberExists(String accessionNumber, Long excludeId) {
        if (excludeId != null) {
            return bookRepository.existsByAccessionNumberAndIdNot(accessionNumber, excludeId);
        }
        return bookRepository.existsByAccessionNumber(accessionNumber);
    }

    @Transactional
    public LibraryBookResponse update(Long id, LibraryBookRequest request) {
        LibraryBook book = require(id);

        String accessionNumber = request.accessionNumber() != null
            ? request.accessionNumber().trim()
            : book.getAccessionNumber();

        if (!accessionNumber.equals(book.getAccessionNumber())
                && bookRepository.existsByAccessionNumberAndIdNot(accessionNumber, id)) {
            throw new IllegalArgumentException(
                "A book with accession number '" + accessionNumber + "' already exists");
        }

        book.setAccessionNumber(accessionNumber);
        applyFields(book, request);
        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void delete(Long id) {
        LibraryBook book = require(id);
        if (book.getStatus() == BookStatus.ISSUED) {
            throw new IllegalStateException("Cannot delete a book that is currently issued");
        }
        bookRepository.deleteById(id);
    }

    // ── Accession number generation ───────────────────────────────

    /**
     * If the caller supplied an accession number, use it (after trim).
     * Otherwise generate the next year-prefixed number: {YEAR}-{N+1}
     * where N is the highest numeric suffix across all existing records.
     */
    String resolveAccessionNumber(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        int year = LocalDate.now().getYear();
        long nextSeq = nextSequence();
        return year + "-" + nextSeq;
    }

    private long nextSequence() {
        return bookRepository.findAll().stream()
            .mapToLong(b -> parseNumericSuffix(b.getAccessionNumber()))
            .max()
            .orElse(0L) + 1L;
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

    // ── Helpers ───────────────────────────────────────────────────

    private LibraryBook require(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private void applyFields(LibraryBook book, LibraryBookRequest r) {
        book.setEntryDate(r.entryDate());
        book.setTitle(trim(r.title()));
        book.setAuthors(trim(r.authors()));
        book.setPublisher(trim(r.publisher()));
        book.setYearOfPublication(trim(r.yearOfPublication()));
        book.setEdition(trim(r.edition()));
        book.setIsbn(trim(r.isbn()));
        book.setCollation(trim(r.collation()));
        book.setSeries(trim(r.series()));
        book.setCallNumber(trim(r.callNumber()));
        book.setShelfLocation(trim(r.shelfLocation()));
        book.setSubjectCategory(trim(r.subjectCategory()));
        book.setSourceOfSupply(r.sourceOfSupply());
        book.setVendorDonorName(trim(r.vendorDonorName()));
        book.setBillNumber(trim(r.billNumber()));
        book.setBillDate(r.billDate());
        book.setPriceRs(r.priceRs());
        if (r.status() != null) book.setStatus(r.status());
        book.setRemarks(trim(r.remarks()));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    LibraryBookResponse toResponse(LibraryBook b) {
        return new LibraryBookResponse(
            b.getId(),
            b.getAccessionNumber(),
            b.getEntryDate(),
            b.getTitle(),
            b.getAuthors(),
            b.getPublisher(),
            b.getYearOfPublication(),
            b.getEdition(),
            b.getIsbn(),
            b.getCollation(),
            b.getSeries(),
            b.getCallNumber(),
            b.getShelfLocation(),
            b.getSubjectCategory(),
            b.getSourceOfSupply(),
            b.getVendorDonorName(),
            b.getBillNumber(),
            b.getBillDate(),
            b.getPriceRs(),
            b.getStatus(),
            b.getRemarks(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
    }
}
