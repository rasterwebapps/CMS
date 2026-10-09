package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeDemandDto;
import com.cms.dto.TermFeeOverrideInput;
import com.cms.dto.TermFeeRow;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Admission;
import com.cms.model.Cohort;
import com.cms.model.Enquiry;
import com.cms.model.FeeDemand;
import com.cms.model.FeeState;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureGroup;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.StudentTermFeeOverride;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeStateRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.StudentTermFeeOverrideRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class FeeDemandServiceImpl implements FeeDemandService {

    private final FeeDemandRepository feeDemandRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final FeeStructureGroupRepository feeStructureGroupRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureYearAmountRepository yearAmountRepository;
    private final TermBillingScheduleRepository billingScheduleRepository;
    private final AdmissionRepository admissionRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final StudentTermFeeOverrideRepository termFeeOverrideRepository;
    private final CurrentUserResolver currentUserResolver;
    private final EnquiryRepository enquiryRepository;
    private final FeeStateRepository feeStateRepository;

    public FeeDemandServiceImpl(FeeDemandRepository feeDemandRepository,
                                 TermInstanceRepository termInstanceRepository,
                                 StudentTermEnrollmentRepository enrollmentRepository,
                                 FeeStructureGroupRepository feeStructureGroupRepository,
                                 FeeStructureRepository feeStructureRepository,
                                 FeeStructureYearAmountRepository yearAmountRepository,
                                 TermBillingScheduleRepository billingScheduleRepository,
                                 AdmissionRepository admissionRepository,
                                 EnquiryPaymentRepository enquiryPaymentRepository,
                                 StudentTermFeeOverrideRepository termFeeOverrideRepository,
                                 CurrentUserResolver currentUserResolver,
                                 EnquiryRepository enquiryRepository,
                                 FeeStateRepository feeStateRepository) {
        this.feeDemandRepository = feeDemandRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.feeStructureGroupRepository = feeStructureGroupRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.yearAmountRepository = yearAmountRepository;
        this.billingScheduleRepository = billingScheduleRepository;
        this.admissionRepository = admissionRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.termFeeOverrideRepository = termFeeOverrideRepository;
        this.currentUserResolver = currentUserResolver;
        this.enquiryRepository = enquiryRepository;
        this.feeStateRepository = feeStateRepository;
    }

    @Override
    @Transactional
    public FeeDemandService.GenerateResult generateDemandsForTermInstance(Long termInstanceId) {
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
        int yearlySkipped = 0;
        for (StudentTermEnrollment enrollment : enrollments) {
            // Yearly programs bill the full annual fee once on ODD term opening.
            // The EVEN term enrollment is skipped — it was already billed at ODD term.
            AssessmentPattern pattern = enrollment.getCohort().getProgram().getAssessmentPattern();
            if (pattern == AssessmentPattern.YEARLY && termInstance.getTermType() == TermType.EVEN) {
                yearlySkipped++;
                continue;
            }

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
            applyEnquiryCredit(demand, enrollment.getStudent().getId());
            count++;
        }
        return new FeeDemandService.GenerateResult(count, yearlySkipped);
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

    @Override
    public StudentTypeSwitchImpact previewStudentTypeSwitchImpact(Long studentId, StudentType targetType) {
        return computeStudentTypeSwitchImpact(studentId, targetType, false);
    }

    @Override
    @Transactional
    public StudentTypeSwitchImpact applyStudentTypeSwitchAdjustment(Long studentId, StudentType targetType,
                                                                      List<TermFeeOverrideInput> overrides) {
        if (overrides != null && !overrides.isEmpty()) {
            StudentTermEnrollment currentEnrollment = resolveCurrentEnrollment(studentId)
                .orElseThrow(() -> new IllegalStateException(
                    "Student has no active term enrollment: " + studentId));
            String actor = currentUserResolver.resolve();
            for (TermFeeOverrideInput input : overrides) {
                StudentTermFeeOverride override = termFeeOverrideRepository
                    .findByStudentIdAndSemesterNumber(studentId, input.semesterNumber())
                    .orElseGet(StudentTermFeeOverride::new);
                override.setStudent(currentEnrollment.getStudent());
                override.setSemesterNumber(input.semesterNumber());
                override.setOverrideAmount(input.amount());
                override.setSetBy(actor);
                override.setSetAt(Instant.now());
                termFeeOverrideRepository.save(override);
            }
        }
        return computeStudentTypeSwitchImpact(studentId, targetType, true);
    }

    @Override
    public List<TermFeeRow> previewTermFeeSchedule(Long studentId, StudentType targetType) {
        StudentTermEnrollment currentEnrollment = resolveCurrentEnrollment(studentId)
            .orElseThrow(() -> new IllegalStateException(
                "Student has no active term enrollment: " + studentId));

        Program program = currentEnrollment.getCohort().getProgram();
        AcademicYear academicYear = currentEnrollment.getTermInstance().getAcademicYear();
        FeeStructureGroup group = resolveFeeStructureGroup(
            currentEnrollment.getStudent(), currentEnrollment.getCohort(), academicYear);
        Integer totalTerms = program.getTotalTerms();
        int lastTerm = totalTerms != null ? totalTerms : currentEnrollment.getSemesterNumber();

        List<TermFeeRow> rows = new ArrayList<>();
        for (int semNum = currentEnrollment.getSemesterNumber(); semNum <= lastTerm; semNum++) {
            int yearOfStudy = computeYearOfStudy(semNum, program);
            BigDecimal calculated;
            try {
                calculated = sumFeeStructureGroupAmount(group, program, yearOfStudy, targetType);
            } catch (IllegalStateException e) {
                // No fee plan configured yet for that future year of study -- admin must
                // supply an explicit override for this row instead of a calculated default.
                calculated = null;
            }
            BigDecimal existingOverride = termFeeOverrideRepository
                .findByStudentIdAndSemesterNumber(studentId, semNum)
                .map(StudentTermFeeOverride::getOverrideAmount)
                .orElse(null);
            rows.add(new TermFeeRow(semNum, yearOfStudy, calculated, existingOverride));
        }
        return rows;
    }

    private int computeYearOfStudy(int semesterNumber, Program program) {
        AssessmentPattern pattern = program.getAssessmentPattern();
        return pattern == AssessmentPattern.YEARLY ? semesterNumber : (int) Math.ceil(semesterNumber / 2.0);
    }

    /**
     * A student can legitimately hold more than one ENROLLED {@link StudentTermEnrollment} at
     * once — an earlier term's enrollment is only marked COMPLETED when an actual promotion
     * decision runs, so it can still be ENROLLED after the student has already started a later
     * term. "Current term" is the ENROLLED row with the highest semesterNumber.
     */
    private Optional<StudentTermEnrollment> resolveCurrentEnrollment(Long studentId) {
        return enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED)
            .stream()
            .max(java.util.Comparator.comparing(StudentTermEnrollment::getSemesterNumber));
    }

    private StudentTypeSwitchImpact computeStudentTypeSwitchImpact(Long studentId, StudentType targetType,
                                                                     boolean persist) {
        List<FeeDemand> demands = feeDemandRepository.findByStudentTermEnrollmentStudentId(studentId)
            .stream()
            .filter(d -> d.getStatus() != DemandStatus.PAID && d.getStatus() != DemandStatus.WAIVED)
            .toList();

        List<DemandAdjustment> adjustments = new ArrayList<>();
        BigDecimal totalDelta = BigDecimal.ZERO;
        for (FeeDemand demand : demands) {
            StudentTermEnrollment enrollment = demand.getStudentTermEnrollment();
            BigDecimal previousAmount = demand.getTotalAmount();
            BigDecimal newAmount = deriveFeeTotalAmount(enrollment, demand.getAcademicYear(), targetType);
            BigDecimal delta = newAmount.subtract(previousAmount);
            if (delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (persist) {
                demand.setTotalAmount(newAmount);
                demand.setStatus(resolveDemandStatus(newAmount, demand.getPaidAmount()));
                feeDemandRepository.save(demand);
            }

            totalDelta = totalDelta.add(delta);
            adjustments.add(new DemandAdjustment(
                demand.getId(),
                enrollment.getId(),
                demand.getAcademicYear().getName() + " " + demand.getTermInstance().getTermType(),
                previousAmount,
                newAmount,
                delta
            ));
        }
        return new StudentTypeSwitchImpact(adjustments.size(), totalDelta, adjustments);
    }

    private DemandStatus resolveDemandStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        // Checked before the "nothing paid" case so a demand recomputed down to a zero total
        // (e.g. an all-HOSTEL_FEE plan for a day scholar) resolves to PAID, not UNPAID.
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return DemandStatus.PAID;
        }
        return paidAmount.compareTo(BigDecimal.ZERO) > 0 ? DemandStatus.PARTIAL : DemandStatus.UNPAID;
    }

    @Override
    public BigDecimal getOutstandingDuesForStudent(Long studentId) {
        return feeDemandRepository.findByStudentTermEnrollmentStudentId(studentId)
            .stream()
            .filter(d -> d.getStatus() != DemandStatus.WAIVED)
            .map(FeeDemand::getOutstandingAmount)
            .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal deriveFeeTotalAmount(StudentTermEnrollment enrollment, AcademicYear academicYear) {
        return deriveFeeTotalAmount(enrollment, academicYear, enrollment.getStudent().getStudentType());
    }

    // Day scholar vs hosteler cost is implicit in the fee plan: the HOSTEL_FEE row on a
    // FeeStructureGroup is a hosteler-only surcharge (see FeeStructureGroup javadoc) — excluded
    // here for DAY_SCHOLAR, mirroring the filter already applied at enquiry-estimate time in
    // FeeStructureService.findForEnquiry.
    //
    // A saved StudentTermFeeOverride (set via a boarding-status switch's editable term-fee step)
    // always wins over the calculated plan amount for that specific term -- this is the single
    // choke point both generateDemandsForTermInstance (future terms) and the switch's own demand
    // recompute (current term) go through, so one override row is honored everywhere.
    private BigDecimal deriveFeeTotalAmount(StudentTermEnrollment enrollment, AcademicYear academicYear,
                                             StudentType studentType) {
        Optional<StudentTermFeeOverride> override = termFeeOverrideRepository
            .findByStudentIdAndSemesterNumber(enrollment.getStudent().getId(), enrollment.getSemesterNumber());
        if (override.isPresent()) {
            return override.get().getOverrideAmount();
        }
        FeeStructureGroup group = resolveFeeStructureGroup(enrollment.getStudent(), enrollment.getCohort(), academicYear);
        return sumFeeStructureGroupAmount(group, enrollment.getCohort().getProgram(),
            enrollment.getYearOfStudy(), studentType);
    }

    /**
     * Resolves the SINGLE FeeStructureGroup matching the student's actual admission dimensions
     * (quota/feeState/gender, per BR-30) -- a program+academicYear can legitimately have many
     * groups, one per dimension combination, via {@code findByProgramIdAndAcademicYearId}. A
     * prior version of this method summed amounts across every group returned by that finder
     * instead of resolving to the one that actually applies to this student, silently inflating
     * every fee demand for any program with more than one configured group. Mirrors the exact
     * resolution (including fallback fee state) already used by FeeStructureService.findForEnquiry.
     */
    private FeeStructureGroup resolveFeeStructureGroup(Student student, Cohort cohort, AcademicYear academicYear) {
        Long courseId = cohort.getCourse() != null ? cohort.getCourse().getId() : null;
        Admission admission = admissionRepository.findByStudentId(student.getId())
            .orElseThrow(() -> new IllegalStateException(
                "No admission record found for student " + student.getId()
                + " -- cannot resolve which fee structure group applies."));
        Enquiry enquiry = enquiryRepository.findById(admission.getEnquiryId())
            .orElseThrow(() -> new IllegalStateException(
                "No enquiry record found for admission " + admission.getId()
                + " -- cannot resolve which fee structure group applies."));

        AdmissionQuota quota = enquiry.getAdmissionQuota();
        Long feeStateId = enquiry.getFeeState() != null ? enquiry.getFeeState().getId() : null;
        Gender gender = student.getGender();
        Long programId = cohort.getProgram().getId();

        Optional<FeeStructureGroup> group = feeStructureGroupRepository.findExact(
            programId, academicYear.getId(), courseId, quota, feeStateId, gender);

        if (group.isEmpty()) {
            Optional<FeeState> fallbackState = feeStateRepository.findByIsFallbackTrue();
            if (fallbackState.isPresent() && !fallbackState.get().getId().equals(feeStateId)) {
                group = feeStructureGroupRepository.findExact(
                    programId, academicYear.getId(), courseId, quota, fallbackState.get().getId(), gender);
            }
        }

        return group.orElseThrow(() -> new IllegalStateException(
            "No fee plan configured for program " + cohort.getProgram().getCode()
            + " and academic year " + academicYear.getName()
            + " matching quota=" + quota + ", gender=" + gender
            + ". Please configure a fee plan first."));
    }

    private BigDecimal sumFeeStructureGroupAmount(FeeStructureGroup group, Program program,
                                                    Integer yearOfStudy, StudentType studentType) {
        List<FeeStructure> structures = feeStructureRepository.findByFeeStructureGroupIdAndIsActiveTrue(group.getId());

        if (structures.isEmpty()) {
            throw new IllegalStateException(
                "No fee plan configured for program "
                + program.getCode()
                + ". Please configure a fee plan first.");
        }

        // Tracked separately from the studentType-filtered total so a plan that happens to be
        // entirely HOSTEL_FEE for this program (filtered to nothing for a day scholar) reports as
        // a legitimate zero rather than a false "no fee amounts configured" error.
        List<FeeStructureYearAmount> unfilteredAmounts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (FeeStructure fs : structures) {
            List<FeeStructureYearAmount> amounts =
                yearAmountRepository.findByFeeStructureIdAndYearNumber(fs.getId(), yearOfStudy);
            unfilteredAmounts.addAll(amounts);
            if (studentType != StudentType.DAY_SCHOLAR || fs.getFeeType() != FeeType.HOSTEL_FEE) {
                for (FeeStructureYearAmount ya : amounts) {
                    total = total.add(ya.getAmount());
                }
            }
        }

        if (unfilteredAmounts.isEmpty()) {
            throw new IllegalStateException(
                "No fee amounts configured for year of study " + yearOfStudy
                + " in program " + program.getCode()
                + ". Please configure fee amounts for this year.");
        }

        return total;
    }

    // Applies any unapplied enquiry payment credit to the freshly created demand.
    // Credit = total enquiry payments - sum of paid_amount already on other demands for this student.
    // Surplus (credit exceeding all demands) is NOT auto-applied — cashier handles manually.
    private void applyEnquiryCredit(FeeDemand demand, Long studentId) {
        admissionRepository.findByStudentId(studentId).ifPresent(admission -> {
            Long enquiryId = admission.getEnquiryId();
            BigDecimal totalEnquiryPaid = enquiryPaymentRepository.sumAmountPaidByEnquiryId(enquiryId);
            if (totalEnquiryPaid.compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal alreadyApplied = feeDemandRepository.sumPaidAmountByStudentId(studentId);
            BigDecimal remaining = totalEnquiryPaid.subtract(alreadyApplied).max(BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal toApply = remaining.min(demand.getTotalAmount());
            demand.setPaidAmount(toApply);
            demand.setStatus(toApply.compareTo(demand.getTotalAmount()) >= 0
                ? DemandStatus.PAID : DemandStatus.PARTIAL);
            feeDemandRepository.save(demand);
        });
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
