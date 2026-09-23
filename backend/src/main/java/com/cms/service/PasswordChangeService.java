package com.cms.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.cms.dto.ChangePasswordRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

/**
 * Self-service "change my password" flow, entirely in-app (no redirect to
 * Keycloak's own account console — see OC password-change UX change).
 *
 * The current password is verified against Keycloak before the new one is
 * accepted. Repeated wrong-password attempts are throttled here, with a
 * short cooldown well below Keycloak's own realm-wide brute-force lockout
 * (5 failed attempts locks the whole account, not just this form) — so a
 * user who mistypes their current password a few times gets a friendly
 * "try again shortly" from this screen instead of being locked out of the
 * entire application.
 */
@Service
public class PasswordChangeService {

    private static final int  MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_MS   = 5 * 60_000L;   // 5 minutes
    private static final long WINDOW_MS    = 10 * 60_000L;  // failed attempts older than this don't count

    private record AttemptState(int count, long firstFailureAt, long lockedUntil) {}

    // Keyed by Keycloak username. Single-instance in-memory throttle — same
    // pattern as RateLimitFilter's per-IP bucket map.
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    private final AppUserRepository appUserRepository;
    private final KeycloakAdminService keycloakAdminService;

    public PasswordChangeService(AppUserRepository appUserRepository,
                                 KeycloakAdminService keycloakAdminService) {
        this.appUserRepository    = appUserRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    public void changePassword(ChangePasswordRequest request) {
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new IllegalArgumentException("New password must be different from your current password");
        }

        Jwt jwt = resolveJwt();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }

        AppUser appUser = appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        String keycloakUserId = appUser.getKeycloakUserId();
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new IllegalStateException("This account is not linked to a Keycloak identity — contact an administrator.");
        }

        checkNotLocked(username);

        boolean verified = keycloakAdminService.verifyPassword(username, request.currentPassword());
        if (!verified) {
            recordFailure(username);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Correct current password — clear any throttle history for this user.
        attempts.remove(username);

        keycloakAdminService.resetPassword(keycloakUserId, request.newPassword());
        keycloakAdminService.logoutUserSessions(keycloakUserId);
    }

    private void checkNotLocked(String username) {
        AttemptState state = attempts.get(username);
        if (state == null) return;

        long now = System.currentTimeMillis();
        if (state.lockedUntil() > now) {
            long minutesLeft = Math.max(1, (state.lockedUntil() - now) / 60_000L + 1);
            throw new IllegalStateException(
                "Too many incorrect attempts. Please try again in " + minutesLeft
                    + (minutesLeft == 1 ? " minute." : " minutes."));
        }
    }

    private void recordFailure(String username) {
        long now = System.currentTimeMillis();
        attempts.compute(username, (key, existing) -> {
            if (existing == null || now - existing.firstFailureAt() >= WINDOW_MS) {
                return new AttemptState(1, now, 0L);
            }
            int nextCount = existing.count() + 1;
            long lockedUntil = nextCount >= MAX_ATTEMPTS ? now + LOCKOUT_MS : 0L;
            return new AttemptState(nextCount, existing.firstFailureAt(), lockedUntil);
        });
    }

    private Jwt resolveJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        return jwt;
    }
}
