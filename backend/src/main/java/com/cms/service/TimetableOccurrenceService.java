package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.model.ClassSchedule;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.repository.ClassScheduleRepository;

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

    public TimetableOccurrenceService(ClassScheduleRepository classScheduleRepository,
                                       ClassScheduleService classScheduleService,
                                       ClassScheduleOccurrenceService occurrenceService,
                                       PersonalTimetableService personalTimetableService) {
        this.classScheduleRepository = classScheduleRepository;
        this.classScheduleService = classScheduleService;
        this.occurrenceService = occurrenceService;
        this.personalTimetableService = personalTimetableService;
    }

    public List<ClassScheduleOccurrenceResponse> findOccurrences(
            ProfileIdentity identity, Long termInstanceId, LocalDate from, LocalDate to, String scope) {
        List<ClassSchedule> schedules = "personal".equalsIgnoreCase(scope)
            ? personalTimetableService.findPublishedSchedules(identity, termInstanceId)
            : classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED);

        Map<Long, List<LocalDate>> datesBySchedule =
            occurrenceService.occurrenceDatesForSchedules(schedules, from, to);

        List<ClassScheduleResponse> responses = classScheduleService.toResponseList(schedules);
        Map<Long, ClassScheduleResponse> responseById = new HashMap<>();
        for (ClassScheduleResponse response : responses) {
            responseById.put(response.id(), response);
        }

        List<ClassScheduleOccurrenceResponse> result = new ArrayList<>();
        for (ClassSchedule schedule : schedules) {
            ClassScheduleResponse response = responseById.get(schedule.getId());
            for (LocalDate date : datesBySchedule.getOrDefault(schedule.getId(), List.of())) {
                result.add(new ClassScheduleOccurrenceResponse(date, response));
            }
        }
        result.sort(Comparator.comparing(ClassScheduleOccurrenceResponse::date)
            .thenComparing(o -> o.session().startTime()));
        return result;
    }
}
