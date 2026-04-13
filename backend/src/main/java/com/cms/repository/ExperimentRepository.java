package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Experiment;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByCourseId(Long courseId);

    List<Experiment> findByCourseIdAndIsActiveTrue(Long courseId);

    Optional<Experiment> findByCourseIdAndExperimentNumber(Long courseId, Integer experimentNumber);

    boolean existsByCourseIdAndExperimentNumber(Long courseId, Integer experimentNumber);

    List<Experiment> findByCourseIdOrderByExperimentNumberAsc(Long courseId);
}
