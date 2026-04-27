package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.TermFeePaymentDto;
import com.cms.dto.TermFeePaymentRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeeDemand;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermFeePayment;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.LateFeeType;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermFeePaymentRepository;

@Service
@Transactional(readOnly = true)
public class TermFeePaymentServiceImpl implements TermFeePaymentService {

    private final TermFeePaymentRepository paymentRepository;
    private final FeeDemandRepository feeDemandRepository;
    private final TermBillingScheduleRepository billingScheduleRepository;

    public TermFeePaymentServiceImpl(TermFeePaymentRepository paymentRepository,
                                      FeeDemandRepository feeDemandRepository,
                                      TermBillingScheduleRepository billingScheduleRepository) {
        this.paymentRepository = paymentRepository;
        this.feeDemandRepository = feeDemandRepository;
        this.billingScheduleRepository = billingScheduleRepository;
    }

    @Override
    @Transactional
    public TermFeePaymentDto recordPayment(TermFeePaymentRequest request) {
        FeeDemand demand = feeDemandRepository.findById(request.feeDemandId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Fee demand not found with id: " + request.feeDemandId()));

        if (demand.getStatus() == DemandStatus.PAID) {
            throw new IllegalStateException(
                "Fee demand " + demand.getId() + " is already fully paid");
        }

        TermBillingSchedule billingSchedule = billingScheduleRepository
            .findByAcademicYearIdAndTermType(
                demand.getAcademicYear().getId(),
                demand.getTermInstance().getTermType())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No billing schedule configured for "
                + demand.getAcademicYear().getName()
                + " " + demand.getTermInstance().getTermType()));

        BigDecimal lateFee = computeLateFee(billingSchedule, demand.getDueDate(), request.paymentDate());

        String receiptNumber = generateReceiptNumber(request.paymentDate());

        TermFeePayment payment = new TermFeePayment();
        payment.setFeeDemand(demand);
        payment.setPaymentDate(request.paymentDate());
        payment.setAmountPaid(request.amountPaid());
        payment.setLateFeeApplied(lateFee);
        payment.setPaymentMode(request.paymentMode());
        payment.setReceiptNumber(receiptNumber);
        payment.setRemarks(request.remarks());
        paymentRepository.save(payment);

        // Update demand
        BigDecimal newPaid = demand.getPaidAmount().add(request.amountPaid());
        demand.setPaidAmount(newPaid);
        if (newPaid.compareTo(demand.getTotalAmount()) >= 0) {
            demand.setStatus(DemandStatus.PAID);
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            demand.setStatus(DemandStatus.PARTIAL);
        } else {
            demand.setStatus(DemandStatus.UNPAID);
        }
        feeDemandRepository.save(demand);

        return toDto(payment);
    }

    @Override
    public List<TermFeePaymentDto> getPaymentsByDemand(Long demandId) {
        if (!feeDemandRepository.existsById(demandId)) {
            throw new ResourceNotFoundException("Fee demand not found with id: " + demandId);
        }
        return paymentRepository.findByFeeDemandId(demandId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public TermFeePaymentDto getById(Long id) {
        TermFeePayment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term fee payment not found with id: " + id));
        return toDto(payment);
    }

    @Override
    public TermFeePaymentDto getPaymentByReceipt(String receiptNumber) {
        TermFeePayment payment = paymentRepository.findByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term fee payment not found with receipt number: " + receiptNumber));
        return toDto(payment);
    }

    @Override
    public List<TermFeePaymentDto> getPaymentsByDateRange(LocalDate from, LocalDate to) {
        return paymentRepository.findByPaymentDateBetween(from, to)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<TermFeePaymentDto> getPaymentsWithLateFeeByTermInstance(Long termInstanceId) {
        // Load all demands for the term, then filter payments with late fee > 0
        return feeDemandRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .flatMap(demand -> paymentRepository.findByFeeDemandId(demand.getId()).stream())
            .filter(p -> p.getLateFeeApplied() != null
                && p.getLateFeeApplied().compareTo(BigDecimal.ZERO) > 0)
            .map(this::toDto)
            .toList();
    }

    private BigDecimal computeLateFee(TermBillingSchedule schedule,
                                       LocalDate dueDate,
                                       LocalDate paymentDate) {
        int graceDays = schedule.getGraceDays() != null ? schedule.getGraceDays() : 0;
        LocalDate effectiveDue = dueDate.plusDays(graceDays);

        if (!paymentDate.isAfter(effectiveDue)) {
            return BigDecimal.ZERO;
        }

        if (schedule.getLateFeeType() == LateFeeType.FLAT) {
            return schedule.getLateFeeAmount();
        } else {
            long daysLate = ChronoUnit.DAYS.between(effectiveDue, paymentDate);
            return schedule.getLateFeeAmount().multiply(BigDecimal.valueOf(daysLate));
        }
    }

    private String generateReceiptNumber(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = paymentRepository.countByPaymentDate(date);
        String seq = String.format("%04d", count + 1);
        return "RCP-" + dateStr + "-" + seq;
    }

    private TermFeePaymentDto toDto(TermFeePayment p) {
        FeeDemand demand = p.getFeeDemand();
        String studentName = demand.getStudentTermEnrollment().getStudent().getFullName();
        return new TermFeePaymentDto(
            p.getId(),
            demand.getId(),
            studentName,
            p.getPaymentDate(),
            p.getAmountPaid(),
            p.getLateFeeApplied(),
            p.getTotalCollected(),
            p.getPaymentMode(),
            p.getReceiptNumber(),
            p.getRemarks(),
            demand.getStatus(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
