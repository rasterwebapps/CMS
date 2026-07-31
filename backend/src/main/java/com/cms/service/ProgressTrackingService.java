package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LogProgressRequest;
import com.cms.dto.OfferingProgressResponse;
import com.cms.dto.SessionOccurrenceDto;
import com.cms.dto.SubjectProgressSummaryDto;
import com.cms.dto.TermProgressSummaryResponse;
import com.cms.dto.UnitCoverageDto;
import com.cms.dto.UnitCoverageRequest;
import com.cms.dto.UnitPickerOptionDto;
import com.cms.dto.UnitProgressDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.SessionOccurrence;
import com.cms.model.SessionOccurrenceUnit;
import com.cms.model.SyllabusUnit;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SyllabusUnitRepository;

/**
 * Portion-completion progress tracking (Timetable planner Round 2, Phase 3) — records how much of
 * each {@link SyllabusUnit} was actually covered in a specific dated firing of a recurring {@link
 * ClassSchedule} row, via {@link SessionOccurrence}/{@link SessionOccurrenceUnit}. Hours-covered is
 * a faculty-entered record of real time spent, never assumed to equal the period's length; a unit
 * is "complete" only once a faculty deliberately says so, never inferred from hours crossing the
 * unit's plannedHours (a unit can genuinely finish early or run long). Progress naturally comes
 * out per-offering (not per-curriculum) even though units are shared across every offering of a
 * subject, because occurrences key off {@code ClassSchedule}, which already carries {@code
 * courseOffering}.
 */
@Service
@Transactional(readOnly = true)
public class ProgressTrackingService {

    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final SyllabusUnitRepository syllabusUnitRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final FacultyRepository facultyRepository;
    private final ClassScheduleOccurrenceService occurrenceService;

    public ProgressTrackingService(SessionOccurrenceRepository sessionOccurrenceRepository,
                                    ClassScheduleRepository classScheduleRepository,
                                    SyllabusUnitRepository syllabusUnitRepository,
                                    CourseOfferingRepository courseOfferingRepository,
                                    FacultyRepository facultyRepository,
                                    ClassScheduleOccurrenceService occurrenceService) {
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.syllabusUnitRepository = syllabusUnitRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.facultyRepository = facultyRepository;
        this.occurrenceService = occurrenceService;
    }

    @Transactional
    public SessionOccurrenceDto logCoverage(LogProgressRequest request, Long recordedByFacultyId) {
        ClassSchedule schedule = classScheduleRepository.findById(request.classScheduleId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Class schedule not found with id: " + request.classScheduleId()));
        // R3 Phase 6: an unstaffed skeleton cell (R3 Phase 4) or any other DRAFT row was never
        // actually held, so there's nothing real to log progress against.
        if (schedule.getStatus() != ClassScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Progress can only be logged against a published, live session");
        }

        LocalDate date = request.occurrenceDate();
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot log progress for a future date");
        }
        List<LocalDate> validOccurrence = occurrenceService.occurrenceDatesFor(schedule, date, date);
        if (validOccurrence.isEmpty()) {
            throw new IllegalArgumentException(
                date + " is not a real occurrence of this session (wrong weekday, outside the term, or a holiday)");
        }

        SessionOccurrence occurrence = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(request.classScheduleId(), date)
            .orElseGet(() -> new SessionOccurrence(schedule, date));

        List<UnitCoverageRequest> units = request.units() != null ? request.units() : List.of();
        CurriculumSemesterCourse csc = schedule.getCourseOffering() != null
            ? schedule.getCourseOffering().getCurriculumSemesterCourse() : null;

        List<SessionOccurrenceUnit> newCoverages = new ArrayList<>();
        for (UnitCoverageRequest unitRequest : units) {
            SyllabusUnit unit = syllabusUnitRepository.findById(unitRequest.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Syllabus unit not found with id: " + unitRequest.unitId()));
            if (csc == null || !unit.getCurriculumSemesterCourse().getId().equals(csc.getId())) {
                throw new IllegalArgumentException(
                    "Unit " + unit.getUnitNumber() + " does not belong to this session's subject");
            }
            if (unitRequest.hoursCovered() != null && unitRequest.hoursCovered().signum() < 0) {
                throw new IllegalArgumentException("Hours covered cannot be negative");
            }
            newCoverages.add(new SessionOccurrenceUnit(occurrence, unit,
                unitRequest.hoursCovered(), unitRequest.markedComplete()));
        }

        // Mutate the existing managed collection in place (never reassign it) so Hibernate's
        // orphanRemoval actually deletes rows dropped from a previous save of this same occurrence.
        occurrence.getUnitCoverages().clear();
        occurrence.getUnitCoverages().addAll(newCoverages);

        occurrence.setRemarks(request.remarks());
        if (recordedByFacultyId != null) {
            Faculty faculty = facultyRepository.findById(recordedByFacultyId).orElse(null);
            occurrence.setRecordedByFaculty(faculty);
        }
        return toDto(sessionOccurrenceRepository.save(occurrence));
    }

    /** Units the "Log Progress" dialog should offer for a session, with each unit's aggregate
     *  state across every date logged so far (not just the date being edited) so the picker can
     *  default to the actual current unit instead of a flat, stateless list of all units. */
    public List<UnitPickerOptionDto> getAvailableUnits(Long classScheduleId) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        CourseOffering offering = schedule.getCourseOffering();
        CurriculumSemesterCourse csc = offering != null ? offering.getCurriculumSemesterCourse() : null;
        if (csc == null) {
            return List.of();
        }
        List<SyllabusUnit> units = syllabusUnitRepository
            .findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(csc.getId());
        Map<Long, UnitAggregate> aggregates = aggregateByUnit(offering.getId());

        return units.stream()
            .map(unit -> {
                UnitAggregate agg = aggregates.getOrDefault(unit.getId(), UnitAggregate.EMPTY);
                return new UnitPickerOptionDto(unit.getId(), unit.getUnitNumber(), unit.getTitle(),
                    unit.getComponentType(), unit.getPlannedHours(), agg.hoursLogged(), agg.completed());
            })
            .toList();
    }

    /** Real calendar dates, bounded to today-or-earlier, that this session has actually occurred
     *  on -- the date picker in "Log Progress" only offers dates that could have happened. */
    public List<LocalDate> getLoggableOccurrenceDates(Long classScheduleId, LocalDate from) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        return occurrenceService.occurrenceDatesFor(schedule, from, LocalDate.now());
    }

    public Optional<SessionOccurrenceDto> getOccurrence(Long classScheduleId, LocalDate date) {
        return sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(classScheduleId, date)
            .map(this::toDto);
    }

    public OfferingProgressResponse getProgressForOffering(Long courseOfferingId) {
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Course offering not found with id: " + courseOfferingId));

        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        List<SyllabusUnit> units = csc != null
            ? syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(csc.getId())
            : List.of();

        Map<Long, UnitAggregate> aggregates = aggregateByUnit(courseOfferingId);

        List<UnitProgressDto> unitProgress = units.stream()
            .map(unit -> {
                UnitAggregate agg = aggregates.getOrDefault(unit.getId(), UnitAggregate.EMPTY);
                return new UnitProgressDto(unit.getId(), unit.getUnitNumber(), unit.getTitle(),
                    unit.getComponentType(), unit.getPlannedHours(), agg.hoursLogged(), agg.completed(), agg.dates());
            })
            .toList();

        int coveredCount = (int) unitProgress.stream().filter(UnitProgressDto::completed).count();
        double percent = units.isEmpty() ? 0.0 : (coveredCount * 100.0) / units.size();

        return new OfferingProgressResponse(courseOfferingId, offering.getSubject().getName(),
            offering.getSubject().getCode(), units.size(), coveredCount, percent, unitProgress);
    }

    public TermProgressSummaryResponse getOverallProgressSummary(Long termInstanceId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);

        List<SubjectProgressSummaryDto> subjects = new ArrayList<>();
        int totalUnitsSum = 0;
        int coveredUnitsSum = 0;
        for (CourseOffering offering : offerings) {
            if (Boolean.TRUE.equals(offering.getCurriculumSemesterCourse() != null
                    ? offering.getCurriculumSemesterCourse().getIsElective() : null)) {
                continue;
            }
            OfferingProgressResponse progress = getProgressForOffering(offering.getId());
            if (progress.totalUnits() == 0) {
                continue;
            }
            subjects.add(new SubjectProgressSummaryDto(offering.getId(), progress.subjectName(),
                progress.subjectCode(), progress.totalUnits(), progress.coveredUnitCount(), progress.percentComplete()));
            totalUnitsSum += progress.totalUnits();
            coveredUnitsSum += progress.coveredUnitCount();
        }

        double overallPercent = totalUnitsSum == 0 ? 0.0 : (coveredUnitsSum * 100.0) / totalUnitsSum;
        return new TermProgressSummaryResponse(termInstanceId, subjects, overallPercent);
    }

    /** Sums hoursCovered and ORs markedComplete for one unit across every session occurrence
     *  logged for a course offering -- a unit is "complete" the instant any occurrence marks it
     *  so, regardless of how that compares to its plannedHours. */
    private Map<Long, UnitAggregate> aggregateByUnit(Long courseOfferingId) {
        Map<Long, UnitAggregate> aggregates = new HashMap<>();
        for (SessionOccurrence occurrence : sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(courseOfferingId)) {
            for (SessionOccurrenceUnit coverage : occurrence.getUnitCoverages()) {
                Long unitId = coverage.getSyllabusUnit().getId();
                aggregates.merge(unitId,
                    new UnitAggregate(
                        coverage.getHoursCovered() != null ? coverage.getHoursCovered() : BigDecimal.ZERO,
                        Boolean.TRUE.equals(coverage.getMarkedComplete()),
                        new ArrayList<>(List.of(occurrence.getOccurrenceDate()))),
                    UnitAggregate::merge);
            }
        }
        return aggregates;
    }

    private record UnitAggregate(BigDecimal hoursLogged, boolean completed, List<LocalDate> dates) {
        static final UnitAggregate EMPTY = new UnitAggregate(BigDecimal.ZERO, false, List.of());

        static UnitAggregate merge(UnitAggregate a, UnitAggregate b) {
            List<LocalDate> mergedDates = new ArrayList<>(a.dates());
            mergedDates.addAll(b.dates());
            return new UnitAggregate(a.hoursLogged().add(b.hoursLogged()), a.completed() || b.completed(), mergedDates);
        }
    }

    private SessionOccurrenceDto toDto(SessionOccurrence occurrence) {
        List<UnitCoverageDto> coverages = occurrence.getUnitCoverages().stream()
            .map(c -> new UnitCoverageDto(c.getSyllabusUnit().getId(), c.getSyllabusUnit().getUnitNumber(),
                c.getSyllabusUnit().getTitle(), c.getHoursCovered(), Boolean.TRUE.equals(c.getMarkedComplete())))
            .toList();
        return new SessionOccurrenceDto(occurrence.getId(), occurrence.getClassSchedule().getId(),
            occurrence.getOccurrenceDate(), coverages, occurrence.getRemarks(),
            occurrence.getCreatedAt(), occurrence.getUpdatedAt());
    }
}
