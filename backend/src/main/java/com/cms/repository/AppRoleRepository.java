package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AppRole;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByName(String name);

    List<AppRole> findByHierarchyLevelGreaterThan(int level);

    List<AppRole> findByIsSystemRoleFalse();

    List<AppRole> findAllByOrderByHierarchyLevelAsc();
}
