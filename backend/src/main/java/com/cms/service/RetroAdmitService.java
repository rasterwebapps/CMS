package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AddressRequest;
import com.cms.dto.RetroAdmitRequest;
import com.cms.dto.RetroAdmitResponse;
import com.cms.dto.LegacyPaymentEntry;
import com.cms.dto.LegacyYearFeeEntry;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.AcademicYear;
import com.cms.model.Address;
import com.cms.model.Course;
import com.cms.model.Enquiry;
import com.cms.model.FeeInstallment;
import com.cms.model.Program;
import com.cms.model.ReferralType;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.TermBillingSchedule;
import com.cms.model.enums.AdmissionCategory;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.model.enums.CommissionSource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.StudentType;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.ReferralTypeRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermBillingScheduleRepository;

@Service
public class RetroAdmitService {

    private static final String[] ORDINALS = {
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth",
        "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth"
    };

    private final StudentRepository studentRepository;
    private final AdmissionRepository admissionRepository;
    private final EnquiryRepository enquiryRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ReferralTypeRepository referralTypeRepository;
    private final AgentRepository agentRepository;
    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final TermBillingScheduleRepository billingScheduleRepository;
    private final ApplicationNumberSequenceService numberSequenceService;
    private final UnifiedReceiptService unifiedReceiptService;

    public RetroAdmitService(StudentRepository studentRepository,
                               AdmissionRepository admissionRepository,
                               EnquiryRepository enquiryRepository,
                               ProgramRepository programRepository,
                               CourseRepository courseRepository,
                               AcademicYearRepository academicYearRepository,
                               ReferralTypeRepository referralTypeRepository,
                               AgentRepository agentRepository,
                               StudentFeeAllocationRepository allocationRepository,
                               SemesterFeeRepository semesterFeeRepository,
                               FeeInstallmentRepository installmentRepository,
                               TermBillingScheduleRepository billingScheduleRepository,
                               ApplicationNumberSequenceService numberSequenceService,
                               UnifiedReceiptService unifiedReceiptService) {
        this.studentRepository = studentRepository;
        this.admissionRepository = admissionRepository;
        this.enquiryRepository = enquiryRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.academicYearRepository = academicYearRepository;
        this.referralTypeRepository = referralTypeRepository;
        this.agentRepository = agentRepository;
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.billingScheduleRepository = billingScheduleRepository;
        this.numberSequenceService = numberSequenceService;
        this.unifiedReceiptService = unifiedReceiptService;
    }

    @Transactional
    public RetroAdmitResponse admit(RetroAdmitRequest request, String performedBy) {

        // ── Step 1: Resolve master entities ───────────────────────────────
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found: " + request.programId()));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.courseId()));
        }

        AcademicYear joiningYear = academicYearRepository.findById(request.joiningAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.joiningAcademicYearId()));

        if (studentRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("A student with this email already exists: " + request.email());
        }
        if (studentRepository.existsByRollNumber(request.rollNumber())) {
            throw new IllegalStateException("A student with Roll Number " + request.rollNumber() + " already exists. Please verify the roll number.");
        }
        if (studentRepository.existsByUniversityRegistrationNumber(request.universityRegistrationNumber())) {
            throw new IllegalStateException("A student with University Registration Number " + request.universityRegistrationNumber() + " already exists. Please verify the URN.");
        }
        if (request.umisNumber() != null && !request.umisNumber().isBlank()
                && studentRepository.existsByUmisNumber(request.umisNumber())) {
            throw new IllegalStateException("A student with UMIS Number " + request.umisNumber() + " already exists. Please verify the UMIS number.");
        }

        // ── Step 2: Create Student ─────────────────────────────────────────
        AdmissionQuota quota = request.admissionQuota() != null ? request.admissionQuota() : AdmissionQuota.MANAGEMENT;
        StudentType studentType = request.studentType() != null ? request.studentType() : StudentType.DAY_SCHOLAR;

        Student student = new Student(
                null,
                request.firstName(),
                request.lastName(),
                request.email(),
                program,
                request.yearOfStudy(),
                request.admissionDate(),
                StudentStatus.ACTIVE
        );
        student.setPhone(request.phone());
        if (course != null) student.setCourse(course);
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setAadharNumber(request.aadharNumber());
        student.setNationality(request.nationality());
        student.setReligion(request.religion());
        student.setCommunityCategory(request.communityCategory());
        student.setCaste(request.caste());
        student.setBloodGroup(request.bloodGroup());
        student.setPhysicalDisability(Boolean.TRUE.equals(request.physicalDisability()));
        student.setAdmissionCategory(AdmissionCategory.valueOf(quota.name()));
        student.setFatherName(request.fatherName());
        student.setFatherPhone(request.fatherPhone());
        student.setFatherEmail(request.fatherEmail());
        student.setMotherName(request.motherName());
        student.setMotherPhone(request.motherPhone());
        student.setMotherEmail(request.motherEmail());
        student.setParentMobile(request.parentMobile());
        student.setRollNumber(request.rollNumber());
        student.setUniversityRegistrationNumber(request.universityRegistrationNumber());
        if (request.umisNumber() != null && !request.umisNumber().isBlank()) {
            student.setUmisNumber(request.umisNumber());
        }

        if (request.address() != null) {
            AddressRequest addr = request.address();
            student.setAddress(new Address(
                    addr.countryId(), addr.postalAddress(), addr.street(),
                    addr.city(), addr.district(), addr.state(), addr.pincode()
            ));
        }

        // ── Step 3: Generate admission number + save ───────────────────────
        student.setAdmissionNumber(numberSequenceService.nextAdmissionNumber(joiningYear, course));
        Student saved = studentRepository.save(student);

        // ── Step 4: Create synthetic Enquiry ──────────────────────────────
        ReferralType referralType;
        if (request.referralTypeId() != null) {
            referralType = referralTypeRepository.findById(request.referralTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Referral type not found: " + request.referralTypeId()));
        } else {
            referralType = referralTypeRepository.findByCode("WALK_IN")
                    .orElseThrow(() -> new ResourceNotFoundException("WALK_IN referral type not seeded"));
        }

        Enquiry enquiry = new Enquiry();
        enquiry.setName(request.firstName() + " " + request.lastName());
        enquiry.setEmail(request.email());
        enquiry.setPhone(request.phone());
        enquiry.setProgram(program);
        enquiry.setCourse(course);
        enquiry.setDateOfBirth(request.dateOfBirth() != null ? request.dateOfBirth() : LocalDate.of(2000, 1, 1));
        enquiry.setGender(request.gender());
        enquiry.setEnquiryDate(request.admissionDate());
        enquiry.setStatus(EnquiryStatus.ADMITTED);
        enquiry.setAdmissionSource("DIRECT_ADMIT");
        enquiry.setAdmissionQuota(quota);
        enquiry.setStudentType(studentType);
        enquiry.setConvertedStudentId(saved.getId());
        enquiry.setReferralType(referralType);

        if (request.agentId() != null) {
            enquiry.setAgent(agentRepository.findById(request.agentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + request.agentId())));
        }
        if (request.commissionAmount() != null && request.commissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            enquiry.setCommissionAmount(request.commissionAmount());
            enquiry.setCommissionPaidAmount(request.commissionAmount());
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.PAID);
            enquiry.setCommissionSource(CommissionSource.AGENT);
        } else {
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.NOT_APPLICABLE);
        }
        if (request.referredStudentId() != null) enquiry.setReferredStudentId(request.referredStudentId());
        if (request.referredFacultyId() != null) enquiry.setReferredFacultyId(request.referredFacultyId());

        Enquiry savedEnquiry = enquiryRepository.save(enquiry);

        // ── Step 5: Create Admission ───────────────────────────────────────
        Admission admission = new Admission(saved, joiningYear, request.applicationDate());
        admission.setEnquiryId(savedEnquiry.getId());
        admission.setDeclarationPlace(request.declarationPlace());
        admission.setDeclarationDate(request.declarationDate());
        admission.setParentConsentGiven(true);
        admission.setApplicantConsentGiven(true);
        admissionRepository.save(admission);

        // ── Step 6: Fee allocation + semester rows ─────────────────────────
        int yearsWithFeeRecords = 0;
        int paymentRowsCreated = 0;
        BigDecimal totalHistoricalPaid = BigDecimal.ZERO;

        List<LegacyYearFeeEntry> yearFees = request.yearFees() != null ? request.yearFees() : List.of();
        List<LegacyPaymentEntry> payments = request.payments() != null ? request.payments() : List.of();

        if (!yearFees.isEmpty()) {
            BigDecimal totalFee = yearFees.stream()
                    .map(LegacyYearFeeEntry::totalFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StudentFeeAllocation allocation = new StudentFeeAllocation(
                    saved, program, totalFee,
                    BigDecimal.ZERO, null, BigDecimal.ZERO, totalFee,
                    FeeAllocationStatus.FINALIZED
            );
            allocation.setHasHostelFee(studentType == StudentType.HOSTELER);
            allocation.setFinalizedAt(Instant.now());
            allocation.setFinalizedBy(performedBy);
            StudentFeeAllocation savedAllocation = allocationRepository.save(allocation);

            boolean isYearly = program.getAssessmentPattern() == AssessmentPattern.YEARLY;
            int joiningStartYear = joiningYear.getStartYear();

            // Resolve fallback billing dates from the joining year
            LocalDate fallbackOdd = billingScheduleRepository
                    .findByAcademicYearIdAndTermType(joiningYear.getId(), TermType.ODD)
                    .map(TermBillingSchedule::getDueDate).orElse(null);
            LocalDate fallbackEven = billingScheduleRepository
                    .findByAcademicYearIdAndTermType(joiningYear.getId(), TermType.EVEN)
                    .map(TermBillingSchedule::getDueDate).orElse(null);

            for (LegacyYearFeeEntry yearFee : yearFees) {
                if (yearFee.totalFee().compareTo(BigDecimal.ZERO) == 0) continue;
                yearsWithFeeRecords++;

                int targetStartYear = joiningStartYear + (yearFee.yearNumber() - 1);
                Optional<AcademicYear> targetYearOpt = academicYearRepository
                        .findByNameStartingWith(String.valueOf(targetStartYear));

                LocalDate oddDueDate;
                LocalDate evenDueDate;
                if (targetYearOpt.isPresent()) {
                    Long ayId = targetYearOpt.get().getId();
                    oddDueDate = billingScheduleRepository.findByAcademicYearIdAndTermType(ayId, TermType.ODD)
                            .map(TermBillingSchedule::getDueDate)
                            .orElseGet(() -> shiftDueYear(fallbackOdd, joiningStartYear, targetStartYear));
                    evenDueDate = billingScheduleRepository.findByAcademicYearIdAndTermType(ayId, TermType.EVEN)
                            .map(TermBillingSchedule::getDueDate)
                            .orElseGet(() -> shiftDueYear(fallbackEven, joiningStartYear, targetStartYear));
                } else {
                    oddDueDate = shiftDueYear(fallbackOdd, joiningStartYear, targetStartYear);
                    evenDueDate = shiftDueYear(fallbackEven, joiningStartYear, targetStartYear);
                }

                if (isYearly) {
                    int globalSeq = yearFee.yearNumber();
                    semesterFeeRepository.save(new SemesterFee(
                            savedAllocation, yearFee.yearNumber(),
                            "Year " + yearFee.yearNumber() + " - " + ordinalLabel(globalSeq),
                            yearFee.totalFee(),
                            oddDueDate != null ? oddDueDate : LocalDate.of(targetStartYear, 6, 30),
                            1
                    ));
                } else {
                    BigDecimal sem1 = yearFee.totalFee().divide(BigDecimal.TWO, 0, RoundingMode.FLOOR);
                    BigDecimal sem2 = yearFee.totalFee().subtract(sem1);
                    int globalSem1 = (yearFee.yearNumber() - 1) * 2 + 1;
                    int globalSem2 = globalSem1 + 1;

                    semesterFeeRepository.save(new SemesterFee(
                            savedAllocation, yearFee.yearNumber(),
                            "Year " + yearFee.yearNumber() + " - " + ordinalLabel(globalSem1),
                            sem1,
                            oddDueDate != null ? oddDueDate : LocalDate.of(targetStartYear, 6, 30),
                            1
                    ));
                    semesterFeeRepository.save(new SemesterFee(
                            savedAllocation, yearFee.yearNumber(),
                            "Year " + yearFee.yearNumber() + " - " + ordinalLabel(globalSem2),
                            sem2,
                            evenDueDate != null ? evenDueDate : LocalDate.of(targetStartYear, 11, 30),
                            2
                    ));
                }
            }

            // ── Step 7: FIFO allocation of historical payments ────────────
            List<SemesterFee> slots = semesterFeeRepository
                    .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(savedAllocation.getId());

            int slotIdx = 0;
            BigDecimal slotRemaining = slots.isEmpty() ? BigDecimal.ZERO : slots.get(0).getAmount();

            String programName = course != null ? course.getName() : program.getName();
            String feeCategory = studentType == StudentType.HOSTELER ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";

            for (LegacyPaymentEntry entry : payments) {
                if (slotIdx >= slots.size()) break;

                BigDecimal pmtRemaining = entry.amount();
                String receiptNo = (entry.receiptNumber() != null && !entry.receiptNumber().isBlank())
                        ? entry.receiptNumber()
                        : unifiedReceiptService.generateReceiptNumber(entry.paymentDate().getYear());

                List<String> coveredLabels = new ArrayList<>();

                while (pmtRemaining.compareTo(BigDecimal.ZERO) > 0 && slotIdx < slots.size()) {
                    SemesterFee sf = slots.get(slotIdx);
                    BigDecimal apply = pmtRemaining.min(slotRemaining);
                    pmtRemaining  = pmtRemaining.subtract(apply);
                    slotRemaining = slotRemaining.subtract(apply);

                    FeeInstallment inst = new FeeInstallment(
                            sf, saved, apply,
                            entry.paymentDate(), entry.paymentMode(), receiptNo
                    );
                    inst.setTransactionReference(entry.transactionReference());
                    inst.setRemarks(entry.remarks());
                    installmentRepository.save(inst);

                    coveredLabels.add(sf.getSemesterLabel());
                    paymentRowsCreated++;
                    totalHistoricalPaid = totalHistoricalPaid.add(apply);

                    if (slotRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                        slotIdx++;
                        slotRemaining = (slotIdx < slots.size())
                                ? slots.get(slotIdx).getAmount()
                                : BigDecimal.ZERO;
                    }
                }

                if (!coveredLabels.isEmpty()) {
                    unifiedReceiptService.saveStudentReceipt(
                            receiptNo,
                            saved.getId(), saved.getFullName(), saved.getRollNumber(), saved.getAdmissionNumber(),
                            programName, entry.amount(),
                            entry.paymentDate(), entry.paymentMode().name(),
                            entry.transactionReference(), entry.remarks(),
                            String.join(", ", coveredLabels), performedBy, feeCategory
                    );
                }
            }
        }

        return new RetroAdmitResponse(
                saved.getId(),
                saved.getAdmissionNumber(),
                saved.getFullName(),
                saved.getRollNumber(),
                savedEnquiry.getId(),
                yearsWithFeeRecords,
                paymentRowsCreated,
                totalHistoricalPaid
        );
    }

    private static String ordinalLabel(int seq) {
        if (seq >= 1 && seq <= ORDINALS.length) return ORDINALS[seq - 1] + " Installment";
        return "Installment " + seq;
    }

    private static LocalDate shiftDueYear(LocalDate base, int fromYear, int toYear) {
        if (base == null) return null;
        return base.plusYears((long) (toYear - fromYear));
    }
}
