package com.cms.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingFacultySummaryDto;
import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
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
 * Manages per-(offering, cohort) faculty assignments (see {@link CourseOfferingSectionFaculty}) --
 * authoritative for placement: {@link TimetableGlobalAutoScheduleService#runGlobalAutoSchedule} and
 * {@link TimetableStaffingAutoAssignService#autoStaff} both resolve a Theory row's faculty from
 * here. Every cohort using an offering gets exactly one row per active section if its Theory
 * delivery has split, or exactly one whole-cohort row if it hasn't -- there is no offering-wide
 * "primary" faculty anymore (retired in V404; a single scalar couldn't represent more than one
 * cohort sharing an offering being assigned independently).
 */
@Service
public class CourseOfferingSectionFacultyService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseOfferingSectionFacultyRepository sectionFacultyRepository;
    private final CohortRepository cohortRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final FacultyRepository facultyRepository;
    private final TimetableSkeletonService timetableSkeletonService;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public CourseOfferingSectionFacultyService(CourseOfferingRepository courseOfferingRepository,
                                                CourseOfferingSectionFacultyRepository sectionFacultyRepository,
                                                CohortRepository cohortRepository,
                                                StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                                FacultyRepository facultyRepository,
                                                TimetableSkeletonService timetableSkeletonService,
                                                TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionFacultyRepository = sectionFacultyRepository;
        this.cohortRepository = cohortRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.facultyRepository = facultyRepository;
        this.timetableSkeletonService = timetableSkeletonService;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
    }

    /** One row per active section for a split cohort, or exactly one whole-cohort row for a
     *  cohort with no split -- every cohort using this offering is represented, always (no more
     *  "fewer than 2 sections means nothing to show" -- that was only ever true when there was a
     *  primary faculty to fall back to). {@code applicable=false} only when zero cohorts resolve
     *  at all (none currently enrolled against this offering's curriculum version + semester). */
    @Transactional(readOnly = true)
    public CourseOfferingSectionFacultyResponse getForOffering(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));

        List<Cohort> cohorts = resolveCohorts(offering);
        if (cohorts.isEmpty()) {
            return new CourseOfferingSectionFacultyResponse(false, NOT_APPLICABLE_REASON, List.of());
        }

        List<CourseOfferingSectionFaculty> existingRows = sectionFacultyRepository.findByCourseOfferingId(offeringId);
        Map<Long, Faculty> facultyBySectionId = existingRows.stream()
            .filter(sf -> sf.getCohortSection() != null)
            .collect(Collectors.toMap(sf -> sf.getCohortSection().getId(), CourseOfferingSectionFaculty::getFaculty));
        Map<Long, Faculty> facultyByWholeCohortId = existingRows.stream()
            .filter(sf -> sf.getCohortSection() == null)
            .collect(Collectors.toMap(sf -> sf.getCohort().getId(), CourseOfferingSectionFaculty::getFaculty));

        List<SectionFacultyAssignment> rows = cohorts.stream()
            .flatMap(cohort -> {
                List<CohortSection> sections = timetableSkeletonService.resolveActiveSections(cohort.getId(), offering.getTermInstance().getId());
                if (sections.isEmpty()) {
                    Faculty assigned = facultyByWholeCohortId.get(cohort.getId());
                    return java.util.stream.Stream.of(new SectionFacultyAssignment(cohort.getId(), null, cohort.getDisplayName(), null,
                        assigned != null ? assigned.getId() : null, assigned != null ? assigned.getFullName() : null));
                }
                return sections.stream().map(section -> {
                    Faculty assigned = facultyBySectionId.get(section.getId());
                    return new SectionFacultyAssignment(cohort.getId(), section.getId(), cohort.getDisplayName(), section.getSectionLabel(),
                        assigned != null ? assigned.getId() : null, assigned != null ? assigned.getFullName() : null);
                });
            })
            .toList();

        return new CourseOfferingSectionFacultyResponse(true, null, rows);
    }

    /** One roll-up row per offering that has ANY assignment row in this term instance -- offerings
     *  with zero rows are simply absent from the result (the caller treats "not present" as
     *  "Unassigned"). Deliberately a raw grouping of already-persisted rows, not a re-run of {@link
     *  #resolveCohorts}/{@link TimetableSkeletonService#resolveActiveSections} per offering -- that
     *  full resolution is what tells you the *expected* row count (e.g. a 3-way split cohort with
     *  only 1 section assigned so far), which is exactly what {@link #getForOffering} is for when
     *  the admin opens a specific offering; the list table only needs a cheap "who's currently on
     *  it" pulse-check across potentially dozens of offerings at once. */
    @Transactional(readOnly = true)
    public List<CourseOfferingFacultySummaryDto> getAssignmentSummaryForTermInstance(Long termInstanceId) {
        return sectionFacultyRepository.findByCourseOffering_TermInstanceId(termInstanceId).stream()
            .collect(Collectors.groupingBy(sf -> sf.getCourseOffering().getId()))
            .entrySet().stream()
            .map(e -> new CourseOfferingFacultySummaryDto(e.getKey(),
                e.getValue().stream().map(sf -> sf.getFaculty().getFullName()).distinct().sorted().toList()))
            .toList();
    }

    /** {@code facultyId} null clears any existing override for this section. Gated by the same
     *  department-eligibility rule as every other faculty assignment, grandfathered against this
     *  specific section's own prior value. */
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
        Cohort cohort = section.getCohortRoomAllocation().getCohort();
        String cohortName = cohort.getDisplayName();

        if (facultyId == null) {
            existing.ifPresent(sectionFacultyRepository::delete);
            return new SectionFacultyAssignment(cohort.getId(), cohortSectionId, cohortName, section.getSectionLabel(), null, null);
        }

        Long previousFacultyId = existing.map(sf -> sf.getFaculty().getId()).orElse(null);
        FacultyEligibility.require(offering.getSubject(), facultyId, previousFacultyId, facultyRepository);
        requireWithinCapacityForSection(offeringId, cohortSectionId, facultyId, previousFacultyId);

        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        CourseOfferingSectionFaculty row = existing.orElseGet(() -> new CourseOfferingSectionFaculty(offering, section, faculty));
        row.setCourseOffering(offering);
        row.setCohort(cohort);
        row.setCohortSection(section);
        row.setFaculty(faculty);
        sectionFacultyRepository.save(row);

        return new SectionFacultyAssignment(cohort.getId(), cohortSectionId, cohortName, section.getSectionLabel(), faculty.getId(), faculty.getFullName());
    }

    /** Whole-cohort counterpart of {@link #upsert} -- for a cohort whose Theory delivery has no
     *  active section split. Rejects a cohort that currently *does* have active sections (that
     *  cohort must be assigned per-section via {@link #upsert} instead, one call per section). */
    @Transactional
    public SectionFacultyAssignment upsertForCohort(Long offeringId, Long cohortId, Long facultyId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));

        Cohort cohort = resolveCohorts(offering).stream()
            .filter(c -> c.getId().equals(cohortId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("This is not a currently enrolled cohort for this offering"));

        List<CohortSection> activeSections = timetableSkeletonService.resolveActiveSections(cohortId, offering.getTermInstance().getId());
        if (!activeSections.isEmpty()) {
            throw new IllegalArgumentException("This cohort's Theory delivery is split into active sections "
                + "-- assign faculty per section instead of for the whole cohort");
        }

        Optional<CourseOfferingSectionFaculty> existing =
            sectionFacultyRepository.findByCourseOfferingIdAndCohortIdAndCohortSectionIdIsNull(offeringId, cohortId);
        String cohortName = cohort.getDisplayName();

        if (facultyId == null) {
            existing.ifPresent(sectionFacultyRepository::delete);
            return new SectionFacultyAssignment(cohortId, null, cohortName, null, null, null);
        }

        Long previousFacultyId = existing.map(sf -> sf.getFaculty().getId()).orElse(null);
        FacultyEligibility.require(offering.getSubject(), facultyId, previousFacultyId, facultyRepository);
        requireWithinCapacityForCohort(offeringId, cohortId, facultyId, previousFacultyId);

        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        CourseOfferingSectionFaculty row = existing.orElseGet(() -> new CourseOfferingSectionFaculty(offering, cohort, faculty));
        row.setCourseOffering(offering);
        row.setCohort(cohort);
        row.setCohortSection(null);
        row.setFaculty(faculty);
        sectionFacultyRepository.save(row);

        return new SectionFacultyAssignment(cohortId, null, cohortName, null, faculty.getId(), faculty.getFullName());
    }

    /** Hard-blocks assigning a section faculty whose real term-wide workload would exceed their
     *  effective capacity, scoped to just this section via {@link
     *  TimetableGlobalAutoScheduleService#checkFacultyCapacityForSection}. Skipped when clearing
     *  or re-saving this section's own already-assigned faculty unchanged. */
    private void requireWithinCapacityForSection(Long offeringId, Long cohortSectionId, Long facultyId, Long previousFacultyId) {
        if (facultyId == null || facultyId.equals(previousFacultyId)) {
            return;
        }
        raiseIfOverCapacity(timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(offeringId, cohortSectionId, facultyId));
    }

    /** Cohort-scoped counterpart of {@link #requireWithinCapacityForSection}, via {@link
     *  TimetableGlobalAutoScheduleService#checkFacultyCapacityForCohort}. */
    private void requireWithinCapacityForCohort(Long offeringId, Long cohortId, Long facultyId, Long previousFacultyId) {
        if (facultyId == null || facultyId.equals(previousFacultyId)) {
            return;
        }
        raiseIfOverCapacity(timetableGlobalAutoScheduleService.checkFacultyCapacityForCohort(offeringId, cohortId, facultyId));
    }

    private void raiseIfOverCapacity(FacultyCapacityCheckResult check) {
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
            new ConstraintViolation("SECTION_FACULTY_OVER_CAPACITY", message.toString())));
    }

    private static String formatHours(double hours) {
        return (Math.round(hours * 10) / 10.0) + "h";
    }

    private static final String NOT_APPLICABLE_REASON = "No cohort is currently enrolled against this offering's curriculum version.";

    /** {@link CourseOffering} has no cohort FK of its own -- it's keyed by (curriculum version,
     *  semesterNumber), and a curriculum version can be shared by more than one cohort's admission
     *  year on the same (program, course). Reconstructs every cohort currently enrolled in this
     *  offering's term whose (program, course) matches this offering's curriculum version AND
     *  which actually has a student enrolled at this offering's own semesterNumber this term,
     *  mirroring {@code CourseOfferingServiceImpl#buildCohortNamesByKey}'s (curriculumVersionId,
     *  semesterNumber) key exactly -- matching on (program, course) alone (the original version of
     *  this method) wrongly pulled in every admission-year cohort sharing that program/course
     *  regardless of which semester they were actually enrolled at this term, e.g. a Semester-1
     *  offering also listing a senior cohort's Semester-3 sections. Each {@link CohortSection}
     *  unambiguously belongs to exactly one cohort regardless of how many share the offering, so
     *  there's no real ambiguity in listing every matching cohort's sections together -- only an
     *  empty result (no cohort enrolled at this semester at all) is genuinely inapplicable. */
    private List<Cohort> resolveCohorts(CourseOffering offering) {
        Long programId = offering.getCurriculumVersion().getProgram().getId();
        Long courseId = offering.getCurriculumVersion().getCourse().getId();
        Integer semesterNumber = offering.getSemesterNumber();
        Long termInstanceId = offering.getTermInstance().getId();

        return studentTermEnrollmentRepository
            .findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED)
            .stream()
            .map(cohortId -> cohortRepository.findById(cohortId).orElse(null))
            .filter(Objects::nonNull)
            .filter(c -> c.getProgram() != null && c.getProgram().getId().equals(programId)
                && c.getCourse() != null && c.getCourse().getId().equals(courseId))
            .filter(c -> studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(termInstanceId, c.getId()).stream()
                .anyMatch(e -> Objects.equals(e.getSemesterNumber(), semesterNumber)))
            .toList();
    }
}
