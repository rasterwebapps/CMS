package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeePaymentRequest;
import com.cms.dto.FeePaymentResponse;
import com.cms.dto.StudentFeeStatusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeePayment;
import com.cms.model.FeeStructure;
import com.cms.model.Student;
import com.cms.repository.FeePaymentRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class FeePaymentService {

    private final FeePaymentRepository feePaymentRepository;
    private final StudentRepository studentRepository;
    private final FeeStructureRepository feeStructureRepository;

    public FeePaymentService(FeePaymentRepository feePaymentRepository,
                              StudentRepository studentRepository,
                              FeeStructureRepository feeStructureRepository) {
        this.feePaymentRepository = feePaymentRepository;
        this.studentRepository = studentRepository;
        this.feeStructureRepository = feeStructureRepository;
    }

    @Transactional
    public FeePaymentResponse create(FeePaymentRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        FeeStructure feeStructure = feeStructureRepository.findById(request.feeStructureId())
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + request.feeStructureId()));

        String receiptNumber = generateReceiptNumber();

        FeePayment feePayment = new FeePayment(
            student, feeStructure, receiptNumber, request.amountPaid(),
            request.paymentDate(), request.paymentMode(), request.status()
        );
        feePayment.setTransactionReference(request.transactionReference());
        feePayment.setRemarks(request.remarks());

        FeePayment saved = feePaymentRepository.save(feePayment);
        return toResponse(saved);
    }

    public List<FeePaymentResponse> findAll() {
        return feePaymentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public FeePaymentResponse findById(Long id) {
        FeePayment feePayment = feePaymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee payment not found with id: " + id));
        return toResponse(feePayment);
    }

    public List<FeePaymentResponse> findByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return feePaymentRepository.findByStudentId(studentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public FeePaymentResponse findByReceiptNumber(String receiptNumber) {
        FeePayment feePayment = feePaymentRepository.findByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Fee payment not found with receipt number: " + receiptNumber));
        return toResponse(feePayment);
    }

    public List<FeePaymentResponse> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return feePaymentRepository.findByPaymentDateBetween(startDate, endDate).stream()
            .map(this::toResponse)
            .toList();
    }

    public StudentFeeStatusResponse getStudentFeeStatus(Long studentId, Long academicYearId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<FeeStructure> feeStructures = feeStructureRepository
            .findByProgramIdAndAcademicYearIdAndIsActiveTrue(student.getProgram().getId(), academicYearId);

        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        List<StudentFeeStatusResponse.FeeItemStatus> feeItems = new ArrayList<>();

        for (FeeStructure fs : feeStructures) {
            BigDecimal paid = feePaymentRepository.sumAmountPaidByStudentIdAndFeeStructureId(studentId, fs.getId());
            if (paid == null) {
                paid = BigDecimal.ZERO;
            }
            BigDecimal pending = fs.getAmount().subtract(paid);
            String status = pending.compareTo(BigDecimal.ZERO) <= 0 ? "PAID" : 
                           (paid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "PENDING");

            feeItems.add(new StudentFeeStatusResponse.FeeItemStatus(
                fs.getId(),
                fs.getFeeType().name(),
                fs.getAmount(),
                paid,
                pending.max(BigDecimal.ZERO),
                status
            ));

            totalFees = totalFees.add(fs.getAmount());
            totalPaid = totalPaid.add(paid);
        }

        BigDecimal pendingAmount = totalFees.subtract(totalPaid).max(BigDecimal.ZERO);

        return new StudentFeeStatusResponse(
            student.getId(),
            student.getFullName(),
            student.getRollNumber(),
            totalFees,
            totalPaid,
            pendingAmount,
            feeItems
        );
    }

    @Transactional
    public FeePaymentResponse update(Long id, FeePaymentRequest request) {
        FeePayment feePayment = feePaymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee payment not found with id: " + id));

        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        FeeStructure feeStructure = feeStructureRepository.findById(request.feeStructureId())
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + request.feeStructureId()));

        feePayment.setStudent(student);
        feePayment.setFeeStructure(feeStructure);
        feePayment.setAmountPaid(request.amountPaid());
        feePayment.setPaymentDate(request.paymentDate());
        feePayment.setPaymentMode(request.paymentMode());
        feePayment.setStatus(request.status());
        feePayment.setTransactionReference(request.transactionReference());
        feePayment.setRemarks(request.remarks());

        FeePayment updated = feePaymentRepository.save(feePayment);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!feePaymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fee payment not found with id: " + id);
        }
        feePaymentRepository.deleteById(id);
    }

    private String generateReceiptNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RCP-" + date + "-" + uuid;
    }

    private FeePaymentResponse toResponse(FeePayment fp) {
        return new FeePaymentResponse(
            fp.getId(),
            fp.getStudent().getId(),
            fp.getStudent().getFullName(),
            fp.getStudent().getRollNumber(),
            fp.getFeeStructure().getId(),
            fp.getFeeStructure().getFeeType(),
            fp.getFeeStructure().getAmount(),
            fp.getReceiptNumber(),
            fp.getAmountPaid(),
            fp.getPaymentDate(),
            fp.getPaymentMode(),
            fp.getStatus(),
            fp.getTransactionReference(),
            fp.getRemarks(),
            fp.getCreatedAt(),
            fp.getUpdatedAt()
        );
    }
}
