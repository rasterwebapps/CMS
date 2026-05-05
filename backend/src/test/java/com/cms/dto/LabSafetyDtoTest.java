package com.cms.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.cms.model.enums.IncidentSeverity;
import com.cms.model.enums.IncidentStatus;
import com.cms.model.enums.IncidentType;
import com.cms.model.enums.PpeCategory;
import com.cms.model.enums.PpeCondition;
import com.cms.model.enums.SafetyGuidelineCategory;
import com.cms.model.enums.SafetyPriority;
import com.cms.model.enums.TraineeType;
import com.cms.model.enums.TrainingStatus;

/**
 * Canonical-constructor and accessor coverage for lab-safety DTOs.
 * These Java records were added as part of the lab-safety compliance
 * feature; the services/controllers that will use them are pending.
 * Until those are implemented these thin tests ensure every accessor
 * is exercised so coverage remains at the project threshold.
 */
class LabSafetyDtoTest {

    // -------------------------------------------------------------------------
    // IncidentReportRequest
    // -------------------------------------------------------------------------

    @Test
    void incidentReportRequest_accessors() {
        LocalDate date = LocalDate.of(2026, 5, 4);
        LocalTime time = LocalTime.of(10, 30);

        IncidentReportRequest req = new IncidentReportRequest(
            1L, "faculty1", "faculty1@college.edu", date, time,
            "Chemical Spill", "Minor spill in lab 3", IncidentSeverity.MINOR,
            IncidentType.CHEMICAL_SPILL, IncidentStatus.REPORTED,
            "Cleaned up", "safety_officer", null, "Store chemicals properly"
        );

        assertThat(req.labId()).isEqualTo(1L);
        assertThat(req.reportedBy()).isEqualTo("faculty1");
        assertThat(req.reportedByEmail()).isEqualTo("faculty1@college.edu");
        assertThat(req.incidentDate()).isEqualTo(date);
        assertThat(req.incidentTime()).isEqualTo(time);
        assertThat(req.title()).isEqualTo("Chemical Spill");
        assertThat(req.description()).isEqualTo("Minor spill in lab 3");
        assertThat(req.severity()).isEqualTo(IncidentSeverity.MINOR);
        assertThat(req.incidentType()).isEqualTo(IncidentType.CHEMICAL_SPILL);
        assertThat(req.status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(req.actionTaken()).isEqualTo("Cleaned up");
        assertThat(req.investigatedBy()).isEqualTo("safety_officer");
        assertThat(req.resolvedDate()).isNull();
        assertThat(req.preventiveMeasures()).isEqualTo("Store chemicals properly");
    }

    // -------------------------------------------------------------------------
    // IncidentReportResponse
    // -------------------------------------------------------------------------

    @Test
    void incidentReportResponse_accessors() {
        Instant now = Instant.now();
        LocalDate date = LocalDate.of(2026, 5, 4);

        IncidentReportResponse resp = new IncidentReportResponse(
            10L, 1L, "Physics Lab", "faculty1", "faculty1@college.edu",
            date, null, "Chemical Spill", "Minor spill", IncidentSeverity.MINOR,
            IncidentType.CHEMICAL_SPILL, IncidentStatus.RESOLVED,
            "Cleaned up", "safety_officer", date.plusDays(1),
            "Store chemicals properly", now, now
        );

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.labId()).isEqualTo(1L);
        assertThat(resp.labName()).isEqualTo("Physics Lab");
        assertThat(resp.reportedBy()).isEqualTo("faculty1");
        assertThat(resp.reportedByEmail()).isEqualTo("faculty1@college.edu");
        assertThat(resp.incidentDate()).isEqualTo(date);
        assertThat(resp.incidentTime()).isNull();
        assertThat(resp.title()).isEqualTo("Chemical Spill");
        assertThat(resp.description()).isEqualTo("Minor spill");
        assertThat(resp.severity()).isEqualTo(IncidentSeverity.MINOR);
        assertThat(resp.incidentType()).isEqualTo(IncidentType.CHEMICAL_SPILL);
        assertThat(resp.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resp.actionTaken()).isEqualTo("Cleaned up");
        assertThat(resp.investigatedBy()).isEqualTo("safety_officer");
        assertThat(resp.resolvedDate()).isEqualTo(date.plusDays(1));
        assertThat(resp.preventiveMeasures()).isEqualTo("Store chemicals properly");
        assertThat(resp.createdAt()).isEqualTo(now);
        assertThat(resp.updatedAt()).isEqualTo(now);
    }

    // -------------------------------------------------------------------------
    // PpeItemRequest
    // -------------------------------------------------------------------------

    @Test
    void ppeItemRequest_accessors() {
        LocalDate inspection = LocalDate.of(2026, 1, 1);

        PpeItemRequest req = new PpeItemRequest(
            2L, "Safety Goggles", PpeCategory.EYE_PROTECTION,
            50, 45, 10, PpeCondition.GOOD, inspection, inspection.plusMonths(6)
        );

        assertThat(req.labId()).isEqualTo(2L);
        assertThat(req.name()).isEqualTo("Safety Goggles");
        assertThat(req.category()).isEqualTo(PpeCategory.EYE_PROTECTION);
        assertThat(req.totalQuantity()).isEqualTo(50);
        assertThat(req.availableQuantity()).isEqualTo(45);
        assertThat(req.minimumRequired()).isEqualTo(10);
        assertThat(req.condition()).isEqualTo(PpeCondition.GOOD);
        assertThat(req.lastInspectionDate()).isEqualTo(inspection);
        assertThat(req.nextInspectionDate()).isEqualTo(inspection.plusMonths(6));
    }

    // -------------------------------------------------------------------------
    // PpeItemResponse
    // -------------------------------------------------------------------------

    @Test
    void ppeItemResponse_accessors() {
        Instant now = Instant.now();
        LocalDate date = LocalDate.of(2026, 1, 1);

        PpeItemResponse resp = new PpeItemResponse(
            5L, 2L, "Chemistry Lab", "Safety Goggles",
            PpeCategory.EYE_PROTECTION, 50, 45, 10,
            PpeCondition.GOOD, date, date.plusMonths(6),
            true, now, now
        );

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.labId()).isEqualTo(2L);
        assertThat(resp.labName()).isEqualTo("Chemistry Lab");
        assertThat(resp.name()).isEqualTo("Safety Goggles");
        assertThat(resp.category()).isEqualTo(PpeCategory.EYE_PROTECTION);
        assertThat(resp.totalQuantity()).isEqualTo(50);
        assertThat(resp.availableQuantity()).isEqualTo(45);
        assertThat(resp.minimumRequired()).isEqualTo(10);
        assertThat(resp.condition()).isEqualTo(PpeCondition.GOOD);
        assertThat(resp.lastInspectionDate()).isEqualTo(date);
        assertThat(resp.nextInspectionDate()).isEqualTo(date.plusMonths(6));
        assertThat(resp.isActive()).isTrue();
        assertThat(resp.createdAt()).isEqualTo(now);
        assertThat(resp.updatedAt()).isEqualTo(now);
    }

    // -------------------------------------------------------------------------
    // SafetyGuidelineRequest
    // -------------------------------------------------------------------------

    @Test
    void safetyGuidelineRequest_accessors() {
        LocalDate effective = LocalDate.of(2026, 6, 1);

        SafetyGuidelineRequest req = new SafetyGuidelineRequest(
            "Handling Acids", "Always wear PPE", 2L, 1L,
            SafetyGuidelineCategory.CHEMICAL, SafetyPriority.HIGH,
            effective, effective.plusYears(1), "admin"
        );

        assertThat(req.title()).isEqualTo("Handling Acids");
        assertThat(req.description()).isEqualTo("Always wear PPE");
        assertThat(req.labId()).isEqualTo(2L);
        assertThat(req.departmentId()).isEqualTo(1L);
        assertThat(req.category()).isEqualTo(SafetyGuidelineCategory.CHEMICAL);
        assertThat(req.priority()).isEqualTo(SafetyPriority.HIGH);
        assertThat(req.effectiveDate()).isEqualTo(effective);
        assertThat(req.reviewDate()).isEqualTo(effective.plusYears(1));
        assertThat(req.createdBy()).isEqualTo("admin");
    }

    // -------------------------------------------------------------------------
    // SafetyGuidelineResponse
    // -------------------------------------------------------------------------

    @Test
    void safetyGuidelineResponse_accessors() {
        Instant now = Instant.now();
        LocalDate date = LocalDate.of(2026, 6, 1);

        SafetyGuidelineResponse resp = new SafetyGuidelineResponse(
            7L, "Handling Acids", "Always wear PPE", 2L, "Chemistry Lab",
            1L, "Chemistry Dept", SafetyGuidelineCategory.CHEMICAL,
            SafetyPriority.HIGH, true, date, date.plusYears(1), "admin", now, now
        );

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.title()).isEqualTo("Handling Acids");
        assertThat(resp.description()).isEqualTo("Always wear PPE");
        assertThat(resp.labId()).isEqualTo(2L);
        assertThat(resp.labName()).isEqualTo("Chemistry Lab");
        assertThat(resp.departmentId()).isEqualTo(1L);
        assertThat(resp.departmentName()).isEqualTo("Chemistry Dept");
        assertThat(resp.category()).isEqualTo(SafetyGuidelineCategory.CHEMICAL);
        assertThat(resp.priority()).isEqualTo(SafetyPriority.HIGH);
        assertThat(resp.isActive()).isTrue();
        assertThat(resp.effectiveDate()).isEqualTo(date);
        assertThat(resp.reviewDate()).isEqualTo(date.plusYears(1));
        assertThat(resp.createdBy()).isEqualTo("admin");
        assertThat(resp.createdAt()).isEqualTo(now);
        assertThat(resp.updatedAt()).isEqualTo(now);
    }

    // -------------------------------------------------------------------------
    // SafetyTrainingRecordRequest
    // -------------------------------------------------------------------------

    @Test
    void safetyTrainingRecordRequest_accessors() {
        LocalDate trainingDate = LocalDate.of(2026, 3, 15);

        SafetyTrainingRecordRequest req = new SafetyTrainingRecordRequest(
            "student001", TraineeType.STUDENT, "Lab Safety Orientation",
            "Basic lab safety practices", 2L, "Dr. Smith",
            trainingDate, trainingDate.plusYears(1), TrainingStatus.COMPLETED, 85
        );

        assertThat(req.trainee()).isEqualTo("student001");
        assertThat(req.traineeType()).isEqualTo(TraineeType.STUDENT);
        assertThat(req.trainingTitle()).isEqualTo("Lab Safety Orientation");
        assertThat(req.description()).isEqualTo("Basic lab safety practices");
        assertThat(req.labId()).isEqualTo(2L);
        assertThat(req.conductedBy()).isEqualTo("Dr. Smith");
        assertThat(req.trainingDate()).isEqualTo(trainingDate);
        assertThat(req.validUntil()).isEqualTo(trainingDate.plusYears(1));
        assertThat(req.status()).isEqualTo(TrainingStatus.COMPLETED);
        assertThat(req.score()).isEqualTo(85);
    }
}

