package com.cms.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;

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
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final BatchRepository batchRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    public TimetableStaffingService(ClassScheduleRepository classScheduleRepository,
                                     FacultyRepository facultyRepository,
                                     ClassroomRepository classroomRepository,
                                     LabRepository labRepository,
                                     ClinicalVenueRepository clinicalVenueRepository,
                                     BatchRepository batchRepository,
                                     CourseRegistrationRepository courseRegistrationRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.facultyRepository = facultyRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.batchRepository = batchRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
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
                if (request.classroomId() == null) {
                    throw new IllegalArgumentException("A classroom is required to staff a THEORY session");
                }
                Classroom classroom = classroomRepository.findById(request.classroomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + request.classroomId()));
                requireRoomFree(classroom.getId(), ClassSessionType.THEORY, cs, start, end);
                requireCapacityFit(cs, classroom.getCapacity());
                cs.setClassroom(classroom);
            }
            case LAB -> {
                if (request.labId() == null) {
                    throw new IllegalArgumentException("A lab is required to staff a LAB session");
                }
                Lab lab = labRepository.findById(request.labId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));
                requireRoomFree(lab.getId(), ClassSessionType.LAB, cs, start, end);
                requireCapacityFit(cs, lab.getCapacity());
                cs.setLab(lab);
            }
            case CLINICAL -> {
                if (request.clinicalVenueId() == null) {
                    throw new IllegalArgumentException("A clinical venue is required to staff a CLINICAL session");
                }
                ClinicalVenue venue = clinicalVenueRepository.findById(request.clinicalVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + request.clinicalVenueId()));
                requireRoomFree(venue.getId(), ClassSessionType.CLINICAL, cs, start, end);
                requireCapacityFit(cs, venue.getCapacity());
                cs.setClinicalVenue(venue);
            }
        }

        cs.setFaculty(faculty);
        return toResponse(classScheduleRepository.save(cs));
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

    /** Blocks assigning a room already occupied at this exact day/time by another session of the
     *  same room type, checked against both PUBLISHED and other already-staffed DRAFT rows. */
    private void requireRoomFree(Long roomId, ClassSessionType type, ClassSchedule cs, LocalTime start, LocalTime end) {
        for (ClassScheduleStatus status : List.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)) {
            List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
                cs.getDayOfWeek(), cs.getTermInstance().getId(), start, end, status, cs.getId());
            boolean conflict = overlapping.stream().anyMatch(other -> {
                if (other.getSessionType() != type) return false;
                Long otherRoomId = switch (type) {
                    case THEORY -> other.getClassroom() != null ? other.getClassroom().getId() : null;
                    case LAB -> other.getLab() != null ? other.getLab().getId() : null;
                    case CLINICAL -> other.getClinicalVenue() != null ? other.getClinicalVenue().getId() : null;
                };
                return roomId.equals(otherRoomId);
            });
            if (conflict) {
                throw new LifecycleConflictException(
                    "This room is already occupied by another session at this exact day and time.",
                    "STAFFING_ROOM_CONFLICT", "ClassSchedule", cs.getId(), null);
            }
        }
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

    private Integer resolveRequiredStrength(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY -> cs.getCourseOffering() == null ? null
                : (int) courseRegistrationRepository.countByCourseOfferingIdAndStatus(
                    cs.getCourseOffering().getId(), RegistrationStatus.REGISTERED);
            case LAB, CLINICAL -> cs.getBatch() == null ? null
                : (int) batchRepository.countStudents(cs.getBatch().getId());
        };
    }

    private UnstaffedCellResponse toResponse(ClassSchedule cs) {
        var period = cs.getPeriod();
        var speciality = cs.getSubject().getSpeciality();
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
            resolveRequiredStrength(cs)
        );
    }
}
