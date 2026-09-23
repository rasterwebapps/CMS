package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.Notification;
import com.cms.model.Speciality;
import com.cms.model.enums.ClassSessionType;
import com.cms.repository.FacultyRepository;
import com.cms.repository.NotificationRepository;

/**
 * Fires an in-app {@link Notification}, scoped to exactly the affected people, the moment a
 * ONE_OFF {@link BlockedPeriod} (holiday-derived or manually created alike) cancels one or more
 * already-PUBLISHED {@link ClassSchedule} rows -- the gap identified alongside the Portion
 * Shortfall check: today nothing tells the affected faculty or their HOD/coordinator that a
 * specific already-published day just lost a session, only that the term is slipping overall.
 *
 * <p>Recipients: the session's own assigned faculty, plus -- for THEORY -- the subject's
 * Speciality HOD, or -- for LAB/CLINICAL -- the session's Batch coordinator (whichever role field
 * applies to that session type; never both). One {@link Notification} row per distinct recipient,
 * summarizing every session that recipient lost that date, so a faculty teaching several periods
 * on a cancelled day gets one alert, not one per period.
 *
 * <p>RECURRING blocks never notify (see {@link ClassScheduleOccurrenceService#schedulesDisruptedBy}
 * -- they're routine, known-in-advance non-teaching time). A compensatory {@code
 * DayMappingOverride} Saturday recovering the same content doesn't get a separate "un-cancelled"
 * notification either -- it's simply never seen as disrupted in the first place, since {@link
 * ClassScheduleOccurrenceService} already resolves it as a real occurrence of the borrowed
 * weekday's schedule.
 */
@Service
public class HolidayDisruptionNotificationService {

    private static final String CATEGORY_KEY = "holidayDisruption";
    private static final String SOURCE_TYPE_CALENDAR_EVENT = "CALENDAR_EVENT";
    private static final String SOURCE_TYPE_BLOCKED_PERIOD = "BLOCKED_PERIOD";

    private final ClassScheduleOccurrenceService occurrenceService;
    private final FacultyRepository facultyRepository;
    private final NotificationRepository notificationRepository;

    public HolidayDisruptionNotificationService(ClassScheduleOccurrenceService occurrenceService,
                                                  FacultyRepository facultyRepository,
                                                  NotificationRepository notificationRepository) {
        this.occurrenceService = occurrenceService;
        this.facultyRepository = facultyRepository;
        this.notificationRepository = notificationRepository;
    }

    /** Manual single-block path ({@code BlockedPeriodService.create}): one block, one date, one
     *  period. */
    @Transactional
    public void notifyIfDisrupts(BlockedPeriod block) {
        notify(SOURCE_TYPE_BLOCKED_PERIOD, block.getId(), block.getSpecificDate(),
            occurrenceService.schedulesDisruptedBy(block), block.getReason());
    }

    /** Holiday path ({@code CalendarEventService.syncHolidayBlocks}): every ONE_OFF block just
     *  created for ONE date (potentially several periods, e.g. a whole-day holiday), batched into a
     *  single call so a faculty teaching multiple periods that day gets one notification, not one
     *  per period. {@code blocksForOneDate} must all share the same {@code specificDate}. */
    @Transactional
    public void notifyIfDisrupts(CalendarEvent event, List<BlockedPeriod> blocksForOneDate) {
        if (blocksForOneDate.isEmpty()) {
            return;
        }
        LocalDate date = blocksForOneDate.get(0).getSpecificDate();
        List<ClassSchedule> disrupted = new ArrayList<>();
        for (BlockedPeriod block : blocksForOneDate) {
            disrupted.addAll(occurrenceService.schedulesDisruptedBy(block));
        }
        notify(SOURCE_TYPE_CALENDAR_EVENT, event.getId(), date, disrupted,
            "Auto-blocked — " + event.getTitle());
    }

    private void notify(String sourceType, Long sourceId, LocalDate date,
                         List<ClassSchedule> disrupted, String reason) {
        if (disrupted.isEmpty()) {
            return;
        }

        Map<Long, Faculty> recipientsById = new LinkedHashMap<>();
        Map<Long, List<ClassSchedule>> entriesByRecipient = new LinkedHashMap<>();
        for (ClassSchedule cs : disrupted) {
            addEntry(recipientsById, entriesByRecipient, cs.getFaculty(), cs);
            Faculty other = resolveHodOrCoordinator(cs);
            if (other != null && !other.getId().equals(cs.getFaculty().getId())) {
                addEntry(recipientsById, entriesByRecipient, other, cs);
            }
        }

        for (Map.Entry<Long, List<ClassSchedule>> entry : entriesByRecipient.entrySet()) {
            Long recipientId = entry.getKey();
            List<ClassSchedule> entries = entry.getValue();
            notificationRepository.save(new Notification(
                CATEGORY_KEY,
                "Session cancelled: " + date,
                buildMessage(recipientId, date, entries, reason),
                "/timetable?date=" + date,
                sourceType,
                sourceId,
                recipientId
            ));
        }
    }

    private void addEntry(Map<Long, Faculty> recipientsById, Map<Long, List<ClassSchedule>> entriesByRecipient,
                           Faculty recipient, ClassSchedule cs) {
        if (recipient == null) {
            return;
        }
        recipientsById.putIfAbsent(recipient.getId(), recipient);
        entriesByRecipient.computeIfAbsent(recipient.getId(), id -> new ArrayList<>()).add(cs);
    }

    /** THEORY sessions escalate to the subject's Speciality HOD; LAB/CLINICAL sessions escalate to
     *  the session's Batch coordinator -- the two existing role fields this codebase already uses
     *  for "who else is responsible for this session," per session type. Never both. */
    private Faculty resolveHodOrCoordinator(ClassSchedule cs) {
        if (cs.getSessionType() == ClassSessionType.THEORY) {
            if (cs.getSubject() == null) {
                return null;
            }
            Speciality speciality = cs.getSubject().getSpeciality();
            Long hodFacultyId = speciality != null ? speciality.getHodFacultyId() : null;
            return hodFacultyId != null ? facultyRepository.findById(hodFacultyId).orElse(null) : null;
        }
        return cs.getBatch() != null ? cs.getBatch().getCoordinatorFaculty() : null;
    }

    private String buildMessage(Long recipientFacultyId, LocalDate date, List<ClassSchedule> entries, String reason) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            ClassSchedule cs = entries.get(i);
            if (i > 0) {
                sb.append("; ");
            }
            if (cs.getFaculty().getId().equals(recipientFacultyId)) {
                sb.append("your ");
            } else {
                sb.append(cs.getFaculty().getFirstName()).append(" ").append(cs.getFaculty().getLastName()).append("'s ");
            }
            sb.append(cs.getSubject().getName()).append(" (").append(cs.getPeriod().getName()).append(")");
        }
        sb.append(" on ").append(date).append(" won't run — ").append(reason).append(".");
        return sb.toString();
    }
}
