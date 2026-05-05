package com.cms.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Program;
import com.cms.model.enums.DocumentType;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final FeeStructureRepository feeStructureRepository;

    public ProgramService(ProgramRepository programRepository,
                          FeeStructureRepository feeStructureRepository) {
        this.programRepository = programRepository;
        this.feeStructureRepository = feeStructureRepository;
    }

    @Transactional
    public ProgramResponse create(ProgramRequest request) {
        validateCode(request.code());
        Program program = new Program(
            request.name(),
            request.code(),
            request.durationYears(),
            request.status()
        );
        return toResponse(programRepository.save(program));
    }

    public List<ProgramResponse> findAll() {
        return programRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
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

        validateCode(request.code());

        if (programRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "A program with the name '" + request.name() + "' already exists");
        }
        if (programRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new IllegalArgumentException(
                "A program with the code '" + request.code() + "' already exists");
        }

        program.setName(request.name());
        program.setCode(request.code());
        program.setDurationYears(request.durationYears());
        if (request.status() != null) {
            program.setStatus(request.status());
        }

        return toResponse(programRepository.save(program));
    }

    @Transactional
    public void delete(Long id) {
        if (!programRepository.existsById(id)) {
            throw new ResourceNotFoundException("Program not found with id: " + id);
        }
        if (feeStructureRepository.existsByProgramId(id)) {
            throw new IllegalStateException(
                "Cannot delete program because fee structures are associated with it.");
        }
        programRepository.deleteById(id);
    }

    public ProgramResponse toResponse(Program program) {
        return new ProgramResponse(
            program.getId(),
            program.getName(),
            program.getCode(),
            program.getDurationYears(),
            program.getTotalSemesters(),
            program.getStatus(),
            new HashSet<>(program.getRequiredDocumentTypes()),
            program.getCreatedAt(),
            program.getUpdatedAt()
        );
    }

    public Set<DocumentType> getRequiredDocumentTypes(Long programId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + programId));
        return new HashSet<>(program.getRequiredDocumentTypes());
    }

    @Transactional
    public Set<DocumentType> setRequiredDocumentTypes(Long programId, Set<DocumentType> types) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + programId));
        Set<DocumentType> sanitized = types != null ? new HashSet<>(types) : new HashSet<>();
        program.setRequiredDocumentTypes(sanitized);
        Program saved = programRepository.save(program);
        return new HashSet<>(saved.getRequiredDocumentTypes());
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
}
