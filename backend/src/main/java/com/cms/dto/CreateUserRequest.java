package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Keycloak username is required")
    String keycloakUsername,

    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Role name is required")
    String roleName
) {}
