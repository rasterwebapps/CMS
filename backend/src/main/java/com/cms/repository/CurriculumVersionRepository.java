package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CurriculumVersion;

public interface CurriculumVersionRepository extends JpaRepository<CurriculumVersion, Long> {

    List<CurriculumVersion> findByProgramId(Long programId);

    List<CurriculumVersion> findByProgramIdAndIsActiveTrue(Long programId);
}
