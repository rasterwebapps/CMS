package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.cms.model.BloodGroupMaster;
import com.cms.repository.BloodGroupRepository;

@ExtendWith(MockitoExtension.class)
class BloodGroupLifecycleStatusToggleTest {

    @Mock
    private BloodGroupRepository bloodGroupRepository;

    private BloodGroupService bloodGroupService;

    @BeforeEach
    void setUp() {
        bloodGroupService = new BloodGroupService(bloodGroupRepository);
    }

    @Test
    void shouldDeactivateBloodGroupUsingStatusEndpoint() {
        BloodGroupMaster entity = createEntity(1L, true);
        when(bloodGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(bloodGroupRepository.save(any(BloodGroupMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bloodGroupService.updateStatus(1L, new ActiveStatusUpdateRequest(false, "retired"));

        assertThat(response.isActive()).isFalse();
    }

    private BloodGroupMaster createEntity(Long id, boolean isActive) {
        BloodGroupMaster entity = new BloodGroupMaster("A Positive", "A+");
        entity.setId(id);
        entity.setIsActive(isActive);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}

