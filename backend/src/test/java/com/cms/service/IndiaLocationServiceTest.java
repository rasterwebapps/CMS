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

import com.cms.dto.IndiaDistrictRequest;
import com.cms.dto.IndiaDistrictResponse;
import com.cms.dto.IndiaStateRequest;
import com.cms.dto.IndiaStateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.IndiaDistrict;
import com.cms.model.IndiaState;
import com.cms.repository.IndiaDistrictRepository;
import com.cms.repository.IndiaStateRepository;

@ExtendWith(MockitoExtension.class)
class IndiaLocationServiceTest {

    @Mock private IndiaStateRepository stateRepository;
    @Mock private IndiaDistrictRepository districtRepository;

    private IndiaLocationService service;

    @BeforeEach
    void setUp() {
        service = new IndiaLocationService(stateRepository, districtRepository);
    }

    // ─── State tests ──────────────────────────────────────────────────────────

    @Test
    void shouldCreateState() {
        IndiaStateRequest req = new IndiaStateRequest("Tamil Nadu", "TN", true);
        IndiaState saved = state(1L, "Tamil Nadu", "TN");

        when(stateRepository.existsByNameIgnoreCase("Tamil Nadu")).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCase("TN")).thenReturn(false);
        when(stateRepository.save(any())).thenReturn(saved);

        IndiaStateResponse res = service.createState(req);

        assertThat(res.name()).isEqualTo("Tamil Nadu");
        assertThat(res.code()).isEqualTo("TN");
        verify(stateRepository).save(any());
    }

    @Test
    void shouldThrowWhenDuplicateStateName() {
        when(stateRepository.existsByNameIgnoreCase("Tamil Nadu")).thenReturn(true);

        assertThatThrownBy(() -> service.createState(new IndiaStateRequest("Tamil Nadu", "TN", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(stateRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDuplicateStateCode() {
        when(stateRepository.existsByNameIgnoreCase("Tamil Nadu")).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCase("TN")).thenReturn(true);

        assertThatThrownBy(() -> service.createState(new IndiaStateRequest("Tamil Nadu", "TN", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldFindAllStates() {
        when(stateRepository.findAllByOrderByNameAsc()).thenReturn(List.of(state(1L, "Tamil Nadu", "TN")));

        List<IndiaStateResponse> list = service.findAllStates();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("Tamil Nadu");
    }

    @Test
    void shouldFindActiveStates() {
        when(stateRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(state(1L, "Tamil Nadu", "TN")));

        assertThat(service.findActiveStates()).hasSize(1);
    }

    @Test
    void shouldFindStateById() {
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state(1L, "Tamil Nadu", "TN")));

        IndiaStateResponse res = service.findStateById(1L);

        assertThat(res.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenStateNotFound() {
        when(stateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStateById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateState() {
        IndiaState existing = state(1L, "Tamil Nadu", "TN");
        when(stateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(stateRepository.existsByNameIgnoreCaseAndIdNot("Tamil Nadu Updated", 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndIdNot("TNU", 1L)).thenReturn(false);
        when(stateRepository.save(any())).thenReturn(existing);

        IndiaStateResponse res = service.updateState(1L, new IndiaStateRequest("Tamil Nadu Updated", "TNU", true));

        verify(stateRepository).save(any());
    }

    @Test
    void shouldDeleteState() {
        when(stateRepository.existsById(1L)).thenReturn(true);

        service.deleteState(1L);

        verify(stateRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentState() {
        when(stateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteState(99L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(stateRepository, never()).deleteById(any());
    }

    // ─── District tests ───────────────────────────────────────────────────────

    @Test
    void shouldCreateDistrict() {
        IndiaState s = state(1L, "Tamil Nadu", "TN");
        IndiaDistrict saved = district(1L, s, "Chennai");

        when(stateRepository.findById(1L)).thenReturn(Optional.of(s));
        when(districtRepository.existsByStateIdAndNameIgnoreCase(1L, "Chennai")).thenReturn(false);
        when(districtRepository.save(any())).thenReturn(saved);

        IndiaDistrictResponse res = service.createDistrict(1L, new IndiaDistrictRequest(1L, "Chennai", true));

        assertThat(res.name()).isEqualTo("Chennai");
        assertThat(res.stateName()).isEqualTo("Tamil Nadu");
    }

    @Test
    void shouldThrowWhenDuplicateDistrict() {
        IndiaState s = state(1L, "Tamil Nadu", "TN");
        when(stateRepository.findById(1L)).thenReturn(Optional.of(s));
        when(districtRepository.existsByStateIdAndNameIgnoreCase(1L, "Chennai")).thenReturn(true);

        assertThatThrownBy(() -> service.createDistrict(1L, new IndiaDistrictRequest(1L, "Chennai", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldFindActiveDistrictsByState() {
        IndiaState s = state(1L, "Tamil Nadu", "TN");
        when(stateRepository.existsById(1L)).thenReturn(true);
        when(districtRepository.findByStateIdAndIsActiveTrueOrderByNameAsc(1L))
            .thenReturn(List.of(district(1L, s, "Chennai")));

        List<IndiaDistrictResponse> list = service.findActiveDistrictsByState(1L);

        assertThat(list).hasSize(1);
    }

    @Test
    void shouldThrowWhenStateNotFoundForDistricts() {
        when(stateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findActiveDistrictsByState(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteDistrict() {
        when(districtRepository.existsById(1L)).thenReturn(true);

        service.deleteDistrict(1L);

        verify(districtRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentDistrict() {
        when(districtRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteDistrict(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private IndiaState state(Long id, String name, String code) {
        IndiaState s = new IndiaState(name, code);
        s.setId(id);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private IndiaDistrict district(Long id, IndiaState state, String name) {
        IndiaDistrict d = new IndiaDistrict(state, name);
        d.setId(id);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }
}

