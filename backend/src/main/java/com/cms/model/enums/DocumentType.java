package com.cms.model.enums;

/**
 * Catalogue of document types collected during enquiry / admission.
 * The {@link #displayName} is the human-readable label used by the frontend
 * (for example, "10th Marksheet" instead of "TENTH_MARKSHEET").
 */
public enum DocumentType {

    // Academic
    TENTH_MARKSHEET("10th Marksheet"),
    ELEVENTH_MARKSHEET("11th Marksheet"),
    TWELFTH_MARKSHEET("12th Marksheet"),
    SEM_1_MARKSHEET("Semester 1 Marksheet"),
    SEM_2_MARKSHEET("Semester 2 Marksheet"),
    SEM_3_MARKSHEET("Semester 3 Marksheet"),
    SEM_4_MARKSHEET("Semester 4 Marksheet"),
    SEM_5_MARKSHEET("Semester 5 Marksheet"),
    SEM_6_MARKSHEET("Semester 6 Marksheet"),
    SEM_7_MARKSHEET("Semester 7 Marksheet"),
    SEM_8_MARKSHEET("Semester 8 Marksheet"),
    TRANSCRIPT("Transcript"),
    DEGREE_CERTIFICATE("Degree Certificate"),
    PROVISIONAL_CERTIFICATE("Provisional Certificate"),
    GENUINENESS_CERTIFICATE("Genuineness Certificate"),
    ELIGIBILITY_CERTIFICATE("Eligibility Certificate"),

    // Administrative
    TRANSFER_CERTIFICATE("School Transfer Certificate"),
    COLLEGE_OR_SCHOOL_TC("College Transfer Certificate"),
    MIGRATION_CERTIFICATE("Migration Certificate"),

    // Identity
    AADHAR_CARD("Aadhar Card"),
    PASSPORT_PHOTO("Passport Photo"),
    SCANNED_SIGNATURE("Scanned Signature"),

    // Statutory
    COMMUNITY_CERTIFICATE("Community Certificate"),
    INCOME_CERTIFICATE("Income Certificate"),
    NATIVITY_CERTIFICATE("Nativity Certificate"),
    FIRST_GRADUATE_CERTIFICATE("First Graduate Certificate"),

    // Other
    MEDICAL_FITNESS("Medical Fitness Certificate"),
    SIGNED_AFFIDAVIT("Signed Affidavit"),

    // Faculty — appointment & service
    APPOINTMENT_LETTER("Appointment Letter"),
    JOINING_REPORT("Joining Report"),
    PROMOTION_LETTER("Promotion Letter"),
    RENEWAL_CERTIFICATE("Renewal Certificate"),

    // Faculty — qualifications
    UG_DEGREE("UG Degree"),
    PG_DEGREE("PG Degree"),
    UG_RNRM("UG RN/RM Registration"),
    PG_RNRM("PG RN/RM Registration"),

    // Faculty — experience
    TEACHING_EXPERIENCE_CERTIFICATE("Teaching Experience Certificate"),
    CLINICAL_EXPERIENCE_CERTIFICATE("Clinical Experience Certificate"),

    // Faculty — identity & misc
    PAN_CARD("PAN Card"),
    FACULTY_PHOTO("Faculty Photo"),
    E_SIGNATURE("e-Signature");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
