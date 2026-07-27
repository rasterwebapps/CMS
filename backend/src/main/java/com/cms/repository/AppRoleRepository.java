package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.AppRole;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByName(String name);

    List<AppRole> findByHierarchyLevelGreaterThan(int level);

    List<AppRole> findByIsSystemRoleFalse();

    List<AppRole> findAllByOrderByHierarchyLevelAsc();

    /** Roles currently holding the given permission code — used for tier-change impact preview and auto-revoke. */
    @Query("SELECT r FROM AppRole r JOIN r.permissions p WHERE p.code = :code")
    List<AppRole> findByPermissionCode(@Param("code") String code);
}
