package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.NotificationPreferenceRequest;
import com.cms.dto.NotificationPreferenceResponse;
import com.cms.service.NotificationPreferenceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notifications")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    public NotificationPreferenceController(NotificationPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences() {
        return ResponseEntity.ok(service.getPreferences());
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return ResponseEntity.ok(service.updatePreferences(request));
    }
}
