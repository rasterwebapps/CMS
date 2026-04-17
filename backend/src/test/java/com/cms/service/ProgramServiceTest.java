package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ProgramRequest;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Program;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    private ProgramService programService;

    @BeforeEach
    void setUp() {
        programService = new ProgramService(programRepository);
    }

    @Test
    void shouldCreateProgram() {
        ProgramRequest request = new ProgramRequest("Bachelor", "BACHELOR", 4);

        Program savedProgram = createProgram(1L, "Bachelor", "BACHELOR", 4);

        when(programRepository.save(any(Program.class))).thenReturn(savedProgram);

        ProgramResponse response = programService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor");
        assertThat(response.code()).isEqualTo("BACHELOR");
        assertThat(response.durationYears()).isEqualTo(4);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        Program captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Bachelor");
        assertThat(captured.getCode()).isEqualTo("BACHELOR");
    }

    @Test
    void shouldFindAllPrograms() {
        Program prog1 = createProgram(1L, "Bachelor", "BACHELOR", 4);
        Program prog2 = createProgram(2L, "Master",   "MASTER",   2);

        when(programRepository.findAll()).thenReturn(List.of(prog1, prog2));

        List<ProgramResponse> responses = programService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Bachelor");
        assertThat(responses.get(1).name()).isEqualTo("Master");
        verify(programRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoPrograms() {
        when(programRepository.findAll()).thenReturn(List.of());

        List<ProgramResponse> responses = programService.findAll();

        assertThat(responses).isEmpty();
        verify(programRepository).findAll();
    }

    @Test
    void shouldFindProgramById() {
        Program program = createProgram(1L, "Bachelor", "BACHELOR", 4);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ProgramResponse response = programService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor");
        assertThat(response.code()).isEqualTo("BACHELOR");
        verify(programRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenProgramNotFoundById() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(programRepository).findById(999L);
    }

    @Test
    void shouldUpdateProgram() {
        Program existingProgram = createProgram(1L, "Bachelor", "BACHELOR", 4);
        ProgramRequest updateRequest = new ProgramRequest("Bachelor Updated", "BACHELOR", 4);
        Program updatedProgram = createProgram(1L, "Bachelor Updated", "BACHELOR", 4);

        when(programRepository.findById(1L)).thenReturn(Optional.of(existingProgram));
        when(programRepository.save(any(Program.class))).thenReturn(updatedProgram);

        ProgramResponse response = programService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor Updated");

        verify(programRepository).findById(1L);
        verify(programRepository).save(any(Program.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProgram() {
        ProgramRequest request = new ProgramRequest("Name", "CODE", 4);

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(programRepository).findById(999L);
        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldDeleteProgram() {
        when(programRepository.existsById(1L)).thenReturn(true);

        programService.delete(1L);

        verify(programRepository).existsById(1L);
        verify(programRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProgram() {
        when(programRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> programService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(programRepository).existsById(999L);
        verify(programRepository, never()).deleteById(any());
    }

    private Program createProgram(Long id, String name, String code, Integer durationYears) {
        Program program = new Program(name, code, durationYears);
        program.setId(id);
        Instant now = Instant.now();
        program.setCreatedAt(now);
        program.setUpdatedAt(now);
        return program;
    }
}

