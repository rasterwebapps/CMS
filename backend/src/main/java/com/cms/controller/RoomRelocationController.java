package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.RoomRelocationRequest;
import com.cms.dto.RoomRelocationResponse;
import com.cms.dto.VenueCandidate;
import com.cms.service.RoomRelocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/timetables/room-relocation")
public class RoomRelocationController {

    private final RoomRelocationService roomRelocationService;

    public RoomRelocationController(RoomRelocationService roomRelocationService) {
        this.roomRelocationService = roomRelocationService;
    }

    @GetMapping("/sessions/{classScheduleId}/candidates")
    @PreAuthorize("@perm.has('TIMETABLE_ROOM_RELOCATE')")
    public ResponseEntity<List<VenueCandidate>> findCandidates(
            @PathVariable Long classScheduleId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(roomRelocationService.findCandidateVenues(classScheduleId, date));
    }

    @PostMapping("/sessions/{classScheduleId}/relocate")
    @PreAuthorize("@perm.has('TIMETABLE_ROOM_RELOCATE')")
    public ResponseEntity<RoomRelocationResponse> relocate(
            @PathVariable Long classScheduleId, @Valid @RequestBody RoomRelocationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(roomRelocationService.relocate(classScheduleId, request, actor(jwt)));
    }

    @DeleteMapping("/sessions/{classScheduleId}/relocate")
    @PreAuthorize("@perm.has('TIMETABLE_ROOM_RELOCATE')")
    public ResponseEntity<RoomRelocationResponse> revert(
            @PathVariable Long classScheduleId, @RequestParam LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(roomRelocationService.revert(classScheduleId, date, actor(jwt)));
    }

    private static String actor(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
