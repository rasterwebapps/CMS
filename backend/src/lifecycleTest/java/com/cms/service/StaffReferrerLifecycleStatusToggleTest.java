package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.StaffReferrer;
import com.cms.repository.CommissionPayoutRepository;
import com.cms.repository.InstitutionRepository;
import com.cms.repository.StaffReferrerRepository;

@ExtendWith(MockitoExtension.class)
class StaffReferrerLifecycleStatusToggleTest {

    @Mock
    private StaffReferrerRepository staffReferrerRepository;
    @Mock
    private CommissionPayoutRepository commissionPayoutRepository;
    @Mock
    private InstitutionRepository institutionRepository;

    private StaffReferrerService staffReferrerService;

    @BeforeEach
    void setUp() {
        staffReferrerService = new StaffReferrerService(staffReferrerRepository, commissionPayoutRepository, institutionRepository);
    }

    @Test
    void shouldBlockDeactivationWhenPayoutReferenceExists() {
        StaffReferrer entity = createEntity(1L, true);
        when(staffReferrerRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(commissionPayoutRepository.existsByStaffReferrerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> staffReferrerService.updateStatus(1L, new ActiveStatusUpdateRequest(false, "retired")))
            .isInstanceOf(LifecycleConflictException.class)
            .satisfies(ex -> {
                LifecycleConflictException conflict = (LifecycleConflictException) ex;
                assertThat(conflict.getCode()).isEqualTo("ACTIVE_REFERENCE_EXISTS");
            });
    }

    @Test
    void shouldActivateStaffReferrerWhenNoDependencyConflict() {
        StaffReferrer entity = createEntity(1L, false);
        when(staffReferrerRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(staffReferrerRepository.save(any(StaffReferrer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = staffReferrerService.updateStatus(1L, new ActiveStatusUpdateRequest(true, "reopen"));

        assertThat(response.isActive()).isTrue();
    }

    private StaffReferrer createEntity(Long id, boolean isActive) {
        StaffReferrer entity = new StaffReferrer();
        entity.setId(id);
        entity.setName("Staff One");
        entity.setIsActive(isActive);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}

