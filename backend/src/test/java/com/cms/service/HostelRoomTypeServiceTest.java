package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.HostelRoomTypeRequest;
import com.cms.dto.HostelRoomTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.HostelRoomType;
import com.cms.repository.HostelRoomTypeRepository;

@ExtendWith(MockitoExtension.class)
class HostelRoomTypeServiceTest {

    @Mock
    private HostelRoomTypeRepository hostelRoomTypeRepository;

    private HostelRoomTypeService hostelRoomTypeService;

    @BeforeEach
    void setUp() {
        hostelRoomTypeService = new HostelRoomTypeService(hostelRoomTypeRepository);
    }

    @Test
    void shouldCreateRoomType() {
        HostelRoomTypeRequest request = new HostelRoomTypeRequest(
            "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"), null, true);
        HostelRoomType saved = createRoomType(1L, "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"));

        when(hostelRoomTypeRepository.existsByNameIgnoreCase("AC Double Sharing")).thenReturn(false);
        when(hostelRoomTypeRepository.existsByCodeIgnoreCase("AC_DOUBLE")).thenReturn(false);
        when(hostelRoomTypeRepository.save(any(HostelRoomType.class))).thenReturn(saved);

        HostelRoomTypeResponse response = hostelRoomTypeService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("AC Double Sharing");
        assertThat(response.code()).isEqualTo("AC_DOUBLE");
        assertThat(response.sharingCapacity()).isEqualTo(2);
        assertThat(response.isAc()).isTrue();
        assertThat(response.feeAmountPerYear()).isEqualByComparingTo("45000.00");
        assertThat(response.isActive()).isTrue();
        verify(hostelRoomTypeRepository).save(any(HostelRoomType.class));
    }

    @Test
    void shouldThrowWhenDuplicateName() {
        HostelRoomTypeRequest request = new HostelRoomTypeRequest(
            "AC Double Sharing", "AC_DOUBLE2", 2, true, new BigDecimal("45000.00"), null, null);

        when(hostelRoomTypeRepository.existsByNameIgnoreCase("AC Double Sharing")).thenReturn(true);

        assertThatThrownBy(() -> hostelRoomTypeService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(hostelRoomTypeRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDuplicateCode() {
        HostelRoomTypeRequest request = new HostelRoomTypeRequest(
            "Non-AC Single", "AC_DOUBLE", 1, false, new BigDecimal("30000.00"), null, null);

        when(hostelRoomTypeRepository.existsByNameIgnoreCase("Non-AC Single")).thenReturn(false);
        when(hostelRoomTypeRepository.existsByCodeIgnoreCase("AC_DOUBLE")).thenReturn(true);

        assertThatThrownBy(() -> hostelRoomTypeService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(hostelRoomTypeRepository, never()).save(any());
    }

    @Test
    void shouldFindAll() {
        HostelRoomType rt = createRoomType(1L, "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"));
        when(hostelRoomTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(rt));

        List<HostelRoomTypeResponse> responses = hostelRoomTypeService.findAll();

        assertThat(responses).hasSize(1);
        verify(hostelRoomTypeRepository).findAllByOrderByNameAsc();
    }

    @Test
    void shouldFindActive() {
        HostelRoomType rt = createRoomType(1L, "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"));
        when(hostelRoomTypeRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(rt));

        List<HostelRoomTypeResponse> responses = hostelRoomTypeService.findActive();

        assertThat(responses).hasSize(1);
        verify(hostelRoomTypeRepository).findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void shouldFindById() {
        HostelRoomType rt = createRoomType(1L, "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"));
        when(hostelRoomTypeRepository.findById(1L)).thenReturn(Optional.of(rt));

        HostelRoomTypeResponse response = hostelRoomTypeService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("AC Double Sharing");
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(hostelRoomTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hostelRoomTypeService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Hostel room type not found with id: 999");
    }

    @Test
    void shouldUpdate() {
        HostelRoomType existing = createRoomType(1L, "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"));
        HostelRoomTypeRequest request = new HostelRoomTypeRequest(
            "AC Double Sharing Deluxe", "AC_DOUBLE", 2, true, new BigDecimal("50000.00"), null, true);
        HostelRoomType updated = createRoomType(1L, "AC Double Sharing Deluxe", "AC_DOUBLE", 2, true, new BigDecimal("50000.00"));

        when(hostelRoomTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(hostelRoomTypeRepository.existsByNameIgnoreCaseAndIdNot("AC Double Sharing Deluxe", 1L)).thenReturn(false);
        when(hostelRoomTypeRepository.existsByCodeIgnoreCaseAndIdNot("AC_DOUBLE", 1L)).thenReturn(false);
        when(hostelRoomTypeRepository.save(any(HostelRoomType.class))).thenReturn(updated);

        HostelRoomTypeResponse response = hostelRoomTypeService.update(1L, request);

        assertThat(response.name()).isEqualTo("AC Double Sharing Deluxe");
        assertThat(response.feeAmountPerYear()).isEqualByComparingTo("50000.00");
    }

    @Test
    void shouldThrowWhenUpdatingWithDuplicateName() {
        HostelRoomType existing = createRoomType(1L, "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"));
        HostelRoomTypeRequest request = new HostelRoomTypeRequest(
            "Non-AC Single", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"), null, null);

        when(hostelRoomTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(hostelRoomTypeRepository.existsByNameIgnoreCaseAndIdNot("Non-AC Single", 1L)).thenReturn(true);

        assertThatThrownBy(() -> hostelRoomTypeService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(hostelRoomTypeRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        HostelRoomTypeRequest request = new HostelRoomTypeRequest(
            "AC Double Sharing", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"), null, null);
        when(hostelRoomTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hostelRoomTypeService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Hostel room type not found with id: 999");
    }

    @Test
    void shouldDelete() {
        when(hostelRoomTypeRepository.existsById(1L)).thenReturn(true);

        hostelRoomTypeService.delete(1L);

        verify(hostelRoomTypeRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(hostelRoomTypeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> hostelRoomTypeService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Hostel room type not found with id: 999");

        verify(hostelRoomTypeRepository, never()).deleteById(any());
    }

    private HostelRoomType createRoomType(Long id, String name, String code, Integer sharingCapacity,
                                           Boolean isAc, BigDecimal feeAmountPerYear) {
        HostelRoomType rt = new HostelRoomType(name, code, sharingCapacity, isAc, feeAmountPerYear, null);
        rt.setId(id);
        Instant now = Instant.now();
        rt.setCreatedAt(now);
        rt.setUpdatedAt(now);
        return rt;
    }
}
