package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.FeeStructure;
import com.cms.model.Program;
import com.cms.model.enums.FeeType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class FeeStructureServiceTest {

    @Mock
    private FeeStructureRepository feeStructureRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;

    private FeeStructureService feeStructureService;

    private Program testProgram;
    private AcademicYear testAcademicYear;

    @BeforeEach
    void setUp() {
        feeStructureService = new FeeStructureService(feeStructureRepository, programRepository, academicYearRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testAcademicYear = new AcademicYear();
        testAcademicYear.setId(1L);
        testAcademicYear.setName("2024-25");
    }

    @Test
    void shouldCreateFeeStructure() {
        FeeStructureRequest request = new FeeStructureRequest(
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), "Annual tuition fee", true, true
        );

        FeeStructure saved = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(saved);

        FeeStructureResponse response = feeStructureService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.feeType()).isEqualTo(FeeType.TUITION);
        assertThat(response.amount()).isEqualTo(new BigDecimal("50000.00"));
    }

    @Test
    void shouldThrowExceptionWhenProgramNotFound() {
        FeeStructureRequest request = new FeeStructureRequest(
            999L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), null, true, true
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenAcademicYearNotFoundOnCreate() {
        FeeStructureRequest request = new FeeStructureRequest(
            1L, 999L, FeeType.TUITION, new BigDecimal("50000.00"), null, true, true
        );

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenProgramNotFoundOnFindByProgramId() {
        when(programRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feeStructureService.findByProgramId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldFindByProgramIdAndAcademicYearId() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(programRepository.existsById(1L)).thenReturn(true);
        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of(fs));

        List<FeeStructureResponse> responses = feeStructureService.findByProgramIdAndAcademicYearId(1L, 1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenProgramNotFoundOnFindByProgramAndYear() {
        when(programRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feeStructureService.findByProgramIdAndAcademicYearId(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenAcademicYearNotFoundOnFindByProgramAndYear() {
        when(programRepository.existsById(1L)).thenReturn(true);
        when(academicYearRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feeStructureService.findByProgramIdAndAcademicYearId(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");
    }

    @Test
    void shouldFindAllFeeStructures() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureRepository.findAll()).thenReturn(List.of(fs));

        List<FeeStructureResponse> responses = feeStructureService.findAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindById() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(fs));

        FeeStructureResponse response = feeStructureService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenNotFoundById() {
        when(feeStructureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee structure not found with id: 999");
    }

    @Test
    void shouldFindByProgramId() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(programRepository.existsById(1L)).thenReturn(true);
        when(feeStructureRepository.findByProgramId(1L)).thenReturn(List.of(fs));

        List<FeeStructureResponse> responses = feeStructureService.findByProgramId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldUpdateFeeStructure() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        FeeStructureRequest updateRequest = new FeeStructureRequest(
            1L, 1L, FeeType.LAB_FEE, new BigDecimal("10000.00"), "Lab fee", true, true
        );

        FeeStructure updated = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.LAB_FEE, new BigDecimal("10000.00"));

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(updated);

        FeeStructureResponse response = feeStructureService.update(1L, updateRequest);

        assertThat(response.feeType()).isEqualTo(FeeType.LAB_FEE);
        assertThat(response.amount()).isEqualTo(new BigDecimal("10000.00"));
    }

    @Test
    void shouldDeleteFeeStructure() {
        when(feeStructureRepository.existsById(1L)).thenReturn(true);

        feeStructureService.delete(1L);

        verify(feeStructureRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistent() {
        when(feeStructureRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feeStructureService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee structure not found with id: 999");

        verify(feeStructureRepository, never()).deleteById(any());
    }

    private FeeStructure createFeeStructure(Long id, Program program, AcademicYear academicYear,
                                             FeeType feeType, BigDecimal amount) {
        FeeStructure fs = new FeeStructure(program, academicYear, feeType, amount, true, true);
        fs.setId(id);
        Instant now = Instant.now();
        fs.setCreatedAt(now);
        fs.setUpdatedAt(now);
        return fs;
    }
}
