package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ImportDefaultsRequest;
import com.cms.dto.ImportExecuteResult;
import com.cms.dto.ImportValidationResult;
import com.cms.service.ExcelTemplateService;
import com.cms.service.StudentImportService;

@WebMvcTest(controllers = ImportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExcelTemplateService templateService;

    @MockitoBean
    private StudentImportService importService;

    @Test
    void shouldDownloadTemplate() throws Exception {
        byte[] dummyBytes = new byte[]{1, 2, 3};
        when(templateService.generateTemplate()).thenReturn(dummyBytes);

        mockMvc.perform(get("/import/template"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(dummyBytes));
    }

    @Test
    void shouldValidateFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1, 2, 3});

        ImportValidationResult result = new ImportValidationResult(
            10, 9, 10, 10, 5, 5, List.of(), List.of());
        when(importService.validate(any(), any(ImportDefaultsRequest.class))).thenReturn(result);

        mockMvc.perform(multipart("/import/validate").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentsTotal").value(10))
            .andExpect(jsonPath("$.studentsValid").value(9));
    }

    @Test
    void shouldExecuteImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1, 2, 3});

        ImportExecuteResult result = new ImportExecuteResult(9, 1, 9, 9, 9, 0, List.of());
        when(importService.execute(any(), any(ImportDefaultsRequest.class), anyString())).thenReturn(result);

        mockMvc.perform(multipart("/import/execute")
                .file(file)
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentsImported").value(9))
            .andExpect(jsonPath("$.studentsSkipped").value(1));
    }

    @Test
    void shouldExecuteImportWithNullJwt() throws Exception {
        // Even without JWT, the controller should handle null jwt gracefully (uses "import" as username)
        MockMultipartFile file = new MockMultipartFile(
            "file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1, 2, 3});

        ImportExecuteResult result = new ImportExecuteResult(5, 0, 5, 5, 5, 0, List.of());
        when(importService.execute(any(), any(ImportDefaultsRequest.class), anyString())).thenReturn(result);

        mockMvc.perform(multipart("/import/execute").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentsImported").value(5));
    }
}

