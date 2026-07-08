package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByKeycloakUsername(String username);

    /** Loads the user with appRole in a single join — use this whenever appRole fields are accessed outside a @Transactional boundary. */
    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.appRole WHERE u.keycloakUsername = :username")
    Optional<AppUser> findByKeycloakUsernameWithRole(@Param("username") String username);

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByAppRoleHierarchyLevelGreaterThan(int level);

    boolean existsByKeycloakUsername(String username);

    boolean existsByEmail(String email);
}
