package com.cms.dto;

/**
 * Self-service password change request. The current password is required and
 * verified against Keycloak before the new one is accepted — see
 * {@link com.cms.service.PasswordChangeService}.
 */
public record ChangePasswordRequest(
    String currentPassword,
    String newPassword
) {}
