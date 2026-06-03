package com.cms.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FacultyDocumentTypeRequirementRequest;
import com.cms.dto.FacultyDocumentTypeRequirementResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Speciality;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocumentTypeRequirement;
import com.cms.model.enums.DocumentType;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.FacultyDocumentTypeRequirementRepository;
import com.cms.repository.FacultyRepository;

@Service
@Transactional(readOnly = true)
public class FacultyDocumentTypeRequirementService {

    private final FacultyDocumentTypeRequirementRepository requirementRepository;
    private final FacultyRepository facultyRepository;
    private final SpecialityRepository specialityRepository;

    public FacultyDocumentTypeRequirementService(
            FacultyDocumentTypeRequirementRepository requirementRepository,
            FacultyRepository facultyRepository,
            SpecialityRepository specialityRepository) {
        this.requirementRepository = requirementRepository;
        this.facultyRepository = facultyRepository;
        this.specialityRepository = specialityRepository;
    }

    public List<FacultyDocumentTypeRequirementResponse> findAll() {
        return requirementRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FacultyDocumentTypeRequirementResponse create(FacultyDocumentTypeRequirementRequest request) {
        if (request.designation() == null && request.specialityId() == null && request.qualification() == null) {
            throw new IllegalArgumentException(
                    "At least one criterion (designation, speciality, or qualification) must be specified");
        }

        Speciality speciality = null;
        if (request.specialityId() != null) {
            speciality = specialityRepository.findById(request.specialityId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Speciality not found with id: " + request.specialityId()));
        }

        FacultyDocumentTypeRequirement rule = new FacultyDocumentTypeRequirement();
        rule.setDocumentType(request.documentType());
        rule.setDesignation(request.designation());
        rule.setSpeciality(speciality);
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
        Long specialityId = faculty.getSpeciality() != null ? faculty.getSpeciality().getId() : null;
        String qualification = faculty.getHighestQualification() != null
                ? faculty.getHighestQualification().name() : null;

        return requirementRepository
                .findMatchingDocumentTypeNames(designation, specialityId, qualification)
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
                rule.getSpeciality() != null ? rule.getSpeciality().getId() : null,
                rule.getSpeciality() != null ? rule.getSpeciality().getName() : null,
                rule.getQualification(),
                rule.getQualification() != null ? rule.getQualification().getDisplayName() : null,
                rule.getCreatedAt()
        );
    }
}
