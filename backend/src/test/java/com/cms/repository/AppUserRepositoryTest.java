package com.cms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.cms.config.JpaConfig;
import com.cms.model.AppRole;
import com.cms.model.AppUser;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AppRoleRepository appRoleRepository;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        appRoleRepository.deleteAll();
    }

    // ── Bug fix: Keycloak issues `preferred_username` lowercased regardless of the casing an
    // admin typed when the account was created, so lookups must be case-insensitive or a
    // mixed-/upper-case username can never resolve its own account again ────────────────────────

    @Test
    void shouldFindByKeycloakUsernameRegardlessOfCase() {
        AppRole role = appRoleRepository.save(new AppRole("ACCOUNTS", "Accounts", 3, false, null));
        appUserRepository.save(new AppUser(
            "SKSCONACCOUNTS", "priya.p@nursing.sksh.ac.in", "Priya P", role, true, "system"));

        Optional<AppUser> found = appUserRepository.findByKeycloakUsername("sksconaccounts");

        assertThat(found).isPresent();
        assertThat(found.get().getKeycloakUsername()).isEqualTo("SKSCONACCOUNTS");
    }

    @Test
    void shouldFindByKeycloakUsernameWithRoleRegardlessOfCase() {
        AppRole role = appRoleRepository.save(new AppRole("ACCOUNTS", "Accounts", 3, false, null));
        appUserRepository.save(new AppUser(
            "SKSCONACCOUNTS", "priya.p@nursing.sksh.ac.in", "Priya P", role, true, "system"));

        Optional<AppUser> found = appUserRepository.findByKeycloakUsernameWithRole("sksconaccounts");

        assertThat(found).isPresent();
        assertThat(found.get().getAppRole().getName()).isEqualTo("ACCOUNTS");
    }

    @Test
    void shouldReturnEmptyWhenKeycloakUsernameNotFoundEvenIgnoringCase() {
        Optional<AppUser> found = appUserRepository.findByKeycloakUsername("nosuchuser");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldTreatKeycloakUsernameAsExistingRegardlessOfCase() {
        AppRole role = appRoleRepository.save(new AppRole("ACCOUNTS", "Accounts", 3, false, null));
        appUserRepository.save(new AppUser(
            "SKSCONACCOUNTS", "priya.p@nursing.sksh.ac.in", "Priya P", role, true, "system"));

        assertThat(appUserRepository.existsByKeycloakUsername("sksconaccounts")).isTrue();
        assertThat(appUserRepository.existsByKeycloakUsername("SKSCONACCOUNTS")).isTrue();
        assertThat(appUserRepository.existsByKeycloakUsername("someoneelse")).isFalse();
    }
}
