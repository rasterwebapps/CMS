package com.cms.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cms.model.enums.DocumentType;

class DocumentTypeInfoTest {

    @Test
    void fromShouldMapCodeLabelAndCategoryAcademic() {
        DocumentTypeInfo info = DocumentTypeInfo.from(DocumentType.TENTH_MARKSHEET);

        assertThat(info.code()).isEqualTo("TENTH_MARKSHEET");
        assertThat(info.label()).isEqualTo(DocumentType.TENTH_MARKSHEET.getDisplayName());
        assertThat(info.category()).isEqualTo("Academic");
    }

    @Test
    void fromShouldMapIdentityCategory() {
        DocumentTypeInfo info = DocumentTypeInfo.from(DocumentType.PAN_CARD);
        assertThat(info.category()).isEqualTo("Identity");
    }

    @Test
    void fromShouldMapOtherCategory() {
        DocumentTypeInfo info = DocumentTypeInfo.from(DocumentType.MEDICAL_FITNESS);
        assertThat(info.category()).isEqualTo("Other");
    }
}

