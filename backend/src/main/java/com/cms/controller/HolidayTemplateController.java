package com.cms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.cms.dto.HolidayTemplateRequest;
import com.cms.dto.HolidayTemplateResponse;
import com.cms.service.HolidayTemplateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/holiday-templates")
public class HolidayTemplateController {

    private final HolidayTemplateService holidayTemplateService;

    public HolidayTemplateController(HolidayTemplateService holidayTemplateService) {
        this.holidayTemplateService = holidayTemplateService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('HOLIDAY_TEMPLATE_MANAGE')")
    public ResponseEntity<HolidayTemplateResponse> create(@Valid @RequestBody HolidayTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayTemplateService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<HolidayTemplateResponse>> findAll() {
        return ResponseEntity.ok(holidayTemplateService.findAll());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<HolidayTemplateResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(holidayTemplateService.findPage(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayTemplateResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(holidayTemplateService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('HOLIDAY_TEMPLATE_MANAGE')")
    public ResponseEntity<HolidayTemplateResponse> update(
            @PathVariable Long id, @Valid @RequestBody HolidayTemplateRequest request) {
        return ResponseEntity.ok(holidayTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('HOLIDAY_TEMPLATE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        holidayTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('HOLIDAY_TEMPLATE_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(holidayTemplateService.nameExists(value, excludeId));
    }
}
