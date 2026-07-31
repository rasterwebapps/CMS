package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AffectedSessionResponse;
import com.cms.dto.FacultyAbsenceDto;
import com.cms.dto.FacultyAbsenceRequest;
import com.cms.dto.SubstituteCandidateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.FacultyAbsence;
import com.cms.model.SessionOccurrence;
import com.cms.model.Speciality;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAbsenceRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SessionOccurrenceRepository;

/**
 * Faculty absence marking + substitute suggestion (Timetable planner Round 2, Phase 6). A date
 * marked absent never mutates the recurring {@link ClassSchedule} row — a substitute (if applied)
 * is recorded on that one date's {@link SessionOccurrence} instead (V327 additive extension of the
 * same spine Phase 3 uses for progress logging).
 */
@Service
@Transactional(readOnly = true)
public class FacultyAbsenceService {

    private final FacultyAbsenceRepository facultyAbsenceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;

    public FacultyAbsenceService(FacultyAbsenceRepository facultyAbsenceRepository,
                                  ClassScheduleRepository classScheduleRepository,
                                  FacultyRepository facultyRepository,
                                  FacultyAvailabilityRepository facultyAvailabilityRepository,
                                  SessionOccurrenceRepository sessionOccurrenceRepository) {
        this.facultyAbsenceRepository = facultyAbsenceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.facultyRepository = facultyRepository;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
    }

    @Transactional
    public FacultyAbsenceDto markAbsent(FacultyAbsenceRequest request, String recordedBy) {
        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

        FacultyAbsence absence = facultyAbsenceRepository
            .findByFacultyIdAndAbsenceDate(request.facultyId(), request.absenceDate())
            .orElseGet(() -> new FacultyAbsence(faculty, request.absenceDate(), request.reason(), recordedBy));
        absence.setReason(request.reason());
        absence.setRecordedBy(recordedBy);
        return toDto(facultyAbsenceRepository.save(absence));
    }

    public FacultyAbsenceDto getAbsence(Long absenceId) {
        return toDto(findAbsenceOrThrow(absenceId));
    }

    /** PUBLISHED sessions this faculty teaches on the absence date's weekday -- Sunday never has
     *  any (com.cms.model.enums.DayOfWeek has no SUNDAY value), so an absence marked on a Sunday
     *  simply has no affected sessions. */
    public List<AffectedSessionResponse> findAffectedSessions(Long absenceId) {
        FacultyAbsence absence = findAbsenceOrThrow(absenceId);
        java.time.DayOfWeek javaDay = absence.getAbsenceDate().getDayOfWeek();
        if (javaDay == java.time.DayOfWeek.SUNDAY) {
            return List.of();
        }
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(javaDay.name());

        List<ClassSchedule> candidates = classScheduleRepository.findByFacultyIdAndStatusAndDayOfWeek(
            absence.getFaculty().getId(), ClassScheduleStatus.PUBLISHED, dayOfWeek);

        List<AffectedSessionResponse> result = new ArrayList<>();
        for (ClassSchedule cs : candidates) {
            LocalDate termStart = cs.getTermInstance().getStartDate();
            LocalDate termEnd = cs.getTermInstance().getEndDate();
            if (absence.getAbsenceDate().isBefore(termStart) || absence.getAbsenceDate().isAfter(termEnd)) {
                continue;
            }
            SessionOccurrence occurrence = sessionOccurrenceRepository
                .findByClassScheduleIdAndOccurrenceDate(cs.getId(), absence.getAbsenceDate()).orElse(null);
            String roomName = resolveRoomName(cs);
            String slotName = cs.getPeriod() != null ? cs.getPeriod().getName() : null;
            LocalTime[] times = resolveTimes(cs);
            result.add(new AffectedSessionResponse(cs.getId(), cs.getSubject().getName(), cs.getSubject().getCode(),
                roomName, slotName, times[0], times[1], cs.getBatchName(),
                occurrence != null ? occurrence.getOccurrenceStatus() : OccurrenceStatus.HELD,
                occurrence != null && occurrence.getEffectiveFaculty() != null
                    ? occurrence.getEffectiveFaculty().getFullName() : null));
        }
        return result;
    }

    /** Eligible substitutes: same speciality as the subject, active, not the absent faculty
     *  themselves, free of any recurring FacultyAvailability block at that day/time, and not
     *  already teaching another PUBLISHED session at that exact day/time. */
    public List<SubstituteCandidateResponse> findEligibleSubstitutes(Long classScheduleId, LocalDate date) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        // R3 Phase 6: an unstaffed skeleton cell (R3 Phase 4) has no faculty yet, so "find a
        // substitute for the absent teacher" is meaningless here -- guard explicitly rather than
        // NPE on schedule.getFaculty() below. Normal UI navigation only ever reaches this via a
        // PUBLISHED row (see findAffectedSessions), but the REST endpoint takes a bare id.
        if (schedule.getStatus() != ClassScheduleStatus.PUBLISHED || schedule.getFaculty() == null) {
            throw new IllegalArgumentException("This session is not a published, staffed class and has no faculty to substitute");
        }
        Speciality speciality = schedule.getSubject().getSpeciality();
        if (speciality == null) {
            return List.of();
        }
        LocalTime[] times = resolveTimes(schedule);
        DayOfWeek day = schedule.getDayOfWeek();

        List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
            day, schedule.getTermInstance().getId(), times[0], times[1], ClassScheduleStatus.PUBLISHED, schedule.getId());

        return facultyRepository.findBySpecialityIdAndStatus(speciality.getId(), FacultyStatus.ACTIVE).stream()
            .filter(f -> !f.getId().equals(schedule.getFaculty().getId()))
            .filter(f -> facultyAvailabilityRepository.findOverlapping(f.getId(), day, times[0], times[1]).isEmpty())
            .filter(f -> overlapping.stream().noneMatch(cs -> cs.getFaculty().getId().equals(f.getId())))
            .map(f -> new SubstituteCandidateResponse(f.getId(), f.getFullName()))
            .toList();
    }

    @Transactional
    public AffectedSessionResponse applySubstitute(Long absenceId, Long classScheduleId, Long substituteFacultyId) {
        FacultyAbsence absence = findAbsenceOrThrow(absenceId);
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));

        // Never trust a stale candidate list -- re-validate eligibility at apply time.
        boolean stillEligible = findEligibleSubstitutes(classScheduleId, absence.getAbsenceDate()).stream()
            .anyMatch(c -> c.facultyId().equals(substituteFacultyId));
        if (!stillEligible) {
            throw new IllegalArgumentException("This substitute is no longer eligible for this session/date");
        }
        Faculty substitute = facultyRepository.findById(substituteFacultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + substituteFacultyId));

        SessionOccurrence occurrence = sessionOccurrenceRepository
            .findByClassScheduleIdAndOccurrenceDate(classScheduleId, absence.getAbsenceDate())
            .orElseGet(() -> new SessionOccurrence(schedule, absence.getAbsenceDate()));
        occurrence.setEffectiveFaculty(substitute);
        occurrence.setFacultyAbsence(absence);
        occurrence.setOccurrenceStatus(OccurrenceStatus.SUBSTITUTED);
        sessionOccurrenceRepository.save(occurrence);

        LocalTime[] times = resolveTimes(schedule);
        String roomName = resolveRoomName(schedule);
        String slotName = schedule.getPeriod() != null ? schedule.getPeriod().getName() : null;
        return new AffectedSessionResponse(schedule.getId(), schedule.getSubject().getName(), schedule.getSubject().getCode(),
            roomName, slotName, times[0], times[1], schedule.getBatchName(), OccurrenceStatus.SUBSTITUTED, substitute.getFullName());
    }

    private LocalTime[] resolveTimes(ClassSchedule cs) {
        return new LocalTime[]{cs.getPeriod().getStartTime(), cs.getPeriod().getEndTime()};
    }

    private String resolveRoomName(ClassSchedule cs) {
        return switch (cs.getSessionType()) {
            case THEORY -> cs.getClassroom() != null ? cs.getClassroom().getName() : null;
            case LAB -> cs.getLab() != null ? cs.getLab().getName() : null;
            case CLINICAL -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getName() : null;
        };
    }

    private FacultyAbsence findAbsenceOrThrow(Long absenceId) {
        return facultyAbsenceRepository.findById(absenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty absence not found with id: " + absenceId));
    }

    private FacultyAbsenceDto toDto(FacultyAbsence absence) {
        return new FacultyAbsenceDto(absence.getId(), absence.getFaculty().getId(), absence.getFaculty().getFullName(),
            absence.getAbsenceDate(), absence.getReason(), absence.getRecordedBy(),
            absence.getCreatedAt(), absence.getUpdatedAt());
    }
}
