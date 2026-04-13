package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LabSlotRequest;
import com.cms.dto.LabSlotResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LabSlot;
import com.cms.repository.LabSlotRepository;

@ExtendWith(MockitoExtension.class)
class LabSlotServiceTest {

    @Mock
    private LabSlotRepository labSlotRepository;

    private LabSlotService labSlotService;

    @BeforeEach
    void setUp() {
        labSlotService = new LabSlotService(labSlotRepository);
    }

    @Test
    void shouldCreateLabSlot() {
        LabSlotRequest request = new LabSlotRequest(
            "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true
        );

        LabSlot savedSlot = createLabSlot(1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true);

        when(labSlotRepository.save(any(LabSlot.class))).thenReturn(savedSlot);

        LabSlotResponse response = labSlotService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Slot 1");
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void shouldFindAllLabSlots() {
        LabSlot slot1 = createLabSlot(1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true);
        LabSlot slot2 = createLabSlot(2L, "Slot 2", LocalTime.of(10, 30), LocalTime.of(12, 0), 2, true);

        when(labSlotRepository.findAllByOrderBySlotOrderAsc()).thenReturn(List.of(slot1, slot2));

        List<LabSlotResponse> responses = labSlotService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).slotOrder()).isEqualTo(1);
        assertThat(responses.get(1).slotOrder()).isEqualTo(2);
    }

    @Test
    void shouldFindAllActiveLabSlots() {
        LabSlot slot1 = createLabSlot(1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true);

        when(labSlotRepository.findByIsActiveTrueOrderBySlotOrderAsc()).thenReturn(List.of(slot1));

        List<LabSlotResponse> responses = labSlotService.findAllActive();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isTrue();
    }

    @Test
    void shouldFindLabSlotById() {
        LabSlot slot = createLabSlot(1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true);

        when(labSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        LabSlotResponse response = labSlotService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Slot 1");
    }

    @Test
    void shouldThrowExceptionWhenLabSlotNotFoundById() {
        when(labSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labSlotService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab slot not found with id: 999");
    }

    @Test
    void shouldUpdateLabSlot() {
        LabSlot existingSlot = createLabSlot(1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true);

        LabSlotRequest updateRequest = new LabSlotRequest(
            "Updated Slot", LocalTime.of(8, 30), LocalTime.of(10, 0), 1, true
        );

        LabSlot updatedSlot = createLabSlot(1L, "Updated Slot", LocalTime.of(8, 30), LocalTime.of(10, 0), 1, true);

        when(labSlotRepository.findById(1L)).thenReturn(Optional.of(existingSlot));
        when(labSlotRepository.save(any(LabSlot.class))).thenReturn(updatedSlot);

        LabSlotResponse response = labSlotService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Updated Slot");
        assertThat(response.startTime()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void shouldDeleteLabSlot() {
        when(labSlotRepository.existsById(1L)).thenReturn(true);

        labSlotService.delete(1L);

        verify(labSlotRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentLabSlot() {
        when(labSlotRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> labSlotService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Lab slot not found with id: 999");

        verify(labSlotRepository, never()).deleteById(any());
    }

    private LabSlot createLabSlot(Long id, String name, LocalTime startTime, LocalTime endTime,
                                   Integer slotOrder, Boolean isActive) {
        LabSlot slot = new LabSlot(name, startTime, endTime, slotOrder, isActive);
        slot.setId(id);
        Instant now = Instant.now();
        slot.setCreatedAt(now);
        slot.setUpdatedAt(now);
        return slot;
    }
}
