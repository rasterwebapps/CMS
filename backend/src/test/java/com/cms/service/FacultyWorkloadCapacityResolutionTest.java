package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;

/** Pure per-faculty-then-designation resolution logic for the daily/continuous caps, kept
 *  separate from {@link FacultyWorkloadCapacityServiceTest} since these don't touch any
 *  repository and shouldn't share its {@code @BeforeEach} mock setup. */
class FacultyWorkloadCapacityResolutionTest {

    private Faculty faculty(DesignationMaster designation) {
        Faculty f = new Faculty();
        f.setId(4L);
        f.setFirstName("Priya");
        f.setLastName("");
        f.setDesignation(designation);
        return f;
    }

    private DesignationMaster designation() {
        DesignationMaster d = new DesignationMaster("Professor", "PROF", null);
        d.setId(1L);
        return d;
    }

    @Test
    void resolveEffectiveDailyCapacity_facultyOverrideWinsOverDesignationDefault() {
        DesignationMaster designation = designation();
        designation.setDefaultDailyTeachingSessions(4);
        Faculty f = faculty(designation);
        f.setPlannedDailySessionsOverride(7);

        assertThat(FacultyWorkloadCapacityService.resolveEffectiveDailyCapacity(f)).isEqualTo(7);
    }

    @Test
    void resolveEffectiveDailyCapacity_fallsBackToDesignationDefaultWhenNoFacultyOverride() {
        DesignationMaster designation = designation();
        designation.setDefaultDailyTeachingSessions(4);
        Faculty f = faculty(designation);

        assertThat(FacultyWorkloadCapacityService.resolveEffectiveDailyCapacity(f)).isEqualTo(4);
    }

    @Test
    void resolveEffectiveDailyCapacity_nullWhenNeitherFacultyNorDesignationConfigured() {
        Faculty f = faculty(designation());

        assertThat(FacultyWorkloadCapacityService.resolveEffectiveDailyCapacity(f)).isNull();
    }

    @Test
    void resolveEffectiveContinuousCapacity_facultyOverrideWinsOverDesignationDefault() {
        DesignationMaster designation = designation();
        designation.setDefaultContinuousTeachingSessions(3);
        Faculty f = faculty(designation);
        f.setPlannedContinuousSessionsOverride(5);

        assertThat(FacultyWorkloadCapacityService.resolveEffectiveContinuousCapacity(f)).isEqualTo(5);
    }

    @Test
    void resolveEffectiveContinuousCapacity_fallsBackToDesignationDefaultWhenNoFacultyOverride() {
        DesignationMaster designation = designation();
        designation.setDefaultContinuousTeachingSessions(3);
        Faculty f = faculty(designation);

        assertThat(FacultyWorkloadCapacityService.resolveEffectiveContinuousCapacity(f)).isEqualTo(3);
    }

    @Test
    void resolveEffectiveContinuousCapacity_nullWhenNeitherFacultyNorDesignationConfigured() {
        Faculty f = faculty(designation());

        assertThat(FacultyWorkloadCapacityService.resolveEffectiveContinuousCapacity(f)).isNull();
    }
}
