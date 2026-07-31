package com.cms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.service.TimetableSkeletonService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/skeleton")
public class TimetableSkeletonController {

    private final TimetableSkeletonService timetableSkeletonService;

    public TimetableSkeletonController(TimetableSkeletonService timetableSkeletonService) {
        this.timetableSkeletonService = timetableSkeletonService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_VIEW')")
    public ResponseEntity<SkeletonBuilderResponse> getSkeleton(@RequestParam Long courseOfferingId) {
        return ResponseEntity.ok(timetableSkeletonService.getSkeleton(courseOfferingId));
    }

    @PostMapping("/cells")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MANAGE')")
    public ResponseEntity<SkeletonCellResponse> placeCell(@Valid @RequestBody SkeletonCellPlacementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableSkeletonService.placeCell(request));
    }

    @DeleteMapping("/cells/{id}")
    @PreAuthorize("@perm.has('TIMETABLE_SKELETON_MANAGE')")
    public ResponseEntity<Void> removeCell(@PathVariable Long id) {
        timetableSkeletonService.removeCell(id);
        return ResponseEntity.noContent().build();
    }
}
