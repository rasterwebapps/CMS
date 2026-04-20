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
        assertThat(response.isSystemDefined()).isFalse();
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
        when(referralTypeRepository.existsByNameAndIdNot("Staff Updated", 1L)).thenReturn(false);
        when(referralTypeRepository.existsByCodeAndIdNot("STAFF", 1L)).thenReturn(false);
        when(referralTypeRepository.save(any(ReferralType.class))).thenReturn(updated);

        ReferralTypeResponse response = referralTypeService.update(1L, request);

        assertThat(response.name()).isEqualTo("Staff Updated");
        assertThat(response.commissionAmount()).isEqualTo(new BigDecimal("10000.00"));
    }

    @Test
    void shouldThrowWhenUpdatingReferralTypeWithDuplicateName() {
        ReferralType existing = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Alumni", "STAFF", BigDecimal.ZERO, false, null, true
        );

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referralTypeRepository.existsByNameAndIdNot("Alumni", 1L)).thenReturn(true);

        assertThatThrownBy(() -> referralTypeService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Alumni")
            .hasMessageContaining("already exists");

        verify(referralTypeRepository, never()).save(any(ReferralType.class));
    }

    @Test
    void shouldThrowWhenUpdatingReferralTypeWithDuplicateCode() {
        ReferralType existing = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Staff", "ALUMNI", BigDecimal.ZERO, false, null, true
        );

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referralTypeRepository.existsByNameAndIdNot("Staff", 1L)).thenReturn(false);
        when(referralTypeRepository.existsByCodeAndIdNot("ALUMNI", 1L)).thenReturn(true);

        assertThatThrownBy(() -> referralTypeService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ALUMNI")
            .hasMessageContaining("already exists");

        verify(referralTypeRepository, never()).save(any(ReferralType.class));
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
        ReferralType rt = createReferralType(1L, "Staff", "STAFF", BigDecimal.ZERO);
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(rt));

        referralTypeService.delete(1L);

        verify(referralTypeRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(referralTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referralTypeService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Referral type not found with id: 999");

        verify(referralTypeRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDeletingSystemDefined() {
        ReferralType rt = createReferralType(1L, "Agent Referral", "AGENT_REFERRAL", new BigDecimal("10000.00"));
        rt.setIsSystemDefined(true);
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> referralTypeService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("System-defined referral types cannot be deleted");

        verify(referralTypeRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDisablingCommissionOnSystemDefined() {
        ReferralType existing = createReferralType(1L, "Agent Referral", "AGENT_REFERRAL", new BigDecimal("10000.00"));
        existing.setIsSystemDefined(true);
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Agent Referral", "AGENT_REFERRAL", new BigDecimal("10000.00"), false, null, true
        );

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referralTypeRepository.existsByNameAndIdNot("Agent Referral", 1L)).thenReturn(false);

        assertThatThrownBy(() -> referralTypeService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot disable commission for a system-defined referral type");

        verify(referralTypeRepository, never()).save(any(ReferralType.class));
    }

    @Test
    void shouldPreserveCodeWhenUpdatingSystemDefined() {
        ReferralType existing = createReferralType(1L, "Agent Referral", "AGENT_REFERRAL", new BigDecimal("10000.00"));
        existing.setIsSystemDefined(true);
        ReferralTypeRequest request = new ReferralTypeRequest(
            "Agent / Consultant", "CHANGED_CODE", new BigDecimal("12000.00"), true, null, true
        );

        ReferralType updated = createReferralType(1L, "Agent / Consultant", "AGENT_REFERRAL", new BigDecimal("12000.00"));
        updated.setIsSystemDefined(true);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referralTypeRepository.existsByNameAndIdNot("Agent / Consultant", 1L)).thenReturn(false);
        when(referralTypeRepository.save(any(ReferralType.class))).thenReturn(updated);

        ReferralTypeResponse response = referralTypeService.update(1L, request);

        assertThat(response.name()).isEqualTo("Agent / Consultant");
        assertThat(response.code()).isEqualTo("AGENT_REFERRAL");
        assertThat(response.isSystemDefined()).isTrue();
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
