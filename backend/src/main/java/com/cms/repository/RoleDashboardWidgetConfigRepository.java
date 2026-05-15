package com.cms.repository;

import com.cms.model.RoleDashboardWidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleDashboardWidgetConfigRepository
        extends JpaRepository<RoleDashboardWidgetConfig, Long> {

    List<RoleDashboardWidgetConfig> findByRoleIdOrderByWidgetOrderAsc(Long roleId);

    @Modifying
    @Query("DELETE FROM RoleDashboardWidgetConfig c WHERE c.role.id = :roleId")
    void deleteAllByRoleId(@Param("roleId") Long roleId);
}
