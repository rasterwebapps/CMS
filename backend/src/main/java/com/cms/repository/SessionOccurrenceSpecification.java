package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.cms.model.SessionOccurrence;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.SpecialClassApprovalStatus;

import jakarta.persistence.criteria.JoinType;

/** BR-55 — filters for the Special Class Approvals screen's admin-facing request history
 *  (pending/approved/rejected/cancelled, not just the pending queue). Mirrors {@link
 *  ClassScheduleSpecification}'s shape. */
public final class SessionOccurrenceSpecification {

    private SessionOccurrenceSpecification() {}

    public static Specification<SessionOccurrence> byOccurrenceSourceIn(List<OccurrenceSource> sources) {
        return (root, query, cb) -> root.get("occurrenceSource").in(sources);
    }

    public static Specification<SessionOccurrence> byApprovalStatus(SpecialClassApprovalStatus status) {
        return (root, query, cb) -> cb.equal(root.get("approvalStatus"), status);
    }

    public static Specification<SessionOccurrence> byRequestedFacultyId(Long facultyId) {
        return (root, query, cb) -> cb.equal(root.get("requestedFaculty").get("id"), facultyId);
    }

    public static Specification<SessionOccurrence> bySubjectId(Long subjectId) {
        return (root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId);
    }

    /** {@code SessionOccurrence} has no direct link to {@code Cohort} -- it only carries a
     *  {@code CohortSection}, and {@code CohortSection} itself has no {@code cohort} field either;
     *  the real chain is {@code cohortSection -> cohortRoomAllocation -> cohort}, same one {@link
     *  com.cms.service.TimetableSkeletonService#resolveActiveSections} walks. Comparing {@code
     *  cohortSection.id} directly against a {@code Cohort.id} (an earlier version of this filter's
     *  bug) silently matches nothing, since the two are entirely different id spaces. */
    public static Specification<SessionOccurrence> byCohortId(Long cohortId) {
        return (root, query, cb) -> cb.equal(
            root.get("cohortSection").get("cohortRoomAllocation").get("cohort").get("id"), cohortId);
    }

    public static Specification<SessionOccurrence> byOccurrenceDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurrenceDate"), from);
    }

    public static Specification<SessionOccurrence> byOccurrenceDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurrenceDate"), to);
    }

    /** Matches subject name/code, requested/requesting faculty name, and every session type's
     *  venue name (classroom, lab, clinical venue -- exactly one is ever populated per row) --
     *  covers the "Venue/Room" filter as free text rather than a three-way dropdown, since a
     *  venue is one of three unrelated entity types depending on the row's own session type. Uses
     *  explicit LEFT JOINs for every nullable relation, same as {@link
     *  ClassScheduleSpecification#bySearch} -- {@code root.get(...)} on a nullable @ManyToOne
     *  silently becomes an INNER JOIN and would drop rows missing that one relation from the
     *  whole OR clause. */
    public static Specification<SessionOccurrence> bySearch(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            var subject = root.join("subject", JoinType.LEFT);
            var requestedFaculty = root.join("requestedFaculty", JoinType.LEFT);
            var requestedByFaculty = root.join("requestedByFaculty", JoinType.LEFT);
            var classroom = root.join("classroom", JoinType.LEFT);
            var lab = root.join("lab", JoinType.LEFT);
            var clinicalVenue = root.join("clinicalVenue", JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(cb.coalesce(subject.get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(subject.get("code"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(requestedFaculty.get("firstName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(requestedFaculty.get("lastName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(requestedByFaculty.get("firstName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(requestedByFaculty.get("lastName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(classroom.get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(lab.get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(clinicalVenue.get("name"), "")), pattern)
            );
        };
    }
}
