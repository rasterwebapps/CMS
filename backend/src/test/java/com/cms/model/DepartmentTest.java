package com.cms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SpecialityTest {

    @Test
    void shouldCreateSpecialityWithNoArgConstructor() {
        Speciality speciality = new Speciality();

        assertThat(speciality.getId()).isNull();
        assertThat(speciality.getName()).isNull();
        assertThat(speciality.getCode()).isNull();
        assertThat(speciality.getDescription()).isNull();
        assertThat(speciality.getHodName()).isNull();
        assertThat(speciality.getCreatedAt()).isNull();
        assertThat(speciality.getUpdatedAt()).isNull();
    }

    @Test
    void shouldCreateSpecialityWithAllArgsConstructor() {
        Speciality speciality = new Speciality(
            "Computer Science",
            "CS",
            "Speciality of Computer Science",
            "Dr. John Doe"
        );

        assertThat(speciality.getName()).isEqualTo("Computer Science");
        assertThat(speciality.getCode()).isEqualTo("CS");
        assertThat(speciality.getDescription()).isEqualTo("Speciality of Computer Science");
        assertThat(speciality.getHodName()).isEqualTo("Dr. John Doe");
    }

    @Test
    void shouldSetAndGetId() {
        Speciality speciality = new Speciality();
        speciality.setId(1L);

        assertThat(speciality.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetName() {
        Speciality speciality = new Speciality();
        speciality.setName("Mathematics");

        assertThat(speciality.getName()).isEqualTo("Mathematics");
    }

    @Test
    void shouldSetAndGetCode() {
        Speciality speciality = new Speciality();
        speciality.setCode("MATH");

        assertThat(speciality.getCode()).isEqualTo("MATH");
    }

    @Test
    void shouldSetAndGetDescription() {
        Speciality speciality = new Speciality();
        speciality.setDescription("Speciality of Mathematics");

        assertThat(speciality.getDescription()).isEqualTo("Speciality of Mathematics");
    }

    @Test
    void shouldSetAndGetHodName() {
        Speciality speciality = new Speciality();
        speciality.setHodName("Dr. Jane Smith");

        assertThat(speciality.getHodName()).isEqualTo("Dr. Jane Smith");
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        Speciality speciality = new Speciality();
        Instant now = Instant.now();
        speciality.setCreatedAt(now);

        assertThat(speciality.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldSetAndGetUpdatedAt() {
        Speciality speciality = new Speciality();
        Instant now = Instant.now();
        speciality.setUpdatedAt(now);

        assertThat(speciality.getUpdatedAt()).isEqualTo(now);
    }
}
