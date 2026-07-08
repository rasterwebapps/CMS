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
    @Mock private CurrentUserResolver currentUserResolver;

    private LibraryBookService service;
    private Library library;

    @BeforeEach
    void setUp() {
        service = new LibraryBookService(bookRepository, accessionRegistry, libraryRepository,
            shelfRepository, transferRepository, currentUserResolver);

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
            "2024-001", null, "Anatomy", "Gray", null, null, null, null,
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
            "2024-001", null, "Anatomy", "Gray", null, null, null, null,
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
            null, null, "New Book", "Author", null, null, null, null,
            null, null, null, 1L, null, null, null, null, null, null, null, null, null
        );

        var response = service.create(request);
        assertThat(response).isNotNull();
        verify(bookRepository).save(any());
    }

    // ── delete ───────────────────────────────────────────────────

    @Test
    void delete_availableBook_succeeds() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.AVAILABLE);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        service.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void delete_issuedBook_throws() {
        LibraryBook book = book(1L, "2024-001", "Anatomy", BookStatus.ISSUED);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("currently issued");
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void delete_notFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
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
