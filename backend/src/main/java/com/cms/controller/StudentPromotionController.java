package com.cms.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CohortTermOption;
import com.cms.dto.PromotionExecuteRequest;
import com.cms.dto.PromotionExecuteResponse;
import com.cms.dto.PromotionPreviewRequest;
import com.cms.dto.PromotionPreviewResponse;
import com.cms.dto.StudentPromotionDecisionDto;
import com.cms.service.StudentPromotionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/student-promotions")
public class StudentPromotionController {

    private final StudentPromotionService studentPromotionService;

    public StudentPromotionController(StudentPromotionService studentPromotionService) {
        this.studentPromotionService = studentPromotionService;
    }

    @GetMapping("/active-terms")
    @PreAuthorize("@perm.has('STUDENT_PROMOTION_VIEW')")
    public ResponseEntity<List<CohortTermOption>> activeTerms(@RequestParam Long cohortId) {
        return ResponseEntity.ok(studentPromotionService.getActiveTermsForCohort(cohortId));
    }

    @GetMapping("/suggested-next-term")
    @PreAuthorize("@perm.has('STUDENT_PROMOTION_VIEW')")
    public ResponseEntity<CohortTermOption> suggestedNextTerm(@RequestParam Long fromTermInstanceId) {
        CohortTermOption suggestion = studentPromotionService.suggestNextTerm(fromTermInstanceId);
        return suggestion != null ? ResponseEntity.ok(suggestion) : ResponseEntity.noContent().build();
    }

    @PostMapping("/preview")
    @PreAuthorize("@perm.has('STUDENT_PROMOTION_VIEW')")
    public ResponseEntity<PromotionPreviewResponse> preview(@Valid @RequestBody PromotionPreviewRequest request) {
        return ResponseEntity.ok(studentPromotionService.previewPromotion(request));
    }

    @PostMapping("/execute")
    @PreAuthorize("@perm.has('STUDENT_PROMOTION_MANAGE')")
    public ResponseEntity<PromotionExecuteResponse> execute(@Valid @RequestBody PromotionExecuteRequest request,
                                                              Principal principal) {
        String decidedBy = principal != null ? principal.getName() : "system";
        return ResponseEntity.ok(studentPromotionService.executePromotion(request, decidedBy));
    }

    @GetMapping("/history")
    @PreAuthorize("@perm.has('STUDENT_PROMOTION_VIEW')")
    public ResponseEntity<List<StudentPromotionDecisionDto>> history(
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) Long studentId) {
        if (studentId != null) {
            return ResponseEntity.ok(studentPromotionService.getHistoryByStudent(studentId));
        } else if (cohortId != null) {
            return ResponseEntity.ok(studentPromotionService.getHistoryByCohort(cohortId));
        }
        return ResponseEntity.badRequest().build();
    }
}
