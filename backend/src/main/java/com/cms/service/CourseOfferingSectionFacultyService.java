package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AutoAssignTheoryResult;
import com.cms.dto.ConfirmFacultySubstitutionItem;
import com.cms.dto.ConfirmFacultySubstitutionsResult;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.CourseOfferingFacultySummaryDto;
import com.cms.dto.CourseOfferingSectionFacultyResponse;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.dto.SectionFacultyCapacityFailure;
import com.cms.dto.SubstitutionAffectedSection;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.SectionFacultyCapacityException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Batch;
import com.cms.model.Cohort;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.OfferingAssignmentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortSectionRepository;
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
    private final BatchService batchService;
    private final CohortSectionRepository cohortSectionRepository;

    public CourseOfferingSectionFacultyService(CourseOfferingRepository courseOfferingRepository,
                                                CourseOfferingSectionFacultyRepository sectionFacultyRepository,
                                                CohortRepository cohortRepository,
                                                StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                                FacultyRepository facultyRepository,
                                                BatchRepository batchRepository,
                                                TimetableSkeletonService timetableSkeletonService,
                                                TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService,
                                                BatchService batchService,
                                                CohortSectionRepository cohortSectionRepository) {
        this.batchService = batchService;
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionFacultyRepository = sectionFacultyRepository;
        this.cohortRepository = cohortRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.facultyRepository = facultyRepository;
        this.batchRepository = batchRepository;
        this.timetableSkeletonService = timetableSkeletonService;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
        this.cohortSectionRepository = cohortSectionRepository;
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
     *  assignedFacultyNames} stays Theory-only (deduplicated, sorted). {@code assignmentStatus}
     *  additionally covers every active Lab/Clinical {@link Batch}'s coordinator, comparing each
     *  offering's *expected* row/batch count (a full {@link #getForOffering} resolution, not just
     *  what's persisted) against how many are actually filled -- backs both the Assign Faculty list
     *  table's status column and {@code TimetableGenerationService#approve}'s Publish gate.
     *
     * <p>{@code assignedFacultyNames} is built from that same {@link #getForOffering} live-resolved
     *  view, not from raw {@link CourseOfferingSectionFaculty} rows -- a row can outlive the section
     *  it names (a cohort's split status changes, a section gets relabeled/deactivated on a Capacity
     *  Auto-Plan recommit) with nothing ever deleting it, since {@code
     *  CourseOfferingSectionFacultyRepository#deleteByCourseOfferingIdAndCohortSectionId}/{@code
     *  deleteByCourseOfferingIdAndCohortIdAndCohortSectionIdIsNull} only run when an admin
     *  explicitly clears an assignment, not when a section's own liveness changes underneath it.
     *  Reading raw rows here would show that stale row's faculty name in the list while {@code
     *  assignmentStatus} (and the edit dialog, which also calls {@link #getForOffering}) correctly
     *  call the same section "Unassigned" -- i.e. exactly the bug this fixes, not a display quirk. */
    @Transactional(readOnly = true)
    public List<CourseOfferingFacultySummaryDto> getAssignmentSummaryForTermInstance(Long termInstanceId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceId(termInstanceId);
        Map<Long, List<Batch>> batchesByOffering = batchRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .collect(Collectors.groupingBy(b -> b.getCourseOffering().getId()));

        List<CourseOfferingFacultySummaryDto> result = new java.util.ArrayList<>();
        for (CourseOffering offering : offerings) {
            List<String> names = List.of();
            int expected = 0;
            int assigned = 0;
            if (!resolveCohorts(offering).isEmpty()) {
                CourseOfferingSectionFacultyResponse resp = getForOffering(offering.getId());
                names = resp.sections().stream()
                    .map(SectionFacultyAssignment::facultyName)
                    .filter(java.util.Objects::nonNull)
                    .distinct().sorted().toList();
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

    /** Fills every currently-Unassigned Theory row in a term instance from the 13-or-however-many
     *  active faculty pool, ranked least-combined-load-first (existing Theory {@code
     *  theoryCredits} sum + existing Lab/Clinical coordinator count, so this pass balances against
     *  what everyone is ALREADY carrying, not from zero) -- never touches a row that already has a
     *  faculty, whether that was set by a human or a prior auto-assign run. Runs the same
     *  eligibility ({@link FacultyEligibility}) and elective-group-conflict gate {@link #upsert}/
     *  {@link #upsertForCohort} already enforce for a manual save, but deliberately allows exceeding
     *  a candidate's configured weekly/daily capacity as a last resort (tries every eligible
     *  candidate under capacity first) -- with a small faculty pool covering a full curriculum,
     *  real overload is the useful finding the Faculty Workload dashboard is for, not something to
     *  hide by leaving the offering unstaffed. Called both from {@link
     *  CohortRoomAllocationService#commit} (right after a Capacity Auto-Plan commit creates/splits
     *  this term's sections) and on demand from the Assign Faculty screen. */
    @Transactional
    public AutoAssignTheoryResult autoAssignTheory(Long termInstanceId) {
        List<Faculty> pool = facultyRepository.findByStatus(FacultyStatus.ACTIVE);
        Map<Long, Double> load = new LinkedHashMap<>();
        pool.forEach(f -> load.put(f.getId(), 0.0));

        for (CourseOfferingSectionFaculty sf : sectionFacultyRepository.findByCourseOffering_TermInstanceId(termInstanceId)) {
            Integer credits = sf.getCourseOffering().getSubject().getTheoryCredits();
            load.merge(sf.getFaculty().getId(), credits != null ? credits.doubleValue() : 1.0, Double::sum);
        }
        for (Batch batch : batchRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId)) {
            if (batch.getCoordinatorFaculty() != null) {
                load.merge(batch.getCoordinatorFaculty().getId(), 1.0, Double::sum);
            }
        }

        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .sorted(Comparator.comparing(CourseOffering::getSemesterNumber).thenComparing(o -> o.getSubject().getName()))
            .toList();

        int assignedCount = 0;
        List<String> skippedNames = new ArrayList<>();

        for (CourseOffering offering : offerings) {
            CourseOfferingSectionFacultyResponse resp = getForOffering(offering.getId());
            if (!resp.applicable()) {
                continue;
            }
            for (SectionFacultyAssignment row : resp.sections()) {
                if (row.facultyId() != null) {
                    continue;
                }
                Subject subject = offering.getSubject();
                double weight = subject.getTheoryCredits() != null ? subject.getTheoryCredits() : 1.0;

                List<Faculty> eligible = FacultyEligibility.eligibleFaculty(subject, pool).stream()
                    .sorted(Comparator.comparingDouble(f -> load.get(f.getId())))
                    .toList();

                Faculty chosen = null;
                Faculty capacityFallback = null;
                for (Faculty candidate : eligible) {
                    try {
                        if (row.cohortSectionId() != null) {
                            upsert(offering.getId(), row.cohortSectionId(), candidate.getId(), row.version());
                        } else {
                            upsertForCohort(offering.getId(), row.cohortId(), candidate.getId(), row.version());
                        }
                        chosen = candidate;
                        break;
                    } catch (TimetableConstraintViolationException e) {
                        boolean overCapacity = e.getViolations().stream().anyMatch(v -> "SECTION_FACULTY_OVER_CAPACITY".equals(v.code()));
                        if (overCapacity && capacityFallback == null) {
                            capacityFallback = candidate;
                        }
                    } catch (IllegalArgumentException e) {
                        // eligibility/section-liveness rejection -- try next candidate
                    }
                }

                if (chosen == null && capacityFallback != null) {
                    Faculty faculty = facultyRepository.getReferenceById(capacityFallback.getId());
                    CourseOfferingSectionFaculty forced = row.cohortSectionId() != null
                        ? new CourseOfferingSectionFaculty(offering, cohortSectionRepository.getReferenceById(row.cohortSectionId()), faculty)
                        : new CourseOfferingSectionFaculty(offering, cohortRepository.getReferenceById(row.cohortId()), faculty);
                    sectionFacultyRepository.save(forced);
                    chosen = capacityFallback;
                }

                if (chosen != null) {
                    load.merge(chosen.getId(), weight, Double::sum);
                    assignedCount++;
                } else {
                    skippedNames.add(subject.getName() + " (" + row.cohortName() + ")");
                }
            }
        }

        return new AutoAssignTheoryResult(assignedCount, skippedNames.size(), skippedNames);
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
        requireTermNotLocked(offering.getTermInstance());

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

    /** Batch-applies every {@link ConfirmFacultySubstitutionItem} the admin ticked on a Global
     *  Auto-Schedule run's faculty-substitution tips (see {@code
     *  GlobalAutoScheduleResult#facultySubstitutionTips}) as real, permanent reassignments — a
     *  Theory row's {@link #upsert}/{@link #upsertForCohort} for a {@code cohortSectionId}-only
     *  affected entry, or {@link BatchService#reassignCoordinator} for a {@code batchId} affected
     *  entry (a LAB/CLINICAL row, whose faculty-of-record lives on {@code Batch#coordinatorFaculty},
     *  not {@code CourseOfferingSectionFaculty} — confirming one of these against the Theory table
     *  could never find a matching row and would always look like an external conflict). Any row
     *  whose current state no longer matches what the tip captured for a reason *outside* this batch
     *  (someone reassigned it manually since the run finished, or any of {@link #upsert}'s own
     *  eligibility/capacity/elective-conflict gates reject it) throws and rolls back everything
     *  already applied earlier in this same call — never a partial batch against a genuine external
     *  conflict.
     *
     * <p>Only reassigns the exact rows/batches named in {@code item.affectedSections()} — never
     *  every row in the offering still on {@code originalFacultyId}. Two sibling sections of the
     *  same offering can independently fall back off the same original faculty onto two *different*
     *  substitutes in one run, producing two separate tips; reassigning "every row still on the
     *  original" would let the first confirmed tip sweep up the second tip's row too, then wrongly
     *  report the second tip as externally stale when it finds nothing left. Re-resolves each
     *  row's/batch's CURRENT faculty live (via {@link #getForOffering} for Theory, {@link
     *  #batchRepository} for Lab/Clinical) rather than trusting any version captured back when the
     *  run itself finished (real time passes between a run finishing and the admin clicking Submit)
     *  — a named row no longer held by {@code originalFacultyId} is treated as a genuine external
     *  conflict and rejected outright, since applying it blind could silently reassign a row someone
     *  already deliberately moved elsewhere.
     *
     * <p>One row/batch CAN legitimately appear in two different tips' {@code affectedSections}
     *  within a single run: a section's own sessions can split across two different substitutes
     *  because each session retries the fallback candidates independently against that day's
     *  availability (e.g. Monday's session lands on substitute X, Wednesday's on substitute Y, both
     *  because the original faculty was unavailable both times). A row/batch can only ever hold one
     *  permanent faculty-of-record, so at most one of those two tips can win it. {@code appliedRows}/
     *  {@code appliedBatches} track every row/batch this call itself has already reassigned; when a
     *  later tip in the same batch reaches one already in that map, that's this batch's own earlier
     *  write, not an external conflict — the first-ticked tip to reach it wins, and the later tip's
     *  claim on that one row/batch is skipped (counted in {@code sectionsSkipped}) rather than
     *  throwing and rolling back the whole batch.
     *
     * <p>{@link #checkCapacityForSubstitutions} validates every item's capacity up front, before
     *  anything below is applied, collecting every over-capacity substitute in one
     *  {@link com.cms.exception.SectionFacultyCapacityException} instead of {@link #upsert}'s own
     *  fail-on-first capacity gate (still in effect below as a safety net, not the primary path --
     *  it would only matter if the same substitute appears in two tips of one batch, where applying
     *  the first shifts their load enough to newly fail the second; that rarer case still throws
     *  and rolls back correctly, just via the older single-message {@code
     *  TimetableConstraintViolationException} rather than the richer per-tip one). */
    @Transactional
    public ConfirmFacultySubstitutionsResult confirmSubstitutions(List<ConfirmFacultySubstitutionItem> items) {
        checkCapacityForSubstitutions(items);

        int rowsReassigned = 0;
        int sectionsSkipped = 0;
        Map<String, Long> appliedRows = new HashMap<>();
        Map<Long, Long> appliedBatches = new HashMap<>();
        Set<Long> offeringIdsTouched = new LinkedHashSet<>();
        for (ConfirmFacultySubstitutionItem item : items) {
            CourseOffering offering = courseOfferingRepository.findById(item.courseOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + item.courseOfferingId()));

            Map<String, SectionFacultyAssignment> byRowKey = getForOffering(item.courseOfferingId()).sections().stream()
                .collect(Collectors.toMap(CourseOfferingSectionFacultyService::rowKey, s -> s, (a, b) -> a));

            for (SubstitutionAffectedSection affected : item.affectedSections()) {
                if (affected.batchId() != null) {
                    Batch batch = batchRepository.findById(affected.batchId()).orElse(null);
                    Long currentCoordinatorId = batch != null && batch.getCoordinatorFaculty() != null
                        ? batch.getCoordinatorFaculty().getId() : null;
                    boolean matchesOriginal = batch != null && item.originalFacultyId().equals(currentCoordinatorId);
                    if (!matchesOriginal) {
                        Long alreadyAppliedTo = appliedBatches.get(affected.batchId());
                        if (batch != null && alreadyAppliedTo != null && alreadyAppliedTo.equals(currentCoordinatorId)) {
                            sectionsSkipped++;
                            continue;
                        }
                        throw new IllegalStateException(offering.getSubject().getName()
                            + "'s Lab/Clinical coordinator was already changed by someone else since this run finished — reload and check the current assignment.");
                    }
                    batchService.reassignCoordinator(affected.batchId(), item.substituteFacultyId(), batch.getVersion());
                    appliedBatches.put(affected.batchId(), item.substituteFacultyId());
                    offeringIdsTouched.add(item.courseOfferingId());
                    rowsReassigned++;
                    continue;
                }

                String key = rowKey(affected.cohortId(), affected.cohortSectionId());
                SectionFacultyAssignment row = byRowKey.get(key);
                boolean matchesOriginal = row != null && item.originalFacultyId().equals(row.facultyId());
                if (!matchesOriginal) {
                    Long alreadyAppliedTo = appliedRows.get(key);
                    if (row != null && alreadyAppliedTo != null && alreadyAppliedTo.equals(row.facultyId())) {
                        sectionsSkipped++;
                        continue;
                    }
                    throw new IllegalStateException(offering.getSubject().getName()
                        + "'s Theory faculty was already changed by someone else since this run finished — reload and check the current assignment.");
                }
                if (row.cohortSectionId() != null) {
                    upsert(item.courseOfferingId(), row.cohortSectionId(), item.substituteFacultyId(), row.version());
                } else {
                    upsertForCohort(item.courseOfferingId(), row.cohortId(), item.substituteFacultyId(), row.version());
                }
                appliedRows.put(key, item.substituteFacultyId());
                offeringIdsTouched.add(item.courseOfferingId());
                rowsReassigned++;
            }
        }
        return new ConfirmFacultySubstitutionsResult(offeringIdsTouched.size(), rowsReassigned, sectionsSkipped);
    }

    /** Validates every ticked item's capacity *before* {@link #confirmSubstitutions} applies
     *  anything, collecting every over-capacity substitute in one pass instead of the apply loop's
     *  own fail-on-first-throw (which would only ever report one problem per Submit click, forcing
     *  a fix-one-resubmit-see-the-next-one loop). Checked using each item's first non-Lab/Clinical
     *  affected section (batchId null) -- capacity is a whole-faculty total, so every Theory-
     *  affected section within the same item necessarily agrees on whether this item's substitute
     *  goes over. An item with no such section (Lab/Clinical only) is never itself capacity-gated
     *  here -- matching {@link #confirmSubstitutions}'s own apply loop, which routes a Lab/Clinical
     *  reassignment straight to {@code batchService#reassignCoordinator} with no capacity check at
     *  all -- but its own Lab/Clinical hours still count toward {@code
     *  committedExtraHoursByFaculty} below, since a coordinator reassignment genuinely adds to that
     *  faculty's real demand once applied, it's just never blocked on it.
     *
     * <p>{@code committedExtraHoursByFaculty} accumulates each substitute's own hours across every
     *  item processed so far *in this same batch*, Lab/Clinical and Theory alike -- {@link
     *  TimetableGlobalAutoScheduleService#checkFacultyCapacityForSection}/{@code ForCohort} each
     *  independently re-query that faculty's CURRENT (pre-transaction) demand fresh from the DB, so
     *  checking every item in isolation would miss a substitute who only goes over capacity from
     *  the *combined* weight of several tips in the same Submit -- confirmed live against real
     *  Global Auto-Schedule output: a substitute who fills in across several subjects at once
     *  routinely picks up a MIX of Theory and Lab/Clinical fallbacks together, and the apply loop
     *  processes items (and so writes Lab/Clinical coordinator changes) in the same order this
     *  pre-pass does, so a later Theory item's own {@code upsert} capacity gate sees exactly the
     *  load this accumulation predicts. */
    private void checkCapacityForSubstitutions(List<ConfirmFacultySubstitutionItem> items) {
        List<SectionFacultyCapacityFailure> failures = new ArrayList<>();
        Map<Long, Double> committedExtraHoursByFaculty = new HashMap<>();
        for (ConfirmFacultySubstitutionItem item : items) {
            double itemHours = 0;
            FacultyCapacityCheckResult rawCheck = null;
            for (SubstitutionAffectedSection affected : item.affectedSections()) {
                if (affected.batchId() != null) {
                    itemHours += batchOfferingHours(item.courseOfferingId(), affected.batchId());
                } else if (rawCheck == null) {
                    rawCheck = affected.cohortSectionId() != null
                        ? timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(
                            item.courseOfferingId(), affected.cohortSectionId(), item.substituteFacultyId())
                        : timetableGlobalAutoScheduleService.checkFacultyCapacityForCohort(
                            item.courseOfferingId(), affected.cohortId(), item.substituteFacultyId());
                    itemHours += rawCheck.offeringHours();
                }
            }
            double alreadyCommitted = committedExtraHoursByFaculty.getOrDefault(item.substituteFacultyId(), 0.0);
            committedExtraHoursByFaculty.merge(item.substituteFacultyId(), itemHours, Double::sum);
            if (rawCheck == null) {
                continue; // Lab/Clinical-only item -- ungated, but its hours are now committed above.
            }

            FacultyCapacityCheckResult check = withExtraCommittedHours(rawCheck, alreadyCommitted);
            String message = buildOverCapacityMessage(item.courseOfferingId(), item.substituteFacultyId(), check);
            if (message != null) {
                Long alternateFacultyId = check.spreadLoad().isEmpty() ? null : check.spreadLoad().get(0).alternateFacultyId();
                failures.add(new SectionFacultyCapacityFailure(item.courseOfferingId(), item.substituteFacultyId(), message,
                    alternateFacultyId, check.suggestedMinDailySessions()));
            }
        }
        if (!failures.isEmpty()) {
            throw new SectionFacultyCapacityException(failures);
        }
    }

    /** This offering's Lab hours if {@code batchId} is a Lab batch, else its Clinical hours --
     *  mirrors the frontend's own {@code TeachingAssignmentDialogComponent#computeHourAdjustments}
     *  reasoning (a Lab-linked batch owes {@code offering.labHours}, a Clinical-linked batch owes
     *  {@code offering.clinicalHours}) since there's no {@code FacultyCapacityCheckResult}-shaped
     *  check for a coordinator reassignment to read this off of. 0 if the offering, batch, or its
     *  curriculum row can't be resolved. */
    private double batchOfferingHours(Long offeringId, Long batchId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId).orElse(null);
        Batch batch = batchRepository.findById(batchId).orElse(null);
        if (offering == null || batch == null || offering.getCurriculumSemesterCourse() == null) {
            return 0;
        }
        Integer hours = batch.getLab() != null
            ? offering.getCurriculumSemesterCourse().getLabHours()
            : offering.getCurriculumSemesterCourse().getClinicalHours();
        return hours != null ? hours : 0;
    }

    /** Re-derives {@code overCapacity}/{@code projectedTotalHours}/{@code suggestedMinDailyHours}
     *  as if {@code extraHours} (some earlier item's own offering-hours, for the same faculty in
     *  the same batch) had already landed on top of {@code check}'s own live snapshot -- everything
     *  else on {@code check} (capacity/tier/spreadLoad/etc.) is unaffected by that and passed
     *  through unchanged. A "NONE" tier (no cap configured at all) is never flagged over capacity,
     *  same rule {@link TimetableGlobalAutoScheduleService#checkFacultyCapacityForSection} itself
     *  applies -- mirrored here via {@code capacityTier}, since a null underlying {@code
     *  CapacityResolution} is exactly what produces "NONE" there. */
    private FacultyCapacityCheckResult withExtraCommittedHours(FacultyCapacityCheckResult check, double extraHours) {
        if (extraHours <= 0) {
            return check;
        }
        double adjustedProjected = check.projectedTotalHours() + extraHours;
        // Same floating-point safety margin as TimetableGlobalAutoScheduleService's own
        // CAPACITY_EPSILON (0.001h) -- not exposed across classes for a value this small.
        boolean overCapacity = !"NONE".equals(check.capacityTier()) && adjustedProjected > check.capacityHours() + 0.001;
        double suggestedMinDailyHours = overCapacity && check.workingDaysInTerm() > 0
            ? Math.ceil(adjustedProjected / check.workingDaysInTerm())
            : check.suggestedMinDailyHours();
        int suggestedMinDailySessions = overCapacity
            ? timetableGlobalAutoScheduleService.minDailySessionsFor(suggestedMinDailyHours)
            : check.suggestedMinDailySessions();
        return new FacultyCapacityCheckResult(overCapacity, check.currentDemandHours(), check.offeringHours(),
            adjustedProjected, check.capacityHours(), check.dailyCap(), check.capacityTier(),
            check.workingDaysInTerm(), suggestedMinDailyHours, suggestedMinDailySessions, check.spreadLoad());
    }

    private static String rowKey(SectionFacultyAssignment s) {
        return rowKey(s.cohortId(), s.cohortSectionId());
    }

    private static String rowKey(Long cohortId, Long cohortSectionId) {
        return cohortId + "|" + cohortSectionId;
    }

    /** Blanket rule shared with every other lifecycle guard in the app (see {@code
     *  CourseRegistrationServiceImpl#requireTermNotLocked}, {@code
     *  TimetableGenerationService#requireNotLocked}): a LOCKED term is frozen, full stop. Applies
     *  to both {@link #upsert}/{@link #upsertForCohort} directly (manual Reassign) and, through
     *  them, to {@link #autoAssignTheory}/{@link #confirmSubstitutions} -- there is no separate
     *  bypass for an automated or batch-confirmed reassignment. */
    private void requireTermNotLocked(TermInstance term) {
        if (term.getStatus() == TermInstanceStatus.LOCKED) {
            throw new IllegalArgumentException("Cannot reassign faculty -- this term is locked.");
        }
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
        requireTermNotLocked(offering.getTermInstance());

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
        raiseIfOverCapacity(offeringId, facultyId,
            timetableGlobalAutoScheduleService.checkFacultyCapacityForSection(offeringId, cohortSectionId, facultyId));
    }

    /** Cohort-scoped counterpart of {@link #requireWithinCapacityForSection}, via {@link
     *  TimetableGlobalAutoScheduleService#checkFacultyCapacityForCohort}. */
    private void requireWithinCapacityForCohort(Long offeringId, Long cohortId, Long facultyId, Long previousFacultyId) {
        if (facultyId == null || facultyId.equals(previousFacultyId)) {
            return;
        }
        raiseIfOverCapacity(offeringId, facultyId,
            timetableGlobalAutoScheduleService.checkFacultyCapacityForCohort(offeringId, cohortId, facultyId));
    }

    /** Throwing wrapper around {@link #buildOverCapacityMessage} for the single-item manual
     *  Reassign path ({@link #requireWithinCapacityForSection}/{@link
     *  #requireWithinCapacityForCohort}) -- {@link #checkCapacityForSubstitutions} calls the
     *  message-builder directly instead, since it needs to collect every failure across a whole
     *  batch rather than throw on the first. */
    private void raiseIfOverCapacity(Long offeringId, Long facultyId, FacultyCapacityCheckResult check) {
        String message = buildOverCapacityMessage(offeringId, facultyId, check);
        if (message != null) {
            throw new TimetableConstraintViolationException(List.of(
                new ConstraintViolation("SECTION_FACULTY_OVER_CAPACITY", message)));
        }
    }

    /** Null when {@code check} isn't actually over capacity. {@code offeringId}/{@code facultyId}
     *  exist only to name the assignment in the message ("Assigning X to Y would put them at...")
     *  -- the capacity numbers themselves come entirely from {@code check}, already computed by
     *  the caller. Without naming the subject and faculty, this message was ambiguous in any
     *  context showing more than one pending assignment at once (e.g. the Global Auto-Schedule
     *  "confirm substitutions" flyout, which can list several tips at a time) -- "this assignment"
     *  / "them" gave no way to tell which one had actually failed. */
    private String buildOverCapacityMessage(Long offeringId, Long facultyId, FacultyCapacityCheckResult check) {
        if (!check.overCapacity()) {
            return null;
        }
        String facultyName = facultyRepository.findById(facultyId).map(Faculty::getFullName).orElse("This faculty member");
        String subjectName = courseOfferingRepository.findById(offeringId)
            .map(offering -> offering.getSubject().getName())
            .orElse("this subject");
        // Term-total demand/capacity stay in hours -- that's curriculum workload's own native unit
        // (Theory/Lab/Clinical hours are defined per curriculum row independent of period length),
        // and nothing here is a field an admin types a term total into. Every *daily* figure below
        // is periods, not hours, though: Raise Cap's own field (Faculty's plannedDailySessionsOverride)
        // is a period COUNT, and stating a daily target in hours instead left the admin to convert it
        // themselves against each Period's real (non-1-hour) duration -- a plausible-looking round
        // number (e.g. entering "6" when periods run 50 minutes, 0.83h each) could land short of the
        // real target instead of clearing it. Naming the period count directly means the number
        // that's actually typed into that field is stated explicitly, never left implied via hours.
        int dailyCapPeriods = timetableGlobalAutoScheduleService.minDailySessionsFor(check.dailyCap());
        StringBuilder message = new StringBuilder()
            .append("Assigning ").append(facultyName).append(" to ").append(subjectName)
            .append(" would put them at ").append(formatHours(check.projectedTotalHours()))
            .append(" against a capacity of ").append(formatHours(check.capacityHours()))
            .append(" (").append(dailyCapPeriods).append(" period(s)/day) — raise their cap to at least ")
            .append(check.suggestedMinDailySessions()).append(" period(s)/day");
        if (!check.spreadLoad().isEmpty()) {
            var alt = check.spreadLoad().get(0);
            message.append(", or assign ").append(alt.alternateFacultyName())
                .append(" instead (").append(formatHours(alt.alternateSpareCapacityHours())).append(" spare capacity)");
        }
        return message.toString();
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
