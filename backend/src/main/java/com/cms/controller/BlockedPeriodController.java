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

import com.cms.dto.BlockedPeriodRequest;
import com.cms.dto.BlockedPeriodResponse;
import com.cms.service.BlockedPeriodService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/blocked-periods")
public class BlockedPeriodController {

    private final BlockedPeriodService blockedPeriodService;

    public BlockedPeriodController(BlockedPeriodService blockedPeriodService) {
        this.blockedPeriodService = blockedPeriodService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('BLOCKED_PERIOD_MANAGE')")
    public ResponseEntity<BlockedPeriodResponse> create(@Valid @RequestBody BlockedPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedPeriodService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BlockedPeriodResponse>> findAll() {
        return ResponseEntity.ok(blockedPeriodService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlockedPeriodResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(blockedPeriodService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('BLOCKED_PERIOD_MANAGE')")
    public ResponseEntity<BlockedPeriodResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody BlockedPeriodRequest request) {
        return ResponseEntity.ok(blockedPeriodService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('BLOCKED_PERIOD_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blockedPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}