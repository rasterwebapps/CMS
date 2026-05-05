package com.cms.dto;

import com.cms.model.enums.DocumentType;

/**
 * Lightweight projection of a {@link DocumentType} enum value for the frontend catalogue.
 * Carries the enum {@code code}, the human-readable {@code label} (sourced from
 * {@link DocumentType#getDisplayName()}), and a coarse {@code category} used for grouping.
 */
public record DocumentTypeInfo(
    String code,
    String label,
    String category
) {
    public static DocumentTypeInfo from(DocumentType type) {
        return new DocumentTypeInfo(type.name(), type.getDisplayName(), categoryOf(type));
    }

    private static String categoryOf(DocumentType type) {
        return switch (type) {
            case TENTH_MARKSHEET, ELEVENTH_MARKSHEET, TWELFTH_MARKSHEET,
                 SEM_1_MARKSHEET, SEM_2_MARKSHEET, SEM_3_MARKSHEET, SEM_4_MARKSHEET,
                 SEM_5_MARKSHEET, SEM_6_MARKSHEET, SEM_7_MARKSHEET, SEM_8_MARKSHEET,
                 TRANSCRIPT, DEGREE_CERTIFICATE, PROVISIONAL_CERTIFICATE,
                 GENUINENESS_CERTIFICATE, ELIGIBILITY_CERTIFICATE -> "Academic";
            case TRANSFER_CERTIFICATE, COLLEGE_OR_SCHOOL_TC, MIGRATION_CERTIFICATE -> "Administrative";
            case AADHAR_CARD, PASSPORT_PHOTO, SCANNED_SIGNATURE,
                 PAN_CARD, FACULTY_PHOTO, E_SIGNATURE -> "Identity";
            case COMMUNITY_CERTIFICATE, INCOME_CERTIFICATE, NATIVITY_CERTIFICATE,
                 FIRST_GRADUATE_CERTIFICATE -> "Statutory";
            case APPOINTMENT_LETTER, JOINING_REPORT, PROMOTION_LETTER,
                 RENEWAL_CERTIFICATE -> "Faculty Service";
            case UG_DEGREE, PG_DEGREE, UG_RNRM, PG_RNRM -> "Faculty Qualification";
            case TEACHING_EXPERIENCE_CERTIFICATE,
                 CLINICAL_EXPERIENCE_CERTIFICATE -> "Faculty Experience";
            case MEDICAL_FITNESS, SIGNED_AFFIDAVIT -> "Other";
        };
    }
}
