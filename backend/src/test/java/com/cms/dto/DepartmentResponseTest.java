package com.cms.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SpecialityResponseTest {

    @Test
    void shouldCreateSpecialityResponse() {
        Instant now = Instant.now();
        SpecialityResponse response = new SpecialityResponse(
            1L,
            "Computer Science",
            "CS",
            "Speciality of Computer Science",
            10L,
            "Dr. John Doe",
            now,
            now
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.description()).isEqualTo("Speciality of Computer Science");
        assertThat(response.hodFacultyId()).isEqualTo(10L);
        assertThat(response.hodName()).isEqualTo("Dr. John Doe");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void shouldCreateSpecialityResponseWithNullValues() {
        SpecialityResponse response = new SpecialityResponse(
            1L, "Mathematics", "MATH", null, null, null, null, null
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mathematics");
        assertThat(response.code()).isEqualTo("MATH");
        assertThat(response.description()).isNull();
        assertThat(response.hodFacultyId()).isNull();
        assertThat(response.hodName()).isNull();
        assertThat(response.createdAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void shouldBeEqualWhenSameValues() {
        Instant now = Instant.now();
        SpecialityResponse response1 = new SpecialityResponse(
            1L, "CS", "CS", "Desc", null, "Dr. Doe", now, now
        );
        SpecialityResponse response2 = new SpecialityResponse(
            1L, "CS", "CS", "Desc", null, "Dr. Doe", now, now
        );

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentValues() {
        Instant now = Instant.now();
        SpecialityResponse response1 = new SpecialityResponse(
            1L, "CS", "CS", "Desc", null, "Dr. Doe", now, now
        );
        SpecialityResponse response2 = new SpecialityResponse(
            2L, "Math", "MATH", "Desc", null, "Dr. Smith", now, now
        );

        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    void shouldHaveToStringMethod() {
        Instant now = Instant.now();
        SpecialityResponse response = new SpecialityResponse(
            1L, "CS", "CS", "Desc", null, "Dr. Doe", now, now
        );

        String toString = response.toString();

        assertThat(toString).contains("SpecialityResponse");
        assertThat(toString).contains("CS");
    }
}
