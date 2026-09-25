package com.cms.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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
import com.cms.model.Period;
import com.cms.model.SessionOccurrence;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
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
    private final TimetableSkeletonService timetableSkeletonService;

    public TimetableOccurrenceService(ClassScheduleRepository classScheduleRepository,
                                       ClassScheduleService classScheduleService,
                                       ClassScheduleOccurrenceService occurrenceService,
                                       PersonalTimetableService personalTimetableService,
                                       SessionOccurrenceRepository sessionOccurrenceRepository,
                                       TimetableSkeletonService timetableSkeletonService) {
        this.classScheduleRepository = classScheduleRepository;
        this.classScheduleService = classScheduleService;
        this.occurrenceService = occurrenceService;
        this.personalTimetableService = personalTimetableService;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
        this.timetableSkeletonService = timetableSkeletonService;
    }

    public List<ClassScheduleOccurrenceResponse> findOccurrences(
            ProfileIdentity identity, Long termInstanceId, LocalDate from, LocalDate to, String scope) {
        return findOccurrences(identity, termInstanceId, from, to, scope, null);
    }

    // cohortId only ever narrows scope=browse (the Timetable browse screen's Cohort filter) --
    // scope=personal is already self-scoped via PersonalTimetableService and ignores it.
    public List<ClassScheduleOccurrenceResponse> findOccurrences(
            ProfileIdentity identity, Long termInstanceId, LocalDate from, LocalDate to, String scope, Long cohortId) {
        List<ClassSchedule> schedules = "personal".equalsIgnoreCase(scope)
            ? personalTimetableService.findPublishedSchedules(identity, termInstanceId)
            : cohortId != null
                ? timetableSkeletonService.getCohortActiveClassSchedules(termInstanceId, cohortId, ClassScheduleStatus.PUBLISHED)
                : classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(termInstanceId, ClassScheduleStatus.PUBLISHED);

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
        // One row per (schedule, date) -- SUBSTITUTED (faculty/room override), CANCELLED (a
        // Reschedule's vacated original date, the only place a CANCELLED SessionOccurrence is
        // consulted at all -- every other cancellation reported below comes from a BlockedPeriod
        // match instead, a wholly separate mechanism), or RESCHEDULED (this same date is also
        // this row's own *target* date -- the "moved to a different period, same day" case).
        // The DB's own (class_schedule_id, occurrence_date) unique constraint guarantees at most
        // one row per key, so a plain (non-multi-valued) toMap is safe.
        Map<Long, Map<LocalDate, SessionOccurrence>> overlayByScheduleAndDate = sessionOccurrenceRepository
            .findByClassSchedule_TermInstance_IdAndClassSchedule_Status(termInstanceId, ClassScheduleStatus.PUBLISHED)
            .stream()
            .filter(occ -> scheduleIds.contains(occ.getClassSchedule().getId()))
            .filter(occ -> occ.getOccurrenceStatus() != OccurrenceStatus.HELD)
            .collect(Collectors.groupingBy(occ -> occ.getClassSchedule().getId(),
                Collectors.toMap(SessionOccurrence::getOccurrenceDate, occ -> occ)));

        List<ClassScheduleOccurrenceResponse> result = new ArrayList<>();
        for (ClassSchedule schedule : schedules) {
            ClassScheduleResponse response = responseById.get(schedule.getId());
            Map<LocalDate, SessionOccurrence> overlayByDate =
                overlayByScheduleAndDate.getOrDefault(schedule.getId(), Map.of());
            for (LocalDate date : datesBySchedule.getOrDefault(schedule.getId(), List.of())) {
                SessionOccurrence overlay = overlayByDate.get(date);
                result.add(toOccurrenceResponse(date, response, overlay));
            }
            for (ClassScheduleOccurrenceService.CancelledOccurrence cancelled
                    : cancelledBySchedule.getOrDefault(schedule.getId(), List.of())) {
                result.add(new ClassScheduleOccurrenceResponse(cancelled.date(), response, OccurrenceStatus.CANCELLED, cancelled.reason()));
            }
        }

        // Reschedule's other half: a REGULAR occurrence moved ONTO a date its own schedule
        // doesn't naturally recur on -- the per-schedule natural-date walk above can never
        // discover these (by definition the target date isn't one of that schedule's own dates),
        // so they're fetched directly by date range + RESCHEDULED status instead. Excludes dates
        // that ARE natural for their schedule (the "moved to a different period, same day" case,
        // where target == source) since the natural-date loop above already emitted that row --
        // without this exclusion it would appear twice. Bounded and cheap since callers of this
        // whole method only ever pass a single-day or single-week window (see the class javadoc).
        for (SessionOccurrence moved : sessionOccurrenceRepository
                .findByOccurrenceStatusAndOccurrenceDateBetweenAndClassSchedule_TermInstance_IdAndClassSchedule_Status(
                    OccurrenceStatus.RESCHEDULED, from, to, termInstanceId, ClassScheduleStatus.PUBLISHED)) {
            Long scheduleId = moved.getClassSchedule().getId();
            if (!scheduleIds.contains(scheduleId)) {
                continue;
            }
            if (datesBySchedule.getOrDefault(scheduleId, List.of()).contains(moved.getOccurrenceDate())) {
                continue;
            }
            ClassScheduleResponse base = responseById.get(scheduleId);
            if (base == null) {
                continue;
            }
            result.add(toOccurrenceResponse(moved.getOccurrenceDate(), base, moved));
        }

        // CLINICAL hours are delivered off-grid via ClinicalShiftGroup/Batch and never produce a real
        // ClassSchedule row (see TimetableSkeletonService#findClinicalShiftGridEntries's own doc
        // comment) -- withOUT this, this endpoint (unlike the published/draft list, which already
        // merges these via TimetableController#withClinicalShiftEntries) silently drops every
        // CLINICAL session from the Date-wise/Day calendar views. Not applied to scope=personal here;
        // PersonalTimetableService resolves an identity's own sessions separately.
        if (!"personal".equalsIgnoreCase(scope)) {
            for (ClassScheduleResponse template : timetableSkeletonService.findClinicalShiftGridEntries(
                    termInstanceId, ClassScheduleStatus.PUBLISHED, cohortId)) {
                for (LocalDate date : datesForDayOfWeek(template.dayOfWeek(), from, to)) {
                    result.add(new ClassScheduleOccurrenceResponse(date, template, OccurrenceStatus.HELD, null));
                }
            }
        }

        result.sort(Comparator.comparing(ClassScheduleOccurrenceResponse::date)
            .thenComparing(o -> o.session().startTime()));
        return result;
    }

    /** Every date in {@code [from, to]} matching {@code dayOfWeek} -- at most one per week, since
     *  callers only ever pass a single-day or single-week window. {@code com.cms.model.enums.
     *  DayOfWeek}'s names line up exactly with {@code java.time.DayOfWeek}'s (MONDAY..SATURDAY), the
     *  same direct mapping ClassScheduleOccurrenceService#weeklyDatesInRange already relies on. */
    private static List<LocalDate> datesForDayOfWeek(DayOfWeek dayOfWeek, LocalDate from, LocalDate to) {
        if (dayOfWeek == null) return List.of();
        java.time.DayOfWeek javaDayOfWeek = java.time.DayOfWeek.valueOf(dayOfWeek.name());
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = from.with(TemporalAdjusters.nextOrSame(javaDayOfWeek));
        while (!date.isAfter(to)) {
            dates.add(date);
            date = date.plusWeeks(1);
        }
        return dates;
    }

    /** Builds one occurrence entry for a (date, base template, overlay row) triple -- {@code
     *  overlay} is null for a plain HELD date. Branches on the overlay's own status: CANCELLED
     *  reports the vacated date with its remark as the reason; SUBSTITUTED/RESCHEDULED apply
     *  whichever of faculty/room/period the row actually carries (a SUBSTITUTED row never has a
     *  period override -- see {@link SessionOccurrence#getEffectivePeriod()} -- so passing it
     *  through unconditionally is safe for both statuses). */
    private static ClassScheduleOccurrenceResponse toOccurrenceResponse(
            LocalDate date, ClassScheduleResponse response, SessionOccurrence overlay) {
        if (overlay == null) {
            return new ClassScheduleOccurrenceResponse(date, response, OccurrenceStatus.HELD, null);
        }
        if (overlay.getOccurrenceStatus() == OccurrenceStatus.CANCELLED) {
            String reason = overlay.getRemarks() != null ? overlay.getRemarks() : "Cancelled";
            return new ClassScheduleOccurrenceResponse(date, response, OccurrenceStatus.CANCELLED, reason);
        }
        VenueResolution roomOverride = SessionOccurrenceVenue.fromOccurrence(overlay);
        ClassScheduleResponse withOverrides = withOverrides(response, overlay.getEffectiveFaculty(),
            roomOverride.venueId() != null ? roomOverride : null,
            overlay.getPeriod() != null ? overlay.getEffectivePeriod() : null);
        return new ClassScheduleOccurrenceResponse(date, withOverrides, overlay.getOccurrenceStatus(), overlay.getRemarks());
    }

    /** Independently overrides the faculty fields (if {@code substitute != null}), the room fields
     *  (if {@code roomOverride != null}), and the period/time fields (if {@code periodOverride !=
     *  null}, set only by Reschedule — SUBSTITUTED occurrences never touch period) — the recurring
     *  {@link ClassSchedule#getFaculty()}/venue/period are never mutated by substitution, relocation,
     *  or reschedule, so every other occurrence of the same schedule must keep showing the
     *  originals. A date can have any combination of these, or (handled by the caller) none. */
    private static ClassScheduleResponse withOverrides(ClassScheduleResponse r, Faculty substitute,
                                                         VenueResolution roomOverride, Period periodOverride) {
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

        Long periodId = r.periodId();
        String slotName = r.slotName();
        LocalTime startTime = r.startTime();
        LocalTime endTime = r.endTime();
        if (periodOverride != null) {
            periodId = periodOverride.getId();
            slotName = periodOverride.getName();
            startTime = periodOverride.getStartTime();
            endTime = periodOverride.getEndTime();
        }

        return new ClassScheduleResponse(
            r.id(), r.sessionType(), r.status(),
            labId, labName,
            r.subjectId(), r.subjectName(), r.subjectCode(),
            facultyId, facultyName,
            periodId, slotName, startTime, endTime,
            r.batchName(), r.batchId(),
            classroomId, clinicalVenueId, roomName,
            r.courseOfferingId(), r.termNumber(),
            r.dayOfWeek(), r.termInstanceId(), r.termInstanceLabel(), r.isActive(),
            r.createdAt(), r.updatedAt());
    }
}
