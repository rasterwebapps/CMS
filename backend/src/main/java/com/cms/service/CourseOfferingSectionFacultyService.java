package com.cms.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.Faculty;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

/**
 * Manages per-section Theory faculty overrides (see {@link CourseOfferingSectionFaculty}).
 * Advisory/accounting-only for v1 -- feeds {@link TimetableGlobalAutoScheduleService}'s capacity
 * math, never Skeleton Builder placement or Staffing.
 */
@Service
public class CourseOfferingSectionFacultyService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseOfferingSectionFacultyRepository sectionFacultyRepository;
    private final CohortRepository cohortRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final FacultyRepository facultyRepository;
    private final TimetableSkeletonService timetableSkeletonService;

    public CourseOfferingSectionFacultyService(CourseOfferingRepository courseOfferingRepository,
                                                CourseOfferingSectionFacultyRepository sectionFacultyRepository,
                                                CohortRepository cohortRepository,
                                                StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                                FacultyRepository facultyRepository,
                                                TimetableSkeletonService timetableSkeletonService) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionFacultyRepository = sectionFacultyRepository;
        this.cohortRepository = cohortRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.facultyRepository = facultyRepository;
        this.timetableSkeletonService = timetableSkeletonService;
    }

    @Transactional(readOnly = true)
    public CourseOfferingSectionFacultyResponse getForOffering(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));

        List<Cohort> cohorts = resolveCohorts(offering);
        if (cohorts.isEmpty()) {
            return new CourseOfferingSectionFacultyResponse(false, NOT_APPLICABLE_REASON, List.of());
        }

        Map<Long, Faculty> facultyBySectionId = sectionFacultyRepository.findByCourseOfferingId(offeringId).stream()
            .collect(Collectors.toMap(sf -> sf.getCohortSection().getId(), CourseOfferingSectionFaculty::getFaculty));

        List<SectionFacultyAssignment> sections = cohorts.stream()
            .flatMap(cohort -> timetableSkeletonService.resolveActiveSections(cohort.getId(), offering.getTermInstance().getId()).stream()
                .map(section -> {
                    Faculty assigned = facultyBySectionId.get(section.getId());
                    return new SectionFacultyAssignment(section.getId(), cohort.getDisplayName(), section.getSectionLabel(),
                        assigned != null ? assigned.getId() : null, assigned != null ? assigned.getFullName() : null);
                }))
            .toList();

        return new CourseOfferingSectionFacultyResponse(true, null, sections);
    }

    /** {@code facultyId} null clears any existing override for this section, falling back to the
     *  offering's own primary faculty. Otherwise upserts, gated by the same department-eligibility
     *  rule as the offering's primary/secondary faculty, grandfathered against this specific
     *  section's own prior value (not the offering's primary). */
    @Transactional
    public SectionFacultyAssignment upsert(Long offeringId, Long cohortSectionId, Long facultyId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));

        List<Cohort> cohorts = resolveCohorts(offering);
        CohortSection section = cohorts.stream()
            .flatMap(cohort -> timetableSkeletonService.resolveActiveSections(cohort.getId(), offering.getTermInstance().getId()).stream())
            .filter(s -> s.getId().equals(cohortSectionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("This is not a currently active section of any cohort using this offering"));

        Optional<CourseOfferingSectionFaculty> existing =
            sectionFacultyRepository.findByCourseOfferingIdAndCohortSectionId(offeringId, cohortSectionId);
        String cohortName = section.getCohortRoomAllocation().getCohort().getDisplayName();

        if (facultyId == null) {
            existing.ifPresent(sectionFacultyRepository::delete);
            return new SectionFacultyAssignment(cohortSectionId, cohortName, section.getSectionLabel(), null, null);
        }

        Long previousFacultyId = existing.map(sf -> sf.getFaculty().getId()).orElse(null);
        FacultyEligibility.require(offering.getSubject(), facultyId, previousFacultyId, facultyRepository);

        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        CourseOfferingSectionFaculty row = existing.orElseGet(() -> new CourseOfferingSectionFaculty(offering, section, faculty));
        row.setCourseOffering(offering);
        row.setCohortSection(section);
        row.setFaculty(faculty);
        sectionFacultyRepository.save(row);

        return new SectionFacultyAssignment(cohortSectionId, cohortName, section.getSectionLabel(), faculty.getId(), faculty.getFullName());
    }

    private static final String NOT_APPLICABLE_REASON = "No cohort is currently enrolled against this offering's curriculum version.";

    /** {@link CourseOffering} has no cohort FK of its own -- it's keyed by curriculum version,
     *  which can be shared by more than one cohort's admission year on the same (program, course).
     *  Reconstructs every cohort currently enrolled in this offering's term whose (program, course)
     *  matches this offering's curriculum version, mirroring {@code CourseOfferingServiceImpl}'s
     *  own cohort-to-curriculum-version resolution. Each {@link CohortSection} unambiguously
     *  belongs to exactly one cohort regardless of how many share the offering, so there's no real
     *  ambiguity in listing every matching cohort's sections together -- only an empty result (no
     *  cohort enrolled at all) is genuinely inapplicable. */
    private List<Cohort> resolveCohorts(CourseOffering offering) {
        Long programId = offering.getCurriculumVersion().getProgram().getId();
        Long courseId = offering.getCurriculumVersion().getCourse().getId();
        Long termInstanceId = offering.getTermInstance().getId();

        return studentTermEnrollmentRepository
            .findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED)
            .stream()
            .map(cohortId -> cohortRepository.findById(cohortId).orElse(null))
            .filter(Objects::nonNull)
            .filter(c -> c.getProgram() != null && c.getProgram().getId().equals(programId)
                && c.getCourse() != null && c.getCourse().getId().equals(courseId))
            .toList();
    }
}
