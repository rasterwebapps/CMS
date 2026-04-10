package com.cms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;

class LabTest {

    @Test
    void shouldCreateLabWithNoArgConstructor() {
        Lab lab = new Lab();

        assertThat(lab.getId()).isNull();
        assertThat(lab.getName()).isNull();
        assertThat(lab.getLabType()).isNull();
        assertThat(lab.getDepartment()).isNull();
        assertThat(lab.getBuilding()).isNull();
        assertThat(lab.getRoomNumber()).isNull();
        assertThat(lab.getCapacity()).isNull();
        assertThat(lab.getStatus()).isNull();
        assertThat(lab.getCreatedAt()).isNull();
        assertThat(lab.getUpdatedAt()).isNull();
    }

    @Test
    void shouldCreateLabWithAllArgsConstructor() {
        Department department = new Department("CS", "CS", "CS Dept", "Dr. John");
        Lab lab = new Lab(
            "Computer Lab 1",
            LabType.COMPUTER,
            department,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        assertThat(lab.getName()).isEqualTo("Computer Lab 1");
        assertThat(lab.getLabType()).isEqualTo(LabType.COMPUTER);
        assertThat(lab.getDepartment()).isEqualTo(department);
        assertThat(lab.getBuilding()).isEqualTo("Main Building");
        assertThat(lab.getRoomNumber()).isEqualTo("101");
        assertThat(lab.getCapacity()).isEqualTo(30);
        assertThat(lab.getStatus()).isEqualTo(LabStatus.ACTIVE);
    }

    @Test
    void shouldSetAndGetId() {
        Lab lab = new Lab();
        lab.setId(1L);

        assertThat(lab.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetName() {
        Lab lab = new Lab();
        lab.setName("Physics Lab");

        assertThat(lab.getName()).isEqualTo("Physics Lab");
    }

    @Test
    void shouldSetAndGetLabType() {
        Lab lab = new Lab();
        lab.setLabType(LabType.PHYSICS);

        assertThat(lab.getLabType()).isEqualTo(LabType.PHYSICS);
    }

    @Test
    void shouldSetAndGetDepartment() {
        Lab lab = new Lab();
        Department department = new Department("Physics", "PHY", "Physics Dept", "Dr. Einstein");
        lab.setDepartment(department);

        assertThat(lab.getDepartment()).isEqualTo(department);
    }

    @Test
    void shouldSetAndGetBuilding() {
        Lab lab = new Lab();
        lab.setBuilding("Science Building");

        assertThat(lab.getBuilding()).isEqualTo("Science Building");
    }

    @Test
    void shouldSetAndGetRoomNumber() {
        Lab lab = new Lab();
        lab.setRoomNumber("A201");

        assertThat(lab.getRoomNumber()).isEqualTo("A201");
    }

    @Test
    void shouldSetAndGetCapacity() {
        Lab lab = new Lab();
        lab.setCapacity(50);

        assertThat(lab.getCapacity()).isEqualTo(50);
    }

    @Test
    void shouldSetAndGetStatus() {
        Lab lab = new Lab();
        lab.setStatus(LabStatus.UNDER_MAINTENANCE);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.UNDER_MAINTENANCE);
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        Lab lab = new Lab();
        Instant now = Instant.now();
        lab.setCreatedAt(now);

        assertThat(lab.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldSetAndGetUpdatedAt() {
        Lab lab = new Lab();
        Instant now = Instant.now();
        lab.setUpdatedAt(now);

        assertThat(lab.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void shouldHandleAllLabTypes() {
        Lab lab = new Lab();
        
        for (LabType type : LabType.values()) {
            lab.setLabType(type);
            assertThat(lab.getLabType()).isEqualTo(type);
        }
    }

    @Test
    void shouldHandleAllLabStatuses() {
        Lab lab = new Lab();
        
        for (LabStatus status : LabStatus.values()) {
            lab.setStatus(status);
            assertThat(lab.getStatus()).isEqualTo(status);
        }
    }
}
