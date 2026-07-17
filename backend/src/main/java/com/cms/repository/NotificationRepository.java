package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findBySourceTypeAndSourceIdAndCategoryKeyAndResolvedAtIsNull(
        String sourceType, Long sourceId, String categoryKey);

    List<Notification> findByCategoryKeyAndResolvedAtIsNull(String categoryKey);

    List<Notification> findByResolvedAtIsNullOrderByCreatedAtDesc();
}
