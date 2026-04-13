package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AcademicQualificationRequest;
import com.cms.dto.AcademicQualificationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicQualification;
import com.cms.model.Admission;
import com.cms.model.Student;
import com.cms.model.enums.AdmissionStatus;
import com.cms.model.enums.QualificationType;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AcademicQualificationRepository;
import com.cms.repository.AdmissionRepository;

@ExtendWith(MockitoExtension.class)
class AcademicQualificationServiceTest {

    @Mock
    private AcademicQualificationRepository academicQualificationRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    private AcademicQualificationService academicQualificationService;

    @BeforeEach
    void setUp() {
        academicQualificationService = new AcademicQualificationService(
            academicQualificationRepository, admissionRepository);
    }

    private Student createStudent() {
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            null, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        return student;
    }

    private Admission createAdmission(Long id) {
        Admission admission = new Admission(createStudent(), 2024, 2025,
            LocalDate.of(2024, 1, 15), AdmissionStatus.DRAFT);
        admission.setId(id);
        admission.setCreatedAt(Instant.now());
        admission.setUpdatedAt(Instant.now());
        return admission;
    }

    private AcademicQualification createQualification(Long id, Admission admission) {
        AcademicQualification q = new AcademicQualification(
            admission, QualificationType.HSC, "ABC School", "Science", 500,
            new BigDecimal("85.00"), "May 2022", "State Board");
        q.setId(id);
        q.setCreatedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        return q;
    }

    @Test
    void shouldAddQualification() {
        AcademicQualificationRequest request = new AcademicQualificationRequest(
            QualificationType.HSC, "ABC School", "Science", 500,
            new BigDecimal("85.00"), "May 2022", "State Board"
        );
        Admission admission = createAdmission(1L);
        AcademicQualification saved = createQualification(1L, admission);

        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));
        when(academicQualificationRepository.save(any(AcademicQualification.class))).thenReturn(saved);

        AcademicQualificationResponse response = academicQualificationService.addQualification(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.qualificationType()).isEqualTo(QualificationType.HSC);
        assertThat(response.schoolName()).isEqualTo("ABC School");
        verify(admissionRepository).findById(1L);
        verify(academicQualificationRepository).save(any(AcademicQualification.class));
    }

    @Test
    void shouldThrowExceptionWhenAdmissionNotFoundOnAddQualification() {
        AcademicQualificationRequest request = new AcademicQualificationRequest(
            QualificationType.HSC, "ABC School", null, null, null, null, null
        );
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicQualificationService.addQualification(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission not found with id: 999");
        verify(academicQualificationRepository, never()).save(any());
    }

    @Test
    void shouldFindQualificationsByAdmissionId() {
        Admission admission = createAdmission(1L);
        AcademicQualification q = createQualification(1L, admission);
        when(academicQualificationRepository.findByAdmissionId(1L)).thenReturn(List.of(q));

        List<AcademicQualificationResponse> responses = academicQualificationService.findByAdmissionId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).qualificationType()).isEqualTo(QualificationType.HSC);
        verify(academicQualificationRepository).findByAdmissionId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoQualifications() {
        when(academicQualificationRepository.findByAdmissionId(1L)).thenReturn(List.of());
        List<AcademicQualificationResponse> responses = academicQualificationService.findByAdmissionId(1L);
        assertThat(responses).isEmpty();
    }

    @Test
    void shouldDeleteQualification() {
        when(academicQualificationRepository.existsById(1L)).thenReturn(true);
        academicQualificationService.delete(1L);
        verify(academicQualificationRepository).existsById(1L);
        verify(academicQualificationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentQualification() {
        when(academicQualificationRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> academicQualificationService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic qualification not found with id: 999");
        verify(academicQualificationRepository, never()).deleteById(any());
    }
}
