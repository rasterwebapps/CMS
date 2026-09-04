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
import com.cms.model.Batch;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.OfferingAssignmentStatus;
import com.cms.repository.BatchRepository;
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
    private final BatchRepository batchRepository;
    private final TimetableSkeletonService timetableSkeletonService;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public CourseOfferingSectionFacultyService(CourseOfferingRepository courseOfferingRepository,
                                                CourseOfferingSectionFacultyRepository sectionFacultyRepository,
                                                CohortRepository cohortRepository,
                                                StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                                FacultyRepository facultyRepository,
                                                BatchRepository batchRepository,
                                                TimetableSkeletonService timetableSkeletonService,
                                                TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionFacultyRepository = sectionFacultyRepository;
        this.cohortRepository = cohortRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.facultyRepository = facultyRepository;
        this.batchRepository = batchRepository;
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
                    CourseOfferingSectionFaculty existingRow = existingRows.stream()
                        .filter(sf -> sf.getCohortSection() == null && sf.getCohort().getId().equals(cohort.getId()))
                        .findFirst().orElse(null);
                    return java.util.stream.Stream.of(new SectionFacultyAssignment(cohort.getId(), null, cohort.getDisplayName(), null,
                        assigned != null ? assigned.getId() : null, assigned != null ? assigned.getFullName() : null,
                        existingRow != null ? existingRow.getVersion() : null));
                }
                return sections.stream().map(section -> {
                    Faculty assigned = facultyBySectionId.get(section.getId());
                    CourseOfferingSectionFaculty existingRow = existingRows.stream()
                        .filter(sf -> sf.getCohortSection() != null && sf.getCohortSection().getId().equals(section.getId()))
                        .findFirst().orElse(null);
                    return new SectionFacultyAssignment(cohort.getId(), section.getId(), cohort.getDisplayName(), section.getSectionLabel(),
                        assigned != null ? assigned.getId() : null, assigned != null ? assigned.getFullName() : null,
                        existingRow != null ? existingRow.getVersion() : null);
                });
            })
            .toList();

        return new CourseOfferingSectionFacultyResponse(true, null, rows);
    }

    /** One roll-up row per offering in this term instance, always -- unlike the old "only offerings
     *  with at least one row" grouping, every offering is now represented so {@link
     *  OfferingAssignmentStatus} can tell "nothing assigned yet" (NONE) apart from "nothing to
     *  assign" (NOT_APPLICABLE), which a bare absence from the list couldn't. {@code
     *  assignedFacultyNames} stays Theory-only (deduplicated, sorted) -- unchanged from before.
     *  {@code assignmentStatus} additionally covers every active Lab/Clinical {@link Batch}'s
     *  coordinator, comparing each offering's *expected* row/batch count (a full {@link
     *  #getForOffering} resolution, not just what's persisted) against how many are actually
     *  filled -- backs both the Assign Faculty list table's status column and {@code
     *  TimetableGenerationService#approve}'s Publish gate. */
    @Transactional(readOnly = true)
    public List<CourseOfferingFacultySummaryDto> getAssignmentSummaryForTermInstance(Long termInstanceId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceId(termInstanceId);
        Map<Long, List<CourseOfferingSectionFaculty>> rowsByOffering = sectionFacultyRepository
            .findByCourseOffering_TermInstanceId(termInstanceId).stream()
            .collect(Collectors.groupingBy(sf -> sf.getCourseOffering().getId()));
        Map<Long, List<Batch>> batchesByOffering = batchRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .collect(Collectors.groupingBy(b -> b.getCourseOffering().getId()));

        List<CourseOfferingFacultySummaryDto> result = new java.util.ArrayList<>();
        for (CourseOffering offering : offerings) {
            List<CourseOfferingSectionFaculty> rows = rowsByOffering.getOrDefault(offering.getId(), List.of());
            List<String> names = rows.stream().map(sf -> sf.getFaculty().getFullName()).distinct().sorted().toList();

            int expected = 0;
            int assigned = 0;
            if (!resolveCohorts(offering).isEmpty()) {
                CourseOfferingSectionFacultyResponse resp = getForOffering(offering.getId());
                expected += resp.sections().size();
                assigned += (int) resp.sections().stream().filter(s -> s.facultyId() != null).count();
            }
            List<Batch> batches = batchesByOffering.getOrDefault(offering.getId(), List.of());
            expected += batches.size();
            assigned += (int) batches.stream().filter(b -> b.getCoordinatorFaculty() != null).count();

            OfferingAssignmentStatus status;
            if (expected == 0) status = OfferingAssignmentStatus.NOT_APPLICABLE;
            else if (assigned == 0) status = OfferingAssignmentStatus.NONE;
            else if (assigned == expected) status = OfferingAssignmentStatus.FULL;
            else status = OfferingAssignmentStatus.PARTIAL;

            result.add(new CourseOfferingFacultySummaryDto(offering.getId(), names, status));
        }
        return result;
    }

    /** {@code facultyId} null clears any existing override for this section. Gated by the same
     *  department-eligibility rule as every other faculty assignment, grandfathered against this
     *  specific section's own prior value. {@code requestVersion} is the row's version as last
     *  seen by the client (null if the client saw no row, i.e. "Unassigned") -- rejected if it no
     *  longer matches, so a stale save can't silently overwrite someone else's concurrent change. */
    @Transactional
    public SectionFacultyAssignment upsert(Long offeringId, Long cohortSectionId, Long facultyId, Long requestVersion) {
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
        requireCurrentVersion(existing, requestVersion, cohortName + (section.getSectionLabel() != null ? " — " + section.getSectionLabel() : ""));

        if (facultyId == null) {
            existing.ifPresent(sectionFacultyRepository::delete);
            return new SectionFacultyAssignment(cohort.getId(), cohortSectionId, cohortName, section.getSectionLabel(), null, null, null);
        }

        Long previousFacultyId = existing.map(sf -> sf.getFaculty().getId()).orElse(null);
        FacultyEligibility.require(offering.getSubject(), facultyId, previousFacultyId, facultyRepository);
        requireNoElectiveGroupFacultyConflict(offering, facultyId, previousFacultyId);
        requireWithinCapacityForSection(offeringId, cohortSectionId, facultyId, previousFacultyId);

        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        CourseOfferingSectionFaculty row = existing.orElseGet(() -> new CourseOfferingSectionFaculty(offering, section, faculty));
        row.setCourseOffering(offering);
        row.setCohort(cohort);
        row.setCohortSection(section);
        row.setFaculty(faculty);
        sectionFacultyRepository.save(row);

        return new SectionFacultyAssignment(cohort.getId(), cohortSectionId, cohortName, section.getSectionLabel(), faculty.getId(), faculty.getFullName(), row.getVersion());
    }

    /** Same optimistic-lock check {@link com.cms.service.BatchService} uses -- rejects a stale
     *  save (including one whose client thought no row existed yet, but one now does) instead of
     *  silently overwriting a concurrent change. */
    private void requireCurrentVersion(Optional<CourseOfferingSectionFaculty> existing, Long requestVersion, String label) {
        Long currentVersion = existing.map(CourseOfferingSectionFaculty::getVersion).orElse(null);
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new IllegalStateException(
                "\"" + label + "\"'s faculty assignment was changed by someone else since you opened this dialog. Reload to see the latest data.");
        }
    }

    /** Whole-cohort counterpart of {@link #upsert} -- for a cohort whose Theory delivery has no
     *  active section split. Rejects a cohort that currently *does* have active sections (that
     *  cohort must be assigned per-section via {@link #upsert} instead, one call per section). */
    @Transactional
    public SectionFacultyAssignment upsertForCohort(Long offeringId, Long cohortId, Long facultyId, Long requestVersion) {
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
        requireCurrentVersion(existing, requestVersion, cohortName);

        if (facultyId == null) {
            existing.ifPresent(sectionFacultyRepository::delete);
            return new SectionFacultyAssignment(cohortId, null, cohortName, null, null, null, null);
        }

        Long previousFacultyId = existing.map(sf -> sf.getFaculty().getId()).orElse(null);
        FacultyEligibility.require(offering.getSubject(), facultyId, previousFacultyId, facultyRepository);
        requireNoElectiveGroupFacultyConflict(offering, facultyId, previousFacultyId);
        requireWithinCapacityForCohort(offeringId, cohortId, facultyId, previousFacultyId);

        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        CourseOfferingSectionFaculty row = existing.orElseGet(() -> new CourseOfferingSectionFaculty(offering, cohort, faculty));
        row.setCourseOffering(offering);
        row.setCohort(cohort);
        row.setCohortSection(null);
        row.setFaculty(faculty);
        sectionFacultyRepository.save(row);

        return new SectionFacultyAssignment(cohortId, null, cohortName, null, faculty.getId(), faculty.getFullName(), row.getVersion());
    }

    /** Hard-blocks binding a faculty member to an elective offering already covered elsewhere by
     *  that exact same person. Every option in an elective group is required to run at one shared
     *  simultaneous slot (see {@code TimetableSkeletonService#checkElectiveGroupSlot}), so one
     *  faculty bound to two different options in the same group is a structural impossibility, not
     *  a scheduling difficulty — it can never be placed by any automated or manual run, since it
     *  would need that one person physically teaching two subjects at once. Confirmed 2026-09-02
     *  after Global Auto-Schedule silently reported "no day/period found" for two elective groups
     *  that turned out to have exactly this shape (three faculty each double-booked across two
     *  options) — this is the gate that should have caught it at assignment time instead of
     *  surfacing as an unexplained scheduling dead-end months later. Skipped entirely for a
     *  non-elective offering, and when re-saving this exact row's own already-assigned faculty
     *  unchanged. */
    private void requireNoElectiveGroupFacultyConflict(CourseOffering offering, Long facultyId, Long previousFacultyId) {
        if (facultyId == null || facultyId.equals(previousFacultyId)) {
            return;
        }
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null || csc.getElectiveGroup() == null) {
            return;
        }
        List<CourseOffering> siblings = courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(
            offering.getTermInstance().getId(), csc.getElectiveGroup().getId());
        for (CourseOffering sibling : siblings) {
            if (sibling.getId().equals(offering.getId()) || !Boolean.TRUE.equals(sibling.getIsActive())) {
                continue;
            }
            boolean alreadyOnSibling = sectionFacultyRepository.findByCourseOfferingId(sibling.getId()).stream()
                .anyMatch(sf -> sf.getFaculty().getId().equals(facultyId));
            if (alreadyOnSibling) {
                Faculty faculty = facultyRepository.findById(facultyId).orElse(null);
                throw new TimetableConstraintViolationException(List.of(new ConstraintViolation(
                    "ELECTIVE_GROUP_FACULTY_CONFLICT",
                    (faculty != null ? faculty.getFullName() : "This faculty member") + " is already assigned to "
                        + sibling.getSubject().getName() + " in " + csc.getElectiveGroup().getGroupName()
                        + " — every option in an elective group runs at one shared simultaneous slot, so the same "
                        + "faculty can never cover two different options in it. Assign a different faculty member.")));
            }
        }
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
