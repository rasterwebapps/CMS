package com.cms.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.PeriodRequest;
import com.cms.dto.PeriodResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Period;
import com.cms.repository.PeriodRepository;

@Service
@Transactional(readOnly = true)
public class PeriodService {

    private final PeriodRepository periodRepository;

    public PeriodService(PeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    @Transactional
    public PeriodResponse create(PeriodRequest request) {
        String name = requireTrimmed(request.name(), "Period name is required");

        if (periodRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A period with the name '" + name + "' already exists");
        }

        LocalTime endTime = computeEndTime(request.startTime(), request.durationMinutes());
        Period period = new Period(name, request.startTime(), endTime, request.periodOrder());
        period.setDurationMinutes(request.durationMinutes());
        if (request.isActive() != null) {
            period.setIsActive(request.isActive());
        }
        return toResponse(periodRepository.save(period));
    }

    public List<PeriodResponse> findAll() {
        return periodRepository.findAllByOrderByPeriodOrderAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<PeriodResponse> findActive() {
        return periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<PeriodResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return periodRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<Period> spec = (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), pattern);
        return periodRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public PeriodResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PeriodResponse update(Long id, PeriodRequest request) {
        Period period = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Period name is required");

        if (periodRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A period with the name '" + name + "' already exists");
        }

        period.setName(name);
        period.setStartTime(request.startTime());
        period.setEndTime(computeEndTime(request.startTime(), request.durationMinutes()));
        period.setDurationMinutes(request.durationMinutes());
        period.setPeriodOrder(request.periodOrder());
        if (request.isActive() != null) {
            period.setIsActive(request.isActive());
        }
        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public void delete(Long id) {
        if (!periodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Period not found with id: " + id);
        }
        periodRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Period period = findOrThrow(id);
        period.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Period saved = periodRepository.save(period);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return periodRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return periodRepository.existsByNameIgnoreCase(trimmed);
    }

    private Period findOrThrow(Long id) {
        return periodRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + id));
    }

    /** End time is derived from start time + duration rather than entered independently, so
     *  there's a single source of truth for a period's span. A duration long enough to wrap past
     *  midnight would silently produce an end time before the start time — reject that outright
     *  rather than storing a nonsensical period. */
    private static LocalTime computeEndTime(LocalTime startTime, int durationMinutes) {
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Duration is too long — the period would cross midnight");
        }
        return endTime;
    }

    private PeriodResponse toResponse(Period p) {
        return new PeriodResponse(p.getId(), p.getName(), p.getStartTime(), p.getEndTime(),
            p.getDurationMinutes(), p.getPeriodOrder(), p.getIsActive(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
