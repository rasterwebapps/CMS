package com.cms.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("name", "Name");
        EXPORT_SORT_FIELDS.put("area", "Area");
        EXPORT_SORT_FIELDS.put("commissionAmount", "Commission Amount");
        EXPORT_SORT_FIELDS.put("isActive", "Active");
    }

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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "name", Sort.Direction.ASC);
        List<AgentResponse> data = agentService.findAll(search, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "name", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Agents Export")
            .filter("Search", search)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "agents",
            () -> agentExportService.toExcel(data, meta),
            () -> agentExportService.toPdf(data, meta));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('AGENT_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(agentService.nameExists(value, excludeId));
    }
}
