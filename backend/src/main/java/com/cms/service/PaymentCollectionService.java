package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.SemesterPaymentDetail;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeeInstallment;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class PaymentCollectionService {

    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final StudentRepository studentRepository;

    public PaymentCollectionService(StudentFeeAllocationRepository allocationRepository,
                                     SemesterFeeRepository semesterFeeRepository,
                                     FeeInstallmentRepository installmentRepository,
                                     StudentRepository studentRepository) {
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public CollectPaymentResponse collectPayment(Long studentId, CollectPaymentRequest request) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        StudentFeeAllocation allocation = allocationRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Fee allocation not found for student: " + student.getRollNumber()));

        if (allocation.getStatus() != FeeAllocationStatus.FINALIZED) {
            throw new IllegalStateException(
                "Fee allocation is not finalized for student: " + student.getRollNumber());
        }

        List<SemesterFee> semesterFees = semesterFeeRepository
            .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());

        BigDecimal remaining = request.amount();
        List<String> allocationDetails = new ArrayList<>();
        List<SemesterPaymentDetail> semesterBreakdown = new ArrayList<>();
        // One receipt number for the entire payment regardless of how many semesters it covers
        String receiptNumber = generateReceiptNumber();

        for (SemesterFee sf : semesterFees) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal pendingForSemester = sf.getAmount().subtract(alreadyPaid).max(BigDecimal.ZERO);

            if (pendingForSemester.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal payForThisSemester = remaining.min(pendingForSemester);

            FeeInstallment installment = new FeeInstallment(
                sf, student, payForThisSemester,
                request.paymentDate(), request.paymentMode(), receiptNumber
            );
            installment.setTransactionReference(request.transactionReference());
            installment.setRemarks(request.remarks());
            installmentRepository.save(installment);

            allocationDetails.add(sf.getSemesterLabel() + ": ₹" + payForThisSemester.toPlainString());
            semesterBreakdown.add(new SemesterPaymentDetail(
                sf.getSemesterLabel(), sf.getYearNumber(), sf.getSemesterSequence(), payForThisSemester
            ));
            remaining = remaining.subtract(payForThisSemester);
        }

        if (allocationDetails.isEmpty()) {
            throw new IllegalStateException("No pending fees found for student: " + student.getRollNumber());
        }

        return new CollectPaymentResponse(
            receiptNumber, student.getId(), student.getFullName(), student.getRollNumber(),
            request.amount().subtract(remaining), request.paymentDate(), request.paymentMode(),
            request.transactionReference(), request.remarks(),
            String.join("; ", allocationDetails),
            semesterBreakdown,
            java.time.Instant.now()
        );
    }

    public List<ReceiptResponse> getReceipts(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return installmentRepository.findByStudentIdOrderByPaymentDateDesc(studentId).stream()
            .map(this::toReceiptResponse)
            .toList();
    }

    public ReceiptResponse getReceiptById(Long studentId, Long installmentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        FeeInstallment installment = installmentRepository.findById(installmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + installmentId));

        if (!installment.getStudent().getId().equals(studentId)) {
            throw new ResourceNotFoundException("Receipt not found for student with id: " + studentId);
        }

        return toReceiptResponse(installment);
    }

    private ReceiptResponse toReceiptResponse(FeeInstallment fi) {
        return new ReceiptResponse(
            fi.getId(), fi.getReceiptNumber(),
            fi.getStudent().getId(), fi.getStudent().getFullName(), fi.getStudent().getRollNumber(),
            fi.getSemesterFee().getId(), fi.getSemesterFee().getSemesterLabel(),
            fi.getSemesterFee().getYearNumber(),
            fi.getAmountPaid(), fi.getPaymentDate(), fi.getPaymentMode(),
            fi.getTransactionReference(), fi.getRemarks(), fi.getCreatedAt()
        );
    }

    private String generateReceiptNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RCP-" + date + "-" + uuid;
    }
}
