package com.cms.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.cms.dto.CourseOfferingDto;
import com.cms.service.CourseOfferingService;

@WebMvcTest(controllers = CourseOfferingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseOfferingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseOfferingService service;

    private CourseOfferingDto createDto(Long id, Long termInstanceId, Integer semNum) {
        return new CourseOfferingDto(
            id, termInstanceId, "2024-2025 ODD",
            1L, "CV-2024",
            1L, "Mathematics", "MATH101",
            semNum, null, null, true,
            Instant.now(), Instant.now()
        );
    }

    @Test
    void getOfferingsByTermInstance() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.getOfferingsByTermInstance(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/course-offerings").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].semesterNumber").value(1));

        verify(service).getOfferingsByTermInstance(1L);
    }

    @Test
    void getOfferingsByTermInstanceAndSemester() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.getOfferingsByTermInstanceAndSemester(1L, 1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/course-offerings")
                .param("termInstanceId", "1")
                .param("semesterNumber", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(service).getOfferingsByTermInstanceAndSemester(1L, 1);
    }

    @Test
    void getById() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/course-offerings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.subjectCode").value("MATH101"));

        verify(service).getById(1L);
    }

    @Test
    void generate() throws Exception {
        when(service.generateOfferingsForTermInstance(1L)).thenReturn(3);

        mockMvc.perform(post("/api/course-offerings/generate").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offeringsCreated").value(3));

        verify(service).generateOfferingsForTermInstance(1L);
    }

    @Test
    void update() throws Exception {
        CourseOfferingDto dto = createDto(1L, 1L, 1);
        when(service.updateOffering(1L, 42L, "A")).thenReturn(dto);

        mockMvc.perform(put("/api/course-offerings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"facultyId\":42,\"sectionLabel\":\"A\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(service).updateOffering(1L, 42L, "A");
    }

    @Test
    void deactivate() throws Exception {
        mockMvc.perform(delete("/api/course-offerings/1"))
            .andExpect(status().isNoContent());

        verify(service).deactivateOffering(1L);
    }
}
