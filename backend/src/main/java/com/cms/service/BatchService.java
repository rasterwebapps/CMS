package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.BatchDto;
import com.cms.dto.BatchRequest;
import com.cms.dto.BatchStudentDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Student;
import com.cms.repository.BatchRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class BatchService {

    private final BatchRepository batchRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;

    public BatchService(BatchRepository batchRepository,
                         CourseOfferingRepository courseOfferingRepository,
                         FacultyRepository facultyRepository,
                         StudentRepository studentRepository) {
        this.batchRepository = batchRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public BatchDto updateBatch(Long id, BatchRequest request) {
        Batch batch = getOrThrow(id);

        if (!batch.getName().equals(request.name())
                && batchRepository.existsByCourseOfferingIdAndName(batch.getCourseOffering().getId(), request.name())) {
            throw new IllegalArgumentException(
                "A batch named '" + request.name() + "' already exists for this course offering");
        }

        batch.setName(request.name());
        batch.setCapacity(request.capacity());
        applyCoordinator(batch, request.coordinatorFacultyId());

        return toDto(batchRepository.save(batch));
    }

    @Transactional
    public void deactivateBatch(Long id) {
        Batch batch = getOrThrow(id);
        batch.setIsActive(false);
        batchRepository.save(batch);
    }

    /** Active batches only — a reverted {@link com.cms.model.CohortRoomAllocation} leaves its
     *  batches behind deactivated (never deleted, for roster-history reasons) rather than
     *  reactivating on a later re-commit ({@code createVentureBatch} always inserts a fresh row),
     *  so a cohort that's had its room allocation reverted and recommitted a few times accumulates
     *  several stale, inactive, same-named rows. Nothing in the product ever reactivates a batch —
     *  every consumer of this list (Batch management, Clinical Shift Group batch-linking, Escort
     *  Rotation setup, Lab Schedule) is a picker that only ever wants the live ones. */
    public List<BatchDto> getBatchesForOffering(Long courseOfferingId) {
        return batchRepository.findByCourseOfferingId(courseOfferingId)
            .stream()
            .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
            .map(this::toDto)
            .toList();
    }

    /**
     * Alternate lookup for callers (e.g. the Lab Schedule form) that only know a subject +
     * term instance, not the concrete CourseOffering id — resolves whichever offering(s) match
     * and returns their batches combined. Active only, same reasoning as {@link
     * #getBatchesForOffering}.
     */
    public List<BatchDto> getBatchesForSubjectAndTerm(Long subjectId, Long termInstanceId) {
        return courseOfferingRepository.findByTermInstanceIdAndSubjectId(termInstanceId, subjectId).stream()
            .flatMap(o -> batchRepository.findByCourseOfferingId(o.getId()).stream())
            .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
            .map(this::toDto)
            .toList();
    }

    public List<BatchStudentDto> getRoster(Long batchId) {
        Batch batch = getOrThrow(batchId);
        return batch.getStudents().stream()
            .map(s -> new BatchStudentDto(s.getId(), s.getFullName(), s.getRollNumber()))
            .toList();
    }

    @Transactional
    public void addStudent(Long batchId, Long studentId) {
        Batch batch = getOrThrow(batchId);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (batchRepository.existsStudentInBatch(batchId, studentId)) {
            return;
        }
        if (batchRepository.countStudents(batchId) >= batch.getCapacity()) {
            throw new IllegalStateException(
                "Batch '" + batch.getName() + "' is already at capacity (" + batch.getCapacity() + ")");
        }
        batch.getStudents().add(student);
        batchRepository.save(batch);
    }

    @Transactional
    public void removeStudent(Long batchId, Long studentId) {
        Batch batch = getOrThrow(batchId);
        batch.getStudents().removeIf(s -> s.getId().equals(studentId));
        batchRepository.save(batch);
    }

    /** Validates and applies this batch's per-batch teaching assignment -- the same
     *  subject-eligibility rule every other faculty-assignment point in the app enforces ({@link
     *  FacultyEligibility}). One faculty coordinating more than one batch (even parallel Lab/Clinical
     *  batches in different venues) is legitimate as long as they're never actually scheduled at an
     *  overlapping day/time -- that real conflict is caught where time is actually known, at
     *  placement, by {@link TimetableStaffingService#checkFacultyFree}, not here. Grandfathered when
     *  the requested faculty already holds this exact batch, matching {@link FacultyEligibility}'s
     *  own grandfathering. */
    private void applyCoordinator(Batch batch, Long coordinatorFacultyId) {
        Faculty previous = batch.getCoordinatorFaculty();
        if (coordinatorFacultyId == null) {
            batch.setCoordinatorFaculty(null);
            return;
        }
        if (previous != null && previous.getId().equals(coordinatorFacultyId)) {
            return;
        }
        Faculty faculty = facultyRepository.findById(coordinatorFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Faculty not found with id: " + coordinatorFacultyId));

        FacultyEligibility.require(batch.getCourseOffering().getSubject(), faculty, previous);

        batch.setCoordinatorFaculty(faculty);
    }

    private Batch getOrThrow(Long id) {
        return batchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
    }

    private BatchDto toDto(Batch b) {
        Faculty coordinator = b.getCoordinatorFaculty();
        return new BatchDto(
            b.getId(),
            b.getCourseOffering().getId(),
            b.getName(),
            b.getCapacity(),
            batchRepository.countStudents(b.getId()),
            b.getTermInstance().getId(),
            coordinator != null ? coordinator.getId() : null,
            coordinator != null ? coordinator.getFirstName() + " " + coordinator.getLastName() : null,
            b.getLab() != null ? b.getLab().getId() : null,
            b.getLab() != null ? b.getLab().getName() : null,
            b.getClinicalVenue() != null ? b.getClinicalVenue().getId() : null,
            b.getClinicalVenue() != null ? b.getClinicalVenue().getName() : null,
            b.getClinicalShiftGroup() != null ? b.getClinicalShiftGroup().getId() : null,
            b.getIsActive(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
    }
}
