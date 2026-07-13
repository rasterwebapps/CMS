package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CurriculumElectiveGroup;

public interface CurriculumElectiveGroupRepository extends JpaRepository<CurriculumElectiveGroup, Long> {

    List<CurriculumElectiveGroup> findByCurriculumVersionIdAndTermNumber(
        Long curriculumVersionId, Integer termNumber);
}
