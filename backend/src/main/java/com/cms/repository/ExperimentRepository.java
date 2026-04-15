package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Experiment;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findBySubjectId(Long subjectId);

    List<Experiment> findBySubjectIdAndIsActiveTrue(Long subjectId);

    Optional<Experiment> findBySubjectIdAndExperimentNumber(Long subjectId, Integer experimentNumber);

    boolean existsBySubjectIdAndExperimentNumber(Long subjectId, Integer experimentNumber);

    List<Experiment> findBySubjectIdOrderByExperimentNumberAsc(Long subjectId);
}
