package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Experiment;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findBySubjectId(Long subjectId);

    List<Experiment> findBySubjectIdAndIsActiveTrue(Long subjectId);

    Optional<Experiment> findBySubjectIdAndExperimentNumber(Long subjectId, Integer experimentNumber);

    List<Experiment> findBySubjectIdOrderByExperimentNumberAsc(Long subjectId);

    @Query("select case when count(e) > 0 then true else false end from Experiment e "
        + "where e.subject.id = :subjectId "
        + "and lower(e.name) = lower(:name) "
        + "and (:excludeId is null or e.id <> :excludeId)")
    boolean existsBySubjectAndName(@Param("subjectId") Long subjectId,
                                    @Param("name") String name,
                                    @Param("excludeId") Long excludeId);

    @Query("select case when count(e) > 0 then true else false end from Experiment e "
        + "where e.subject.id = :subjectId "
        + "and e.experimentNumber = :experimentNumber "
        + "and (:excludeId is null or e.id <> :excludeId)")
    boolean existsBySubjectAndExperimentNumber(@Param("subjectId") Long subjectId,
                                                @Param("experimentNumber") Integer experimentNumber,
                                                @Param("excludeId") Long excludeId);
}
