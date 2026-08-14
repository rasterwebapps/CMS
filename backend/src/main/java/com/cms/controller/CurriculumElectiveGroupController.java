package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CurriculumElectiveGroupDto;
import com.cms.dto.CurriculumElectiveGroupRequest;
import com.cms.dto.UpdateElectiveSelectionModeRequest;
import com.cms.service.CurriculumElectiveGroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/curriculum-elective-groups")
public class CurriculumElectiveGroupController {

    private final CurriculumElectiveGroupService service;

    public CurriculumElectiveGroupController(CurriculumElectiveGroupService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('CURRICULUM_ELECTIVE_GROUP_MANAGE')")
    public ResponseEntity<CurriculumElectiveGroupDto> createGroup(
            @Valid @RequestBody CurriculumElectiveGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(request));
    }

    @GetMapping
    @PreAuthorize("@perm.has('CURRICULUM_ELECTIVE_GROUP_VIEW')")
    public ResponseEntity<List<CurriculumElectiveGroupDto>> getGroups(
            @RequestParam Long curriculumVersionId,
            @RequestParam Integer termNumber) {
        return ResponseEntity.ok(service.getGroups(curriculumVersionId, termNumber));
    }

    @PutMapping("/{id}/selection-mode")
    @PreAuthorize("@perm.has('CURRICULUM_ELECTIVE_GROUP_MANAGE')")
    public ResponseEntity<CurriculumElectiveGroupDto> updateSelectionMode(
            @PathVariable Long id,
            @Valid @RequestBody UpdateElectiveSelectionModeRequest request) {
        return ResponseEntity.ok(service.updateSelectionMode(id, request.selectionMode()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('CURRICULUM_ELECTIVE_GROUP_MANAGE')")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        service.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }
}
