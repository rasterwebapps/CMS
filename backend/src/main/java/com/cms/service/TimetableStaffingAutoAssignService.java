package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AutoStaffResult;
import com.cms.dto.AutoStaffUnplacedItem;
import com.cms.dto.FacultyCapacityCheckResult;
import com.cms.dto.SectionFacultyAssignment;
import com.cms.dto.StaffingAssignmentRequest;
import com.cms.dto.UnstaffedCellResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.Faculty;
import com.cms.model.Subject;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.FacultyRepository;

/** R3 Step 6 — staffs a term's remaining unstaffed Skeleton cells automatically, augmenting
 *  (never replacing) manual staffing: every constraint check is {@link TimetableStaffingService}'s
 *  own (via {@link TimetableStaffingService#staffCell}), never re-implemented here. Electives are
 *  handled like any other Theory subject: faculty comes from the offering's own {@link
 *  CourseOfferingSectionFaculty} assignment (see {@link #resolveElectiveFacultyId}, mirroring
 *  {@link TimetableGlobalAutoScheduleService#resolveElectiveMemberFacultyId}) and the room is
 *  auto-picked best-fit-first (see {@link #rankClassroomsByFit}) instead of requiring a manual
 *  pick — a student-choice elective still has no single owning cohort, so its room can never be
 *  pre-committed in Capacity Planner, but it can still be resolved automatically here once real
 *  enrolled strength is known. No backtracking here, unlike auto-place — a staffing conflict is a
 *  specific faculty member's time already being spent elsewhere, not a shared slot another row
 *  could be nudged out of, so unstaffing a different cell to retry has no equivalent "freed
 *  resource" logic to lean on.
 *
 *  <p>A section-scoped THEORY cell prefers its own {@link CourseOfferingSectionFaculty} override
 *  (see {@link #staffFromSectionOverride}) before falling back to the ranked eligible-department
 *  pool below, mirroring {@link TimetableGlobalAutoScheduleService#runGlobalAutoSchedule}'s own
 *  per-section resolution — Section Faculty is authoritative for placement, not just accounting. */
@Service
public class TimetableStaffingAutoAssignService {

    private final TimetableStaffingService timetableStaffingService;
    private final FacultyRepository facultyRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassroomRepository classroomRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;
    private final CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public TimetableStaffingAutoAssignService(TimetableStaffingService timetableStaffingService,
                                               FacultyRepository facultyRepository,
                                               ClassScheduleRepository classScheduleRepository,
                                               ClassroomRepository classroomRepository,
                                               CourseOfferingRepository courseOfferingRepository,
                                               CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository,
                                               CourseOfferingSectionFacultyService courseOfferingSectionFacultyService,
                                               TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.timetableStaffingService = timetableStaffingService;
        this.facultyRepository = facultyRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.classroomRepository = classroomRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseOfferingSectionFacultyRepository = courseOfferingSectionFacultyRepository;
        this.courseOfferingSectionFacultyService = courseOfferingSectionFacultyService;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
    }

    @Transactional
    public AutoStaffResult autoStaff(Long termInstanceId) {
        List<UnstaffedCellResponse> cells = timetableStaffingService.getUnstaffedCells(termInstanceId);
        int staffedCount = 0;
        List<AutoStaffUnplacedItem> unplaced = new ArrayList<>();

        for (UnstaffedCellResponse cell : cells) {
            if (cell.isElective()) {
                if (autoStaffElective(cell)) {
                    staffedCount++;
                } else {
                    unplaced.add(new AutoStaffUnplacedItem(cell.subjectName(),
                        "no single faculty assigned on this elective option (via Assign Faculty), or no free-fitting "
                            + "classroom/faculty slot could be found — resolve manually"));
                }
                continue;
            }
            if (cell.subjectSpecialityId() == null) {
                unplaced.add(new AutoStaffUnplacedItem(cell.subjectName(),
                    "no department set on this subject — assign faculty manually"));
                continue;
            }
            if (cell.venueId() == null) {
                unplaced.add(new AutoStaffUnplacedItem(cell.subjectName(),
                    "no room committed in Capacity Planner for this session — commit one before staffing"));
                continue;
            }

            CourseOffering offering = cell.courseOfferingId() != null
                ? courseOfferingRepository.findById(cell.courseOfferingId()).orElse(null) : null;

            boolean staffed = staffFromSectionOverride(cell, offering);
            List<Faculty> ranked = List.of();
            if (!staffed) {
                Subject subject = offering != null ? offering.getSubject() : null;
                List<Faculty> basePool = facultyRepository.findBySpecialityIdAndStatus(cell.subjectSpecialityId(), FacultyStatus.ACTIVE);
                ranked = rankBySameSubjectReuse(
                    subject != null ? FacultyEligibility.eligibleFaculty(subject, basePool) : basePool, cell.courseOfferingId());
                for (Faculty candidate : ranked) {
                    try {
                        timetableStaffingService.staffCell(cell.id(), new StaffingAssignmentRequest(candidate.getId(), null));
                        staffed = true;
                        break;
                    } catch (TimetableConstraintViolationException | LifecycleConflictException ex) {
                        // try the next candidate
                    }
                }
            }
            if (staffed) {
                staffedCount++;
            } else {
                unplaced.add(new AutoStaffUnplacedItem(cell.subjectName(), ranked.isEmpty()
                    ? "no active faculty found in this subject's department"
                    : "every eligible faculty is unavailable, already committed, or over their workload cap at this slot"));
            }
        }
        return new AutoStaffResult(staffedCount, unplaced);
    }

    /** Elective cells are treated like any other Theory subject: the faculty is whatever's already
     *  assigned to this specific elective option via Assign Faculty (never a department-pool guess —
     *  {@code subjectSpecialityId} is deliberately NULL for every elective, so there is no pool to
     *  rank here), and the room is auto-picked best-fit-first since a student-choice elective can
     *  never have a room pre-committed in Capacity Planner (no single owning cohort). */
    private boolean autoStaffElective(UnstaffedCellResponse cell) {
        if (cell.courseOfferingId() == null) {
            return false;
        }
        Long facultyId = resolveElectiveFacultyId(cell.courseOfferingId());
        if (facultyId == null) {
            return false;
        }
        for (Classroom room : rankClassroomsByFit(cell.requiredStrength())) {
            try {
                timetableStaffingService.staffCell(cell.id(), new StaffingAssignmentRequest(facultyId, room.getId()));
                return true;
            } catch (TimetableConstraintViolationException | LifecycleConflictException ex) {
                // try the next room
            }
        }
        return false;
    }

    /** Mirrors {@link TimetableGlobalAutoScheduleService#resolveElectiveMemberFacultyId}: an
     *  elective option is unambiguously staffable only when every resolved section assignment for
     *  it agrees on exactly one faculty (including the common case of just one row). */
    private Long resolveElectiveFacultyId(Long courseOfferingId) {
        Set<Long> facultyIds = courseOfferingSectionFacultyService.getForOffering(courseOfferingId).sections().stream()
            .map(SectionFacultyAssignment::facultyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return facultyIds.size() == 1 ? facultyIds.iterator().next() : null;
    }

    /** Best-fit-first ranking for an elective's auto-picked room — mirrors the Staffing screen's own
     *  (now-retired) manual {@code roomOptionsFor}: classrooms that seat the required strength sort
     *  tightest-fit-first, then unknown capacity, then undersized (closest-to-fitting first). Never
     *  filtered out, just ranked — {@code staffCell}'s own capacity check still hard-blocks an actual
     *  over-capacity save, so a too-small room here is only ever a last-resort attempt. */
    private List<Classroom> rankClassroomsByFit(Integer requiredStrength) {
        List<Classroom> all = classroomRepository.findByIsActiveTrueOrderByNameAsc();
        if (requiredStrength == null) {
            return all;
        }
        List<Classroom> fits = all.stream()
            .filter(c -> c.getCapacity() != null && c.getCapacity() >= requiredStrength)
            .sorted(Comparator.comparingInt(Classroom::getCapacity))
            .toList();
        List<Classroom> unknown = all.stream().filter(c -> c.getCapacity() == null).toList();
        List<Classroom> tooSmall = all.stream()
            .filter(c -> c.getCapacity() != null && c.getCapacity() < requiredStrength)
            .sorted(Comparator.comparingInt(Classroom::getCapacity).reversed())
            .toList();
        List<Classroom> ranked = new ArrayList<>(fits);
        ranked.addAll(unknown);
        ranked.addAll(tooSmall);
        return ranked;
    }

    /** Prefers this section's own {@link CourseOfferingSectionFaculty} override when one exists and
     *  currently has free workload capacity — makes auto-staffing respect the same per-section
     *  assignment that's now authoritative for Global Auto-Schedule placement. Falls through
     *  (returns {@code false}, letting the caller try the ranked department pool instead) when
     *  there's no section, no override, the override is already over capacity, or staffing them
     *  still fails a scheduling constraint (busy elsewhere at this exact slot, etc). */
    private boolean staffFromSectionOverride(UnstaffedCellResponse cell, CourseOffering offering) {
        if (offering == null || cell.cohortSectionId() == null) {
            return false;
        }
        Optional<CourseOfferingSectionFaculty> override = courseOfferingSectionFacultyRepository
            .findByCourseOfferingIdAndCohortSectionId(offering.getId(), cell.cohortSectionId());
        if (override.isEmpty() || override.get().getFaculty().getStatus() != FacultyStatus.ACTIVE) {
            return false;
        }
        Long preferredId = override.get().getFaculty().getId();
        FacultyCapacityCheckResult check = timetableGlobalAutoScheduleService
            .checkFacultyCapacityForSection(offering.getId(), cell.cohortSectionId(), preferredId);
        if (check.overCapacity()) {
            return false;
        }
        try {
            timetableStaffingService.staffCell(cell.id(), new StaffingAssignmentRequest(preferredId, null));
            return true;
        } catch (TimetableConstraintViolationException | LifecycleConflictException ex) {
            return false;
        }
    }

    /** Prefers a faculty who already teaches other sessions of this exact subject (course
     *  offering) — a simple, server-computable proxy for reducing faculty fragmentation per
     *  subject. Not the same signal as the Staffing screen's own frontend hint (a session-local,
     *  day-scoped reuse tally with no server-side equivalent to reproduce), just aimed at the same
     *  underlying goal. */
    private List<Faculty> rankBySameSubjectReuse(List<Faculty> pool, Long courseOfferingId) {
        Map<Long, Long> reuseCounts = classScheduleRepository.findByCourseOfferingIdAndIsActiveTrue(courseOfferingId).stream()
            .filter(cs -> cs.getFaculty() != null)
            .collect(Collectors.groupingBy(cs -> cs.getFaculty().getId(), Collectors.counting()));
        return pool.stream()
            .sorted(Comparator.comparingLong((Faculty f) -> reuseCounts.getOrDefault(f.getId(), 0L)).reversed())
            .toList();
    }
}
