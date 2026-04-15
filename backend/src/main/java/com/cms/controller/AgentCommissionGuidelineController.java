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

import com.cms.dto.AgentCommissionGuidelineRequest;
import com.cms.dto.AgentCommissionGuidelineResponse;
import com.cms.service.AgentCommissionGuidelineService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/agent-commission-guidelines")
public class AgentCommissionGuidelineController {

    private final AgentCommissionGuidelineService guidelineService;

    public AgentCommissionGuidelineController(AgentCommissionGuidelineService guidelineService) {
        this.guidelineService = guidelineService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AgentCommissionGuidelineResponse> create(
            @Valid @RequestBody AgentCommissionGuidelineRequest request) {
        AgentCommissionGuidelineResponse response = guidelineService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AgentCommissionGuidelineResponse>> findAll(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) Long programId) {
        List<AgentCommissionGuidelineResponse> guidelines;
        if (agentId != null) {
            guidelines = guidelineService.findByAgentId(agentId);
        } else if (programId != null) {
            guidelines = guidelineService.findByProgramId(programId);
        } else {
            guidelines = guidelineService.findAll();
        }
        return ResponseEntity.ok(guidelines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentCommissionGuidelineResponse> findById(@PathVariable Long id) {
        AgentCommissionGuidelineResponse response = guidelineService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AgentCommissionGuidelineResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AgentCommissionGuidelineRequest request) {
        AgentCommissionGuidelineResponse response = guidelineService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        guidelineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
