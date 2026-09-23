package com.cms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AnnouncementRequest;
import com.cms.dto.AnnouncementResponse;
import com.cms.service.AnnouncementService;

import jakarta.validation.Valid;

@RestController
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/announcements")
    @PreAuthorize("@perm.has('ANNOUNCEMENT_MANAGE')")
    public ResponseEntity<AnnouncementResponse> create(
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "";
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(request, username));
    }

    @GetMapping("/announcements")
    @PreAuthorize("@perm.has('ANNOUNCEMENT_MANAGE')")
    public ResponseEntity<List<AnnouncementResponse>> findAll() {
        return ResponseEntity.ok(announcementService.findAll());
    }

    /** Current authenticated user's own announcement feed, matched by role/cohort/section --
     *  never a client-supplied identity. */
    @GetMapping("/announcements/my")
    @PreAuthorize("@perm.has('ANNOUNCEMENT_VIEW')")
    public ResponseEntity<List<AnnouncementResponse>> myFeed(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "";
        return ResponseEntity.ok(announcementService.findMyFeed(username));
    }

    @PostMapping("/announcements/{id}/read")
    @PreAuthorize("@perm.has('ANNOUNCEMENT_VIEW')")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "";
        announcementService.markRead(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/announcements/my/unread-count")
    @PreAuthorize("@perm.has('ANNOUNCEMENT_VIEW')")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "";
        return ResponseEntity.ok(Map.of("unreadCount", announcementService.unreadCount(username)));
    }
}
