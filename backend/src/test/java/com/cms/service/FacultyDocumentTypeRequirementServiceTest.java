package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FacultyDocumentTypeRequirementRequest;
import com.cms.dto.FacultyDocumentTypeRequirementResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocumentTypeRequirement;
import com.cms.model.Speciality;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.FacultyQualification;
import com.cms.repository.DesignationRepository;
import com.cms.repository.FacultyDocumentTypeRequirementRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SpecialityRepository;

@ExtendWith(MockitoExtension.class)
class FacultyDocumentTypeRequirementServiceTest {

    @Mock private FacultyDocumentTypeRequirementRepository requirementRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private SpecialityRepository specialityRepository;
    @Mock private DesignationRepository designationRepository;

    private FacultyDocumentTypeRequirementService service;

    private DesignationMaster professor;

    @BeforeEach
    void setUp() {
        service = new FacultyDocumentTypeRequirementService(
            requirementRepository, facultyRepository, specialityRepository, designationRepository);
        professor = new DesignationMaster("Professor", "PROFESSOR", null);
        professor.setId(1L);
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    void findAllReturnsMappedList() {
        FacultyDocumentTypeRequirement rule = buildRule(1L, DocumentType.AADHAR_CARD, professor, null, null);
        when(requirementRepository.findAll()).thenReturn(List.of(rule));

        List<FacultyDocumentTypeRequirementResponse> results = service.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
        assertThat(results.get(0).documentType()).isEqualTo(DocumentType.AADHAR_CARD);
        assertThat(results.get(0).designationId()).isEqualTo(1L);
        assertThat(results.get(0).designationName()).isEqualTo("Professor");
    }

    @Test
    void findAllReturnsEmptyList() {
        when(requirementRepository.findAll()).thenReturn(List.of());

        List<FacultyDocumentTypeRequirementResponse> results = service.findAll();

        assertThat(results).isEmpty();
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Test
    void createWithDesignationCriterionPersistsRule() {
        FacultyDocumentTypeRequirementRequest request = new FacultyDocumentTypeRequirementRequest(
            DocumentType.AADHAR_CARD, 1L, null, null);

        FacultyDocumentTypeRequirement saved = buildRule(1L, DocumentType.AADHAR_CARD, professor, null, null);
        when(designationRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(requirementRepository.save(any(FacultyDocumentTypeRequirement.class))).thenReturn(saved);

        FacultyDocumentTypeRequirementResponse result = service.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.documentType()).isEqualTo(DocumentType.AADHAR_CARD);
        verify(requirementRepository).save(any(FacultyDocumentTypeRequirement.class));
    }

    @Test
    void createWithSpecialityCriterionLooksUpSpeciality() {
        Speciality speciality = new Speciality("Computer Science", "CS", null, null, null);
        speciality.setId(5L);

        FacultyDocumentTypeRequirementRequest request = new FacultyDocumentTypeRequirementRequest(
            DocumentType.TENTH_MARKSHEET, null, 5L, null);

        FacultyDocumentTypeRequirement saved = buildRule(2L, DocumentType.TENTH_MARKSHEET, null, speciality, null);
        when(specialityRepository.findById(5L)).thenReturn(Optional.of(speciality));
        when(requirementRepository.save(any(FacultyDocumentTypeRequirement.class))).thenReturn(saved);

        FacultyDocumentTypeRequirementResponse result = service.create(request);

        assertThat(result.specialityId()).isEqualTo(5L);
        assertThat(result.specialityName()).isEqualTo("Computer Science");
        verify(specialityRepository).findById(5L);
    }

    @Test
    void createWithSpecialityThrowsWhenSpecialityNotFound() {
        FacultyDocumentTypeRequirementRequest request = new FacultyDocumentTypeRequirementRequest(
            DocumentType.AADHAR_CARD, null, 99L, null);

        when(specialityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void createWithQualificationCriterion() {
        FacultyDocumentTypeRequirementRequest request = new FacultyDocumentTypeRequirementRequest(
            DocumentType.PAN_CARD, null, null, FacultyQualification.PHD);

        FacultyDocumentTypeRequirement saved = buildRule(3L, DocumentType.PAN_CARD, null, null, FacultyQualification.PHD);
        when(requirementRepository.save(any(FacultyDocumentTypeRequirement.class))).thenReturn(saved);

        FacultyDocumentTypeRequirementResponse result = service.create(request);

        assertThat(result.qualification()).isEqualTo(FacultyQualification.PHD);
    }

    @Test
    void createThrowsWhenNoCriterionProvided() {
        FacultyDocumentTypeRequirementRequest request = new FacultyDocumentTypeRequirementRequest(
            DocumentType.AADHAR_CARD, null, null, null);

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("criterion");
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    void deleteRemovesExistingRule() {
        when(requirementRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(requirementRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(requirementRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─── getRequiredDocumentTypesForFaculty ──────────────────────────────────

    @Test
    void getRequiredDocumentTypesForFacultyReturnsMatchingTypes() {
        Speciality speciality = new Speciality("Nursing", "GN", null, null, null);
        speciality.setId(1L);

        Faculty faculty = new Faculty();
        faculty.setDesignation(professor);
        faculty.setSpeciality(speciality);
        faculty.setHighestQualification(FacultyQualification.PHD);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(requirementRepository.findMatchingDocumentTypeNames(1L, 1L, "PHD"))
            .thenReturn(List.of("AADHAR_CARD", "PAN_CARD"));

        Set<String> result = service.getRequiredDocumentTypesForFaculty(1L);

        assertThat(result).containsExactlyInAnyOrder("AADHAR_CARD", "PAN_CARD");
    }

    @Test
    void getRequiredDocumentTypesHandlesFacultyWithNullAttributes() {
        Faculty faculty = new Faculty();
        faculty.setDesignation(null);
        faculty.setSpeciality(null);
        faculty.setHighestQualification(null);

        when(facultyRepository.findById(2L)).thenReturn(Optional.of(faculty));
        when(requirementRepository.findMatchingDocumentTypeNames(null, null, null))
            .thenReturn(List.of());

        Set<String> result = service.getRequiredDocumentTypesForFaculty(2L);

        assertThat(result).isEmpty();
    }

    @Test
    void getRequiredDocumentTypesThrowsWhenFacultyNotFound() {
        when(facultyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredDocumentTypesForFaculty(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private FacultyDocumentTypeRequirement buildRule(Long id, DocumentType docType,
                                                     DesignationMaster designation,
                                                     Speciality speciality,
                                                     FacultyQualification qualification) {
        FacultyDocumentTypeRequirement rule = new FacultyDocumentTypeRequirement();
        rule.setId(id);
        rule.setDocumentType(docType);
        rule.setDesignation(designation);
        rule.setSpeciality(speciality);
        rule.setQualification(qualification);
        return rule;
    }
}
