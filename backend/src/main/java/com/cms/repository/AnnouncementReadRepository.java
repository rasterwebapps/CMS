package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AnnouncementRead;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    boolean existsByAnnouncementIdAndUserId(Long announcementId, String userId);

    List<AnnouncementRead> findByUserIdAndAnnouncementIdIn(String userId, List<Long> announcementIds);
}
