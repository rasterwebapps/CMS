package com.cms.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.model.Enquiry;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.AgentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final ProgramRepository programRepository;
    private final AgentRepository agentRepository;
    private final StudentRepository studentRepository;

    public EnquiryService(EnquiryRepository enquiryRepository,
                           ProgramRepository programRepository,
                           AgentRepository agentRepository,
                           StudentRepository studentRepository) {
        this.enquiryRepository = enquiryRepository;
        this.programRepository = programRepository;
        this.agentRepository = agentRepository;
        this.studentRepository = studentRepository;
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

        EnquiryStatus status = request.status() != null ? request.status() : EnquiryStatus.NEW;

        Enquiry enquiry = new Enquiry(
            request.name(), request.email(), request.phone(),
            program, request.enquiryDate(), request.source(), status
        );
        enquiry.setAgent(agent);
        enquiry.setAssignedTo(request.assignedTo());
        enquiry.setRemarks(request.remarks());
        enquiry.setFeeDiscussedAmount(request.feeDiscussedAmount());

        Enquiry saved = enquiryRepository.save(enquiry);
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

    public List<EnquiryResponse> findBySource(EnquirySource source) {
        return enquiryRepository.findBySource(source).stream()
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

        enquiry.setName(request.name());
        enquiry.setEmail(request.email());
        enquiry.setPhone(request.phone());
        enquiry.setProgram(program);
        enquiry.setEnquiryDate(request.enquiryDate());
        enquiry.setSource(request.source());
        enquiry.setAgent(agent);
        enquiry.setAssignedTo(request.assignedTo());
        enquiry.setRemarks(request.remarks());
        enquiry.setFeeDiscussedAmount(request.feeDiscussedAmount());

        if (request.status() != null) {
            enquiry.setStatus(request.status());
        }

        Enquiry updated = enquiryRepository.save(enquiry);
        return toResponse(updated);
    }

    @Transactional
    public EnquiryResponse convertToStudent(Long enquiryId, Long studentId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        if (enquiry.getStatus() != EnquiryStatus.INTERESTED && enquiry.getStatus() != EnquiryStatus.FEE_DISCUSSED) {
            throw new IllegalStateException(
                "Enquiry must be in INTERESTED or FEE_DISCUSSED status to convert. Current status: " + enquiry.getStatus()
            );
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        enquiry.setStatus(EnquiryStatus.CONVERTED);
        enquiry.setConvertedStudentId(student.getId());

        Enquiry saved = enquiryRepository.save(enquiry);
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
        enquiry.setStatus(status);
        Enquiry saved = enquiryRepository.save(enquiry);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!enquiryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + id);
        }
        enquiryRepository.deleteById(id);
    }

    private EnquiryResponse toResponse(Enquiry e) {
        return new EnquiryResponse(
            e.getId(),
            e.getName(),
            e.getEmail(),
            e.getPhone(),
            e.getProgram() != null ? e.getProgram().getId() : null,
            e.getProgram() != null ? e.getProgram().getName() : null,
            e.getEnquiryDate(),
            e.getSource(),
            e.getStatus(),
            e.getAgent() != null ? e.getAgent().getId() : null,
            e.getAgent() != null ? e.getAgent().getName() : null,
            e.getAssignedTo(),
            e.getRemarks(),
            e.getFeeDiscussedAmount(),
            e.getConvertedStudentId(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
