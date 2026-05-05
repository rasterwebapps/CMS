package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SafetyGuideline;
import com.cms.model.enums.SafetyGuidelineCategory;

public interface SafetyGuidelineRepository extends JpaRepository<SafetyGuideline, Long> {

    List<SafetyGuideline> findByLabId(Long labId);

    List<SafetyGuideline> findByDepartmentId(Long departmentId);

    List<SafetyGuideline> findByCategory(SafetyGuidelineCategory category);

    List<SafetyGuideline> findByIsActiveTrue();
}

