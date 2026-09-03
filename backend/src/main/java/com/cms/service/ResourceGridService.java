package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ClinicalShiftWindow;
import com.cms.dto.ResourceGridCellResponse;
import com.cms.dto.ResourceGridRowResponse;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.ClinicalVenue;
import com.cms.model.CourseOffering;
import com.cms.model.DayMappingOverride;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.LabStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;

/**
 * Master resource-matrix views (Faculty/Classroom) for the Timetable planner Round 2 initiative —
 * rows are every active resource of the requested type, not a single faculty/room filtered down
 * from one grid ({@link com.cms.controller.TimetableController} browse endpoint already covers
 * that narrower case). One day at a time, since a full week × every faculty/room would be a very
 * wide grid; the frontend's own Week/Day toggle (Phase 4) supplies the date navigation UI.
 */
@Service
@Transactional(readOnly = true)
public class ResourceGridService {

    public enum ResourceType { FACULTY, CLASSROOM }

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleService classScheduleService;
    private final FacultyRepository facultyRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final DayMappingOverrideRepository dayMappingOverrideRepository;
    private final ClinicalShiftGroupRepository clinicalShiftGroupRepository;
    private final BatchRepository batchRepository;

    /** Synthetic sessionId floor for a Clinical Shift cell (no backing {@code ClassSchedule} row)
     *  — {@code SHIFT_CELL_ID_BASE - batch.getId()} is always negative, so it can never collide
     *  with a real, positive-sequence {@code ClassSchedule} id. */
    private static final long SHIFT_CELL_ID_BASE = -1_000_000L;

    public ResourceGridService(ClassScheduleRepository classScheduleRepository,
                                ClassScheduleService classScheduleService,
                                FacultyRepository facultyRepository,
                                ClassroomRepository classroomRepository,
                                LabRepository labRepository,
                                ClinicalVenueRepository clinicalVenueRepository,
                                DayMappingOverrideRepository dayMappingOverrideRepository,
                                ClinicalShiftGroupRepository clinicalShiftGroupRepository,
                                BatchRepository batchRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.classScheduleService = classScheduleService;
        this.facultyRepository = facultyRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.dayMappingOverrideRepository = dayMappingOverrideRepository;
        this.clinicalShiftGroupRepository = clinicalShiftGroupRepository;
        this.batchRepository = batchRepository;
    }

    /** @param date when supplied, resolves the effective day-of-week through any {@link
     *              DayMappingOverride} covering it (falling back to the date's own actual
     *              weekday) and takes precedence over {@code dayOfWeek}; when null, behaves
     *              exactly as before, using {@code dayOfWeek} directly (backward compatible with
     *              the grid's existing Mon-Sat planning-mode toggle). */
    public List<ResourceGridRowResponse> getResourceGrid(ResourceType type, Long termInstanceId,
                                                           DayOfWeek dayOfWeek, LocalDate date) {
        if (date == null && dayOfWeek == null) {
            throw new IllegalArgumentException("Either date or dayOfWeek is required");
        }
        DayOfWeek effectiveDayOfWeek = date != null ? resolveEffectiveDayOfWeek(date) : dayOfWeek;
        List<ClassSchedule> daySchedules = classScheduleRepository
            .findByTermInstanceIdAndStatusAndDayOfWeek(termInstanceId, ClassScheduleStatus.PUBLISHED, effectiveDayOfWeek);

        Map<Long, ClassScheduleResponse> responseById = new HashMap<>();
        for (ClassScheduleResponse response : classScheduleService.toResponseList(daySchedules)) {
            responseById.put(response.id(), response);
        }

        // Clinical Shift sessions never produce a ClassSchedule row (see TimetableSkeletonService's
        // own comment on this), so without this they'd be invisible on this grid entirely even
        // though their bus-depart-to-bus-return window occupies a coordinator faculty and a
        // ClinicalVenue for real. Loaded once per request, filtered to this exact effective day.
        List<ClinicalShiftGroup> shiftGroupsToday = clinicalShiftGroupRepository
            .findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .filter(g -> g.getDayOfWeek() == effectiveDayOfWeek)
            .toList();

        return type == ResourceType.FACULTY
            ? facultyRows(daySchedules, responseById, shiftGroupsToday)
            : classroomAndLabRows(daySchedules, responseById, shiftGroupsToday);
    }

    private List<ResourceGridRowResponse> facultyRows(List<ClassSchedule> daySchedules,
                                                        Map<Long, ClassScheduleResponse> responseById,
                                                        List<ClinicalShiftGroup> shiftGroupsToday) {
        List<ResourceGridRowResponse> rows = new ArrayList<>();
        for (Faculty faculty : facultyRepository.findByStatus(FacultyStatus.ACTIVE)) {
            List<ResourceGridCellResponse> cells = new ArrayList<>(daySchedules.stream()
                .filter(cs -> cs.getFaculty() != null && cs.getFaculty().getId().equals(faculty.getId()))
                .map(cs -> toCell(responseById.get(cs.getId())))
                .toList());
            cells.addAll(shiftCellsFor(shiftGroupsToday,
                batch -> batch.getCoordinatorFaculty() != null && batch.getCoordinatorFaculty().getId().equals(faculty.getId())));
            rows.add(new ResourceGridRowResponse(faculty.getId(), faculty.getFullName(), cells));
        }
        return rows;
    }

    private List<ResourceGridRowResponse> classroomAndLabRows(List<ClassSchedule> daySchedules,
                                                                Map<Long, ClassScheduleResponse> responseById,
                                                                List<ClinicalShiftGroup> shiftGroupsToday) {
        List<ResourceGridRowResponse> rows = new ArrayList<>();
        for (Classroom classroom : classroomRepository.findByIsActiveTrueOrderByNameAsc()) {
            List<ResourceGridCellResponse> cells = daySchedules.stream()
                .filter(cs -> cs.getClassroom() != null && cs.getClassroom().getId().equals(classroom.getId()))
                .map(cs -> toCell(responseById.get(cs.getId())))
                .toList();
            rows.add(new ResourceGridRowResponse(classroom.getId(), classroom.getName(), cells));
        }
        for (Lab lab : labRepository.findAll()) {
            if (lab.getStatus() == LabStatus.INACTIVE || lab.getStatus() == LabStatus.UNDER_MAINTENANCE) {
                continue;
            }
            List<ResourceGridCellResponse> cells = daySchedules.stream()
                .filter(cs -> cs.getLab() != null && cs.getLab().getId().equals(lab.getId()))
                .map(cs -> toCell(responseById.get(cs.getId())))
                .toList();
            rows.add(new ResourceGridRowResponse(lab.getId(), lab.getName(), cells));
        }
        // R3 Phase 6: CLINICAL sessions live in their own ClinicalVenue master (never a
        // Classroom or Lab), so without this they'd silently never appear in this grid at all --
        // folded into the same "room" grid as Classroom/Lab rather than adding a whole new
        // ResourceType, since the frontend already treats this as one combined room view.
        for (ClinicalVenue venue : clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc()) {
            List<ResourceGridCellResponse> cells = new ArrayList<>(daySchedules.stream()
                .filter(cs -> cs.getClinicalVenue() != null && cs.getClinicalVenue().getId().equals(venue.getId()))
                .map(cs -> toCell(responseById.get(cs.getId())))
                .toList());
            cells.addAll(shiftCellsFor(shiftGroupsToday,
                batch -> batch.getClinicalVenue() != null && batch.getClinicalVenue().getId().equals(venue.getId())));
            rows.add(new ResourceGridRowResponse(venue.getId(), venue.getName(), cells));
        }
        return rows;
    }

    /** Every synthetic Clinical Shift cell, across {@code shiftGroupsToday}, whose linked active
     *  {@link Batch} matches {@code matches} (the calling row's own faculty/venue identity check).
     *  A group with no configured duration/buffer on its offering is skipped — its window can't be
     *  computed yet, matching {@link ClinicalShiftWindow#overlaps} treating that the same way. */
    private List<ResourceGridCellResponse> shiftCellsFor(List<ClinicalShiftGroup> shiftGroupsToday,
                                                           java.util.function.Predicate<Batch> matches) {
        List<ResourceGridCellResponse> cells = new ArrayList<>();
        for (ClinicalShiftGroup group : shiftGroupsToday) {
            ClinicalShiftWindow window = ClinicalShiftWindow.from(group);
            if (window.busDepart() == null || window.busReturn() == null) {
                continue;
            }
            for (Batch batch : batchRepository.findByClinicalShiftGroupId(group.getId())) {
                if (Boolean.TRUE.equals(batch.getIsActive()) && matches.test(batch)) {
                    cells.add(toShiftCell(group, window, batch));
                }
            }
        }
        return cells;
    }

    private ResourceGridCellResponse toShiftCell(ClinicalShiftGroup group, ClinicalShiftWindow window, Batch batch) {
        CourseOffering offering = group.getCourseOffering();
        return new ResourceGridCellResponse(
            SHIFT_CELL_ID_BASE - batch.getId(),
            offering.getSubject().getName() + " — Off-campus Clinical Shift",
            offering.getSubject().getCode(),
            batch.getClinicalVenue() != null ? batch.getClinicalVenue().getName() : null,
            batch.getCoordinatorFaculty() != null ? batch.getCoordinatorFaculty().getFullName() : null,
            batch.getName(),
            window.busDepart(),
            window.busReturn(),
            group.getLabel(),
            ClassSessionType.CLINICAL,
            ClassScheduleStatus.PUBLISHED,
            true);
    }

    private DayOfWeek resolveEffectiveDayOfWeek(LocalDate date) {
        if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No timetable data for Sunday");
        }
        return dayMappingOverrideRepository.findByMappedDate(date)
            .map(DayMappingOverride::getBorrowedDayOfWeek)
            .orElseGet(() -> DayOfWeek.valueOf(date.getDayOfWeek().name()));
    }

    private ResourceGridCellResponse toCell(ClassScheduleResponse r) {
        return new ResourceGridCellResponse(r.id(), r.subjectName(), r.subjectCode(), r.roomName(),
            r.facultyName(), r.batchName(), r.startTime(), r.endTime(), r.slotName(), r.sessionType(), r.status(), false);
    }
}
