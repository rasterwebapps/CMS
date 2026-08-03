package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearFullUpdateRequest;
import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CohortSeatAllocationRequest;
import com.cms.dto.TermBillingScheduleRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class AcademicYearService {

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");

    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureGroupRepository feeStructureGroupRepository;
    private final TermInstanceService termInstanceService;
    private final TermInstanceRepository termInstanceRepository;
    private final TermBillingScheduleService termBillingScheduleService;
    private final CohortRepository cohortRepository;
    private final CourseRepository courseRepository;
    private final HolidayTemplateSeedingService holidayTemplateSeedingService;

    public AcademicYearService(AcademicYearRepository academicYearRepository,
                               FeeStructureGroupRepository feeStructureGroupRepository,
                               TermInstanceService termInstanceService,
                               TermInstanceRepository termInstanceRepository,
                               TermBillingScheduleService termBillingScheduleService,
                               CohortRepository cohortRepository,
                               CourseRepository courseRepository,
                               HolidayTemplateSeedingService holidayTemplateSeedingService) {
        this.academicYearRepository = academicYearRepository;
        this.feeStructureGroupRepository = feeStructureGroupRepository;
        this.termInstanceService = termInstanceService;
        this.termInstanceRepository = termInstanceRepository;
        this.termBillingScheduleService = termBillingScheduleService;
        this.cohortRepository = cohortRepository;
        this.courseRepository = courseRepository;
        this.holidayTemplateSeedingService = holidayTemplateSeedingService;
    }

    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request) {
        String name = requireTrimmed(request.name(), "Academic year name is required");
        validateDateRange(request, null);
        validateNameFormat(name);

        if (request.endDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Academic year end date cannot be in the past");
        }

        if (academicYearRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "An academic year with the name '" + name + "' already exists");
        }

        // The very first academic year ever created must be current — there would be no other
        // row to promote, so leaving it not-current would mean zero current years.
        boolean isFirstEver = academicYearRepository.count() == 0;
        Boolean isCurrent = isFirstEver ? Boolean.TRUE
            : (request.isCurrent() != null ? request.isCurrent() : false);

        if (Boolean.TRUE.equals(isCurrent)) {
            academicYearRepository.clearCurrentAcademicYear();
        }

        AcademicYear academicYear = new AcademicYear(
            name,
            request.startDate(),
            request.endDate(),
            isCurrent
        );
        AcademicYear saved = academicYearRepository.save(academicYear);
        termInstanceService.createTermInstancesForAcademicYear(saved);
        createCohortsWithSeats(saved, request.cohortSeatAllocations());
        holidayTemplateSeedingService.seedForAcademicYear(saved);
        return toResponse(saved);
    }

    public List<AcademicYearResponse> findAll() {
        return academicYearRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<AcademicYearResponse> findPage(String search, Boolean isCurrent, Pageable pageable) {
        Specification<AcademicYear> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), pattern));
        }
        if (isCurrent != null) {
            Boolean isCurrentFinal = isCurrent;
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("isCurrent"), isCurrentFinal));
        }
        return academicYearRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public AcademicYearResponse findById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        return toResponse(academicYear);
    }

    public AcademicYearResponse findCurrent() {
        AcademicYear academicYear = academicYearRepository.findByIsCurrentTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No current academic year found"));
        return toResponse(academicYear);
    }

    @Transactional
    public AcademicYearResponse update(Long id, AcademicYearRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        String name = requireTrimmed(request.name(), "Academic year name is required");

        validateDateRange(request, id);
        validateNameFormat(name);
        validateTermInstancesFitWithinBounds(id, request.startDate(), request.endDate());

        if (academicYearRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "An academic year with the name '" + name + "' already exists");
        }

        Boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : false;

        // The only way to remove current status from a year is to mark a different year
        // current instead (which clears this one via clearCurrentAcademicYear() below) —
        // un-checking it directly would leave zero current years.
        if (Boolean.TRUE.equals(academicYear.getIsCurrent()) && !Boolean.TRUE.equals(isCurrent)) {
            throw new IllegalStateException(
                "Cannot remove current-year status — mark a different academic year as current instead.");
        }

        if (Boolean.TRUE.equals(isCurrent) && !Boolean.TRUE.equals(academicYear.getIsCurrent())) {
            academicYearRepository.clearCurrentAcademicYear();
        }

        academicYear.setName(name);
        academicYear.setStartDate(request.startDate());
        academicYear.setEndDate(request.endDate());
        academicYear.setIsCurrent(isCurrent);

        AcademicYear updated = academicYearRepository.save(academicYear);
        return toResponse(updated);
    }

    /**
     * Updates the academic year's own dates together with both term instances' dates and billing
     * details in one transaction, validating the complete combined target state up front.
     * <p>
     * {@link #update} and {@link TermInstanceService#updateTermInstance} each validate their own
     * dates against the *other*'s still-persisted value — fine independently, but sequencing them
     * as separate calls deadlocks when an admin shrinks (or widens) the academic year and its term
     * together in one save: whichever call runs first gets rejected because the other side hasn't
     * been narrowed (or widened) yet. Validating every date against the full incoming request
     * before writing anything sidesteps that ordering problem entirely.
     */
    @Transactional
    public AcademicYearResponse updateFull(Long id, AcademicYearFullUpdateRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        String name = requireTrimmed(request.name(), "Academic year name is required");

        AcademicYearRequest ayFields = new AcademicYearRequest(name, request.startDate(), request.endDate(), request.isCurrent());
        validateDateRange(ayFields, id);
        validateNameFormat(name);

        if (academicYearRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "An academic year with the name '" + name + "' already exists");
        }

        Boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : false;
        if (Boolean.TRUE.equals(academicYear.getIsCurrent()) && !Boolean.TRUE.equals(isCurrent)) {
            throw new IllegalStateException(
                "Cannot remove current-year status — mark a different academic year as current instead.");
        }

        TermInstance odd = termInstanceRepository.findByAcademicYearIdAndTermType(id, TermType.ODD)
            .orElseThrow(() -> new ResourceNotFoundException("ODD term instance not found for academic year " + id));
        TermInstance even = termInstanceRepository.findByAcademicYearIdAndTermType(id, TermType.EVEN)
            .orElseThrow(() -> new ResourceNotFoundException("EVEN term instance not found for academic year " + id));

        // Validate the combined target state (new AY bounds + new term bounds together) rather
        // than each piece against the other's not-yet-updated, still-persisted value.
        termInstanceService.assertTermWithinAcademicYear(
            request.oddTerm().startDate(), request.oddTerm().endDate(), request.startDate(), request.endDate());
        termInstanceService.assertTermWithinAcademicYear(
            request.evenTerm().startDate(), request.evenTerm().endDate(), request.startDate(), request.endDate());
        termInstanceService.assertTermsDoNotOverlap(
            request.oddTerm().startDate(), request.oddTerm().endDate(),
            request.evenTerm().startDate(), request.evenTerm().endDate(), TermType.EVEN.toString());

        // All validation passed — apply and save. Term instances are saved before the billing
        // upsert below so its own due-date-vs-term-bounds check sees the new term dates.
        if (Boolean.TRUE.equals(isCurrent) && !Boolean.TRUE.equals(academicYear.getIsCurrent())) {
            academicYearRepository.clearCurrentAcademicYear();
        }
        academicYear.setName(name);
        academicYear.setStartDate(request.startDate());
        academicYear.setEndDate(request.endDate());
        academicYear.setIsCurrent(isCurrent);
        AcademicYear savedAcademicYear = academicYearRepository.save(academicYear);

        odd.setStartDate(request.oddTerm().startDate());
        odd.setEndDate(request.oddTerm().endDate());
        termInstanceRepository.save(odd);

        even.setStartDate(request.evenTerm().startDate());
        even.setEndDate(request.evenTerm().endDate());
        termInstanceRepository.save(even);

        termBillingScheduleService.createOrUpdate(new TermBillingScheduleRequest(
            id, TermType.ODD, request.oddBilling().dueDate(), request.oddBilling().lateFeeType(),
            request.oddBilling().lateFeeAmount(), request.oddBilling().graceDays()));
        termBillingScheduleService.createOrUpdate(new TermBillingScheduleRequest(
            id, TermType.EVEN, request.evenBilling().dueDate(), request.evenBilling().lateFeeType(),
            request.evenBilling().lateFeeAmount(), request.evenBilling().graceDays()));

        return toResponse(savedAcademicYear);
    }

    @Transactional
    public void delete(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
        if (Boolean.TRUE.equals(academicYear.getIsCurrent())) {
            throw new IllegalStateException(
                "Cannot delete the current academic year. Mark a different year as current first.");
        }
        if (feeStructureGroupRepository.existsByAcademicYearId(id)) {
            throw new IllegalStateException(
                "Cannot delete academic year because fee structures are associated with it.");
        }
        academicYearRepository.deleteById(id);
    }

    private void validateDateRange(AcademicYearRequest request, Long excludeId) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (academicYearRepository.existsOverlapping(request.startDate(), request.endDate(), excludeId)) {
            throw new IllegalArgumentException(
                "Dates overlap with an existing academic year");
        }
    }

    /**
     * Existing term instances are never resized automatically when their parent academic year's
     * dates change — an admin must adjust them deliberately. So shrinking the academic year to no
     * longer contain an existing term instance's dates would silently orphan it; block that instead.
     */
    private void validateTermInstancesFitWithinBounds(Long academicYearId, LocalDate newStart, LocalDate newEnd) {
        for (var term : termInstanceService.getTermInstancesByAcademicYear(academicYearId)) {
            if (term.startDate().isBefore(newStart) || term.endDate().isAfter(newEnd)) {
                throw new IllegalArgumentException(
                    "Cannot update academic year dates — the " + term.termType() + " term ("
                        + term.startDate() + " to " + term.endDate() + ") would fall outside the new range ("
                        + newStart + " to " + newEnd + "). Adjust the term instance dates first.");
            }
        }
    }

    private void validateNameFormat(String name) {
        if (name == null) return;
        if (!YEAR_RANGE_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "Academic year name must be in YYYY-YYYY format (e.g. 2026-2027)");
        }
        String[] parts = name.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = Integer.parseInt(parts[1]);
        if (endYear != startYear + 1) {
            throw new IllegalArgumentException(
                "Academic year end year must be exactly one year after start year (e.g. 2026-2027)");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }

    private void createCohortsWithSeats(AcademicYear academicYear,
                                        List<CohortSeatAllocationRequest> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return;
        }

        Set<Long> duplicateCourseIds = allocations.stream()
            .collect(Collectors.groupingBy(CohortSeatAllocationRequest::courseId, Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (!duplicateCourseIds.isEmpty()) {
            throw new IllegalArgumentException(
                "Duplicate seat allocation found for course id(s): " + duplicateCourseIds);
        }

        Map<Long, CohortSeatAllocationRequest> allocationsByCourseId = allocations.stream()
            .collect(Collectors.toMap(CohortSeatAllocationRequest::courseId, Function.identity()));

        for (Long courseId : allocationsByCourseId.keySet()) {
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            if (course.getProgram() != null && course.getProgram().getStatus() != ProgramStatus.ACTIVE) {
                throw new IllegalArgumentException(
                    "Seats can only be allocated for active programs — '" + course.getProgram().getName()
                        + "' is not active");
            }
            CohortSeatAllocationRequest allocation = allocationsByCourseId.get(courseId);
            Cohort cohort = buildCohort(course, academicYear);
            int total = allocation.totalSeats() != null ? allocation.totalSeats() : 0;
            BigDecimal pct = allocation.managementPercentage() != null
                ? allocation.managementPercentage() : BigDecimal.ZERO;
            int mgmt = pct.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
            cohort.setTotalSeats(total);
            cohort.setManagementPercentage(pct);
            cohort.setManagementSeats(mgmt);
            cohort.setCounsellingSeats(total - mgmt);
            cohortRepository.save(cohort);
        }
    }

    private Cohort buildCohort(Course course, AcademicYear admissionAcademicYear) {
        int startYear = admissionAcademicYear.getStartYear();
        int durationYears = course.getProgram() != null ? course.getProgram().getDurationYears() : 0;
        int endYear = startYear + durationYears;

        Cohort cohort = new Cohort();
        cohort.setCourse(course);
        cohort.setAdmissionAcademicYear(admissionAcademicYear);
        cohort.setExpectedGraduationAcademicYear(
            academicYearRepository.findByName(endYear + "-" + (endYear + 1)).orElse(null));
        cohort.setCohortCode(course.getCode() + "-" + startYear + "-" + endYear);
        cohort.setDisplayName(course.getName() + " (" + startYear + "-" + endYear + ")");
        cohort.setStatus(CohortStatus.ACTIVE);
        return cohort;
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return academicYearRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return academicYearRepository.existsByNameIgnoreCase(trimmed);
    }

    private AcademicYearResponse toResponse(AcademicYear academicYear) {
        return new AcademicYearResponse(
            academicYear.getId(),
            academicYear.getName(),
            academicYear.getStartDate(),
            academicYear.getEndDate(),
            academicYear.getIsCurrent(),
            academicYear.getCreatedAt(),
            academicYear.getUpdatedAt()
        );
    }
}
