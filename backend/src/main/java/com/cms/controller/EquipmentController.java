package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.EquipmentRequest;
import com.cms.dto.EquipmentResponse;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;
import com.cms.service.EquipmentExportService;
import com.cms.service.EquipmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    private final EquipmentService       equipmentService;
    private final EquipmentExportService equipmentExportService;

    public EquipmentController(EquipmentService equipmentService, EquipmentExportService equipmentExportService) {
        this.equipmentService       = equipmentService;
        this.equipmentExportService = equipmentExportService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('EQUIPMENT_MANAGE')")
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse response = equipmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> findAll(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) EquipmentCategory category) {
        List<EquipmentResponse> equipment;
        if (labId != null) {
            equipment = equipmentService.findByLabId(labId);
        } else if (status != null) {
            equipment = equipmentService.findByStatus(status);
        } else if (category != null) {
            equipment = equipmentService.findByCategory(category);
        } else {
            equipment = equipmentService.findAll();
        }
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> findById(@PathVariable Long id) {
        EquipmentResponse response = equipmentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/asset/{assetCode}")
    public ResponseEntity<EquipmentResponse> findByAssetCode(@PathVariable String assetCode) {
        EquipmentResponse response = equipmentService.findByAssetCode(assetCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('EQUIPMENT_MANAGE')")
    public ResponseEntity<EquipmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse response = equipmentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('EQUIPMENT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('EQUIPMENT_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search) {
        List<EquipmentResponse> data = equipmentService.findAll(search);
        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = equipmentExportService.toPdf(data);
                String filename = "equipment-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = equipmentExportService.toExcel(data);
                String filename = "equipment-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/page")
    public ResponseEntity<Page<EquipmentResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(equipmentService.findPage(search, pageable));
    }
}
