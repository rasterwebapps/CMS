package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SpecialityRequest;
import com.cms.dto.SpecialityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Speciality;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.FacultyRepository;

@ExtendWith(MockitoExtension.class)
class SpecialityServiceTest {

    @Mock
    private SpecialityRepository specialityRepository;

    @Mock
    private FacultyRepository facultyRepository;

    private SpecialityService specialityService;

    @BeforeEach
    void setUp() {
        specialityService = new SpecialityService(specialityRepository, facultyRepository);
    }

    @Test
    void shouldCreateSpeciality() {
        SpecialityRequest request = new SpecialityRequest(
            "Computer Science",
            "CS",
            "Speciality of Computer Science",
            null
        );

        Speciality savedSpeciality = createSpeciality(1L, "Computer Science", "CS",
            "Speciality of Computer Science", null);

        when(specialityRepository.save(any(Speciality.class))).thenReturn(savedSpeciality);

        SpecialityResponse response = specialityService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.description()).isEqualTo("Speciality of Computer Science");
        assertThat(response.hodFacultyId()).isNull();
        assertThat(response.hodName()).isNull();

        ArgumentCaptor<Speciality> captor = ArgumentCaptor.forClass(Speciality.class);
        verify(specialityRepository).save(captor.capture());
        Speciality captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Computer Science");
        assertThat(captured.getCode()).isEqualTo("CS");
    }

    @Test
    void shouldFindAllSpecialities() {
        Speciality dept1 = createSpeciality(1L, "Computer Science", "CS", "CS Dept", null);
        Speciality dept2 = createSpeciality(2L, "Mathematics", "MATH", "Math Dept", null);

        when(specialityRepository.findAll()).thenReturn(List.of(dept1, dept2));

        List<SpecialityResponse> responses = specialityService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Computer Science");
        assertThat(responses.get(1).name()).isEqualTo("Mathematics");
        verify(specialityRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoSpecialities() {
        when(specialityRepository.findAll()).thenReturn(List.of());

        List<SpecialityResponse> responses = specialityService.findAll();

        assertThat(responses).isEmpty();
        verify(specialityRepository).findAll();
    }

    @Test
    void shouldFindSpecialityById() {
        Speciality speciality = createSpeciality(1L, "Computer Science", "CS",
            "Speciality of Computer Science", null);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));

        SpecialityResponse response = specialityService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.code()).isEqualTo("CS");
        verify(specialityRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSpecialityNotFoundById() {
        when(specialityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialityService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");

        verify(specialityRepository).findById(999L);
    }

    @Test
    void shouldUpdateSpeciality() {
        Speciality existingSpeciality = createSpeciality(1L, "Computer Science", "CS",
            "Old Description", null);

        SpecialityRequest updateRequest = new SpecialityRequest(
            "Computer Science Updated",
            "CSU",
            "New Description",
            null
        );

        Speciality updatedSpeciality = createSpeciality(1L, "Computer Science Updated", "CSU",
            "New Description", null);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(existingSpeciality));
        when(specialityRepository.existsByNameIgnoreCaseAndIdNot("Computer Science Updated", 1L)).thenReturn(false);
        when(specialityRepository.existsByCodeIgnoreCaseAndIdNot("CSU", 1L)).thenReturn(false);
        when(specialityRepository.save(any(Speciality.class))).thenReturn(updatedSpeciality);

        SpecialityResponse response = specialityService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science Updated");
        assertThat(response.code()).isEqualTo("CSU");
        assertThat(response.description()).isEqualTo("New Description");
        assertThat(response.hodFacultyId()).isNull();

        verify(specialityRepository).findById(1L);
        verify(specialityRepository).save(any(Speciality.class));
    }

    @Test
    void shouldThrowWhenUpdatingSpecialityWithDuplicateName() {
        Speciality existing = createSpeciality(1L, "Computer Science", "CS", "Desc", null);
        SpecialityRequest request = new SpecialityRequest("Mathematics", "CS", "Desc", null);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(specialityRepository.existsByNameIgnoreCaseAndIdNot("Mathematics", 1L)).thenReturn(true);

        assertThatThrownBy(() -> specialityService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mathematics")
            .hasMessageContaining("already exists");

        verify(specialityRepository, never()).save(any(Speciality.class));
    }

    @Test
    void shouldThrowWhenUpdatingSpecialityWithDuplicateCode() {
        Speciality existing = createSpeciality(1L, "Computer Science", "CS", "Desc", null);
        SpecialityRequest request = new SpecialityRequest("Computer Science", "MATH", "Desc", null);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(specialityRepository.existsByNameIgnoreCaseAndIdNot("Computer Science", 1L)).thenReturn(false);
        when(specialityRepository.existsByCodeIgnoreCaseAndIdNot("MATH", 1L)).thenReturn(true);

        assertThatThrownBy(() -> specialityService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MATH")
            .hasMessageContaining("already exists");

        verify(specialityRepository, never()).save(any(Speciality.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentSpeciality() {
        SpecialityRequest request = new SpecialityRequest("Name", "CODE", "Desc", null);

        when(specialityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialityService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");

        verify(specialityRepository).findById(999L);
        verify(specialityRepository, never()).save(any(Speciality.class));
    }

    @Test
    void shouldDeleteSpeciality() {
        when(specialityRepository.existsById(1L)).thenReturn(true);

        specialityService.delete(1L);

        verify(specialityRepository).existsById(1L);
        verify(specialityRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentSpeciality() {
        when(specialityRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> specialityService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");

        verify(specialityRepository).existsById(999L);
        verify(specialityRepository, never()).deleteById(any());
    }

    private Speciality createSpeciality(Long id, String name, String code,
                                        String description, String hodName) {
        Speciality speciality = new Speciality(name, code, description, null, hodName);
        speciality.setId(id);
        Instant now = Instant.now();
        speciality.setCreatedAt(now);
        speciality.setUpdatedAt(now);
        return speciality;
    }
}
