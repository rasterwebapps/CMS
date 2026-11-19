package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LibraryBookBulkTransferRequest;
import com.cms.dto.LibraryBookTransferRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Library;
import com.cms.model.LibraryBook;
import com.cms.model.LibraryRack;
import com.cms.model.LibraryShelf;
import com.cms.model.enums.BookStatus;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibraryBookShelfTransferRepository;
import com.cms.repository.LibraryIssueRepository;
import com.cms.repository.LibraryRepository;
import com.cms.repository.LibraryShelfRepository;
import com.cms.util.CurrentUserResolver;

@ExtendWith(MockitoExtension.class)
class LibraryBookServiceTest {

    @Mock private LibraryBookRepository bookRepository;
    @Mock private LibraryAccessionRegistryService accessionRegistry;
    @Mock private LibraryRepository libraryRepository;
    @Mock private LibraryShelfRepository shelfRepository;
    @Mock private LibraryBookShelfTransferRepository transferRepository;
    @Mock private LibraryIssueRepository issueRepository;
    @Mock private CurrentUserResolver currentUserResolver;

    private LibraryBookService service;
    private Library library;

    @BeforeEach
    void setUp() {
        service = new LibraryBookService(bookRepository, accessionRegistry, libraryRepository,
            shelfRepository, transferRepository, issueRepository, currentUserResolver);

        library = new Library();
        library.setId(1L);
        library.setName("Main Library");
        lenient().when(libraryRepository.findById(1L)).thenReturn(Optional.of(library));
        lenient().when(currentUserResolver.resolve()).thenReturn("librarian1");
        lenient().when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── create ───────────────────────────────────────────────────

    @Test
    void create_happyPath_savesBook() {
        when(accessionRegistry.resolveAccessionNumber("2024-001")).thenReturn("2024-001");
        when(accessionRegistry.exists("2024-001", null, null)).thenReturn(false);

        LibraryBook saved = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        when(bookRepository.save(any())).thenReturn(saved);

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, null, null
        );

        var response = service.create(request);

        assertThat(response.accessionNumber()).isEqualTo("2024-001");
        assertThat(response.title()).isEqualTo("Anatomy");
        verify(bookRepository).save(any());
    }

    @Test
    void create_duplicateAccessionNumber_throws() {
        when(accessionRegistry.resolveAccessionNumber("2024-001")).thenReturn("2024-001");
        when(accessionRegistry.exists("2024-001", null, null)).thenReturn(true);

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2024-001");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void create_autoGeneratesAccessionNumber_whenNotProvided() {
        when(accessionRegistry.resolveAccessionNumber(null)).thenReturn("2026-6");
        when(accessionRegistry.exists("2026-6", null, null)).thenReturn(false);

        LibraryBook saved = book(2L, "2026-6", "New Book", BookStatus.AVAILABLE);
        when(bookRepository.save(any())).thenReturn(saved);

        var request = new com.cms.dto.LibraryBookRequest(
            null, null, null, "New Book", "Author", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, null, null
        );

        var response = service.create(request);
        assertThat(response).isNotNull();
        verify(bookRepository).save(any());
    }

    // ── status lifecycle guards ──────────────────────────────────

    @Test
    void create_withIssuedStatus_throws() {
        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, BookStatus.ISSUED, null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ISSUED");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void update_whenBookIsIssued_changingStatusThrows() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.ISSUED);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, BookStatus.WITHDRAWN, null
        );

        assertThatThrownBy(() -> service.update(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("currently issued");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void update_whenBookIsAvailable_settingStatusToIssuedThrows() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, BookStatus.ISSUED, null
        );

        assertThatThrownBy(() -> service.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ISSUED");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void update_whenBookIsAvailable_changingStatusToWithdrawnSucceeds() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, BookStatus.WITHDRAWN, "Withdrawn from stock"
        );

        var response = service.update(1L, request);

        assertThat(response.status()).isEqualTo(BookStatus.WITHDRAWN);
        verify(bookRepository).save(book);
    }

    // ── transfer ─────────────────────────────────────────────────

    @Test
    void transferBook_issuedBook_throws() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.ISSUED);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new LibraryBookTransferRequest(10L, "moving shelf");

        assertThatThrownBy(() -> service.transferBook(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("currently issued");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferBook_lostBook_throws() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.LOST);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new LibraryBookTransferRequest(10L, "moving shelf");

        assertThatThrownBy(() -> service.transferBook(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("marked as lost");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferBook_withdrawnBook_throws() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.WITHDRAWN);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new LibraryBookTransferRequest(10L, "moving shelf");

        assertThatThrownBy(() -> service.transferBook(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("withdrawn from stock");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferBook_sameShelf_throws() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        LibraryShelf currentShelf = shelf(10L, "Tier 1", rack(5L, "Rack B", library));
        book.setShelf(currentShelf);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new LibraryBookTransferRequest(10L, "moving shelf");

        assertThatThrownBy(() -> service.transferBook(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already on this shelf");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferBook_damagedBook_movesShelfAndLibrary() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.DAMAGED);
        book.setLibrary(library);

        LibraryShelf newShelf = shelf(10L, "Repair Tier", rack(5L, "Rack B", library));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(shelfRepository.findById(10L)).thenReturn(Optional.of(newShelf));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LibraryBookTransferRequest(10L, "moving damaged book");
        var response = service.transferBook(1L, request);

        assertThat(response.newShelfName()).isEqualTo("Repair Tier");
        assertThat(book.getShelf()).isEqualTo(newShelf);
        verify(bookRepository).save(book);
        verify(transferRepository).save(any());
    }

    @Test
    void transferBook_availableBook_movesShelfAndLibrary() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        book.setLibrary(library);

        LibraryShelf newShelf = shelf(10L, "Tier 1", rack(5L, "Rack B", library));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(shelfRepository.findById(10L)).thenReturn(Optional.of(newShelf));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LibraryBookTransferRequest(10L, "moving shelf");
        var response = service.transferBook(1L, request);

        assertThat(response.newShelfName()).isEqualTo("Tier 1");
        assertThat(response.newRackName()).isEqualTo("Rack B");
        assertThat(book.getShelf()).isEqualTo(newShelf);
        verify(bookRepository).save(book);
        verify(transferRepository).save(any());
    }

    @Test
    void bulkTransfer_partialSuccess_whenOneBookIsIssued() {
        LibraryBook available = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        available.setLibrary(library);
        LibraryBook issued = book(2L, "2024-002", "Physiology", BookStatus.ISSUED);

        LibraryShelf newShelf = shelf(10L, "Tier 1", rack(5L, "Rack B", library));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(available));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(issued));
        when(shelfRepository.findById(10L)).thenReturn(Optional.of(newShelf));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LibraryBookBulkTransferRequest(List.of(1L, 2L), 10L, "bulk move");
        var result = service.bulkTransfer(request);

        assertThat(result.succeededBookIds()).containsExactly(1L);
        assertThat(result.failed()).hasSize(1);
        assertThat(result.failed().get(0).bookId()).isEqualTo(2L);
        assertThat(result.failed().get(0).reason()).contains("currently issued");
    }

    @Test
    void bulkTransfer_partialSuccess_whenBooksAreLostOrWithdrawn() {
        LibraryBook available = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        available.setLibrary(library);
        LibraryBook lost = book(2L, "2024-002", "Physiology", BookStatus.LOST);
        LibraryBook withdrawn = book(3L, "2024-003", "Biochemistry", BookStatus.WITHDRAWN);

        LibraryShelf newShelf = shelf(10L, "Tier 1", rack(5L, "Rack B", library));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(available));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(lost));
        when(bookRepository.findById(3L)).thenReturn(Optional.of(withdrawn));
        when(shelfRepository.findById(10L)).thenReturn(Optional.of(newShelf));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LibraryBookBulkTransferRequest(List.of(1L, 2L, 3L), 10L, "bulk move");
        var result = service.bulkTransfer(request);

        assertThat(result.succeededBookIds()).containsExactly(1L);
        assertThat(result.failed()).hasSize(2);
        assertThat(result.failed().get(0).bookId()).isEqualTo(2L);
        assertThat(result.failed().get(0).reason()).contains("marked as lost");
        assertThat(result.failed().get(1).bookId()).isEqualTo(3L);
        assertThat(result.failed().get(1).reason()).contains("withdrawn from stock");
    }

    @Test
    void update_whenShelfChangedOnAvailableBook_recordsTransferAudit() {
        LibraryShelf oldShelf = shelf(8L, "Tier 1", rack(4L, "Rack A", library));
        LibraryShelf newShelf = shelf(10L, "Tier 2", rack(5L, "Rack B", library));
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        book.setLibrary(library);
        book.setShelf(oldShelf);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(shelfRepository.findById(10L)).thenReturn(Optional.of(newShelf));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, 10L, null, null, null, null, null, null, BookStatus.AVAILABLE, null
        );

        var response = service.update(1L, request);

        assertThat(response.shelfId()).isEqualTo(10L);
        verify(transferRepository).save(any());
    }

    @Test
    void update_whenBookIsLost_changingShelfThrows() {
        LibraryShelf oldShelf = shelf(8L, "Tier 1", rack(4L, "Rack A", library));
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.LOST);
        book.setLibrary(library);
        book.setShelf(oldShelf);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, 10L, null, null, null, null, null, null, BookStatus.LOST, null
        );

        assertThatThrownBy(() -> service.update(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("status is LOST");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void update_whenBookMarkedLostWithNullShelf_preservesLastKnownShelf() {
        LibraryShelf oldShelf = shelf(8L, "Tier 1", rack(4L, "Rack A", library));
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        book.setLibrary(library);
        book.setShelf(oldShelf);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.cms.dto.LibraryBookRequest(
            "2024-001", null, null, "Anatomy", "Gray", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, BookStatus.LOST, "Reported lost"
        );

        var response = service.update(1L, request);

        assertThat(response.status()).isEqualTo(BookStatus.LOST);
        assertThat(response.shelfId()).isEqualTo(8L);
        verify(transferRepository, never()).save(any());
    }

    // ── helpers ──────────────────────────────────────────────────

    private LibraryBook book(Long id, String accessionNumber, String title, BookStatus status) {
        LibraryBook b = new LibraryBook();
        b.setId(id);
        b.setAccessionNumber(accessionNumber);
        b.setTitle(title);
        b.setAuthors("Author");
        b.setStatus(status);
        return b;
    }

    private LibraryRack rack(Long id, String name, Library library) {
        LibraryRack r = new LibraryRack();
        r.setId(id);
        r.setName(name);
        r.setLibrary(library);
        return r;
    }

    private LibraryShelf shelf(Long id, String name, LibraryRack rack) {
        LibraryShelf s = new LibraryShelf();
        s.setId(id);
        s.setName(name);
        s.setRack(rack);
        return s;
    }
}
