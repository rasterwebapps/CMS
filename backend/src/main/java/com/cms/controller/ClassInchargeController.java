package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ClassInchargeAssignment;
import com.cms.dto.ClassInchargeUpsertRequest;
import com.cms.service.ClassInchargeService;

/** Class Teacher / Class Incharge assignment for a term's committed cohort sections -- reached
 *  from Assign Faculty, not scoped to any single course offering (see {@link ClassInchargeService}). */
@RestController
@RequestMapping("/class-incharge")
public class ClassInchargeController {

    private final ClassInchargeService classInchargeService;

    public ClassInchargeController(ClassInchargeService classInchargeService) {
        this.classInchargeService = classInchargeService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('CLASS_INCHARGE_VIEW')")
    public ResponseEntity<List<ClassInchargeAssignment>> getForTermInstance(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(classInchargeService.getForTermInstance(termInstanceId));
    }

    @PutMapping("/{cohortSectionId}")
    @PreAuthorize("@perm.has('CLASS_INCHARGE_MANAGE')")
    public ResponseEntity<ClassInchargeAssignment> upsert(
            @PathVariable Long cohortSectionId,
            @RequestBody ClassInchargeUpsertRequest request) {
        return ResponseEntity.ok(classInchargeService.upsert(cohortSectionId, request.facultyId()));
    }
}
