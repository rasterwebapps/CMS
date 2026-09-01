package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.ClinicalShiftTheoryBlock;
import com.cms.model.CourseOffering;
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
