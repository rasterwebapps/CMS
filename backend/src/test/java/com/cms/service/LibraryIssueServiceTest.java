package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LibraryIssueRequest;
import com.cms.dto.LibraryReturnRequest;
import com.cms.dto.LibraryRenewRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.LibraryBook;
import com.cms.model.LibraryFine;
import com.cms.model.LibraryIssue;
import com.cms.model.LibrarySetting;
import com.cms.model.Student;
import com.cms.model.enums.BookStatus;
import com.cms.model.enums.FineStatus;
import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryMemberType;
import com.cms.repository.AppUserRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibraryFineRepository;
import com.cms.repository.LibraryIssueRepository;
import com.cms.repository.LibrarySettingRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LibraryIssueServiceTest {

    @Mock private LibraryIssueRepository  issueRepository;
    @Mock private LibraryBookRepository   bookRepository;
    @Mock private LibraryFineRepository   fineRepository;
    @Mock private LibrarySettingRepository settingRepository;
    @Mock private StudentRepository       studentRepository;
    @Mock private FacultyRepository       facultyRepository;
    @Mock private AppUserRepository       appUserRepository;

    private LibraryIssueService service;

    private LibraryBook availableBook;
    private Student     student;
    private Faculty     faculty;

    @BeforeEach
    void setUp() {
        service = new LibraryIssueService(issueRepository, bookRepository, fineRepository,
            settingRepository, studentRepository, facultyRepository, appUserRepository);

        availableBook = new LibraryBook();
        availableBook.setId(1L);
        availableBook.setAccessionNumber("2024-001");
        availableBook.setTitle("Anatomy Textbook");
        availableBook.setAuthors("Gray");
        availableBook.setStatus(BookStatus.AVAILABLE);

        student = new Student();
        student.setId(10L);
        student.setFirstName("Ravi");
        student.setLastName("Kumar");
        student.setRollNumber("NUR2024001");

        faculty = new Faculty();
        faculty.setId(20L);
        faculty.setFirstName("Dr. Priya");
        faculty.setLastName("Sharma");
        faculty.setEmployeeCode("FAC001");
    }

    // ── issue ─────────────────────────────────────────────────────

    @Test
    void issue_studentHappyPath_setsBookIssuedAndSaves() {
        LibraryIssueRequest request = new LibraryIssueRequest(
            1L, LibraryMemberType.STUDENT, 10L, null, null, null);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(issueRepository.countByStudentIdAndStatusIn(eq(10L), anyList())).thenReturn(0L);
        when(settingRepository.findBySettingKey(any())).thenReturn(Optional.empty());

        LibraryIssue saved = issuedIssue(100L, availableBook, student, null, LocalDate.now().plusDays(14));
        when(issueRepository.save(any())).thenReturn(saved);
        when(fineRepository.findByIssueId(100L)).thenReturn(Optional.empty());

        var response = service.issue(request, "librarian");

        assertThat(response.bookId()).isEqualTo(1L);
        assertThat(response.memberType()).isEqualTo(LibraryMemberType.STUDENT);
        assertThat(response.studentId()).isEqualTo(10L);

        ArgumentCaptor<LibraryBook> bookCaptor = ArgumentCaptor.forClass(LibraryBook.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getStatus()).isEqualTo(BookStatus.ISSUED);
    }

    @Test
    void issue_bookNotFound_throwsResourceNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new LibraryIssueRequest(99L, LibraryMemberType.STUDENT, 10L, null, null, null);

        assertThatThrownBy(() -> service.issue(request, "librarian"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void issue_bookNotAvailable_throws() {
        availableBook.setStatus(BookStatus.ISSUED);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));

        var request = new LibraryIssueRequest(1L, LibraryMemberType.STUDENT, 10L, null, null, null);

        assertThatThrownBy(() -> service.issue(request, "librarian"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not available");
    }

    @Test
    void issue_studentNotFound_throwsResourceNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());
        when(settingRepository.findBySettingKey(any())).thenReturn(Optional.empty());

        var request = new LibraryIssueRequest(1L, LibraryMemberType.STUDENT, 99L, null, null, null);

        assertThatThrownBy(() -> service.issue(request, "librarian"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void issue_studentExceedsMaxBooks_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(settingRepository.findBySettingKey(any())).thenReturn(Optional.empty());
        when(issueRepository.countByStudentIdAndStatusIn(eq(10L), anyList())).thenReturn(2L);

        var request = new LibraryIssueRequest(1L, LibraryMemberType.STUDENT, 10L, null, null, null);

        assertThatThrownBy(() -> service.issue(request, "librarian"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Maximum allowed");
        verify(issueRepository, never()).save(any());
    }

    @Test
    void issue_studentIdMissing_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
        when(settingRepository.findBySettingKey(any())).thenReturn(Optional.empty());

        var request = new LibraryIssueRequest(1L, LibraryMemberType.STUDENT, null, null, null, null);

        assertThatThrownBy(() -> service.issue(request, "librarian"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Student ID is required");
    }

    // ── returnBook ────────────────────────────────────────────────

    @Test
    void returnBook_onTime_noFineCreated() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null,
            LocalDate.now().plusDays(5));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenReturn(issue);
        when(fineRepository.findByIssueId(1L)).thenReturn(Optional.empty());

        service.returnBook(1L, new LibraryReturnRequest(null), "librarian");

        verify(fineRepository, never()).save(any());

        ArgumentCaptor<LibraryBook> bookCaptor = ArgumentCaptor.forClass(LibraryBook.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getStatus()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void returnBook_overdue_createsFineWithCorrectAmount() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null,
            LocalDate.now().minusDays(3));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenReturn(issue);
        when(settingRepository.findBySettingKey("fine_per_day")).thenReturn(
            Optional.of(setting("fine_per_day", "2.00")));
        when(fineRepository.findByIssueId(1L)).thenReturn(Optional.empty());

        service.returnBook(1L, new LibraryReturnRequest(null), "librarian");

        ArgumentCaptor<LibraryFine> fineCaptor = ArgumentCaptor.forClass(LibraryFine.class);
        verify(fineRepository).save(fineCaptor.capture());

        LibraryFine fine = fineCaptor.getValue();
        assertThat(fine.getOverdueDays()).isEqualTo(3);
        assertThat(fine.getTotalFine()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(fine.getStatus()).isEqualTo(FineStatus.PENDING);
    }

    @Test
    void returnBook_alreadyReturned_throws() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null, LocalDate.now());
        issue.setStatus(IssueStatus.RETURNED);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.returnBook(1L, null, "librarian"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been returned");
    }

    @Test
    void returnBook_lostBook_throws() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null, LocalDate.now());
        issue.setStatus(IssueStatus.LOST);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.returnBook(1L, null, "librarian"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("lost");
    }

    // ── renew ─────────────────────────────────────────────────────

    @Test
    void renew_happyPath_incrementsCountAndExtendsDueDate() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null,
            LocalDate.now().plusDays(3));
        issue.setRenewalCount(0);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(settingRepository.findBySettingKey(any())).thenReturn(Optional.empty());
        when(issueRepository.save(any())).thenReturn(issue);
        when(fineRepository.findByIssueId(1L)).thenReturn(Optional.empty());

        var response = service.renew(1L, new LibraryRenewRequest(null));

        ArgumentCaptor<LibraryIssue> issueCaptor = ArgumentCaptor.forClass(LibraryIssue.class);
        verify(issueRepository).save(issueCaptor.capture());

        assertThat(issueCaptor.getValue().getRenewalCount()).isEqualTo(1);
        assertThat(issueCaptor.getValue().getDueDate()).isAfter(LocalDate.now());
        assertThat(issueCaptor.getValue().getStatus()).isEqualTo(IssueStatus.ISSUED);
    }

    @Test
    void renew_maxRenewalsReached_throws() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null, LocalDate.now());
        issue.setRenewalCount(2);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(settingRepository.findBySettingKey("max_renewals")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renew(1L, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Renewal limit");
        verify(issueRepository, never()).save(any());
    }

    @Test
    void renew_alreadyReturned_throws() {
        LibraryIssue issue = issuedIssue(1L, availableBook, student, null, LocalDate.now());
        issue.setStatus(IssueStatus.RETURNED);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> service.renew(1L, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been returned");
    }

    // ── markOverdueIssues ─────────────────────────────────────────

    @Test
    void markOverdueIssues_pastDueIssued_flipsToOverdue() {
        LibraryIssue overdue = issuedIssue(1L, availableBook, student, null,
            LocalDate.now().minusDays(1));
        LibraryIssue current = issuedIssue(2L, availableBook, student, null,
            LocalDate.now().plusDays(5));

        when(issueRepository.findByStatusAndDueDateBefore(eq(IssueStatus.ISSUED), any()))
            .thenReturn(List.of(overdue));

        service.markOverdueIssues();

        assertThat(overdue.getStatus()).isEqualTo(IssueStatus.OVERDUE);
        verify(issueRepository).saveAll(List.of(overdue));
    }

    @Test
    void markOverdueIssues_noOverdueIssues_doesNotSave() {
        when(issueRepository.findByStatusAndDueDateBefore(any(), any())).thenReturn(List.of());

        service.markOverdueIssues();

        verify(issueRepository, never()).saveAll(any());
    }

    // ── helpers ───────────────────────────────────────────────────

    private LibraryIssue issuedIssue(Long id, LibraryBook book, Student student,
                                      Faculty faculty, LocalDate dueDate) {
        LibraryIssue issue = new LibraryIssue();
        issue.setId(id);
        issue.setBook(book);
        issue.setStudent(student);
        issue.setFaculty(faculty);
        issue.setMemberType(student != null ? LibraryMemberType.STUDENT : LibraryMemberType.FACULTY);
        issue.setIssuedDate(LocalDate.now().minusDays(7));
        issue.setDueDate(dueDate);
        issue.setStatus(IssueStatus.ISSUED);
        issue.setIssuedBy("librarian");
        issue.setRenewalCount(0);
        return issue;
    }

    private LibrarySetting setting(String key, String value) {
        LibrarySetting s = new LibrarySetting();
        s.setSettingKey(key);
        s.setSettingValue(value);
        return s;
    }
}
