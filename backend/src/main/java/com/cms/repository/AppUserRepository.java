package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Case-insensitive on purpose: Keycloak issues the {@code preferred_username} JWT claim
     * lowercased regardless of the casing an admin typed into the Add User form when the
     * account's {@code keycloak_username} row was created, so a case-sensitive match here
     * silently 404s every lookup for any username containing an uppercase letter — see the
     * "SKSCONACCOUNTS" incident where the DB stored the uppercase form but Keycloak always
     * returned "sksconaccounts", so this account (and any other created the same way) could
     * never resolve its own permissions no matter what role it held.
     */
    @Query("SELECT u FROM AppUser u WHERE LOWER(u.keycloakUsername) = LOWER(:username)")
    Optional<AppUser> findByKeycloakUsername(@Param("username") String username);

    /** Loads the user with appRole in a single join — use this whenever appRole fields are accessed outside a @Transactional boundary. */
    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.appRole WHERE LOWER(u.keycloakUsername) = LOWER(:username)")
    Optional<AppUser> findByKeycloakUsernameWithRole(@Param("username") String username);

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByAppRoleHierarchyLevelGreaterThan(int level);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM AppUser u WHERE LOWER(u.keycloakUsername) = LOWER(:username)")
    boolean existsByKeycloakUsername(@Param("username") String username);

    boolean existsByEmail(String email);

    /** Active-and-inactive user count for a role — used for tier-change impact preview. */
    long countByAppRoleId(Long roleId);
}
