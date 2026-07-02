package com.cms.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ProgramDocumentRequirementsRequest;
import com.cms.dto.ProgramDocumentRequirementsResponse;
import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.dto.ProgramStatusUpdateRequest;
import com.cms.dto.ProgramStatusUpdateResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Program;
import com.cms.model.ProgramDocumentRequirement;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.ProgramDocumentCategory;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.CourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.IntakeRuleRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final FeeStructureGroupRepository feeStructureGroupRepository;
    private final IntakeRuleRepository intakeRuleRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;

    public ProgramService(ProgramRepository programRepository,
                          CourseRepository courseRepository,
                          FeeStructureGroupRepository feeStructureGroupRepository,
                          IntakeRuleRepository intakeRuleRepository,
                          CurriculumVersionRepository curriculumVersionRepository) {
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.feeStructureGroupRepository = feeStructureGroupRepository;
        this.intakeRuleRepository = intakeRuleRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
    }

    @Transactional
    public ProgramResponse create(ProgramRequest request) {
        String name = requireTrimmed(request.name(), "Program name is required");
        String code = requireTrimmed(request.code(), "Program code is required");
        validateCode(code);
        validateAgeCutoffDate(request.ageCutoffDay(), request.ageCutoffMonth());

        if (programRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A program with the name '" + name + "' already exists");
        }
        if (programRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A program with the code '" + code + "' already exists");
        }

        Program program = new Program(
            name,
            code,
            request.durationYears(),
            request.status(),
            request.assessmentPattern()
        );
        program.setMinimumAgeYears(request.minimumAgeYears());
        program.setAgeCutoffDay(request.ageCutoffDay());
        program.setAgeCutoffMonth(request.ageCutoffMonth());
        return toResponse(programRepository.save(program));
    }

    public List<ProgramResponse> findAll() {
        return programRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<ProgramResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return programRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<Program> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return programRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ProgramResponse findById(Long id) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));
        return toResponse(program);
    }

    @Transactional
    public ProgramResponse update(Long id, ProgramRequest request) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));
        String name = requireTrimmed(request.name(), "Program name is required");
        String code = requireTrimmed(request.code(), "Program code is required");

        validateCode(code);
        validateAgeCutoffDate(request.ageCutoffDay(), request.ageCutoffMonth());

        if (programRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A program with the name '" + name + "' already exists");
        }
        if (programRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A program with the code '" + code + "' already exists");
        }

        program.setName(name);
        program.setCode(code);
        program.setDurationYears(request.durationYears());
        if (request.status() != null) {
            if (isProgramDeactivation(program.getStatus(), request.status())) {
                ensureCanDeactivateProgram(program.getId());
            }
            program.setStatus(request.status());
        }
        if (request.assessmentPattern() != null) {
            program.setAssessmentPattern(request.assessmentPattern());
        }
        program.setMinimumAgeYears(request.minimumAgeYears());
        program.setAgeCutoffDay(request.ageCutoffDay());
        program.setAgeCutoffMonth(request.ageCutoffMonth());
        return toResponse(programRepository.save(program));
    }

    @Transactional
    public void delete(Long id) {
        if (!programRepository.existsById(id)) {
            throw new ResourceNotFoundException("Program not found with id: " + id);
        }
        if (feeStructureGroupRepository.existsByProgramId(id)) {
            throw new IllegalStateException(
                "Cannot delete program because fee structures are associated with it.");
        }
        programRepository.deleteById(id);
    }

    @Transactional
    public ProgramStatusUpdateResponse updateStatus(Long id, ProgramStatusUpdateRequest request) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));

        ProgramStatus requestedStatus = request.status();
        if (isProgramDeactivation(program.getStatus(), requestedStatus)) {
            ensureCanDeactivateProgram(program.getId());
        }

        program.setStatus(requestedStatus);
        Program saved = programRepository.save(program);
        return new ProgramStatusUpdateResponse(saved.getId(), saved.getStatus(), saved.getUpdatedAt());
    }

    public ProgramResponse toResponse(Program program) {
        Set<String> mandatory = program.getMandatoryDocumentTypes().stream()
            .map(Enum::name).collect(Collectors.toSet());
        Set<String> optional = program.getOptionalDocumentTypes().stream()
            .map(Enum::name).collect(Collectors.toSet());
        return new ProgramResponse(
            program.getId(),
            program.getName(),
            program.getCode(),
            program.getDurationYears(),
            program.getTotalTerms(),
            program.getStatus(),
            program.getAssessmentPattern(),
            mandatory,
            optional,
            program.getMinimumAgeYears(),
            program.getAgeCutoffDay(),
            program.getAgeCutoffMonth(),
            program.getCreatedAt(),
            program.getUpdatedAt()
        );
    }

    public ProgramDocumentRequirementsResponse getDocumentRequirements(Long programId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + programId));
        Set<String> mandatory = program.getMandatoryDocumentTypes().stream()
            .map(Enum::name).collect(Collectors.toSet());
        Set<String> optional = program.getOptionalDocumentTypes().stream()
            .map(Enum::name).collect(Collectors.toSet());
        return new ProgramDocumentRequirementsResponse(mandatory, optional);
    }

    @Transactional
    public ProgramDocumentRequirementsResponse setDocumentRequirements(
            Long programId, ProgramDocumentRequirementsRequest request) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + programId));

        Set<ProgramDocumentRequirement> requirements = new HashSet<>();
        if (request.mandatory() != null) {
            for (String code : request.mandatory()) {
                requirements.add(new ProgramDocumentRequirement(
                    parseDocumentType(code), ProgramDocumentCategory.MANDATORY));
            }
        }
        if (request.optional() != null) {
            for (String code : request.optional()) {
                DocumentType type = parseDocumentType(code);
                // A type cannot be in both categories; MANDATORY wins if already added.
                boolean alreadyMandatory = requirements.stream()
                    .anyMatch(r -> r.getDocumentType() == type
                        && r.getCategory() == ProgramDocumentCategory.MANDATORY);
                if (!alreadyMandatory) {
                    requirements.add(new ProgramDocumentRequirement(type, ProgramDocumentCategory.OPTIONAL));
                }
            }
        }

        program.setDocumentRequirements(requirements);
        Program saved = programRepository.save(program);
        Set<String> savedMandatory = saved.getMandatoryDocumentTypes().stream()
            .map(Enum::name).collect(Collectors.toSet());
        Set<String> savedOptional = saved.getOptionalDocumentTypes().stream()
            .map(Enum::name).collect(Collectors.toSet());
        return new ProgramDocumentRequirementsResponse(savedMandatory, savedOptional);
    }

    private DocumentType parseDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Document type code must not be blank");
        }
        try {
            return DocumentType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown document type: " + raw);
        }
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return programRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return programRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return programRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return programRepository.existsByCodeIgnoreCase(trimmed);
    }

    private void validateCode(String code) {
        if (code == null) return;
        if (!code.equals(code.toUpperCase())) {
            throw new IllegalArgumentException("Program code must be uppercase");
        }
        if (code.contains(" ")) {
            throw new IllegalArgumentException("Program code must not contain spaces");
        }
    }

    private void validateAgeCutoffDate(Integer day, Integer month) {
        if (day == null || month == null) return;
        try {
            LocalDate.of(2000, month, day);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid age cutoff date: day " + day + " does not exist in month " + month);
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }

    private boolean isProgramDeactivation(ProgramStatus currentStatus, ProgramStatus requestedStatus) {
        return currentStatus == ProgramStatus.ACTIVE && requestedStatus == ProgramStatus.INACTIVE;
    }

    private void ensureCanDeactivateProgram(Long programId) {
        if (courseRepository.existsByProgramIdAndIsActiveTrue(programId)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Program: active Courses exist.",
                "ACTIVE_CHILD_EXISTS",
                "Program",
                programId,
                null
            );
        }
        if (intakeRuleRepository.existsByProgramIdAndIsActiveTrue(programId)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Program: active Intake Rules exist.",
                "ACTIVE_REFERENCE_EXISTS",
                "Program",
                programId,
                null
            );
        }
        if (curriculumVersionRepository.existsByProgramIdAndIsActiveTrue(programId)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Program: active Curriculum Versions exist.",
                "ACTIVE_REFERENCE_EXISTS",
                "Program",
                programId,
                null
            );
        }
        if (feeStructureGroupRepository.existsByProgramIdAndIsActiveTrue(programId)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Program: active Fee Structures exist.",
                "ACTIVE_REFERENCE_EXISTS",
                "Program",
                programId,
                null
            );
        }
    }
}
