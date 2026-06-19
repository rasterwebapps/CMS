package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.ReferralType;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.ReferralTypeRepository;

@ExtendWith(MockitoExtension.class)
class ReferralTypeLifecycleStatusToggleTest {

    @Mock
    private ReferralTypeRepository referralTypeRepository;
    @Mock
    private EnquiryRepository enquiryRepository;

    private ReferralTypeService referralTypeService;

    @BeforeEach
    void setUp() {
        referralTypeService = new ReferralTypeService(referralTypeRepository, enquiryRepository);
    }

    @Test
    void shouldBlockDeactivationWhenEnquiryReferenceExists() {
        ReferralType referralType = createEntity(1L, true);
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(referralType));
        when(enquiryRepository.existsByReferralTypeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> referralTypeService.updateStatus(1L, new ActiveStatusUpdateRequest(false, "retired")))
            .isInstanceOf(LifecycleConflictException.class)
            .satisfies(ex -> {
                LifecycleConflictException conflict = (LifecycleConflictException) ex;
                assertThat(conflict.getCode()).isEqualTo("ACTIVE_REFERENCE_EXISTS");
            });
    }

    @Test
    void shouldActivateReferralTypeWhenNoDependencyConflict() {
        ReferralType referralType = createEntity(1L, false);
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(referralType));
        when(referralTypeRepository.save(any(ReferralType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = referralTypeService.updateStatus(1L, new ActiveStatusUpdateRequest(true, "reopen"));

        assertThat(response.isActive()).isTrue();
    }

    private ReferralType createEntity(Long id, boolean isActive) {
        ReferralType entity = new ReferralType(
            "Agent", "AGENT", BigDecimal.valueOf(2500), true, "Agent referral", isActive
        );
        entity.setId(id);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}

