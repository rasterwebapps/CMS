package com.cms.repository;

import com.cms.model.UserDashboardWidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDashboardWidgetConfigRepository
        extends JpaRepository<UserDashboardWidgetConfig, Long> {

    List<UserDashboardWidgetConfig> findByUserIdOrderByWidgetOrderAsc(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserDashboardWidgetConfig c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
