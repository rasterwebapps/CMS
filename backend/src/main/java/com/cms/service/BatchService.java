package com.cms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.BatchDto;
import com.cms.dto.BatchLifecycleImpactDto;
import com.cms.dto.BatchRequest;
import com.cms.dto.BatchStudentDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Student;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.EscortRotationAssignmentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class BatchService {

    private final BatchRepository batchRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final RotationMemberAssignmentRepository rotationMemberAssignmentRepository;
    private final EscortRotationAssignmentRepository escortRotationAssignmentRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;

    public BatchService(BatchRepository batchRepository,
                         CourseOfferingRepository courseOfferingRepository,
                         FacultyRepository facultyRepository,
                         StudentRepository studentRepository,
                         ClassScheduleRepository classScheduleRepository,
                         RotationMemberAssignmentRepository rotationMemberAssignmentRepository,
                         EscortRotationAssignmentRepository escortRotationAssignmentRepository,
                         SessionOccurrenceRepository sessionOccurrenceRepository) {
        this.batchRepository = batchRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.rotationMemberAssignmentRepository = rotationMemberAssignmentRepository;
        this.escortRotationAssignmentRepository = escortRotationAssignmentRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
    }

    @Transactional
    public BatchDto updateBatch(Long id, BatchRequest request) {
        Batch batch = getOrThrow(id);
        requireCurrentVersion(batch.getVersion(), request.version(), batch.getName());

        if (!batch.getName().equalsIgnoreCase(request.name())
                && batchRepository.existsByCourseOfferingIdAndNameIgnoreCase(batch.getCourseOffering().getId(), request.name())) {
            throw new IllegalArgumentException(
                "A batch named '" + request.name() + "' already exists for this course offering");
        }

        batch.setName(request.name());
        batch.setCapacity(request.capacity());
        applyCoordinator(batch, request.coordinatorFacultyId());

        return toDto(batchRepository.save(batch));
    }

    /** Optimistic-lock check: the client's request carries the version it last saw (from its own
     *  fetch); if that no longer matches the current row, someone else changed it in between, so
     *  reject rather than silently overwrite their change with a full-replace PUT. */
    private void requireCurrentVersion(Long currentVersion, Long requestVersion, String name) {
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new IllegalStateException(
                "\"" + name + "\" was changed by someone else since you opened this dialog. Reload to see the latest data.");
        }
    }

    public boolean nameExists(String name, Long courseOfferingId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (courseOfferingId == null || trimmed.isEmpty()) return false;
        if (excludeId != null) {
            return batchRepository.existsByCourseOfferingIdAndNameIgnoreCaseAndIdNot(courseOfferingId, trimmed, excludeId);
        }
        return batchRepository.existsByCourseOfferingIdAndNameIgnoreCase(courseOfferingId, trimmed);
    }

    /** A genuine DELETE, not a soft-flag flip -- hard-blocked whenever anything is still attached
     *  (student roster, timetable/rotation/session data), so by the time this runs the batch is
     *  guaranteed to carry zero history worth keeping. That guarantee is exactly what makes a real
     *  delete safe here: nothing downstream (Manage Batches, pickers, reports) can ever need to see
     *  this row again, so there's no reason to keep a soft-deactivated husk around -- unlike the
     *  automatic deactivation a reverted Cohort Room Allocation performs directly on its own
     *  batches, which is a separate path this method has no bearing on and which can legitimately
     *  leave real history behind. */
    @Transactional
    public void deleteBatch(Long id) {
        Batch batch = getOrThrow(id);
        BatchLifecycleImpactDto impact = computeLifecycleImpact(batch);
        if (impact.hasAny()) {
            throw new IllegalStateException(
                "Cannot delete '" + batch.getName() + "' — it still has " + describeImpact(impact)
                    + ". Remove them first.");
        }
        batchRepository.delete(batch);
    }

    public BatchLifecycleImpactDto getLifecycleImpact(Long id) {
        return computeLifecycleImpact(getOrThrow(id));
    }

    private BatchLifecycleImpactDto computeLifecycleImpact(Batch batch) {
        Long id = batch.getId();
        return new BatchLifecycleImpactDto(
            batchRepository.countStudents(id),
            classScheduleRepository.countByBatchIdAndIsActiveTrue(id),
            rotationMemberAssignmentRepository.countByBatchId(id),
            escortRotationAssignmentRepository.countByBatchId(id),
            sessionOccurrenceRepository.countByBatch_IdAndOccurrenceStatusNot(id, OccurrenceStatus.CANCELLED)
        );
    }

    private String describeImpact(BatchLifecycleImpactDto impact) {
        List<String> parts = new ArrayList<>();
        if (impact.enrolledStudents() > 0) parts.add(impact.enrolledStudents() + " enrolled student(s)");
        if (impact.classScheduleCount() > 0) parts.add(impact.classScheduleCount() + " timetable slot(s)");
        if (impact.rotationAssignmentCount() > 0) parts.add(impact.rotationAssignmentCount() + " rotation assignment(s)");
        if (impact.escortAssignmentCount() > 0) parts.add(impact.escortAssignmentCount() + " escort assignment(s)");
        if (impact.sessionOccurrenceCount() > 0) parts.add(impact.sessionOccurrenceCount() + " scheduled session(s)");
        return String.join(", ", parts);
    }

    /** Active batches only — a reverted {@link com.cms.model.CohortRoomAllocation} leaves its
     *  batches behind deactivated (never deleted, for roster-history reasons — that automatic path
     *  is unrelated to {@link #deleteBatch}, which is a real delete gated on there being no history
     *  at all) rather than reactivating on a later re-commit ({@code createVentureBatch} always
     *  inserts a fresh row), so a cohort that's had its room allocation reverted and recommitted a
     *  few times accumulates several stale, inactive, same-named rows. Every consumer of this list
     *  (Assign Faculty, Clinical Shift Group batch-linking, Escort Rotation setup, Lab Schedule) is
     *  a picker that only ever wants the live ones — nothing in the product surfaces inactive
     *  batches again once they're deactivated. */
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
        CohortSection section = b.getCohortSection();
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
            section != null ? section.getId() : null,
            section != null ? section.getSectionLabel() : null,
            b.getIsActive(),
            b.getVersion(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
    }
}
