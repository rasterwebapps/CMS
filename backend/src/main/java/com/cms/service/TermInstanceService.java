package com.cms.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.TermInstanceDto;
import com.cms.dto.TermInstanceUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Student;
import com.cms.model.TermInstance;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class TermInstanceService {

    private final TermInstanceRepository termInstanceRepository;
    private final AcademicYearRepository academicYearRepository;

    // Field injection with @Lazy breaks the circular dependency:
    // TermInstanceService -> StudentTermEnrollmentService -> TermInstanceRepository
    @Autowired
    @Lazy
    private StudentTermEnrollmentService studentTermEnrollmentService;

    @Autowired
    @Lazy
    private CourseOfferingService courseOfferingService;

    @Autowired
    @Lazy
    private CourseRegistrationService courseRegistrationService;

    @Autowired
    @Lazy
    private FeeDemandService feeDemandService;

    public TermInstanceService(TermInstanceRepository termInstanceRepository,
                                AcademicYearRepository academicYearRepository) {
        this.termInstanceRepository = termInstanceRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public void createTermInstancesForAcademicYear(AcademicYear academicYear) {
        int startYear = academicYear.getStartDate().getYear();

        TermInstance odd = new TermInstance(
            academicYear,
            TermType.ODD,
            LocalDate.of(startYear, 6, 1),
            LocalDate.of(startYear, 11, 30),
            TermInstanceStatus.PLANNED
        );

        TermInstance even = new TermInstance(
            academicYear,
            TermType.EVEN,
            LocalDate.of(startYear, 12, 1),
            LocalDate.of(startYear + 1, 5, 31),
            TermInstanceStatus.PLANNED
        );

        termInstanceRepository.save(odd);
        termInstanceRepository.save(even);
    }

    /**
     * Returns the calendar year fee collection should anchor to for a student: their cohort's
     * admission academic year, or the current calendar year when no cohort is assigned.
     * Mirrors the anchor FeeFinalizationService already uses when generating SemesterFee due dates.
     */
    public int resolveJoiningStartYear(Student student) {
        return student.getCohort() != null
            ? student.getCohort().getAdmissionAcademicYear().getStartYear()
            : LocalDate.now().getYear();
    }

    /**
     * Returns whether a student's installment (program yearNumber + semesterSequence, where
     * 1 = ODD term, 2 = EVEN term) is open for collection right now — i.e. its TermInstance has
     * been opened (or already locked) by an admin, not merely PLANNED.
     */
    public boolean isSemesterFeeCollectibleNow(int joiningStartYear, int yearNumber, Integer semesterSequence) {
        int targetStartYear = joiningStartYear + (yearNumber - 1);
        TermType termType = semesterSequence != null && semesterSequence == 2 ? TermType.EVEN : TermType.ODD;
        return isTermCollectibleNow(targetStartYear, termType);
    }

    /**
     * Returns whether the term instance for the given calendar start year + term type has been
     * opened for collection (status OPEN or LOCKED) rather than still PLANNED. An academic year
     * or term instance that isn't configured yet is treated as not collectible — it's a future term.
     */
    public boolean isTermCollectibleNow(int targetStartYear, TermType termType) {
        return academicYearRepository.findByNameStartingWith(String.valueOf(targetStartYear))
            .flatMap(ay -> termInstanceRepository.findByAcademicYearIdAndTermType(ay.getId(), termType))
            .map(ti -> ti.getStatus() != TermInstanceStatus.PLANNED)
            .orElse(false);
    }

    public List<TermInstanceDto> getTermInstancesByAcademicYear(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return termInstanceRepository.findByAcademicYearId(academicYearId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public TermInstanceDto getById(Long id) {
        TermInstance instance = termInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + id));
        return toDto(instance);
    }

    @Transactional
    public TermInstanceDto updateTermInstance(Long id, TermInstanceUpdateRequest request) {
        TermInstance instance = termInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + id));

        if (request.startDate() != null) {
            instance.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            instance.setEndDate(request.endDate());
        }
        if (request.status() != null) {
            validateStatusTransition(instance.getStatus(), request.status());
            instance.setStatus(request.status());
        }

        TermInstance saved = termInstanceRepository.save(instance);
        if (request.status() != null && request.status() == TermInstanceStatus.OPEN) {
            studentTermEnrollmentService.generateEnrollmentsForTermInstance(id);
            courseOfferingService.generateOfferingsForTermInstance(id);
            courseRegistrationService.generateRegistrationsForTermInstance(id);
            feeDemandService.generateDemandsForTermInstance(id);
        }
        if (request.status() != null && request.status() == TermInstanceStatus.LOCKED) {
            courseOfferingService.deactivateAllOfferingsForTermInstance(id);
        }
        return toDto(saved);
    }

    private void validateStatusTransition(TermInstanceStatus current, TermInstanceStatus next) {
        boolean valid = switch (current) {
            case PLANNED -> next == TermInstanceStatus.OPEN;
            case OPEN -> next == TermInstanceStatus.LOCKED;
            case LOCKED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                "Invalid status transition from " + current + " to " + next +
                ". Allowed: PLANNED → OPEN → LOCKED (no backward transitions)");
        }
    }

    private TermInstanceDto toDto(TermInstance ti) {
        return new TermInstanceDto(
            ti.getId(),
            ti.getAcademicYear().getId(),
            ti.getAcademicYear().getName(),
            ti.getTermType(),
            ti.getStartDate(),
            ti.getEndDate(),
            ti.getStatus(),
            ti.getCreatedAt(),
            ti.getUpdatedAt()
        );
    }

    /** Package-private setter for test injection of the lazy-wired service. */
    void setStudentTermEnrollmentService(StudentTermEnrollmentService studentTermEnrollmentService) {
        this.studentTermEnrollmentService = studentTermEnrollmentService;
    }

    /** Package-private setter for test injection of the lazy-wired service. */
    void setCourseOfferingService(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    /** Package-private setter for test injection of the lazy-wired service. */
    void setCourseRegistrationService(CourseRegistrationService courseRegistrationService) {
        this.courseRegistrationService = courseRegistrationService;
    }

    /** Package-private setter for test injection of the lazy-wired service. */
    void setFeeDemandService(FeeDemandService feeDemandService) {
        this.feeDemandService = feeDemandService;
    }
}
