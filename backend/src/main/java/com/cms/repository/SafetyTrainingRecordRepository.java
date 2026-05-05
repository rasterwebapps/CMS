package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SafetyTrainingRecord;
import com.cms.model.enums.TraineeType;
import com.cms.model.enums.TrainingStatus;

public interface SafetyTrainingRecordRepository extends JpaRepository<SafetyTrainingRecord, Long> {

    List<SafetyTrainingRecord> findByLabId(Long labId);

    List<SafetyTrainingRecord> findByTraineeType(TraineeType traineeType);

    List<SafetyTrainingRecord> findByStatus(TrainingStatus status);
}

