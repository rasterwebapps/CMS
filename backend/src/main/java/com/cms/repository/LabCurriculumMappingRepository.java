package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LabCurriculumMapping;
import com.cms.model.enums.OutcomeType;

public interface LabCurriculumMappingRepository extends JpaRepository<LabCurriculumMapping, Long> {

    List<LabCurriculumMapping> findByExperimentId(Long experimentId);

    List<LabCurriculumMapping> findByExperimentIdAndOutcomeType(Long experimentId, OutcomeType outcomeType);

    List<LabCurriculumMapping> findByOutcomeCode(String outcomeCode);

    List<LabCurriculumMapping> findByOutcomeTypeAndOutcomeCode(OutcomeType outcomeType, String outcomeCode);

    boolean existsByExperimentIdAndOutcomeTypeAndOutcomeCode(Long experimentId, OutcomeType outcomeType, String outcomeCode);

    void deleteByExperimentId(Long experimentId);
}
