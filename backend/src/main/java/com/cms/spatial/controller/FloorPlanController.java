package com.cms.spatial.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.spatial.dto.FloorPlanCalibrationRequest;
import com.cms.spatial.dto.FloorPlanCreateRequest;
import com.cms.spatial.dto.FloorPlanResponse;
import com.cms.spatial.dto.FloorPlanUpdateRequest;
import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;
import com.cms.spatial.service.FloorPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/spatial/floor-plans")
public class FloorPlanController {

    private final FloorPlanService floorPlanService;

    public FloorPlanController(FloorPlanService floorPlanService) {
        this.floorPlanService = floorPlanService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_VIEW')")
    public ResponseEntity<List<FloorPlanResponse>> findByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(floorPlanService.findByEntity(entityType, entityId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_VIEW')")
    public ResponseEntity<FloorPlanResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(floorPlanService.findById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_MANAGE')")
    public ResponseEntity<FloorPlanResponse> create(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "METERS") UnitSystem unitSystem,
            @RequestParam(required = false, defaultValue = "TOP_LEFT") OriginAnchor originAnchor,
            @RequestParam(required = false, defaultValue = "0") Double originX,
            @RequestParam(required = false, defaultValue = "0") Double originY,
            @RequestParam(required = false) Double viewboxWidth,
            @RequestParam(required = false) Double viewboxHeight,
            @RequestPart("file") MultipartFile file) {
        FloorPlanCreateRequest request = new FloorPlanCreateRequest(
            entityType, entityId, name, unitSystem, originAnchor, originX, originY, viewboxWidth, viewboxHeight);
        return ResponseEntity.status(HttpStatus.CREATED).body(floorPlanService.create(request, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_MANAGE')")
    public ResponseEntity<FloorPlanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FloorPlanUpdateRequest request) {
        return ResponseEntity.ok(floorPlanService.update(id, request));
    }

    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_MANAGE')")
    public ResponseEntity<FloorPlanResponse> replaceFile(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(floorPlanService.replaceFile(id, file));
    }

    @PostMapping("/{id}/calibrate")
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_MANAGE')")
    public ResponseEntity<FloorPlanResponse> calibrate(
            @PathVariable Long id,
            @Valid @RequestBody FloorPlanCalibrationRequest request) {
        return ResponseEntity.ok(floorPlanService.calibrate(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        floorPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@perm.has('SPATIAL_FLOOR_PLAN_VIEW')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentFileDownload download = floorPlanService.getFileForDownload(id);
        ByteArrayResource resource = new ByteArrayResource(download.data());

        String encoded = URLEncoder.encode(download.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + sanitizeForHeader(download.fileName())
            + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.data().length)
            .body(resource);
    }

    private static String sanitizeForHeader(String name) {
        return name.replaceAll("[\\\\\"\\r\\n]", "_");
    }
}
