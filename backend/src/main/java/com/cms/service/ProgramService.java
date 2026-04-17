package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Program;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;

    public ProgramService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @Transactional
    public ProgramResponse create(ProgramRequest request) {
        Program program = new Program(
            request.name(),
            request.code(),
            request.durationYears()
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

        program.setName(request.name());
        program.setCode(request.code());
        program.setDurationYears(request.durationYears());

        return toResponse(programRepository.save(program));
    }

    @Transactional
    public void delete(Long id) {
        if (!programRepository.existsById(id)) {
            throw new ResourceNotFoundException("Program not found with id: " + id);
        }
        programRepository.deleteById(id);
    }

    public ProgramResponse toResponse(Program program) {
        return new ProgramResponse(
            program.getId(),
            program.getName(),
            program.getCode(),
            program.getDurationYears(),
            program.getCreatedAt(),
            program.getUpdatedAt()
        );
    }
}

