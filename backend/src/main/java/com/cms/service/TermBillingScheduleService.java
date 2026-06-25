package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.TermBillingScheduleDto;
import com.cms.dto.TermBillingScheduleRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.SystemConfigurationRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class TermBillingScheduleService {

    /** Falls back to this many days if the config row is missing rather than hard-failing fee billing. */
    private static final int DEFAULT_ADVANCE_DAYS = 30;
    private static final String ADVANCE_DAYS_CONFIG_KEY = "fee.collection_advance_days";

    private final TermBillingScheduleRepository scheduleRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;

    public TermBillingScheduleService(TermBillingScheduleRepository scheduleRepository,
                                       AcademicYearRepository academicYearRepository,
                                       TermInstanceRepository termInstanceRepository,
                                       SystemConfigurationRepository systemConfigurationRepository) {
        this.scheduleRepository = scheduleRepository;
        this.academicYearRepository = academicYearRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
    }

    @Transactional
    public TermBillingScheduleDto createOrUpdate(TermBillingScheduleRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));
        validateDueDate(request.academicYearId(), request.termType(), request.dueDate());

        TermBillingSchedule schedule = scheduleRepository
            .findByAcademicYearIdAndTermType(request.academicYearId(), request.termType())
            .orElseGet(TermBillingSchedule::new);

        schedule.setAcademicYear(academicYear);
        schedule.setTermType(request.termType());
        schedule.setDueDate(request.dueDate());
        schedule.setLateFeeType(request.lateFeeType());
        schedule.setLateFeeAmount(request.lateFeeAmount());
        schedule.setGraceDays(request.graceDays() != null ? request.graceDays() : 0);

        return toDto(scheduleRepository.save(schedule));
    }

    @Transactional
    public TermBillingScheduleDto update(Long id, TermBillingScheduleRequest request) {
        TermBillingSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term billing schedule not found with id: " + id));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));
        validateDueDate(request.academicYearId(), request.termType(), request.dueDate());

        schedule.setAcademicYear(academicYear);
        schedule.setTermType(request.termType());
        schedule.setDueDate(request.dueDate());
        schedule.setLateFeeType(request.lateFeeType());
        schedule.setLateFeeAmount(request.lateFeeAmount());
        schedule.setGraceDays(request.graceDays() != null ? request.graceDays() : 0);

        return toDto(scheduleRepository.save(schedule));
    }

    public TermBillingScheduleDto getTermBillingSchedule(Long academicYearId, TermType termType) {
        return scheduleRepository.findByAcademicYearIdAndTermType(academicYearId, termType)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No billing schedule found for academic year " + academicYearId + " and term type " + termType));
    }

    public List<TermBillingScheduleDto> getAllForAcademicYear(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return scheduleRepository.findByAcademicYearId(academicYearId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public TermBillingScheduleDto getById(Long id) {
        TermBillingSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term billing schedule not found with id: " + id));
        return toDto(schedule);
    }

    public BigDecimal computeLateFee(Long academicYearId, TermType termType, LocalDate paymentDate) {
        TermBillingSchedule schedule = scheduleRepository
            .findByAcademicYearIdAndTermType(academicYearId, termType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No billing schedule found for academic year " + academicYearId + " and term type " + termType));

        LocalDate effectiveDueDate = schedule.getDueDate().plusDays(schedule.getGraceDays());
        if (!paymentDate.isAfter(effectiveDueDate)) {
            return BigDecimal.ZERO;
        }

        if (schedule.getLateFeeType() == LateFeeType.FLAT) {
            return schedule.getLateFeeAmount();
        } else {
            long daysLate = ChronoUnit.DAYS.between(effectiveDueDate, paymentDate);
            return schedule.getLateFeeAmount().multiply(BigDecimal.valueOf(daysLate));
        }
    }

    /**
     * Due date must not be later than the term's own end date, but is allowed to fall before the
     * term even starts — up to a configurable number of days — so collection can run ahead of the
     * term opening rather than being confined strictly inside it.
     */
    private void validateDueDate(Long academicYearId, TermType termType, LocalDate dueDate) {
        TermInstance term = termInstanceRepository.findByAcademicYearIdAndTermType(academicYearId, termType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No " + termType + " term instance configured for academic year " + academicYearId));

        LocalDate earliestAllowed = term.getStartDate().minusDays(resolveAdvanceDays());
        if (dueDate.isBefore(earliestAllowed) || dueDate.isAfter(term.getEndDate())) {
            throw new IllegalArgumentException(
                "Due date must be between " + earliestAllowed + " and " + term.getEndDate()
                    + " for the " + termType + " term");
        }
    }

    /** Negative values would push the earliest-allowed due date past the term start; clamp to 0. */
    private int resolveAdvanceDays() {
        return systemConfigurationRepository.findByConfigKey(ADVANCE_DAYS_CONFIG_KEY)
            .map(config -> {
                try {
                    return Math.max(0, Integer.parseInt(config.getConfigValue()));
                } catch (NumberFormatException e) {
                    return DEFAULT_ADVANCE_DAYS;
                }
            })
            .orElse(DEFAULT_ADVANCE_DAYS);
    }

    private TermBillingScheduleDto toDto(TermBillingSchedule s) {
        return new TermBillingScheduleDto(
            s.getId(),
            s.getAcademicYear().getId(),
            s.getAcademicYear().getName(),
            s.getTermType(),
            s.getDueDate(),
            s.getLateFeeType(),
            s.getLateFeeAmount(),
            s.getGraceDays(),
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }
}
