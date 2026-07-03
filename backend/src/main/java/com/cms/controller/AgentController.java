package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AgentRequest;
import com.cms.dto.AgentResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.service.AgentExportService;
import com.cms.service.AgentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentService       agentService;
    private final AgentExportService agentExportService;

    public AgentController(AgentService agentService, AgentExportService agentExportService) {
        this.agentService       = agentService;
        this.agentExportService = agentExportService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<AgentResponse> create(@Valid @RequestBody AgentRequest request) {
        AgentResponse response = agentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AgentResponse>> findAll(
            @RequestParam(required = false) Boolean active) {
        List<AgentResponse> agents;
        if (Boolean.TRUE.equals(active)) {
            agents = agentService.findActiveAgents();
        } else {
            agents = agentService.findAll();
        }
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentResponse> findById(@PathVariable Long id) {
        AgentResponse response = agentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<AgentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AgentRequest request) {
        AgentResponse response = agentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(agentService.updateStatus(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<AgentResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.deactivate(id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<AgentResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.reactivate(id));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<AgentResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(agentService.findPage(search, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('AGENT_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search) {
        List<AgentResponse> data = agentService.findAll(search);
        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = agentExportService.toPdf(data);
                String filename = "agents-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = agentExportService.toExcel(data);
                String filename = "agents-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(agentService.nameExists(value, excludeId));
    }
}
