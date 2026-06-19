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
import com.cms.model.Community;
import com.cms.repository.CommunityRepository;

@ExtendWith(MockitoExtension.class)
class CommunityLifecycleStatusToggleTest {

    @Mock
    private CommunityRepository communityRepository;

    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(communityRepository);
    }

    @Test
    void shouldActivateCommunityUsingStatusEndpoint() {
        Community entity = createEntity(1L, false);
        when(communityRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(communityRepository.save(any(Community.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = communityService.updateStatus(1L, new ActiveStatusUpdateRequest(true, "reopen"));

        assertThat(response.isActive()).isTrue();
    }

    private Community createEntity(Long id, boolean isActive) {
        Community entity = new Community("OBC", "OBC", "Other backward class");
        entity.setId(id);
        entity.setIsActive(isActive);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}

