package com.cms.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FacultyDocumentTypeRequirementRequest;
import com.cms.dto.FacultyDocumentTypeRequirementResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocumentTypeRequirement;
import com.cms.model.enums.DocumentType;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.FacultyDocumentTypeRequirementRepository;
import com.cms.repository.FacultyRepository;

@Service
@Transactional(readOnly = true)
public class FacultyDocumentTypeRequirementService {

    private final FacultyDocumentTypeRequirementRepository requirementRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;

    public FacultyDocumentTypeRequirementService(
            FacultyDocumentTypeRequirementRepository requirementRepository,
            FacultyRepository facultyRepository,
            DepartmentRepository departmentRepository) {
        this.requirementRepository = requirementRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<FacultyDocumentTypeRequirementResponse> findAll() {
        return requirementRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FacultyDocumentTypeRequirementResponse create(FacultyDocumentTypeRequirementRequest request) {
        if (request.designation() == null && request.departmentId() == null && request.qualification() == null) {
            throw new IllegalArgumentException(
                    "At least one criterion (designation, department, or qualification) must be specified");
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.departmentId()));
        }

        FacultyDocumentTypeRequirement rule = new FacultyDocumentTypeRequirement();
        rule.setDocumentType(request.documentType());
        rule.setDesignation(request.designation());
        rule.setDepartment(department);
        rule.setQualification(request.qualification());

        return toResponse(requirementRepository.save(rule));
    }

    @Transactional
    public void delete(Long id) {
        if (!requirementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Requirement rule not found with id: " + id);
        }
        requirementRepository.deleteById(id);
    }

    public Set<String> getRequiredDocumentTypesForFaculty(Long facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        String designation = faculty.getDesignation() != null ? faculty.getDesignation().name() : null;
        Long departmentId = faculty.getDepartment() != null ? faculty.getDepartment().getId() : null;
        String qualification = faculty.getHighestQualification() != null
                ? faculty.getHighestQualification().name() : null;

        return requirementRepository
                .findMatchingDocumentTypeNames(designation, departmentId, qualification)
                .stream()
                .collect(Collectors.toSet());
    }

    private FacultyDocumentTypeRequirementResponse toResponse(FacultyDocumentTypeRequirement rule) {
        DocumentType dt = rule.getDocumentType();
        return new FacultyDocumentTypeRequirementResponse(
                rule.getId(),
                dt,
                dt != null ? dt.getDisplayName() : null,
                rule.getDesignation(),
                rule.getDepartment() != null ? rule.getDepartment().getId() : null,
                rule.getDepartment() != null ? rule.getDepartment().getName() : null,
                rule.getQualification(),
                rule.getQualification() != null ? rule.getQualification().getDisplayName() : null,
                rule.getCreatedAt()
        );
    }
}
