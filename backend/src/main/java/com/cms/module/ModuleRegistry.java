package com.cms.module;

import java.util.List;
import java.util.Optional;

/**
 * The canonical, backend-owned list of toggleable modules and which permission codes each one
 * owns. This is the single source of truth the frontend fetches (via {@code GET /modules/enabled})
 * rather than maintaining its own copy — see {@code docs/module-architecture/MODULE_REGISTRY.md}
 * for the full module ⇄ nav-group ⇄ permission-prefix table this class implements.
 *
 * <p>Permission codes with no entry here (e.g. {@code USER_VIEW}, {@code DESIGNATION_MANAGE},
 * {@code SETTINGS_EDIT}) are "core" — always available regardless of which modules are enabled.
 *
 * <p>Matching is prefix-based (an entry ending in {@code "_"}) or exact (a standalone code like
 * {@code "FEE_FINALIZE"}), via {@link String#startsWith}. Entries across modules are chosen to be
 * disjoint by construction — e.g. Finance's {@code STUDENT_FEE_} vs. Student Management's exact
 * {@code STUDENT_VIEW}/{@code STUDENT_CREATE}/... never collide because the characters right after
 * {@code STUDENT_} differ — so registration order does not matter for correctness.
 */
public final class ModuleRegistry {

    public static final String ADMISSIONS = "ADMISSIONS";
    public static final String STUDENT_MGMT = "STUDENT_MGMT";
    public static final String FINANCE = "FINANCE";
    public static final String ACADEMICS = "ACADEMICS";
    public static final String LIBRARY = "LIBRARY";
    public static final String CORE_INFRA = "CORE_INFRA";
    public static final String INVENTORY = "INVENTORY";
    public static final String HOSTEL = "HOSTEL";
    public static final String REPORTS = "REPORTS";

    private static final List<ModuleDefinition> MODULES = List.of(
        new ModuleDefinition(
            ADMISSIONS,
            "Admission Management",
            List.of(
                "ENQUIRY_", "FEE_FINALIZE", "FEE_COLLECT",
                "DOCUMENT_SUBMISSION_", "DOCUMENT_VERIFICATION_",
                "ADMISSION_", "RETRO_ADMIT",
                "AGENT_", "INSTITUTION_", "REFERRAL_TYPE_", "STAFF_REFERRER_"
            ),
            List.of()
        ),
        new ModuleDefinition(
            STUDENT_MGMT,
            "Student Management",
            List.of(
                "STUDENT_VIEW", "STUDENT_CREATE", "STUDENT_EDIT", "STUDENT_DELETE",
                "STUDENT_EXPORT", "STUDENT_MANAGE",
                "ROLL_NUMBER_", "SCHOLARSHIP_", "IMPORT_DATA",
                "BLOOD_GROUP_", "COMMUNITY_"
            ),
            List.of()
        ),
        new ModuleDefinition(
            FINANCE,
            "Finance",
            List.of("STUDENT_FEE_", "RECEIPT_", "FEE_REFUND_", "COMMISSION_", "FEE_STRUCTURE_"),
            List.of(ACADEMICS)
        ),
        new ModuleDefinition(
            ACADEMICS,
            "Academics",
            List.of(
                "CURRICULUM_", "ATTENDANCE_", "SYLLABUS_", "EXPERIMENT_", "COPO_",
                "COURSE_", "TIMETABLE_", "LAB_", "FACULTY_", "PROGRESS_REPORT_",
                "EXAMINATION_", "EXAM_RESULT_", "STUDENT_PROMOTION_",
                "ACADEMIC_", "SEMESTER_", "CLASSROOM_", "CLINICAL_VENUE_",
                "HOLIDAY_TEMPLATE_", "PERIOD_", "PROGRAM_", "DEPT_", "SUBJECT_"
            ),
            List.of()
        ),
        new ModuleDefinition(
            LIBRARY,
            "Library",
            List.of("LIBRARY_", "MY_LIBRARY_"),
            List.of()
        ),
        new ModuleDefinition(
            CORE_INFRA,
            "Core Infrastructure",
            List.of("CAMPUS_INFRASTRUCTURE_", "ROOM_", "SPATIAL_"),
            List.of()
        ),
        new ModuleDefinition(
            INVENTORY,
            "Inventory Management",
            List.of("INVENTORY_", "MAINTENANCE_", "EQUIPMENT_"),
            List.of()
        ),
        new ModuleDefinition(
            HOSTEL,
            "Hostel Management",
            List.of("HOSTEL_"),
            List.of(CORE_INFRA)
        ),
        new ModuleDefinition(
            REPORTS,
            "Reports & Analytics",
            List.of("REPORT_", "FEE_REPORT_"),
            List.of()
        )
    );

    private ModuleRegistry() {
    }

    public static List<ModuleDefinition> all() {
        return MODULES;
    }

    public static Optional<ModuleDefinition> findByCode(String code) {
        return MODULES.stream().filter(m -> m.code().equals(code)).findFirst();
    }

    /**
     * Resolves which module owns the given permission code, if any. A code that doesn't match
     * any module's owned prefixes is "core" (always available) — represented as {@link Optional#empty()}.
     */
    public static Optional<ModuleDefinition> resolveModuleForPermissionCode(String permissionCode) {
        if (permissionCode == null) {
            return Optional.empty();
        }
        return MODULES.stream()
            .filter(m -> m.ownedPermissionPrefixes().stream().anyMatch(permissionCode::startsWith))
            .findFirst();
    }
}
