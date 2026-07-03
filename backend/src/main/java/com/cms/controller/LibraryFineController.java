package com.cms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryFineDetailResponse;
import com.cms.dto.LibraryFineRequest;
import com.cms.model.enums.FineStatus;
import com.cms.model.enums.LibraryMemberType;
import com.cms.service.LibraryFineService;

@RestController
@RequestMapping("/library/fines")
public class LibraryFineController {

    private final LibraryFineService fineService;

    public LibraryFineController(LibraryFineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_FINE_VIEW', 'LIBRARY_FINE_MANAGE')")
    public ResponseEntity<List<LibraryFineDetailResponse>> findAll(
            @RequestParam(required = false) FineStatus status) {
        return ResponseEntity.ok(fineService.findAll(status));
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("@perm.has('LIBRARY_FINE_MANAGE')")
    public ResponseEntity<LibraryFineDetailResponse> waive(
            @PathVariable Long id,
            @RequestBody(required = false) LibraryFineRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getClaimAsString("preferred_username") : "librarian";
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(fineService.waive(id, remarks, actor));
    }

    @PostMapping("/{id}/collect")
    @PreAuthorize("@perm.has('LIBRARY_FINE_MANAGE')")
    public ResponseEntity<LibraryFineDetailResponse> collect(
            @PathVariable Long id,
            @RequestBody(required = false) LibraryFineRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getClaimAsString("preferred_username") : "librarian";
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(fineService.collect(id, remarks, actor));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('LIBRARY_FINE_VIEW', 'LIBRARY_FINE_MANAGE')")
    public ResponseEntity<Page<LibraryFineDetailResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FineStatus status,
            @RequestParam(required = false) LibraryMemberType memberType,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(fineService.findPage(search, status, memberType, pageable));
    }
}
