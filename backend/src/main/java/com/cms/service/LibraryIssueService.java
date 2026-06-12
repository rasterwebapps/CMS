package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryFineResponse;
import com.cms.dto.LibraryIssueRequest;
import com.cms.dto.LibraryIssueResponse;
import com.cms.dto.LibraryReturnRequest;
import com.cms.dto.LibraryRenewRequest;
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
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AppUserRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibraryFineRepository;
import com.cms.repository.LibraryIssueRepository;
import com.cms.repository.LibrarySettingRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class LibraryIssueService {

    private static final List<IssueStatus> ACTIVE_STATUSES = List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE);

    private final LibraryIssueRepository   issueRepository;
    private final LibraryBookRepository    bookRepository;
    private final LibraryFineRepository    fineRepository;
    private final LibrarySettingRepository settingRepository;
    private final StudentRepository        studentRepository;
    private final FacultyRepository        facultyRepository;
    private final AppUserRepository        appUserRepository;

    public LibraryIssueService(LibraryIssueRepository issueRepository,
                                LibraryBookRepository bookRepository,
                                LibraryFineRepository fineRepository,
                                LibrarySettingRepository settingRepository,
                                StudentRepository studentRepository,
                                FacultyRepository facultyRepository,
                                AppUserRepository appUserRepository) {
        this.issueRepository    = issueRepository;
        this.bookRepository     = bookRepository;
        this.fineRepository     = fineRepository;
        this.settingRepository  = settingRepository;
        this.studentRepository  = studentRepository;
        this.facultyRepository  = facultyRepository;
        this.appUserRepository  = appUserRepository;
    }

    // ── Issue a book ──────────────────────────────────────────────

    @Transactional
    public LibraryIssueResponse issue(LibraryIssueRequest request, String issuedBy) {
        LibraryBook book = bookRepository.findById(request.bookId())
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.bookId()));

        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new IllegalStateException(
                "Book '" + book.getTitle() + "' is not available for issue (current status: " + book.getStatus() + ")");
        }

        LibraryMemberType memberType = request.memberType();
        int loanDays = memberType == LibraryMemberType.STUDENT
            ? getSettingInt("student_loan_days", 14)
            : getSettingInt("faculty_loan_days", 30);
        int maxBooks = memberType == LibraryMemberType.STUDENT
            ? getSettingInt("student_max_books", 2)
            : getSettingInt("faculty_max_books", 3);

        LocalDate issuedDate = request.issuedDate() != null ? request.issuedDate() : LocalDate.now();
        LocalDate dueDate    = issuedDate.plusDays(loanDays);

        LibraryIssue issue = new LibraryIssue();
        issue.setBook(book);
        issue.setMemberType(memberType);
        issue.setIssuedDate(issuedDate);
        issue.setDueDate(dueDate);
        issue.setStatus(IssueStatus.ISSUED);
        issue.setIssuedBy(issuedBy);
        issue.setRemarks(request.remarks());

        if (memberType == LibraryMemberType.STUDENT) {
            if (request.studentId() == null) {
                throw new IllegalArgumentException("Student ID is required for student issues");
            }
            Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

            long activeCount = issueRepository.countByStudentIdAndStatusIn(student.getId(), ACTIVE_STATUSES);
            if (activeCount >= maxBooks) {
                throw new IllegalStateException(
                    student.getFirstName() + " already has " + activeCount + " book(s) issued. Maximum allowed is " + maxBooks);
            }
            issue.setStudent(student);

        } else {
            if (request.facultyId() == null) {
                throw new IllegalArgumentException("Faculty ID is required for faculty issues");
            }
            Faculty faculty = facultyRepository.findById(request.facultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

            long activeCount = issueRepository.countByFacultyIdAndStatusIn(faculty.getId(), ACTIVE_STATUSES);
            if (activeCount >= maxBooks) {
                throw new IllegalStateException(
                    faculty.getFirstName() + " already has " + activeCount + " book(s) issued. Maximum allowed is " + maxBooks);
            }
            issue.setFaculty(faculty);
        }

        book.setStatus(BookStatus.ISSUED);
        bookRepository.save(book);

        LibraryIssue saved = issueRepository.save(issue);
        return toResponse(saved);
    }

    // ── Return a book ─────────────────────────────────────────────

    @Transactional
    public LibraryIssueResponse returnBook(Long issueId, LibraryReturnRequest request, String returnedTo) {
        LibraryIssue issue = requireIssue(issueId);

        if (issue.getStatus() == IssueStatus.RETURNED) {
            throw new IllegalStateException("This book has already been returned");
        }
        if (issue.getStatus() == IssueStatus.LOST) {
            throw new IllegalStateException("This book is marked as lost and cannot be returned normally");
        }

        LocalDate today = LocalDate.now();
        issue.setReturnedDate(today);
        issue.setReturnedTo(returnedTo);
        issue.setStatus(IssueStatus.RETURNED);
        if (request != null && request.remarks() != null) {
            issue.setRemarks(request.remarks());
        }

        issue.getBook().setStatus(BookStatus.AVAILABLE);
        bookRepository.save(issue.getBook());

        // Calculate and record fine if overdue
        if (today.isAfter(issue.getDueDate())) {
            int overdueDays = (int) ChronoUnit.DAYS.between(issue.getDueDate(), today);
            BigDecimal finePerDay = getSettingDecimal("fine_per_day", BigDecimal.ONE);
            BigDecimal totalFine  = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

            LibraryFine fine = new LibraryFine();
            fine.setIssue(issue);
            fine.setOverdueDays(overdueDays);
            fine.setFinePerDay(finePerDay);
            fine.setTotalFine(totalFine);
            fine.setStatus(FineStatus.PENDING);
            fineRepository.save(fine);
        }

        return toResponse(issueRepository.save(issue));
    }

    // ── Renew a book ──────────────────────────────────────────────

    @Transactional
    public LibraryIssueResponse renew(Long issueId, LibraryRenewRequest request) {
        LibraryIssue issue = requireIssue(issueId);

        if (issue.getStatus() == IssueStatus.RETURNED) {
            throw new IllegalStateException("Cannot renew a book that has already been returned");
        }

        int maxRenewals = getSettingInt("max_renewals", 2);
        if (issue.getRenewalCount() >= maxRenewals) {
            throw new IllegalStateException(
                "Renewal limit reached. This book can be renewed at most " + maxRenewals + " time(s)");
        }

        LibraryMemberType memberType = issue.getMemberType();
        int loanDays = memberType == LibraryMemberType.STUDENT
            ? getSettingInt("student_loan_days", 14)
            : getSettingInt("faculty_loan_days", 30);

        issue.setDueDate(LocalDate.now().plusDays(loanDays));
        issue.setRenewalCount(issue.getRenewalCount() + 1);
        issue.setLastRenewedDate(LocalDate.now());
        issue.setStatus(IssueStatus.ISSUED);  // Reset OVERDUE → ISSUED after renewal
        if (request != null && request.remarks() != null) {
            issue.setRemarks(request.remarks());
        }

        return toResponse(issueRepository.save(issue));
    }

    // ── Queries ───────────────────────────────────────────────────

    @Transactional
    public List<LibraryIssueResponse> findAll(LibraryMemberType memberType, IssueStatus status) {
        markOverdueIssues();
        List<LibraryIssue> issues;
        if (memberType != null) {
            issues = issueRepository.findByMemberType(memberType);
        } else if (status != null) {
            issues = issueRepository.findByStatus(status);
        } else {
            issues = issueRepository.findAll();
        }
        return issues.stream().map(this::toResponse).toList();
    }

    public List<LibraryIssueResponse> findByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return issueRepository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    public List<LibraryIssueResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return issueRepository.findByFacultyId(facultyId).stream().map(this::toResponse).toList();
    }

    /** Returns the active issues for the currently authenticated user (student or faculty). */
    public List<LibraryIssueResponse> findMyIssues(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(user -> {
                if (user.getLinkedStudent() != null) {
                    return issueRepository.findByStudentId(user.getLinkedStudent().getId())
                        .stream().map(this::toResponse).toList();
                } else if (user.getLinkedFaculty() != null) {
                    return issueRepository.findByFacultyId(user.getLinkedFaculty().getId())
                        .stream().map(this::toResponse).toList();
                }
                return List.<LibraryIssueResponse>of();
            })
            .orElse(List.of());
    }

    public LibraryIssueResponse findById(Long id) {
        return toResponse(requireIssue(id));
    }

    /**
     * Check if a student has active (ISSUED/OVERDUE) library issues.
     * Used by StudentService to block deactivation.
     */
    public boolean hasActiveIssues(Long studentId) {
        return issueRepository.countByStudentIdAndStatusIn(studentId, ACTIVE_STATUSES) > 0;
    }

    // ── Overdue auto-marking ──────────────────────────────────────

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueIssues() {
        List<LibraryIssue> overdue = issueRepository
            .findByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now());
        for (LibraryIssue issue : overdue) {
            issue.setStatus(IssueStatus.OVERDUE);
        }
        if (!overdue.isEmpty()) {
            issueRepository.saveAll(overdue);
        }
    }

    // ── Settings helpers ──────────────────────────────────────────

    private int getSettingInt(String key, int defaultValue) {
        return settingRepository.findBySettingKey(key)
            .map(s -> { try { return Integer.parseInt(s.getSettingValue()); } catch (NumberFormatException e) { return defaultValue; } })
            .orElse(defaultValue);
    }

    private BigDecimal getSettingDecimal(String key, BigDecimal defaultValue) {
        return settingRepository.findBySettingKey(key)
            .map(s -> { try { return new BigDecimal(s.getSettingValue()); } catch (NumberFormatException e) { return defaultValue; } })
            .orElse(defaultValue);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private LibraryIssue requireIssue(Long id) {
        return issueRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Library issue not found with id: " + id));
    }

    LibraryIssueResponse toResponse(LibraryIssue issue) {
        LibraryBook book = issue.getBook();
        Student student  = issue.getStudent();
        Faculty faculty  = issue.getFaculty();

        LibraryFineResponse fineResponse = fineRepository.findByIssueId(issue.getId())
            .map(f -> new LibraryFineResponse(
                f.getId(), f.getOverdueDays(), f.getFinePerDay(), f.getTotalFine(),
                f.getStatus(), f.getWaivedBy(), f.getCollectedAt(), f.getRemarks()))
            .orElse(null);

        return new LibraryIssueResponse(
            issue.getId(),
            book.getId(),
            book.getAccessionNumber(),
            book.getTitle(),
            book.getAuthors(),
            book.getCallNumber(),
            book.getShelfLocation(),
            issue.getMemberType(),
            student != null ? student.getId() : null,
            student != null ? student.getFirstName() + " " + student.getLastName() : null,
            student != null ? student.getRollNumber() : null,
            faculty != null ? faculty.getId() : null,
            faculty != null ? faculty.getFirstName() + " " + faculty.getLastName() : null,
            faculty != null ? faculty.getEmployeeCode() : null,
            issue.getIssuedDate(),
            issue.getDueDate(),
            issue.getReturnedDate(),
            issue.getRenewalCount(),
            issue.getLastRenewedDate(),
            issue.getStatus(),
            issue.getIssuedBy(),
            issue.getReturnedTo(),
            issue.getRemarks(),
            fineResponse,
            issue.getCreatedAt(),
            issue.getUpdatedAt()
        );
    }
}
