package com.cms.service;

import java.util.List;

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
        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));

        Program program = new Program(
            request.name(),
            request.code(),
            request.degreeType(),
            request.durationYears(),
            department
        );
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

    public List<ProgramResponse> findByDepartmentId(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return programRepository.findByDepartmentId(departmentId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ProgramResponse update(Long id, ProgramRequest request) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + id));

        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));

        program.setName(request.name());
        program.setCode(request.code());
        program.setDegreeType(request.degreeType());
        program.setDurationYears(request.durationYears());
        program.setDepartment(department);

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
        Department department = program.getDepartment();
        DepartmentResponse departmentResponse = new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getCode(),
            department.getDescription(),
            department.getHodName(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );

        return new ProgramResponse(
            program.getId(),
            program.getName(),
            program.getCode(),
            program.getDegreeType(),
            program.getDurationYears(),
            departmentResponse,
            program.getCreatedAt(),
            program.getUpdatedAt()
        );
    }
}
