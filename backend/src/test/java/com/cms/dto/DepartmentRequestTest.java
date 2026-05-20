package com.cms.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DepartmentRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateValidDepartmentRequest() {
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science",
            "CS",
            "Department of Computer Science",
            null
        );

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.name()).isEqualTo("Computer Science");
        assertThat(request.code()).isEqualTo("CS");
        assertThat(request.description()).isEqualTo("Department of Computer Science");
        assertThat(request.hodFacultyId()).isNull();
    }

    @Test
    void shouldCreateValidDepartmentRequestWithHodFacultyId() {
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science",
            "CS",
            "Department of Computer Science",
            42L
        );

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.hodFacultyId()).isEqualTo(42L);
    }

    @Test
    void shouldFailValidationWhenNameIsBlank() {
        DepartmentRequest request = new DepartmentRequest("", "CS", "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Name is required");
    }

    @Test
    void shouldFailValidationWhenNameIsNull() {
        DepartmentRequest request = new DepartmentRequest(null, "CS", "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Name is required");
    }

    @Test
    void shouldFailValidationWhenCodeIsBlank() {
        DepartmentRequest request = new DepartmentRequest("Computer Science", "", "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Code is required");
    }

    @Test
    void shouldFailValidationWhenCodeIsNull() {
        DepartmentRequest request = new DepartmentRequest("Computer Science", null, "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Code is required");
    }

    @Test
    void shouldFailValidationWhenNameExceeds255Characters() {
        String longName = "a".repeat(256);
        DepartmentRequest request = new DepartmentRequest(longName, "CS", "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Name must not exceed 255 characters");
    }

    @Test
    void shouldFailValidationWhenCodeExceeds50Characters() {
        String longCode = "a".repeat(51);
        DepartmentRequest request = new DepartmentRequest("Computer Science", longCode, "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Code must not exceed 50 characters");
    }

    @Test
    void shouldFailValidationWhenDescriptionExceeds1000Characters() {
        String longDescription = "a".repeat(1001);
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science", "CS", longDescription, null
        );

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Description must not exceed 1000 characters");
    }

    @Test
    void shouldAllowNullDescription() {
        DepartmentRequest request = new DepartmentRequest("Computer Science", "CS", null, null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowNullHodFacultyId() {
        DepartmentRequest request = new DepartmentRequest("Computer Science", "CS", "Description", null);

        Set<ConstraintViolation<DepartmentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
