package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.Designation;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.FacultyQualification;

public record FacultyDocumentTypeRequirementResponse(
    Long id,
    DocumentType documentType,
    String documentTypeLabel,
    Designation designation,
    Long specialityId,
    String specialityName,
    FacultyQualification qualification,
    String qualificationLabel,
    Instant createdAt
) {}
