package com.cms.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cms.dto.StudentResponse;
import com.cms.service.StudentService;

/**
 * Orchestrates AI Smart Search: parse the free-text query into a validated {@link SearchIntent}
 * (local-LLM-backed, allow-list-validated), translate it into a {@code Specification<Student>},
 * and execute it via {@link StudentService#findBySpecification}, reusing the exact same
 * fetch/enrichment/DTO pipeline every other student list screen uses.
 */
@Service
public class AiStudentSearchService {

    private final StudentSearchIntentParser intentParser;
    private final StudentSearchIntentTranslator intentTranslator;
    private final StudentService studentService;

    public AiStudentSearchService(StudentSearchIntentParser intentParser,
                                   StudentSearchIntentTranslator intentTranslator,
                                   StudentService studentService) {
        this.intentParser = intentParser;
        this.intentTranslator = intentTranslator;
        this.studentService = studentService;
    }

    public Page<StudentResponse> search(String query, Pageable pageable) {
        SearchIntent intent = intentParser.parse(query);
        var spec = intentTranslator.translate(intent);
        return studentService.findBySpecification(spec, pageable);
    }
}
