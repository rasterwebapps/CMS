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
import com.cms.model.ScholarshipType;
import com.cms.repository.ScholarshipTypeRepository;

@ExtendWith(MockitoExtension.class)
class ScholarshipTypeLifecycleStatusToggleTest {

    @Mock
    private ScholarshipTypeRepository scholarshipTypeRepository;

    private ScholarshipTypeService scholarshipTypeService;

    @BeforeEach
    void setUp() {
        scholarshipTypeService = new ScholarshipTypeService(scholarshipTypeRepository);
    }

    @Test
    void shouldDeactivateScholarshipTypeUsingStatusEndpoint() {
        ScholarshipType type = createType(1L, true);
        when(scholarshipTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(scholarshipTypeRepository.save(any(ScholarshipType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = scholarshipTypeService.updateStatus(1L, new ActiveStatusUpdateRequest(false, "retired"), "admin");

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldReactivateScholarshipTypeUsingStatusEndpoint() {
        ScholarshipType type = createType(1L, false);
        when(scholarshipTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(scholarshipTypeRepository.save(any(ScholarshipType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = scholarshipTypeService.updateStatus(1L, new ActiveStatusUpdateRequest(true, "reopen"), "admin");

        assertThat(response.isActive()).isTrue();
    }

    private ScholarshipType createType(Long id, boolean active) {
        ScholarshipType type = new ScholarshipType();
        type.setId(id);
        type.setCode("SC001");
        type.setName("Govt Scholarship");
        type.setActive(active);
        Instant now = Instant.now();
        type.setCreatedAt(now);
        type.setUpdatedAt(now);
        return type;
    }
}

