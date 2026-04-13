package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DepartmentRequest;
import com.cms.dto.DepartmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.repository.DepartmentRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department department = new Department(
            request.name(),
            request.code(),
            request.description(),
            request.hodName()
        );
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

        department.setName(request.name());
        department.setCode(request.code());
        department.setDescription(request.description());
        department.setHodName(request.hodName());

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

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getCode(),
            department.getDescription(),
            department.getHodName(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );
    }
}
