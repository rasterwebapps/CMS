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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.BloodGroupRequest;
import com.cms.dto.BloodGroupResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.BloodGroupMaster;
import com.cms.repository.BloodGroupRepository;

@ExtendWith(MockitoExtension.class)
class BloodGroupServiceTest {

    @Mock
    private BloodGroupRepository bloodGroupRepository;

    private BloodGroupService bloodGroupService;

    @BeforeEach
    void setUp() {
        bloodGroupService = new BloodGroupService(bloodGroupRepository);
    }

    @Test
    void shouldCreateBloodGroup() {
        BloodGroupRequest request = new BloodGroupRequest("A Positive", "A+", true);
        BloodGroupMaster saved = createBloodGroup(1L, "A Positive", "A+");

        when(bloodGroupRepository.existsByCode("A+")).thenReturn(false);
        when(bloodGroupRepository.save(any(BloodGroupMaster.class))).thenReturn(saved);

        BloodGroupResponse response = bloodGroupService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("A Positive");
        assertThat(response.code()).isEqualTo("A+");
        assertThat(response.isActive()).isTrue();
        verify(bloodGroupRepository).save(any(BloodGroupMaster.class));
    }

    @Test
    void shouldThrowWhenDuplicateCode() {
        BloodGroupRequest request = new BloodGroupRequest("A Positive", "A+", true);

        when(bloodGroupRepository.existsByCode("A+")).thenReturn(true);

        assertThatThrownBy(() -> bloodGroupService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(bloodGroupRepository, never()).save(any());
    }

    @Test
    void shouldCreateWithDefaultActiveWhenIsActiveNull() {
        BloodGroupRequest request = new BloodGroupRequest("O Positive", "O+", null);
        BloodGroupMaster saved = createBloodGroup(1L, "O Positive", "O+");

        when(bloodGroupRepository.existsByCode("O+")).thenReturn(false);
        when(bloodGroupRepository.save(any(BloodGroupMaster.class))).thenReturn(saved);

        BloodGroupResponse response = bloodGroupService.create(request);

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void shouldFindAll() {
        BloodGroupMaster bg = createBloodGroup(1L, "A Positive", "A+");
        when(bloodGroupRepository.findAllByOrderByNameAsc()).thenReturn(List.of(bg));

        List<BloodGroupResponse> responses = bloodGroupService.findAll();

        assertThat(responses).hasSize(1);
        verify(bloodGroupRepository).findAllByOrderByNameAsc();
    }

    @Test
    void shouldFindActive() {
        BloodGroupMaster bg = createBloodGroup(1L, "A Positive", "A+");
        when(bloodGroupRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(bg));

        List<BloodGroupResponse> responses = bloodGroupService.findActive();

        assertThat(responses).hasSize(1);
        verify(bloodGroupRepository).findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void shouldFindById() {
        BloodGroupMaster bg = createBloodGroup(1L, "A Positive", "A+");
        when(bloodGroupRepository.findById(1L)).thenReturn(Optional.of(bg));

        BloodGroupResponse response = bloodGroupService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("A Positive");
        assertThat(response.code()).isEqualTo("A+");
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(bloodGroupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloodGroupService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Blood group not found with id: 999");
    }

    @Test
    void shouldUpdate() {
        BloodGroupMaster existing = createBloodGroup(1L, "A Positive", "A+");
        BloodGroupRequest request = new BloodGroupRequest("A Positive Blood", "A+", true);
        BloodGroupMaster updated = createBloodGroup(1L, "A Positive Blood", "A+");

        when(bloodGroupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bloodGroupRepository.existsByCodeAndIdNot("A+", 1L)).thenReturn(false);
        when(bloodGroupRepository.existsByNameAndIdNot("A Positive Blood", 1L)).thenReturn(false);
        when(bloodGroupRepository.save(any(BloodGroupMaster.class))).thenReturn(updated);

        BloodGroupResponse response = bloodGroupService.update(1L, request);

        assertThat(response.name()).isEqualTo("A Positive Blood");
    }

    @Test
    void shouldThrowWhenUpdatingWithDuplicateCode() {
        BloodGroupMaster existing = createBloodGroup(1L, "A Positive", "A+");
        BloodGroupRequest request = new BloodGroupRequest("A Positive", "B+", true);

        when(bloodGroupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bloodGroupRepository.existsByCodeAndIdNot("B+", 1L)).thenReturn(true);

        assertThatThrownBy(() -> bloodGroupService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(bloodGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingWithDuplicateName() {
        BloodGroupMaster existing = createBloodGroup(1L, "A Positive", "A+");
        BloodGroupRequest request = new BloodGroupRequest("B Positive", "A+", true);

        when(bloodGroupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bloodGroupRepository.existsByCodeAndIdNot("A+", 1L)).thenReturn(false);
        when(bloodGroupRepository.existsByNameAndIdNot("B Positive", 1L)).thenReturn(true);

        assertThatThrownBy(() -> bloodGroupService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(bloodGroupRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        BloodGroupRequest request = new BloodGroupRequest("A Positive", "A+", null);
        when(bloodGroupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloodGroupService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Blood group not found with id: 999");
    }

    @Test
    void shouldDelete() {
        when(bloodGroupRepository.existsById(1L)).thenReturn(true);

        bloodGroupService.delete(1L);

        verify(bloodGroupRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(bloodGroupRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> bloodGroupService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Blood group not found with id: 999");

        verify(bloodGroupRepository, never()).deleteById(any());
    }

    private BloodGroupMaster createBloodGroup(Long id, String name, String code) {
        BloodGroupMaster bg = new BloodGroupMaster(name, code);
        bg.setId(id);
        Instant now = Instant.now();
        bg.setCreatedAt(now);
        bg.setUpdatedAt(now);
        return bg;
    }
}

