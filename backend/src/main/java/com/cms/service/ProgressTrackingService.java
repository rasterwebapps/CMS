package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CoveredUnitDto;
import com.cms.dto.LogProgressRequest;
import com.cms.dto.OfferingProgressResponse;
import com.cms.dto.SessionOccurrenceDto;
import com.cms.dto.SubjectProgressSummaryDto;
import com.cms.dto.SyllabusUnitDto;
import com.cms.dto.TermProgressSummaryResponse;
import com.cms.dto.UnitProgressDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.SessionOccurrence;
import com.cms.model.SyllabusUnit;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SyllabusUnitRepository;

/**
 * Portion-completion progress tracking (Timetable planner Round 2, Phase 3) — records which
 * {@link SyllabusUnit}s were covered in a specific dated firing of a recurring {@link
 * ClassSchedule} row, via {@link SessionOccurrence}. Progress naturally comes out per-offering
 * (not per-curriculum) even though units are shared across every offering of a subject, because
 * occurrences key off {@code ClassSchedule}, which already carries {@code courseOffering}.
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

        List<Long> unitIds = request.unitIds() != null ? request.unitIds() : List.of();
        Set<SyllabusUnit> units = new LinkedHashSet<>(syllabusUnitRepository.findAllById(unitIds));
        if (units.size() != unitIds.size()) {
            throw new IllegalArgumentException("One or more syllabus units were not found");
        }
        CurriculumSemesterCourse csc = schedule.getCourseOffering() != null
            ? schedule.getCourseOffering().getCurriculumSemesterCourse() : null;
        for (SyllabusUnit unit : units) {
            if (csc == null || !unit.getCurriculumSemesterCourse().getId().equals(csc.getId())) {
                throw new IllegalArgumentException(
                    "Unit " + unit.getUnitNumber() + " does not belong to this session's subject");
            }
        }

        occurrence.setCoveredUnits(units);
        occurrence.setRemarks(request.remarks());
        if (recordedByFacultyId != null) {
            Faculty faculty = facultyRepository.findById(recordedByFacultyId).orElse(null);
            occurrence.setRecordedByFaculty(faculty);
        }
        return toDto(sessionOccurrenceRepository.save(occurrence));
    }

    /** Units the "Log Progress" dialog should offer for a session, resolved from that session's
     *  own subject rather than requiring the frontend to already know its curriculumTermCourseId. */
    public List<SyllabusUnitDto> getAvailableUnits(Long classScheduleId) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        CurriculumSemesterCourse csc = schedule.getCourseOffering() != null
            ? schedule.getCourseOffering().getCurriculumSemesterCourse() : null;
        if (csc == null) {
            return List.of();
        }
        return syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(csc.getId())
            .stream()
            .map(u -> new SyllabusUnitDto(u.getId(), csc.getId(), u.getUnitNumber(), u.getTitle(),
                u.getComponentType(), u.getPlannedHours(), u.getDescription(), u.getSortOrder(),
                u.getIsActive(), u.getCreatedAt(), u.getUpdatedAt()))
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

        Map<Long, List<LocalDate>> coveredDatesByUnit = new HashMap<>();
        for (SessionOccurrence occurrence : sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(courseOfferingId)) {
            for (SyllabusUnit unit : occurrence.getCoveredUnits()) {
                coveredDatesByUnit.computeIfAbsent(unit.getId(), k -> new ArrayList<>()).add(occurrence.getOccurrenceDate());
            }
        }

        List<UnitProgressDto> unitProgress = units.stream()
            .map(unit -> {
                List<LocalDate> dates = coveredDatesByUnit.getOrDefault(unit.getId(), List.of());
                return new UnitProgressDto(unit.getId(), unit.getUnitNumber(), unit.getTitle(),
                    unit.getComponentType(), unit.getPlannedHours(), !dates.isEmpty(), dates);
            })
            .toList();

        int coveredCount = (int) unitProgress.stream().filter(UnitProgressDto::covered).count();
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

    private SessionOccurrenceDto toDto(SessionOccurrence occurrence) {
        List<CoveredUnitDto> covered = occurrence.getCoveredUnits().stream()
            .map(u -> new CoveredUnitDto(u.getId(), u.getUnitNumber(), u.getTitle()))
            .toList();
        return new SessionOccurrenceDto(occurrence.getId(), occurrence.getClassSchedule().getId(),
            occurrence.getOccurrenceDate(), covered, occurrence.getRemarks(),
            occurrence.getCreatedAt(), occurrence.getUpdatedAt());
    }
}
