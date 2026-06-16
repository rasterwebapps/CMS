package com.cms.model;

import java.util.Objects;

import com.cms.model.enums.DocumentType;
import com.cms.model.enums.ProgramDocumentCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ProgramDocumentRequirement {

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 100, nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private ProgramDocumentCategory category = ProgramDocumentCategory.MANDATORY;

    public ProgramDocumentRequirement() {}

    public ProgramDocumentRequirement(DocumentType documentType, ProgramDocumentCategory category) {
        this.documentType = documentType;
        this.category = category != null ? category : ProgramDocumentCategory.MANDATORY;
    }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public ProgramDocumentCategory getCategory() { return category; }
    public void setCategory(ProgramDocumentCategory category) { this.category = category; }

    // Equality by documentType only: a type cannot be in both MANDATORY and OPTIONAL.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramDocumentRequirement)) return false;
        ProgramDocumentRequirement that = (ProgramDocumentRequirement) o;
        return Objects.equals(documentType, that.documentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentType);
    }
}
