package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LibrarySetting;

public interface LibrarySettingRepository extends JpaRepository<LibrarySetting, Long> {

    Optional<LibrarySetting> findBySettingKey(String settingKey);
}
