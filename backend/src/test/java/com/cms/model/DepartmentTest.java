package com.cms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DepartmentTest {

    @Test
    void shouldCreateDepartmentWithNoArgConstructor() {
        Department department = new Department();

        assertThat(department.getId()).isNull();
        assertThat(department.getName()).isNull();
        assertThat(department.getCode()).isNull();
        assertThat(department.getDescription()).isNull();
        assertThat(department.getHodName()).isNull();
        assertThat(department.getCreatedAt()).isNull();
        assertThat(department.getUpdatedAt()).isNull();
    }

    @Test
    void shouldCreateDepartmentWithAllArgsConstructor() {
        Department department = new Department(
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe"
        );

        assertThat(department.getName()).isEqualTo("Computer Science");
        assertThat(department.getCode()).isEqualTo("CS");
        assertThat(department.getDescription()).isEqualTo("Department of Computer Science");
        assertThat(department.getHodName()).isEqualTo("Dr. John Doe");
    }

    @Test
    void shouldSetAndGetId() {
        Department department = new Department();
        department.setId(1L);

        assertThat(department.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetName() {
        Department department = new Department();
        department.setName("Mathematics");

        assertThat(department.getName()).isEqualTo("Mathematics");
    }

    @Test
    void shouldSetAndGetCode() {
        Department department = new Department();
        department.setCode("MATH");

        assertThat(department.getCode()).isEqualTo("MATH");
    }

    @Test
    void shouldSetAndGetDescription() {
        Department department = new Department();
        department.setDescription("Department of Mathematics");

        assertThat(department.getDescription()).isEqualTo("Department of Mathematics");
    }

    @Test
    void shouldSetAndGetHodName() {
        Department department = new Department();
        department.setHodName("Dr. Jane Smith");

        assertThat(department.getHodName()).isEqualTo("Dr. Jane Smith");
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        Department department = new Department();
        Instant now = Instant.now();
        department.setCreatedAt(now);

        assertThat(department.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldSetAndGetUpdatedAt() {
        Department department = new Department();
        Instant now = Instant.now();
        department.setUpdatedAt(now);

        assertThat(department.getUpdatedAt()).isEqualTo(now);
    }
}
