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
import java.util.Set;

import com.cms.model.enums.DocumentType;

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
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private FeeStructureRepository feeStructureRepository;

    private ProgramService programService;

    @BeforeEach
    void setUp() {
        programService = new ProgramService(programRepository, feeStructureRepository);
    }

    @Test
    void shouldCreateProgram() {
        ProgramRequest request = new ProgramRequest("Bachelor", "BACHELOR", 4, null, null);

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
        ProgramRequest updateRequest = new ProgramRequest("Bachelor Updated", "BACHELOR", 4, null, null);
        Program updatedProgram = createProgram(1L, "Bachelor Updated", "BACHELOR", 4);

        when(programRepository.findById(1L)).thenReturn(Optional.of(existingProgram));
        when(programRepository.existsByNameIgnoreCaseAndIdNot("Bachelor Updated", 1L)).thenReturn(false);
        when(programRepository.existsByCodeIgnoreCaseAndIdNot("BACHELOR", 1L)).thenReturn(false);
        when(programRepository.save(any(Program.class))).thenReturn(updatedProgram);

        ProgramResponse response = programService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Bachelor Updated");

        verify(programRepository).findById(1L);
        verify(programRepository).save(any(Program.class));
    }

    @Test
    void shouldThrowWhenUpdatingProgramWithDuplicateName() {
        Program existing = createProgram(1L, "Bachelor", "BACHELOR", 4);
        ProgramRequest request = new ProgramRequest("Master", "BACHELOR", 4, null, null);

        when(programRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.existsByNameIgnoreCaseAndIdNot("Master", 1L)).thenReturn(true);

        assertThatThrownBy(() -> programService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Master")
            .hasMessageContaining("already exists");

        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldThrowWhenUpdatingProgramWithDuplicateCode() {
        Program existing = createProgram(1L, "Bachelor", "BACHELOR", 4);
        ProgramRequest request = new ProgramRequest("Bachelor", "MASTER", 4, null, null);

        when(programRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.existsByNameIgnoreCaseAndIdNot("Bachelor", 1L)).thenReturn(false);
        when(programRepository.existsByCodeIgnoreCaseAndIdNot("MASTER", 1L)).thenReturn(true);

        assertThatThrownBy(() -> programService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MASTER")
            .hasMessageContaining("already exists");

        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProgram() {
        ProgramRequest request = new ProgramRequest("Name", "CODE", 4, null, null);

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
        when(feeStructureRepository.existsByProgramId(1L)).thenReturn(false);

        programService.delete(1L);

        verify(programRepository).existsById(1L);
        verify(programRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingProgramWithFeeStructures() {
        when(programRepository.existsById(1L)).thenReturn(true);
        when(feeStructureRepository.existsByProgramId(1L)).thenReturn(true);

        assertThatThrownBy(() -> programService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fee structures");

        verify(programRepository, never()).deleteById(any());
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

    @Test
    void shouldThrowWhenCodeIsLowercase() {
        ProgramRequest request = new ProgramRequest("Bachelor", "bachelor", 4, null, null);

        assertThatThrownBy(() -> programService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uppercase");

        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldThrowWhenCodeHasSpaces() {
        ProgramRequest request = new ProgramRequest("Bachelor", "BSC N", 4, null, null);

        assertThatThrownBy(() -> programService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("spaces");

        verify(programRepository, never()).save(any(Program.class));
    }

    @Test
    void shouldCreateProgramWithStatus() {
        ProgramRequest request = new ProgramRequest("Bachelor", "BACHELOR", 4, com.cms.model.enums.ProgramStatus.INACTIVE, null);
        Program saved = createProgram(1L, "Bachelor", "BACHELOR", 4);
        saved.setStatus(com.cms.model.enums.ProgramStatus.INACTIVE);

        when(programRepository.save(any(Program.class))).thenReturn(saved);

        ProgramResponse response = programService.create(request);

        assertThat(response.status()).isEqualTo(com.cms.model.enums.ProgramStatus.INACTIVE);
    }

    @Test
    void shouldExposeCorrectTotalSemesters() {
        Program program = createProgram(1L, "Bachelor", "BACHELOR", 4);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ProgramResponse response = programService.findById(1L);

        assertThat(response.totalTerms()).isEqualTo(8);
    }

    @Test
    void shouldGetRequiredDocumentTypes() {
        Program program = createProgram(1L, "Bachelor", "BACHELOR", 4);
        program.setRequiredDocumentTypes(Set.of(DocumentType.TENTH_MARKSHEET, DocumentType.AADHAR_CARD));

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        Set<DocumentType> result = programService.getRequiredDocumentTypes(1L);

        assertThat(result).containsExactlyInAnyOrder(DocumentType.TENTH_MARKSHEET, DocumentType.AADHAR_CARD);
    }

    @Test
    void shouldThrowWhenGettingDocumentTypesForNonExistentProgram() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.getRequiredDocumentTypes(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldSetRequiredDocumentTypes() {
        Program program = createProgram(1L, "Bachelor", "BACHELOR", 4);
        Set<DocumentType> types = Set.of(DocumentType.TWELFTH_MARKSHEET, DocumentType.PASSPORT_PHOTO);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRepository.save(any(Program.class))).thenReturn(program);

        Set<DocumentType> result = programService.setRequiredDocumentTypes(1L, types);

        assertThat(result).containsExactlyInAnyOrder(DocumentType.TWELFTH_MARKSHEET, DocumentType.PASSPORT_PHOTO);
        verify(programRepository).save(any(Program.class));
    }

    @Test
    void shouldSetRequiredDocumentTypesToEmptyWhenNullPassed() {
        Program program = createProgram(1L, "Bachelor", "BACHELOR", 4);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRepository.save(any(Program.class))).thenReturn(program);

        Set<DocumentType> result = programService.setRequiredDocumentTypes(1L, null);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowWhenSettingDocumentTypesForNonExistentProgram() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.setRequiredDocumentTypes(999L, Set.of()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
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

