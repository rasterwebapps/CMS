package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryConversionPrefillResponse;
import com.cms.dto.EnquiryConversionRequest;
import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.dto.EnquiryStatusHistoryResponse;
import com.cms.dto.FeeFinalizationRequest;
import com.cms.dto.FeeFinalizationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.model.Course;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.Program;
import com.cms.model.ReferralType;
import com.cms.model.Student;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.AgentRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
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
    private final ReferralTypeRepository referralTypeRepository;
    private final CourseRepository courseRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;

    public EnquiryService(EnquiryRepository enquiryRepository,
                           ProgramRepository programRepository,
                           AgentRepository agentRepository,
                           StudentRepository studentRepository,
                           ReferralTypeRepository referralTypeRepository,
                           CourseRepository courseRepository,
                           EnquiryStatusHistoryRepository statusHistoryRepository) {
        this.enquiryRepository = enquiryRepository;
        this.programRepository = programRepository;
        this.agentRepository = agentRepository;
        this.studentRepository = studentRepository;
        this.referralTypeRepository = referralTypeRepository;
        this.courseRepository = courseRepository;
        this.statusHistoryRepository = statusHistoryRepository;
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
        enquiry.setFeeDiscussedAmount(request.feeDiscussedAmount());
        enquiry.setFeeGuidelineTotal(request.feeGuidelineTotal());
        enquiry.setReferralAdditionalAmount(request.referralAdditionalAmount());
        enquiry.setFinalCalculatedFee(request.finalCalculatedFee());
        enquiry.setYearWiseFees(request.yearWiseFees());
        enquiry.setStudentType(request.studentType());

        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, null, saved.getStatus(), "system", null);
        return toResponse(saved);
    }

    public List<EnquiryResponse> findAll() {
        return enquiryRepository.findAll().stream()
            .map(this::toResponse)
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
        enquiry.setFeeDiscussedAmount(request.feeDiscussedAmount());
        enquiry.setFeeGuidelineTotal(request.feeGuidelineTotal());
        enquiry.setReferralAdditionalAmount(request.referralAdditionalAmount());
        enquiry.setFinalCalculatedFee(request.finalCalculatedFee());
        enquiry.setYearWiseFees(request.yearWiseFees());
        enquiry.setStudentType(request.studentType());

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

        BigDecimal discount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        BigDecimal netFee = request.totalFee().subtract(discount);

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setFinalizedTotalFee(request.totalFee());
        enquiry.setFinalizedDiscountAmount(discount);
        enquiry.setFinalizedDiscountReason(request.discountReason());
        enquiry.setFinalizedNetFee(netFee);
        enquiry.setFinalizedBy(adminUsername);
        enquiry.setFinalizedAt(Instant.now());
        enquiry.setStatus(EnquiryStatus.FEES_FINALIZED);

        if (request.yearWiseFees() != null) {
            enquiry.setYearWiseFees(request.yearWiseFees());
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

        if (enquiry.getStatus() != EnquiryStatus.DOCUMENTS_SUBMITTED) {
            throw new IllegalStateException(
                "Enquiry must be in DOCUMENTS_SUBMITTED status to convert. Current status: " + enquiry.getStatus()
            );
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(EnquiryStatus.CONVERTED);
        enquiry.setConvertedStudentId(student.getId());

        Enquiry saved = enquiryRepository.save(enquiry);
        recordHistory(saved, oldStatus, EnquiryStatus.CONVERTED, "system", null);
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
    public EnquiryResponse updateStatus(Long id, EnquiryStatus status) {
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
        recordHistory(saved, oldStatus, status, "system", null);
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
        if (!enquiryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + id);
        }
        enquiryRepository.deleteById(id);
    }

    @Transactional
    public EnquiryResponse convertToStudentWithData(Long enquiryId, EnquiryConversionRequest request, String performedBy) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        if (enquiry.getStatus() != EnquiryStatus.DOCUMENTS_SUBMITTED) {
            throw new IllegalStateException(
                "Enquiry must be in DOCUMENTS_SUBMITTED status to convert. Current: " + enquiry.getStatus()
            );
        }

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

        Student savedStudent = studentRepository.save(student);

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(EnquiryStatus.CONVERTED);
        enquiry.setConvertedStudentId(savedStudent.getId());
        Enquiry saved = enquiryRepository.save(enquiry);

        recordHistory(saved, oldStatus, EnquiryStatus.CONVERTED, performedBy,
            "Converted to student ID: " + savedStudent.getId());

        return toResponse(saved);
    }

    public EnquiryConversionPrefillResponse getConversionPrefill(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        String fullName = enquiry.getName() != null ? enquiry.getName().trim() : "";
        int spaceIdx = fullName.indexOf(' ');
        String firstName = spaceIdx > 0 ? fullName.substring(0, spaceIdx) : fullName;
        String lastName = spaceIdx > 0 ? fullName.substring(spaceIdx + 1) : "";

        return new EnquiryConversionPrefillResponse(
            firstName,
            lastName,
            enquiry.getEmail(),
            enquiry.getPhone(),
            enquiry.getProgram() != null ? enquiry.getProgram().getId() : null,
            enquiry.getProgram() != null ? enquiry.getProgram().getName() : null,
            enquiry.getCourse() != null ? enquiry.getCourse().getId() : null,
            enquiry.getCourse() != null ? enquiry.getCourse().getName() : null,
            1
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

    private EnquiryResponse toResponse(Enquiry e) {
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
            e.getFeeGuidelineTotal(),
            e.getReferralAdditionalAmount(),
            e.getFinalCalculatedFee(),
            e.getYearWiseFees(),
            e.getStudentType(),
            e.getFinalizedTotalFee(),
            e.getFinalizedDiscountAmount(),
            e.getFinalizedDiscountReason(),
            e.getFinalizedNetFee(),
            e.getFinalizedBy(),
            e.getFinalizedAt(),
            e.getConvertedStudentId(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
