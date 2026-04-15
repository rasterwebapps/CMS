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

import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.service.EnquiryDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = EnquiryDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnquiryDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnquiryDocumentService documentService;

    @Test
    void shouldAddDocument() throws Exception {
        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TENTH_MARKSHEET, null, "10th certificate"
        );

        EnquiryDocumentResponse response = createResponse(1L, 1L, DocumentType.TENTH_MARKSHEET);

        when(documentService.addDocument(eq(1L), any(EnquiryDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/enquiries/1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.documentType").value("TENTH_MARKSHEET"));

        verify(documentService).addDocument(eq(1L), any(EnquiryDocumentRequest.class));
    }

    @Test
    void shouldFindByEnquiryId() throws Exception {
        EnquiryDocumentResponse response = createResponse(1L, 1L, DocumentType.TENTH_MARKSHEET);

        when(documentService.findByEnquiryId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries/1/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(documentService).findByEnquiryId(1L);
    }

    @Test
    void shouldReturnNotFoundWhenEnquiryDoesNotExist() throws Exception {
        when(documentService.findByEnquiryId(999L))
            .thenThrow(new ResourceNotFoundException("Enquiry not found with id: 999"));

        mockMvc.perform(get("/enquiries/999/documents"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateDocument() throws Exception {
        EnquiryDocumentRequest request = new EnquiryDocumentRequest(
            DocumentType.TWELFTH_MARKSHEET, DocumentVerificationStatus.VERIFIED, "Verified"
        );

        EnquiryDocumentResponse response = new EnquiryDocumentResponse(
            1L, 1L, DocumentType.TWELFTH_MARKSHEET, DocumentVerificationStatus.VERIFIED,
            "Verified", null, null, Instant.now(), Instant.now()
        );

        when(documentService.updateDocument(eq(1L), any(EnquiryDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/enquiries/1/documents/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.documentType").value("TWELFTH_MARKSHEET"));

        verify(documentService).updateDocument(eq(1L), any(EnquiryDocumentRequest.class));
    }

    @Test
    void shouldDeleteDocument() throws Exception {
        doNothing().when(documentService).deleteDocument(1L);

        mockMvc.perform(delete("/enquiries/1/documents/1"))
            .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistent() throws Exception {
        doThrow(new ResourceNotFoundException("Document not found with id: 999"))
            .when(documentService).deleteDocument(999L);

        mockMvc.perform(delete("/enquiries/1/documents/999"))
            .andExpect(status().isNotFound());

        verify(documentService).deleteDocument(999L);
    }

    private EnquiryDocumentResponse createResponse(Long id, Long enquiryId, DocumentType type) {
        Instant now = Instant.now();
        return new EnquiryDocumentResponse(
            id, enquiryId, type, DocumentVerificationStatus.NOT_UPLOADED,
            null, null, null, now, now
        );
    }
}
