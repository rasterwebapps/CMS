package com.cms.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.Room;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyAbsenceRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

/**
 * R3 Phase 5 — the "staff what's already placed" pass that follows the Phase 4 skeleton builder.
 * Scoped per {@link com.cms.model.TermInstance} (not per subject like the skeleton builder)
 * because its job is finding everything still missing before that term's whole draft can be
 * approved, across every subject at once.
 *
 * <p>Conflict checking here is deliberately narrower than {@link ClassScheduleService#checkConflicts}:
 * placement-time concerns (does this subject already have something at this exact day/period)
 * were already handled by {@link TimetableSkeletonService#placeCell} — staffing only needs to
 * guard the two genuinely shared, limited resources being assigned right now: the faculty member
 * and the room. Checked against both PUBLISHED (live) rows and other already-staffed DRAFT rows
 * in the same term, since two different subjects' skeletons can double-book a faculty/room before
 * either is ever published.
 */
@Service
@Transactional(readOnly = true)
public class TimetableStaffingService {

    private final ClassScheduleRepository classScheduleRepository;
    private final FacultyRepository facultyRepository;
    private final ClassroomRepository classroomRepository;
    private final BatchRepository batchRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final CohortRoomAllocationRepository cohortRoomAllocationRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final RotationResolverService rotationResolverService;
    private final TimetableBlockedPeriodChecker blockedPeriodChecker;
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final FacultyAbsenceRepository facultyAbsenceRepository;
    private final SystemConfigurationService systemConfigurationService;

    public TimetableStaffingService(ClassScheduleRepository classScheduleRepository,
                                     FacultyRepository facultyRepository,
                                     ClassroomRepository classroomRepository,
                                     BatchRepository batchRepository,
                                     CourseRegistrationRepository courseRegistrationRepository,
                                     StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                     CohortRoomAllocationRepository cohortRoomAllocationRepository,
                                     CohortSectionRepository cohortSectionRepository,
                                     RotationResolverService rotationResolverService,
                                     TimetableBlockedPeriodChecker blockedPeriodChecker,
                                     FacultyAvailabilityRepository facultyAvailabilityRepository,
                                     FacultyAbsenceRepository facultyAbsenceRepository,
                                     SystemConfigurationService systemConfigurationService) {
        this.classScheduleRepository = classScheduleRepository;
        this.facultyRepository = facultyRepository;
        this.classroomRepository = classroomRepository;
        this.batchRepository = batchRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.cohortRoomAllocationRepository = cohortRoomAllocationRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.rotationResolverService = rotationResolverService;
        this.blockedPeriodChecker = blockedPeriodChecker;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.facultyAbsenceRepository = facultyAbsenceRepository;
        this.systemConfigurationService = systemConfigurationService;
    }

    public List<UnstaffedCellResponse> getUnstaffedCells(Long termInstanceId) {
        return classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT)
            .stream()
            .filter(cs -> cs.getFaculty() == null)
            .map(this::toResponse)
            .toList();
    }

    /** R3.4 — the faculty-side guards below are independent of each other, so all of them run and
     *  every failure is collected into one {@link TimetableConstraintViolationException} rather
     *  than throwing on the first one found: fixing a blocked period only to discover the faculty
     *  was also double-booked (on a second, separate attempt) was a frustrating fix-one-resubmit
     *  loop. Room resolution and its own checks (room-free/capacity-fit) only run once the
     *  faculty side is entirely clean — committed-room resolution ({@code requireCommitted*}/
     *  {@code requireRequestedClassroom}) is a fail-fast setup-gap check, not a constraint to
     *  collect, and every check after it needs a resolved room's identity to even run; skipping it
     *  whenever a faculty-side violation already exists also keeps this from probing Capacity
     *  Planner room commitments that were never going to matter for a rejected attempt. */
    @Transactional
    public UnstaffedCellResponse staffCell(Long classScheduleId, StaffingAssignmentRequest request) {
        ClassSchedule cs = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (cs.getStatus() != ClassScheduleStatus.DRAFT) {
            throw new LifecycleConflictException(
                "Only a draft skeleton cell can be staffed here.",
                "CELL_NOT_DRAFT", "ClassSchedule", classScheduleId, null);
        }

        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));
        ClassScheduleService.requireEligibleFaculty(cs.getSubject(), faculty, cs.getFaculty());

        // OC-127 periodSpan: a multi-period session's rows are staffed as one atomic unit -- same
        // faculty, same resolved room, every sibling's own checks must all pass before any of them
        // are applied. Ordinary single-period cells are just a one-element group.
        List<ClassSchedule> group = cs.getSessionGroupId() != null
            ? classScheduleRepository.findBySessionGroupIdOrderByPeriod_PeriodOrderAsc(cs.getSessionGroupId())
            : List.of(cs);

        List<ConstraintViolation> violations = new ArrayList<>();
        List<CellStaging> stagings = new ArrayList<>();
        for (ClassSchedule member : group) {
            CellStaging staging = stageCell(member, faculty, request);
            violations.addAll(staging.violations());
            stagings.add(staging);
        }

        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        for (int i = 0; i < group.size(); i++) {
            ClassSchedule member = group.get(i);
            stagings.get(i).applyRoom().run();
            member.setFaculty(faculty);
            classScheduleRepository.save(member);
        }
        return toResponse(cs);
    }

    private record CellStaging(List<ConstraintViolation> violations, Runnable applyRoom) {}

    /** Faculty-then-room-resolution ordering guarantee for exactly one row: both passes funnel
     *  through {@link #validateAssignment} (OC-127), kept as two calls (not one) specifically because
     *  room resolution can throw ({@code STAFFING_VENUE_NOT_COMMITTED}) and must never run before the
     *  faculty side is known clean. Reused per-sibling by a periodSpan group (OC-127) so every row's
     *  own checks are collected before any of them are applied. */
    private CellStaging stageCell(ClassSchedule cs, Faculty faculty, StaffingAssignmentRequest request) {
        LocalTime start = cs.getPeriod().getStartTime();
        LocalTime end = cs.getPeriod().getEndTime();
        DayOfWeek day = cs.getDayOfWeek();

        List<ConstraintViolation> violations = new ArrayList<>(
            validateAssignment(cs, day, start, end, faculty, null, null, null, null).violations());

        Runnable applyRoom = null;
        if (violations.isEmpty()) {
            switch (cs.getSessionType()) {
                case THEORY -> {
                    Classroom classroom = isElectiveOffering(cs)
                        ? requireRequestedClassroom(request)
                        : requireCommittedTheoryClassroom(cs);
                    violations.addAll(validateAssignment(cs, day, start, end, null, null,
                        new RoomCheckSpec(ClassSessionType.THEORY, classroom.getId(), classroom.getRoom(), RoomMode.STRICT), null, null).violations());
                    checkCapacityFit(cs, classroom.getCapacity()).ifPresent(violations::add);
                    applyRoom = () -> cs.setClassroom(classroom);
                }
                case LAB -> {
                    Lab lab = requireCommittedLab(cs);
                    violations.addAll(validateAssignment(cs, day, start, end, null, null,
                        new RoomCheckSpec(ClassSessionType.LAB, lab.getId(), lab.getRoom(), RoomMode.STRICT), null, null).violations());
                    checkCapacityFit(cs, lab.getCapacity()).ifPresent(violations::add);
                    applyRoom = () -> cs.setLab(lab);
                }
                case CLINICAL -> {
                    ClinicalVenue venue = requireCommittedClinicalVenue(cs);
                    violations.addAll(validateAssignment(cs, day, start, end, null, null,
                        new RoomCheckSpec(ClassSessionType.CLINICAL, venue.getId(), venue.getRoom(), RoomMode.STRICT), null, null).violations());
                    checkCapacityFit(cs, venue.getCapacity()).ifPresent(violations::add);
                    applyRoom = () -> cs.setClinicalVenue(venue);
                }
            }
        }
        return new CellStaging(violations, applyRoom);
    }

    /** OC-127 — the one shared validation entry point {@link TimetableSwapService#evaluateSlot} and
     *  {@link FacultySessionSwapService} now funnel through instead of each reimplementing their own
     *  combination of the checks below. {@code faculty}/{@code roomCheck}/{@code audienceId} are each
     *  independently optional — null skips that whole category — which is what lets one method serve
     *  {@code staffCell}'s faculty-then-room-resolution ordering (two calls, see above; room
     *  resolution can throw and must never run before the faculty side is known clean), a staff-swap's
     *  faculty-only recheck (one call, no room), and a draft-swap's full faculty+room+audience recheck
     *  (one call) alike. Every check here has always been non-throwing/collect-all (OC-113) except the
     *  room/audience scan, which is new here — {@code checkRoomFree}/{@code checkFacultyFree}'s
     *  existing overloads are deliberately left untouched (still used directly by
     *  {@code SpecialClassRequestService}, {@code RoomRelocationService},
     *  {@code TimetableConflictInspectorService}, and {@code TimetableSkeletonService#moveCell}, none
     *  of which need the extra swap-partner/audience/alsoExcludeId behavior below). {@code date} is
     *  the one genuinely optional argument here (unlike the others, which are simply "not applicable"
     *  when null): only single-date callers like {@code FacultySessionSwapService} know it — the
     *  recurring-placement callers ({@code stageCell}, {@code TimetableSwapService}, which move a
     *  DRAFT row's day/period rather than trading a specific calendar date) pass null and skip the
     *  {@link com.cms.model.FacultyAbsence} check, since a one-off date absence has no bearing on a
     *  weekly recurring slot. */
    public AssignmentValidationResult validateAssignment(ClassSchedule cs, DayOfWeek day, LocalTime start, LocalTime end,
                                                           Faculty faculty, Long alsoExcludeId,
                                                           RoomCheckSpec roomCheck, Long audienceId, LocalDate date) {
        List<ConstraintViolation> violations = new ArrayList<>();
        checkBlocked(day, start, end, cs.getTermInstance()).ifPresent(violations::add);

        if (faculty != null) {
            checkFacultyAvailable(faculty.getId(), day, start, end, date).ifPresent(violations::add);
            if (date != null) {
                checkFacultyAbsent(faculty.getId(), date).ifPresent(violations::add);
            }
            checkFacultyFree(faculty.getId(), cs, day, start, end, alsoExcludeId).ifPresent(violations::add);
            violations.addAll(checkWithinWorkloadCaps(faculty, cs, day, start, end));
        }

        ClassSchedule swapPartner = null;
        if (roomCheck != null) {
            RoomAudienceScanResult scan = scanRoomAndAudience(cs, day, start, end, roomCheck, audienceId, alsoExcludeId);
            violations.addAll(scan.violations());
            swapPartner = scan.swapPartnerOccupant();
        }

        return new AssignmentValidationResult(violations, swapPartner);
    }

    public record RoomCheckSpec(ClassSessionType type, Long venueId, Room physicalRoom, RoomMode mode) {}

    public enum RoomMode {
        /** Any occupant at all is a conflict — staffing/group-staffing use. */
        STRICT,
        /** Exactly one same-room DRAFT occupant is offered back as a swap partner instead of a
         *  violation; a PUBLISHED occupant or a second DRAFT occupant is still a violation. */
        ALLOW_SINGLE_DRAFT_SWAP_PARTNER
    }

    public record AssignmentValidationResult(List<ConstraintViolation> violations, ClassSchedule swapPartnerOccupant) {
        public boolean isValid() {
            return violations.isEmpty();
        }
    }

    private record RoomAudienceScanResult(List<ConstraintViolation> violations, ClassSchedule swapPartnerOccupant) {}

    /** One pass over every PUBLISHED+DRAFT row overlapping this day/time, checking room and (when
     *  {@code audienceId} is given) audience conflicts together — a genuinely new combined
     *  implementation (not a reuse of {@link #checkRoomFree}, which has no swap-partner/audience
     *  concept and stays frozen for its own remaining direct callers). {@code alsoExcludeId} leaves
     *  an additional row (besides {@code cs} itself, always excluded via {@code findOverlapping}'s
     *  own exclude param) out of the scan — the reverse-swap partner's own still-unmutated row, which
     *  must never count as a blocker against itself. Folds in the physical-room cross-check {@link
     *  #conflictsOnRoom} already does for staffing (two different venues mapped to the same physical
     *  {@link Room}) — previously invisible to {@link TimetableSwapService}, which only compared
     *  virtual room ids. */
    private RoomAudienceScanResult scanRoomAndAudience(ClassSchedule cs, DayOfWeek day, LocalTime start, LocalTime end,
                                                         RoomCheckSpec roomCheck, Long audienceId, Long alsoExcludeId) {
        List<ConstraintViolation> violations = new ArrayList<>();
        ClassSchedule swapPartner = null;
        boolean roomConflictAlreadyFlagged = false;
        String roomViolationCode = roomCheck.mode() == RoomMode.STRICT ? "STAFFING_ROOM_CONFLICT" : "SWAP_ROOM_CONFLICT";

        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                day, cs.getTermInstance().getId(), start, end, status, cs.getId());
            for (ClassSchedule other : overlapping) {
                if (alsoExcludeId != null && other.getId().equals(alsoExcludeId)) {
                    continue;
                }
                if (!roomConflictAlreadyFlagged && conflictsOnRoom(other, roomCheck.type(), roomCheck.venueId(), roomCheck.physicalRoom())) {
                    if (roomCheck.mode() == RoomMode.ALLOW_SINGLE_DRAFT_SWAP_PARTNER
                            && other.getStatus() == ClassScheduleStatus.DRAFT && swapPartner == null) {
                        swapPartner = other;
                    } else {
                        violations.add(new ConstraintViolation(roomViolationCode,
                            "This room is already occupied by another session at this exact day and time."));
                        roomConflictAlreadyFlagged = true;
                    }
                }
                if (audienceId != null && other.getSessionType() == cs.getSessionType()
                        && audienceId.equals(resolveAudienceId(other))) {
                    violations.add(new ConstraintViolation("SWAP_AUDIENCE_CONFLICT",
                        "This slot is already occupied by another session for the same cohort/batch."));
                }
            }
        }
        return new RoomAudienceScanResult(violations, swapPartner);
    }

    /** THEORY audience is batch-scoped when a section was picked (R3 Phase 3), falling back to the
     *  whole-cohort courseOffering for un-sectioned rows; LAB/CLINICAL are always batch-scoped.
     *  Package-static so {@link TimetableSwapService} can resolve the *target* audience id it passes
     *  into {@link #validateAssignment} with the same logic used here for every candidate row. */
    static Long resolveAudienceId(ClassSchedule cs) {
        if (cs.getSessionType() == ClassSessionType.THEORY) {
            return cs.getBatch() != null ? cs.getBatch().getId()
                : (cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null);
        }
        return cs.getBatch() != null ? cs.getBatch().getId() : null;
    }

    /** Non-throwing: returns a violation if this cell falls in a recurring institutional lock
     *  (lunch, assembly, sports) or a holiday-derived one-off block, backed by the same shared
     *  {@link TimetableBlockedPeriodChecker} {@link TimetableSkeletonService#placeCell} and {@link
     *  TimetableSwapService} use — re-checked here separately (not just at placement time) since a
     *  skeleton cell can sit unstaffed for a while and a block could be added/changed in between. */
    private Optional<ConstraintViolation> checkBlocked(DayOfWeek dayOfWeek, LocalTime start, LocalTime end, TermInstance termInstance) {
        return blockedPeriodChecker.blockReason(dayOfWeek, start, end, termInstance.getStartDate(), termInstance.getEndDate())
            .map(reason -> new ConstraintViolation("STAFFING_PERIOD_BLOCKED", "This day and period is blocked: " + reason));
    }

    /** LAB rooms are no longer a free pick here — the venue was already decided and
     *  capacity-checked once, in Cohort Room Allocation (Capacity Planner), when this batch was
     *  created. Re-picking a different lab at staffing time is exactly the silent-divergence gap
     *  that let one batch's students end up with no traceable room; a null venue means this batch
     *  predates that flow (or was created outside it) and must be committed there first.
     *
     *  <p>A rotation-governed cell has no fixed batch at all ({@link ClassSchedule#getBatch()} is
     *  null once linked into a Rotation Group) — the venue there is resolved instead from any one
     *  of the batches rotating through this slot, since RotationGroupService already validated
     *  they all share the exact same venue. Faculty staffing is a one-time assignment to the
     *  slot itself; which physical group actually occupies it is resolved per-date separately. */
    private Lab requireCommittedLab(ClassSchedule cs) {
        Lab lab = resolveBatchForVenue(cs).map(Batch::getLab).orElse(null);
        if (lab == null) {
            throw new LifecycleConflictException(
                "This batch has no Lab committed in Cohort Room Allocation — commit it in Capacity Planner before staffing.",
                "STAFFING_VENUE_NOT_COMMITTED", "ClassSchedule", cs.getId(), null);
        }
        return lab;
    }

    /** Mirrors {@link #requireCommittedLab} for CLINICAL sessions. */
    private ClinicalVenue requireCommittedClinicalVenue(ClassSchedule cs) {
        ClinicalVenue venue = resolveBatchForVenue(cs).map(Batch::getClinicalVenue).orElse(null);
        if (venue == null) {
            throw new LifecycleConflictException(
                "This batch has no Clinical Venue committed in Cohort Room Allocation — commit it in Capacity Planner before staffing.",
                "STAFFING_VENUE_NOT_COMMITTED", "ClassSchedule", cs.getId(), null);
        }
        return venue;
    }

    private Optional<Batch> resolveBatchForVenue(ClassSchedule cs) {
        if (cs.getBatch() != null) {
            return Optional.of(cs.getBatch());
        }
        return rotationResolverService.anyAssignmentForSlot(cs.getId()).map(RotationMemberAssignment::getBatch);
    }

    /** Electives have no single owning cohort by design (that's the whole point of an elective —
     *  students from different cohorts/sections opt in), so they're exempt from the Theory
     *  hard-lock below and keep a free classroom pick, mirroring Capacity Planner's own exclusion
     *  of electives from Cohort Room Allocation. */
    private boolean isElectiveOffering(ClassSchedule cs) {
        CourseOffering offering = cs.getCourseOffering();
        return offering != null && offering.getCurriculumSemesterCourse() != null
            && Boolean.TRUE.equals(offering.getCurriculumSemesterCourse().getIsElective());
    }

    private Classroom requireRequestedClassroom(StaffingAssignmentRequest request) {
        if (request.classroomId() == null) {
            throw new IllegalArgumentException("A classroom is required to staff a THEORY session");
        }
        return classroomRepository.findById(request.classroomId())
            .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + request.classroomId()));
    }

    /** Non-elective Theory sessions are hard-locked the same way LAB/CLINICAL is, but Theory has
     *  no direct batch/venue FK to read — a whole cohort attends together, not a sub-group batch.
     *  Resolved by finding the single cohort enrolled in this offering's term+semester (this
     *  1:1 mapping already backs Capacity Planner's own offering filter) and reading that
     *  cohort's committed Theory room from Cohort Room Allocation. Ambiguous (more than one
     *  cohort sharing a semester) or missing resolution never guesses — it's surfaced as
     *  "not committed" rather than silently picking a room. */
    private Classroom requireCommittedTheoryClassroom(ClassSchedule cs) {
        return resolveCommittedTheoryClassroom(cs)
            .orElseThrow(() -> new LifecycleConflictException(
                "This cohort has no Theory room committed in Cohort Room Allocation, or this row predates "
                    + "section-scoped placement and its cohort has multiple Theory sections committed — commit an "
                    + "allocation in Capacity Planner before staffing, or re-place this session via Skeleton Builder "
                    + "so it carries a specific section.",
                "STAFFING_VENUE_NOT_COMMITTED", "ClassSchedule", cs.getId(), null));
    }

    /** Since R3.2, a Theory {@code ClassSchedule} row placed via Skeleton Builder carries its own
     *  {@link CohortSection} directly (V368) whenever the cohort's commit was sectioned at
     *  placement time — that's checked first and, when present, resolves the room with no
     *  ambiguity at all. Rows placed before that column existed (or via any other path) fall back
     *  to the original enrollment-inference chain, which only auto-resolves a room when the
     *  cohort's commit is unsectioned (exactly one active {@link CohortSection}) -- a sectioned
     *  cohort has more than one Theory room and, without the direct link, no way to disambiguate
     *  which section that particular row belongs to, so the fallback deliberately returns empty
     *  rather than guessing. */
    private Optional<Classroom> resolveCommittedTheoryClassroom(ClassSchedule cs) {
        if (cs.getCohortSection() != null) {
            return Optional.of(cs.getCohortSection().getClassroom());
        }

        CourseOffering offering = cs.getCourseOffering();
        if (offering == null) {
            return Optional.empty();
        }
        List<StudentTermEnrollment> enrollments = studentTermEnrollmentRepository
            .findByTermInstanceIdAndSemesterNumber(offering.getTermInstance().getId(), offering.getSemesterNumber());
        Set<Long> cohortIds = enrollments.stream()
            .map(e -> e.getCohort().getId())
            .collect(Collectors.toSet());
        if (cohortIds.size() != 1) {
            return Optional.empty();
        }
        Optional<CohortRoomAllocation> allocation = cohortRoomAllocationRepository
            .findByCohortIdAndTermInstanceIdAndStatus(
                cohortIds.iterator().next(), offering.getTermInstance().getId(), CohortRoomAllocationStatus.COMMITTED);
        if (allocation.isEmpty()) {
            return Optional.empty();
        }
        List<CohortSection> sections = cohortSectionRepository.findByCohortRoomAllocationIdAndIsActiveTrue(allocation.get().getId());
        return sections.size() == 1 ? Optional.of(sections.get(0).getClassroom()) : Optional.empty();
    }

    /** Non-throwing: returns a violation if this faculty member has declared themselves
     *  unavailable at this slot (leave, external duty, a visiting lecturer's fixed weekly window,
     *  etc) — the same {@link FacultyAvailability} check {@link TimetableSwapService} and {@code
     *  FacultySessionSwapService} already apply, closing the one staffing path that skipped it. A
     *  faculty member with no rows in this table is assumed fully available.
     *
     *  <p>{@code date}, when known (single-date callers only — mirrors {@link #checkFacultyAbsent}'s
     *  own null-means-recurring convention), narrows a date-ranged {@link FacultyAvailability} row
     *  to only actually block dates that fall inside its range. When {@code date} is null (recurring
     *  placement, which has no single date to compare against), every matching row still blocks
     *  regardless of whether it's ranged or indefinite — deliberately conservative, since a
     *  date-ranged block can't be perfectly represented against a term-long recurring slot; the
     *  admin resolves any real gap once concrete dates are involved (Staff Swap). */
    Optional<ConstraintViolation> checkFacultyAvailable(Long facultyId, DayOfWeek dayOfWeek, LocalTime start, LocalTime end, LocalDate date) {
        List<FacultyAvailability> blocks = facultyAvailabilityRepository.findOverlapping(facultyId, dayOfWeek, start, end);
        if (date != null) {
            blocks = blocks.stream()
                .filter(b -> b.getStartDate() == null
                    || (!date.isBefore(b.getStartDate()) && !date.isAfter(b.getEndDate())))
                .toList();
        }
        if (blocks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConstraintViolation("STAFFING_FACULTY_UNAVAILABLE",
            "This faculty member is unavailable at this day and time: " + blocks.get(0).getReason()));
    }

    /** Non-throwing: returns a violation if this faculty member has a {@link
     *  com.cms.model.FacultyAbsence} marked for this exact calendar date — a one-off, date-specific
     *  check distinct from {@link #checkFacultyAvailable}'s recurring weekly block. Only meaningful
     *  to callers that know a specific date (single-date staff swaps); recurring placement has no
     *  date to check against. */
    Optional<ConstraintViolation> checkFacultyAbsent(Long facultyId, LocalDate date) {
        if (!facultyAbsenceRepository.existsByFacultyIdAndAbsenceDate(facultyId, date)) {
            return Optional.empty();
        }
        return Optional.of(new ConstraintViolation("STAFFING_FACULTY_ABSENT",
            "This faculty member is marked absent on " + date + "."));
    }

    /** Non-throwing: returns a violation if this faculty member is already committed elsewhere at
     *  this exact day/time — checked against both live PUBLISHED rows and other already-staffed
     *  DRAFT rows in the same term, excluding this cell itself. {@code day} is passed explicitly
     *  rather than read from {@code cs.getDayOfWeek()} so {@link TimetableSkeletonService#moveCell}
     *  can re-check a *target* day for an already-staffed cell without first mutating it. */
    Optional<ConstraintViolation> checkFacultyFree(Long facultyId, ClassSchedule cs, DayOfWeek day, LocalTime start, LocalTime end) {
        return checkFacultyFree(facultyId, cs, day, start, end, null);
    }

    /** Overload adding a second excluded row (besides {@code cs} itself) — used by {@link
     *  #validateAssignment} for the reverse-swap-partner case, where that row must never count as a
     *  blocker against itself. {@code alsoExcludeId} null reproduces the 5-arg overload above exactly. */
    private Optional<ConstraintViolation> checkFacultyFree(Long facultyId, ClassSchedule cs, DayOfWeek day, LocalTime start,
                                                            LocalTime end, Long alsoExcludeId) {
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                day, cs.getTermInstance().getId(), start, end, status, cs.getId());
            boolean conflict = overlapping.stream()
                .filter(other -> alsoExcludeId == null || !other.getId().equals(alsoExcludeId))
                .anyMatch(other -> other.getFaculty() != null && other.getFaculty().getId().equals(facultyId));
            if (conflict) {
                return Optional.of(new ConstraintViolation("STAFFING_FACULTY_CONFLICT",
                    "This faculty member is already scheduled for another session at this exact day and time."));
            }
        }
        return Optional.empty();
    }

    /** Tuple-shaped overload of {@link #checkFacultyFree(Long, ClassSchedule, DayOfWeek, LocalTime, LocalTime)}
     *  for BR-55 special classes, which have no backing {@link ClassSchedule} to read a term/exclude-id
     *  from — {@code excludeClassScheduleId} is nullable (a special class never excludes an existing
     *  recurring row). Checks only against the recurring weekly template; the caller is responsible
     *  for separately checking against other special classes, which this can't see (they have no
     *  ClassSchedule row for {@code findOverlapping} to match). */
    Optional<ConstraintViolation> checkFacultyFree(Long facultyId, Long termInstanceId, Long excludeClassScheduleId,
                                                    DayOfWeek day, LocalTime start, LocalTime end) {
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                day, termInstanceId, start, end, status, excludeClassScheduleId);
            boolean conflict = overlapping.stream()
                .anyMatch(other -> other.getFaculty() != null && other.getFaculty().getId().equals(facultyId));
            if (conflict) {
                return Optional.of(new ConstraintViolation("STAFFING_FACULTY_CONFLICT",
                    "This faculty member is already scheduled for another session at this exact day and time."));
            }
        }
        return Optional.empty();
    }

    /** Non-throwing: returns every exceeded cap (daily/weekly/continuous can all be exceeded at
     *  once), rather than stopping at the first — see the class-level note on why every check here
     *  collects instead of throws. Hard-blocks staffing a faculty member past whatever
     *  daily/weekly/continuous-hours caps the admin has configured in System Configuration
     *  (category {@code TIMETABLE}) — a blank or non-positive/unparseable cap value is treated as
     *  "no cap", never as an error, since these ship blank by default and a malformed manual edit
     *  must never crash staffing. "Weekly" sums every session this faculty has in the term
     *  regardless of day, since the whole timetable is one recurring weekly template with no
     *  separate calendar-week concept anywhere in {@link ClassSchedule}. Checked against both
     *  PUBLISHED and other already-staffed DRAFT rows, excluding this cell itself so a re-staff
     *  (same cell, different or same faculty) doesn't double-count its own slot. {@code day} is
     *  passed explicitly for the same reason as {@link #checkFacultyFree}. */
    List<ConstraintViolation> checkWithinWorkloadCaps(Faculty faculty, ClassSchedule cs, DayOfWeek day, LocalTime start, LocalTime end) {
        Optional<Double> dailyCap = resolveDailyCap(faculty);
        Optional<Double> weeklyCap = resolveWeeklyCap(faculty);
        Optional<Double> continuousCap = resolveContinuousCap(faculty);
        if (dailyCap.isEmpty() && weeklyCap.isEmpty() && continuousCap.isEmpty()) {
            return List.of();
        }

        double newSessionHours = Duration.between(start, end).toMinutes() / 60.0;
        List<ClassSchedule> otherSessions = Stream.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)
            .flatMap(status -> classScheduleRepository
                .findByTermInstanceIdAndStatusAndFacultyId(cs.getTermInstance().getId(), status, faculty.getId())
                .stream())
            .filter(other -> !other.getId().equals(cs.getId()))
            .filter(other -> other.getPeriod() != null)
            .toList();

        List<ConstraintViolation> violations = new ArrayList<>();

        if (dailyCap.isPresent()) {
            double dailyHours = otherSessions.stream()
                .filter(other -> other.getDayOfWeek() == day)
                .mapToDouble(other -> sessionHours(other.getPeriod()))
                .sum() + newSessionHours;
            if (dailyHours > dailyCap.get()) {
                violations.add(new ConstraintViolation("STAFFING_WORKLOAD_DAILY_CAP_EXCEEDED",
                    "Staffing this session would put this faculty member at " + formatHours(dailyHours)
                        + " hours today, over the configured daily cap of " + formatHours(dailyCap.get()) + " hours."));
            }
        }

        if (weeklyCap.isPresent()) {
            double weeklyHours = otherSessions.stream()
                .mapToDouble(other -> sessionHours(other.getPeriod()))
                .sum() + newSessionHours;
            if (weeklyHours > weeklyCap.get()) {
                violations.add(new ConstraintViolation("STAFFING_WORKLOAD_WEEKLY_CAP_EXCEEDED",
                    "Staffing this session would put this faculty member at " + formatHours(weeklyHours)
                        + " hours this week, over the configured weekly cap of " + formatHours(weeklyCap.get()) + " hours."));
            }
        }

        if (continuousCap.isPresent()) {
            double continuousHours = continuousChainHours(otherSessions, day, start, end);
            if (continuousHours > continuousCap.get()) {
                violations.add(new ConstraintViolation("STAFFING_WORKLOAD_CONTINUOUS_CAP_EXCEEDED",
                    "Staffing this session would put this faculty member into a " + formatHours(continuousHours)
                        + "-hour unbroken run, over the configured continuous-hours cap of "
                        + formatHours(continuousCap.get()) + " hours."));
            }
        }

        return violations;
    }

    /** Sums the back-to-back (no-gap) run of same-day sessions that the new [start,end) interval
     *  joins onto, including the new interval itself — faculty-free already guarantees no true
     *  overlap, so "continuous" here just means adjacent intervals with zero gap between them. */
    private double continuousChainHours(List<ClassSchedule> otherSessions, DayOfWeek dayOfWeek,
                                         LocalTime start, LocalTime end) {
        record Interval(LocalTime start, LocalTime end) {}

        List<Interval> intervals = Stream.concat(
                otherSessions.stream()
                    .filter(other -> other.getDayOfWeek() == dayOfWeek)
                    .map(other -> new Interval(other.getPeriod().getStartTime(), other.getPeriod().getEndTime())),
                Stream.of(new Interval(start, end)))
            .sorted(Comparator.comparing(Interval::start))
            .toList();

        List<Interval> runs = new java.util.ArrayList<>();
        LocalTime runStart = null;
        LocalTime runEnd = null;
        for (Interval interval : intervals) {
            if (runStart == null || !interval.start().equals(runEnd)) {
                if (runStart != null) runs.add(new Interval(runStart, runEnd));
                runStart = interval.start();
            }
            runEnd = interval.end();
        }
        runs.add(new Interval(runStart, runEnd));

        return runs.stream()
            .filter(run -> !run.start().isAfter(start) && !run.end().isBefore(end))
            .mapToDouble(run -> Duration.between(run.start(), run.end()).toMinutes() / 60.0)
            .findFirst()
            .orElse(Duration.between(start, end).toMinutes() / 60.0);
    }

    private double sessionHours(Period period) {
        return Duration.between(period.getStartTime(), period.getEndTime()).toMinutes() / 60.0;
    }

    /** Per-faculty override, then designation default (both from {@link
     *  FacultyWorkloadCapacityService}'s existing resolver, OC-119) -- falling back to the flat
     *  institution-wide config value, falling back to no cap. Reused rather than duplicated so the
     *  advisory Faculty Workload report and this hard gate can never resolve a faculty's weekly
     *  capacity differently. A configured value of 0 or less is treated as unset, mirroring {@link
     *  #resolveCapHours}'s own "blank/zero = no cap" convention. */
    Optional<Double> resolveWeeklyCap(Faculty faculty) {
        Integer perFacultyOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveCapacity(faculty);
        if (perFacultyOrDesignation != null && perFacultyOrDesignation > 0) {
            return Optional.of(perFacultyOrDesignation.doubleValue());
        }
        return resolveCapHours("timetable.faculty_max_weekly_hours");
    }

    /** Same per-faculty-then-designation-then-global precedence as {@link #resolveWeeklyCap}. */
    Optional<Double> resolveDailyCap(Faculty faculty) {
        Integer perFacultyOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveDailyCapacity(faculty);
        if (perFacultyOrDesignation != null && perFacultyOrDesignation > 0) {
            return Optional.of(perFacultyOrDesignation.doubleValue());
        }
        return resolveCapHours("timetable.faculty_max_daily_hours");
    }

    /** Same per-faculty-then-designation-then-global precedence as {@link #resolveWeeklyCap}. */
    private Optional<Double> resolveContinuousCap(Faculty faculty) {
        Integer perFacultyOrDesignation = FacultyWorkloadCapacityService.resolveEffectiveContinuousCapacity(faculty);
        if (perFacultyOrDesignation != null && perFacultyOrDesignation > 0) {
            return Optional.of(perFacultyOrDesignation.doubleValue());
        }
        return resolveCapHours("timetable.faculty_max_continuous_hours");
    }

    private Optional<Double> resolveCapHours(String configKey) {
        return systemConfigurationService.findByKey(configKey)
            .map(config -> config.configValue())
            .filter(value -> value != null && !value.isBlank())
            .flatMap(value -> {
                try {
                    double parsed = Double.parseDouble(value.trim());
                    return parsed > 0 ? Optional.of(parsed) : Optional.<Double>empty();
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            });
    }

    private static String formatHours(double hours) {
        return hours == Math.floor(hours) ? String.valueOf((long) hours) : String.valueOf(hours);
    }

    /** Blocks assigning a room already occupied at this exact day/time — either the exact same
     *  venue row (same session type, same Classroom/Lab/ClinicalVenue id), or a *different* venue
     *  that happens to share the same underlying physical Room (e.g. two separate Classrooms both
     *  linked to Room 101 in Campus Infrastructure). The latter used to be invisible here: each
     *  venue looked "free" on its own even though the real physical space was double-booked —
     *  closed by comparing the resolved physical Room, not just the virtual venue id, whenever one
     *  is linked. Checked against both PUBLISHED and other already-staffed DRAFT rows. {@code day}
     *  is passed explicitly for the same reason as {@link #checkFacultyFree}. */
    Optional<ConstraintViolation> checkRoomFree(ClassSessionType type, Long venueId, Room physicalRoom, ClassSchedule cs, DayOfWeek day, LocalTime start, LocalTime end) {
        return checkRoomFree(type, venueId, physicalRoom, cs.getTermInstance().getId(), cs.getId(), day, start, end);
    }

    /** Tuple-shaped overload of {@link #checkRoomFree(ClassSessionType, Long, Room, ClassSchedule, DayOfWeek, LocalTime, LocalTime)}
     *  for BR-55 special classes — see {@link #checkFacultyFree(Long, Long, Long, DayOfWeek, LocalTime, LocalTime)}
     *  for why {@code excludeClassScheduleId} is nullable and what this can't see. */
    Optional<ConstraintViolation> checkRoomFree(ClassSessionType type, Long venueId, Room physicalRoom,
                                                 Long termInstanceId, Long excludeClassScheduleId,
                                                 DayOfWeek day, LocalTime start, LocalTime end) {
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                day, termInstanceId, start, end, status, excludeClassScheduleId);
            boolean conflict = overlapping.stream().anyMatch(other -> conflictsOnRoom(other, type, venueId, physicalRoom));
            if (conflict) {
                return Optional.of(new ConstraintViolation("STAFFING_ROOM_CONFLICT",
                    "This room is already occupied by another session at this exact day and time."));
            }
        }
        return Optional.empty();
    }

    private boolean conflictsOnRoom(ClassSchedule other, ClassSessionType type, Long venueId, Room physicalRoom) {
        if (other.getSessionType() == type && venueId.equals(venueIdOf(other))) {
            return true;
        }
        if (physicalRoom == null) {
            return false;
        }
        Room otherRoom = physicalRoomOf(other);
        return otherRoom != null && otherRoom.getId().equals(physicalRoom.getId());
    }

    static Long venueIdOf(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY -> cs.getClassroom() != null ? cs.getClassroom().getId() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getId() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getId() : null;
        };
    }

    static Room physicalRoomOf(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY -> cs.getClassroom() != null ? cs.getClassroom().getRoom() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getRoom() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getRoom() : null;
        };
    }

    /** Hard-blocks assigning a venue that can't seat the group being placed in it. Strength is
     *  resolved from the same entities the rest of the app already uses to answer "who's actually
     *  in this session" — {@link com.cms.model.CourseRegistration} for THEORY (a whole-cohort
     *  audience) and {@link com.cms.model.Batch#getId()} student roster for LAB/CLINICAL (a
     *  sub-group audience) — never guessed. Unknown venue capacity or unresolvable strength (e.g.
     *  a legacy row with no courseOffering/batch link) never blocks: only a *known* mismatch does.
     *  Package-private (like the other checks above) so {@link TimetableConflictInspectorService}
     *  can re-run it against an already-committed room, not just at staffing time. */
    Optional<ConstraintViolation> checkCapacityFit(ClassSchedule cs, Integer venueCapacity) {
        if (venueCapacity == null) {
            return Optional.empty();
        }
        Integer strength = resolveRequiredStrength(cs);
        if (strength == null || strength <= venueCapacity) {
            return Optional.empty();
        }
        return Optional.of(new ConstraintViolation("STAFFING_CAPACITY_EXCEEDED",
            "This venue seats " + venueCapacity + ", but " + strength + " students need to be accommodated for this session."));
    }

    /** For a rotation-governed cell (no fixed batch), required strength is the largest of the
     *  rotating batches — the venue must fit whichever group's turn it is on any given week. */
    private Integer resolveRequiredStrength(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY -> cs.getCourseOffering() == null ? null
                : (int) courseRegistrationRepository.countByCourseOfferingIdAndStatus(
                    cs.getCourseOffering().getId(), RegistrationStatus.REGISTERED);
            case LAB, CLINICAL -> {
                if (cs.getBatch() != null) {
                    yield (int) batchRepository.countStudents(cs.getBatch().getId());
                }
                List<RotationMemberAssignment> rotating = rotationResolverService.allAssignmentsForSlot(cs.getId());
                yield rotating.isEmpty() ? null : rotating.stream()
                    .mapToInt(a -> (int) batchRepository.countStudents(a.getBatch().getId()))
                    .max().orElse(0);
            }
        };
    }

    private UnstaffedCellResponse toResponse(ClassSchedule cs) {
        var period = cs.getPeriod();
        var speciality = cs.getSubject().getSpeciality();
        boolean elective = isElectiveOffering(cs);

        Long venueId = null;
        String venueName = null;
        Integer venueCapacity = null;
        List<String> rotatingBatchNames = List.of();
        if (cs.getSessionType() == ClassSessionType.THEORY) {
            if (!elective) {
                Classroom resolved = resolveCommittedTheoryClassroom(cs).orElse(null);
                if (resolved != null) {
                    venueId = resolved.getId();
                    venueName = resolved.getName();
                    venueCapacity = resolved.getCapacity();
                }
            }
        } else {
            Batch batch = resolveBatchForVenue(cs).orElse(null);
            Lab lab = batch != null ? batch.getLab() : null;
            ClinicalVenue clinicalVenue = batch != null ? batch.getClinicalVenue() : null;
            venueId = lab != null ? lab.getId() : (clinicalVenue != null ? clinicalVenue.getId() : null);
            venueName = lab != null ? lab.getName() : (clinicalVenue != null ? clinicalVenue.getName() : null);
            venueCapacity = lab != null ? lab.getCapacity() : (clinicalVenue != null ? clinicalVenue.getCapacity() : null);
            if (cs.getBatch() == null) {
                rotatingBatchNames = rotationResolverService.allAssignmentsForSlot(cs.getId()).stream()
                    .map(a -> a.getBatch().getName())
                    .toList();
            }
        }

        return new UnstaffedCellResponse(
            cs.getId(),
            cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null,
            cs.getSubject().getName(),
            cs.getSubject().getCode(),
            speciality != null ? speciality.getId() : null,
            speciality != null ? speciality.getName() : null,
            cs.getSessionType(),
            cs.getDayOfWeek(),
            period != null ? period.getId() : null,
            period != null ? period.getName() : null,
            period != null ? period.getStartTime() : null,
            period != null ? period.getEndTime() : null,
            cs.getBatchName() != null ? cs.getBatchName() : (cs.getBatch() != null ? cs.getBatch().getName() : null),
            resolveRequiredStrength(cs),
            venueId,
            venueName,
            venueCapacity,
            elective,
            rotatingBatchNames,
            cs.getSessionGroupId(),
            cs.getCohortSection() != null ? cs.getCohortSection().getId() : null
        );
    }
}
