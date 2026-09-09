package com.cms.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Locks in the module ⇄ permission-code mapping — in particular the cross-module collisions
 * that were deliberately avoided when the prefixes were chosen (e.g. {@code STUDENT_FEE_*}
 * resolving to Finance, not Student Management, even though both start with {@code STUDENT_}-ish
 * text). See docs/module-architecture/MODULE_REGISTRY.md.
 */
class ModuleRegistryTest {

    @Test
    void resolvesEachSampleCodeToItsIntendedModule() {
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("ENQUIRY_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.ADMISSIONS);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("FEE_FINALIZE"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.ADMISSIONS);

        assertThat(ModuleRegistry.resolveModuleForPermissionCode("STUDENT_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.STUDENT_MGMT);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("SCHOLARSHIP_APPROVE"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.STUDENT_MGMT);

        // STUDENT_FEE_* must resolve to Finance, NOT Student Management, despite the shared
        // "STUDENT_" text — this is the collision the prefix choices were designed to avoid.
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("STUDENT_FEE_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.FINANCE);
        // Likewise STUDENT_PROMOTION_* must resolve to Academics, not Student Management.
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("STUDENT_PROMOTION_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.ACADEMICS);

        assertThat(ModuleRegistry.resolveModuleForPermissionCode("TIMETABLE_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.ACADEMICS);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("LIBRARY_ISSUE_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.LIBRARY);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("CAMPUS_INFRASTRUCTURE_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.CORE_INFRA);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("INVENTORY_STOCK_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.INVENTORY);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("MAINTENANCE_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.INVENTORY);

        // HOSTEL_ROOM_* must resolve to Hostel, not Core Infrastructure's bare "ROOM_" prefix.
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("HOSTEL_ROOM_TYPE_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.HOSTEL);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("ROOM_PURPOSE_CATEGORY_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.CORE_INFRA);

        // FEE_REPORT_* must resolve to Reports, not Finance.
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("FEE_REPORT_VIEW"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.REPORTS);
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("FEE_REFUND_APPROVE"))
            .map(ModuleDefinition::code).contains(ModuleRegistry.FINANCE);
    }

    @Test
    void coreCodesHaveNoModuleMapping() {
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("USER_VIEW")).isEmpty();
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("ROLE_VIEW")).isEmpty();
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("DESIGNATION_MANAGE")).isEmpty();
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("SETTINGS_EDIT")).isEmpty();
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("NUMBER_SEQUENCE_VIEW")).isEmpty();
        assertThat(ModuleRegistry.resolveModuleForPermissionCode("INDIA_LOCATION_VIEW")).isEmpty();
    }
}
