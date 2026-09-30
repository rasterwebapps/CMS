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

import com.cms.ai.AiStudentSearchService;
import com.cms.dto.AiStudentSearchRequest;
import com.cms.dto.StudentResponse;

/**
 * AI Smart Search: natural-language search over Student records via a locally-hosted Ollama
 * model. See {@code com.cms.ai} package for the parse -> translate -> execute pipeline and its
 * allow-list safety boundary.
 */
@RestController
@RequestMapping("/ai-search")
public class AiSearchController {

    private final AiStudentSearchService aiStudentSearchService;

    public AiSearchController(AiStudentSearchService aiStudentSearchService) {
        this.aiStudentSearchService = aiStudentSearchService;
    }

    @PostMapping("/students")
    @PreAuthorize("@perm.has('STUDENT_AI_SEARCH_VIEW')")
    public Page<StudentResponse> searchStudents(
            @RequestBody AiStudentSearchRequest request,
            @PageableDefault(size = 25, sort = "admissionNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return aiStudentSearchService.search(request.query(), pageable);
    }
}
