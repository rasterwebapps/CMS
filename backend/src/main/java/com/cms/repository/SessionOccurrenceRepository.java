package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SessionOccurrence;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.SpecialClassApprovalStatus;

public interface SessionOccurrenceRepository extends JpaRepository<SessionOccurrence, Long> {

    Optional<SessionOccurrence> findByClassScheduleIdAndOccurrenceDate(Long classScheduleId, LocalDate occurrenceDate);

    List<SessionOccurrence> findByClassSchedule_CourseOffering_Id(Long courseOfferingId);

    List<SessionOccurrence> findByClassSchedule_TermInstance_IdAndClassSchedule_Status(
        Long termInstanceId, com.cms.model.enums.ClassScheduleStatus status);

    // ---- BR-55: special-class / day-repeat lookups. Additive only -- the three methods above,
    // all implicitly inner-joined through classSchedule, structurally can never see a row with a
    // null classSchedule, so they're untouched and continue to only surface REGULAR rows. ----

    /** Same-date/period conflict check against other special classes -- the one thing
     *  {@code ClassScheduleRepository.findOverlapping} can't see, since these rows have no
     *  ClassSchedule for it to match against. */
    List<SessionOccurrence> findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(
        List<OccurrenceSource> sources, LocalDate occurrenceDate, Long periodId);

    /** Faculty's own "My Special Classes" list. */
    List<SessionOccurrence> findByRequestedByFaculty_IdAndOccurrenceSourceInOrderByOccurrenceDateDesc(
        Long facultyId, List<OccurrenceSource> sources);

    /** Admin approval queue. */
    List<SessionOccurrence> findByApprovalStatusAndOccurrenceSourceInOrderByRequestedAtAsc(
        SpecialClassApprovalStatus approvalStatus, List<OccurrenceSource> sources);

    /** Fetch/bulk-act on every row of one DAY_REPEAT submission. */
    List<SessionOccurrence> findByRequestBatchId(UUID requestBatchId);

    /** Reserved for the progress-report-crediting fast-follow (BR-55, explicitly out of scope for
     *  v1) -- additive counterpart to {@link #findByClassSchedule_CourseOffering_Id}, which
     *  cannot see these rows either. Not yet called from any service. */
    List<SessionOccurrence> findByCourseOffering_Id(Long courseOfferingId);
}
