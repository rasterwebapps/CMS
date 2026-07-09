package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import com.cms.dto.LibraryCirculationLookupResponse;
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
import com.cms.model.LibraryPeriodical;
import com.cms.model.Student;
import com.cms.model.enums.BookStatus;
import com.cms.model.enums.FineStatus;
import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryItemType;
import com.cms.model.enums.LibraryMemberType;
import com.cms.repository.AppUserRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibraryFineRepository;
import com.cms.repository.LibraryIssueRepository;
import com.cms.repository.LibraryPeriodicalRepository;
import com.cms.repository.LibrarySettingRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class LibraryIssueService {

    private static final List<IssueStatus> ACTIVE_STATUSES = List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE);

    private final LibraryIssueRepository      issueRepository;
    private final LibraryBookRepository       bookRepository;
    private final LibraryPeriodicalRepository periodicalRepository;
    private final LibraryFineRepository       fineRepository;
    private final LibrarySettingRepository    settingRepository;
    private final StudentRepository           studentRepository;
    private final FacultyRepository           facultyRepository;
    private final AppUserRepository           appUserRepository;

    public LibraryIssueService(LibraryIssueRepository issueRepository,
                                LibraryBookRepository bookRepository,
                                LibraryPeriodicalRepository periodicalRepository,
                                LibraryFineRepository fineRepository,
                                LibrarySettingRepository settingRepository,
                                StudentRepository studentRepository,
                                FacultyRepository facultyRepository,
                                AppUserRepository appUserRepository) {
        this.issueRepository      = issueRepository;
        this.bookRepository       = bookRepository;
        this.periodicalRepository = periodicalRepository;
        this.fineRepository       = fineRepository;
        this.settingRepository    = settingRepository;
        this.studentRepository    = studentRepository;
        this.facultyRepository    = facultyRepository;
        this.appUserRepository    = appUserRepository;
    }

    // ── Accession lookup (Issue Book screen) ───────────────────────

    public LibraryCirculationLookupResponse lookupByAccessionNumber(String accessionNumber) {
        String trimmed = accessionNumber.trim();

        return bookRepository.findByAccessionNumber(trimmed)
            .<LibraryCirculationLookupResponse>map(b -> new LibraryCirculationLookupResponse(
                LibraryItemType.BOOK, b.getId(), b.getAccessionNumber(), b.getTitle(), b.getAuthors(),
                b.getCallNumber(), b.getShelf() != null ? b.getShelf().getName() : null, b.getStatus()))
            .or(() -> periodicalRepository.findByAccessionNumber(trimmed)
                .map(p -> new LibraryCirculationLookupResponse(
                    LibraryItemType.JOURNAL, p.getId(), p.getAccessionNumber(), p.getJournalName(),
                    formatPeriodicalDetail(p), null, null, p.getStatus())))
            .orElseThrow(() -> new ResourceNotFoundException(
                "No book or journal found with accession number '" + trimmed + "'"));
    }

    private static String formatPeriodicalDetail(LibraryPeriodical p) {
        List<String> parts = new ArrayList<>();
        if (p.getVolumeNumber() != null) parts.add("Vol " + p.getVolumeNumber());
        if (p.getIssueNumber() != null) parts.add("Issue " + p.getIssueNumber());
        String monthYear = ((p.getMonthRange() != null ? p.getMonthRange() : "")
            + (p.getYear() != null ? " " + p.getYear() : "")).trim();
        if (!monthYear.isEmpty()) parts.add(monthYear);
        return String.join(" • ", parts);
    }

    // ── Issue a book or journal ────────────────────────────────────

    @Transactional
    public LibraryIssueResponse issue(LibraryIssueRequest request, String issuedBy) {
        LibraryBook book = null;
        LibraryPeriodical periodical = null;

        if (request.itemType() == LibraryItemType.BOOK) {
            if (request.bookId() == null) {
                throw new IllegalArgumentException("Book ID is required");
            }
            book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.bookId()));
            if (book.getStatus() != BookStatus.AVAILABLE) {
                throw new IllegalStateException(
                    "Book '" + book.getTitle() + "' is not available for issue (current status: " + book.getStatus() + ")");
            }
        } else {
            if (request.periodicalId() == null) {
                throw new IllegalArgumentException("Journal ID is required");
            }
            periodical = periodicalRepository.findById(request.periodicalId())
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + request.periodicalId()));
            if (periodical.getStatus() != BookStatus.AVAILABLE) {
                throw new IllegalStateException(
                    "Journal '" + periodical.getJournalName() + "' is not available for issue (current status: " + periodical.getStatus() + ")");
            }
        }

        LibraryMemberType memberType = request.memberType();
        int loanDays = memberType == LibraryMemberType.STUDENT
            ? getSettingInt("student_loan_days", 14)
            : getSettingInt("faculty_loan_days", 30);
        int maxItems = memberType == LibraryMemberType.STUDENT
            ? getSettingInt("student_max_books", 2)
            : getSettingInt("faculty_max_books", 3);

        LocalDate issuedDate = request.issuedDate() != null ? request.issuedDate() : LocalDate.now();
        LocalDate dueDate    = issuedDate.plusDays(loanDays);

        LibraryIssue issue = new LibraryIssue();
        issue.setBook(book);
        issue.setPeriodical(periodical);
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
            if (activeCount >= maxItems) {
                throw new IllegalStateException(
                    student.getFirstName() + " already has " + activeCount + " item(s) issued. Maximum allowed is " + maxItems);
            }
            issue.setStudent(student);

        } else {
            if (request.facultyId() == null) {
                throw new IllegalArgumentException("Faculty ID is required for faculty issues");
            }
            Faculty faculty = facultyRepository.findById(request.facultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

            long activeCount = issueRepository.countByFacultyIdAndStatusIn(faculty.getId(), ACTIVE_STATUSES);
            if (activeCount >= maxItems) {
                throw new IllegalStateException(
                    faculty.getFirstName() + " already has " + activeCount + " item(s) issued. Maximum allowed is " + maxItems);
            }
            issue.setFaculty(faculty);
        }

        if (book != null) {
            book.setStatus(BookStatus.ISSUED);
            bookRepository.save(book);
        } else {
            periodical.setStatus(BookStatus.ISSUED);
            periodicalRepository.save(periodical);
        }

        LibraryIssue saved = issueRepository.save(issue);
        return toResponse(saved);
    }

    // ── Return a book or journal ───────────────────────────────────

    @Transactional
    public LibraryIssueResponse returnBook(Long issueId, LibraryReturnRequest request, String returnedTo) {
        LibraryIssue issue = requireIssue(issueId);

        if (issue.getStatus() == IssueStatus.RETURNED) {
            throw new IllegalStateException("This item has already been returned");
        }
        if (issue.getStatus() == IssueStatus.LOST) {
            throw new IllegalStateException("This item is marked as lost and cannot be returned normally");
        }

        LocalDate today = LocalDate.now();
        issue.setReturnedDate(today);
        issue.setReturnedTo(returnedTo);
        issue.setStatus(IssueStatus.RETURNED);
        if (request != null && request.remarks() != null) {
            issue.setRemarks(request.remarks());
        }

        if (issue.getBook() != null) {
            issue.getBook().setStatus(BookStatus.AVAILABLE);
            bookRepository.save(issue.getBook());
        } else {
            issue.getPeriodical().setStatus(BookStatus.AVAILABLE);
            periodicalRepository.save(issue.getPeriodical());
        }

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

    // ── Renew a book or journal ─────────────────────────────────────

    @Transactional
    public LibraryIssueResponse renew(Long issueId, LibraryRenewRequest request) {
        LibraryIssue issue = requireIssue(issueId);

        if (issue.getStatus() == IssueStatus.RETURNED) {
            throw new IllegalStateException("Cannot renew an item that has already been returned");
        }

        int maxRenewals = getSettingInt("max_renewals", 2);
        if (issue.getRenewalCount() >= maxRenewals) {
            throw new IllegalStateException(
                "Renewal limit reached. This item can be renewed at most " + maxRenewals + " time(s)");
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

    public List<LibraryIssueResponse> findAll(LibraryMemberType memberType, IssueStatus status) {
        List<LibraryIssue> issues;
        if (memberType != null) {
            issues = issueRepository.findByMemberType(memberType);
        } else if (status != null) {
            issues = issueRepository.findByStatus(status);
        } else {
            issues = issueRepository.findAll();
        }
        return toResponses(issues);
    }

    public Page<LibraryIssueResponse> findPage(String search, IssueStatus status, LibraryMemberType memberType,
                                                LibraryItemType itemType, Pageable pageable) {
        Specification<LibraryIssue> spec = buildSpec(search, status, memberType, itemType);
        Page<LibraryIssue> page = issueRepository.findAll(spec, pageable);
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    /** Unpaged, filtered, sorted — used by the Export endpoint (respects the same filters as findPage). */
    public List<LibraryIssueResponse> findAllMatching(String search, IssueStatus status, LibraryMemberType memberType,
                                                       LibraryItemType itemType, Sort sort) {
        List<LibraryIssue> issues = issueRepository.findAll(buildSpec(search, status, memberType, itemType), sort);
        return toResponses(issues);
    }

    private Specification<LibraryIssue> buildSpec(String search, IssueStatus status, LibraryMemberType memberType,
                                                   LibraryItemType itemType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String p = "%" + search.trim().toLowerCase() + "%";
                Join<Object, Object> book = root.join("book", JoinType.LEFT);
                Join<Object, Object> periodical = root.join("periodical", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(book.get("title")), p),
                    cb.like(cb.lower(book.get("accessionNumber")), p),
                    cb.like(cb.lower(periodical.get("journalName")), p),
                    cb.like(cb.lower(periodical.get("accessionNumber")), p)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (memberType != null) predicates.add(cb.equal(root.get("memberType"), memberType));
            if (itemType == LibraryItemType.BOOK) predicates.add(cb.isNotNull(root.get("book")));
            if (itemType == LibraryItemType.JOURNAL) predicates.add(cb.isNotNull(root.get("periodical")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<LibraryIssueResponse> findByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return toResponses(issueRepository.findByStudentId(studentId));
    }

    public List<LibraryIssueResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return toResponses(issueRepository.findByFacultyId(facultyId));
    }

    /** Returns the active issues for the currently authenticated user (student or faculty). */
    public List<LibraryIssueResponse> findMyIssues(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(user -> {
                if (user.getLinkedStudent() != null) {
                    return toResponses(issueRepository.findByStudentId(user.getLinkedStudent().getId()));
                } else if (user.getLinkedFaculty() != null) {
                    return toResponses(issueRepository.findByFacultyId(user.getLinkedFaculty().getId()));
                }
                return List.<LibraryIssueResponse>of();
            })
            .orElse(List.of());
    }

    public LibraryIssueResponse findById(Long id) {
        return toResponse(requireIssue(id));
    }

    /**
     * Effectively-overdue issues as of right now — a read-only check for the Overdue
     * Report so it stays accurate between runs of the nightly {@link #markOverdueIssues()}
     * job, without that job's write cost on every report load.
     */
    public List<LibraryIssueResponse> findEffectivelyOverdue() {
        return toResponses(issueRepository.findByStatusInAndDueDateBefore(ACTIVE_STATUSES, LocalDate.now()));
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

    /** Batch-maps issues to responses, fetching all fines in a single query instead of one-per-issue. */
    private List<LibraryIssueResponse> toResponses(List<LibraryIssue> issues) {
        if (issues.isEmpty()) return List.of();
        List<Long> issueIds = issues.stream().map(LibraryIssue::getId).toList();
        Map<Long, LibraryFine> finesByIssueId = fineRepository.findByIssueIdIn(issueIds).stream()
            .collect(Collectors.toMap(f -> f.getIssue().getId(), f -> f));
        return issues.stream().map(issue -> toResponse(issue, finesByIssueId.get(issue.getId()))).toList();
    }

    LibraryIssueResponse toResponse(LibraryIssue issue) {
        LibraryFine fine = fineRepository.findByIssueId(issue.getId()).orElse(null);
        return toResponse(issue, fine);
    }

    private LibraryIssueResponse toResponse(LibraryIssue issue, LibraryFine fine) {
        Student student  = issue.getStudent();
        Faculty faculty  = issue.getFaculty();

        LibraryFineResponse fineResponse = fine == null ? null : new LibraryFineResponse(
            fine.getId(), fine.getOverdueDays(), fine.getFinePerDay(), fine.getTotalFine(),
            fine.getStatus(), fine.getWaivedBy(), fine.getCollectedAt(), fine.getRemarks());

        return new LibraryIssueResponse(
            issue.getId(),
            issue.getItemType(),
            issue.getBook() != null ? issue.getBook().getId() : null,
            issue.getPeriodical() != null ? issue.getPeriodical().getId() : null,
            issue.getItemAccessionNumber(),
            issue.getItemTitle(),
            issue.getItemDetail(),
            issue.getCallNumber(),
            issue.getShelfLocation(),
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
