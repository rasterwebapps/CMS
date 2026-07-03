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
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.NumberSeriesDefinitionRequest;
import com.cms.dto.NumberSeriesDefinitionResponse;
import com.cms.service.NumberSeriesDefinitionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/number-series")
public class NumberSeriesDefinitionController {

    private final NumberSeriesDefinitionService service;

    public NumberSeriesDefinitionController(NumberSeriesDefinitionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@perm.has('NUMBER_SERIES_VIEW')")
    public ResponseEntity<List<NumberSeriesDefinitionResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('NUMBER_SERIES_VIEW')")
    public ResponseEntity<NumberSeriesDefinitionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("@perm.has('NUMBER_SERIES_VIEW')")
    public ResponseEntity<String> preview(@PathVariable Long id) {
        return ResponseEntity.ok(service.preview(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('NUMBER_SERIES_MANAGE')")
    public ResponseEntity<NumberSeriesDefinitionResponse> create(
            @Valid @RequestBody NumberSeriesDefinitionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('NUMBER_SERIES_MANAGE')")
    public ResponseEntity<NumberSeriesDefinitionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NumberSeriesDefinitionRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('NUMBER_SERIES_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
