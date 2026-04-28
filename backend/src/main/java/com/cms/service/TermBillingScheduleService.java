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
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.TermBillingScheduleRepository;

@Service
@Transactional(readOnly = true)
public class TermBillingScheduleService {

    private final TermBillingScheduleRepository scheduleRepository;
    private final AcademicYearRepository academicYearRepository;

    public TermBillingScheduleService(TermBillingScheduleRepository scheduleRepository,
                                       AcademicYearRepository academicYearRepository) {
        this.scheduleRepository = scheduleRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public TermBillingScheduleDto createOrUpdate(TermBillingScheduleRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));

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
