package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeDemandDto;
import com.cms.dto.FeeCollectionSummaryDto;
import com.cms.dto.StudentFeeLedgerDto;
import com.cms.dto.TermFeePaymentDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeeDemand;
import com.cms.model.Student;
import com.cms.model.TermFeePayment;
import com.cms.model.enums.DemandStatus;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermFeePaymentRepository;

@Service
@Transactional(readOnly = true)
public class FeeReportService {

    private final FeeDemandRepository feeDemandRepository;
    private final TermFeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final FeeDemandService feeDemandService;

    public FeeReportService(FeeDemandRepository feeDemandRepository,
                             TermFeePaymentRepository paymentRepository,
                             StudentRepository studentRepository,
                             FeeDemandService feeDemandService) {
        this.feeDemandRepository = feeDemandRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.feeDemandService = feeDemandService;
    }

    public List<FeeDemandDto> getOutstandingDemands(Long termInstanceId) {
        return feeDemandService.getOutstandingDemands(termInstanceId);
    }

    public List<FeeCollectionSummaryDto> getCollectionSummary(Long termInstanceId) {
        List<FeeDemand> demands = feeDemandRepository.findByTermInstanceId(termInstanceId);

        // Group by program
        Map<Long, ProgramAccumulator> accMap = new LinkedHashMap<>();
        for (FeeDemand demand : demands) {
            Long programId = demand.getStudentTermEnrollment().getCohort().getProgram().getId();
            ProgramAccumulator acc = accMap.computeIfAbsent(programId, id -> new ProgramAccumulator(
                demand.getStudentTermEnrollment().getCohort().getProgram().getName(),
                demand.getStudentTermEnrollment().getCohort().getProgram().getCode()
            ));
            acc.add(demand);
        }

        return accMap.values().stream().map(ProgramAccumulator::toDto).toList();
    }

    public List<TermFeePaymentDto> getLateFeeCollection(Long termInstanceId) {
        List<FeeDemand> demands = feeDemandRepository.findByTermInstanceId(termInstanceId);
        List<TermFeePaymentDto> result = new ArrayList<>();
        for (FeeDemand demand : demands) {
            List<TermFeePayment> payments = paymentRepository.findByFeeDemandId(demand.getId());
            for (TermFeePayment p : payments) {
                if (p.getLateFeeApplied() != null
                        && p.getLateFeeApplied().compareTo(BigDecimal.ZERO) > 0) {
                    result.add(toPaymentDto(p));
                }
            }
        }
        return result;
    }

    public StudentFeeLedgerDto getStudentLedger(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<FeeDemand> demands = feeDemandRepository.findByStudentTermEnrollmentStudentId(studentId);
        List<StudentFeeLedgerDto.LedgerEntry> entries = new ArrayList<>();
        for (FeeDemand demand : demands) {
            String termLabel = demand.getTermInstance().getAcademicYear().getName()
                + " " + demand.getTermInstance().getTermType();
            List<TermFeePayment> payments = paymentRepository.findByFeeDemandId(demand.getId());
            List<TermFeePaymentDto> paymentDtos = payments.stream().map(this::toPaymentDto).toList();
            entries.add(new StudentFeeLedgerDto.LedgerEntry(
                demand.getId(),
                termLabel,
                demand.getTotalAmount(),
                demand.getPaidAmount(),
                demand.getOutstandingAmount(),
                demand.getDueDate(),
                demand.getStatus(),
                paymentDtos
            ));
        }
        return new StudentFeeLedgerDto(student.getId(), student.getFullName(), entries);
    }

    private TermFeePaymentDto toPaymentDto(TermFeePayment p) {
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

    /** Internal accumulator for summary grouping. */
    private static class ProgramAccumulator {
        final String programName;
        final String programCode;
        long totalDemands = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal collectedAmount = BigDecimal.ZERO;
        long paidCount = 0;
        long partialCount = 0;
        long unpaidCount = 0;

        ProgramAccumulator(String programName, String programCode) {
            this.programName = programName;
            this.programCode = programCode;
        }

        void add(FeeDemand demand) {
            totalDemands++;
            totalAmount = totalAmount.add(demand.getTotalAmount());
            collectedAmount = collectedAmount.add(demand.getPaidAmount());
            if (demand.getStatus() == DemandStatus.PAID) {
                paidCount++;
            } else if (demand.getStatus() == DemandStatus.PARTIAL) {
                partialCount++;
            } else if (demand.getStatus() == DemandStatus.UNPAID) {
                unpaidCount++;
            }
        }

        FeeCollectionSummaryDto toDto() {
            BigDecimal outstanding = totalAmount.subtract(collectedAmount).max(BigDecimal.ZERO);
            return new FeeCollectionSummaryDto(
                programName, programCode, totalDemands,
                totalAmount, collectedAmount, outstanding,
                paidCount, partialCount, unpaidCount
            );
        }
    }
}
