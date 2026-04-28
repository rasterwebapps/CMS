package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeDemandDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.FeeDemand;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class FeeDemandServiceImpl implements FeeDemandService {

    private final FeeDemandRepository feeDemandRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureYearAmountRepository yearAmountRepository;
    private final TermBillingScheduleRepository billingScheduleRepository;

    public FeeDemandServiceImpl(FeeDemandRepository feeDemandRepository,
                                 TermInstanceRepository termInstanceRepository,
                                 StudentTermEnrollmentRepository enrollmentRepository,
                                 FeeStructureRepository feeStructureRepository,
                                 FeeStructureYearAmountRepository yearAmountRepository,
                                 TermBillingScheduleRepository billingScheduleRepository) {
        this.feeDemandRepository = feeDemandRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.yearAmountRepository = yearAmountRepository;
        this.billingScheduleRepository = billingScheduleRepository;
    }

    @Override
    @Transactional
    public int generateDemandsForTermInstance(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term instance not found with id: " + termInstanceId));

        if (termInstance.getStatus() != TermInstanceStatus.OPEN) {
            throw new IllegalStateException(
                "Fee demands can only be generated for OPEN term instances");
        }

        AcademicYear academicYear = termInstance.getAcademicYear();

        TermBillingSchedule billingSchedule = billingScheduleRepository
            .findByAcademicYearIdAndTermType(academicYear.getId(), termInstance.getTermType())
            .orElseThrow(() -> new IllegalStateException(
                "No billing schedule configured for " + academicYear.getName()
                + " " + termInstance.getTermType()
                + ". Please configure a billing schedule first."));

        List<StudentTermEnrollment> enrollments = enrollmentRepository
            .findByTermInstanceIdAndStatus(termInstanceId, EnrollmentStatus.ENROLLED);

        int count = 0;
        for (StudentTermEnrollment enrollment : enrollments) {
            Optional<FeeDemand> existing =
                feeDemandRepository.findByStudentTermEnrollmentId(enrollment.getId());
            if (existing.isPresent()) {
                continue;
            }

            BigDecimal totalAmount = deriveFeeTotalAmount(enrollment, academicYear);

            FeeDemand demand = new FeeDemand();
            demand.setStudentTermEnrollment(enrollment);
            demand.setTermInstance(termInstance);
            demand.setAcademicYear(academicYear);
            demand.setTotalAmount(totalAmount);
            demand.setDueDate(billingSchedule.getDueDate());
            demand.setPaidAmount(BigDecimal.ZERO);
            demand.setStatus(DemandStatus.UNPAID);
            feeDemandRepository.save(demand);
            count++;
        }
        return count;
    }

    @Override
    public List<FeeDemandDto> getDemandsByTermInstance(Long termInstanceId) {
        return feeDemandRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<FeeDemandDto> getDemandsByTermInstanceAndStatus(Long termInstanceId, DemandStatus status) {
        return feeDemandRepository.findByTermInstanceIdAndStatus(termInstanceId, status)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public FeeDemandDto getDemandByEnrollment(Long enrollmentId) {
        FeeDemand demand = feeDemandRepository.findByStudentTermEnrollmentId(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Fee demand not found for enrollment id: " + enrollmentId));
        return toDto(demand);
    }

    @Override
    public FeeDemandDto getById(Long id) {
        FeeDemand demand = feeDemandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee demand not found with id: " + id));
        return toDto(demand);
    }

    @Override
    public List<FeeDemandDto> getOutstandingDemands(Long termInstanceId) {
        return feeDemandRepository.findByTermInstanceIdAndStatusNot(termInstanceId, DemandStatus.PAID)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<FeeDemandDto> getDemandsByStudent(Long studentId) {
        return feeDemandRepository.findByStudentTermEnrollmentStudentId(studentId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    private BigDecimal deriveFeeTotalAmount(StudentTermEnrollment enrollment, AcademicYear academicYear) {
        Long programId = enrollment.getCohort().getProgram().getId();
        Integer yearOfStudy = enrollment.getYearOfStudy();

        List<FeeStructure> feeStructures = feeStructureRepository
            .findByProgramIdAndAcademicYearIdAndIsActiveTrue(programId, academicYear.getId());

        if (feeStructures.isEmpty()) {
            throw new IllegalStateException(
                "No fee plan configured for program "
                + enrollment.getCohort().getProgram().getCode()
                + " and academic year " + academicYear.getName()
                + ". Please configure a fee plan first.");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<FeeStructureYearAmount> matchingAmounts = new ArrayList<>();
        for (FeeStructure fs : feeStructures) {
            List<FeeStructureYearAmount> amounts =
                yearAmountRepository.findByFeeStructureIdAndYearNumber(fs.getId(), yearOfStudy);
            matchingAmounts.addAll(amounts);
        }

        for (FeeStructureYearAmount ya : matchingAmounts) {
            total = total.add(ya.getAmount());
        }

        if (matchingAmounts.isEmpty()) {
            throw new IllegalStateException(
                "No fee amounts configured for year of study " + yearOfStudy
                + " in program " + enrollment.getCohort().getProgram().getCode()
                + ". Please configure fee amounts for this year.");
        }

        return total;
    }

    private FeeDemandDto toDto(FeeDemand d) {
        StudentTermEnrollment enrollment = d.getStudentTermEnrollment();
        String termLabel = d.getTermInstance().getAcademicYear().getName()
            + " " + d.getTermInstance().getTermType();
        return new FeeDemandDto(
            d.getId(),
            enrollment.getId(),
            enrollment.getStudent().getId(),
            enrollment.getStudent().getFullName(),
            enrollment.getCohort().getCohortCode(),
            d.getTermInstance().getId(),
            termLabel,
            d.getAcademicYear().getId(),
            d.getAcademicYear().getName(),
            d.getTotalAmount(),
            d.getDueDate(),
            d.getPaidAmount(),
            d.getOutstandingAmount(),
            d.getStatus()
        );
    }
}
