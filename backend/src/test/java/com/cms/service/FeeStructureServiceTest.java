package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.cms.dto.BulkFeeStructureRequest;
import com.cms.dto.FeeStructureItemRequest;
import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.FeeStructure;
import com.cms.model.Program;
import com.cms.model.enums.FeeType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class FeeStructureServiceTest {

    @Mock
    private FeeStructureRepository feeStructureRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private FeeStructureYearAmountRepository yearAmountRepository;
    @Mock
    private CourseRepository courseRepository;

    private FeeStructureService feeStructureService;

    private Program testProgram;
    private AcademicYear testAcademicYear;

    @BeforeEach
    void setUp() {
        feeStructureService = new FeeStructureService(feeStructureRepository, programRepository, academicYearRepository, yearAmountRepository, courseRepository);

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
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), "Annual tuition fee", true, true, null, null
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
            999L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, null, null
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenAcademicYearNotFoundOnCreate() {
        FeeStructureRequest request = new FeeStructureRequest(
            1L, 999L, FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, null, null
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
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

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
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<FeeStructureResponse> responses = feeStructureService.findAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindById() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

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
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<FeeStructureResponse> responses = feeStructureService.findByProgramId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldUpdateFeeStructure() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        FeeStructureRequest updateRequest = new FeeStructureRequest(
            1L, 1L, FeeType.LAB_FEE, new BigDecimal("10000.00"), "Lab fee", true, true, null, null
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
    void shouldThrowExceptionWhenNotFoundOnUpdate() {
        FeeStructureRequest updateRequest = new FeeStructureRequest(
            1L, 1L, FeeType.LAB_FEE, new BigDecimal("10000.00"), "Lab fee", true, true, null, null
        );

        when(feeStructureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.update(999L, updateRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee structure not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenProgramNotFoundOnUpdate() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        FeeStructureRequest updateRequest = new FeeStructureRequest(
            999L, 1L, FeeType.LAB_FEE, new BigDecimal("10000.00"), "Lab fee", true, true, null, null
        );

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.update(1L, updateRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenAcademicYearNotFoundOnUpdate() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        FeeStructureRequest updateRequest = new FeeStructureRequest(
            1L, 999L, FeeType.LAB_FEE, new BigDecimal("10000.00"), "Lab fee", true, true, null, null
        );

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.update(1L, updateRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");
    }

    @Test
    void shouldDeleteFeeStructure() {
        when(feeStructureRepository.existsById(1L)).thenReturn(true);

        feeStructureService.delete(1L);

        verify(yearAmountRepository).deleteByFeeStructureId(1L);
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

    @Test
    void shouldFindByProgramIdAndCourseId() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(programRepository.existsById(1L)).thenReturn(true);
        when(feeStructureRepository.findByProgramIdAndCourseId(1L, 2L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<FeeStructureResponse> responses = feeStructureService.findByProgramIdAndCourseId(1L, 2L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowWhenProgramNotFoundOnFindByProgramIdAndCourseId() {
        when(programRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feeStructureService.findByProgramIdAndCourseId(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldCreateFeeStructureWithCourse() {
        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101",
            null, testProgram);
        testCourse.setId(1L);

        FeeStructureRequest request = new FeeStructureRequest(
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), "Tuition fee", true, true, 1L, null
        );

        FeeStructure saved = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        saved.setCourse(testCourse);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(saved);

        FeeStructureResponse response = feeStructureService.create(request);

        assertThat(response.courseId()).isEqualTo(1L);
        assertThat(response.courseName()).isEqualTo("CS101");
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnCreate() {
        FeeStructureRequest request = new FeeStructureRequest(
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, 999L, null
        );

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldUpdateFeeStructureWithCourse() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101",
            null, testProgram);
        testCourse.setId(1L);

        FeeStructureRequest updateRequest = new FeeStructureRequest(
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), "Updated", true, true, 1L, null
        );

        FeeStructure updated = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        updated.setCourse(testCourse);

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(updated);

        FeeStructureResponse response = feeStructureService.update(1L, updateRequest);

        assertThat(response.courseId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnUpdate() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        FeeStructureRequest updateRequest = new FeeStructureRequest(
            1L, 1L, FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, 999L, null
        );

        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.update(1L, updateRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldBulkCreateFeeStructures() {
        FeeStructureItemRequest tuition = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("50000.00"), "Tuition", true, true, null
        );
        FeeStructureItemRequest lab = new FeeStructureItemRequest(
            FeeType.LAB_FEE, new BigDecimal("5000.00"), "Lab", true, true, null
        );
        BulkFeeStructureRequest request = new BulkFeeStructureRequest(1L, 1L, null, List.of(tuition, lab));

        FeeStructure savedTuition = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        FeeStructure savedLab = createFeeStructure(2L, testProgram, testAcademicYear, FeeType.LAB_FEE, new BigDecimal("5000.00"));

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(feeStructureRepository.save(any(FeeStructure.class)))
            .thenReturn(savedTuition)
            .thenReturn(savedLab);

        List<FeeStructureResponse> responses = feeStructureService.bulkCreate(request);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).feeType()).isEqualTo(FeeType.TUITION);
        assertThat(responses.get(1).feeType()).isEqualTo(FeeType.LAB_FEE);
    }

    @Test
    void shouldThrowWhenBulkCreateHasDuplicateFeeTypes() {
        FeeStructureItemRequest tuition1 = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, null
        );
        FeeStructureItemRequest tuition2 = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("60000.00"), null, true, true, null
        );
        BulkFeeStructureRequest request = new BulkFeeStructureRequest(1L, 1L, null, List.of(tuition1, tuition2));

        assertThatThrownBy(() -> feeStructureService.bulkCreate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate fee type");
    }

    @Test
    void shouldThrowWhenBulkCreateProgramNotFound() {
        FeeStructureItemRequest item = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, null
        );
        BulkFeeStructureRequest request = new BulkFeeStructureRequest(999L, 1L, null, List.of(item));

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.bulkCreate(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowWhenBulkCreateAcademicYearNotFound() {
        FeeStructureItemRequest item = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, null
        );
        BulkFeeStructureRequest request = new BulkFeeStructureRequest(1L, 999L, null, List.of(item));

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.bulkCreate(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");
    }

    @Test
    void shouldBulkCreateWithCourseAndYearAmounts() {
        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101", null, testProgram);
        testCourse.setId(1L);

        com.cms.dto.YearAmountRequest ya1 = new com.cms.dto.YearAmountRequest(1, "Year 1", new BigDecimal("25000.00"));
        com.cms.dto.YearAmountRequest ya2 = new com.cms.dto.YearAmountRequest(2, "Year 2", new BigDecimal("25000.00"));
        FeeStructureItemRequest item = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("50000.00"), null, true, true, List.of(ya1, ya2)
        );
        BulkFeeStructureRequest request = new BulkFeeStructureRequest(1L, 1L, 1L, List.of(item));

        FeeStructure saved = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        saved.setCourse(testCourse);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(saved);
        when(yearAmountRepository.save(any(com.cms.model.FeeStructureYearAmount.class))).thenAnswer(inv -> inv.getArgument(0));

        List<FeeStructureResponse> responses = feeStructureService.bulkCreate(request);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
    }

    @Test
    void shouldFindGroupedFeeStructures() {
        FeeStructure fs1 = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        FeeStructure fs2 = createFeeStructure(2L, testProgram, testAcademicYear, FeeType.LAB_FEE, new BigDecimal("5000.00"));

        when(feeStructureRepository.findAll()).thenReturn(List.of(fs1, fs2));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<com.cms.dto.GroupedFeeStructureResponse> result = feeStructureService.findGrouped(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(result.get(0).items()).hasSize(2);
    }

    @Test
    void shouldFindGroupedWithProgramFilter() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureRepository.findByProgramId(1L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<com.cms.dto.GroupedFeeStructureResponse> result = feeStructureService.findGrouped(1L, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).programId()).isEqualTo(1L);
    }

    @Test
    void shouldFindGroupedWithAcademicYearFilter() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureRepository.findByAcademicYearId(1L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<com.cms.dto.GroupedFeeStructureResponse> result = feeStructureService.findGrouped(null, 1L, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindGroupedWithProgramAndAcademicYearFilter() {
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        when(feeStructureRepository.findByProgramIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<com.cms.dto.GroupedFeeStructureResponse> result = feeStructureService.findGrouped(1L, 1L, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindGroupedWithProgramAndCourseFilter() {
        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101", null, testProgram);
        testCourse.setId(2L);
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        fs.setCourse(testCourse);

        when(feeStructureRepository.findByProgramIdAndCourseId(1L, 2L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<com.cms.dto.GroupedFeeStructureResponse> result = feeStructureService.findGrouped(1L, null, 2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).courseId()).isEqualTo(2L);
    }

    @Test
    void shouldFindGroupedWithAllFilters() {
        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101", null, testProgram);
        testCourse.setId(2L);
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        fs.setCourse(testCourse);

        when(feeStructureRepository.findByProgramIdAndCourseIdAndAcademicYearId(1L, 2L, 1L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdOrderByYearNumber(anyLong())).thenReturn(List.of());

        List<com.cms.dto.GroupedFeeStructureResponse> result = feeStructureService.findGrouped(1L, 1L, 2L);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldBulkUpdateFeeStructures() {
        FeeStructure existing = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));

        FeeStructureItemRequest newTuition = new FeeStructureItemRequest(
            FeeType.TUITION, new BigDecimal("60000.00"), "Updated", true, true, null
        );
        BulkFeeStructureRequest request = new BulkFeeStructureRequest(1L, 1L, null, List.of(newTuition));

        FeeStructure savedNew = createFeeStructure(2L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("60000.00"));

        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndCourseIsNull(1L, 1L)).thenReturn(List.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(savedNew);

        List<FeeStructureResponse> responses = feeStructureService.bulkUpdate(request);

        verify(yearAmountRepository).deleteByFeeStructureId(1L);
        verify(feeStructureRepository).deleteById(1L);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).amount()).isEqualByComparingTo(new BigDecimal("60000.00"));
    }

    @Test
    void shouldDeleteGroup() {
        FeeStructure fs1 = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        FeeStructure fs2 = createFeeStructure(2L, testProgram, testAcademicYear, FeeType.LAB_FEE, new BigDecimal("5000.00"));

        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndCourseIsNull(1L, 1L)).thenReturn(List.of(fs1, fs2));

        feeStructureService.deleteGroup(1L, 1L, null);

        verify(yearAmountRepository).deleteByFeeStructureId(1L);
        verify(yearAmountRepository).deleteByFeeStructureId(2L);
        verify(feeStructureRepository).deleteById(1L);
        verify(feeStructureRepository).deleteById(2L);
    }

    @Test
    void shouldDeleteGroupWithCourse() {
        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101", null, testProgram);
        testCourse.setId(2L);
        FeeStructure fs = createFeeStructure(1L, testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"));
        fs.setCourse(testCourse);

        when(feeStructureRepository.findByProgramIdAndCourseIdAndAcademicYearId(1L, 2L, 1L)).thenReturn(List.of(fs));

        feeStructureService.deleteGroup(1L, 1L, 2L);

        verify(yearAmountRepository).deleteByFeeStructureId(1L);
        verify(feeStructureRepository).deleteById(1L);
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
