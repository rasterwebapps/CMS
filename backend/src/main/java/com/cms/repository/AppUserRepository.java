package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByKeycloakUsername(String username);

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByAppRoleHierarchyLevelGreaterThan(int level);

    boolean existsByKeycloakUsername(String username);

    boolean existsByEmail(String email);
}
