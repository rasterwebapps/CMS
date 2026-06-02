package com.cms.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CohortLapsedSummaryResponse;
import com.cms.dto.CohortLapsedSummaryResponse.CohortLapsedRow;
import com.cms.dto.CohortQuotaStatusRequest;
import com.cms.dto.CohortSeatsRequest;
import com.cms.dto.CohortSummaryResponse;
import com.cms.dto.SeatAvailabilityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.enums.AdmissionCategory;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.StudentRepository;

@RestController
@RequestMapping("/cohorts")
public class CohortController {

    private final CohortRepository       cohortRepository;
    private final CourseRepository        courseRepository;
    private final AcademicYearRepository  academicYearRepository;
    private final StudentRepository       studentRepository;

    public CohortController(CohortRepository cohortRepository,
                            CourseRepository courseRepository,
                            AcademicYearRepository academicYearRepository,
                            StudentRepository studentRepository) {
        this.cohortRepository      = cohortRepository;
        this.courseRepository      = courseRepository;
        this.academicYearRepository = academicYearRepository;
        this.studentRepository     = studentRepository;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('ACADEMIC_YEAR_MANAGE','STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<CohortSummaryResponse>> getCohorts(
            @RequestParam(required = false) Long academicYearId) {
        List<Cohort> cohorts = academicYearId != null
            ? cohortRepository.findByAdmissionAcademicYearId(academicYearId)
            : cohortRepository.findAll();
        return ResponseEntity.ok(cohorts.stream().map(this::toResponse).toList());
    }

    /** Preflight seat-availability check used by the admission conversion UI. */
    @GetMapping("/seat-availability")
    @PreAuthorize("@perm.hasAny('ENQUIRY_MANAGE','STUDENT_MANAGE','ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<SeatAvailabilityResponse> seatAvailability(
            @RequestParam Long courseId,
            @RequestParam Long academicYearId,
            @RequestParam AdmissionQuota quota) {
        Cohort cohort = cohortRepository
            .findByCourseIdAndAdmissionAcademicYearId(courseId, academicYearId)
            .orElse(null);
        if (cohort == null) {
            return ResponseEntity.ok(new SeatAvailabilityResponse(true, 0, null, false, false, false));
        }

        AdmissionCategory cat = AdmissionCategory.valueOf(quota.name());
        long filledQuota = studentRepository.countByCohortIdAndAdmissionCategory(cohort.getId(), cat);

        if (quota == AdmissionQuota.COUNSELLING) {
            Integer total = cohort.getCounsellingSeats();
            boolean closed = cohort.isCounsellingClosed();
            boolean full   = total != null && filledQuota >= total;
            return ResponseEntity.ok(new SeatAvailabilityResponse(!full && !closed, filledQuota, total, full, closed, false));
        }

        // MANAGEMENT: locked → hard block; total exhausted → hard block; over allocation → soft warning
        if (cohort.isManagementClosed()) {
            return ResponseEntity.ok(
                new SeatAvailabilityResponse(false, filledQuota, cohort.getManagementSeats(), false, true, false));
        }
        long filledCounselling = studentRepository.countByCohortIdAndAdmissionCategory(
            cohort.getId(), AdmissionCategory.COUNSELLING);
        Integer totalSeats = cohort.getTotalSeats();
        if (totalSeats != null && (filledQuota + filledCounselling) >= totalSeats) {
            return ResponseEntity.ok(
                new SeatAvailabilityResponse(false, filledQuota, cohort.getManagementSeats(), true, false, false));
        }
        boolean overQuota = cohort.getManagementSeats() != null && filledQuota >= cohort.getManagementSeats();
        return ResponseEntity.ok(
            new SeatAvailabilityResponse(true, filledQuota, cohort.getManagementSeats(), false, false, overQuota));
    }

    /** Per-cohort govt lapsed seat breakdown for the current academic year. */
    @GetMapping("/lapsed-summary")
    @PreAuthorize("@perm.hasAny('ACADEMIC_YEAR_MANAGE','STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<CohortLapsedSummaryResponse> lapsedSummary(
            @RequestParam(required = false) Long academicYearId) {
        AcademicYear ay;
        if (academicYearId != null) {
            ay = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearId));
        } else {
            ay = academicYearRepository.findByIsCurrentTrue().orElse(null);
            if (ay == null) {
                return ResponseEntity.ok(
                    new CohortLapsedSummaryResponse(List.of(), 0, 0, 0, 0.0));
            }
        }

        List<Cohort> cohorts = cohortRepository.findByAdmissionAcademicYearId(ay.getId());
        long totalCounselling = 0, totalFilled = 0, totalLapsed = 0;

        List<CohortLapsedRow> rows = cohorts.stream().map(c -> {
            long seats  = c.getCounsellingSeats() != null ? c.getCounsellingSeats() : 0L;
            long filled = studentRepository.countByCohortIdAndAdmissionCategory(c.getId(), AdmissionCategory.COUNSELLING);
            long lapsed = c.isCounsellingClosed() ? Math.max(0L, seats - filled) : 0L;
            String courseName = c.getCourse() != null ? c.getCourse().getName() : "—";
            String courseCode = c.getCourse() != null ? c.getCourse().getCode() : "—";
            return new CohortLapsedRow(c.getId(), courseName, courseCode, seats, filled, lapsed, c.isCounsellingClosed());
        }).toList();

        for (CohortLapsedRow r : rows) {
            totalCounselling += r.counsellingSeats();
            totalFilled      += r.filledCounselling();
            totalLapsed      += r.lapsedSeats();
        }
        double pct = totalCounselling > 0 ? (double) totalLapsed / totalCounselling * 100.0 : 0.0;

        return ResponseEntity.ok(
            new CohortLapsedSummaryResponse(rows, totalCounselling, totalFilled, totalLapsed, pct));
    }

    @PostMapping("/initialize")
    @PreAuthorize("@perm.has('ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<List<CohortSummaryResponse>> initializeCohorts(
            @RequestParam Long academicYearId) {
        AcademicYear ay = academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearId));
        int startYear = ay.getStartYear();

        List<Course> activeCourses = courseRepository.findAll().stream()
            .filter(c -> c.getProgram() != null && c.getProgram().getStatus() == ProgramStatus.ACTIVE)
            .toList();

        for (Course course : activeCourses) {
            cohortRepository.findByCourseIdAndAdmissionAcademicYearId(course.getId(), academicYearId)
                .orElseGet(() -> {
                    int durationYears = course.getProgram().getDurationYears();
                    int endYear = startYear + durationYears;
                    String code = course.getCode() + "-" + startYear + "-" + endYear;
                    String name = course.getName() + " (" + startYear + "-" + endYear + ")";
                    AcademicYear gradAY = academicYearRepository
                        .findByName(endYear + "-" + (endYear + 1))
                        .orElse(null);
                    Cohort c = new Cohort();
                    c.setCourse(course);
                    c.setAdmissionAcademicYear(ay);
                    c.setExpectedGraduationAcademicYear(gradAY);
                    c.setCohortCode(code);
                    c.setDisplayName(name);
                    c.setStatus(CohortStatus.ACTIVE);
                    return cohortRepository.save(c);
                });
        }
        List<Cohort> result = cohortRepository.findByAdmissionAcademicYearId(academicYearId);
        return ResponseEntity.ok(result.stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{id}/seats")
    @PreAuthorize("@perm.has('ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<CohortSummaryResponse> updateSeats(
            @PathVariable Long id,
            @RequestBody CohortSeatsRequest request) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found: " + id));

        Integer total = request.totalSeats();
        BigDecimal pct = request.managementPercentage();
        if (total != null && pct != null) {
            int mgmt = pct.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
            cohort.setTotalSeats(total);
            cohort.setManagementPercentage(pct);
            cohort.setManagementSeats(mgmt);
            cohort.setCounsellingSeats(total - mgmt);
        }
        return ResponseEntity.ok(toResponse(cohortRepository.save(cohort)));
    }

    @PatchMapping("/{id}/quota-status")
    @PreAuthorize("@perm.has('ACADEMIC_YEAR_MANAGE')")
    public ResponseEntity<CohortSummaryResponse> updateQuotaStatus(
            @PathVariable Long id,
            @RequestBody CohortQuotaStatusRequest request) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found: " + id));
        LocalDate today = request.closed() ? LocalDate.now() : null;
        if (request.quota() == AdmissionQuota.COUNSELLING) {
            cohort.setCounsellingClosed(request.closed());
            cohort.setCounsellingClosedDate(today);
        } else {
            cohort.setManagementClosed(request.closed());
            cohort.setManagementClosedDate(today);
        }
        return ResponseEntity.ok(toResponse(cohortRepository.save(cohort)));
    }

    private CohortSummaryResponse toResponse(Cohort c) {
        String courseName = c.getCourse() != null ? c.getCourse().getName() : "—";
        String courseCode = c.getCourse() != null ? c.getCourse().getCode() : "—";
        return new CohortSummaryResponse(
            c.getId(), c.getCohortCode(), c.getDisplayName(),
            courseName, courseCode,
            c.getTotalSeats(), c.getManagementPercentage(),
            c.getManagementSeats(), c.getCounsellingSeats(),
            studentRepository.existsByCohortId(c.getId()),
            c.isCounsellingClosed(), c.getCounsellingClosedDate(),
            c.isManagementClosed(), c.getManagementClosedDate()
        );
    }
}
