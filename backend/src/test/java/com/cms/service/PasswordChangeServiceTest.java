package com.cms.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.cms.dto.ChangePasswordRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    private static final String USERNAME = "faculty.jane";
    private static final String KEYCLOAK_USER_ID = "kc-uuid-123";

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    private PasswordChangeService service;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(appUserRepository, keycloakAdminService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePassword_verifiesResetsAndRevokesOtherSessions_whenCurrentPasswordCorrect() {
        setJwt(USERNAME);
        when(appUserRepository.findByKeycloakUsername(USERNAME)).thenReturn(Optional.of(appUser()));
        when(keycloakAdminService.verifyPassword(USERNAME, "OldPass1")).thenReturn(true);

        service.changePassword(new ChangePasswordRequest("OldPass1", "NewPass2"));

        verify(keycloakAdminService).resetPassword(KEYCLOAK_USER_ID, "NewPass2");
        verify(keycloakAdminService).logoutUserSessions(KEYCLOAK_USER_ID);
    }

    @Test
    void changePassword_rejects_whenCurrentPasswordWrong() {
        setJwt(USERNAME);
        when(appUserRepository.findByKeycloakUsername(USERNAME)).thenReturn(Optional.of(appUser()));
        when(keycloakAdminService.verifyPassword(USERNAME, "WrongPass")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("WrongPass", "NewPass2")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Current password is incorrect");

        verify(keycloakAdminService, never()).resetPassword(anyString(), anyString());
        verify(keycloakAdminService, never()).logoutUserSessions(anyString());
    }

    @Test
    void changePassword_rejects_whenNewPasswordSameAsCurrent() {
        setJwt(USERNAME);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("SamePass1", "SamePass1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different from your current password");

        verify(keycloakAdminService, never()).verifyPassword(anyString(), anyString());
    }

    @Test
    void changePassword_rejects_whenNewPasswordBlank() {
        setJwt(USERNAME);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("OldPass1", " ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("New password is required");
    }

    @Test
    void changePassword_rejects_whenCurrentPasswordBlank() {
        setJwt(USERNAME);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("", "NewPass2")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Current password is required");
    }

    @Test
    void changePassword_throws_whenNoAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("OldPass1", "NewPass2")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_throws_whenAppUserNotFound() {
        setJwt(USERNAME);
        when(appUserRepository.findByKeycloakUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("OldPass1", "NewPass2")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_throws_whenAppUserHasNoKeycloakId() {
        setJwt(USERNAME);
        AppUser user = appUser();
        user.setKeycloakUserId(null);
        when(appUserRepository.findByKeycloakUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("OldPass1", "NewPass2")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not linked to a Keycloak identity");
    }

    @Test
    void changePassword_locksOutAfterThreeFailedAttempts_withinTheWindow() {
        setJwt(USERNAME);
        when(appUserRepository.findByKeycloakUsername(USERNAME)).thenReturn(Optional.of(appUser()));
        when(keycloakAdminService.verifyPassword(eq(USERNAME), any())).thenReturn(false);

        // 3 wrong attempts trips the throttle (MAX_ATTEMPTS = 3)
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("Wrong" + 1, "NewPass2")))
                .isInstanceOf(IllegalArgumentException.class);
        }

        // The 4th attempt is rejected by our own throttle before ever calling Keycloak again.
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("Wrong1", "NewPass2")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Too many incorrect attempts");

        verify(keycloakAdminService, times(3)).verifyPassword(eq(USERNAME), any());
    }

    @Test
    void changePassword_clearsThrottleHistory_afterASuccessfulChange() {
        setJwt(USERNAME);
        when(appUserRepository.findByKeycloakUsername(USERNAME)).thenReturn(Optional.of(appUser()));
        when(keycloakAdminService.verifyPassword(USERNAME, "WrongPass")).thenReturn(false);
        when(keycloakAdminService.verifyPassword(USERNAME, "OldPass1")).thenReturn(true);

        // Two failures, then a correct attempt succeeds and clears the counter.
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("WrongPass", "NewPass2")));
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("WrongPass", "NewPass2")));
        assertThatCode(() -> service.changePassword(new ChangePasswordRequest("OldPass1", "NewPass2")))
            .doesNotThrowAnyException();

        // A subsequent wrong attempt is evaluated fresh (not still counted toward the old lockout).
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("WrongPass", "NewPass2")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Current password is incorrect");
    }

    private AppUser appUser() {
        AppUser user = new AppUser();
        user.setKeycloakUsername(USERNAME);
        user.setKeycloakUserId(KEYCLOAK_USER_ID);
        return user;
    }

    private void setJwt(String username) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", username)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, java.util.List.of()));
    }
}
