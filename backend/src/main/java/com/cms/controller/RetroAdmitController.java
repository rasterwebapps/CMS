package com.cms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.RetroAdmitRequest;
import com.cms.dto.RetroAdmitResponse;
import com.cms.service.RetroAdmitService;
import com.cms.util.CurrentUserResolver;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/students/retro-admit")
public class RetroAdmitController {

    private final RetroAdmitService retroAdmitService;
    private final CurrentUserResolver currentUserResolver;

    public RetroAdmitController(RetroAdmitService retroAdmitService, CurrentUserResolver currentUserResolver) {
        this.retroAdmitService = retroAdmitService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @PreAuthorize("@perm.has('RETRO_ADMIT')")
    public ResponseEntity<RetroAdmitResponse> admit(@Valid @RequestBody RetroAdmitRequest request) {
        RetroAdmitResponse response = retroAdmitService.admit(request, currentUserResolver.resolveFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
