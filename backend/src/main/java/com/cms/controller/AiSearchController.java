package com.cms.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.cms.ai.AiStudentSearchService;
import com.cms.ai.rag.DocumentRagSearchService;
import com.cms.dto.AdmissionDocumentRagResult;
import com.cms.dto.AiDocumentSearchRequest;
import com.cms.dto.AiStudentSearchRequest;
import com.cms.dto.StudentResponse;

/**
 * AI Smart Search: natural-language search over Student records via a locally-hosted Ollama
 * model. See {@code com.cms.ai} package for the parse -> translate -> execute pipeline and its
 * allow-list safety boundary. Also hosts the Admission document RAG search (OC-277) -- a
 * different pipeline (retrieval, not a fixed-field filter), same "local Ollama only" boundary.
 */
@RestController
@RequestMapping("/ai-search")
public class AiSearchController {

    private final AiStudentSearchService aiStudentSearchService;
    private final DocumentRagSearchService documentRagSearchService;

    public AiSearchController(AiStudentSearchService aiStudentSearchService,
                               DocumentRagSearchService documentRagSearchService) {
        this.aiStudentSearchService = aiStudentSearchService;
        this.documentRagSearchService = documentRagSearchService;
    }

    @PostMapping("/students")
    @PreAuthorize("@perm.has('STUDENT_AI_SEARCH_VIEW')")
    public Page<StudentResponse> searchStudents(
            @RequestBody AiStudentSearchRequest request,
            @PageableDefault(size = 25, sort = "admissionNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return aiStudentSearchService.search(request.query(), pageable);
    }

    @PostMapping("/admission-documents")
    @PreAuthorize("@perm.has('ADMISSION_DOCUMENT_AI_SEARCH_VIEW')")
    public List<AdmissionDocumentRagResult> searchAdmissionDocuments(@RequestBody AiDocumentSearchRequest request) {
        return documentRagSearchService.search(request.query());
    }
}
