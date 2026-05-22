package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DepartmentRequest;
import com.cms.dto.DepartmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Faculty;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.FacultyRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                              FacultyRepository facultyRepository) {
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = requireTrimmed(request.name(), "Department name is required");
        String code = requireTrimmed(request.code(), "Department code is required");

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A department with the name '" + name + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A department with the code '" + code + "' already exists");
        }

        Department department = new Department(name, code, trim(request.description()), null, null);
        applyHod(department, request.hodFacultyId());
        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public DepartmentResponse findById(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        String name = requireTrimmed(request.name(), "Department name is required");
        String code = requireTrimmed(request.code(), "Department code is required");

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A department with the name '" + name + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A department with the code '" + code + "' already exists");
        }

        department.setName(name);
        department.setCode(code);
        department.setDescription(trim(request.description()));
        applyHod(department, request.hodFacultyId());

        Department updated = departmentRepository.save(department);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return departmentRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return departmentRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return departmentRepository.existsByCodeIgnoreCase(trimmed);
    }

    private void applyHod(Department department, Long hodFacultyId) {
        if (hodFacultyId == null) {
            department.setHodFacultyId(null);
            department.setHodName(null);
        } else {
            Faculty faculty = facultyRepository.findById(hodFacultyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Faculty not found with id: " + hodFacultyId));
            department.setHodFacultyId(hodFacultyId);
            department.setHodName(faculty.getFullName());
        }
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getCode(),
            department.getDescription(),
            department.getHodFacultyId(),
            department.getHodName(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );
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
}
