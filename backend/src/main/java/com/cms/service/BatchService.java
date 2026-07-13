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
    public BatchDto createBatch(BatchRequest request) {
        CourseOffering offering = courseOfferingRepository.findById(request.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Course offering not found with id: " + request.courseOfferingId()));

        if (batchRepository.existsByCourseOfferingIdAndName(offering.getId(), request.name())) {
            throw new IllegalArgumentException(
                "A batch named '" + request.name() + "' already exists for this course offering");
        }

        Batch batch = new Batch(offering, request.name(), request.capacity(), offering.getTermInstance());
        applyCoordinator(batch, request.coordinatorFacultyId());

        return toDto(batchRepository.save(batch));
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

    public List<BatchDto> getBatchesForOffering(Long courseOfferingId) {
        return batchRepository.findByCourseOfferingId(courseOfferingId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * Alternate lookup for callers (e.g. the Lab Schedule form) that only know a subject +
     * term instance, not the concrete CourseOffering id — resolves whichever offering(s) match
     * and returns their batches combined.
     */
    public List<BatchDto> getBatchesForSubjectAndTerm(Long subjectId, Long termInstanceId) {
        return courseOfferingRepository.findByTermInstanceIdAndSubjectId(termInstanceId, subjectId).stream()
            .flatMap(o -> batchRepository.findByCourseOfferingId(o.getId()).stream())
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

    private void applyCoordinator(Batch batch, Long coordinatorFacultyId) {
        if (coordinatorFacultyId == null) {
            batch.setCoordinatorFaculty(null);
            return;
        }
        Faculty faculty = facultyRepository.findById(coordinatorFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Faculty not found with id: " + coordinatorFacultyId));
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
            b.getIsActive(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
    }
}
