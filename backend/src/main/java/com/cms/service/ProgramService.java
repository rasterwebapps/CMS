package com.cms.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DepartmentResponse;
import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;

    public ProgramService(ProgramRepository programRepository, DepartmentRepository departmentRepository) {
        this.programRepository = programRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public ProgramResponse create(ProgramRequest request) {
        Program program = new Program(
            request.name(),
            request.code(),
            request.programLevel(),
            request.durationYears()
        );

        if (request.departmentIds() != null && !request.departmentIds().isEmpty()) {
            Set<Department> departments = new HashSet<>(departmentRepository.findAllById(request.departmentIds()));
            if (departments.size() != request.departmentIds().size()) {
                throw new ResourceNotFoundException("One or more departments not found");
            }
            program.setDepartments(departments);
        }

        Program saved = programRepository.save(program);
        return toResponse(saved);
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
        program.setProgramLevel(request.programLevel());
        program.setDurationYears(request.durationYears());

        if (request.departmentIds() != null) {
            Set<Department> departments = new HashSet<>(departmentRepository.findAllById(request.departmentIds()));
            if (departments.size() != request.departmentIds().size()) {
                throw new ResourceNotFoundException("One or more departments not found");
            }
            program.setDepartments(departments);
        } else {
            program.setDepartments(new HashSet<>());
        }

        Program updated = programRepository.save(program);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!programRepository.existsById(id)) {
            throw new ResourceNotFoundException("Program not found with id: " + id);
        }
        programRepository.deleteById(id);
    }

    private ProgramResponse toResponse(Program program) {
        List<DepartmentResponse> departmentResponses = program.getDepartments().stream()
            .map(dept -> new DepartmentResponse(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getDescription(),
                dept.getHodName(),
                dept.getCreatedAt(),
                dept.getUpdatedAt()
            ))
            .toList();

        return new ProgramResponse(
            program.getId(),
            program.getName(),
            program.getCode(),
            program.getProgramLevel(),
            program.getDurationYears(),
            departmentResponses,
            program.getCreatedAt(),
            program.getUpdatedAt()
        );
    }
}
