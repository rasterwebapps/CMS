package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableGenerationResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.ClassScheduleService;
import com.cms.service.PersonalTimetableService;
import com.cms.service.ProfileService;
import com.cms.service.TimetableGenerationService;

@WebMvcTest(controllers = TimetableController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimetableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimetableGenerationService timetableGenerationService;

    @MockitoBean
    private ClassScheduleService classScheduleService;

    @MockitoBean
    private PersonalTimetableService personalTimetableService;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void shouldGenerateTimetable() throws Exception {
        TimetableGenerationResponse response = new TimetableGenerationResponse(3, Collections.emptyList());
        when(timetableGenerationService.generate(10L)).thenReturn(response);

        mockMvc.perform(post("/timetables/generate").param("termInstanceId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generatedCount").value(3))
            .andExpect(jsonPath("$.unplaceable").isEmpty());

        verify(timetableGenerationService).generate(10L);
    }

    @Test
    void shouldReturnConflictWhenTimetableAlreadyExists() throws Exception {
        when(timetableGenerationService.generate(10L)).thenThrow(
            new LifecycleConflictException("A timetable already exists for this term. Clear it before regenerating.",
                "TIMETABLE_ALREADY_EXISTS", "TermInstance", 10L, null));

        mockMvc.perform(post("/timetables/generate").param("termInstanceId", "10"))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldFindDraftRows() throws Exception {
        when(classScheduleService.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());

        mockMvc.perform(get("/timetables/draft").param("termInstanceId", "10"))
            .andExpect(status().isOk());

        verify(classScheduleService).findByTermInstanceIdAndStatus(eq(10L), any());
    }

    @Test
    void shouldFindPublishedRows() throws Exception {
        when(classScheduleService.findByTermInstanceIdAndStatus(eq(10L), any())).thenReturn(List.of());

        mockMvc.perform(get("/timetables").param("termInstanceId", "10"))
            .andExpect(status().isOk());

        verify(classScheduleService).findByTermInstanceIdAndStatus(eq(10L), any());
    }

    @Test
    void shouldApproveDraftTimetable() throws Exception {
        when(timetableGenerationService.approve(10L)).thenReturn(new TimetableActionResponse(5));

        mockMvc.perform(post("/timetables/10/approve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affectedCount").value(5));

        verify(timetableGenerationService).approve(10L);
    }

    @Test
    void shouldReturnNotFoundWhenApprovingWithNoDrafts() throws Exception {
        when(timetableGenerationService.approve(999L))
            .thenThrow(new ResourceNotFoundException("No draft timetable found for term instance id: 999"));

        mockMvc.perform(post("/timetables/999/approve"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldClearTimetable() throws Exception {
        when(timetableGenerationService.clear(10L)).thenReturn(new TimetableActionResponse(7));

        mockMvc.perform(delete("/timetables/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affectedCount").value(7));

        verify(timetableGenerationService).clear(10L);
    }

    @Test
    void shouldFindMyTimetable() throws Exception {
        ProfileIdentity identity = new ProfileIdentity("FACULTY", 5L, null, null, "Dr. Faculty", null, null, null, null);
        when(profileService.resolveCurrentUser()).thenReturn(identity);
        when(personalTimetableService.findMyTimetable(eq(identity), eq(10L), any()))
            .thenReturn(new MyTimetableResponse(List.of(), List.of()));

        mockMvc.perform(get("/timetables/me").param("termInstanceId", "10"))
            .andExpect(status().isOk());

        verify(personalTimetableService).findMyTimetable(eq(identity), eq(10L), any());
    }
}
