package com.cms.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CalendarEventRequest;
import com.cms.dto.CalendarEventResponse;
import com.cms.dto.EventRecurrenceRequest;
import com.cms.dto.HolidayTemplateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.HolidayTemplate;
import com.cms.model.Period;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.HolidayCategory;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.HolidayTemplateRepository;
import com.cms.repository.PeriodRepository;

@Service
@Transactional(readOnly = true)
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;
    private final PeriodRepository periodRepository;
    private final HolidayTemplateRepository holidayTemplateRepository;
    private final HolidayTemplateService holidayTemplateService;

    public CalendarEventService(CalendarEventRepository calendarEventRepository,
                                AcademicYearRepository academicYearRepository,
                                BlockedPeriodRepository blockedPeriodRepository,
                                PeriodRepository periodRepository,
                                HolidayTemplateRepository holidayTemplateRepository,
                                HolidayTemplateService holidayTemplateService) {
        this.calendarEventRepository = calendarEventRepository;
        this.academicYearRepository = academicYearRepository;
        this.blockedPeriodRepository = blockedPeriodRepository;
        this.periodRepository = periodRepository;
        this.holidayTemplateRepository = holidayTemplateRepository;
        this.holidayTemplateService = holidayTemplateService;
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));
        validateDateRange(request, academicYear);
        HolidayCategory holidayCategory = resolveHolidayCategory(request);

        CalendarEvent event = new CalendarEvent();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setEventType(request.eventType());
        event.setHolidayCategory(holidayCategory);
        event.setAcademicYear(academicYear);

        CalendarEvent saved = calendarEventRepository.save(event);
        if (saved.getEventType() == CalendarEventType.HOLIDAY) {
            syncHolidayBlocks(saved, resolvePeriodIds(request.blockedPeriodIds()));
        }
        saved = applyRecurrence(saved, request);
        return toResponse(saved);
    }

    public List<CalendarEventResponse> findAll() {
        return calendarEventRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CalendarEventResponse findById(Long id) {
        return toResponse(calendarEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Calendar event not found with id: " + id)));
    }

    public List<CalendarEventResponse> findByAcademicYearId(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return calendarEventRepository.findByAcademicYearIdOrderByStartDate(academicYearId).stream()
            .map(this::toResponse).toList();
    }

    public List<CalendarEventResponse> findByAcademicYearIdAndEventType(
            Long academicYearId, CalendarEventType eventType) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return calendarEventRepository
            .findByAcademicYearIdAndEventTypeOrderByStartDate(academicYearId, eventType).stream()
            .map(this::toResponse).toList();
    }

    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request) {
        CalendarEvent event = calendarEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Calendar event not found with id: " + id));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));
        validateDateRange(request, academicYear);
        HolidayCategory holidayCategory = resolveHolidayCategory(request);

        boolean wasHoliday = event.getEventType() == CalendarEventType.HOLIDAY;
        LocalDate previousStart = event.getStartDate();
        LocalDate previousEnd = event.getEndDate();
        Set<Long> previousLinkedPeriodIds = wasHoliday
            ? blockedPeriodRepository.findBySourceCalendarEventId(id).stream()
                .map(bp -> bp.getPeriod().getId()).collect(Collectors.toSet())
            : Set.of();

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setEventType(request.eventType());
        event.setHolidayCategory(holidayCategory);
        event.setAcademicYear(academicYear);

        CalendarEvent saved = calendarEventRepository.save(event);
        boolean willBeHoliday = saved.getEventType() == CalendarEventType.HOLIDAY;

        if (willBeHoliday) {
            Set<Long> requestedPeriodIds = new HashSet<>(resolvePeriodIds(request.blockedPeriodIds()));
            // No-op guard: if the date range and period selection are unchanged from what's
            // already live, skip the resync entirely so a title/description-only edit can never
            // re-block a slot an admin manually unblocked (deleted) earlier. Only an actual change
            // to dates or periods re-materializes the full desired set, which can re-cover a
            // previously-freed slot -- an accepted, confirmed trade-off (see CLAUDE plan notes).
            boolean unchanged = wasHoliday
                && previousStart.equals(saved.getStartDate())
                && previousEnd.equals(saved.getEndDate())
                && previousLinkedPeriodIds.equals(requestedPeriodIds);
            if (!unchanged) {
                syncHolidayBlocks(saved, requestedPeriodIds);
            }
        } else if (wasHoliday) {
            blockedPeriodRepository.deleteBySourceCalendarEventId(id);
        }

        saved = applyRecurrence(saved, request);
        return toResponse(saved);
    }

    /** Reconciles the event's "Repeats" state against its (possibly absent) linked
     *  HolidayTemplate: creates one on the first repeat, updates it in place if the event is
     *  already linked, or deactivates it (without touching past/future sibling events) if
     *  repeats was turned off. Returns the possibly-updated event (re-fetched after the extra
     *  save when linkage changes, so the caller's response reflects it). */
    private CalendarEvent applyRecurrence(CalendarEvent saved, CalendarEventRequest request) {
        HolidayTemplate existingTemplate = saved.getSourceHolidayTemplate();
        boolean repeats = Boolean.TRUE.equals(request.repeats()) && request.recurrence() != null;

        if (repeats) {
            HolidayTemplateRequest templateRequest = buildTemplateRequest(request.recurrence(), saved);
            if (existingTemplate == null) {
                HolidayTemplate created = holidayTemplateService.createFromEvent(templateRequest);
                saved.setSourceHolidayTemplate(created);
                return calendarEventRepository.save(saved);
            }
            holidayTemplateService.update(existingTemplate.getId(), templateRequest);
            return saved;
        }
        if (existingTemplate != null) {
            holidayTemplateService.deactivate(existingTemplate.getId());
            saved.setSourceHolidayTemplate(null);
            return calendarEventRepository.save(saved);
        }
        return saved;
    }

    private HolidayTemplateRequest buildTemplateRequest(EventRecurrenceRequest recurrence, CalendarEvent event) {
        int durationDays = (int) ChronoUnit.DAYS.between(event.getStartDate(), event.getEndDate()) + 1;
        return new HolidayTemplateRequest(
            event.getTitle(),
            recurrence.recurrenceType(),
            event.getEventType(),
            event.getEventType() == CalendarEventType.HOLIDAY ? event.getHolidayCategory() : null,
            event.getDescription(),
            durationDays,
            recurrence.intervalCount(),
            event.getStartDate(),
            recurrence.endDate(),
            recurrence.month(),
            recurrence.dayOfMonth(),
            recurrence.weekOfMonth(),
            recurrence.dayOfWeek(),
            true);
    }

    @Transactional
    public void delete(Long id) {
        if (!calendarEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Calendar event not found with id: " + id);
        }
        blockedPeriodRepository.deleteBySourceCalendarEventId(id);
        calendarEventRepository.deleteById(id);
    }

    /** "Delete this and all future occurrences" for an event seeded from a HolidayTemplate: stops
     *  the template from seeding any further years, then deletes this event plus every other
     *  future-dated (>= today) event linked to the same template -- through the normal delete()
     *  path so each one's auto-blocks cascade correctly too. Past/already-occurred instances are
     *  never touched. */
    @Transactional
    public void deleteSeries(Long id) {
        CalendarEvent event = calendarEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Calendar event not found with id: " + id));
        HolidayTemplate template = event.getSourceHolidayTemplate();
        if (template == null) {
            throw new IllegalArgumentException(
                "This event was not created from a recurring Holiday Template -- delete it individually.");
        }
        template.setIsActive(false);
        holidayTemplateRepository.save(template);

        LocalDate today = LocalDate.now();
        List<CalendarEvent> futureInstances = calendarEventRepository
            .findBySourceHolidayTemplateIdAndStartDateGreaterThanEqual(template.getId(), today);
        for (CalendarEvent instance : futureInstances) {
            delete(instance.getId());
        }
    }

    public List<CalendarEventResponse> findOverlappingAnyType(
            Long academicYearId, LocalDate rangeStart, LocalDate rangeEnd, Long excludeId) {
        return calendarEventRepository.findOverlappingAnyType(academicYearId, rangeStart, rangeEnd, excludeId)
            .stream().map(this::toResponse).toList();
    }

    /** Creates a seeded event from a HolidayTemplate, using whatever eventType the template
     *  itself records (originally always HOLIDAY, now any type since a repeating event created
     *  inline from the Add Event form can seed any type) -- always whole-day when it is a
     *  HOLIDAY (templates don't carry a period subset, only the per-event Add Event form does).
     *  Used by {@code HolidayTemplateSeedingService} when a new AcademicYear is created. */
    @Transactional
    public CalendarEventResponse createSeededHolidayEvent(
            HolidayTemplate template, LocalDate start, LocalDate end, AcademicYear academicYear) {
        CalendarEvent event = new CalendarEvent();
        event.setTitle(template.getName());
        event.setDescription(template.getDescription());
        event.setStartDate(start);
        event.setEndDate(end);
        event.setEventType(template.getEventType());
        event.setHolidayCategory(template.getEventType() == CalendarEventType.HOLIDAY ? template.getHolidayCategory() : null);
        event.setAcademicYear(academicYear);
        event.setSourceHolidayTemplate(template);

        CalendarEvent saved = calendarEventRepository.save(event);
        if (saved.getEventType() == CalendarEventType.HOLIDAY) {
            syncHolidayBlocks(saved, resolvePeriodIds(null));
        }
        return toResponse(saved);
    }

    /** Null/empty means "whole day" -- every currently active period. Otherwise validates each
     *  requested id refers to a real, active period. */
    private List<Long> resolvePeriodIds(List<Long> requested) {
        if (requested == null || requested.isEmpty()) {
            return periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc().stream()
                .map(Period::getId).toList();
        }
        List<Period> periods = periodRepository.findAllById(requested);
        if (periods.size() != requested.size()) {
            throw new IllegalArgumentException("One or more selected periods do not exist");
        }
        for (Period p : periods) {
            if (!Boolean.TRUE.equals(p.getIsActive())) {
                throw new IllegalArgumentException("Period '" + p.getName() + "' is not active");
            }
        }
        return requested;
    }

    /** Diffs the desired (date x periodId) set for a HOLIDAY event's full date range against the
     *  currently-linked auto-blocks, deleting rows no longer desired and inserting missing ones.
     *  Skips inserting into any slot already covered by an existing block from any source (e.g.
     *  two overlapping holidays, or a pre-existing manual block on that exact period+date) to
     *  avoid duplicate rows double-counting in Capacity Planner hours math. */
    private void syncHolidayBlocks(CalendarEvent event, Collection<Long> resolvedPeriodIds) {
        List<BlockedPeriod> existingLinked = blockedPeriodRepository.findBySourceCalendarEventId(event.getId());
        Set<String> desired = new HashSet<>();
        for (LocalDate d = event.getStartDate(); !d.isAfter(event.getEndDate()); d = d.plusDays(1)) {
            for (Long periodId : resolvedPeriodIds) {
                desired.add(periodId + "|" + d);
            }
        }

        List<BlockedPeriod> toDelete = new ArrayList<>();
        Set<String> alreadyLinked = new HashSet<>();
        for (BlockedPeriod bp : existingLinked) {
            String key = bp.getPeriod().getId() + "|" + bp.getSpecificDate();
            alreadyLinked.add(key);
            if (!desired.contains(key)) {
                toDelete.add(bp);
            }
        }
        if (!toDelete.isEmpty()) {
            blockedPeriodRepository.deleteAll(toDelete);
        }

        Map<Long, Period> periodsById = periodRepository.findAllById(resolvedPeriodIds).stream()
            .collect(Collectors.toMap(Period::getId, Function.identity()));

        for (LocalDate d = event.getStartDate(); !d.isAfter(event.getEndDate()); d = d.plusDays(1)) {
            for (Long periodId : resolvedPeriodIds) {
                String key = periodId + "|" + d;
                if (alreadyLinked.contains(key)) {
                    continue;
                }
                if (blockedPeriodRepository.existsByPeriodIdAndBlockTypeAndSpecificDate(periodId, BlockType.ONE_OFF, d)) {
                    continue;
                }
                BlockedPeriod bp = new BlockedPeriod();
                bp.setPeriod(periodsById.get(periodId));
                bp.setBlockType(BlockType.ONE_OFF);
                bp.setSpecificDate(d);
                bp.setReason("Auto-blocked — " + event.getTitle());
                bp.setSourceCalendarEvent(event);
                blockedPeriodRepository.save(bp);
            }
        }
    }

    /** Mirrors the containment check {@code TermInstanceService.assertTermWithinAcademicYear}
     *  already enforces for terms -- an event dated outside its own linked Academic Year was
     *  previously accepted silently, which is exactly the class of bug that produced
     *  inconsistent term/calendar data before this was caught. */
    private void validateDateRange(CalendarEventRequest request, AcademicYear academicYear) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        if (request.startDate().isBefore(academicYear.getStartDate())
                || request.endDate().isAfter(academicYear.getEndDate())) {
            throw new IllegalArgumentException(
                "Event dates must fall within the academic year's dates (" +
                    academicYear.getStartDate() + " to " + academicYear.getEndDate() + ")");
        }
    }

    /** Holiday category (government/local/institutional) is only a meaningful classification for
     *  eventType == HOLIDAY — silently dropped for every other type so a non-holiday event can
     *  never carry a stray category from a prior edit. Optional (not required) even for holidays,
     *  since not every institution needs the classification. */
    private HolidayCategory resolveHolidayCategory(CalendarEventRequest request) {
        return request.eventType() == CalendarEventType.HOLIDAY ? request.holidayCategory() : null;
    }

    private CalendarEventResponse toResponse(CalendarEvent event) {
        AcademicYear ay = event.getAcademicYear();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            ay.getId(), ay.getName(), ay.getStartDate(), ay.getEndDate(),
            ay.getIsCurrent(), ay.getCreatedAt(), ay.getUpdatedAt());

        List<Long> blockedPeriodIds = event.getEventType() == CalendarEventType.HOLIDAY
            ? blockedPeriodRepository.findBySourceCalendarEventId(event.getId()).stream()
                .map(bp -> bp.getPeriod().getId()).distinct().toList()
            : List.of();
        HolidayTemplate template = event.getSourceHolidayTemplate();

        return new CalendarEventResponse(
            event.getId(), event.getTitle(), event.getDescription(),
            event.getStartDate(), event.getEndDate(), event.getEventType(), event.getHolidayCategory(),
            ayResponse, event.getCreatedAt(), event.getUpdatedAt(),
            blockedPeriodIds, template != null ? template.getId() : null, template != null ? template.getName() : null);
    }
}
