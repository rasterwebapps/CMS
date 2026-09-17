package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
}
