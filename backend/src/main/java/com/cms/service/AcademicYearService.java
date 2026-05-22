package com.cms.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CohortSeatAllocationRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.enums.CohortStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureGroupRepository;

@Service
@Transactional(readOnly = true)
public class AcademicYearService {

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");

    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureGroupRepository feeStructureGroupRepository;
    private final TermInstanceService termInstanceService;
    private final CohortRepository cohortRepository;
    private final CourseRepository courseRepository;

    public AcademicYearService(AcademicYearRepository academicYearRepository,
                               FeeStructureGroupRepository feeStructureGroupRepository,
                               TermInstanceService termInstanceService,
                               CohortRepository cohortRepository,
                               CourseRepository courseRepository) {
        this.academicYearRepository = academicYearRepository;
        this.feeStructureGroupRepository = feeStructureGroupRepository;
        this.termInstanceService = termInstanceService;
        this.cohortRepository = cohortRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request) {
        String name = requireTrimmed(request.name(), "Academic year name is required");
        validateDateRange(request);
        validateNameFormat(name);

        if (academicYearRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "An academic year with the name '" + name + "' already exists");
        }

        Boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : false;

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
        return toResponse(saved);
    }

    public List<AcademicYearResponse> findAll() {
        return academicYearRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
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

        validateDateRange(request);
        validateNameFormat(name);

        if (academicYearRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "An academic year with the name '" + name + "' already exists");
        }

        Boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : false;

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

    @Transactional
    public void delete(Long id) {
        if (!academicYearRepository.existsById(id)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + id);
        }
        if (feeStructureGroupRepository.existsByAcademicYearId(id)) {
            throw new IllegalStateException(
                "Cannot delete academic year because fee structures are associated with it.");
        }
        academicYearRepository.deleteById(id);
    }

    private void validateDateRange(AcademicYearRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
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
            CohortSeatAllocationRequest allocation = allocationsByCourseId.get(courseId);
            Cohort cohort = buildCohort(course, academicYear);
            cohort.setManagementSeats(defaultSeats(allocation.managementSeats()));
            cohort.setCounsellingSeats(defaultSeats(allocation.counsellingSeats()));
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

    private static Integer defaultSeats(Integer seats) {
        return seats != null ? seats : 0;
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
