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

import com.cms.dto.RetroAdmitRequest;
import com.cms.dto.RetroAdmitResponse;
import com.cms.service.RetroAdmitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/students/retro-admit")
public class RetroAdmitController {

    private final RetroAdmitService retroAdmitService;

    public RetroAdmitController(RetroAdmitService retroAdmitService) {
        this.retroAdmitService = retroAdmitService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('RETRO_ADMIT')")
    public ResponseEntity<RetroAdmitResponse> admit(
            @Valid @RequestBody RetroAdmitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String user = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        RetroAdmitResponse response = retroAdmitService.admit(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
