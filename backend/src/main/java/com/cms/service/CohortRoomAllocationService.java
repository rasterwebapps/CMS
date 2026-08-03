package com.cms.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AllocatedBatchResponse;
import com.cms.dto.CohortRoomAllocationCommitRequest;
import com.cms.dto.CohortRoomAllocationResponse;
import com.cms.dto.VentureSplitRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CourseOffering;
import com.cms.model.Lab;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Commits a Cohort's physical-location claim for a Term: one Theory home room plus however many
 * Lab/Clinical batches are needed, editable-not-forced-even split sizes, capacity-fit validated
 * against each chosen venue. Term-scoped only — no day/period here, that belongs to the later
 * Staffing pass (see the deferred requirements captured in the implementation plan). Reverting
 * never deletes: it flips {@link CohortRoomAllocationStatus} to REVERTED and soft-deactivates the
 * batches this commit created, so roster history survives.
 */
@Service
@Transactional(readOnly = true)
public class CohortRoomAllocationService {

    private final CohortRoomAllocationRepository allocationRepository;
    private final CohortRepository cohortRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final BatchRepository batchRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    public CohortRoomAllocationService(CohortRoomAllocationRepository allocationRepository,
                                        CohortRepository cohortRepository,
                                        TermInstanceRepository termInstanceRepository,
                                        ClassroomRepository classroomRepository,
                                        LabRepository labRepository,
                                        ClinicalVenueRepository clinicalVenueRepository,
                                        CourseOfferingRepository courseOfferingRepository,
                                        BatchRepository batchRepository,
                                        StudentTermEnrollmentRepository studentTermEnrollmentRepository) {
        this.allocationRepository = allocationRepository;
        this.cohortRepository = cohortRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.batchRepository = batchRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
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
        Classroom theoryClassroom = classroomRepository.findById(request.theoryClassroomId())
            .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + request.theoryClassroomId()));

        long strength = studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(
            request.termInstanceId(), request.cohortId(), EnrollmentStatus.ENROLLED);

        if (theoryClassroom.getCapacity() != null && theoryClassroom.getCapacity() < strength) {
            throw new LifecycleConflictException(
                "Theory classroom '" + theoryClassroom.getName() + "' seats " + theoryClassroom.getCapacity()
                    + ", but this cohort has " + strength + " enrolled students.",
                "COHORT_ROOM_ALLOCATION_CAPACITY_EXCEEDED", "Classroom", theoryClassroom.getId(), null);
        }

        for (VentureSplitRequest split : request.ventureSplits()) {
            validateVentureCapacity(split);
        }

        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, theoryClassroom, committedBy);
        try {
            allocation = allocationRepository.save(allocation);
        } catch (DataIntegrityViolationException e) {
            throw new LifecycleConflictException(
                "This cohort already has a committed room allocation for this term, or another cohort has already "
                    + "claimed this Theory classroom for this term — revert the existing allocation first.",
                "COHORT_ROOM_ALLOCATION_CONFLICT", "CohortRoomAllocation", null, null);
        }

        for (VentureSplitRequest split : request.ventureSplits()) {
            createVentureBatch(allocation, split);
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

        allocation.setStatus(CohortRoomAllocationStatus.REVERTED);
        allocation.setRevertedBy(revertedBy);
        allocation.setRevertedAt(java.time.Instant.now());
        allocationRepository.save(allocation);

        for (Batch batch : batchRepository.findByCohortRoomAllocationId(allocationId)) {
            batch.setIsActive(false);
            batchRepository.save(batch);
        }

        return toResponse(allocation);
    }

    private void validateVentureCapacity(VentureSplitRequest split) {
        Integer capacity = switch (split.sessionType()) {
            case LAB -> labRepository.findById(split.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + split.venueId()))
                .getCapacity();
            case CLINICAL -> clinicalVenueRepository.findById(split.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + split.venueId()))
                .getCapacity();
            case THEORY -> throw new IllegalArgumentException(
                "Venture splits are for LAB/CLINICAL batches only — the Theory room is set once on the allocation header.");
        };
        if (capacity != null && capacity < split.plannedSize()) {
            throw new LifecycleConflictException(
                "Batch '" + split.batchName() + "' plans " + split.plannedSize() + " students, but the assigned venue "
                    + "only seats " + capacity + ".",
                "COHORT_ROOM_ALLOCATION_CAPACITY_EXCEEDED", split.sessionType().name(), split.venueId(), null);
        }
    }

    private void createVentureBatch(CohortRoomAllocation allocation, VentureSplitRequest split) {
        CourseOffering offering = courseOfferingRepository.findById(split.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + split.courseOfferingId()));

        Batch batch = new Batch(offering, split.batchName(), split.plannedSize(), offering.getTermInstance());
        batch.setCohortRoomAllocation(allocation);
        switch (split.sessionType()) {
            case LAB -> batch.setLab(labRepository.getReferenceById(split.venueId()));
            case CLINICAL -> batch.setClinicalVenue(clinicalVenueRepository.getReferenceById(split.venueId()));
            case THEORY -> throw new IllegalArgumentException(
                "Venture splits are for LAB/CLINICAL batches only — the Theory room is set once on the allocation header.");
        }
        batchRepository.save(batch);
    }

    private CohortRoomAllocationResponse toResponse(CohortRoomAllocation allocation) {
        Cohort cohort = allocation.getCohort();
        TermInstance term = allocation.getTermInstance();
        Classroom theoryClassroom = allocation.getTheoryClassroom();

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
            theoryClassroom.getId(),
            theoryClassroom.getName(),
            theoryClassroom.getCapacity(),
            allocation.getCommittedBy(),
            allocation.getCommittedAt(),
            allocation.getRevertedBy(),
            allocation.getRevertedAt(),
            batches
        );
    }

    private AllocatedBatchResponse toAllocatedBatchResponse(Batch batch) {
        Lab lab = batch.getLab();
        ClinicalVenue clinicalVenue = batch.getClinicalVenue();
        ClassSessionType sessionType = lab != null ? ClassSessionType.LAB : ClassSessionType.CLINICAL;
        Long venueId = lab != null ? lab.getId() : (clinicalVenue != null ? clinicalVenue.getId() : null);
        String venueName = lab != null ? lab.getName() : (clinicalVenue != null ? clinicalVenue.getName() : null);
        Integer venueCapacity = lab != null ? lab.getCapacity() : (clinicalVenue != null ? clinicalVenue.getCapacity() : null);

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
            batch.getIsActive()
        );
    }
}
