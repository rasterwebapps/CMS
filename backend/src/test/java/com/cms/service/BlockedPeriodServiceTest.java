package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.BlockedPeriodRequest;
import com.cms.dto.BlockedPeriodResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.BlockedPeriod;
import com.cms.model.Period;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.PeriodRepository;

@ExtendWith(MockitoExtension.class)
class BlockedPeriodServiceTest {

    @Mock
    private BlockedPeriodRepository blockedPeriodRepository;

    @Mock
    private PeriodRepository periodRepository;

    private BlockedPeriodService service;

    private Period period;

    @BeforeEach
    void setUp() {
        service = new BlockedPeriodService(blockedPeriodRepository, periodRepository);
        period = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        period.setId(1L);
        period.setDurationMinutes(50);
    }

    private BlockedPeriod buildBlock(Long id, BlockType type) {
        BlockedPeriod b = new BlockedPeriod();
        b.setId(id);
        b.setPeriod(period);
        b.setBlockType(type);
        b.setReason("Staff meeting");
        if (type == BlockType.ONE_OFF) {
            b.setSpecificDate(LocalDate.of(2026, 11, 15));
        } else {
            b.setDayOfWeek(DayOfWeek.WEDNESDAY);
            b.setRangeStartDate(LocalDate.of(2026, 10, 1));
            b.setRangeEndDate(LocalDate.of(2027, 3, 31));
        }
        return b;
    }

    // ─── CREATE ──────────────────────────────────────

    @Test
    void shouldCreateOneOffBlock() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.ONE_OFF, LocalDate.of(2026, 11, 15), null, null, null, "Staff meeting");

        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(blockedPeriodRepository.save(any(BlockedPeriod.class))).thenReturn(buildBlock(1L, BlockType.ONE_OFF));

        BlockedPeriodResponse response = service.create(request);

        assertThat(response.blockType()).isEqualTo(BlockType.ONE_OFF);
        assertThat(response.specificDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        verify(blockedPeriodRepository).save(any(BlockedPeriod.class));
    }

    @Test
    void shouldCreateRecurringBlock() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.RECURRING, null, DayOfWeek.WEDNESDAY,
            LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), "Staff meeting");

        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(blockedPeriodRepository.save(any(BlockedPeriod.class))).thenReturn(buildBlock(1L, BlockType.RECURRING));

        BlockedPeriodResponse response = service.create(request);

        assertThat(response.blockType()).isEqualTo(BlockType.RECURRING);
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        verify(blockedPeriodRepository).save(any(BlockedPeriod.class));
    }

    @Test
    void shouldThrowWhenOneOffMissingSpecificDate() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.ONE_OFF, null, null, null, null, "Staff meeting");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Specific date is required");

        verify(blockedPeriodRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRecurringMissingDayOfWeekOrRange() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.RECURRING, null, null, null, null, "Staff meeting");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Day of week and a start/end date range are required");

        verify(blockedPeriodRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRecurringRangeEndBeforeStart() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.RECURRING, null, DayOfWeek.WEDNESDAY,
            LocalDate.of(2027, 3, 31), LocalDate.of(2026, 10, 1), "Staff meeting");

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Range end date must not be before range start date");

        verify(blockedPeriodRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCreatingWithNonExistentPeriod() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            999L, BlockType.ONE_OFF, LocalDate.of(2026, 11, 15), null, null, null, "Staff meeting");

        when(periodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Period not found with id: 999");

        verify(blockedPeriodRepository, never()).save(any());
    }

    // ─── FIND ─────────────────────────────────────────

    @Test
    void shouldFindAllBlocks() {
        when(blockedPeriodRepository.findAllByOrderByIdDesc())
            .thenReturn(List.of(buildBlock(2L, BlockType.RECURRING), buildBlock(1L, BlockType.ONE_OFF)));

        List<BlockedPeriodResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
    }

    @Test
    void shouldFindBlockById() {
        when(blockedPeriodRepository.findById(1L)).thenReturn(Optional.of(buildBlock(1L, BlockType.ONE_OFF)));

        BlockedPeriodResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenBlockNotFoundById() {
        when(blockedPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Blocked period not found with id: 999");
    }

    // ─── UPDATE ───────────────────────────────────────

    @Test
    void shouldUpdateBlock() {
        BlockedPeriod existing = buildBlock(1L, BlockType.ONE_OFF);
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.ONE_OFF, LocalDate.of(2026, 12, 25), null, null, null, "Christmas prep");

        when(blockedPeriodRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(blockedPeriodRepository.save(any(BlockedPeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        BlockedPeriodResponse response = service.update(1L, request);

        assertThat(response.reason()).isEqualTo("Christmas prep");
        assertThat(response.specificDate()).isEqualTo(LocalDate.of(2026, 12, 25));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentBlock() {
        BlockedPeriodRequest request = new BlockedPeriodRequest(
            1L, BlockType.ONE_OFF, LocalDate.of(2026, 11, 15), null, null, null, "Staff meeting");
        when(blockedPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Blocked period not found with id: 999");

        verify(blockedPeriodRepository, never()).save(any());
    }

    // ─── DELETE ───────────────────────────────────────

    @Test
    void shouldDeleteBlock() {
        when(blockedPeriodRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(blockedPeriodRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentBlock() {
        when(blockedPeriodRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Blocked period not found with id: 999");

        verify(blockedPeriodRepository, never()).deleteById(any());
    }
}
