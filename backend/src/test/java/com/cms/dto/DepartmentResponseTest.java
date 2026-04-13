package com.cms.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DepartmentResponseTest {

    @Test
    void shouldCreateDepartmentResponse() {
        Instant now = Instant.now();
        DepartmentResponse response = new DepartmentResponse(
            1L,
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe",
            now,
            now
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.description()).isEqualTo("Department of Computer Science");
        assertThat(response.hodName()).isEqualTo("Dr. John Doe");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void shouldCreateDepartmentResponseWithNullValues() {
        DepartmentResponse response = new DepartmentResponse(
            1L,
            "Mathematics",
            "MATH",
            null,
            null,
            null,
            null
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mathematics");
        assertThat(response.code()).isEqualTo("MATH");
        assertThat(response.description()).isNull();
        assertThat(response.hodName()).isNull();
        assertThat(response.createdAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void shouldBeEqualWhenSameValues() {
        Instant now = Instant.now();
        DepartmentResponse response1 = new DepartmentResponse(
            1L, "CS", "CS", "Desc", "Dr. Doe", now, now
        );
        DepartmentResponse response2 = new DepartmentResponse(
            1L, "CS", "CS", "Desc", "Dr. Doe", now, now
        );

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentValues() {
        Instant now = Instant.now();
        DepartmentResponse response1 = new DepartmentResponse(
            1L, "CS", "CS", "Desc", "Dr. Doe", now, now
        );
        DepartmentResponse response2 = new DepartmentResponse(
            2L, "Math", "MATH", "Desc", "Dr. Smith", now, now
        );

        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    void shouldHaveToStringMethod() {
        Instant now = Instant.now();
        DepartmentResponse response = new DepartmentResponse(
            1L, "CS", "CS", "Desc", "Dr. Doe", now, now
        );

        String toString = response.toString();

        assertThat(toString).contains("DepartmentResponse");
        assertThat(toString).contains("CS");
    }
}
