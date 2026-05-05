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

import com.cms.dto.BloodGroupRequest;
import com.cms.dto.BloodGroupResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.BloodGroupService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = BloodGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class BloodGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BloodGroupService bloodGroupService;

    @Test
    void shouldCreateBloodGroup() throws Exception {
        BloodGroupRequest request = new BloodGroupRequest("A Positive", "A+", true);
        BloodGroupResponse response = createResponse(1L, "A Positive", "A+");

        when(bloodGroupService.create(any(BloodGroupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/blood-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("A Positive"))
            .andExpect(jsonPath("$.code").value("A+"));

        verify(bloodGroupService).create(any(BloodGroupRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        BloodGroupRequest request = new BloodGroupRequest("", "A+", true);

        mockMvc.perform(post("/blood-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        BloodGroupRequest request = new BloodGroupRequest("A Positive", "", true);

        mockMvc.perform(post("/blood-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAll() throws Exception {
        BloodGroupResponse response = createResponse(1L, "A Positive", "A+");

        when(bloodGroupService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/blood-groups"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(bloodGroupService).findAll();
    }

    @Test
    void shouldFindActiveOnly() throws Exception {
        BloodGroupResponse response = createResponse(1L, "A Positive", "A+");

        when(bloodGroupService.findActive()).thenReturn(List.of(response));

        mockMvc.perform(get("/blood-groups").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(bloodGroupService).findActive();
    }

    @Test
    void shouldFindById() throws Exception {
        BloodGroupResponse response = createResponse(1L, "A Positive", "A+");

        when(bloodGroupService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/blood-groups/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("A+"));

        verify(bloodGroupService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenBloodGroupNotExists() throws Exception {
        when(bloodGroupService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Blood group not found with id: 999"));

        mockMvc.perform(get("/blood-groups/999"))
            .andExpect(status().isNotFound());

        verify(bloodGroupService).findById(999L);
    }

    @Test
    void shouldUpdateBloodGroup() throws Exception {
        BloodGroupRequest request = new BloodGroupRequest("A Positive Updated", "A+", true);
        BloodGroupResponse response = createResponse(1L, "A Positive Updated", "A+");

        when(bloodGroupService.update(eq(1L), any(BloodGroupRequest.class))).thenReturn(response);

        mockMvc.perform(put("/blood-groups/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("A Positive Updated"));

        verify(bloodGroupService).update(eq(1L), any(BloodGroupRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentBloodGroup() throws Exception {
        BloodGroupRequest request = new BloodGroupRequest("Name", "CODE", null);

        when(bloodGroupService.update(eq(999L), any(BloodGroupRequest.class)))
            .thenThrow(new ResourceNotFoundException("Blood group not found with id: 999"));

        mockMvc.perform(put("/blood-groups/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(bloodGroupService).update(eq(999L), any(BloodGroupRequest.class));
    }

    @Test
    void shouldDeleteBloodGroup() throws Exception {
        doNothing().when(bloodGroupService).delete(1L);

        mockMvc.perform(delete("/blood-groups/1"))
            .andExpect(status().isNoContent());

        verify(bloodGroupService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentBloodGroup() throws Exception {
        doThrow(new ResourceNotFoundException("Blood group not found with id: 999"))
            .when(bloodGroupService).delete(999L);

        mockMvc.perform(delete("/blood-groups/999"))
            .andExpect(status().isNotFound());

        verify(bloodGroupService).delete(999L);
    }

    private BloodGroupResponse createResponse(Long id, String name, String code) {
        Instant now = Instant.now();
        return new BloodGroupResponse(id, name, code, true, now, now);
    }
}

