package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ProgramStatusUpdateRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.CourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.IntakeRuleRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class ProgramLifecycleStatusToggleTest {

    @Mock
    private ProgramRepository programRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private FeeStructureGroupRepository feeStructureGroupRepository;
    @Mock
    private IntakeRuleRepository intakeRuleRepository;
    @Mock
    private CurriculumVersionRepository curriculumVersionRepository;

    private ProgramService programService;

    @BeforeEach
    void setUp() {
        programService = new ProgramService(
            programRepository,
            courseRepository,
            feeStructureGroupRepository,
            intakeRuleRepository,
            curriculumVersionRepository
        );
    }

    @Test
    void shouldDeactivateProgramWhenNoActiveDependenciesExist() {
        Program program = createProgram(1L, ProgramStatus.ACTIVE);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramIdAndIsActiveTrue(1L)).thenReturn(false);
        when(intakeRuleRepository.existsByProgramIdAndIsActiveTrue(1L)).thenReturn(false);
        when(curriculumVersionRepository.existsByProgramIdAndIsActiveTrue(1L)).thenReturn(false);
        when(feeStructureGroupRepository.existsByProgramIdAndIsActiveTrue(1L)).thenReturn(false);
        when(programRepository.save(any(Program.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = programService.updateStatus(1L, new ProgramStatusUpdateRequest(ProgramStatus.INACTIVE, "retired"));

        assertThat(response.status()).isEqualTo(ProgramStatus.INACTIVE);
    }

    @Test
    void shouldBlockDeactivationWhenActiveCoursesExist() {
        Program program = createProgram(1L, ProgramStatus.ACTIVE);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramIdAndIsActiveTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> programService.updateStatus(
            1L,
            new ProgramStatusUpdateRequest(ProgramStatus.INACTIVE, "retired")
        ))
            .isInstanceOf(LifecycleConflictException.class)
            .satisfies(ex -> {
                LifecycleConflictException conflict = (LifecycleConflictException) ex;
                assertThat(conflict.getCode()).isEqualTo("ACTIVE_CHILD_EXISTS");
            });
    }

    private Program createProgram(Long id, ProgramStatus status) {
        Program program = new Program("Bachelor", "BACHELOR", 4);
        program.setId(id);
        program.setStatus(status);
        Instant now = Instant.now();
        program.setCreatedAt(now);
        program.setUpdatedAt(now);
        return program;
    }
}

