package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Username is required")
    String keycloakUsername,

    @NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Role name is required")
    String roleName,

    /** Link to a specific student record. Set when creating a student login account. */
    Long studentId,

    /** Link to a specific faculty record. Set when creating a faculty login account. */
    Long facultyId
) {}
