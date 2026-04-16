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
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.enums.ProgramLevel;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    private ProgramService programService;

    private Department department;

    @BeforeEach
    void setUp() {
        programService = new ProgramService(programRepository, departmentRepository);
        department = createDepartment(1L, "Computer Science", "CS", "CS Department", "Dr. John");
    }

    @Test
    void shouldCreateProgram() {
        ProgramRequest request = new ProgramRequest(
            "Bachelor of Computer Science",
            "BCS",
            ProgramLevel.UNDERGRADUATE,
            4,
            List.of(1L)
        );

        Program savedProgram = createProgram(1L, "Bachelor of Computer Science", "BCS",
            ProgramLevel.UNDERGRADUATE, 4, department);

        when(departmentRepository.findAllById(any())).thenReturn(List.of(department));
        when(programRepository.save(any(Program.class))).thenReturn(savedProgram);

        ProgramResponse response = programService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor of Computer Science");
        assertThat(response.code()).isEqualTo("BCS");
        assertThat(response.programLevel()).isEqualTo(ProgramLevel.UNDERGRADUATE);
        assertThat(response.departments()).hasSize(1);
        assertThat(response.departments().get(0).id()).isEqualTo(1L);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        Program captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Bachelor of Computer Science");
        assertThat(captured.getCode()).isEqualTo("BCS");
    }

    @Test
    void shouldThrowExceptionWhenCreatingProgramWithNonExistentDepartment() {
        ProgramRequest request = new ProgramRequest(
            "Bachelor of Computer Science",
            "BCS",
            ProgramLevel.UNDERGRADUATE,
            4,
            List.of(999L)
        );

        when(departmentRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> programService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("One or more departments not found");

        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldFindAllPrograms() {
        Program prog1 = createProgram(1L, "Bachelor of CS", "BCS", ProgramLevel.UNDERGRADUATE, 4, department);
        Program prog2 = createProgram(2L, "Master of CS", "MCS", ProgramLevel.POSTGRADUATE, 2, department);

        when(programRepository.findAll()).thenReturn(List.of(prog1, prog2));

        List<ProgramResponse> responses = programService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Bachelor of CS");
        assertThat(responses.get(1).name()).isEqualTo("Master of CS");
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
        Program program = createProgram(1L, "Bachelor of CS", "BCS",
            ProgramLevel.UNDERGRADUATE, 4, department);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ProgramResponse response = programService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor of CS");
        assertThat(response.code()).isEqualTo("BCS");
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
        Program existingProgram = createProgram(1L, "Bachelor of CS", "BCS",
            ProgramLevel.UNDERGRADUATE, 4, department);

        Department newDepartment = createDepartment(2L, "Mathematics", "MATH", "Math Dept", "Dr. Jane");

        ProgramRequest updateRequest = new ProgramRequest(
            "Bachelor of Computer Science Updated",
            "BCSU",
            ProgramLevel.UNDERGRADUATE,
            4,
            List.of(2L)
        );

        Program updatedProgram = createProgram(1L, "Bachelor of Computer Science Updated", "BCSU",
            ProgramLevel.UNDERGRADUATE, 4, newDepartment);

        when(programRepository.findById(1L)).thenReturn(Optional.of(existingProgram));
        when(departmentRepository.findAllById(any())).thenReturn(List.of(newDepartment));
        when(programRepository.save(any(Program.class))).thenReturn(updatedProgram);

        ProgramResponse response = programService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor of Computer Science Updated");
        assertThat(response.code()).isEqualTo("BCSU");
        assertThat(response.departments()).hasSize(1);
        assertThat(response.departments().get(0).id()).isEqualTo(2L);

        verify(programRepository).findById(1L);
        verify(programRepository).save(any(Program.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProgram() {
        ProgramRequest request = new ProgramRequest("Name", "CODE", ProgramLevel.UNDERGRADUATE, 4, List.of(1L));

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");

        verify(programRepository).findById(999L);
        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentDepartment() {
        Program existingProgram = createProgram(1L, "Bachelor of CS", "BCS",
            ProgramLevel.UNDERGRADUATE, 4, department);

        ProgramRequest request = new ProgramRequest("Name", "CODE", ProgramLevel.UNDERGRADUATE, 4, List.of(999L));

        when(programRepository.findById(1L)).thenReturn(Optional.of(existingProgram));
        when(departmentRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> programService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("One or more departments not found");

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

    private Department createDepartment(Long id, String name, String code,
                                        String description, String hodName) {
        Department dept = new Department(name, code, description, hodName);
        dept.setId(id);
        Instant now = Instant.now();
        dept.setCreatedAt(now);
        dept.setUpdatedAt(now);
        return dept;
    }

    private Program createProgram(Long id, String name, String code,
                                  ProgramLevel programLevel, Integer durationYears, Department dept) {
        Program program = new Program(name, code, programLevel, durationYears);
        if (dept != null) {
            program.setDepartments(new java.util.HashSet<>(java.util.Set.of(dept)));
        }
        program.setId(id);
        Instant now = Instant.now();
        program.setCreatedAt(now);
        program.setUpdatedAt(now);
        return program;
    }
}
