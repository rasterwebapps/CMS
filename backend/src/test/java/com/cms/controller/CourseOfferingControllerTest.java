package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.service.CourseOfferingSectionFacultyService;
import com.cms.service.CourseOfferingService;
import com.cms.service.TimetableGlobalAutoScheduleService;

@WebMvcTest(controllers = CourseOfferingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseOfferingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseOfferingService service;

    @MockitoBean
    private TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    @MockitoBean
    private CourseOfferingSectionFacultyService sectionFacultyService;

    private CourseOfferingDto createDto(Long id, Long termInstanceId, Integer semNum) {
        return new CourseOfferingDto(
            id, termInstanceId, "2024-2025 ODD",
            1L, "CV-2024",
            1L, "Mathematics", "MATH101", null, null, List.of(),
            semNum, true,
            null, false, com.cms.model.enums.SubjectType.CORE,
            null, null, null,
            0, 0, 0,
            null,
            null, null,
            Instant.now(), Instant.now(),
            List.of()
        );
    }

    @Test
    void getOfferingsByTermInstance() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.getOfferingsByTermInstance(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/course-offerings").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].termNumber").value(1));

        verify(service).getOfferingsByTermInstance(1L);
    }

    @Test
    void getOfferingsByTermInstanceAndSemester() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.getOfferingsByTermInstanceAndSemester(1L, 1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/course-offerings")
                .param("termInstanceId", "1")
                .param("termNumber", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(service).getOfferingsByTermInstanceAndSemester(1L, 1);
    }

    @Test
    void getById() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/course-offerings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.subjectCode").value("MATH101"));

        verify(service).getById(1L);
    }

    @Test
    void generate() throws Exception {
        when(service.generateOfferingsForTermInstance(1L))
            .thenReturn(new GenerateOfferingsResponse(3, 2, List.of(), 0, 0, List.of()));

        mockMvc.perform(post("/course-offerings/generate").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offeringsCreated").value(3));

        verify(service).generateOfferingsForTermInstance(1L);
    }

    @Test
    void upsertCohortFaculty() throws Exception {
        com.cms.dto.SectionFacultyAssignment assignment =
            new com.cms.dto.SectionFacultyAssignment(9L, null, "2023-2027 Batch", null, 42L, "Jane Doe", 0L);
        when(sectionFacultyService.upsertForCohort(1L, 9L, 42L, null)).thenReturn(assignment);

        mockMvc.perform(put("/course-offerings/1/cohort-faculty/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"facultyId\":42}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.facultyId").value(42));

        verify(sectionFacultyService).upsertForCohort(1L, 9L, 42L, null);
    }

    @Test
    void checkFacultyCapacityForCohort() throws Exception {
        com.cms.dto.FacultyCapacityCheckResult check =
            new com.cms.dto.FacultyCapacityCheckResult(false, 0, 10, 10, 100, 5, "NONE", 100, 0, List.of());
        when(timetableGlobalAutoScheduleService.checkFacultyCapacityForCohort(1L, 9L, 42L)).thenReturn(check);

        mockMvc.perform(get("/course-offerings/1/cohort-faculty-capacity-check")
                .param("cohortId", "9")
                .param("facultyId", "42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overCapacity").value(false));

        verify(timetableGlobalAutoScheduleService).checkFacultyCapacityForCohort(1L, 9L, 42L);
    }

    @Test
    void updateStatus() throws Exception {
        when(service.updateStatus(eq(1L), any())).thenReturn(new ActiveStatusUpdateResponse(1L, false, Instant.now()));

        mockMvc.perform(patch("/course-offerings/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
    }

}
