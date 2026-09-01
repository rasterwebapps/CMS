package com.cms.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AllocatedBatchResponse;
import com.cms.dto.CohortRoomAllocationCommitRequest;
import com.cms.dto.CohortRoomAllocationResponse;
import com.cms.dto.CohortSectionRequest;
import com.cms.dto.CohortSectionResponse;
import com.cms.dto.VentureSplitRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.Lab;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Commits a Cohort's physical-location claim for a Term: one or more Theory sections (each its
 * own classroom + headcount, forced into 2+ when no single classroom fits) plus however many
 * Lab/Clinical batches are needed per section, editable-not-forced-even split sizes, capacity-fit
 * validated against each chosen venue. Term-scoped only — no day/period here, that belongs to the
 * later Staffing pass. Reverting never deletes: it flips {@link CohortRoomAllocationStatus} to
 * REVERTED and soft-deactivates the sections/batches this commit created, so roster history
 * survives.
 */
@Service
@Transactional(readOnly = true)
public class CohortRoomAllocationService {

    private final CohortRoomAllocationRepository allocationRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final CohortRepository cohortRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final BatchRepository batchRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;

    public CohortRoomAllocationService(CohortRoomAllocationRepository allocationRepository,
                                        CohortSectionRepository cohortSectionRepository,
                                        CohortRepository cohortRepository,
                                        TermInstanceRepository termInstanceRepository,
                                        ClassroomRepository classroomRepository,
                                        LabRepository labRepository,
                                        ClinicalVenueRepository clinicalVenueRepository,
                                        CourseOfferingRepository courseOfferingRepository,
                                        BatchRepository batchRepository,
                                        StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                        ClassScheduleRepository classScheduleRepository,
                                        CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository) {
        this.allocationRepository = allocationRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.cohortRepository = cohortRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.batchRepository = batchRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.courseOfferingSectionFacultyRepository = courseOfferingSectionFacultyRepository;
    }

    public CohortRoomAllocationResponse getCurrent(Long cohortId, Long termInstanceId) {
        return allocationRepository
            .findByCohortIdAndTermInstanceIdAndStatus(cohortId, termInstanceId, CohortRoomAllocationStatus.COMMITTED)
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional
    public CohortRoomAllocationResponse commit(CohortRoomAllocationCommitRequest request, String committedBy) {
        Cohort cohort = cohortRepository.findByIdWithCourse(request.cohortId())
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + request.cohortId()));
        TermInstance term = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));

        int strength = resolveStrength(cohort, request.termInstanceId(), request.cohortId(), request.planningBasis());

        // Validate sections: each fits its own classroom, no classroom claimed twice in this
        // request, and together they cover exactly the planning-basis strength -- not "at least":
        // the chosen basis (SANCTIONED intake, when a first-term cohort's live enrollment is still
        // unsettled) is already the deliberate ceiling to plan against, so committing beyond it
        // just double-books an extra room for seats that were never going to exist.
        Map<String, Classroom> classroomsByLabel = new HashMap<>();
        Set<Long> classroomIdsUsed = new HashSet<>();
        int totalPlanned = 0;
        for (CohortSectionRequest section : request.sections()) {
            Classroom classroom = classroomRepository.findById(section.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + section.classroomId()));
            if (Boolean.TRUE.equals(classroom.getAllowsConcurrentSharing())) {
                throw new LifecycleConflictException(
                    "Classroom '" + classroom.getName() + "' allows concurrent sharing and can't be committed as an "
                        + "exclusive Theory section — pick a different classroom.",
                    "COHORT_ROOM_ALLOCATION_SHARED_CLASSROOM_NOT_ALLOWED", "Classroom", classroom.getId(), null);
            }
            if (!classroomIdsUsed.add(classroom.getId())) {
                throw new LifecycleConflictException(
                    "Classroom '" + classroom.getName() + "' is assigned to more than one section in this commit.",
                    "COHORT_ROOM_ALLOCATION_DUPLICATE_CLASSROOM", "Classroom", classroom.getId(), null);
            }
            if (classroom.getCapacity() != null && classroom.getCapacity() < section.plannedSize()) {
                throw new LifecycleConflictException(
                    "Section '" + section.sectionLabel() + "' plans " + section.plannedSize() + " students, but "
                        + classroom.getName() + " only seats " + classroom.getCapacity() + ".",
                    "COHORT_ROOM_ALLOCATION_CAPACITY_EXCEEDED", "Classroom", classroom.getId(), null);
            }
            classroomsByLabel.put(section.sectionLabel(), classroom);
            totalPlanned += section.plannedSize();
        }
        if (totalPlanned < strength) {
            throw new LifecycleConflictException(
                "Sections cover only " + totalPlanned + " of " + strength + " students — add more seats before committing.",
                "COHORT_ROOM_ALLOCATION_UNDER_COVERED", "Cohort", cohort.getId(), null);
        }
        if (totalPlanned > strength) {
            throw new LifecycleConflictException(
                "Sections cover " + totalPlanned + " students, " + (totalPlanned - strength) + " more than the "
                    + strength + " being planned for — resize or remove a section.",
                "COHORT_ROOM_ALLOCATION_OVER_COVERED", "Cohort", cohort.getId(), null);
        }

        for (VentureSplitRequest split : request.ventureSplits()) {
            validateVentureCapacity(split);
        }

        // Which section each venture split belongs to: auto-resolves to the lone section when the
        // cohort isn't split, otherwise must be an explicit, valid section label.
        Set<String> sectionLabels = classroomsByLabel.keySet();
        String soleSectionLabel = sectionLabels.size() == 1 ? sectionLabels.iterator().next() : null;
        for (VentureSplitRequest split : request.ventureSplits()) {
            String label = soleSectionLabel != null ? soleSectionLabel : split.cohortSectionLabel();
            if (label == null || !sectionLabels.contains(label)) {
                throw new LifecycleConflictException(
                    "Batch '" + split.batchName() + "' must specify which section it belongs to — this cohort has "
                        + "multiple sections committed.",
                    "COHORT_ROOM_ALLOCATION_SECTION_REQUIRED", "Batch", null, null);
            }
        }

        // Per (section, subject, session type): the sum of planned batch sizes must exactly cover
        // that section's own planned size — two subjects' Lab batches are independent partitions of
        // the same section, so this is grouped per subject too, not just per section. Exact, not
        // "at most": under-covering silently leaves some of that section's students with no
        // Lab/Clinical batch at all, which is just as wrong as over-committing.
        Map<String, Integer> plannedSizeByLabel = new HashMap<>();
        for (CohortSectionRequest section : request.sections()) {
            plannedSizeByLabel.put(section.sectionLabel(), section.plannedSize());
        }
        Map<VentureKey, Integer> ventureTotals = new HashMap<>();
        for (VentureSplitRequest split : request.ventureSplits()) {
            String label = soleSectionLabel != null ? soleSectionLabel : split.cohortSectionLabel();
            VentureKey key = new VentureKey(label, split.courseOfferingId(), split.sessionType());
            ventureTotals.merge(key, split.plannedSize(), Integer::sum);
        }
        for (Map.Entry<VentureKey, Integer> entry : ventureTotals.entrySet()) {
            VentureKey key = entry.getKey();
            int total = entry.getValue();
            int sectionSize = plannedSizeByLabel.get(key.sectionLabel());
            if (total != sectionSize) {
                String verb = total > sectionSize ? "more" : "fewer";
                throw new LifecycleConflictException(
                    "This subject's " + key.sessionType() + " batches for section '" + key.sectionLabel() + "' plan "
                        + verb + " students (" + total + ") than that section itself (" + sectionSize + ") — "
                        + "the batches must add up to exactly the section's headcount.",
                    "COHORT_ROOM_ALLOCATION_SECTION_MISMATCH", "Batch", null, null);
            }
        }

        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, request.planningBasis(), strength, committedBy);
        try {
            allocation = allocationRepository.save(allocation);
        } catch (DataIntegrityViolationException e) {
            throw new LifecycleConflictException(
                "This cohort already has a committed room allocation for this term — revert the existing allocation first.",
                "COHORT_ROOM_ALLOCATION_CONFLICT", "CohortRoomAllocation", null, null);
        }

        Map<String, CohortSection> sectionsByLabel = new HashMap<>();
        for (CohortSectionRequest sectionRequest : request.sections()) {
            CohortSection section = new CohortSection(allocation, term, sectionRequest.sectionLabel(),
                classroomsByLabel.get(sectionRequest.sectionLabel()), sectionRequest.plannedSize());
            try {
                section = cohortSectionRepository.save(section);
            } catch (DataIntegrityViolationException e) {
                throw new LifecycleConflictException(
                    "Classroom '" + section.getClassroom().getName() + "' is already claimed by another cohort's "
                        + "section for this term — revert that allocation first or pick a different classroom.",
                    "COHORT_ROOM_ALLOCATION_CONFLICT", "CohortSection", null, null);
            }
            sectionsByLabel.put(sectionRequest.sectionLabel(), section);
            migrateFacultyAssignmentsToNewSection(cohort.getId(), sectionRequest.sectionLabel(), section);
        }

        for (VentureSplitRequest split : request.ventureSplits()) {
            String label = soleSectionLabel != null ? soleSectionLabel : split.cohortSectionLabel();
            createVentureBatch(allocation, sectionsByLabel.get(label), split);
        }

        return toResponse(allocation);
    }

    @Transactional
    public CohortRoomAllocationResponse revert(Long allocationId, String revertedBy) {
        CohortRoomAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort room allocation not found with id: " + allocationId));

        if (allocation.getStatus() == CohortRoomAllocationStatus.REVERTED) {
            throw new LifecycleConflictException(
                "This allocation was already reverted.",
                "COHORT_ROOM_ALLOCATION_ALREADY_REVERTED", "CohortRoomAllocation", allocation.getId(), null);
        }

        List<Batch> batches = batchRepository.findByCohortRoomAllocationId(allocationId);
        List<CohortSection> sections = cohortSectionRepository.findByCohortRoomAllocationId(allocationId);
        List<Long> batchIds = batches.stream().map(Batch::getId).toList();
        List<Long> sectionIds = sections.stream().map(CohortSection::getId).toList();

        List<ClassSchedule> ridingCells = new java.util.ArrayList<>();
        if (!batchIds.isEmpty()) ridingCells.addAll(classScheduleRepository.findByBatchIdInAndIsActiveTrue(batchIds));
        if (!sectionIds.isEmpty()) ridingCells.addAll(classScheduleRepository.findByCohortSectionIdInAndIsActiveTrue(sectionIds));

        // A already-published cell riding on this allocation means real faculty/students are
        // already relying on it -- reverting the allocation underneath it would silently orphan a
        // live timetable (still isActive=true, still blocking every conflict check, but invisible
        // to Skeleton Builder's per-batch/per-section hour tally once its batch/section is
        // deactivated below). Force an explicit unpublish first rather than let that happen quietly.
        boolean hasPublished = ridingCells.stream().anyMatch(cs -> cs.getStatus() == ClassScheduleStatus.PUBLISHED);
        if (hasPublished) {
            throw new LifecycleConflictException(
                "This allocation has already-published timetable sessions riding on it — unpublish or remove those "
                    + "sessions before reverting the room allocation underneath them.",
                "COHORT_ROOM_ALLOCATION_HAS_PUBLISHED_SESSIONS", "CohortRoomAllocation", allocation.getId(), null);
        }

        allocation.setStatus(CohortRoomAllocationStatus.REVERTED);
        allocation.setRevertedBy(revertedBy);
        allocation.setRevertedAt(java.time.Instant.now());
        allocationRepository.save(allocation);

        for (Batch batch : batches) {
            batch.setIsActive(false);
            batchRepository.save(batch);
        }
        for (CohortSection section : sections) {
            section.setIsActive(false);
            cohortSectionRepository.save(section);
        }
        // DRAFT cells riding on the now-deactivated batches/sections would otherwise sit orphaned
        // forever: still isActive=true, so still fully blocking their faculty/room/day/period in
        // every future conflict check, yet no longer counted anywhere as "placed" since Skeleton
        // Builder's tally only looks at the currently-active batch/section list. Deactivating them
        // here frees the slot for re-placement and keeps the tally honest.
        for (ClassSchedule cell : ridingCells) {
            cell.setIsActive(false);
            classScheduleRepository.save(cell);
        }

        return toResponse(allocation);
    }

    /** Carries forward existing per-section faculty assignments (Assign Faculty) onto a freshly
     *  committed section that replaces an earlier, now-reverted one with the same label -- without
     *  this, every {@link CourseOfferingSectionFaculty} row for the old section stays pinned to it
     *  forever once it's deactivated, invisibly orphaned exactly like the {@code class_schedules}
     *  ghost-cell bug this mirrors (both stem from {@link #revert} creating a new generation of
     *  {@link CohortSection} ids without anything downstream following along). Repeated
     *  revert/recommit cycles can leave several stale generations sharing the same label; only the
     *  most-recently-updated orphaned row per course offering is revived -- the rest are harmless
     *  dead rows nothing will ever query again. */
    private void migrateFacultyAssignmentsToNewSection(Long cohortId, String sectionLabel, CohortSection newSection) {
        List<CourseOfferingSectionFaculty> orphaned = courseOfferingSectionFacultyRepository
            .findByCohort_IdAndCohortSection_IsActiveFalseAndCohortSection_SectionLabel(cohortId, sectionLabel);
        Map<Long, CourseOfferingSectionFaculty> freshestByOffering = new HashMap<>();
        for (CourseOfferingSectionFaculty row : orphaned) {
            Long offeringId = row.getCourseOffering().getId();
            CourseOfferingSectionFaculty existing = freshestByOffering.get(offeringId);
            java.time.Instant rowUpdatedAt = row.getUpdatedAt();
            java.time.Instant existingUpdatedAt = existing != null ? existing.getUpdatedAt() : null;
            if (existing == null || existingUpdatedAt == null
                || (rowUpdatedAt != null && rowUpdatedAt.isAfter(existingUpdatedAt))) {
                freshestByOffering.put(offeringId, row);
            }
        }
        for (CourseOfferingSectionFaculty row : freshestByOffering.values()) {
            row.setCohortSection(newSection);
            courseOfferingSectionFacultyRepository.save(row);
        }
    }

    private int resolveStrength(Cohort cohort, Long termInstanceId, Long cohortId, PlanningBasis basis) {
        if (basis == PlanningBasis.SANCTIONED) {
            Integer sanctioned = cohort.getSanctionedIntake();
            if (sanctioned == null) {
                throw new LifecycleConflictException(
                    "This cohort has no sanctioned intake seats configured — set Total Seats in Cohort masters "
                        + "before planning against sanctioned intake.",
                    "COHORT_ROOM_ALLOCATION_NO_SANCTIONED_STRENGTH", "Cohort", cohort.getId(), null);
            }
            return sanctioned;
        }
        return (int) studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(
            termInstanceId, cohortId, EnrollmentStatus.ENROLLED);
    }

    private void validateVentureCapacity(VentureSplitRequest split) {
        Integer capacity = switch (split.sessionType()) {
            case LAB -> labRepository.findById(split.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + split.venueId()))
                .getCapacity();
            case CLINICAL -> clinicalVenueRepository.findById(split.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + split.venueId()))
                .getCapacity();
            case THEORY, LIBRARY -> throw new IllegalArgumentException(
                "Venture splits are for LAB/CLINICAL batches only — Theory sections are committed via the "
                    + "'sections' field.");
        };
        if (capacity != null && capacity < split.plannedSize()) {
            throw new LifecycleConflictException(
                "Batch '" + split.batchName() + "' plans " + split.plannedSize() + " students, but the assigned venue "
                    + "only seats " + capacity + ".",
                "COHORT_ROOM_ALLOCATION_CAPACITY_EXCEEDED", split.sessionType().name(), split.venueId(), null);
        }
    }

    private void createVentureBatch(CohortRoomAllocation allocation, CohortSection section, VentureSplitRequest split) {
        CourseOffering offering = courseOfferingRepository.findById(split.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + split.courseOfferingId()));

        Batch batch = new Batch(offering, split.batchName(), split.plannedSize(), offering.getTermInstance());
        batch.setCohortRoomAllocation(allocation);
        batch.setCohortSection(section);
        switch (split.sessionType()) {
            case LAB -> batch.setLab(labRepository.getReferenceById(split.venueId()));
            case CLINICAL -> batch.setClinicalVenue(clinicalVenueRepository.getReferenceById(split.venueId()));
            case THEORY, LIBRARY -> throw new IllegalArgumentException(
                "Venture splits are for LAB/CLINICAL batches only — Theory sections are committed via the "
                    + "'sections' field.");
        }
        batchRepository.save(batch);
    }

    private CohortRoomAllocationResponse toResponse(CohortRoomAllocation allocation) {
        Cohort cohort = allocation.getCohort();
        TermInstance term = allocation.getTermInstance();

        List<CohortSectionResponse> sections = cohortSectionRepository.findByCohortRoomAllocationId(allocation.getId()).stream()
            .map(this::toSectionResponse)
            .toList();
        List<AllocatedBatchResponse> batches = batchRepository.findByCohortRoomAllocationId(allocation.getId()).stream()
            .map(this::toAllocatedBatchResponse)
            .toList();

        return new CohortRoomAllocationResponse(
            allocation.getId(),
            cohort.getId(),
            cohort.getDisplayName(),
            term.getId(),
            term.getAcademicYear().getName() + " " + term.getTermType(),
            allocation.getStatus(),
            allocation.getPlanningBasis(),
            allocation.getPlannedStrength(),
            sections,
            allocation.getCommittedBy(),
            allocation.getCommittedAt(),
            allocation.getRevertedBy(),
            allocation.getRevertedAt(),
            batches
        );
    }

    private CohortSectionResponse toSectionResponse(CohortSection section) {
        Classroom classroom = section.getClassroom();
        return new CohortSectionResponse(
            section.getId(),
            section.getSectionLabel(),
            classroom.getId(),
            classroom.getName(),
            classroom.getCapacity(),
            section.getPlannedSize(),
            section.getIsActive()
        );
    }

    private AllocatedBatchResponse toAllocatedBatchResponse(Batch batch) {
        Lab lab = batch.getLab();
        ClinicalVenue clinicalVenue = batch.getClinicalVenue();
        ClassSessionType sessionType = lab != null ? ClassSessionType.LAB : ClassSessionType.CLINICAL;
        Long venueId = lab != null ? lab.getId() : (clinicalVenue != null ? clinicalVenue.getId() : null);
        String venueName = lab != null ? lab.getName() : (clinicalVenue != null ? clinicalVenue.getName() : null);
        Integer venueCapacity = lab != null ? lab.getCapacity() : (clinicalVenue != null ? clinicalVenue.getCapacity() : null);
        CohortSection section = batch.getCohortSection();

        return new AllocatedBatchResponse(
            batch.getId(),
            batch.getCourseOffering().getId(),
            batch.getCourseOffering().getSubject().getName(),
            sessionType,
            venueId,
            venueName,
            venueCapacity,
            batch.getName(),
            batch.getCapacity(),
            batch.getIsActive(),
            section != null ? section.getId() : null,
            section != null ? section.getSectionLabel() : null
        );
    }

    /** Groups venture splits by which section/subject/session-type partition they belong to, for
     *  the exact-coverage check above -- a plain record instead of a delimited string key so a
     *  section label containing '|' (or any other punctuation) can never misparse into the wrong
     *  group. */
    private record VentureKey(String sectionLabel, Long courseOfferingId, ClassSessionType sessionType) {
    }
}
