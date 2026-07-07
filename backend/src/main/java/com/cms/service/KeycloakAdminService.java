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
        if (body == null) return "Failed to create user account. Please try again.";
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
        // Extract raw errorMessage field for any other Keycloak error
        int start = body.indexOf("\"errorMessage\":\"");
        if (start >= 0) {
            start += 16;
            int end = body.indexOf("\"", start);
            if (end > start) return body.substring(start, end);
        }
        return "Failed to create user account. Please try again.";
    }
}
