package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CalendarEventRequest;
import com.cms.dto.CalendarEventResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.CalendarEventType;
import com.cms.service.CalendarEventService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CalendarEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalendarEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CalendarEventService calendarEventService;

    private AcademicYearResponse buildAyResponse() {
        Instant now = Instant.now();
        return new AcademicYearResponse(
            1L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now);
    }

    private CalendarEventResponse buildEventResponse(Long id, String title,
            CalendarEventType type) {
        Instant now = Instant.now();
        return new CalendarEventResponse(
            id, title, "A description",
            LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            type, buildAyResponse(), null, now, now);
    }

    @Test
    void shouldCreateCalendarEvent() throws Exception {
        CalendarEventRequest request = new CalendarEventRequest(
            "Diwali", "Holiday", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.HOLIDAY, 1L, null);

        CalendarEventResponse response = buildEventResponse(1L, "Diwali", CalendarEventType.HOLIDAY);

        when(calendarEventService.create(any(CalendarEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Diwali"))
            .andExpect(jsonPath("$.eventType").value("HOLIDAY"));

        verify(calendarEventService).create(any(CalendarEventRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        CalendarEventRequest request = new CalendarEventRequest(
            "", null, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.HOLIDAY, 1L, null);

        mockMvc.perform(post("/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEventTypeIsNull() throws Exception {
        String json = """
            {"title":"Test","startDate":"2024-10-01","endDate":"2024-10-01","academicYearId":1}
            """;

        mockMvc.perform(post("/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllCalendarEvents() throws Exception {
        CalendarEventResponse e1 = buildEventResponse(1L, "Diwali", CalendarEventType.HOLIDAY);
        CalendarEventResponse e2 = buildEventResponse(2L, "Mid-Term Exam", CalendarEventType.EXAM);

        when(calendarEventService.findAll()).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/calendar-events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));

        verify(calendarEventService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoEvents() throws Exception {
        when(calendarEventService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/calendar-events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldFindCalendarEventById() throws Exception {
        CalendarEventResponse response = buildEventResponse(1L, "Diwali", CalendarEventType.HOLIDAY);
        when(calendarEventService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/calendar-events/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Diwali"));

        verify(calendarEventService).findById(1L);
    }

    @Test
    void shouldReturn404WhenEventNotFound() throws Exception {
        when(calendarEventService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Calendar event not found with id: 999"));

        mockMvc.perform(get("/calendar-events/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindEventsByAcademicYear() throws Exception {
        CalendarEventResponse e1 = buildEventResponse(1L, "Diwali", CalendarEventType.HOLIDAY);
        when(calendarEventService.findByAcademicYearId(1L)).thenReturn(List.of(e1));

        mockMvc.perform(get("/calendar-events/academic-year/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(calendarEventService).findByAcademicYearId(1L);
    }

    @Test
    void shouldFindEventsByAcademicYearAndEventType() throws Exception {
        CalendarEventResponse e1 = buildEventResponse(1L, "Mid-Term", CalendarEventType.EXAM);
        when(calendarEventService.findByAcademicYearIdAndEventType(1L, CalendarEventType.EXAM))
            .thenReturn(List.of(e1));

        mockMvc.perform(get("/calendar-events/academic-year/1?eventType=EXAM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].eventType").value("EXAM"));

        verify(calendarEventService).findByAcademicYearIdAndEventType(1L, CalendarEventType.EXAM);
    }

    @Test
    void shouldFindEventsBySemester() throws Exception {
        CalendarEventResponse e1 = buildEventResponse(1L, "Diwali", CalendarEventType.HOLIDAY);
        when(calendarEventService.findBySemesterId(2L)).thenReturn(List.of(e1));

        mockMvc.perform(get("/calendar-events/semester/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(calendarEventService).findBySemesterId(2L);
    }

    @Test
    void shouldUpdateCalendarEvent() throws Exception {
        CalendarEventRequest request = new CalendarEventRequest(
            "Diwali Updated", null, LocalDate.of(2024, 10, 2), LocalDate.of(2024, 10, 2),
            CalendarEventType.HOLIDAY, 1L, null);

        CalendarEventResponse response = buildEventResponse(1L, "Diwali Updated", CalendarEventType.HOLIDAY);
        when(calendarEventService.update(eq(1L), any(CalendarEventRequest.class))).thenReturn(response);

        mockMvc.perform(put("/calendar-events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Diwali Updated"));

        verify(calendarEventService).update(eq(1L), any(CalendarEventRequest.class));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentEvent() throws Exception {
        CalendarEventRequest request = new CalendarEventRequest(
            "Test", null, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1),
            CalendarEventType.OTHER, 1L, null);

        when(calendarEventService.update(eq(999L), any(CalendarEventRequest.class)))
            .thenThrow(new ResourceNotFoundException("Calendar event not found with id: 999"));

        mockMvc.perform(put("/calendar-events/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCalendarEvent() throws Exception {
        doNothing().when(calendarEventService).delete(1L);

        mockMvc.perform(delete("/calendar-events/1"))
            .andExpect(status().isNoContent());

        verify(calendarEventService).delete(1L);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentEvent() throws Exception {
        doThrow(new ResourceNotFoundException("Calendar event not found with id: 999"))
            .when(calendarEventService).delete(999L);

        mockMvc.perform(delete("/calendar-events/999"))
            .andExpect(status().isNotFound());
    }
}
