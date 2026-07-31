package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StaffSwapCandidateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.SessionOccurrenceRepository;

/**
 * Generalized single-date staff-to-staff session swap (Timetable planner Round 2, Phase 7) — two
 * PUBLISHED sessions on the same date trade faculty for that one date only, via {@link
 * SessionOccurrence} overrides (never mutating the recurring {@link ClassSchedule#getFaculty()}).
 * Deliberately a separate service from {@link TimetableSwapService}, which is explicitly DRAFT-only
 * and mutates day/period instead of faculty — conflating the two would violate that service's own
 * documented contract. Both directions of availability are checked (mutual swap), unlike Phase 6's
 * one-way absence substitute.
 */
@Service
@Transactional(readOnly = true)
public class FacultySessionSwapService {

    private final ClassScheduleRepository classScheduleRepository;
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;
    private final ClassScheduleOccurrenceService occurrenceService;

    public FacultySessionSwapService(ClassScheduleRepository classScheduleRepository,
                                      FacultyAvailabilityRepository facultyAvailabilityRepository,
                                      SessionOccurrenceRepository sessionOccurrenceRepository,
                                      ClassScheduleOccurrenceService occurrenceService) {
        this.classScheduleRepository = classScheduleRepository;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.occurrenceService = occurrenceService;
    }

    public List<StaffSwapCandidateResponse> findSwapCandidates(Long classScheduleId, LocalDate date) {
        ClassSchedule source = requirePublishedRealOccurrence(classScheduleId, date);
        LocalTime[] sourceTimes = resolveTimes(source);

        List<ClassSchedule> sameDay = classScheduleRepository.findByTermInstanceIdAndStatusAndDayOfWeek(
            source.getTermInstance().getId(), ClassScheduleStatus.PUBLISHED, source.getDayOfWeek());

        List<StaffSwapCandidateResponse> results = new ArrayList<>();
        for (ClassSchedule candidate : sameDay) {
            if (candidate.getId().equals(source.getId())) continue;
            if (candidate.getFaculty().getId().equals(source.getFaculty().getId())) continue;

            LocalTime[] candidateTimes = resolveTimes(candidate);
            boolean sourceFacultyFreeAtCandidateSlot = isFacultyFreeAt(source.getFaculty().getId(),
                source.getDayOfWeek(), candidateTimes[0], candidateTimes[1], source.getId(), source.getTermInstance().getId());
            boolean candidateFacultyFreeAtSourceSlot = isFacultyFreeAt(candidate.getFaculty().getId(),
                source.getDayOfWeek(), sourceTimes[0], sourceTimes[1], candidate.getId(), source.getTermInstance().getId());

            if (sourceFacultyFreeAtCandidateSlot && candidateFacultyFreeAtSourceSlot) {
                results.add(new StaffSwapCandidateResponse(candidate.getId(), candidate.getSubject().getName(),
                    candidate.getFaculty().getFullName(), candidateTimes[0], candidateTimes[1]));
            }
        }
        return results;
    }

    @Transactional
    public void applySwap(Long sessionAId, Long sessionBId, LocalDate date) {
        ClassSchedule a = requirePublishedRealOccurrence(sessionAId, date);
        ClassSchedule b = requirePublishedRealOccurrence(sessionBId, date);

        // Never trust a stale candidate list -- re-validate mutual availability from scratch.
        boolean stillValid = findSwapCandidates(sessionAId, date).stream()
            .anyMatch(c -> c.classScheduleId().equals(sessionBId));
        if (!stillValid) {
            throw new IllegalArgumentException("This swap is no longer available — availability may have changed since candidates were loaded");
        }

        SessionOccurrence occA = sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(sessionAId, date)
            .orElseGet(() -> new SessionOccurrence(a, date));
        SessionOccurrence occB = sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(sessionBId, date)
            .orElseGet(() -> new SessionOccurrence(b, date));

        occA.setEffectiveFaculty(b.getFaculty());
        occA.setOccurrenceStatus(com.cms.model.enums.OccurrenceStatus.SUBSTITUTED);
        occB.setEffectiveFaculty(a.getFaculty());
        occB.setOccurrenceStatus(com.cms.model.enums.OccurrenceStatus.SUBSTITUTED);

        SessionOccurrence savedA = sessionOccurrenceRepository.save(occA);
        SessionOccurrence savedB = sessionOccurrenceRepository.save(occB);
        savedA.setSwapPartnerOccurrence(savedB);
        savedB.setSwapPartnerOccurrence(savedA);
        sessionOccurrenceRepository.save(savedA);
        sessionOccurrenceRepository.save(savedB);
    }

    private boolean isFacultyFreeAt(Long facultyId, DayOfWeek day, LocalTime start, LocalTime end,
                                     Long excludeScheduleId, Long termInstanceId) {
        if (!facultyAvailabilityRepository.findOverlapping(facultyId, day, start, end).isEmpty()) {
            return false;
        }
        List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
            day, termInstanceId, start, end, ClassScheduleStatus.PUBLISHED, excludeScheduleId);
        return overlapping.stream().noneMatch(cs -> cs.getFaculty().getId().equals(facultyId));
    }

    private ClassSchedule requirePublishedRealOccurrence(Long classScheduleId, LocalDate date) {
        ClassSchedule schedule = classScheduleRepository.findById(classScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + classScheduleId));
        if (schedule.getStatus() != ClassScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published sessions can be staff-swapped");
        }
        if (occurrenceService.occurrenceDatesFor(schedule, date, date).isEmpty()) {
            throw new IllegalArgumentException(date + " is not a real occurrence of this session");
        }
        return schedule;
    }

    private LocalTime[] resolveTimes(ClassSchedule cs) {
        return new LocalTime[]{cs.getPeriod().getStartTime(), cs.getPeriod().getEndTime()};
    }
}
