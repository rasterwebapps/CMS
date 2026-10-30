package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.LibrarySubjectCategoryRequest;
import com.cms.dto.LibrarySubjectCategoryResponse;
import com.cms.service.LibrarySubjectCategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LibrarySubjectCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class LibrarySubjectCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LibrarySubjectCategoryService categoryService;

    @Test
    void shouldCreateCategory() throws Exception {
        var request = new LibrarySubjectCategoryRequest("Pharmacology", "PHARM", "Drugs", true);
        var response = new LibrarySubjectCategoryResponse(1L, "Pharmacology", "PHARM", "Drugs", true, Instant.now(), Instant.now());

        when(categoryService.create(any(LibrarySubjectCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/library/subject-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Pharmacology"))
            .andExpect(jsonPath("$.code").value("PHARM"));
    }

    @Test
    void shouldFindAllCategories() throws Exception {
        var response = new LibrarySubjectCategoryResponse(1L, "Biochemistry", "BIO", null, true, Instant.now(), Instant.now());
        when(categoryService.findAll(true)).thenReturn(List.of(response));

        mockMvc.perform(get("/library/subject-categories").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Biochemistry"));
    }

    @Test
    void shouldFindById() throws Exception {
        var response = new LibrarySubjectCategoryResponse(1L, "Biochemistry", "BIO", null, true, Instant.now(), Instant.now());
        when(categoryService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/library/subject-categories/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Biochemistry"));
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        var request = new LibrarySubjectCategoryRequest("Biochemistry Updated", "BIO", null, true);
        var response = new LibrarySubjectCategoryResponse(1L, "Biochemistry Updated", "BIO", null, true, Instant.now(), Instant.now());
        when(categoryService.update(eq(1L), any(LibrarySubjectCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/library/subject-categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Biochemistry Updated"));
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/library/subject-categories/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        var request = new ActiveStatusUpdateRequest(false, "Deactivated");
        var response = new ActiveStatusUpdateResponse(1L, false, Instant.now());
        when(categoryService.updateStatus(eq(1L), any(ActiveStatusUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/library/subject-categories/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
    }
}
