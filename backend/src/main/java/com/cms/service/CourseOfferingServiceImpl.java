package com.cms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseOfferingDto;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.exception.ResourceNotFoundException;
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
import com.cms.model.enums.TermType;
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

    public CourseOfferingServiceImpl(CourseOfferingRepository courseOfferingRepository,
                                      TermInstanceRepository termInstanceRepository,
                                      CohortRepository cohortRepository,
                                      CurriculumVersionRepository curriculumVersionRepository,
                                      CurriculumSemesterCourseRepository curriculumSemesterCourseRepository,
                                      FacultyRepository facultyRepository,
                                      StudentTermEnrollmentRepository studentTermEnrollmentRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.cohortRepository = cohortRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
        this.facultyRepository = facultyRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
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
        return courseOfferingRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstanceAndSemester(Long termInstanceId,
                                                                          Integer semesterNumber) {
        return courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber)
            .stream()
            .map(this::toDto)
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
        return courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSemesterNumberIn(termInstanceId, cv.getId(), semesterNumbers)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<CourseOfferingDto> getOfferingsByTermInstanceAndElectiveGroup(Long termInstanceId, Long electiveGroupId) {
        return courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId)
            .stream()
            .map(this::toDto)
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
    public CourseOfferingDto updateOffering(Long id, Long facultyId, Long secondaryFacultyId, String sectionLabel) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        requireEligibleFaculty(offering, facultyId, offering.getFacultyId());
        offering.setFacultyId(facultyId);
        // OC-127 gap-closure follow-up: secondaryFacultyId reopened from informational-only to a
        // real substitute-matching-eligible co-instructor -- same department-eligibility gate as
        // the primary, grandfathered against its own prior value independently of the primary's.
        requireEligibleFaculty(offering, secondaryFacultyId, offering.getSecondaryFacultyId());
        offering.setSecondaryFacultyId(secondaryFacultyId);
        offering.setSectionLabel(sectionLabel);
        return toDto(courseOfferingRepository.save(offering));
    }

    /**
     * Department-level (Speciality) eligibility gate, mirroring {@code ClassScheduleService}'s
     * check for manually-edited sessions. Skipped when unassigning (facultyId null), when the
     * subject has no speciality set, and grandfathered when the requested faculty is already the
     * one previously on this slot — blocks new/changed mismatched assignments without
     * retroactively breaking a row saved before this rule existed on an otherwise-unrelated edit
     * (e.g. section label). Shared by both the primary and secondary faculty slots, each checked
     * against its own prior value.
     */
    private void requireEligibleFaculty(CourseOffering offering, Long facultyId, Long previousFacultyId) {
        if (facultyId == null || facultyId.equals(previousFacultyId)) {
            return;
        }
        Subject subject = offering.getSubject();
        if (subject.getSpeciality() == null) {
            return;
        }
        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
        if (!subject.getSpeciality().getId().equals(faculty.getSpeciality().getId())) {
            throw new IllegalArgumentException("Faculty '" + faculty.getFullName() + "' belongs to the "
                + faculty.getSpeciality().getName() + " department and is not eligible to teach '"
                + subject.getName() + "' (" + subject.getSpeciality().getName() + ")");
        }
    }

    @Override
    @Transactional
    public void deactivateOffering(Long id) {
        CourseOffering offering = courseOfferingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + id));
        offering.setIsActive(false);
        courseOfferingRepository.save(offering);
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

    private CourseOfferingDto toDto(CourseOffering o) {
        String termInstanceLabel = o.getTermInstance().getAcademicYear().getName()
            + " " + o.getTermInstance().getTermType();
        CurriculumSemesterCourse csc = o.getCurriculumSemesterCourse();
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
            o.getSemesterNumber(),
            o.getFacultyId(),
            o.getSecondaryFacultyId(),
            o.getSectionLabel(),
            o.getIsActive(),
            csc != null ? csc.getId() : null,
            csc != null && Boolean.TRUE.equals(csc.getIsElective()),
            csc != null ? csc.getSubjectType() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getId() : null,
            csc != null && csc.getElectiveGroup() != null ? csc.getElectiveGroup().getGroupName() : null,
            csc != null ? csc.getLabHours() : 0,
            csc != null ? csc.getClinicalHours() : 0,
            o.getCreatedAt(),
            o.getUpdatedAt()
        );
    }
}
