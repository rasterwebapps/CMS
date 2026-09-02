package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.DocumentFileDownload;
import com.cms.dto.FacultyDocumentRequest;
import com.cms.dto.FacultyDocumentResponse;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.service.FacultyDocumentService;
import com.cms.service.FacultyDocumentTypeRequirementService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = FacultyDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class FacultyDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FacultyDocumentService documentService;

    @MockitoBean
    private FacultyDocumentTypeRequirementService documentTypeRequirementService;

    @Test
    void shouldListDocumentsForFaculty() throws Exception {
        Instant now = Instant.now();
        FacultyDocumentResponse response = new FacultyDocumentResponse(
            5L, 10L, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED,
            null, null, null, now, now,
            "pan.pdf", MediaType.APPLICATION_PDF_VALUE, 11L, now, true
        );

        when(documentService.findByFacultyId(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/faculty/10/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(5))
            .andExpect(jsonPath("$[0].facultyId").value(10))
            .andExpect(jsonPath("$[0].documentType").value("PAN_CARD"))
            .andExpect(jsonPath("$[0].hasFile").value(true));

        verify(documentService).findByFacultyId(10L);
    }

    @Test
    void shouldCreateDocument() throws Exception {
        Instant now = Instant.now();
        FacultyDocumentRequest request = new FacultyDocumentRequest(DocumentType.UG_DEGREE, null, "remarks");
        FacultyDocumentResponse response = new FacultyDocumentResponse(
            1L, 10L, DocumentType.UG_DEGREE, DocumentVerificationStatus.NOT_UPLOADED,
            "remarks", null, null, now, now,
            null, null, null, null, false
        );

        when(documentService.addDocument(eq(10L), any(FacultyDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/faculty/10/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.facultyId").value(10))
            .andExpect(jsonPath("$.documentType").value("UG_DEGREE"))
            .andExpect(jsonPath("$.status").value("NOT_UPLOADED"));

        verify(documentService).addDocument(eq(10L), any(FacultyDocumentRequest.class));
    }

    @Test
    void shouldUpdateDocument() throws Exception {
        Instant now = Instant.now();
        FacultyDocumentRequest request = new FacultyDocumentRequest(DocumentType.PG_DEGREE, DocumentVerificationStatus.UPLOADED, "ok");
        FacultyDocumentResponse response = new FacultyDocumentResponse(
            7L, 10L, DocumentType.PG_DEGREE, DocumentVerificationStatus.UPLOADED,
            "ok", null, null, now, now,
            null, null, null, null, false
        );

        when(documentService.updateDocument(eq(7L), any(FacultyDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/faculty/10/documents/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.documentType").value("PG_DEGREE"));

        verify(documentService).updateDocument(eq(7L), any(FacultyDocumentRequest.class));
    }

    @Test
    void shouldDeleteDocument() throws Exception {
        doNothing().when(documentService).deleteDocument(9L);

        mockMvc.perform(delete("/faculty/10/documents/9"))
            .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(9L);
    }

    @Test
    void shouldUploadDocumentFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "scan.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF-CONTENT".getBytes()
        );

        Instant now = Instant.now();
        FacultyDocumentResponse response = new FacultyDocumentResponse(
            5L, 10L, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED,
            null, null, null, now, now,
            "scan.pdf", MediaType.APPLICATION_PDF_VALUE, 11L, now, true
        );

        when(documentService.uploadFile(eq(10L), eq(DocumentType.PAN_CARD), eq("note"), any(), eq(false)))
            .thenReturn(response);

        mockMvc.perform(multipart("/faculty/10/documents/upload")
                .file(file)
                .param("documentType", "PAN_CARD")
                .param("remarks", "note"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.fileName").value("scan.pdf"))
            .andExpect(jsonPath("$.hasFile").value(true))
            .andExpect(jsonPath("$.status").value("UPLOADED"));

        verify(documentService).uploadFile(eq(10L), eq(DocumentType.PAN_CARD), eq("note"), any(), eq(false));
    }

    @Test
    void shouldDownloadDocumentFile() throws Exception {
        byte[] data = "PDF-CONTENT".getBytes();
        when(documentService.getFileForDownload(5L))
            .thenReturn(new DocumentFileDownload("scan.pdf", MediaType.APPLICATION_PDF_VALUE, data));

        mockMvc.perform(get("/faculty/10/documents/5/download"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", Matchers.containsString("scan.pdf")))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(data));
    }

    @Test
    void shouldSanitizeFileNameForHeader() throws Exception {
        byte[] data = "X".getBytes();
        when(documentService.getFileForDownload(7L))
            .thenReturn(new DocumentFileDownload("weird \"name\"\r\n.pdf", MediaType.APPLICATION_PDF_VALUE, data));

        mockMvc.perform(get("/faculty/10/documents/7/download"))
            .andExpect(status().isOk())
            // Quote + CR/LF characters in the ASCII fallback must be replaced with underscores.
            .andExpect(header().string("Content-Disposition", Matchers.containsString("weird _name___.pdf")))
            .andExpect(header().string("Content-Disposition", Matchers.not(Matchers.containsString("\"name\""))));
    }
}


