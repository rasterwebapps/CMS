package com.cms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.CourseRegistrationDto;
import com.cms.model.enums.RegistrationStatus;
import com.cms.service.CourseRegistrationService;

@WebMvcTest(controllers = CourseRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseRegistrationService service;

    private CourseRegistrationDto createDto(Long id, Long enrollmentId, Long offeringId) {
        return new CourseRegistrationDto(
            id, enrollmentId, 1L, "John Doe", "BCA-2024-2027",
            offeringId, "Mathematics", "MATH101", 1,
            RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        );
    }

    @Test
    void getRegistrationsByEnrollment() throws Exception {
        CourseRegistrationDto dto = createDto(1L, 1L, 1L);
        when(service.getRegistrationsByEnrollment(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/course-registrations").param("enrollmentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].status").value("REGISTERED"));

        verify(service).getRegistrationsByEnrollment(1L);
    }

    @Test
    void getRegistrationsByCourseOffering() throws Exception {
        CourseRegistrationDto dto = createDto(1L, 1L, 1L);
        when(service.getRegistrationsByCourseOffering(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/course-registrations").param("courseOfferingId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(service).getRegistrationsByCourseOffering(1L);
    }

    @Test
    void getRegistrations_badRequestWhenNoParams() throws Exception {
        mockMvc.perform(get("/api/course-registrations"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getById() throws Exception {
        CourseRegistrationDto dto = createDto(1L, 1L, 1L);
        when(service.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/course-registrations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"));

        verify(service).getById(1L);
    }

    @Test
    void generate() throws Exception {
        when(service.generateRegistrationsForTermInstance(1L)).thenReturn(5);

        mockMvc.perform(post("/api/course-registrations/generate").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.registrationsCreated").value(5));

        verify(service).generateRegistrationsForTermInstance(1L);
    }

    @Test
    void drop() throws Exception {
        CourseRegistrationDto dto = new CourseRegistrationDto(
            1L, 1L, 1L, "John Doe", "BCA-2024-2027",
            1L, "Mathematics", "MATH101", 1,
            RegistrationStatus.DROPPED, Instant.now(), Instant.now()
        );
        when(service.dropRegistration(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/course-registrations/1/drop"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DROPPED"));

        verify(service).dropRegistration(1L);
    }
}
