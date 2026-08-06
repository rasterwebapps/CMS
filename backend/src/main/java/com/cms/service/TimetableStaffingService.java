package com.cms.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.RotationMemberAssignment;
import com.cms.model.Room;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CourseRegistrationRepository;
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
    private final RotationResolverService rotationResolverService;

    public TimetableStaffingService(ClassScheduleRepository classScheduleRepository,
                                     FacultyRepository facultyRepository,
                                     ClassroomRepository classroomRepository,
                                     BatchRepository batchRepository,
                                     CourseRegistrationRepository courseRegistrationRepository,
                                     StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                     CohortRoomAllocationRepository cohortRoomAllocationRepository,
                                     RotationResolverService rotationResolverService) {
        this.classScheduleRepository = classScheduleRepository;
        this.facultyRepository = facultyRepository;
        this.classroomRepository = classroomRepository;
        this.batchRepository = batchRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.cohortRoomAllocationRepository = cohortRoomAllocationRepository;
        this.rotationResolverService = rotationResolverService;
    }

    public List<UnstaffedCellResponse> getUnstaffedCells(Long termInstanceId) {
        return classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT)
            .stream()
            .filter(cs -> cs.getFaculty() == null)
            .map(this::toResponse)
            .toList();
    }

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

        LocalTime start = cs.getPeriod().getStartTime();
        LocalTime end = cs.getPeriod().getEndTime();
        requireFacultyFree(faculty.getId(), cs, start, end);

        switch (cs.getSessionType()) {
            case THEORY -> {
                Classroom classroom = isElectiveOffering(cs)
                    ? requireRequestedClassroom(request)
                    : requireCommittedTheoryClassroom(cs);
                requireRoomFree(ClassSessionType.THEORY, classroom.getId(), classroom.getRoom(), cs, start, end);
                requireCapacityFit(cs, classroom.getCapacity());
                cs.setClassroom(classroom);
            }
            case LAB -> {
                Lab lab = requireCommittedLab(cs);
                requireRoomFree(ClassSessionType.LAB, lab.getId(), lab.getRoom(), cs, start, end);
                requireCapacityFit(cs, lab.getCapacity());
                cs.setLab(lab);
            }
            case CLINICAL -> {
                ClinicalVenue venue = requireCommittedClinicalVenue(cs);
                requireRoomFree(ClassSessionType.CLINICAL, venue.getId(), venue.getRoom(), cs, start, end);
                requireCapacityFit(cs, venue.getCapacity());
                cs.setClinicalVenue(venue);
            }
        }

        cs.setFaculty(faculty);
        return toResponse(classScheduleRepository.save(cs));
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
                "This cohort has no Theory room committed in Cohort Room Allocation — commit it in Capacity Planner before staffing.",
                "STAFFING_VENUE_NOT_COMMITTED", "ClassSchedule", cs.getId(), null));
    }

    private Optional<Classroom> resolveCommittedTheoryClassroom(ClassSchedule cs) {
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
        return cohortRoomAllocationRepository
            .findByCohortIdAndTermInstanceIdAndStatus(
                cohortIds.iterator().next(), offering.getTermInstance().getId(), CohortRoomAllocationStatus.COMMITTED)
            .map(CohortRoomAllocation::getTheoryClassroom);
    }

    /** Blocks assigning a faculty member already committed elsewhere at this exact day/time —
     *  checked against both live PUBLISHED rows and other already-staffed DRAFT rows in the same
     *  term, excluding this cell itself. */
    private void requireFacultyFree(Long facultyId, ClassSchedule cs, LocalTime start, LocalTime end) {
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                cs.getDayOfWeek(), cs.getTermInstance().getId(), start, end, status, cs.getId());
            boolean conflict = overlapping.stream()
                .anyMatch(other -> other.getFaculty() != null && other.getFaculty().getId().equals(facultyId));
            if (conflict) {
                throw new LifecycleConflictException(
                    "This faculty member is already scheduled for another session at this exact day and time.",
                    "STAFFING_FACULTY_CONFLICT", "ClassSchedule", cs.getId(), null);
            }
        }
    }

    /** Blocks assigning a room already occupied at this exact day/time — either the exact same
     *  venue row (same session type, same Classroom/Lab/ClinicalVenue id), or a *different* venue
     *  that happens to share the same underlying physical Room (e.g. two separate Classrooms both
     *  linked to Room 101 in Campus Infrastructure). The latter used to be invisible here: each
     *  venue looked "free" on its own even though the real physical space was double-booked —
     *  closed by comparing the resolved physical Room, not just the virtual venue id, whenever one
     *  is linked. Checked against both PUBLISHED and other already-staffed DRAFT rows. */
    private void requireRoomFree(ClassSessionType type, Long venueId, Room physicalRoom, ClassSchedule cs, LocalTime start, LocalTime end) {
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                cs.getDayOfWeek(), cs.getTermInstance().getId(), start, end, status, cs.getId());
            boolean conflict = overlapping.stream().anyMatch(other -> conflictsOnRoom(other, type, venueId, physicalRoom));
            if (conflict) {
                throw new LifecycleConflictException(
                    "This room is already occupied by another session at this exact day and time.",
                    "STAFFING_ROOM_CONFLICT", "ClassSchedule", cs.getId(), null);
            }
        }
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

    private static Long venueIdOf(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY -> cs.getClassroom() != null ? cs.getClassroom().getId() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getId() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getId() : null;
        };
    }

    private static Room physicalRoomOf(ClassSchedule cs) {
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
     *  a legacy row with no courseOffering/batch link) never blocks: only a *known* mismatch does. */
    private void requireCapacityFit(ClassSchedule cs, Integer venueCapacity) {
        if (venueCapacity == null) {
            return;
        }
        Integer strength = resolveRequiredStrength(cs);
        if (strength == null || strength <= venueCapacity) {
            return;
        }
        throw new LifecycleConflictException(
            "This venue seats " + venueCapacity + ", but " + strength + " students need to be accommodated for this session.",
            "STAFFING_CAPACITY_EXCEEDED", "ClassSchedule", cs.getId(), null);
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
            rotatingBatchNames
        );
    }
}
