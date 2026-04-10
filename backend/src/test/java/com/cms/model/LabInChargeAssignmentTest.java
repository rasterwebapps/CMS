package com.cms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.cms.model.enums.LabInChargeRole;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;

class LabInChargeAssignmentTest {

    @Test
    void shouldCreateAssignmentWithNoArgConstructor() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();

        assertThat(assignment.getId()).isNull();
        assertThat(assignment.getLab()).isNull();
        assertThat(assignment.getAssigneeId()).isNull();
        assertThat(assignment.getAssigneeName()).isNull();
        assertThat(assignment.getRole()).isNull();
        assertThat(assignment.getAssignedDate()).isNull();
        assertThat(assignment.getCreatedAt()).isNull();
        assertThat(assignment.getUpdatedAt()).isNull();
    }

    @Test
    void shouldCreateAssignmentWithAllArgsConstructor() {
        Department department = new Department("CS", "CS", "CS Dept", "Dr. John");
        Lab lab = new Lab("Computer Lab", LabType.COMPUTER, department, "Building A", "101", 30, LabStatus.ACTIVE);
        LocalDate assignedDate = LocalDate.of(2024, 1, 15);
        
        LabInChargeAssignment assignment = new LabInChargeAssignment(
            lab,
            100L,
            "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE,
            assignedDate
        );

        assertThat(assignment.getLab()).isEqualTo(lab);
        assertThat(assignment.getAssigneeId()).isEqualTo(100L);
        assertThat(assignment.getAssigneeName()).isEqualTo("Dr. Smith");
        assertThat(assignment.getRole()).isEqualTo(LabInChargeRole.LAB_INCHARGE);
        assertThat(assignment.getAssignedDate()).isEqualTo(assignedDate);
    }

    @Test
    void shouldSetAndGetId() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        assignment.setId(1L);

        assertThat(assignment.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetLab() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        Department department = new Department("CS", "CS", "CS Dept", "Dr. John");
        Lab lab = new Lab("Physics Lab", LabType.PHYSICS, department, "Building B", "201", 25, LabStatus.ACTIVE);
        assignment.setLab(lab);

        assertThat(assignment.getLab()).isEqualTo(lab);
    }

    @Test
    void shouldSetAndGetAssigneeId() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        assignment.setAssigneeId(200L);

        assertThat(assignment.getAssigneeId()).isEqualTo(200L);
    }

    @Test
    void shouldSetAndGetAssigneeName() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        assignment.setAssigneeName("Mr. Brown");

        assertThat(assignment.getAssigneeName()).isEqualTo("Mr. Brown");
    }

    @Test
    void shouldSetAndGetRole() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        assignment.setRole(LabInChargeRole.TECHNICIAN);

        assertThat(assignment.getRole()).isEqualTo(LabInChargeRole.TECHNICIAN);
    }

    @Test
    void shouldSetAndGetAssignedDate() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        LocalDate date = LocalDate.of(2024, 6, 1);
        assignment.setAssignedDate(date);

        assertThat(assignment.getAssignedDate()).isEqualTo(date);
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        Instant now = Instant.now();
        assignment.setCreatedAt(now);

        assertThat(assignment.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldSetAndGetUpdatedAt() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        Instant now = Instant.now();
        assignment.setUpdatedAt(now);

        assertThat(assignment.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void shouldHandleAllLabInChargeRoles() {
        LabInChargeAssignment assignment = new LabInChargeAssignment();
        
        for (LabInChargeRole role : LabInChargeRole.values()) {
            assignment.setRole(role);
            assertThat(assignment.getRole()).isEqualTo(role);
        }
    }
}
