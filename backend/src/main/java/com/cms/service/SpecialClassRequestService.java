package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ConstraintViolation;
import com.cms.dto.DayRepeatRequest;
import com.cms.dto.DayRepeatResult;
import com.cms.dto.SpecialClassOccurrenceDto;
import com.cms.dto.SpecialClassRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.ClinicalVenue;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.Room;
import com.cms.model.SessionOccurrence;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.OccurrenceSource;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.SpecialClassApprovalStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SubjectRepository;
import com.cms.service.SessionOccurrenceVenue.VenueResolution;

/**
 * BR-55 — Special/Remedial Class Scheduler. Faculty request a single-subject ad-hoc session or a
 * whole-day-repeat batch; Admin approves/rejects before it goes live. Every request is conflict-
 * checked in two parts, since a special class has no backing {@link ClassSchedule} row: (1)
 * against the recurring weekly template via {@link ClassScheduleRepository#findOverlapping}, and
 * (2) against other pending/approved special classes on the same date+period, which (1) can't
 * see. Deliberately does not enforce {@link TimetableStaffingService#checkWithinWorkloadCaps} —
 * a one-off session was judged not to fit that *weekly* ceiling's semantics (see BR-55).
 */
@Service
@Transactional(readOnly = true)
public class SpecialClassRequestService {

    private static final List<OccurrenceSource> SPECIAL_SOURCES =
        List.of(OccurrenceSource.SPECIAL_CLASS, OccurrenceSource.DAY_REPEAT);
    private static final List<SpecialClassApprovalStatus> LIVE_STATUSES =
        List.of(SpecialClassApprovalStatus.PENDING, SpecialClassApprovalStatus.APPROVED);

    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final SubjectRepository subjectRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final PeriodRepository periodRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final TimetableStaffingService timetableStaffingService;
    private final AuditLogService auditLogService;

    public SpecialClassRequestService(SessionOccurrenceRepository sessionOccurrenceRepository,
                                       ClassScheduleRepository classScheduleRepository,
                                       SubjectRepository subjectRepository,
                                       CourseOfferingRepository courseOfferingRepository,
                                       CohortSectionRepository cohortSectionRepository,
                                       PeriodRepository periodRepository,
                                       ClassroomRepository classroomRepository,
                                       LabRepository labRepository,
                                       ClinicalVenueRepository clinicalVenueRepository,
                                       FacultyRepository facultyRepository,
                                       CourseRegistrationRepository courseRegistrationRepository,
                                       TimetableStaffingService timetableStaffingService,
                                       AuditLogService auditLogService) {
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.subjectRepository = subjectRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.periodRepository = periodRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.facultyRepository = facultyRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.timetableStaffingService = timetableStaffingService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SpecialClassOccurrenceDto requestSingleSubject(SpecialClassRequest request, Long requestingFacultyId, String actor) {
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));
        CourseOffering courseOffering = courseOfferingRepository.findById(request.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + request.courseOfferingId()));
        CohortSection cohortSection = request.cohortSectionId() != null
            ? cohortSectionRepository.findById(request.cohortSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Cohort section not found with id: " + request.cohortSectionId()))
            : null;
        Period period = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
        Faculty requestedFaculty = facultyRepository.findById(request.requestedFacultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.requestedFacultyId()));
        Faculty requestingFaculty = facultyRepository.findById(requestingFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + requestingFacultyId));

        TermInstance term = courseOffering.getTermInstance();
        requireNotLocked(term);
        DayOfWeek day = dayOfWeek(request.occurrenceDate());

        VenueResolution venue = resolveVenue(request.sessionType(), request.classroomId(), request.labId(), request.clinicalVenueId());

        List<ConstraintViolation> violations = new ArrayList<>();
        checkConflicts(violations, term, day, period, requestedFaculty.getId(), request.sessionType(),
            venue.venueId(), venue.physicalRoom(), venue.capacity(), courseOffering.getId(), request.occurrenceDate(),
            subject.getId(), cohortSection != null ? cohortSection.getId() : null, null);
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }

        SessionOccurrence occurrence = SessionOccurrence.forSpecialClass(OccurrenceSource.SPECIAL_CLASS,
            request.occurrenceDate(), subject, courseOffering, cohortSection, period, request.sessionType(),
            requestedFaculty, requestingFaculty, request.reason());
        applyVenue(occurrence, request.sessionType(), venue);
        occurrence = sessionOccurrenceRepository.save(occurrence);

        auditLogService.record(actor, "SPECIAL_CLASS_REQUESTED", "SessionOccurrence", occurrence.getId().toString(),
            "Requested special class: " + subject.getName() + " on " + request.occurrenceDate() + " (" + period.getName() + ")");
        return toDto(occurrence);
    }

    @Transactional
    public DayRepeatResult requestDayRepeat(DayRepeatRequest request, Long requestingFacultyId, String actor) {
        CohortSection cohortSection = cohortSectionRepository.findById(request.cohortSectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Cohort section not found with id: " + request.cohortSectionId()));
        Faculty requestingFaculty = facultyRepository.findById(requestingFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + requestingFacultyId));
        TermInstance term = cohortSection.getTermInstance();
        if (!term.getId().equals(request.termInstanceId())) {
            throw new IllegalArgumentException("Cohort section does not belong to the given term instance.");
        }
        requireNotLocked(term);
        DayOfWeek targetDay = dayOfWeek(request.targetDate());

        List<ClassSchedule> sourceRows = classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(
            term.getId(), ClassScheduleStatus.PUBLISHED, request.sourceDayOfWeek());

        List<ClassSchedule> resolvable = sourceRows.stream()
            .filter(cs -> belongsToCohortSection(cs, cohortSection.getId()))
            .toList();
        int skippedCount = sourceRows.size() - resolvable.size();

        UUID requestBatchId = UUID.randomUUID();
        List<SessionOccurrence> toSave = new ArrayList<>();
        List<ConstraintViolation> allViolations = new ArrayList<>();

        for (ClassSchedule cs : resolvable) {
            VenueResolution venue = SessionOccurrenceVenue.fromClassSchedule(cs);
            List<ConstraintViolation> rowViolations = new ArrayList<>();
            Long facultyId = cs.getFaculty() != null ? cs.getFaculty().getId() : null;
            Long rowCourseOfferingId = cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null;
            checkConflicts(rowViolations, term, targetDay, cs.getPeriod(), facultyId, cs.getSessionType(),
                venue.venueId(), venue.physicalRoom(), venue.capacity(), rowCourseOfferingId, request.targetDate(),
                cs.getSubject().getId(), cohortSection.getId(), null);
            if (!rowViolations.isEmpty()) {
                allViolations.addAll(rowViolations.stream()
                    .map(v -> new ConstraintViolation(v.code(), cs.getSubject().getName() + ": " + v.message()))
                    .toList());
                continue;
            }
            SessionOccurrence occurrence = SessionOccurrence.forSpecialClass(OccurrenceSource.DAY_REPEAT,
                request.targetDate(), cs.getSubject(), cs.getCourseOffering(), cohortSection, cs.getPeriod(),
                cs.getSessionType(), cs.getFaculty(), requestingFaculty, request.reason());
            occurrence.setSourceDayOfWeek(request.sourceDayOfWeek());
            occurrence.setRequestBatchId(requestBatchId);
            applyVenue(occurrence, cs.getSessionType(), venue);
            toSave.add(occurrence);
        }

        // All-or-nothing: a partially-created day-repeat would be a confusing half-timetable to
        // review/approve. Any single-row conflict fails the whole batch (see BR-55 plan).
        if (!allViolations.isEmpty()) {
            throw new TimetableConstraintViolationException(allViolations);
        }
        if (toSave.isEmpty()) {
            throw new IllegalArgumentException(
                "No sessions from " + request.sourceDayOfWeek() + " could be resolved to this cohort section.");
        }

        List<SessionOccurrence> saved = sessionOccurrenceRepository.saveAll(toSave);
        auditLogService.record(actor, "SPECIAL_CLASS_DAY_REPEAT_REQUESTED", "SessionOccurrence",
            requestBatchId.toString(),
            saved.size() + " sessions copied from " + request.sourceDayOfWeek() + " onto " + request.targetDate()
                + " (" + skippedCount + " skipped, cohort unresolved)");

        return new DayRepeatResult(saved.stream().map(this::toDto).toList(), skippedCount);
    }

    /** Never guesses: a source row only belongs to the target cohort section if it (or, for
     *  LAB/CLINICAL, its Batch) carries a direct link to it — mirrors the same "never guess"
     *  contract {@link TimetableStaffingService#resolveCommittedTheoryClassroom} already applies
     *  to THEORY room resolution. Everything else is skipped and reported via skippedCount. */
    private boolean belongsToCohortSection(ClassSchedule cs, Long cohortSectionId) {
        if (cs.getSessionType() == ClassSessionType.THEORY) {
            return cs.getCohortSection() != null && cs.getCohortSection().getId().equals(cohortSectionId);
        }
        return cs.getBatch() != null && cs.getBatch().getCohortSection() != null
            && cs.getBatch().getCohortSection().getId().equals(cohortSectionId);
    }

    @Transactional
    public SpecialClassOccurrenceDto approve(Long id, String approver) {
        SessionOccurrence occurrence = requireSpecialClass(id);
        requirePending(occurrence);
        reCheckStillFree(occurrence);
        occurrence.setApprovalStatus(SpecialClassApprovalStatus.APPROVED);
        occurrence.setApprovedBy(approver);
        occurrence.setApprovedAt(java.time.Instant.now());
        occurrence = sessionOccurrenceRepository.save(occurrence);
        auditLogService.record(approver, "SPECIAL_CLASS_APPROVED", "SessionOccurrence", id.toString(), null);
        return toDto(occurrence);
    }

    @Transactional
    public List<SpecialClassOccurrenceDto> approveBatch(UUID requestBatchId, String approver) {
        List<SessionOccurrence> batch = requireBatch(requestBatchId);
        batch.forEach(occurrence -> {
            requirePending(occurrence);
            reCheckStillFree(occurrence);
        });
        batch.forEach(occurrence -> {
            occurrence.setApprovalStatus(SpecialClassApprovalStatus.APPROVED);
            occurrence.setApprovedBy(approver);
            occurrence.setApprovedAt(java.time.Instant.now());
        });
        List<SessionOccurrence> saved = sessionOccurrenceRepository.saveAll(batch);
        auditLogService.record(approver, "SPECIAL_CLASS_DAY_REPEAT_APPROVED", "SessionOccurrence",
            requestBatchId.toString(), saved.size() + " sessions approved");
        return saved.stream().map(this::toDto).toList();
    }

    @Transactional
    public SpecialClassOccurrenceDto reject(Long id, String reason, String approver) {
        SessionOccurrence occurrence = requireSpecialClass(id);
        requirePending(occurrence);
        occurrence.setApprovalStatus(SpecialClassApprovalStatus.REJECTED);
        occurrence.setRejectionReason(reason);
        occurrence.setApprovedBy(approver);
        occurrence.setApprovedAt(java.time.Instant.now());
        occurrence = sessionOccurrenceRepository.save(occurrence);
        auditLogService.record(approver, "SPECIAL_CLASS_REJECTED", "SessionOccurrence", id.toString(), reason);
        return toDto(occurrence);
    }

    @Transactional
    public List<SpecialClassOccurrenceDto> rejectBatch(UUID requestBatchId, String reason, String approver) {
        List<SessionOccurrence> batch = requireBatch(requestBatchId);
        batch.forEach(this::requirePending);
        batch.forEach(occurrence -> {
            occurrence.setApprovalStatus(SpecialClassApprovalStatus.REJECTED);
            occurrence.setRejectionReason(reason);
            occurrence.setApprovedBy(approver);
            occurrence.setApprovedAt(java.time.Instant.now());
        });
        List<SessionOccurrence> saved = sessionOccurrenceRepository.saveAll(batch);
        auditLogService.record(approver, "SPECIAL_CLASS_DAY_REPEAT_REJECTED", "SessionOccurrence",
            requestBatchId.toString(), saved.size() + " sessions rejected: " + reason);
        return saved.stream().map(this::toDto).toList();
    }

    @Transactional
    public SpecialClassOccurrenceDto cancel(Long id, String actor) {
        SessionOccurrence occurrence = requireSpecialClass(id);
        if (occurrence.getApprovalStatus() != SpecialClassApprovalStatus.APPROVED) {
            throw new LifecycleConflictException("Only an approved special class can be cancelled.",
                "SPECIAL_CLASS_NOT_APPROVED", "SessionOccurrence", id, null);
        }
        if (!occurrence.getOccurrenceDate().isAfter(LocalDate.now())) {
            throw new LifecycleConflictException("This special class's date has already passed.",
                "SPECIAL_CLASS_ALREADY_OCCURRED", "SessionOccurrence", id, null);
        }
        occurrence.setApprovalStatus(SpecialClassApprovalStatus.CANCELLED);
        occurrence = sessionOccurrenceRepository.save(occurrence);
        auditLogService.record(actor, "SPECIAL_CLASS_CANCELLED", "SessionOccurrence", id.toString(), null);
        return toDto(occurrence);
    }

    public List<SpecialClassOccurrenceDto> listMyRequests(Long facultyId) {
        return sessionOccurrenceRepository
            .findByRequestedByFaculty_IdAndOccurrenceSourceInOrderByOccurrenceDateDesc(facultyId, SPECIAL_SOURCES)
            .stream().map(this::toDto).toList();
    }

    public List<SpecialClassOccurrenceDto> listApprovalQueue() {
        List<SessionOccurrence> pending = sessionOccurrenceRepository
            .findByApprovalStatusAndOccurrenceSourceInOrderByRequestedAtAsc(SpecialClassApprovalStatus.PENDING, SPECIAL_SOURCES);
        return pending.stream().map(this::toDto).toList();
    }

    // ---- internal helpers ----

    private SessionOccurrence requireSpecialClass(Long id) {
        SessionOccurrence occurrence = sessionOccurrenceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session occurrence not found with id: " + id));
        if (occurrence.getOccurrenceSource() == OccurrenceSource.REGULAR) {
            throw new IllegalArgumentException("Session occurrence " + id + " is a regular session, not a special class.");
        }
        return occurrence;
    }

    private List<SessionOccurrence> requireBatch(UUID requestBatchId) {
        List<SessionOccurrence> batch = sessionOccurrenceRepository.findByRequestBatchId(requestBatchId);
        if (batch.isEmpty()) {
            throw new ResourceNotFoundException("No day-repeat batch found with id: " + requestBatchId);
        }
        return batch;
    }

    private void requirePending(SessionOccurrence occurrence) {
        if (occurrence.getApprovalStatus() != SpecialClassApprovalStatus.PENDING) {
            throw new LifecycleConflictException(
                "Only a pending request can be approved or rejected (current status: " + occurrence.getApprovalStatus() + ").",
                "SPECIAL_CLASS_NOT_PENDING", "SessionOccurrence", occurrence.getId(), null);
        }
    }

    /** Re-runs the same two-part conflict check at approval time -- other requests may have been
     *  approved in the meantime, so the candidate slot that looked free at request time might not
     *  be anymore (same "never trust a stale candidate list" principle already applied elsewhere
     *  in this module, e.g. FacultyAbsenceService.applySubstitute). */
    private void reCheckStillFree(SessionOccurrence occurrence) {
        TermInstance term = occurrence.getCourseOffering() != null ? occurrence.getCourseOffering().getTermInstance() : null;
        if (term == null) {
            return;
        }
        DayOfWeek day = dayOfWeek(occurrence.getOccurrenceDate());
        VenueResolution venue = SessionOccurrenceVenue.fromOccurrence(occurrence);
        Long facultyId = occurrence.getRequestedFaculty() != null ? occurrence.getRequestedFaculty().getId() : null;
        Long courseOfferingId = occurrence.getCourseOffering() != null ? occurrence.getCourseOffering().getId() : null;
        List<ConstraintViolation> violations = new ArrayList<>();
        checkConflicts(violations, term, day, occurrence.getPeriod(), facultyId, occurrence.getSessionType(),
            venue.venueId(), venue.physicalRoom(), venue.capacity(), courseOfferingId, occurrence.getOccurrenceDate(),
            occurrence.getSubject() != null ? occurrence.getSubject().getId() : null,
            occurrence.getCohortSection() != null ? occurrence.getCohortSection().getId() : null,
            occurrence.getId());
        if (!violations.isEmpty()) {
            throw new TimetableConstraintViolationException(violations);
        }
    }

    private void requireNotLocked(TermInstance term) {
        if (term.getStatus() == TermInstanceStatus.LOCKED) {
            throw new LifecycleConflictException(
                "This term is locked. Its timetable can no longer be changed.",
                "TIMETABLE_TERM_LOCKED", "TermInstance", term.getId(), null);
        }
    }

    private DayOfWeek dayOfWeek(LocalDate date) {
        try {
            return DayOfWeek.valueOf(date.getDayOfWeek().name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Special classes can only be scheduled Monday through Saturday.");
        }
    }

    /** Two-part conflict check: (1) the recurring weekly template via
     *  {@link TimetableStaffingService}'s tuple overloads, (2) other live (pending/approved)
     *  special classes on the same date+period -- which (1) can't see, since they have no
     *  ClassSchedule row. {@code excludeOccurrenceId} is non-null only when re-checking an
     *  existing request (approval time). Faculty checks are skipped when {@code facultyId} is
     *  null (a day-repeat row copied from an unstaffed source session). */
    private void checkConflicts(List<ConstraintViolation> violations, TermInstance term, DayOfWeek day, Period period,
                                 Long facultyId, ClassSessionType sessionType, Long venueId, Room physicalRoom,
                                 Integer venueCapacity, Long courseOfferingId, LocalDate date, Long subjectId,
                                 Long cohortSectionId, Long excludeOccurrenceId) {
        var start = period.getStartTime();
        var end = period.getEndTime();

        if (facultyId != null) {
            timetableStaffingService.checkFacultyAvailable(facultyId, day, start, end, date).ifPresent(violations::add);
            timetableStaffingService.checkFacultyFree(facultyId, term.getId(), null, day, start, end).ifPresent(violations::add);
        }
        if (venueId != null) {
            timetableStaffingService.checkRoomFree(sessionType, venueId, physicalRoom, term.getId(), null, day, start, end)
                .ifPresent(violations::add);
        }
        // Capacity-fit mirrors TimetableStaffingService.checkCapacityFit's THEORY branch only --
        // LAB/CLINICAL strength there is resolved from a Batch roster, which a special class has
        // none of; a known venue-capacity/registered-strength mismatch is still worth catching for
        // THEORY, where the audience is the whole course offering's registered cohort.
        if (venueCapacity != null && sessionType == ClassSessionType.THEORY && courseOfferingId != null) {
            int strength = (int) courseRegistrationRepository.countByCourseOfferingIdAndStatus(
                courseOfferingId, RegistrationStatus.REGISTERED);
            if (strength > venueCapacity) {
                violations.add(new ConstraintViolation("SPECIAL_CLASS_CAPACITY_EXCEEDED",
                    "This venue seats " + venueCapacity + ", but " + strength + " students need to be accommodated for this session."));
            }
        }

        List<SessionOccurrence> others = sessionOccurrenceRepository
            .findByOccurrenceSourceInAndOccurrenceDateAndPeriod_Id(SPECIAL_SOURCES, date, period.getId())
            .stream()
            .filter(o -> LIVE_STATUSES.contains(o.getApprovalStatus()))
            .filter(o -> excludeOccurrenceId == null || !o.getId().equals(excludeOccurrenceId))
            .toList();

        for (SessionOccurrence other : others) {
            if (subjectId != null && subjectId.equals(other.getSubject() != null ? other.getSubject().getId() : null)
                && Objects.equals(cohortSectionId, other.getCohortSection() != null ? other.getCohortSection().getId() : null)) {
                violations.add(new ConstraintViolation("SPECIAL_CLASS_DUPLICATE_REQUEST",
                    "A special class for this subject/cohort is already requested or approved at this date and period."));
            }
            if (facultyId != null && other.getRequestedFaculty() != null && facultyId.equals(other.getRequestedFaculty().getId())) {
                violations.add(new ConstraintViolation("SPECIAL_CLASS_FACULTY_CONFLICT",
                    "This faculty member already has another special class at this exact date and period."));
            }
            if (venueId != null) {
                VenueResolution otherVenue = SessionOccurrenceVenue.fromOccurrence(other);
                boolean sameVenue = other.getSessionType() == sessionType && venueId.equals(otherVenue.venueId());
                boolean samePhysicalRoom = physicalRoom != null && otherVenue.physicalRoom() != null
                    && physicalRoom.getId().equals(otherVenue.physicalRoom().getId());
                if (sameVenue || samePhysicalRoom) {
                    violations.add(new ConstraintViolation("SPECIAL_CLASS_ROOM_CONFLICT",
                        "This room is already occupied by another special class at this exact date and period."));
                }
            }
        }
    }

    private VenueResolution resolveVenue(ClassSessionType sessionType, Long classroomId, Long labId, Long clinicalVenueId) {
        return switch (sessionType) {
            case THEORY -> {
                if (classroomId == null) {
                    throw new IllegalArgumentException("A classroom is required for a THEORY session.");
                }
                Classroom classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + classroomId));
                yield new VenueResolution(classroom.getId(), classroom.getRoom(), classroom.getCapacity(), classroom, null, null);
            }
            case LAB -> {
                if (labId == null) {
                    throw new IllegalArgumentException("A lab is required for a LAB session.");
                }
                Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + labId));
                yield new VenueResolution(lab.getId(), lab.getRoom(), lab.getCapacity(), null, lab, null);
            }
            case CLINICAL -> {
                if (clinicalVenueId == null) {
                    throw new IllegalArgumentException("A clinical venue is required for a CLINICAL session.");
                }
                ClinicalVenue venue = clinicalVenueRepository.findById(clinicalVenueId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + clinicalVenueId));
                yield new VenueResolution(venue.getId(), venue.getRoom(), venue.getCapacity(), null, null, venue);
            }
        };
    }

    private void applyVenue(SessionOccurrence occurrence, ClassSessionType sessionType, VenueResolution venue) {
        switch (sessionType) {
            case THEORY -> occurrence.setClassroom(venue.classroom());
            case LAB -> occurrence.setLab(venue.lab());
            case CLINICAL -> occurrence.setClinicalVenue(venue.clinicalVenue());
        }
    }

    private SpecialClassOccurrenceDto toDto(SessionOccurrence o) {
        VenueResolution venue = SessionOccurrenceVenue.fromOccurrence(o);
        String venueName = venue.classroom() != null ? venue.classroom().getName()
            : venue.lab() != null ? venue.lab().getName()
            : venue.clinicalVenue() != null ? venue.clinicalVenue().getName() : null;
        return new SpecialClassOccurrenceDto(
            o.getId(), o.getOccurrenceSource(), o.getOccurrenceDate(),
            o.getSubject() != null ? o.getSubject().getId() : null,
            o.getSubject() != null ? o.getSubject().getName() : null,
            o.getSubject() != null ? o.getSubject().getCode() : null,
            o.getCourseOffering() != null ? o.getCourseOffering().getId() : null,
            o.getCohortSection() != null ? o.getCohortSection().getId() : null,
            o.getCohortSection() != null ? o.getCohortSection().getSectionLabel() : null,
            o.getPeriod() != null ? o.getPeriod().getId() : null,
            o.getPeriod() != null ? o.getPeriod().getName() : null,
            o.getPeriod() != null ? o.getPeriod().getStartTime() : null,
            o.getPeriod() != null ? o.getPeriod().getEndTime() : null,
            o.getSessionType(),
            venue.venueId(), venueName,
            o.getRequestedFaculty() != null ? o.getRequestedFaculty().getId() : null,
            o.getRequestedFaculty() != null ? o.getRequestedFaculty().getFullName() : null,
            o.getApprovalStatus(),
            o.getRequestedByFaculty() != null ? o.getRequestedByFaculty().getId() : null,
            o.getRequestedByFaculty() != null ? o.getRequestedByFaculty().getFullName() : null,
            o.getRequestedAt(), o.getRequestReason(), o.getSourceDayOfWeek(), o.getRequestBatchId(),
            o.getApprovedBy(), o.getApprovedAt(), o.getRejectionReason()
        );
    }
}
