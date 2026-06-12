package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.LibraryBook;
import com.cms.model.LibraryFine;
import com.cms.model.LibraryIssue;
import com.cms.model.Student;
import com.cms.model.enums.BookStatus;
import com.cms.model.enums.FineStatus;
import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryMemberType;
import com.cms.repository.LibraryFineRepository;

@ExtendWith(MockitoExtension.class)
class LibraryFineServiceTest {

    @Mock private LibraryFineRepository fineRepository;

    private LibraryFineService service;

    @BeforeEach
    void setUp() {
        service = new LibraryFineService(fineRepository);
    }

    // ── findAll ───────────────────────────────────────────────────

    @Test
    void findAll_noFilter_returnsAll() {
        LibraryFine f1 = fine(1L, FineStatus.PENDING,  Instant.now().minusSeconds(200));
        LibraryFine f2 = fine(2L, FineStatus.COLLECTED, Instant.now().minusSeconds(100));
        when(fineRepository.findAll()).thenReturn(List.of(f1, f2));

        var results = service.findAll(null);

        assertThat(results).hasSize(2);
        // sorted descending by createdAt — f2 is more recent
        assertThat(results.get(0).id()).isEqualTo(2L);
        assertThat(results.get(1).id()).isEqualTo(1L);
    }

    @Test
    void findAll_withStatusFilter_callsFindByStatus() {
        LibraryFine pending = fine(1L, FineStatus.PENDING, Instant.now());
        when(fineRepository.findByStatus(FineStatus.PENDING)).thenReturn(List.of(pending));

        var results = service.findAll(FineStatus.PENDING);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(FineStatus.PENDING);
        verify(fineRepository).findByStatus(FineStatus.PENDING);
    }

    // ── waive ─────────────────────────────────────────────────────

    @Test
    void waive_pendingFine_setsWaivedStatusAndActor() {
        LibraryFine fine = fine(1L, FineStatus.PENDING, Instant.now());
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(fineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.waive(1L, "Student appealed", "librarian01");

        assertThat(result.status()).isEqualTo(FineStatus.WAIVED);
        assertThat(result.waivedBy()).isEqualTo("librarian01");

        ArgumentCaptor<LibraryFine> captor = ArgumentCaptor.forClass(LibraryFine.class);
        verify(fineRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FineStatus.WAIVED);
        assertThat(captor.getValue().getWaivedBy()).isEqualTo("librarian01");
        assertThat(captor.getValue().getRemarks()).isEqualTo("Student appealed");
    }

    @Test
    void waive_alreadyWaived_throws() {
        LibraryFine fine = fine(1L, FineStatus.WAIVED, Instant.now());
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.waive(1L, null, "librarian01"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    void waive_alreadyCollected_throws() {
        LibraryFine fine = fine(1L, FineStatus.COLLECTED, Instant.now());
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.waive(1L, null, "librarian01"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void waive_notFound_throws() {
        when(fineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.waive(99L, null, "librarian01"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── collect ───────────────────────────────────────────────────

    @Test
    void collect_pendingFine_setsCollectedStatusAndTimestamp() {
        LibraryFine fine = fine(1L, FineStatus.PENDING, Instant.now());
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));
        when(fineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.collect(1L, null, "cashier01");

        assertThat(result.status()).isEqualTo(FineStatus.COLLECTED);
        assertThat(result.collectedAt()).isNotNull();
    }

    @Test
    void collect_alreadyCollected_throws() {
        LibraryFine fine = fine(1L, FineStatus.COLLECTED, Instant.now());
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.collect(1L, null, "cashier01"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    void collect_waived_throws() {
        LibraryFine fine = fine(1L, FineStatus.WAIVED, Instant.now());
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.collect(1L, null, "cashier01"))
            .isInstanceOf(IllegalStateException.class);
    }

    // ── helpers ───────────────────────────────────────────────────

    private LibraryFine fine(Long id, FineStatus status, Instant createdAt) {
        LibraryBook book = new LibraryBook();
        book.setId(10L);
        book.setAccessionNumber("2024-001");
        book.setTitle("Anatomy");
        book.setAuthors("Gray");
        book.setStatus(BookStatus.AVAILABLE);

        Student student = new Student();
        student.setId(20L);
        student.setFirstName("Ravi");
        student.setLastName("Kumar");
        student.setRollNumber("NUR2024001");

        LibraryIssue issue = new LibraryIssue();
        issue.setId(30L);
        issue.setBook(book);
        issue.setStudent(student);
        issue.setMemberType(LibraryMemberType.STUDENT);
        issue.setIssuedDate(LocalDate.of(2026, 1, 1));
        issue.setDueDate(LocalDate.of(2026, 1, 15));
        issue.setStatus(IssueStatus.RETURNED);

        LibraryFine fine = new LibraryFine();
        fine.setId(id);
        fine.setIssue(issue);
        fine.setOverdueDays(5);
        fine.setFinePerDay(new BigDecimal("1.00"));
        fine.setTotalFine(new BigDecimal("5.00"));
        fine.setStatus(status);
        // Simulate the @CreatedDate field via reflection-friendly setter alternative
        setCreatedAt(fine, createdAt);
        return fine;
    }

    private void setCreatedAt(LibraryFine fine, Instant instant) {
        try {
            var field = LibraryFine.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(fine, instant);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
