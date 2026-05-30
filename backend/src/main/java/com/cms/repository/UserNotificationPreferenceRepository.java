package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.UserNotificationPreference;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    List<UserNotificationPreference> findByUserId(String userId);

    Optional<UserNotificationPreference> findByUserIdAndCategoryKey(String userId, String categoryKey);

    void deleteByUserId(String userId);
}
