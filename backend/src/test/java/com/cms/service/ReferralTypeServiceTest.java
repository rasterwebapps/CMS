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

import com.cms.dto.ReferralTypeRequest;
import com.cms.dto.ReferralTypeResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ReferralType;
import com.cms.repository.ReferralTypeRepository;

@ExtendWith(MockitoExtension.class)
class ReferralTypeServiceTest {

    @Mock
    private ReferralTypeRepository referralTypeRepository;

    private ReferralTypeService referralTypeService;

    @BeforeEach
    void setUp() {
        referralTypeService = new ReferralTypeService(referralTypeRepository);
    }

    @Test
    void shouldCreateReferralType() {
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff", "STAFF", new BigDecimal("5000.00"), true, "Staff referral", true
        );

        ReferralType saved = createReferralType(1L, "Staff", "STAFF", new BigDecimal("5000.00"));

        when(referralTypeRepository.existsByCode("STAFF")).thenReturn(false);
        when(referralTypeRepository.save(any(ReferralType.class))).thenReturn(saved);

        ReferralTypeResponse response = referralTypeService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Staff");
        assertThat(response.code()).isEqualTo("STAFF");
        assertThat(response.commissionAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.hasCommission()).isTrue();
        verify(referralTypeRepository).save(any(ReferralType.class));
    }

    @Test
    void shouldThrowWhenDuplicateCode() {
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff", "STAFF", BigDecimal.ZERO, false, null, true
        );

        when(referralTypeRepository.existsByCode("STAFF")).thenReturn(true);

        assertThatThrownBy(() -> referralTypeService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldCreateWithDefaultActiveTrue() {
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff", "STAFF", BigDecimal.ZERO, null, null, null
        );

        ReferralType saved = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeRepository.existsByCode("STAFF")).thenReturn(false);
        when(referralTypeRepository.save(any(ReferralType.class))).thenReturn(saved);

        ReferralTypeResponse response = referralTypeService.create(request);

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void shouldFindAll() {
        ReferralType rt = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeRepository.findAll()).thenReturn(List.of(rt));

        List<ReferralTypeResponse> responses = referralTypeService.findAll();

        assertThat(responses).hasSize(1);
        verify(referralTypeRepository).findAll();
    }

    @Test
    void shouldFindActive() {
        ReferralType rt = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeRepository.findByIsActiveTrue()).thenReturn(List.of(rt));

        List<ReferralTypeResponse> responses = referralTypeService.findActive();

        assertThat(responses).hasSize(1);
        verify(referralTypeRepository).findByIsActiveTrue();
    }

    @Test
    void shouldFindById() {
        ReferralType rt = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(rt));

        ReferralTypeResponse response = referralTypeService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Staff");
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(referralTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referralTypeService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Referral type not found with id: 999");
    }

    @Test
    void shouldUpdate() {
        ReferralType existing = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff Updated", "STAFF", new BigDecimal("10000.00"), true, "Updated", true
        );

        ReferralType updated = createReferralType(1L, "Staff Updated", "STAFF", new BigDecimal("10000.00"));

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referralTypeRepository.save(any(ReferralType.class))).thenReturn(updated);

        ReferralTypeResponse response = referralTypeService.update(1L, request);

        assertThat(response.name()).isEqualTo("Staff Updated");
        assertThat(response.commissionAmount()).isEqualTo(new BigDecimal("10000.00"));
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff", "STAFF", BigDecimal.ZERO, false, null, true
        );

        when(referralTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referralTypeService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Referral type not found with id: 999");
    }

    @Test
    void shouldDelete() {
        when(referralTypeRepository.existsById(1L)).thenReturn(true);

        referralTypeService.delete(1L);

        verify(referralTypeRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(referralTypeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> referralTypeService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Referral type not found with id: 999");

        verify(referralTypeRepository, never()).deleteById(any());
    }

    private ReferralType createReferralType(Long id, String name, String code, BigDecimal commissionAmount) {
        ReferralType rt = new ReferralType(name, code, commissionAmount, true, name + " description", true);
        rt.setId(id);
        Instant now = Instant.now();
        rt.setCreatedAt(now);
        rt.setUpdatedAt(now);
        return rt;
    }
}
