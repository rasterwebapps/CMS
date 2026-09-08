package com.cms.inventory.asset.service;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.asset.dto.AssetMaintenanceMarkPerformedRequest;
import com.cms.inventory.asset.dto.AssetMaintenanceScheduleRequest;
import com.cms.inventory.asset.dto.AssetMaintenanceScheduleResponse;
import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.model.AssetMaintenanceSchedule;
import com.cms.inventory.asset.model.enums.MaintenanceScheduleType;
import com.cms.inventory.asset.repository.AssetMaintenanceScheduleRepository;
import com.cms.inventory.asset.repository.AssetRepository;

/**
 * Owns Asset Maintenance Schedules — Phase 5's second slice, first half. {@link #markPerformed}
 * is the one meaningful transition: it records {@code lastPerformedDate} and, for a {@code
 * RECURRING} schedule, advances {@code nextDueDate} by {@code recurrenceIntervalDays}; a {@code
 * ONE_OFF} schedule is simply deactivated once performed (nothing to recur to). "Overdue" is
 * computed at read time from {@code nextDueDate} vs. today, never stored — same pattern {@code
 * LoanableItemIssue} already established. See the "Maintenance & Service Contracts slice"
 * decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class AssetMaintenanceScheduleService {

    private final AssetMaintenanceScheduleRepository scheduleRepository;
    private final AssetRepository assetRepository;

    public AssetMaintenanceScheduleService(AssetMaintenanceScheduleRepository scheduleRepository, AssetRepository assetRepository) {
        this.scheduleRepository = scheduleRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional
    public AssetMaintenanceScheduleResponse create(AssetMaintenanceScheduleRequest request) {
        AssetMaintenanceSchedule schedule = new AssetMaintenanceSchedule();
        applyRequest(schedule, request);
        return toResponse(scheduleRepository.save(schedule));
    }

    public Page<AssetMaintenanceScheduleResponse> findPage(Long assetId, Boolean overdueOnly, Boolean activeOnly, Pageable pageable) {
        Specification<AssetMaintenanceSchedule> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (assetId != null) predicate = cb.and(predicate, cb.equal(root.get("asset").get("id"), assetId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            if (Boolean.TRUE.equals(overdueOnly)) {
                predicate = cb.and(predicate, cb.isTrue(root.get("isActive")), cb.lessThan(root.get("nextDueDate"), LocalDate.now()));
            }
            return predicate;
        };
        return scheduleRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public AssetMaintenanceScheduleResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public AssetMaintenanceScheduleResponse update(Long id, AssetMaintenanceScheduleRequest request) {
        AssetMaintenanceSchedule schedule = findOrThrow(id);
        applyRequest(schedule, request);
        return toResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public AssetMaintenanceScheduleResponse markPerformed(Long id, AssetMaintenanceMarkPerformedRequest request) {
        AssetMaintenanceSchedule schedule = findOrThrow(id);
        LocalDate performedDate = (request != null && request.performedDate() != null) ? request.performedDate() : LocalDate.now();
        schedule.setLastPerformedDate(performedDate);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            schedule.setNotes(schedule.getNotes() != null ? schedule.getNotes() + " | " + request.notes().trim() : request.notes().trim());
        }
        if (schedule.getScheduleType() == MaintenanceScheduleType.RECURRING && schedule.getRecurrenceIntervalDays() != null) {
            schedule.setNextDueDate(performedDate.plusDays(schedule.getRecurrenceIntervalDays()));
        } else {
            schedule.setIsActive(false);
        }
        return toResponse(scheduleRepository.save(schedule));
    }

    private void applyRequest(AssetMaintenanceSchedule schedule, AssetMaintenanceScheduleRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + request.assetId()));
        MaintenanceScheduleType type = parseType(request.scheduleType());
        if (type == MaintenanceScheduleType.RECURRING && request.recurrenceIntervalDays() == null) {
            throw new IllegalArgumentException("A recurring schedule needs a recurrence interval (in days)");
        }

        schedule.setAsset(asset);
        schedule.setScheduleType(type);
        schedule.setRecurrenceIntervalDays(type == MaintenanceScheduleType.RECURRING ? request.recurrenceIntervalDays() : null);
        schedule.setNextDueDate(request.nextDueDate());
        schedule.setNotes(trim(request.notes()));
    }

    private AssetMaintenanceSchedule findOrThrow(Long id) {
        return scheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset maintenance schedule not found with id: " + id));
    }

    private MaintenanceScheduleType parseType(String value) {
        try {
            return MaintenanceScheduleType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid schedule type '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private AssetMaintenanceScheduleResponse toResponse(AssetMaintenanceSchedule schedule) {
        Asset asset = schedule.getAsset();
        boolean overdue = Boolean.TRUE.equals(schedule.getIsActive()) && schedule.getNextDueDate().isBefore(LocalDate.now());
        return new AssetMaintenanceScheduleResponse(
            schedule.getId(), asset.getId(), asset.getAssetTag(), asset.getProduct().getProductName(),
            schedule.getScheduleType().name(), schedule.getRecurrenceIntervalDays(),
            schedule.getNextDueDate(), schedule.getLastPerformedDate(), overdue, schedule.getIsActive(),
            schedule.getNotes(), schedule.getCreatedAt(), schedule.getUpdatedAt());
    }
}
