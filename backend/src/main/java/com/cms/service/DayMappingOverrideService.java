package com.cms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DayMappingOverrideRequest;
import com.cms.dto.DayMappingOverrideResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.DayMappingOverride;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.TermInstanceRepository;

/** CRUD plus the shared read-time resolver every downstream consumer ({@link
 *  ClassScheduleOccurrenceService}, {@link FacultyAbsenceService}, {@link ResourceGridService},
 *  {@link AttendanceService}) should call rather than re-deriving mapping logic independently.
 *  Has zero dependency on {@code ClassSchedule} by design, so all four of those services can
 *  depend on it (or on {@link DayMappingOverrideRepository} directly) without any circularity. */
@Service
@Transactional(readOnly = true)
public class DayMappingOverrideService {

    private final DayMappingOverrideRepository dayMappingOverrideRepository;
    private final TermInstanceRepository termInstanceRepository;

    public DayMappingOverrideService(DayMappingOverrideRepository dayMappingOverrideRepository,
                                      TermInstanceRepository termInstanceRepository) {
        this.dayMappingOverrideRepository = dayMappingOverrideRepository;
        this.termInstanceRepository = termInstanceRepository;
    }

    @Transactional
    public DayMappingOverrideResponse create(DayMappingOverrideRequest request) {
        TermInstance termInstance = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));
        validate(request, termInstance);
        if (dayMappingOverrideRepository.findByMappedDate(request.mappedDate()).isPresent()) {
            throw new IllegalStateException("A day mapping already exists for " + request.mappedDate());
        }

        DayMappingOverride mapping = new DayMappingOverride();
        applyRequest(mapping, request, termInstance);

        return toResponse(dayMappingOverrideRepository.save(mapping));
    }

    public List<DayMappingOverrideResponse> findAll() {
        return dayMappingOverrideRepository.findAllByOrderByMappedDateAsc()
            .stream().map(this::toResponse).toList();
    }

    public DayMappingOverrideResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public DayMappingOverrideResponse update(Long id, DayMappingOverrideRequest request) {
        DayMappingOverride mapping = findOrThrow(id);
        TermInstance termInstance = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));
        validate(request, termInstance);
        dayMappingOverrideRepository.findByMappedDate(request.mappedDate())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalStateException("A day mapping already exists for " + request.mappedDate());
            });

        applyRequest(mapping, request, termInstance);

        return toResponse(dayMappingOverrideRepository.save(mapping));
    }

    @Transactional
    public void delete(Long id) {
        if (!dayMappingOverrideRepository.existsById(id)) {
            throw new ResourceNotFoundException("Day mapping override not found with id: " + id);
        }
        dayMappingOverrideRepository.deleteById(id);
    }

    /** The resolver every downstream consumer should call: the mapping's borrowed weekday if one
     *  exists for this date, else the date's own actual weekday (empty for Sunday, since {@link
     *  DayOfWeek} has no SUNDAY constant and no ClassSchedule can ever run on one). */
    public Optional<DayOfWeek> resolveEffectiveDayOfWeek(LocalDate date) {
        return dayMappingOverrideRepository.findByMappedDate(date)
            .map(DayMappingOverride::getBorrowedDayOfWeek)
            .or(() -> actualDayOfWeek(date));
    }

    /** Batched lookup for a whole [from, to] window, keyed by mapped date -- one query per
     *  calendar/occurrence-resolution window instead of one per schedule/date. */
    public Map<LocalDate, DayMappingOverride> findMappingsInRange(LocalDate from, LocalDate to) {
        return dayMappingOverrideRepository.findByMappedDateBetween(from, to).stream()
            .collect(java.util.stream.Collectors.toMap(DayMappingOverride::getMappedDate, Function.identity()));
    }

    private static Optional<DayOfWeek> actualDayOfWeek(LocalDate date) {
        if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return Optional.empty();
        }
        return Optional.of(DayOfWeek.valueOf(date.getDayOfWeek().name()));
    }

    /** Mirrors the shape/business invariants that would otherwise only surface as a raw
     *  constraint-violation error. */
    private void validate(DayMappingOverrideRequest request, TermInstance termInstance) {
        LocalDate mappedDate = request.mappedDate();
        if (mappedDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("A day mapping cannot be created for a Sunday");
        }
        if (mappedDate.isBefore(termInstance.getStartDate()) || mappedDate.isAfter(termInstance.getEndDate())) {
            throw new IllegalArgumentException("Mapped date must fall within the selected term's bounds");
        }
        DayOfWeek actualDayOfWeek = DayOfWeek.valueOf(mappedDate.getDayOfWeek().name());
        if (request.borrowedDayOfWeek() == actualDayOfWeek) {
            throw new IllegalArgumentException("Borrowed day of week must differ from the mapped date's own weekday");
        }
    }

    private void applyRequest(DayMappingOverride mapping, DayMappingOverrideRequest request, TermInstance termInstance) {
        mapping.setTermInstance(termInstance);
        mapping.setMappedDate(request.mappedDate());
        mapping.setBorrowedDayOfWeek(request.borrowedDayOfWeek());
        mapping.setReason(request.reason());
    }

    private DayMappingOverride findOrThrow(Long id) {
        return dayMappingOverrideRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Day mapping override not found with id: " + id));
    }

    private DayMappingOverrideResponse toResponse(DayMappingOverride m) {
        return new DayMappingOverrideResponse(
            m.getId(), m.getTermInstance().getId(), m.getMappedDate(), m.getBorrowedDayOfWeek(),
            m.getReason(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
