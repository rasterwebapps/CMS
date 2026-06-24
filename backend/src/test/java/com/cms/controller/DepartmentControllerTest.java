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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.SpecialityRequest;
import com.cms.dto.SpecialityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.SpecialityService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SpecialityController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpecialityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SpecialityService specialityService;

    @Test
    void shouldCreateSpeciality() throws Exception {
        SpecialityRequest request = new SpecialityRequest(
            "Computer Science", "CS", "Speciality of Computer Science", null, null
        );

        Instant now = Instant.now();
        SpecialityResponse response = new SpecialityResponse(
            1L, "Computer Science", "CS", "Speciality of Computer Science", null, null, now, now
        );

        when(specialityService.create(any(SpecialityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/specialities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Science"))
            .andExpect(jsonPath("$.code").value("CS"))
            .andExpect(jsonPath("$.description").value("Speciality of Computer Science"));

        verify(specialityService).create(any(SpecialityRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        SpecialityRequest request = new SpecialityRequest("", "CS", "Description", null, null);

        mockMvc.perform(post("/specialities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        SpecialityRequest request = new SpecialityRequest("Computer Science", "", "Description", null, null);

        mockMvc.perform(post("/specialities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenNameIsNull() throws Exception {
        String jsonRequest = """
            {
                "code": "CS",
                "description": "Description"
            }
            """;

        mockMvc.perform(post("/specialities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllSpecialities() throws Exception {
        Instant now = Instant.now();
        SpecialityResponse dept1 = new SpecialityResponse(
            1L, "Computer Science", "CS", "CS Dept", null, null, now, now
        );
        SpecialityResponse dept2 = new SpecialityResponse(
            2L, "Mathematics", "MATH", "Math Dept", null, null, now, now
        );

        when(specialityService.findAll()).thenReturn(List.of(dept1, dept2));

        mockMvc.perform(get("/specialities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Computer Science"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Mathematics"));

        verify(specialityService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoSpecialities() throws Exception {
        when(specialityService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/specialities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(specialityService).findAll();
    }

    @Test
    void shouldFindSpecialityById() throws Exception {
        Instant now = Instant.now();
        SpecialityResponse response = new SpecialityResponse(
            1L, "Computer Science", "CS", "Speciality of Computer Science", null, null, now, now
        );

        when(specialityService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/specialities/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Science"))
            .andExpect(jsonPath("$.code").value("CS"));

        verify(specialityService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenSpecialityNotExists() throws Exception {
        when(specialityService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Speciality not found with id: 999"));

        mockMvc.perform(get("/specialities/999"))
            .andExpect(status().isNotFound());

        verify(specialityService).findById(999L);
    }

    @Test
    void shouldUpdateSpeciality() throws Exception {
        SpecialityRequest request = new SpecialityRequest(
            "Computer Science Updated", "CSU", "Updated Description", null, null
        );

        Instant now = Instant.now();
        SpecialityResponse response = new SpecialityResponse(
            1L, "Computer Science Updated", "CSU", "Updated Description", null, null, now, now
        );

        when(specialityService.update(eq(1L), any(SpecialityRequest.class))).thenReturn(response);

        mockMvc.perform(put("/specialities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Science Updated"))
            .andExpect(jsonPath("$.code").value("CSU"));

        verify(specialityService).update(eq(1L), any(SpecialityRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentSpeciality() throws Exception {
        SpecialityRequest request = new SpecialityRequest("Name", "CODE", "Description", null, null);

        when(specialityService.update(eq(999L), any(SpecialityRequest.class)))
            .thenThrow(new ResourceNotFoundException("Speciality not found with id: 999"));

        mockMvc.perform(put("/specialities/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(specialityService).update(eq(999L), any(SpecialityRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        SpecialityRequest request = new SpecialityRequest("", "", "Description", null, null);

        mockMvc.perform(put("/specialities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteSpeciality() throws Exception {
        doNothing().when(specialityService).delete(1L);

        mockMvc.perform(delete("/specialities/1"))
            .andExpect(status().isNoContent());

        verify(specialityService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentSpeciality() throws Exception {
        doThrow(new ResourceNotFoundException("Speciality not found with id: 999"))
            .when(specialityService).delete(999L);

        mockMvc.perform(delete("/specialities/999"))
            .andExpect(status().isNotFound());

        verify(specialityService).delete(999L);
    }
}
