package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.HolidayTemplateRequest;
import com.cms.dto.HolidayTemplateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.HolidayTemplate;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.repository.HolidayTemplateRepository;

@Service
@Transactional(readOnly = true)
public class HolidayTemplateService {

    private final HolidayTemplateRepository holidayTemplateRepository;

    public HolidayTemplateService(HolidayTemplateRepository holidayTemplateRepository) {
        this.holidayTemplateRepository = holidayTemplateRepository;
    }

    @Transactional
    public HolidayTemplateResponse create(HolidayTemplateRequest request) {
        String name = requireTrimmed(request.name(), "Name is required");
        if (holidayTemplateRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A holiday template named '" + name + "' already exists");
        }
        validateShape(request);

        HolidayTemplate template = new HolidayTemplate();
        applyRequest(template, request, name);
        return toResponse(holidayTemplateRepository.save(template));
    }

    public List<HolidayTemplateResponse> findAll() {
        return holidayTemplateRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Page<HolidayTemplateResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return holidayTemplateRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<HolidayTemplate> spec = (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), pattern);
        return holidayTemplateRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public HolidayTemplateResponse findById(Long id) {
        return toResponse(holidayTemplateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holiday template not found with id: " + id)));
    }

    @Transactional
    public HolidayTemplateResponse update(Long id, HolidayTemplateRequest request) {
        HolidayTemplate template = holidayTemplateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holiday template not found with id: " + id));
        String name = requireTrimmed(request.name(), "Name is required");
        if (holidayTemplateRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A holiday template named '" + name + "' already exists");
        }
        validateShape(request);

        applyRequest(template, request, name);
        return toResponse(holidayTemplateRepository.save(template));
    }

    /** Used only by {@code CalendarEventService} when a repeating event is created inline from
     *  the Add Event form -- skips the name-uniqueness check (an auto-derived name from an
     *  event's own title has no reason to collide-check against admin-authored templates; a
     *  collision here just gets a disambiguating suffix instead of hard-failing the event save)
     *  but still runs the same shape validation as every other write path. */
    @Transactional
    public HolidayTemplate createFromEvent(HolidayTemplateRequest request) {
        String name = requireTrimmed(request.name(), "Name is required");
        if (holidayTemplateRepository.existsByNameIgnoreCase(name)) {
            name = name + " #" + System.currentTimeMillis();
        }
        validateShape(request);

        HolidayTemplate template = new HolidayTemplate();
        applyRequest(template, request, name);
        return holidayTemplateRepository.save(template);
    }

    /** Stops future seeding without touching already-seeded historical events (their
     *  source_holiday_template_id FK is ON DELETE SET NULL, not cascading delete). Used when an
     *  event's "Repeats" toggle is turned off after having been on. */
    @Transactional
    public void deactivate(Long id) {
        HolidayTemplate template = holidayTemplateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holiday template not found with id: " + id));
        template.setIsActive(false);
        holidayTemplateRepository.save(template);
    }

    @Transactional
    public void delete(Long id) {
        if (!holidayTemplateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Holiday template not found with id: " + id);
        }
        // Already-seeded calendar_events keep their own history (source_holiday_template_id ->
        // ON DELETE SET NULL) -- this never retroactively touches historical events.
        holidayTemplateRepository.deleteById(id);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return holidayTemplateRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return holidayTemplateRepository.existsByNameIgnoreCase(trimmed);
    }

    /** Mirrors the DB CHECK constraint that used to live on this table (chk_holiday_template_shape,
     *  dropped in V356 -- the 4-way branch below plus MONTHLY's two mutually exclusive sub-patterns
     *  became too unwieldy to express as a single SQL CHECK) at the app layer, for a friendlier
     *  error message than a raw constraint violation. */
    private void validateShape(HolidayTemplateRequest request) {
        int durationDays = request.durationDays() != null ? request.durationDays() : 1;
        if (durationDays <= 0) {
            throw new IllegalArgumentException("Duration (days) must be greater than zero");
        }
        int intervalCount = request.intervalCount() != null ? request.intervalCount() : 1;
        if (intervalCount <= 0) {
            throw new IllegalArgumentException("Interval must be greater than zero");
        }
        boolean needsAnchor = request.recurrenceType() == HolidayRecurrenceType.DAILY || intervalCount > 1;
        if (needsAnchor && request.anchorDate() == null) {
            throw new IllegalArgumentException(
                "A start date is required " + (intervalCount > 1
                    ? "when repeating every " + intervalCount + " units" : "for a daily repeat"));
        }
        if (request.endDate() != null && request.anchorDate() != null
                && request.endDate().isBefore(request.anchorDate())) {
            throw new IllegalArgumentException("End date must not be before the start date");
        }

        switch (request.recurrenceType()) {
            case YEARLY -> {
                if (request.month() == null || request.month() < 1 || request.month() > 12) {
                    throw new IllegalArgumentException("A valid month (1-12) is required for a yearly repeat");
                }
                if (request.dayOfMonth() == null || request.dayOfMonth() < 1 || request.dayOfMonth() > 31) {
                    throw new IllegalArgumentException("A valid day of month (1-31) is required for a yearly repeat");
                }
                if (request.weekOfMonth() != null || request.dayOfWeek() != null) {
                    throw new IllegalArgumentException("Week-of-month/day-of-week only apply to a monthly repeat");
                }
            }
            case MONTHLY -> {
                boolean hasFixedDay = request.dayOfMonth() != null;
                boolean hasNthWeekday = request.weekOfMonth() != null || request.dayOfWeek() != null;
                if (hasFixedDay == hasNthWeekday) {
                    throw new IllegalArgumentException(
                        "A monthly repeat needs either a day of month, or a week + day of week -- not both, not neither");
                }
                if (hasFixedDay && (request.dayOfMonth() < 1 || request.dayOfMonth() > 31)) {
                    throw new IllegalArgumentException("Day of month must be between 1 and 31");
                }
                if (hasNthWeekday && (request.weekOfMonth() == null || request.dayOfWeek() == null)) {
                    throw new IllegalArgumentException("Both week and day of week are required for an nth-weekday monthly repeat");
                }
                if (request.month() != null) {
                    throw new IllegalArgumentException("Month only applies to a yearly repeat");
                }
            }
            case WEEKLY -> {
                if (request.dayOfWeek() == null) {
                    throw new IllegalArgumentException("Day of week is required for a weekly repeat");
                }
                if (request.month() != null || request.dayOfMonth() != null || request.weekOfMonth() != null) {
                    throw new IllegalArgumentException("Month/day-of-month/week-of-month only apply to yearly or monthly repeats");
                }
            }
            case DAILY -> {
                if (request.month() != null || request.dayOfMonth() != null
                        || request.weekOfMonth() != null || request.dayOfWeek() != null) {
                    throw new IllegalArgumentException("A daily repeat needs no day/month/week pattern fields");
                }
            }
        }
    }

    private void applyRequest(HolidayTemplate template, HolidayTemplateRequest request, String name) {
        CalendarEventType eventType = request.eventType() != null ? request.eventType() : CalendarEventType.HOLIDAY;
        template.setName(name);
        template.setRecurrenceType(request.recurrenceType());
        template.setEventType(eventType);
        template.setHolidayCategory(eventType == CalendarEventType.HOLIDAY ? request.holidayCategory() : null);
        template.setDescription(trim(request.description()));
        template.setDurationDays(request.durationDays() != null ? request.durationDays() : 1);
        template.setIntervalCount(request.intervalCount() != null ? request.intervalCount() : 1);
        template.setAnchorDate(request.anchorDate());
        template.setEndDate(request.endDate());
        template.setMonth(request.recurrenceType() == HolidayRecurrenceType.YEARLY ? request.month() : null);
        template.setDayOfMonth(
            request.recurrenceType() == HolidayRecurrenceType.YEARLY || request.recurrenceType() == HolidayRecurrenceType.MONTHLY
                ? request.dayOfMonth() : null);
        template.setWeekOfMonth(request.recurrenceType() == HolidayRecurrenceType.MONTHLY ? request.weekOfMonth() : null);
        template.setDayOfWeek(
            request.recurrenceType() == HolidayRecurrenceType.WEEKLY || request.recurrenceType() == HolidayRecurrenceType.MONTHLY
                ? request.dayOfWeek() : null);
        template.setIsActive(request.isActive() != null ? request.isActive() : true);
    }

    private HolidayTemplateResponse toResponse(HolidayTemplate t) {
        return new HolidayTemplateResponse(
            t.getId(), t.getName(), t.getRecurrenceType(), t.getEventType(), t.getHolidayCategory(), t.getDescription(),
            t.getDurationDays(), t.getIntervalCount(), t.getAnchorDate(), t.getEndDate(),
            t.getMonth(), t.getDayOfMonth(), t.getWeekOfMonth(), t.getDayOfWeek(),
            t.getIsActive(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }
}
