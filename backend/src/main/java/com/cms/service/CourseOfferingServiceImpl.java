package com.cms.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Faculty;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class CourseOfferingServiceImpl implements CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final CohortRepository cohortRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;
    private final FacultyRepository facultyRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final BatchRepository batchRepository;

    // Field injection with @Lazy breaks the circular dependency:
    // CourseOfferingServiceImpl -> TimetableGlobalAutoScheduleService -> CourseOfferingService
    @Autowired
    @Lazy
    private TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public CourseOfferingServiceImpl(CourseOfferingRepository courseOfferingRepository,
                                      TermInstanceRepository termInstanceRepository,
                                      CohortRepository cohortRepository,
                                      CurriculumVersionRepository curriculumVersionRepository,
                                      CurriculumSemesterCourseRepository curriculumSemesterCourseRepository,
                                      FacultyRepository facultyRepository,
                                      StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                      ClassScheduleRepository classScheduleRepository,
                                      BatchRepository batchRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRepository = cohortRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
        this.facultyRepository = facultyRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.batchRepository = batchRepository;
    }

    /** Package-private setter for test injection of the lazy-wired service. */
    void setTimetableGlobalAutoScheduleService(TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
    }

    @Override
    @Transactional
    public GenerateOfferingsResponse generateOfferingsForTermInstance(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        List<Cohort> activeCohorts = cohortRepository.findByStatus(CohortStatus.ACTIVE);
        int count = 0;
        int alreadyExisting = 0;
        int cohortsWithoutTotalTerms = 0;
        List<String> cohortsWithoutCurriculumVersion = new ArrayList<>();

        for (Cohort cohort : activeCohorts) {
            CurriculumVersion cv = resolveActiveCurriculumVersion(cohort);
            if (cv == null) {
                cohortsWithoutCurriculumVersion.add(cohort.getDisplayName());
                continue;
            }

            Integer totalSemesters = cohort.getProgram().getTotalTerms();
            if (totalSemesters == null) {
                cohortsWithoutTotalTerms++;
                continue;
            }

            // Determine relevant semester numbers for this term type and program pattern
            AssessmentPattern pattern = cohort.getProgram().getAssessmentPattern();
            Set<Integer> relevantSemesters = buildRelevantSemesters(termInstance.getTermType(), totalSemesters, pattern);

            // Load CurriculumSemesterCourses for this CV where semesterNumber is relevant
            List<CurriculumSemesterCourse> courses =
                curriculumSemesterCourseRepository.findByCurriculumVersionId(cv.getId());

            for (CurriculumSemesterCourse csc : courses) {
                if (!relevantSemesters.contains(csc.getSemesterNumber())) {
                    continue;
                }
                // Idempotent check: create only if not already existing
                Optional<CourseOffering> existing =
                    courseOfferingRepository.findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(
                        termInstanceId, cv.getId(), csc.getSubject().getId(), csc.getSemesterNumber());
                if (existing.isEmpty()) {
                    CourseOffering offering = new CourseOffering();
                    offering.setTermInstance(termInstance);
                    offering.setCurriculumVersion(cv);
                    offering.setSubject(csc.getSubject());
                    offering.setSemesterNumber(csc.getSemesterNumber());
                    offering.setCurriculumSemesterCourse(csc);
                    offering.setIsActive(true);
                    courseOfferingRepository.save(offering);
                    count++;
                } else {
                    alreadyExisting++;
                }
            }
        }
        return new GenerateOfferingsResponse(
            count, activeCohorts.size(), cohortsWithoutCurriculumVersion, cohortsWithoutTotalTerms, alreadyExisting);
    }

    /**
     * Resolves the active curriculum version scoped to this cohort's exact course (e.g. MSc
     * Nursing (Adult) vs (Child), which share one Program but need independent curricula — every
     * CurriculumVersion is now mandatorily course-scoped, so there is no program-wide fallback).
     * Among ties, the most recently created wins.
     */
    private CurriculumVersion resolveActiveCurriculumVersion(Cohort cohort) {
        Long programId = cohort.getProgram().getId();
        Long courseId = cohort.getCourse().getId();

        List<CurriculumVersion> courseScoped =
            curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(programId, courseId);
        if (courseScoped.isEmpty()) {
            return null;
        }
        return courseScoped.stream()
            .max(java.util.Comparator.comparing(CurriculumVersion::getCreatedAt))
            .orElseThrow();
    }

    @Override
    public List<Cohort> findActiveCohortsWithoutCurriculumVersion() {
        return cohortRepository.findByStatus(CohortStatus.ACTIVE).stream()
            .filter(cohort -> resolveActiveCurriculumVersion(cohort) == null)
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstance(Long termInstanceId) {
        Map<CohortSemesterKey, List<String>> cohortNamesByKey = buildCohortNamesByKey(termInstanceId);
        return courseOfferingRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .map(o -> toDto(o, cohortNamesByKey))
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstanceAndSemester(Long termInstanceId,
                                                                          Integer semesterNumber) {
        Map<CohortSemesterKey, List<String>> cohortNamesByKey = buildCohortNamesByKey(termInstanceId);
        return courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber)
            .stream()
            .map(o -> toDto(o, cohortNamesByKey))
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstanceAndCohort(Long termInstanceId, Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));
        CurriculumVersion cv = resolveActiveCurriculumVersion(cohort);
        if (cv == null) {
            return List.of();
        }
        Set<Integer> semesterNumbers = studentTermEnrollmentRepository
            .findByTermInstanceIdAndCohortId(termInstanceId, cohortId)
            .stream()
            .map(StudentTermEnrollment::getSemesterNumber)
            .collect(Collectors.toSet());
        if (semesterNumbers.isEmpty()) {
            return List.of();
        }
        Map<CohortSemesterKey, List<String>> cohortNamesByKey = buildCohortNamesByKey(termInstanceId);
        return courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSemesterNumberIn(termInstanceId, cv.getId(), semesterNumbers)
            .stream()
            .map(o -> toDto(o, cohortNamesByKey))
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstanceAndElectiveGroup(Long termInstanceId, Long electiveGroupId) {
        Map<CohortSemesterKey, List<String>> cohortNamesByKey = buildCohortNamesByKey(termInstanceId);
        return courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId)
            .stream()
            .map(o -> toDto(o, cohortNamesByKey))
            .toList();
    }

    @Override
    public CourseOfferingDto getById(Long id) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        return toDto(offering);
    }

    @Override
    @Transactional
    public CourseOfferingDto updateOffering(Long id, Long facultyId, Long secondaryFacultyId) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        requireEligibleFaculty(offering, facultyId, offering.getFacultyId());
        requireWithinCapacity(offering, facultyId);
        offering.setFacultyId(facultyId);
        // OC-127 gap-closure follow-up: secondaryFacultyId reopened from informational-only to a
        // real substitute-matching-eligible co-instructor -- same department-eligibility gate as
        // the primary, grandfathered against its own prior value independently of the primary's.
        requireEligibleFaculty(offering, secondaryFacultyId, offering.getSecondaryFacultyId());
        if (facultyId != null && facultyId.equals(secondaryFacultyId)) {
            throw new IllegalArgumentException("Primary and secondary faculty cannot be the same person");
        }
        offering.setSecondaryFacultyId(secondaryFacultyId);
        return toDto(courseOfferingRepository.save(offering));
    }

    /**
     * Department-level (Speciality) eligibility gate, mirroring {@code ClassScheduleService}'s
     * check for manually-edited sessions. Skipped when unassigning (facultyId null), when the
     * subject has no speciality set, and grandfathered when the requested faculty is already the
     * one previously on this slot — blocks new/changed mismatched assignments without
     * retroactively breaking a row saved before this rule existed on an otherwise-unrelated edit
     * (e.g. section label). Shared by both the primary and secondary faculty slots, each checked
     * against its own prior value. Delegates to {@link FacultyEligibility}, the same shared check
     * {@link CourseOfferingSectionFacultyService} uses for per-section Theory faculty.
     */
    private void requireEligibleFaculty(CourseOffering offering, Long facultyId, Long previousFacultyId) {
        FacultyEligibility.require(offering.getSubject(), facultyId, previousFacultyId, facultyRepository);
    }

    /**
     * Hard-blocks assigning a faculty whose real term-wide workload (every offering they're
     * already bound to across every cohort, plus this one) would exceed their effective capacity —
     * same check {@link TimetableGlobalAutoScheduleService}'s live precheck runs, reused here so
     * the two can never disagree. Same grandfathering as {@link #requireEligibleFaculty}: skipped
     * when unassigning or re-saving the same already-assigned faculty unchanged, since neither
     * changes anyone's real workload.
     */
    private void requireWithinCapacity(CourseOffering offering, Long facultyId) {
        if (facultyId == null || facultyId.equals(offering.getFacultyId())) {
            return;
        }
        FacultyCapacityCheckResult check = timetableGlobalAutoScheduleService.checkFacultyCapacityForOffering(
            offering.getTermInstance().getId(), offering.getId(), facultyId);
        if (!check.overCapacity()) {
            return;
        }
        StringBuilder message = new StringBuilder()
            .append("This assignment would put them at ").append(formatHours(check.projectedTotalHours()))
            .append(" against a capacity of ").append(formatHours(check.capacityHours()))
            .append(" (").append(formatHours(check.dailyCap())).append("/day) — raise their cap to at least ")
            .append(formatHours(check.suggestedMinDailyHours())).append("/day");
        if (!check.spreadLoad().isEmpty()) {
            var alt = check.spreadLoad().get(0);
            message.append(", or assign ").append(alt.alternateFacultyName())
                .append(" instead (").append(formatHours(alt.alternateSpareCapacityHours())).append(" spare capacity)");
        }
        throw new TimetableConstraintViolationException(List.of(
            new ConstraintViolation("COURSE_OFFERING_FACULTY_OVER_CAPACITY", message.toString())));
    }

    private static String formatHours(double hours) {
        return (Math.round(hours * 10) / 10.0) + "h";
    }

    @Override
    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        boolean nextActive = Boolean.TRUE.equals(request.isActive());
        if (!nextActive && Boolean.TRUE.equals(offering.getIsActive())) {
            requireSafeToDeactivate(offering);
        }
        offering.setIsActive(nextActive);
        CourseOffering saved = courseOfferingRepository.save(offering);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    /** Deactivating an offering that's already live in the timetable would silently orphan placed
     *  sessions and rostered lab/clinical batches -- block outright rather than let the admin
     *  discover the damage later in Skeleton Builder or Staffing. Reactivating (handled by the
     *  caller, {@link #updateStatus}) has no equivalent guard: deactivation never touches anything
     *  else, so flipping the flag back restores exactly the prior state. */
    private void requireSafeToDeactivate(CourseOffering offering) {
        if (!classScheduleRepository.findByCourseOfferingId(offering.getId()).isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot deactivate — this offering already has sessions placed in Skeleton Builder. Remove them there first.");
        }
        if (batchRepository.existsAnyStudentInBatchesForOffering(offering.getId())) {
            throw new IllegalArgumentException(
                "Cannot deactivate — this offering has batches with students already rostered. Remove them via Manage Batches first.");
        }
    }

    @Override
    @Transactional
    public void deactivateAllOfferingsForTermInstance(Long termInstanceId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        for (CourseOffering o : offerings) {
            o.setIsActive(false);
            courseOfferingRepository.save(o);
        }
    }

    private Set<Integer> buildRelevantSemesters(TermType termType, int totalSemesters,
                                                  AssessmentPattern pattern) {
        if (pattern == AssessmentPattern.YEARLY) {
            // Both ODD and EVEN terms teach subjects mapped to any year position;
            // the enrollment's termNumber (= year number) drives which offerings each student registers for
            return IntStream.rangeClosed(1, totalSemesters).boxed().collect(Collectors.toSet());
        }
        // TERM_BASED: odd-numbered terms belong to ODD term instance, even-numbered to EVEN
        return IntStream.rangeClosed(1, totalSemesters)
            .filter(s -> termType == TermType.ODD ? s % 2 != 0 : s % 2 == 0)
            .boxed()
            .collect(Collectors.toSet());
    }

    private record CohortSemesterKey(Long curriculumVersionId, Integer semesterNumber) {}

    /** Builds (curriculumVersionId, semesterNumber) -> cohort display names, from every cohort with
     *  an ENROLLED student in this term -- CourseOffering has no cohort FK of its own (shareable
     *  across cohorts on the same curriculum version, e.g. multiple intake years that haven't been
     *  given separate curriculum versions yet), so this reconstructs which cohort(s) each offering
     *  row actually belongs to. Cost is bounded by cohort count (small), not offering count, since
     *  callers compute it once per list request and reuse it for every row. */
    private Map<CohortSemesterKey, List<String>> buildCohortNamesByKey(Long termInstanceId) {
        Map<CohortSemesterKey, List<String>> result = new LinkedHashMap<>();
        for (Long cohortId : studentTermEnrollmentRepository.findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED)) {
            Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
            if (cohort == null) {
                continue;
            }
            CurriculumVersion cv = resolveActiveCurriculumVersion(cohort);
            if (cv == null) {
                continue;
            }
            Set<Integer> semesters = studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(termInstanceId, cohortId).stream()
                .map(StudentTermEnrollment::getSemesterNumber)
                .collect(Collectors.toSet());
            for (Integer semester : semesters) {
                result.computeIfAbsent(new CohortSemesterKey(cv.getId(), semester), k -> new ArrayList<>()).add(cohort.getDisplayName());
            }
        }
        return result;
    }

    private CourseOfferingDto toDto(CourseOffering o) {
        return toDto(o, buildCohortNamesByKey(o.getTermInstance().getId()));
    }

    private CourseOfferingDto toDto(CourseOffering o, Map<CohortSemesterKey, List<String>> cohortNamesByKey) {
        String termInstanceLabel = o.getTermInstance().getAcademicYear().getName()
            + " " + o.getTermInstance().getTermType();
        CurriculumSemesterCourse csc = o.getCurriculumSemesterCourse();
        List<String> cohortNames = cohortNamesByKey.getOrDefault(
            new CohortSemesterKey(o.getCurriculumVersion().getId(), o.getSemesterNumber()), List.of());
        return new CourseOfferingDto(
            o.getId(),
            o.getTermInstance().getId(),
            termInstanceLabel,
            o.getCurriculumVersion().getId(),
            o.getCurriculumVersion().getVersionName(),
            o.getSubject().getId(),
            o.getSubject().getName(),
            o.getSubject().getCode(),
            o.getSubject().getSpeciality() != null ? o.getSubject().getSpeciality().getId() : null,
            o.getSubject().getSpeciality() != null ? o.getSubject().getSpeciality().getName() : null,
            o.getSubject().getEligibleFaculty().stream().map(Faculty::getId).toList(),
            o.getSemesterNumber(),
            o.getFacultyId(),
            o.getSecondaryFacultyId(),
            o.getIsActive(),
            csc != null ? csc.getId() : null,
            csc != null && Boolean.TRUE.equals(csc.getIsElective()),
            csc != null ? csc.getSubjectType() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getId() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getGroupName() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getSelectionMode() : null,
            csc != null ? csc.getLabHours() : 0,
            csc != null ? csc.getClinicalHours() : 0,
            o.getCreatedAt(),
            o.getUpdatedAt(),
            cohortNames
        );
    }
}
