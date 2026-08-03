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

    /** Mirrors the DB CHECK constraint (chk_holiday_template_shape) at the app layer for a
     *  friendlier error message: YEARLY needs month+dayOfMonth (not week/day-of-week); MONTHLY
     *  needs weekOfMonth+dayOfWeek (not month/dayOfMonth). */
    private void validateShape(HolidayTemplateRequest request) {
        int durationDays = request.durationDays() != null ? request.durationDays() : 1;
        if (durationDays <= 0) {
            throw new IllegalArgumentException("Duration (days) must be greater than zero");
        }
        if (request.recurrenceType() == HolidayRecurrenceType.YEARLY) {
            if (request.month() == null || request.month() < 1 || request.month() > 12) {
                throw new IllegalArgumentException("A valid month (1-12) is required for a yearly holiday");
            }
            if (request.dayOfMonth() == null || request.dayOfMonth() < 1 || request.dayOfMonth() > 31) {
                throw new IllegalArgumentException("A valid day of month (1-31) is required for a yearly holiday");
            }
            if (request.weekOfMonth() != null || request.dayOfWeek() != null) {
                throw new IllegalArgumentException("Week-of-month/day-of-week only apply to a monthly holiday");
            }
        } else {
            if (request.weekOfMonth() == null) {
                throw new IllegalArgumentException("Week of month is required for a monthly holiday");
            }
            if (request.dayOfWeek() == null) {
                throw new IllegalArgumentException("Day of week is required for a monthly holiday");
            }
            if (request.month() != null || request.dayOfMonth() != null) {
                throw new IllegalArgumentException("Month/day-of-month only apply to a yearly holiday");
            }
        }
    }

    private void applyRequest(HolidayTemplate template, HolidayTemplateRequest request, String name) {
        template.setName(name);
        template.setRecurrenceType(request.recurrenceType());
        template.setHolidayCategory(request.holidayCategory());
        template.setDescription(trim(request.description()));
        template.setDurationDays(request.durationDays() != null ? request.durationDays() : 1);
        if (request.recurrenceType() == HolidayRecurrenceType.YEARLY) {
            template.setMonth(request.month());
            template.setDayOfMonth(request.dayOfMonth());
            template.setWeekOfMonth(null);
            template.setDayOfWeek(null);
        } else {
            template.setMonth(null);
            template.setDayOfMonth(null);
            template.setWeekOfMonth(request.weekOfMonth());
            template.setDayOfWeek(request.dayOfWeek());
        }
        template.setIsActive(request.isActive() != null ? request.isActive() : true);
    }

    private HolidayTemplateResponse toResponse(HolidayTemplate t) {
        return new HolidayTemplateResponse(
            t.getId(), t.getName(), t.getRecurrenceType(), t.getHolidayCategory(), t.getDescription(),
            t.getDurationDays(), t.getMonth(), t.getDayOfMonth(), t.getWeekOfMonth(), t.getDayOfWeek(),
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
