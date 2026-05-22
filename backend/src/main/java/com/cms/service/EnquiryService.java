 package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryConversionPrefillResponse;
import com.cms.dto.EnquiryConversionRequest;
import com.cms.dto.AddressRequest;
import com.cms.dto.DocumentVerificationStatusResponse;
import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.dto.EnquiryStatusHistoryResponse;
import com.cms.dto.FeeFinalizationRequest;
import com.cms.dto.FeeFinalizationResponse;
import com.cms.dto.FeeStructureResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.AcademicYear;
import com.cms.model.Agent;
import com.cms.model.Course;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.Program;
import com.cms.model.ReferralType;
import com.cms.model.Student;
import com.cms.model.FeeState;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.model.enums.CommissionSource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeeStateRepository;
import com.cms.repository.LocationCountryRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.ReferralTypeRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class EnquiryService {

    private static final Map<EnquiryStatus, Set<EnquiryStatus>> ALLOWED_MANUAL_TRANSITIONS;

    static {
        Map<EnquiryStatus, Set<EnquiryStatus>> map = new EnumMap<>(EnquiryStatus.class);
        map.put(EnquiryStatus.ENQUIRED,       EnumSet.of(EnquiryStatus.INTERESTED, EnquiryStatus.NOT_INTERESTED));
        map.put(EnquiryStatus.NOT_INTERESTED, EnumSet.of(EnquiryStatus.INTERESTED));
        map.put(EnquiryStatus.FEES_FINALIZED, EnumSet.of(EnquiryStatus.NOT_INTERESTED));
        map.put(EnquiryStatus.CLOSED,         EnumSet.of(EnquiryStatus.ENQUIRED));
        ALLOWED_MANUAL_TRANSITIONS = Map.copyOf(map);
    }

    private final EnquiryRepository enquiryRepository;
    private final ProgramRepository programRepository;
    private final AgentRepository agentRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ReferralTypeRepository referralTypeRepository;
    private final CourseRepository courseRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;
    private final AdmissionRepository admissionRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final EnquiryDocumentRepository enquiryDocumentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureService feeStructureService;
    private final FeeStateRepository feeStateRepository;
    private final EnquiryDocumentService enquiryDocumentService;
    private final ApplicationNumberSequenceService numberSequenceService;
    private final LocationCountryRepository countryRepository;

    public EnquiryService(EnquiryRepository enquiryRepository,
                           ProgramRepository programRepository,
                           AgentRepository agentRepository,
                           StudentRepository studentRepository,
                           FacultyRepository facultyRepository,
                           ReferralTypeRepository referralTypeRepository,
                           CourseRepository courseRepository,
                           EnquiryStatusHistoryRepository statusHistoryRepository,
                           AdmissionRepository admissionRepository,
                           EnquiryPaymentRepository enquiryPaymentRepository,
                           EnquiryDocumentRepository enquiryDocumentRepository,
                           AcademicYearRepository academicYearRepository,
                           FeeStructureService feeStructureService,
                           FeeStateRepository feeStateRepository,
                           EnquiryDocumentService enquiryDocumentService,
                           ApplicationNumberSequenceService numberSequenceService,
                           LocationCountryRepository countryRepository) {
        this.enquiryRepository = enquiryRepository;
        this.programRepository = programRepository;
        this.agentRepository = agentRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.referralTypeRepository = referralTypeRepository;
        this.courseRepository = courseRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.admissionRepository = admissionRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.enquiryDocumentRepository = enquiryDocumentRepository;
        this.academicYearRepository = academicYearRepository;
        this.feeStructureService = feeStructureService;
        this.feeStateRepository = feeStateRepository;
        this.enquiryDocumentService = enquiryDocumentService;
        this.numberSequenceService = numberSequenceService;
        this.countryRepository = countryRepository;
    }

    @Transactional
    public EnquiryResponse create(EnquiryRequest request) {
        Program program = null;
        if (request.programId() != null) {
            program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));
        }

        Agent agent = null;
        if (request.agentId() != null) {
            agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.agentId()));
        }

        ReferralType referralType = referralTypeRepository.findById(request.referralTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Referral type not found with id: " + request.referralTypeId()));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        EnquiryStatus status = request.status() != null ? request.status() : EnquiryStatus.ENQUIRED;

        Enquiry enquiry = new Enquiry(
            request.name(), request.email(), request.phone(),
            program, request.enquiryDate(), referralType, status
        );
        enquiry.setAgent(agent);
        enquiry.setCourse(course);
        enquiry.setRemarks(request.remarks());
        enquiry.setStudentType(request.studentType());
        enquiry.setDateOfBirth(request.dateOfBirth());
        enquiry.setGender(request.gender());
        enquiry.setAdmissionQuota(request.admissionQuota());
        applyFeeState(enquiry, request.feeStateId());
        applyAuthoritativeFees(enquiry, request);
        applyResolvedCommission(enquiry, referralType, agent);
        enquiry.setCountry(request.countryId() != null
            ? countryRepository.findById(request.countryId()).orElse(null)
            : null);
        enquiry.setState(request.state());
        enquiry.setDistrict(request.district());
        enquiry.setReferredStudentId(request.referredStudentId());
        enquiry.setReferredFacultyId(request.referredFacultyId());
        enquiry.setReferredStaffName(request.referredStaffName());

        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, null, saved.getStatus(), "system", null);
        return toResponse(saved);
    }

    public List<EnquiryResponse> findAll() {
        List<Enquiry> enquiries = enquiryRepository.findAll();
        List<Long> ids = enquiries.stream().map(Enquiry::getId).toList();
        Map<Long, BigDecimal> paidMap = enquiryPaymentRepository.paidTotalsForIds(ids);
        return enquiries.stream()
            .map(e -> toResponse(e, paidMap.getOrDefault(e.getId(), BigDecimal.ZERO)))
            .toList();
    }

    public EnquiryResponse findById(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));
        return toResponse(enquiry);
    }

    public List<EnquiryResponse> findByStatus(EnquiryStatus status) {
        return enquiryRepository.findByStatus(status).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EnquiryResponse> findByReferralTypeId(Long referralTypeId) {
        return enquiryRepository.findByReferralTypeId(referralTypeId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EnquiryResponse> findByAgentId(Long agentId) {
        return enquiryRepository.findByAgentId(agentId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EnquiryResponse update(Long id, EnquiryRequest request) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));

        Program program = null;
        if (request.programId() != null) {
            program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));
        }

        Agent agent = null;
        if (request.agentId() != null) {
            agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.agentId()));
        }

        ReferralType referralType = referralTypeRepository.findById(request.referralTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Referral type not found with id: " + request.referralTypeId()));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        enquiry.setName(request.name());
        enquiry.setEmail(request.email());
        enquiry.setPhone(request.phone());
        enquiry.setProgram(program);
        enquiry.setCourse(course);
        enquiry.setEnquiryDate(request.enquiryDate());
        enquiry.setAgent(agent);
        enquiry.setReferralType(referralType);
        enquiry.setRemarks(request.remarks());
        enquiry.setStudentType(request.studentType());
        enquiry.setDateOfBirth(request.dateOfBirth());
        enquiry.setGender(request.gender());
        enquiry.setAdmissionQuota(request.admissionQuota());
        applyFeeState(enquiry, request.feeStateId());
        applyAuthoritativeFees(enquiry, request);
        applyResolvedCommission(enquiry, referralType, agent);
        enquiry.setCountry(request.countryId() != null
            ? countryRepository.findById(request.countryId()).orElse(null)
            : null);
        enquiry.setState(request.state());
        enquiry.setDistrict(request.district());
        enquiry.setReferredStudentId(request.referredStudentId());
        enquiry.setReferredFacultyId(request.referredFacultyId());
        enquiry.setReferredStaffName(request.referredStaffName());

        if (request.status() != null) {
            enquiry.setStatus(request.status());
        }

        Enquiry updated = enquiryRepository.save(enquiry);
        return toResponse(updated);
    }

    @Transactional
    public FeeFinalizationResponse finalizeFees(Long enquiryId, FeeFinalizationRequest request, String adminUsername) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        if (enquiry.getStatus() != EnquiryStatus.INTERESTED) {
            throw new IllegalStateException(
                "Enquiry must be in INTERESTED status to finalize fees. Current status: " + enquiry.getStatus()
            );
        }

        // Validate that fee cannot be increased - only discounts are allowed
        BigDecimal originalCalculatedFee = enquiry.getFinalCalculatedFee();
        if (originalCalculatedFee == null) {
            throw new IllegalStateException(
                "Cannot finalize fees: no calculated fee found for this enquiry. Please ensure the fee is calculated first."
            );
        }

        BigDecimal requestedTotal = normalizeAmount(request.totalFee());
        if (requestedTotal.compareTo(originalCalculatedFee) > 0) {
            throw new IllegalArgumentException(
                "Fee increase is not allowed. Requested fee ₹" + requestedTotal +
                " exceeds the original calculated fee ₹" + originalCalculatedFee +
                ". Only discounts can be applied during finalization."
            );
        }

        BigDecimal authoritativeTotal = normalizeAmount(originalCalculatedFee);
        BigDecimal discount = normalizeAmount(request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount amount cannot be negative");
        }
        if (discount.compareTo(authoritativeTotal) > 0) {
            throw new IllegalArgumentException("Discount amount cannot exceed total fee");
        }
        if (discount.compareTo(BigDecimal.ZERO) > 0 &&
                (request.discountReason() == null || request.discountReason().isBlank())) {
            throw new IllegalArgumentException("Discount reason is required when a discount is applied");
        }
        BigDecimal netFee = authoritativeTotal.subtract(discount).setScale(2, RoundingMode.UNNECESSARY);

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setFinalizedTotalFee(authoritativeTotal);
        enquiry.setFinalizedDiscountAmount(discount);
        enquiry.setFinalizedDiscountReason(request.discountReason());
        enquiry.setFinalizedNetFee(netFee);
        enquiry.setFinalizedBy(adminUsername);
        enquiry.setFinalizedAt(Instant.now());
        enquiry.setStatus(EnquiryStatus.FEES_FINALIZED);

        if (request.yearWiseFees() != null) {
            enquiry.setYearWiseFees(request.yearWiseFees());
        }
        if (request.termWiseFees() != null) {
            enquiry.setSemesterWiseFees(request.termWiseFees());
        }

        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, oldStatus, EnquiryStatus.FEES_FINALIZED, adminUsername, null);

        return new FeeFinalizationResponse(
            saved.getId(),
            saved.getFinalizedTotalFee(),
            saved.getFinalizedDiscountAmount(),
            saved.getFinalizedDiscountReason(),
            saved.getFinalizedNetFee(),
            saved.getFinalizedBy(),
            saved.getFinalizedAt(),
            saved.getStatus().name()
        );
    }

    @Transactional
    public EnquiryResponse convertToStudent(Long enquiryId, Long studentId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        validateAdmissionReadiness(enquiry);

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(EnquiryStatus.ADMITTED);
        enquiry.setConvertedStudentId(student.getId());

        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, oldStatus, EnquiryStatus.ADMITTED, "system", null);
        return toResponse(saved);
    }

    public List<EnquiryResponse> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        return enquiryRepository.findByEnquiryDateBetween(fromDate, toDate).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EnquiryResponse> findByDateRangeAndStatus(LocalDate fromDate, LocalDate toDate, EnquiryStatus status) {
        return enquiryRepository.findByEnquiryDateBetweenAndStatus(fromDate, toDate, status).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EnquiryResponse updateStatus(Long id, EnquiryStatus status, String changedBy) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));
        Set<EnquiryStatus> allowed = ALLOWED_MANUAL_TRANSITIONS.getOrDefault(enquiry.getStatus(), Set.of());
        if (!allowed.contains(status)) {
            throw new IllegalStateException(
                "Cannot manually transition from " + enquiry.getStatus() + " to " + status
            );
        }
        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(status);
        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, oldStatus, status, changedBy, null);
        return toResponse(saved);
    }

    @Transactional
    public EnquiryResponse submitDocuments(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));
        if (enquiry.getStatus() != EnquiryStatus.FEES_PAID
            && enquiry.getStatus() != EnquiryStatus.PARTIALLY_PAID) {
            throw new IllegalStateException(
                "Enquiry must be in FEES_PAID or PARTIALLY_PAID status to submit documents. Current status: " + enquiry.getStatus()
            );
        }
        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(EnquiryStatus.DOCUMENTS_SUBMITTED);
        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, oldStatus, EnquiryStatus.DOCUMENTS_SUBMITTED, "system", null);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + id));
        if (enquiry.getStatus() != EnquiryStatus.ENQUIRED) {
            throw new IllegalStateException(
                "Enquiry can only be deleted when in ENQUIRED status. Current status: " + enquiry.getStatus()
            );
        }
        enquiryRepository.deleteById(id);
    }

    public List<EnquiryResponse> findDocumentPending() {
        return enquiryRepository.findByStatusIn(
            List.of(EnquiryStatus.FEES_PAID, EnquiryStatus.PARTIALLY_PAID)
        ).stream().map(this::toResponse).toList();
    }

    public List<EnquiryResponse> findDocumentVerificationPending() {
        return enquiryRepository.findByStatus(EnquiryStatus.DOCUMENTS_SUBMITTED).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<EnquiryResponse> findAdmissionPending() {
        return enquiryRepository.findByStatus(EnquiryStatus.DOCUMENTS_VERIFIED).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EnquiryResponse convertToStudentWithData(Long enquiryId, EnquiryConversionRequest request, String performedBy) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        validateAdmissionReadiness(enquiry);

        if (studentRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("A student with this email already exists: " + request.email());
        }

        Program program = enquiry.getProgram();
        if (program == null) {
            throw new IllegalStateException("Enquiry must have a program to convert to student");
        }

        Student student = new Student(
            null,
            request.firstName(),
            request.lastName(),
            request.email(),
            program,
            request.semester(),
            request.admissionDate(),
            com.cms.model.enums.StudentStatus.ACTIVE
        );
        student.setPhone(request.phone());
        if (enquiry.getCourse() != null) {
            student.setCourse(enquiry.getCourse());
        }

        // Personal information (fall back to enquiry data if not provided in convert request)
        student.setDateOfBirth(request.dateOfBirth() != null ? request.dateOfBirth() : enquiry.getDateOfBirth());
        student.setGender(request.gender() != null ? request.gender() : enquiry.getGender());
        student.setAadharNumber(request.aadharNumber());

        // Demographics
        student.setNationality(request.nationality());
        student.setReligion(request.religion());
        student.setCommunityCategory(request.communityCategory());
        student.setCaste(request.caste());
        student.setBloodGroup(request.bloodGroup());

        // Family information
        student.setFatherName(request.fatherName());
        student.setFatherPhone(request.fatherPhone());
        student.setFatherEmail(request.fatherEmail());
        student.setMotherName(request.motherName());
        student.setMotherPhone(request.motherPhone());
        student.setMotherEmail(request.motherEmail());
        student.setParentMobile(request.parentMobile());

        // Address
        if (request.address() != null) {
            AddressRequest addr = request.address();
            student.setAddress(new com.cms.model.Address(
                addr.countryId(),
                addr.postalAddress(),
                addr.street(),
                addr.city(),
                addr.district(),
                addr.state(),
                addr.pincode()
            ));
        }

        Student savedStudent = studentRepository.save(student);

        AcademicYear joiningYear = academicYearRepository.findById(request.joiningAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found: " + request.joiningAcademicYearId()));

        Admission admission = new Admission(savedStudent, joiningYear, request.applicationDate());
        admission.setParentConsentGiven(request.parentConsentGiven());
        admission.setApplicantConsentGiven(request.applicantConsentGiven());
        admission.setDeclarationPlace(request.declarationPlace());
        admission.setDeclarationDate(request.declarationDate());
        Admission savedAdmission = admissionRepository.save(admission);
        linkEnquiryDocumentsToAdmission(enquiryId, savedAdmission);
        savedStudent.setAdmissionNumber(numberSequenceService.nextAdmissionNumber(joiningYear, savedStudent.getCourse()));
        studentRepository.save(savedStudent);

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(EnquiryStatus.ADMITTED);
        enquiry.setConvertedStudentId(savedStudent.getId());
        Enquiry saved = enquiryRepository.save(enquiry);

        recordHistory(saved, oldStatus, EnquiryStatus.ADMITTED, performedBy,
            "Admitted: student ID " + savedStudent.getId() + ", admission number "
                + savedStudent.getAdmissionNumber() + ", admission created");

        BigDecimal paid = enquiryPaymentRepository.sumAmountPaidByEnquiryId(saved.getId());
        return toResponse(saved, paid, savedStudent.getAdmissionNumber());
    }

    private void linkEnquiryDocumentsToAdmission(Long enquiryId, Admission admission) {
        List<EnquiryDocument> enquiryDocuments = enquiryDocumentRepository.findByEnquiryId(enquiryId);
        enquiryDocuments.forEach(doc -> doc.setAdmission(admission));
        if (!enquiryDocuments.isEmpty()) {
            enquiryDocumentRepository.saveAll(enquiryDocuments);
        }
    }

    private void validateAdmissionReadiness(Enquiry enquiry) {
        if (enquiry.getStatus() != EnquiryStatus.DOCUMENTS_VERIFIED) {
            throw new IllegalStateException(
                "Enquiry must be in DOCUMENTS_VERIFIED status to complete admission. Current status: " + enquiry.getStatus()
            );
        }

        DocumentVerificationStatusResponse verification = enquiryDocumentService
            .allMandatoryDocumentsVerified(enquiry.getId());
        if (!verification.allVerified()) {
            String unverified = verification.unverifiedDocumentTypes().isEmpty()
                ? ""
                : " Unverified: " + String.join(", ", verification.unverifiedDocumentTypes());
            throw new IllegalStateException(
                "All mandatory documents must be verified before completing admission." + unverified
            );
        }
    }

    public EnquiryConversionPrefillResponse getConversionPrefill(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        String fullName = enquiry.getName() != null ? enquiry.getName().trim() : "";
        int spaceIdx = fullName.indexOf(' ');
        String firstName = spaceIdx > 0 ? fullName.substring(0, spaceIdx) : fullName;
        String lastName = spaceIdx > 0 ? fullName.substring(spaceIdx + 1) : "";

        int currentYear = LocalDate.now().getYear();

        return new EnquiryConversionPrefillResponse(
            firstName,
            lastName,
            enquiry.getEmail(),
            enquiry.getPhone(),
            enquiry.getProgram() != null ? enquiry.getProgram().getId() : null,
            enquiry.getProgram() != null ? enquiry.getProgram().getName() : null,
            enquiry.getCourse() != null ? enquiry.getCourse().getId() : null,
            enquiry.getCourse() != null ? enquiry.getCourse().getName() : null,
            1,
            currentYear,
            currentYear + 1,
            LocalDate.now(),
            enquiry.getDateOfBirth(),
            enquiry.getGender(),
            enquiry.getStudentType(),
            enquiry.getCountry() != null ? enquiry.getCountry().getId() : null,
            enquiry.getCountry() != null ? enquiry.getCountry().getName() : null,
            enquiry.getState(),
            enquiry.getDistrict(),
            enquiry.getRemarks()
        );
    }

    public List<EnquiryStatusHistoryResponse> getStatusHistory(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + enquiryId);
        }
        return statusHistoryRepository.findByEnquiryIdOrderByChangedAtAsc(enquiryId).stream()
            .map(h -> new EnquiryStatusHistoryResponse(
                h.getId(),
                h.getEnquiry().getId(),
                h.getFromStatus() != null ? h.getFromStatus().name() : null,
                h.getToStatus().name(),
                h.getChangedBy(),
                h.getChangedAt(),
                h.getRemarks()
            ))
            .toList();
    }

    private void recordHistory(Enquiry enquiry, EnquiryStatus from, EnquiryStatus to,
                                String changedBy, String remarks) {
        statusHistoryRepository.save(new EnquiryStatusHistory(enquiry, from, to, changedBy, remarks));
    }

    private void applyFeeState(Enquiry enquiry, Long feeStateId) {
        if (feeStateId != null) {
            feeStateRepository.findById(feeStateId).ifPresent(enquiry::setFeeState);
        }
    }

    private void applyAuthoritativeFees(Enquiry enquiry, EnquiryRequest request) {
        // courseId is intentionally NOT in the null-guard — programs without courses
        // send courseId = null and must still have their fee calculated.
        // The form's updateCourseValidator enforces courseId when the program has courses.
        if (request.programId() == null
                || request.admissionQuota() == null || request.feeStateId() == null
                || request.gender() == null) {
            enquiry.setFeeDiscussedAmount(normalizeNullable(request.feeDiscussedAmount()));
            enquiry.setFinalCalculatedFee(null);
            enquiry.setYearWiseFees(null);
            return;
        }

        var feeItems = feeStructureService.findForEnquiry(
            request.programId(), request.courseId(),
            request.admissionQuota(), request.feeStateId(),
            request.gender()
        );

        if (feeItems.isEmpty()) {
            String stateName = feeStateRepository.findById(request.feeStateId())
                .map(com.cms.model.FeeState::getName)
                .orElse("unknown state");
            throw new IllegalArgumentException(
                "No fee structure configured for this combination: "
                + request.admissionQuota() + " quota, "
                + stateName + ", "
                + request.gender()
                + ". Please ask the admin to configure the fee structure.");
        }

        List<FeeStructureResponse> items = feeItems.get();
        BigDecimal total = items.stream()
            .map(FeeStructureResponse::amount)
            .map(this::normalizeAmount)
            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        enquiry.setFeeDiscussedAmount(total);
        enquiry.setFinalCalculatedFee(total);
        enquiry.setYearWiseFees(buildYearWiseFeesJson(items));
    }

    private String buildYearWiseFeesJson(List<FeeStructureResponse> feeStructures) {
        Map<Integer, BigDecimal> yearTotals = new LinkedHashMap<>();
        for (FeeStructureResponse fs : feeStructures) {
            for (var yearAmount : fs.yearAmounts()) {
                yearTotals.merge(yearAmount.yearNumber(), normalizeAmount(yearAmount.amount()), BigDecimal::add);
            }
        }
        if (yearTotals.isEmpty()) {
            return null;
        }
        return yearTotals.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> "{\"yearNumber\":" + e.getKey() + ",\"amount\":" + e.getValue().toPlainString() + "}")
            .collect(Collectors.joining(",", "[", "]"));
    }

    private void applyResolvedCommission(Enquiry enquiry, ReferralType referralType, Agent agent) {
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        CommissionSource source = CommissionSource.NONE;
        if (agent != null && agent.getCommissionAmount() != null
                && agent.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            amount = normalizeAmount(agent.getCommissionAmount());
            source = CommissionSource.AGENT;
        } else if (referralType != null && Boolean.TRUE.equals(referralType.getHasCommission())) {
            amount = normalizeAmount(referralType.getCommissionAmount() != null
                ? referralType.getCommissionAmount() : BigDecimal.ZERO);
            source = amount.compareTo(BigDecimal.ZERO) > 0 ? CommissionSource.REFERRAL_TYPE : CommissionSource.NONE;
        }
        enquiry.setCommissionAmount(amount);
        enquiry.setCommissionSource(source);
        enquiry.setCommissionPaymentStatus(
            amount.compareTo(BigDecimal.ZERO) > 0 ? CommissionPaymentStatus.PENDING : CommissionPaymentStatus.NOT_APPLICABLE);
    }

    private BigDecimal normalizeNullable(BigDecimal value) {
        return value == null ? null : normalizeAmount(value);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Monetary amounts must not have more than two decimal places");
        }
    }

    private String resolveStudentName(Long studentId) {
        if (studentId == null) return null;
        return studentRepository.findById(studentId)
            .map(s -> s.getFullName()).orElse(null);
    }

    private String resolveFacultyName(Long facultyId) {
        if (facultyId == null) return null;
        return facultyRepository.findById(facultyId)
            .map(f -> f.getFullName()).orElse(null);
    }

    private EnquiryResponse toResponse(Enquiry e) {
        BigDecimal paid = enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId());
        return toResponse(e, paid, null);
    }

    private EnquiryResponse toResponse(Enquiry e, BigDecimal totalPaid) {
        return toResponse(e, totalPaid, null);
    }

    private EnquiryResponse toResponse(Enquiry e, BigDecimal totalPaid, String admissionNumber) {
        return new EnquiryResponse(
            e.getId(),
            e.getName(),
            e.getEmail(),
            e.getPhone(),
            e.getProgram() != null ? e.getProgram().getId() : null,
            e.getProgram() != null ? e.getProgram().getName() : null,
            e.getCourse() != null ? e.getCourse().getId() : null,
            e.getCourse() != null ? e.getCourse().getName() : null,
            e.getEnquiryDate(),
            e.getReferralType() != null ? e.getReferralType().getId() : null,
            e.getReferralType() != null ? e.getReferralType().getName() : null,
            e.getReferralType() != null ? e.getReferralType().getCommissionAmount() : null,
            e.getReferralType() != null ? e.getReferralType().getHasCommission() : null,
            e.getStatus(),
            e.getAgent() != null ? e.getAgent().getId() : null,
            e.getAgent() != null ? e.getAgent().getName() : null,
            e.getRemarks(),
            e.getFeeDiscussedAmount(),
            null, // feeGuidelineTotal — field removed from Enquiry entity
            null, // referralAdditionalAmount — field removed from Enquiry entity
            e.getFinalCalculatedFee(),
            e.getYearWiseFees(),
            e.getSemesterWiseFees(),
            e.getStudentType(),
            e.getFinalizedTotalFee(),
            e.getFinalizedDiscountAmount(),
            e.getFinalizedDiscountReason(),
            e.getFinalizedNetFee(),
            e.getFinalizedBy(),
            e.getFinalizedAt(),
            e.getConvertedStudentId(),
            e.getCountry() != null ? e.getCountry().getId() : null,
            e.getCountry() != null ? e.getCountry().getName() : null,
            e.getState(),
            e.getDistrict(),
            e.getReferredStudentId(),
            resolveStudentName(e.getReferredStudentId()),
            e.getReferredFacultyId(),
            resolveFacultyName(e.getReferredFacultyId()),
            e.getReferredStaffName(),
            e.getCreatedAt(),
            e.getUpdatedAt(),
            totalPaid,
            e.getDateOfBirth(),
            e.getGender(),
            e.getAdmissionQuota(),
            e.getFeeState() != null ? e.getFeeState().getId() : null,
            e.getFeeState() != null ? e.getFeeState().getName() : null,
            admissionNumber
        );
    }
}
