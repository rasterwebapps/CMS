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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.EscortCandidateDto;
import com.cms.dto.EscortDutyDto;
import com.cms.dto.EscortRotationPoolDto;
import com.cms.dto.EscortRotationPoolRequest;
import com.cms.dto.ProfileIdentity;
import com.cms.service.EscortRotationResolverService;
import com.cms.service.EscortRotationService;
import com.cms.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/escort-rotation")
public class EscortRotationController {

    private final EscortRotationService service;
    private final EscortRotationResolverService resolverService;
    private final ProfileService profileService;

    public EscortRotationController(EscortRotationService service,
                                     EscortRotationResolverService resolverService,
                                     ProfileService profileService) {
        this.service = service;
        this.resolverService = resolverService;
        this.profileService = profileService;
    }

    @GetMapping("/batches/{batchId}/candidates")
    @PreAuthorize("@perm.has('TIMETABLE_ESCORT_ROTATION_MANAGE')")
    public ResponseEntity<List<EscortCandidateDto>> eligibleCandidates(@PathVariable Long batchId) {
        return ResponseEntity.ok(service.eligibleCandidates(batchId));
    }

    @PostMapping("/pools")
    @PreAuthorize("@perm.has('TIMETABLE_ESCORT_ROTATION_MANAGE')")
    public ResponseEntity<EscortRotationPoolDto> setupPool(@Valid @RequestBody EscortRotationPoolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.setupPool(request));
    }

    @DeleteMapping("/batches/{batchId}/pool")
    @PreAuthorize("@perm.has('TIMETABLE_ESCORT_ROTATION_MANAGE')")
    public ResponseEntity<Void> deactivatePool(@PathVariable Long batchId) {
        service.deactivatePool(batchId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/batches/{batchId}/pool")
    @PreAuthorize("@perm.has('TIMETABLE_ESCORT_ROTATION_VIEW')")
    public ResponseEntity<EscortRotationPoolDto> getPool(@PathVariable Long batchId) {
        EscortRotationPoolDto pool = service.getPool(batchId);
        return pool == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pool);
    }

    @GetMapping("/batches/{batchId}/resolve")
    @PreAuthorize("@perm.has('TIMETABLE_ESCORT_ROTATION_VIEW')")
    public ResponseEntity<EscortDutyDto> resolveForDate(@PathVariable Long batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resolverService.resolveForDate(batchId, date)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/my-duties")
    @PreAuthorize("@perm.has('TIMETABLE_ESCORT_ROTATION_VIEW')")
    public ResponseEntity<List<EscortDutyDto>> myUpcomingDuties() {
        ProfileIdentity identity = profileService.resolveCurrentUser();
        if (!"FACULTY".equals(identity.entityType())) {
            throw new IllegalArgumentException("Only a faculty member has escort duties.");
        }
        return ResponseEntity.ok(resolverService.myUpcomingDuties(identity.entityId()));
    }
}
