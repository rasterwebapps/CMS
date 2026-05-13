package com.cms.dto;

import com.cms.model.enums.Designation;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.FacultyQualification;

import jakarta.validation.constraints.NotNull;

public record FacultyDocumentTypeRequirementRequest(

    @NotNull(message = "Document type is required")
    DocumentType documentType,

    Designation designation,

    Long departmentId,

    FacultyQualification qualification
) {}
