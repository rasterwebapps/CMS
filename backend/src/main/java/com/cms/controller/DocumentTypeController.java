package com.cms.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DocumentTypeInfo;
import com.cms.model.enums.DocumentType;

/**
 * Exposes the catalogue of available document types (with display labels and
 * categories). The frontend uses this as the single source of truth for
 * rendering document type pickers and labels.
 */
@RestController
@RequestMapping("/document-types")
public class DocumentTypeController {

    @GetMapping
    public ResponseEntity<List<DocumentTypeInfo>> findAll() {
        List<DocumentTypeInfo> all = Arrays.stream(DocumentType.values())
            .map(DocumentTypeInfo::from)
            .toList();
        return ResponseEntity.ok(all);
    }
}

