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

import com.cms.dto.CommunityRequest;
import com.cms.dto.CommunityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Community;
import com.cms.repository.CommunityRepository;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private CommunityRepository communityRepository;

    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(communityRepository);
    }

    @Test
    void shouldCreateCommunity() {
        CommunityRequest request = new CommunityRequest("Backward Class", "BC", "Backward Class category", true);
        Community saved = createCommunity(1L, "Backward Class", "BC");

        when(communityRepository.existsByCode("BC")).thenReturn(false);
        when(communityRepository.save(any(Community.class))).thenReturn(saved);

        CommunityResponse response = communityService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Backward Class");
        assertThat(response.code()).isEqualTo("BC");
        assertThat(response.isActive()).isTrue();
        verify(communityRepository).save(any(Community.class));
    }

    @Test
    void shouldThrowWhenDuplicateCode() {
        CommunityRequest request = new CommunityRequest("Backward Class", "BC", null, true);

        when(communityRepository.existsByCode("BC")).thenReturn(true);

        assertThatThrownBy(() -> communityService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(communityRepository, never()).save(any());
    }

    @Test
    void shouldCreateWithDefaultActiveWhenIsActiveNull() {
        CommunityRequest request = new CommunityRequest("Other Caste", "OC", null, null);
        Community saved = createCommunity(1L, "Other Caste", "OC");

        when(communityRepository.existsByCode("OC")).thenReturn(false);
        when(communityRepository.save(any(Community.class))).thenReturn(saved);

        CommunityResponse response = communityService.create(request);

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void shouldFindAll() {
        Community c = createCommunity(1L, "Backward Class", "BC");
        when(communityRepository.findAllByOrderByNameAsc()).thenReturn(List.of(c));

        List<CommunityResponse> responses = communityService.findAll();

        assertThat(responses).hasSize(1);
        verify(communityRepository).findAllByOrderByNameAsc();
    }

    @Test
    void shouldFindActive() {
        Community c = createCommunity(1L, "Backward Class", "BC");
        when(communityRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(c));

        List<CommunityResponse> responses = communityService.findActive();

        assertThat(responses).hasSize(1);
        verify(communityRepository).findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void shouldFindById() {
        Community c = createCommunity(1L, "Backward Class", "BC");
        when(communityRepository.findById(1L)).thenReturn(Optional.of(c));

        CommunityResponse response = communityService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Backward Class");
        assertThat(response.code()).isEqualTo("BC");
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(communityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Community not found with id: 999");
    }

    @Test
    void shouldUpdate() {
        Community existing = createCommunity(1L, "Backward Class", "BC");
        CommunityRequest request = new CommunityRequest("Backward Class Updated", "BC", "Updated desc", true);
        Community updated = createCommunity(1L, "Backward Class Updated", "BC");

        when(communityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(communityRepository.existsByCodeAndIdNot("BC", 1L)).thenReturn(false);
        when(communityRepository.existsByNameAndIdNot("Backward Class Updated", 1L)).thenReturn(false);
        when(communityRepository.save(any(Community.class))).thenReturn(updated);

        CommunityResponse response = communityService.update(1L, request);

        assertThat(response.name()).isEqualTo("Backward Class Updated");
    }

    @Test
    void shouldThrowWhenUpdatingWithDuplicateCode() {
        Community existing = createCommunity(1L, "Backward Class", "BC");
        CommunityRequest request = new CommunityRequest("Backward Class", "OC", null, true);

        when(communityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(communityRepository.existsByCodeAndIdNot("OC", 1L)).thenReturn(true);

        assertThatThrownBy(() -> communityService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(communityRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingWithDuplicateName() {
        Community existing = createCommunity(1L, "Backward Class", "BC");
        CommunityRequest request = new CommunityRequest("Most Backward Class", "BC", null, true);

        when(communityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(communityRepository.existsByCodeAndIdNot("BC", 1L)).thenReturn(false);
        when(communityRepository.existsByNameAndIdNot("Most Backward Class", 1L)).thenReturn(true);

        assertThatThrownBy(() -> communityService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(communityRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        CommunityRequest request = new CommunityRequest("BC", "BC", null, null);
        when(communityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Community not found with id: 999");
    }

    @Test
    void shouldDelete() {
        when(communityRepository.existsById(1L)).thenReturn(true);

        communityService.delete(1L);

        verify(communityRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(communityRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Community not found with id: 999");

        verify(communityRepository, never()).deleteById(any());
    }

    private Community createCommunity(Long id, String name, String code) {
        Community c = new Community(name, code, name + " description");
        c.setId(id);
        Instant now = Instant.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return c;
    }
}

