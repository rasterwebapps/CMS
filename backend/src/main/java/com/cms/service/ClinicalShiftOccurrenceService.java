package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.ClinicalShiftTheoryBlock;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
import com.cms.repository.ClinicalShiftTheoryBlockRepository;
import com.cms.repository.SessionOccurrenceRepository;

/**
 * Materializes a {@link ClinicalShiftGroup}'s clinical + shared theory blocks into real,
 * attendance-capable {@link SessionOccurrence} rows for a specific date (OC-175 Piece 2) --
 * mirrors how regular ClassSchedule occurrences get materialized lazily rather than
 * pre-populating every theoretical future date. Idempotent: re-running for an already-generated
 * date is a no-op for rows that already exist.
 */
@Service
@Transactional
public class ClinicalShiftOccurrenceService {

    private final ClinicalShiftGroupRepository shiftGroupRepository;
    private final ClinicalShiftTheoryBlockRepository theoryBlockRepository;
    private final BatchRepository batchRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;

    public ClinicalShiftOccurrenceService(ClinicalShiftGroupRepository shiftGroupRepository,
                                           ClinicalShiftTheoryBlockRepository theoryBlockRepository,
                                           BatchRepository batchRepository,
                                           SessionOccurrenceRepository sessionOccurrenceRepository) {
        this.shiftGroupRepository = shiftGroupRepository;
        this.theoryBlockRepository = theoryBlockRepository;
        this.batchRepository = batchRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
    }

    /**
     * Generates the CLINICAL (one per linked batch) and shared THEORY occurrences for one shift
     * group on one date. Caller is responsible for only invoking this on a date whose day-of-week
     * matches {@link ClinicalShiftGroup#getDayOfWeek()} (e.g. an academic-calendar-aware nightly
     * job or an admin-triggered "generate this week" action) -- this method does not itself derive
     * dates from the recurring day-of-week rule.
     */
    public List<SessionOccurrence> generateForDate(Long shiftGroupId, LocalDate occurrenceDate) {
        ClinicalShiftGroup group = shiftGroupRepository.findById(shiftGroupId)
            .orElseThrow(() -> new ResourceNotFoundException("Clinical shift group not found with id: " + shiftGroupId));
        CourseOffering offering = group.getCourseOffering();
        if (offering.getClinicalShiftDurationMinutes() == null) {
            throw new IllegalStateException(
                "Course offering " + offering.getId() + " has no clinical shift duration configured");
        }
        if ((group.getEffectiveStartDate() != null && occurrenceDate.isBefore(group.getEffectiveStartDate()))
            || (group.getEffectiveEndDate() != null && occurrenceDate.isAfter(group.getEffectiveEndDate()))) {
            throw new IllegalStateException(
                "Shift group " + shiftGroupId + " is only effective " + group.getEffectiveStartDate()
                    + " to " + group.getEffectiveEndDate() + " -- " + occurrenceDate + " is outside that window");
        }
        LocalDate hoursSufficientCutoff = hoursSufficientCutoffDate(group, offering);
        if (hoursSufficientCutoff != null && occurrenceDate.isAfter(hoursSufficientCutoff)) {
            throw new IllegalStateException(
                "Shift group " + shiftGroupId + " already delivers its full "
                    + offering.getCurriculumSemesterCourse().getClinicalHours()
                    + "h Clinical requirement by " + hoursSufficientCutoff + " -- " + occurrenceDate
                    + " would over-deliver and should not be generated (raise the offering's curriculum "
                    + "Clinical hours if more duty weeks are genuinely needed)");
        }
        LocalTime clinicalEnd = group.getClinicalStartTime().plusMinutes(offering.getClinicalShiftDurationMinutes());

        List<SessionOccurrence> created = new java.util.ArrayList<>();
        for (Batch batch : batchRepository.findByClinicalShiftGroupId(shiftGroupId)) {
            created.addAll(generateClinicalOccurrence(group, batch, occurrenceDate, clinicalEnd).stream().toList());
        }
        for (ClinicalShiftTheoryBlock block : theoryBlockRepository.findByShiftGroupIdOrderBySequenceOrderAsc(shiftGroupId)) {
            generateTheoryOccurrence(group, block, occurrenceDate).ifPresent(created::add);
        }
        return created;
    }

    /** The last calendar date this group should still generate a real occurrence for, given how
     *  many weekly duty-length occurrences the offering's curriculum Clinical hours actually need
     *  (see {@link CurriculumHoursCalculator#weeksNeededFor}) -- {@code null} means "no cap
     *  applies" (no positive Clinical hours configured), in which case only the group's own manual
     *  {@code effectiveStartDate}/{@code effectiveEndDate} (already checked above) governs. Always
     *  the TIGHTER of "hours math" and any manual range, never looser -- see {@link
     *  TimetableSkeletonService#toClinicalShiftHours}'s identical cap on the hours-crediting side;
     *  the two must never disagree, or a subject's reported hours and its real duty calendar would
     *  silently diverge again. */
    private LocalDate hoursSufficientCutoffDate(ClinicalShiftGroup group, CourseOffering offering) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        Integer rawHours = csc != null ? csc.getClinicalHours() : null;
        double hoursPerOccurrence = offering.getClinicalShiftDurationMinutes() / 60.0;
        int neededWeeks = CurriculumHoursCalculator.weeksNeededFor(rawHours != null ? rawHours : 0, hoursPerOccurrence);
        if (neededWeeks <= 0) {
            return null;
        }
        LocalDate firstOccurrence = group.getEffectiveStartDate() != null
            ? group.getEffectiveStartDate()
            // com.cms.model.enums.DayOfWeek only has MONDAY..SATURDAY -- names line up exactly
            // with java.time.DayOfWeek's, so valueOf() is a safe direct mapping (see
            // ClassScheduleOccurrenceService#weeklyDatesInRange for the same pattern).
            : group.getTermInstance().getStartDate().with(
                TemporalAdjusters.nextOrSame(java.time.DayOfWeek.valueOf(group.getDayOfWeek().name())));
        return firstOccurrence.plusWeeks(neededWeeks - 1L);
    }

    private java.util.Optional<SessionOccurrence> generateClinicalOccurrence(ClinicalShiftGroup group, Batch batch,
            LocalDate occurrenceDate, LocalTime clinicalEnd) {
        if (sessionOccurrenceRepository.findByOccurrenceSourceAndBatch_IdAndOccurrenceDate(
                OccurrenceSource.CLINICAL_SHIFT, batch.getId(), occurrenceDate).isPresent()) {
            return java.util.Optional.empty();
        }
        SessionOccurrence occurrence = new SessionOccurrence();
        occurrence.setOccurrenceSource(OccurrenceSource.CLINICAL_SHIFT);
        occurrence.setOccurrenceDate(occurrenceDate);
        occurrence.setOccurrenceStatus(OccurrenceStatus.HELD);
        occurrence.setSessionType(ClassSessionType.CLINICAL);
        occurrence.setCourseOffering(group.getCourseOffering());
        occurrence.setSubject(group.getCourseOffering().getSubject());
        occurrence.setBatch(batch);
        occurrence.setClinicalVenue(batch.getClinicalVenue());
        occurrence.setBlockStartTime(group.getClinicalStartTime());
        occurrence.setBlockEndTime(clinicalEnd);
        return java.util.Optional.of(sessionOccurrenceRepository.save(occurrence));
    }

    private java.util.Optional<SessionOccurrence> generateTheoryOccurrence(ClinicalShiftGroup group,
            ClinicalShiftTheoryBlock block, LocalDate occurrenceDate) {
        if (group.getCohortSection() == null) {
            throw new IllegalStateException(
                "Shift group " + group.getId() + " has no cohort section set -- required to scope the shared theory block");
        }
        if (sessionOccurrenceRepository.findByOccurrenceSourceAndCohortSection_IdAndSubject_IdAndBlockStartTimeAndOccurrenceDate(
                OccurrenceSource.CLINICAL_SHIFT, group.getCohortSection().getId(), block.getSubject().getId(),
                block.getStartTime(), occurrenceDate).isPresent()) {
            return java.util.Optional.empty();
        }
        SessionOccurrence occurrence = new SessionOccurrence();
        occurrence.setOccurrenceSource(OccurrenceSource.CLINICAL_SHIFT);
        occurrence.setOccurrenceDate(occurrenceDate);
        occurrence.setOccurrenceStatus(OccurrenceStatus.HELD);
        occurrence.setSessionType(ClassSessionType.THEORY);
        occurrence.setCourseOffering(group.getCourseOffering());
        occurrence.setSubject(block.getSubject());
        occurrence.setCohortSection(group.getCohortSection());
        occurrence.setClassroom(block.getClassroom());
        occurrence.setBlockStartTime(block.getStartTime());
        occurrence.setBlockEndTime(block.getEndTime());
        return java.util.Optional.of(sessionOccurrenceRepository.save(occurrence));
    }
}
