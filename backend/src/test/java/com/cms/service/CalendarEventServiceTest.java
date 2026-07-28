package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CalendarEventRequest;
import com.cms.dto.CalendarEventResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.CalendarEvent;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.HolidayCategory;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    private CalendarEventService calendarEventService;

    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        calendarEventService = new CalendarEventService(
            calendarEventRepository, academicYearRepository);
        academicYear = new AcademicYear(
            "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);
        academicYear.setId(1L);
        Instant now = Instant.now();
        academicYear.setCreatedAt(now);
        academicYear.setUpdatedAt(now);
    }

    private CalendarEvent buildEvent(Long id, String title, CalendarEventType type, AcademicYear ay) {
        CalendarEvent e = new CalendarEvent();
        e.setId(id);
        e.setTitle(title);
        e.setDescription("desc");
        e.setStartDate(LocalDate.of(2024, 10, 1));
        e.setEndDate(LocalDate.of(2024, 10, 1));
        e.setEventType(type);
        e.setAcademicYear(ay);
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    // ─── CREATE ──────────────────────────────────────

    @Test
    void shouldCreateCalendarEvent() {
        CalendarEventRequest request = new CalendarEventRequest(
            "Diwali", "Holiday", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.HOLIDAY, 1L);

        CalendarEvent saved = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(saved);

        CalendarEventResponse response = calendarEventService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Diwali");
        assertThat(response.eventType()).isEqualTo(CalendarEventType.HOLIDAY);
        verify(calendarEventRepository).save(any(CalendarEvent.class));
    }

    @Test
    void shouldThrowWhenCreatingWithNonExistentAcademicYear() {
        CalendarEventRequest request = new CalendarEventRequest(
            "Diwali", null, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.HOLIDAY, 999L);

        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Academic year not found with id: 999");

        verify(calendarEventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEndDateBeforeStartDate() {
        CalendarEventRequest request = new CalendarEventRequest(
            "Event", null, LocalDate.of(2024, 10, 5), LocalDate.of(2024, 10, 1),
            CalendarEventType.OTHER, 1L);

        assertThatThrownBy(() -> calendarEventService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must not be before start date");
    }

    @Test
    void shouldStoreHolidayCategoryForHolidayEvent() {
        CalendarEventRequest request = new CalendarEventRequest(
            "Independence Day", "National holiday", LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 15),
            CalendarEventType.HOLIDAY, 1L, HolidayCategory.GOVERNMENT);

        CalendarEvent saved = buildEvent(1L, "Independence Day", CalendarEventType.HOLIDAY, academicYear);
        saved.setHolidayCategory(HolidayCategory.GOVERNMENT);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(saved);

        CalendarEventResponse response = calendarEventService.create(request);

        assertThat(response.holidayCategory()).isEqualTo(HolidayCategory.GOVERNMENT);
    }

    @Test
    void shouldDropHolidayCategoryForNonHolidayEvent() {
        // A category only makes sense for HOLIDAY events -- passing one for e.g. an EXAM event
        // must be silently ignored rather than stored, so a later type-change to HOLIDAY doesn't
        // resurrect a stale category from an unrelated event.
        CalendarEventRequest request = new CalendarEventRequest(
            "Mid-Term", null, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.EXAM, 1L, HolidayCategory.LOCAL);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        CalendarEventResponse response = calendarEventService.create(request);

        assertThat(response.holidayCategory()).isNull();
    }

    // ─── FIND ALL ─────────────────────────────────────

    @Test
    void shouldFindAllEvents() {
        CalendarEvent e1 = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);
        CalendarEvent e2 = buildEvent(2L, "Exam",   CalendarEventType.EXAM,    academicYear);
        when(calendarEventRepository.findAll()).thenReturn(List.of(e1, e2));

        List<CalendarEventResponse> responses = calendarEventService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("Diwali");
        assertThat(responses.get(1).title()).isEqualTo("Exam");
    }

    @Test
    void shouldReturnEmptyListWhenNoEvents() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        assertThat(calendarEventService.findAll()).isEmpty();
    }

    // ─── FIND BY ID ───────────────────────────────────

    @Test
    void shouldFindEventById() {
        CalendarEvent event = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);
        when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(event));

        CalendarEventResponse response = calendarEventService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Diwali");
    }

    @Test
    void shouldThrowWhenEventNotFoundById() {
        when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Calendar event not found with id: 999");
    }

    // ─── FIND BY ACADEMIC YEAR ────────────────────────

    @Test
    void shouldFindEventsByAcademicYear() {
        CalendarEvent e1 = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);
        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(calendarEventRepository.findByAcademicYearIdOrderByStartDate(1L))
            .thenReturn(List.of(e1));

        List<CalendarEventResponse> responses = calendarEventService.findByAcademicYearId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Diwali");
    }

    @Test
    void shouldThrowWhenFindingByNonExistentAcademicYear() {
        when(academicYearRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> calendarEventService.findByAcademicYearId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Academic year not found with id: 999");
    }

    @Test
    void shouldFindEventsByAcademicYearAndEventType() {
        CalendarEvent e1 = buildEvent(1L, "Mid-Term", CalendarEventType.EXAM, academicYear);
        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(calendarEventRepository.findByAcademicYearIdAndEventTypeOrderByStartDate(
            1L, CalendarEventType.EXAM)).thenReturn(List.of(e1));

        List<CalendarEventResponse> responses =
            calendarEventService.findByAcademicYearIdAndEventType(1L, CalendarEventType.EXAM);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventType()).isEqualTo(CalendarEventType.EXAM);
    }

    // ─── UPDATE ───────────────────────────────────────

    @Test
    void shouldUpdateCalendarEvent() {
        CalendarEvent existing = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);
        CalendarEventRequest request = new CalendarEventRequest(
            "Diwali Updated", "desc", LocalDate.of(2024, 10, 2), LocalDate.of(2024, 10, 2),
            CalendarEventType.HOLIDAY, 1L);
        CalendarEvent updated = buildEvent(1L, "Diwali Updated", CalendarEventType.HOLIDAY, academicYear);

        when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenReturn(updated);

        CalendarEventResponse response = calendarEventService.update(1L, request);

        assertThat(response.title()).isEqualTo("Diwali Updated");
        verify(calendarEventRepository).save(any(CalendarEvent.class));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentEvent() {
        CalendarEventRequest request = new CalendarEventRequest(
            "Test", null, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.OTHER, 1L);
        when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Calendar event not found with id: 999");

        verify(calendarEventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingWithNonExistentAcademicYear() {
        CalendarEvent existing = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);
        CalendarEventRequest request = new CalendarEventRequest(
            "Test", null, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.HOLIDAY, 999L);

        when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Academic year not found with id: 999");
    }

    @Test
    void shouldThrowWhenUpdatingWithInvalidDateRange() {
        CalendarEvent existing = buildEvent(1L, "Diwali", CalendarEventType.HOLIDAY, academicYear);
        CalendarEventRequest request = new CalendarEventRequest(
            "Diwali", null, LocalDate.of(2024, 10, 5), LocalDate.of(2024, 10, 1),
            CalendarEventType.HOLIDAY, 1L);

        when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> calendarEventService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must not be before start date");
    }

    // ─── DELETE ───────────────────────────────────────

    @Test
    void shouldDeleteEvent() {
        when(calendarEventRepository.existsById(1L)).thenReturn(true);

        calendarEventService.delete(1L);

        verify(calendarEventRepository).existsById(1L);
        verify(calendarEventRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentEvent() {
        when(calendarEventRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> calendarEventService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Calendar event not found with id: 999");

        verify(calendarEventRepository, never()).deleteById(any());
    }
}
