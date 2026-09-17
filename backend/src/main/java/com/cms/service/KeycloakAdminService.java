package com.cms.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper around the Keycloak Admin REST API.
 *
 * Manages user lifecycle (create / delete) inside the configured realm.
 * Credentials are resolved from environment variables so the admin password
 * never appears in source code.
 *
 * Required env vars (with local-dev defaults):
 *   KEYCLOAK_BASE_URL        – e.g. http://localhost:8280
 *   KEYCLOAK_REALM           – e.g. cms
 *   KEYCLOAK_ADMIN_CLIENT_ID – e.g. admin-cli
 *   KEYCLOAK_ADMIN_USERNAME  – e.g. admin
 *   KEYCLOAK_ADMIN_PASSWORD  – (no default – must be set in production)
 */
@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    @Value("${keycloak.admin.base-url:http://localhost:8280}")
    private String baseUrl;

    @Value("${keycloak.admin.realm:cms}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String clientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    // Public SPA client — used only to verify a user's own current password via
    // a direct (resource-owner password credentials) grant. Never used for anything
    // else; it is a public client (no secret) exactly like the frontend's own login.
    @Value("${keycloak.public-client-id:cms-frontend}")
    private String publicClientId;

    private final RestClient restClient = RestClient.create();

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Creates a new user in Keycloak with a temporary password.
     * The user is immediately enabled and will be prompted to change the
     * password on first login.
     *
     * @return the Keycloak user UUID (stored in app_users.keycloak_user_id)
     * @throws IllegalStateException if Keycloak is unreachable or returns an error
     */
    public String createUser(String username, String email,
                             String fullName, String password) {
        String token = getAdminToken();

        // Split full name into first / last (best-effort)
        String[] parts   = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName  = parts.length > 1 ? parts[1] : "";

        Map<String, Object> body = Map.of(
            "username",    username,
            "email",       email,
            "firstName",   firstName,
            "lastName",    lastName,
            "enabled",     true,
            "credentials", List.of(Map.of(
                "type",      "password",
                "value",     password,
                "temporary", true   // user is prompted to change on first login
            ))
        );

        var response = restClient.post()
            .uri(adminUsersUri())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .body(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                String rawBody = "(no body)";
                try { rawBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8); } catch (Exception ignored) {}
                log.error("Keycloak user creation failed: HTTP {} — {}", res.getStatusCode(), rawBody);
                throw new IllegalStateException(friendlyKeycloakError(rawBody));
            })
            .toBodilessEntity();

        // Keycloak returns the new user's URI in the Location header
        var location = response.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a user Location header");
        }
        String path = location.getPath();          // …/admin/realms/cms/users/{uuid}
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Updates the email address of an existing Keycloak user.
     * Called whenever a student or faculty email is changed in CMS so that
     * the login-email → CMS-record link stays intact.
     *
     * @param keycloakUserId the UUID stored in app_users.keycloak_user_id
     * @param newEmail       the new email address
     */
    @SuppressWarnings("unchecked")
    public void updateUserEmail(String keycloakUserId, String newEmail) {
        try {
            String token   = getAdminToken();
            String userUri = adminUsersUri() + "/" + keycloakUserId;

            // Fetch full representation first — Keycloak PUT replaces the entire user,
            // so sending only the email field would wipe username, firstName, lastName, etc.
            Map<String, Object> current = restClient.get()
                .uri(userUri)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(Map.class);

            if (current == null) {
                log.warn("Keycloak user {} not found — email not synced", keycloakUserId);
                return;
            }

            current.put("email", newEmail);
            current.put("emailVerified", true);

            restClient.put()
                .uri(userUri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(current)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) ->
                    log.error("Keycloak email update failed for user {}: HTTP {}", keycloakUserId, res.getStatusCode()))
                .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to sync email to Keycloak for user {} — manual update may be needed", keycloakUserId, ex);
        }
    }

    /**
     * Deletes a Keycloak user by their UUID.
     * Called as a best-effort rollback when the CMS database save fails after
     * the Keycloak user was already created.
     */
    public void deleteUser(String keycloakUserId) {
        try {
            String token = getAdminToken();
            restClient.delete()
                .uri(adminUsersUri() + "/" + keycloakUserId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) ->
                    log.error("Keycloak user delete rollback failed: HTTP {}", res.getStatusCode()))
                .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Keycloak rollback (deleteUser) threw an exception — manual cleanup may be needed", ex);
        }
    }

    /**
     * Verifies a user's current password by attempting a direct password grant
     * against the public frontend client — exactly what the browser's own login
     * does. Returns {@code false} (never throws) for wrong credentials; throws
     * only if Keycloak itself is unreachable, so callers can tell "wrong password"
     * apart from "Keycloak is down".
     */
    @SuppressWarnings("unchecked")
    public boolean verifyPassword(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id",  publicClientId);
        form.add("username",   username);
        form.add("password",   password);
        form.add("grant_type", "password");

        try {
            restClient.post()
                .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
            return true;
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            // 401/400 = wrong username/password — the expected "no" answer.
            return false;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot reach Keycloak at " + baseUrl + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Sets a new permanent (non-temporary) password for an existing user.
     *
     * @param keycloakUserId the UUID stored in app_users.keycloak_user_id
     * @param newPassword    the new password — validated against the realm's
     *                       password policy by Keycloak itself
     * @throws IllegalStateException with a user-friendly message if Keycloak rejects it
     */
    public void resetPassword(String keycloakUserId, String newPassword) {
        String token = getAdminToken();
        Map<String, Object> body = Map.of(
            "type",      "password",
            "value",     newPassword,
            "temporary", false
        );

        restClient.put()
            .uri(adminUsersUri() + "/" + keycloakUserId + "/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .body(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                String rawBody = "(no body)";
                try { rawBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8); } catch (Exception ignored) {}
                log.error("Keycloak password reset failed: HTTP {} — {}", res.getStatusCode(), rawBody);
                throw new IllegalStateException(friendlyKeycloakError(rawBody, "Failed to change password. Please try again."));
            })
            .toBodilessEntity();
    }

    /**
     * Invalidates every other active session for this user (all devices/tabs),
     * called after a successful self-service password change so a leaked old
     * password stops working immediately elsewhere. Best-effort: failures are
     * logged, never thrown, since the password itself has already been changed
     * successfully by the time this runs.
     */
    public void logoutUserSessions(String keycloakUserId) {
        try {
            String token = getAdminToken();
            restClient.post()
                .uri(adminUsersUri() + "/" + keycloakUserId + "/logout")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) ->
                    log.error("Keycloak session logout failed for user {}: HTTP {}", keycloakUserId, res.getStatusCode()))
                .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to revoke other sessions for user {} after password change", keycloakUserId, ex);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id",  clientId);
        form.add("username",   adminUsername);
        form.add("password",   adminPassword);
        form.add("grant_type", "password");

        String tokenUri = baseUrl + "/realms/master/protocol/openid-connect/token";

        try {
            Map<String, Object> resp = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException(
                        "Failed to obtain Keycloak admin token: HTTP " + res.getStatusCode());
                })
                .body(Map.class);

            if (resp == null || !resp.containsKey("access_token")) {
                throw new IllegalStateException("Keycloak token response is empty");
            }
            return (String) resp.get("access_token");

        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot reach Keycloak at " + baseUrl + ": " + ex.getMessage(), ex);
        }
    }

    private String adminUsersUri() {
        return baseUrl + "/admin/realms/" + realm + "/users";
    }

    private static String friendlyKeycloakError(String body) {
        return friendlyKeycloakError(body, "Failed to create user account. Please try again.");
    }

    /**
     * Turns a raw Keycloak error response body into a user-facing message.
     *
     * Keycloak is not consistent about the field name across endpoints — user
     * create/update errors use {@code errorMessage}, while others (e.g.
     * reset-password on a missing user) use a plain {@code error} field — so
     * both are checked. {@code fallback} is caller-supplied because the same
     * generic text ("Failed to create user account…") previously leaked into
     * unrelated flows like password reset whenever neither field was present.
     */
    private static String friendlyKeycloakError(String body, String fallback) {
        if (body == null) return fallback;
        String lower = body.toLowerCase();
        if (lower.contains("password policy")) {
            return "Password does not meet requirements: minimum 8 characters, at least one uppercase letter and one digit.";
        }
        if (lower.contains("same username") || lower.contains("exists with same user")) {
            return "A user with this username already exists.";
        }
        if (lower.contains("same email")) {
            return "A user with this email address already exists.";
        }
        if (lower.contains("user not found")) {
            return "Your account could not be found in the identity system. Please contact an administrator.";
        }
        // Extract the raw message for any other Keycloak error — try every field
        // name Keycloak is known to use across its various admin endpoints.
        for (String field : new String[] {"\"errorMessage\":\"", "\"error_description\":\"", "\"error\":\""}) {
            int start = body.indexOf(field);
            if (start >= 0) {
                start += field.length();
                int end = body.indexOf("\"", start);
                if (end > start) return body.substring(start, end);
            }
        }
        return fallback;
    }
}
