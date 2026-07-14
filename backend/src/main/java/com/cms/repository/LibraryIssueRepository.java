package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.LibraryIssue;
import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryMemberType;

public interface LibraryIssueRepository extends JpaRepository<LibraryIssue, Long>, JpaSpecificationExecutor<LibraryIssue> {

    List<LibraryIssue> findByStudentId(Long studentId);

    List<LibraryIssue> findByFacultyId(Long facultyId);

    List<LibraryIssue> findByStudentIdAndStatus(Long studentId, IssueStatus status);

    List<LibraryIssue> findByFacultyIdAndStatus(Long facultyId, IssueStatus status);

    List<LibraryIssue> findByStatus(IssueStatus status);

    List<LibraryIssue> findByMemberType(LibraryMemberType memberType);

    Optional<LibraryIssue> findByBookIdAndStatusIn(Long bookId, List<IssueStatus> statuses);

    Optional<LibraryIssue> findByPeriodicalIdAndStatusIn(Long periodicalId, List<IssueStatus> statuses);

    List<LibraryIssue> findByBookIdOrderByIssuedDateDesc(Long bookId);

    List<LibraryIssue> findByPeriodicalIdOrderByIssuedDateDesc(Long periodicalId);

    long countByStudentIdAndStatusIn(Long studentId, List<IssueStatus> statuses);

    long countByFacultyIdAndStatusIn(Long facultyId, List<IssueStatus> statuses);

    // For overdue detection: issued books whose due date has passed and are not returned
    List<LibraryIssue> findByStatusAndDueDateBefore(IssueStatus status, LocalDate date);

    boolean existsByBookId(Long bookId);
}
