package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ApplicationNumberSequenceResponse;
import com.cms.service.ApplicationNumberSequenceService;

@RestController
@RequestMapping("/number-sequences")
public class ApplicationNumberSequenceController {

    private final ApplicationNumberSequenceService sequenceService;

    public ApplicationNumberSequenceController(ApplicationNumberSequenceService sequenceService) {
        this.sequenceService = sequenceService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('NUMBER_SEQUENCE_VIEW')")
    public ResponseEntity<List<ApplicationNumberSequenceResponse>> findAll() {
        return ResponseEntity.ok(sequenceService.findAll());
    }
}
