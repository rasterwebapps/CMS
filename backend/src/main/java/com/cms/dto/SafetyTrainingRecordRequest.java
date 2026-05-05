package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.TraineeType;
import com.cms.model.enums.TrainingStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SafetyTrainingRecordRequest(
    @NotBlank String trainee,
    @NotNull TraineeType traineeType,
    @NotBlank String trainingTitle,
    String description,
    Long labId,
    @NotBlank String conductedBy,
    @NotNull LocalDate trainingDate,
    LocalDate validUntil,
    @NotNull TrainingStatus status,
    Integer score
) {}

