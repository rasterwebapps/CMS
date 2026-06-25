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
        // Derived from the academic year's own chosen dates rather than a fixed calendar
        // pattern — this college's academic year start month varies (follows the NEET/
        // counselling calendar), so a hardcoded June/December split would be wrong whenever
        // a year doesn't start in June.
        LocalDate start = academicYear.getStartDate();
        LocalDate end = academicYear.getEndDate();
        LocalDate oddEnd = start.plusMonths(6).minusDays(1);
        LocalDate evenStart = start.plusMonths(6);

        TermInstance odd = new TermInstance(
            academicYear,
            TermType.ODD,
            start,
            oddEnd,
            TermInstanceStatus.PLANNED
        );

        TermInstance even = new TermInstance(
            academicYear,
            TermType.EVEN,
            evenStart,
            end,
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

        if (request.startDate() != null || request.endDate() != null) {
            LocalDate newStart = request.startDate() != null ? request.startDate() : instance.getStartDate();
            LocalDate newEnd = request.endDate() != null ? request.endDate() : instance.getEndDate();
            validateTermDates(instance, newStart, newEnd);
            instance.setStartDate(newStart);
            instance.setEndDate(newEnd);
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

    /**
     * Term dates may equal the academic year's own start/end (inclusive) but must not exceed
     * them, and must not overlap the sibling term (ODD vs EVEN) — gaps between the two are fine,
     * since this college's term boundaries aren't always contiguous.
     */
    private void validateTermDates(TermInstance instance, LocalDate start, LocalDate end) {
        AcademicYear academicYear = instance.getAcademicYear();
        assertTermWithinAcademicYear(start, end, academicYear.getStartDate(), academicYear.getEndDate());

        TermType siblingType = instance.getTermType() == TermType.ODD ? TermType.EVEN : TermType.ODD;
        termInstanceRepository.findByAcademicYearIdAndTermType(academicYear.getId(), siblingType)
            .ifPresent(sibling -> assertTermsDoNotOverlap(
                start, end, sibling.getStartDate(), sibling.getEndDate(), siblingType.toString()));
    }

    /**
     * Pure date-rule check shared with AcademicYearService.updateFull(), which validates a new
     * academic year's bounds together with its terms' new bounds in one combined pass rather than
     * checking each side against the other's still-persisted (not-yet-updated) value — sequencing
     * those as separate calls created a chicken-and-egg deadlock when shrinking/widening both at
     * once (e.g. the academic year's own shrink guard would reject the new dates because the term
     * hadn't been narrowed yet, and vice versa for the term's own bounds check).
     */
    void assertTermWithinAcademicYear(LocalDate termStart, LocalDate termEnd, LocalDate ayStart, LocalDate ayEnd) {
        if (!termEnd.isAfter(termStart)) {
            throw new IllegalArgumentException("Term end date must be after start date");
        }
        if (termStart.isBefore(ayStart) || termEnd.isAfter(ayEnd)) {
            throw new IllegalArgumentException(
                "Term dates must fall within the academic year's dates (" + ayStart + " to " + ayEnd + ")");
        }
    }

    /** Pure date-rule check shared with AcademicYearService.updateFull() — see assertTermWithinAcademicYear. */
    void assertTermsDoNotOverlap(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd, String bLabel) {
        boolean overlaps = !aStart.isAfter(bEnd) && !bStart.isAfter(aEnd);
        if (overlaps) {
            throw new IllegalArgumentException(
                "Term dates overlap with the " + bLabel + " term (" + bStart + " to " + bEnd + ")");
        }
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
