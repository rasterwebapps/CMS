package com.cms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LegacyAdmitRequest;
import com.cms.dto.LegacyAdmitResponse;
import com.cms.service.LegacyAdmitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/students/legacy-admit")
public class LegacyAdmitController {

    private final LegacyAdmitService legacyAdmitService;

    public LegacyAdmitController(LegacyAdmitService legacyAdmitService) {
        this.legacyAdmitService = legacyAdmitService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LEGACY_ADMIT')")
    public ResponseEntity<LegacyAdmitResponse> admit(
            @Valid @RequestBody LegacyAdmitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String user = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        LegacyAdmitResponse response = legacyAdmitService.admit(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
