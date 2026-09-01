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
import com.cms.dto.ClinicalShiftConfigUpdateRequest;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Faculty;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
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
    private final CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;

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
                                      BatchRepository batchRepository,
                                      CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRepository = cohortRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
        this.facultyRepository = facultyRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.batchRepository = batchRepository;
        this.courseOfferingSectionFacultyRepository = courseOfferingSectionFacultyRepository;
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

    /**
     * Replaces this offering's admin-curated faculty pool wholesale. A pool member must already
     * pass {@link FacultyEligibility#eligibleFaculty} (Speciality match OR the subject's Eligible
     * Faculty list) — the pool only ever narrows that set further, never widens it. Removing anyone
     * currently relied upon (holding any {@link CourseOfferingSectionFaculty} row for this
     * offering, whole-cohort or per-section) is hard-blocked rather than silently orphaning their
     * assignment — the admin must reassign that slot first.
     */
    @Override
    @Transactional
    public List<EligibleFacultyCandidateDto> updateFacultyPool(Long id, List<Long> facultyIds) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));

        Set<Long> requestedIds = new java.util.LinkedHashSet<>(facultyIds);
        List<Faculty> activeFaculty = facultyRepository.findByStatus(FacultyStatus.ACTIVE);
        Map<Long, Faculty> activeById = activeFaculty.stream().collect(Collectors.toMap(Faculty::getId, f -> f));
        Set<Long> eligibleIds = FacultyEligibility.eligibleFaculty(offering.getSubject(), activeFaculty).stream()
            .map(Faculty::getId).collect(Collectors.toSet());
        List<Long> ineligible = requestedIds.stream().filter(fid -> !eligibleIds.contains(fid)).toList();
        if (!ineligible.isEmpty()) {
            throw new IllegalArgumentException("Faculty id(s) " + ineligible + " are not eligible for this subject "
                + "-- only Speciality match or the subject's Eligible Faculty list can be pooled");
        }

        Set<Long> currentPoolIds = offering.getFacultyPool().stream().map(Faculty::getId).collect(Collectors.toSet());
        Set<Long> removedIds = currentPoolIds.stream().filter(fid -> !requestedIds.contains(fid)).collect(Collectors.toSet());
        for (CourseOfferingSectionFaculty sf : courseOfferingSectionFacultyRepository.findByCourseOfferingId(id)) {
            if (removedIds.contains(sf.getFaculty().getId())) {
                String where = sf.getCohortSection() != null
                    ? "section " + sf.getCohortSection().getSectionLabel()
                    : sf.getCohort().getDisplayName();
                throw new IllegalArgumentException("Can't remove " + sf.getFaculty().getFullName()
                    + " -- they're currently assigned to " + where + "; reassign that first");
            }
        }

        offering.setFacultyPool(requestedIds.stream().map(activeById::get).collect(Collectors.toSet()));
        courseOfferingRepository.save(offering);

        return timetableGlobalAutoScheduleService.getEligibleFacultyForOffering(id);
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
            o.getIsActive(),
            csc != null ? csc.getId() : null,
            csc != null && Boolean.TRUE.equals(csc.getIsElective()),
            csc != null ? csc.getSubjectType() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getId() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getGroupName() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getSelectionMode() : null,
            csc != null ? csc.getLabHours() : 0,
            csc != null ? csc.getClinicalHours() : 0,
            o.getClinicalShiftDurationMinutes(),
            o.getClinicalTravelBufferMinutes(),
            o.getCreatedAt(),
            o.getUpdatedAt(),
            cohortNames
        );
    }

    @Override
    @Transactional
    public CourseOfferingDto updateClinicalShiftConfig(Long id, ClinicalShiftConfigUpdateRequest request) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        boolean settingDuration = request.clinicalShiftDurationMinutes() != null;
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        boolean hasClinicalHours = csc != null && csc.getClinicalHours() != null && csc.getClinicalHours() > 0;
        if (settingDuration && !hasClinicalHours) {
            throw new IllegalStateException(
                "Course offering " + id + "'s subject has no clinical hours in the curriculum -- "
                    + "shift-based clinical scheduling only applies to subjects with clinical hours");
        }
        offering.setClinicalShiftDurationMinutes(request.clinicalShiftDurationMinutes());
        offering.setClinicalTravelBufferMinutes(request.clinicalTravelBufferMinutes());
        return toDto(courseOfferingRepository.save(offering));
    }
}
