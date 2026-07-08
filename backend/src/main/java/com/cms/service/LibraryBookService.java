package com.cms.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryBookBulkTransferRequest;
import com.cms.dto.LibraryBookRequest;
import com.cms.dto.LibraryBookResponse;
import com.cms.dto.LibraryBookShelfTransferResponse;
import com.cms.dto.LibraryBookTransferRequest;
import com.cms.dto.LibraryBookTransferResult;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Library;
import com.cms.model.LibraryBook;
import com.cms.model.LibraryBookShelfTransfer;
import com.cms.model.LibraryShelf;
import com.cms.model.enums.BookStatus;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibraryBookShelfTransferRepository;
import com.cms.repository.LibraryRepository;
import com.cms.repository.LibraryShelfRepository;
import com.cms.util.CurrentUserResolver;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class LibraryBookService {

    private final LibraryBookRepository bookRepository;
    private final LibraryAccessionRegistryService accessionRegistry;
    private final LibraryRepository libraryRepository;
    private final LibraryShelfRepository shelfRepository;
    private final LibraryBookShelfTransferRepository transferRepository;
    private final CurrentUserResolver currentUserResolver;

    public LibraryBookService(LibraryBookRepository bookRepository,
                               LibraryAccessionRegistryService accessionRegistry,
                               LibraryRepository libraryRepository,
                               LibraryShelfRepository shelfRepository,
                               LibraryBookShelfTransferRepository transferRepository,
                               CurrentUserResolver currentUserResolver) {
        this.bookRepository = bookRepository;
        this.accessionRegistry = accessionRegistry;
        this.libraryRepository = libraryRepository;
        this.shelfRepository = shelfRepository;
        this.transferRepository = transferRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public LibraryBookResponse create(LibraryBookRequest request) {
        String accessionNumber = accessionRegistry.resolveAccessionNumber(request.accessionNumber());

        if (accessionRegistry.exists(accessionNumber, null, null)) {
            throw new IllegalArgumentException(
                "An item with accession number '" + accessionNumber + "' already exists");
        }

        LibraryBook book = new LibraryBook();
        applyFields(book, request);
        book.setAccessionNumber(accessionNumber);

        return toResponse(bookRepository.save(book));
    }

    public List<LibraryBookResponse> findAll() {
        return bookRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Page<LibraryBookResponse> findPage(String search, BookStatus status, String category, Long rackId, Long shelfId, Pageable pageable) {
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
            if (shelfId != null) predicates.add(cb.equal(root.get("shelf").get("id"), shelfId));
            else if (rackId != null) predicates.add(cb.equal(root.get("shelf").get("rack").get("id"), rackId));
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
        return accessionRegistry.exists(accessionNumber, excludeId, null);
    }

    @Transactional
    public LibraryBookResponse update(Long id, LibraryBookRequest request) {
        LibraryBook book = require(id);

        String accessionNumber = request.accessionNumber() != null
            ? request.accessionNumber().trim()
            : book.getAccessionNumber();

        if (!accessionNumber.equals(book.getAccessionNumber())
                && accessionRegistry.exists(accessionNumber, id, null)) {
            throw new IllegalArgumentException(
                "An item with accession number '" + accessionNumber + "' already exists");
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

    // ── Shelf transfer ───────────────────────────────────────────

    @Transactional
    public LibraryBookShelfTransferResponse transferBook(Long bookId, LibraryBookTransferRequest request) {
        LibraryBook book = require(bookId);
        LibraryBookShelfTransfer record = doTransfer(book, request.newShelfId(), request.notes());
        return toTransferResponse(record);
    }

    @Transactional
    public LibraryBookTransferResult bulkTransfer(LibraryBookBulkTransferRequest request) {
        List<Long> succeeded = new ArrayList<>();
        List<LibraryBookTransferResult.Failure> failed = new ArrayList<>();

        for (Long bookId : request.bookIds()) {
            try {
                LibraryBook book = require(bookId);
                doTransfer(book, request.newShelfId(), request.notes());
                succeeded.add(bookId);
            } catch (IllegalStateException | ResourceNotFoundException e) {
                failed.add(new LibraryBookTransferResult.Failure(bookId, e.getMessage()));
            }
        }
        return new LibraryBookTransferResult(succeeded, failed);
    }

    public List<LibraryBookShelfTransferResponse> getTransferHistory(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found with id: " + bookId);
        }
        return transferRepository.findByBookIdOrderByTransferredAtDesc(bookId).stream()
            .map(this::toTransferResponse)
            .toList();
    }

    private LibraryBookShelfTransfer doTransfer(LibraryBook book, Long newShelfId, String notes) {
        if (book.getStatus() == BookStatus.ISSUED) {
            throw new IllegalStateException(
                "Cannot transfer '" + book.getTitle() + "' — it is currently issued");
        }

        LibraryShelf newShelf = shelfRepository.findById(newShelfId)
            .orElseThrow(() -> new ResourceNotFoundException("Shelf not found with id: " + newShelfId));
        Library newLibrary = newShelf.getRack().getLibrary();

        LibraryBookShelfTransfer record = new LibraryBookShelfTransfer();
        record.setBook(book);
        record.setOldLibrary(book.getLibrary());
        record.setOldRack(book.getShelf() != null ? book.getShelf().getRack() : null);
        record.setOldShelf(book.getShelf());
        record.setNewLibrary(newLibrary);
        record.setNewRack(newShelf.getRack());
        record.setNewShelf(newShelf);
        record.setTransferredAt(Instant.now());
        record.setTransferredBy(currentUserResolver.resolve());
        record.setNotes(notes);

        book.setLibrary(newLibrary);
        book.setShelf(newShelf);
        bookRepository.save(book);

        return transferRepository.save(record);
    }

    private LibraryBookShelfTransferResponse toTransferResponse(LibraryBookShelfTransfer t) {
        return new LibraryBookShelfTransferResponse(
            t.getId(),
            t.getBook().getId(),
            t.getOldLibrary() != null ? t.getOldLibrary().getName() : null,
            t.getOldRack() != null ? t.getOldRack().getName() : null,
            t.getOldShelf() != null ? t.getOldShelf().getName() : null,
            t.getNewLibrary().getName(),
            t.getNewRack() != null ? t.getNewRack().getName() : null,
            t.getNewShelf() != null ? t.getNewShelf().getName() : null,
            t.getTransferredAt(),
            t.getTransferredBy(),
            t.getNotes()
        );
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

        Library library = libraryRepository.findById(r.libraryId())
            .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + r.libraryId()));
        book.setLibrary(library);

        if (r.shelfId() != null) {
            LibraryShelf shelf = shelfRepository.findById(r.shelfId())
                .orElseThrow(() -> new ResourceNotFoundException("Shelf not found with id: " + r.shelfId()));
            book.setShelf(shelf);
        } else {
            book.setShelf(null);
        }

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
        LibraryShelf shelf = b.getShelf();
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
            b.getLibrary() != null ? b.getLibrary().getId() : null,
            b.getLibrary() != null ? b.getLibrary().getName() : null,
            shelf != null ? shelf.getRack().getId() : null,
            shelf != null ? shelf.getRack().getName() : null,
            shelf != null ? shelf.getId() : null,
            shelf != null ? shelf.getName() : null,
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
