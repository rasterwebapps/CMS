package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.cms.dto.ClinicalShiftGroupDto;
import com.cms.dto.ClinicalShiftGroupRequest;
import com.cms.dto.ClinicalShiftTheoryBlockDto;
import com.cms.dto.ClinicalShiftTheoryBlockRequest;
import com.cms.model.SessionOccurrence;
import com.cms.service.ClinicalShiftGroupService;
import com.cms.service.ClinicalShiftOccurrenceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clinical-shift-groups")
public class ClinicalShiftGroupController {

    private final ClinicalShiftGroupService service;
    private final ClinicalShiftOccurrenceService occurrenceService;

    public ClinicalShiftGroupController(ClinicalShiftGroupService service,
                                         ClinicalShiftOccurrenceService occurrenceService) {
        this.service = service;
        this.occurrenceService = occurrenceService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<ClinicalShiftGroupDto> create(@Valid @RequestBody ClinicalShiftGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<ClinicalShiftGroupDto> update(@PathVariable Long id,
            @Valid @RequestBody ClinicalShiftGroupRequest request) {
        return ResponseEntity.ok(service.updateGroup(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivateGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_VIEW')")
    public ResponseEntity<List<ClinicalShiftGroupDto>> getForOffering(@RequestParam Long courseOfferingId) {
        return ResponseEntity.ok(service.getGroupsForOffering(courseOfferingId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_VIEW')")
    public ResponseEntity<ClinicalShiftGroupDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getGroup(id));
    }

    @PutMapping("/{id}/batches/{batchId}")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<Void> linkBatch(@PathVariable Long id, @PathVariable Long batchId) {
        service.linkBatch(id, batchId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/batches/{batchId}")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<Void> unlinkBatch(@PathVariable Long id, @PathVariable Long batchId) {
        service.unlinkBatch(id, batchId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/theory-blocks")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<List<ClinicalShiftTheoryBlockDto>> replaceTheoryBlocks(@PathVariable Long id,
            @Valid @RequestBody List<ClinicalShiftTheoryBlockRequest> requests) {
        return ResponseEntity.ok(service.replaceTheoryBlocks(id, requests));
    }

    @PostMapping("/{id}/generate")
    @PreAuthorize("@perm.has('TIMETABLE_CLINICAL_SHIFT_MANAGE')")
    public ResponseEntity<Integer> generateForDate(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SessionOccurrence> created = occurrenceService.generateForDate(id, date);
        return ResponseEntity.ok(created.size());
    }
}
