package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.service.SessionOccurrenceVenue.VenueResolution;

/**
 * Explodes PUBLISHED {@link ClassSchedule} rows onto real calendar dates within a window, for the
 * Month/Week/Day calendar views (Round 2 of the Timetable planner/calendar initiative) — the read
 * side of {@link ClassScheduleOccurrenceService}'s date math, scoped to either the whole term
 * (browse) or one identity's own sessions (personal, reusing {@link PersonalTimetableService}'s
 * existing resolution so the two screens never diverge on "what counts as mine").
 */
@Service
@Transactional(readOnly = true)
public class TimetableOccurrenceService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleService classScheduleService;
    private final ClassScheduleOccurrenceService occurrenceService;
    private final PersonalTimetableService personalTimetableService;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;

    public TimetableOccurrenceService(ClassScheduleRepository classScheduleRepository,
                                       ClassScheduleService classScheduleService,
                                       ClassScheduleOccurrenceService occurrenceService,
                                       PersonalTimetableService personalTimetableService,
                                       SessionOccurrenceRepository sessionOccurrenceRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.classScheduleService = classScheduleService;
        this.occurrenceService = occurrenceService;
        this.personalTimetableService = personalTimetableService;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
    }

    public List<ClassScheduleOccurrenceResponse> findOccurrences(
            ProfileIdentity identity, Long termInstanceId, LocalDate from, LocalDate to, String scope) {
        List<ClassSchedule> schedules = "personal".equalsIgnoreCase(scope)
            ? personalTimetableService.findPublishedSchedules(identity, termInstanceId)
            : classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED);

        Map<Long, List<LocalDate>> datesBySchedule =
            occurrenceService.occurrenceDatesForSchedules(schedules, from, to);
        Map<Long, List<ClassScheduleOccurrenceService.CancelledOccurrence>> cancelledBySchedule =
            occurrenceService.cancelledDatesForSchedules(schedules, from, to);

        List<ClassScheduleResponse> responses = classScheduleService.toResponseList(schedules);
        Map<Long, ClassScheduleResponse> responseById = new HashMap<>();
        for (ClassScheduleResponse response : responses) {
            responseById.put(response.id(), response);
        }

        Set<Long> scheduleIds = schedules.stream().map(ClassSchedule::getId).collect(Collectors.toSet());
        List<SessionOccurrence> substitutedOccurrences = sessionOccurrenceRepository
            .findByClassSchedule_TermInstance_IdAndClassSchedule_Status(termInstanceId, ClassScheduleStatus.PUBLISHED)
            .stream()
            .filter(occ -> occ.getOccurrenceStatus() == OccurrenceStatus.SUBSTITUTED)
            .filter(occ -> scheduleIds.contains(occ.getClassSchedule().getId()))
            .toList();

        // A SUBSTITUTED occurrence can carry a faculty override, a room relocation (BR-55
        // Room Relocation), both, or -- transiently, mid-revert -- neither; Collectors.toMap
        // forbids null values, so each map is independently filtered to rows that actually
        // carry that specific override rather than every SUBSTITUTED row.
        Map<Long, Map<LocalDate, Faculty>> substituteFacultyByScheduleAndDate = substitutedOccurrences.stream()
            .filter(occ -> occ.getEffectiveFaculty() != null)
            .collect(Collectors.groupingBy(occ -> occ.getClassSchedule().getId(),
                Collectors.toMap(SessionOccurrence::getOccurrenceDate, SessionOccurrence::getEffectiveFaculty)));
        Map<Long, Map<LocalDate, VenueResolution>> roomOverrideByScheduleAndDate = substitutedOccurrences.stream()
            .map(occ -> Map.entry(occ, SessionOccurrenceVenue.fromOccurrence(occ)))
            .filter(entry -> entry.getValue().venueId() != null)
            .collect(Collectors.groupingBy(entry -> entry.getKey().getClassSchedule().getId(),
                Collectors.toMap(entry -> entry.getKey().getOccurrenceDate(), Map.Entry::getValue)));

        List<ClassScheduleOccurrenceResponse> result = new ArrayList<>();
        for (ClassSchedule schedule : schedules) {
            ClassScheduleResponse response = responseById.get(schedule.getId());
            Map<LocalDate, Faculty> substitutesByDate =
                substituteFacultyByScheduleAndDate.getOrDefault(schedule.getId(), Map.of());
            Map<LocalDate, VenueResolution> roomOverridesByDate =
                roomOverrideByScheduleAndDate.getOrDefault(schedule.getId(), Map.of());
            for (LocalDate date : datesBySchedule.getOrDefault(schedule.getId(), List.of())) {
                Faculty substitute = substitutesByDate.get(date);
                VenueResolution roomOverride = roomOverridesByDate.get(date);
                if (substitute != null || roomOverride != null) {
                    result.add(new ClassScheduleOccurrenceResponse(
                        date, withOverrides(response, substitute, roomOverride), OccurrenceStatus.SUBSTITUTED, null));
                } else {
                    result.add(new ClassScheduleOccurrenceResponse(date, response, OccurrenceStatus.HELD, null));
                }
            }
            for (ClassScheduleOccurrenceService.CancelledOccurrence cancelled
                    : cancelledBySchedule.getOrDefault(schedule.getId(), List.of())) {
                result.add(new ClassScheduleOccurrenceResponse(cancelled.date(), response, OccurrenceStatus.CANCELLED, cancelled.reason()));
            }
        }
        result.sort(Comparator.comparing(ClassScheduleOccurrenceResponse::date)
            .thenComparing(o -> o.session().startTime()));
        return result;
    }

    /** Independently overrides the faculty fields (if {@code substitute != null}) and/or the room
     *  fields (if {@code roomOverride != null}) for a SUBSTITUTED occurrence — the recurring
     *  {@link ClassSchedule#getFaculty()}/venue are never mutated by substitution or relocation, so
     *  every other occurrence of the same schedule must keep showing the originals. A date can have
     *  either override, both, or (handled by the caller) neither. */
    private static ClassScheduleResponse withOverrides(ClassScheduleResponse r, Faculty substitute, VenueResolution roomOverride) {
        Long facultyId = substitute != null ? substitute.getId() : r.facultyId();
        String facultyName = substitute != null ? substitute.getFullName() : r.facultyName();

        Long labId = r.labId();
        String labName = r.labName();
        Long classroomId = r.classroomId();
        Long clinicalVenueId = r.clinicalVenueId();
        String roomName = r.roomName();
        if (roomOverride != null) {
            labId = roomOverride.lab() != null ? roomOverride.lab().getId() : null;
            labName = roomOverride.lab() != null ? roomOverride.lab().getName() : null;
            classroomId = roomOverride.classroom() != null ? roomOverride.classroom().getId() : null;
            clinicalVenueId = roomOverride.clinicalVenue() != null ? roomOverride.clinicalVenue().getId() : null;
            roomName = roomOverride.classroom() != null ? roomOverride.classroom().getName()
                : roomOverride.lab() != null ? roomOverride.lab().getName()
                : roomOverride.clinicalVenue() != null ? roomOverride.clinicalVenue().getName() : null;
        }

        return new ClassScheduleResponse(
            r.id(), r.sessionType(), r.status(),
            labId, labName,
            r.subjectId(), r.subjectName(), r.subjectCode(),
            facultyId, facultyName,
            r.periodId(), r.slotName(), r.startTime(), r.endTime(),
            r.batchName(), r.batchId(),
            classroomId, clinicalVenueId, roomName,
            r.courseOfferingId(),
            r.dayOfWeek(), r.termInstanceId(), r.termInstanceLabel(), r.isActive(),
            r.createdAt(), r.updatedAt());
    }
}
