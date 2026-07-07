package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibraryBook;
import com.cms.model.enums.BookStatus;
import com.cms.repository.LibraryBookRepository;

@ExtendWith(MockitoExtension.class)
class LibraryBookServiceTest {

    @Mock private LibraryBookRepository bookRepository;
    @Mock private LibraryAccessionRegistryService accessionRegistry;

    private LibraryBookService service;

    @BeforeEach
    void setUp() {
        service = new LibraryBookService(bookRepository, accessionRegistry);
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
            null, null, null, null, null, null, null, null, null, null, null, null
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
            null, null, null, null, null, null, null, null, null, null, null, null
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
            null, null, null, null, null, null, null, null, null, null, null, null
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
}
