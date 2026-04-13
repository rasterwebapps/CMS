package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AdmissionRequest;
import com.cms.dto.AdmissionResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.Student;
import com.cms.model.enums.AdmissionStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private StudentRepository studentRepository;

    private AdmissionService admissionService;

    @BeforeEach
    void setUp() {
        admissionService = new AdmissionService(admissionRepository, studentRepository);
    }

    private Student createStudent(Long id) {
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            null, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(id);
        return student;
    }

    private Admission createAdmission(Long id, Student student) {
        Admission admission = new Admission(student, 2024, 2025, LocalDate.of(2024, 1, 15), AdmissionStatus.DRAFT);
        admission.setId(id);
        admission.setCreatedAt(Instant.now());
        admission.setUpdatedAt(Instant.now());
        return admission;
    }

    @Test
    void shouldCreateAdmission() {
        AdmissionRequest request = new AdmissionRequest(
            1L, 2024, 2025, LocalDate.of(2024, 1, 15), null, "Chennai", null, true, true
        );
        Student student = createStudent(1L);
        Admission saved = createAdmission(1L, student);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(admissionRepository.save(any(Admission.class))).thenReturn(saved);

        AdmissionResponse response = admissionService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(AdmissionStatus.DRAFT);

        ArgumentCaptor<Admission> captor = ArgumentCaptor.forClass(Admission.class);
        verify(admissionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AdmissionStatus.DRAFT);
    }

    @Test
    void shouldCreateAdmissionWithDefaultDraftStatusWhenStatusIsNull() {
        AdmissionRequest request = new AdmissionRequest(
            1L, 2024, 2025, LocalDate.of(2024, 1, 15), null, null, null, null, null
        );
        Student student = createStudent(1L);
        Admission saved = createAdmission(1L, student);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(admissionRepository.save(any(Admission.class))).thenReturn(saved);

        AdmissionResponse response = admissionService.create(request);
        assertThat(response).isNotNull();
        verify(admissionRepository).save(any(Admission.class));
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundOnCreate() {
        AdmissionRequest request = new AdmissionRequest(
            999L, 2024, 2025, LocalDate.of(2024, 1, 15), null, null, null, null, null
        );
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> admissionService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void shouldFindAllAdmissions() {
        Student student = createStudent(1L);
        Admission admission = createAdmission(1L, student);
        when(admissionRepository.findAll()).thenReturn(List.of(admission));

        List<AdmissionResponse> responses = admissionService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        verify(admissionRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoAdmissions() {
        when(admissionRepository.findAll()).thenReturn(List.of());
        List<AdmissionResponse> responses = admissionService.findAll();
        assertThat(responses).isEmpty();
    }

    @Test
    void shouldFindAdmissionById() {
        Student student = createStudent(1L);
        Admission admission = createAdmission(1L, student);
        when(admissionRepository.findById(1L)).thenReturn(Optional.of(admission));

        AdmissionResponse response = admissionService.findById(1L);
        assertThat(response.id()).isEqualTo(1L);
        verify(admissionRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenAdmissionNotFoundById() {
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission not found with id: 999");
    }

    @Test
    void shouldFindAdmissionByStudentId() {
        Student student = createStudent(1L);
        Admission admission = createAdmission(1L, student);
        when(admissionRepository.findByStudentId(1L)).thenReturn(Optional.of(admission));

        AdmissionResponse response = admissionService.findByStudentId(1L);
        assertThat(response.studentId()).isEqualTo(1L);
        verify(admissionRepository).findByStudentId(1L);
    }

    @Test
    void shouldThrowExceptionWhenAdmissionNotFoundByStudentId() {
        when(admissionRepository.findByStudentId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionService.findByStudentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission not found for student id: 999");
    }

    @Test
    void shouldUpdateAdmission() {
        Student student = createStudent(1L);
        Admission existing = createAdmission(1L, student);
        AdmissionRequest request = new AdmissionRequest(
            1L, 2024, 2025, LocalDate.of(2024, 1, 15), AdmissionStatus.SUBMITTED, null, null, null, null
        );
        Admission updated = createAdmission(1L, student);
        updated.setStatus(AdmissionStatus.SUBMITTED);

        when(admissionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(admissionRepository.save(any(Admission.class))).thenReturn(updated);

        AdmissionResponse response = admissionService.update(1L, request);
        assertThat(response.id()).isEqualTo(1L);
        verify(admissionRepository).findById(1L);
        verify(admissionRepository).save(any(Admission.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentAdmission() {
        AdmissionRequest request = new AdmissionRequest(
            1L, 2024, 2025, LocalDate.of(2024, 1, 15), null, null, null, null, null
        );
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void shouldUpdateAdmissionStatus() {
        Student student = createStudent(1L);
        Admission existing = createAdmission(1L, student);
        Admission updated = createAdmission(1L, student);
        updated.setStatus(AdmissionStatus.APPROVED);

        when(admissionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(admissionRepository.save(any(Admission.class))).thenReturn(updated);

        AdmissionResponse response = admissionService.updateStatus(1L, AdmissionStatus.APPROVED);
        assertThat(response).isNotNull();
        verify(admissionRepository).save(any(Admission.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingStatusOfNonExistentAdmission() {
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admissionService.updateStatus(999L, AdmissionStatus.APPROVED))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteAdmission() {
        when(admissionRepository.existsById(1L)).thenReturn(true);
        admissionService.delete(1L);
        verify(admissionRepository).existsById(1L);
        verify(admissionRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAdmission() {
        when(admissionRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> admissionService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Admission not found with id: 999");
        verify(admissionRepository, never()).deleteById(any());
    }
}
