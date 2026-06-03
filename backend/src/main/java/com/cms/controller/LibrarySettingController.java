package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibrarySettingResponse;
import com.cms.dto.LibrarySettingUpdateRequest;
import com.cms.service.LibrarySettingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/settings")
public class LibrarySettingController {

    private final LibrarySettingService settingService;

    public LibrarySettingController(LibrarySettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_SETTINGS_MANAGE', 'LIBRARY_CATALOGUE_VIEW', 'LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<List<LibrarySettingResponse>> findAll() {
        return ResponseEntity.ok(settingService.findAll());
    }

    @PutMapping("/{key}")
    @PreAuthorize("@perm.has('LIBRARY_SETTINGS_MANAGE')")
    public ResponseEntity<LibrarySettingResponse> update(
            @PathVariable String key,
            @Valid @RequestBody LibrarySettingUpdateRequest request) {
        return ResponseEntity.ok(settingService.updateByKey(key, request));
    }
}
