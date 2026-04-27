package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CurriculumVersionDto;
import com.cms.dto.CurriculumVersionRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class CurriculumVersionServiceTest {

    @Mock
    private CurriculumVersionRepository curriculumVersionRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    private CurriculumVersionService curriculumVersionService;

    private Program testProgram;
    private AcademicYear testAcademicYear;

    @BeforeEach
    void setUp() {
        curriculumVersionService = new CurriculumVersionService(
            curriculumVersionRepository, programRepository, academicYearRepository);

        testProgram = createProgram(1L, "BSc Nursing", "BSCN", 4);
        testAcademicYear = createAcademicYear(1L, "2026-2027",
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31));
    }

    @Test
    void shouldCreateCurriculumVersion() {
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "BSCN-2026", 1L, true);

        CurriculumVersion cv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(curriculumVersionRepository.save(any(CurriculumVersion.class))).thenReturn(cv);

        CurriculumVersionDto dto = curriculumVersionService.createCurriculumVersion(request);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.versionName()).isEqualTo("BSCN-2026");
        assertThat(dto.programId()).isEqualTo(1L);
        assertThat(dto.isActive()).isTrue();

        verify(curriculumVersionRepository).save(any(CurriculumVersion.class));
    }

    @Test
    void shouldThrowWhenProgramNotFoundOnCreate() {
        CurriculumVersionRequest request = new CurriculumVersionRequest(999L, "BSCN-2026", 1L, true);

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculumVersionService.createCurriculumVersion(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldThrowWhenAcademicYearNotFoundOnCreate() {
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "BSCN-2026", 999L, true);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculumVersionService.createCurriculumVersion(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetCurriculumVersionsByProgram() {
        CurriculumVersion cv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);

        when(programRepository.existsById(1L)).thenReturn(true);
        when(curriculumVersionRepository.findByProgramId(1L)).thenReturn(List.of(cv));

        List<CurriculumVersionDto> dtos = curriculumVersionService.getCurriculumVersionsByProgram(1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).versionName()).isEqualTo("BSCN-2026");
    }

    @Test
    void shouldThrowWhenProgramNotFoundForGetByProgram() {
        when(programRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> curriculumVersionService.getCurriculumVersionsByProgram(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetCurriculumVersionById() {
        CurriculumVersion cv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(cv));

        CurriculumVersionDto dto = curriculumVersionService.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.versionName()).isEqualTo("BSCN-2026");
    }

    @Test
    void shouldThrowWhenCurriculumVersionNotFoundById() {
        when(curriculumVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculumVersionService.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldUpdateCurriculumVersion() {
        CurriculumVersion cv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "BSCN-2026-Updated", 1L, false);

        CurriculumVersion updated = createCurriculumVersion(1L, testProgram, "BSCN-2026-Updated", testAcademicYear, false);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(cv));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(curriculumVersionRepository.save(any(CurriculumVersion.class))).thenReturn(updated);

        CurriculumVersionDto dto = curriculumVersionService.update(1L, request);

        assertThat(dto.versionName()).isEqualTo("BSCN-2026-Updated");
        assertThat(dto.isActive()).isFalse();
    }

    @Test
    void shouldDeactivateCurriculumVersion() {
        CurriculumVersion cv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);
        CurriculumVersion deactivated = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, false);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(cv));
        when(curriculumVersionRepository.save(any(CurriculumVersion.class))).thenReturn(deactivated);

        CurriculumVersionDto dto = curriculumVersionService.deactivateCurriculumVersion(1L);

        assertThat(dto.isActive()).isFalse();
    }

    @Test
    void shouldThrowWhenDeactivatingNonExistentVersion() {
        when(curriculumVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculumVersionService.deactivateCurriculumVersion(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldCloneCurriculumVersion() {
        CurriculumVersion source = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);
        AcademicYear newAY = createAcademicYear(2L, "2027-2028",
            LocalDate.of(2027, 6, 1), LocalDate.of(2028, 5, 31));
        CurriculumVersion cloned = createCurriculumVersion(2L, testProgram, "BSCN-2027", newAY, true);

        when(curriculumVersionRepository.findById(1L)).thenReturn(Optional.of(source));
        when(academicYearRepository.findById(2L)).thenReturn(Optional.of(newAY));
        when(curriculumVersionRepository.save(any(CurriculumVersion.class))).thenReturn(cloned);

        CurriculumVersionDto dto = curriculumVersionService.cloneCurriculumVersion(1L, "BSCN-2027", 2L);

        assertThat(dto.versionName()).isEqualTo("BSCN-2027");
        assertThat(dto.isActive()).isTrue();

        verify(curriculumVersionRepository).save(any(CurriculumVersion.class));
    }

    @Test
    void shouldThrowWhenSourceVersionNotFoundForClone() {
        when(curriculumVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculumVersionService.cloneCurriculumVersion(999L, "BSCN-2027", 2L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldCreateVersionWithNullIsActive() {
        CurriculumVersionRequest request = new CurriculumVersionRequest(1L, "BSCN-2026", 1L, null);
        CurriculumVersion cv = createCurriculumVersion(1L, testProgram, "BSCN-2026", testAcademicYear, true);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(testAcademicYear));
        when(curriculumVersionRepository.save(any(CurriculumVersion.class))).thenReturn(cv);

        CurriculumVersionDto dto = curriculumVersionService.createCurriculumVersion(request);

        assertThat(dto.isActive()).isTrue();
    }

    private Program createProgram(Long id, String name, String code, Integer durationYears) {
        Program p = new Program(name, code, durationYears, ProgramStatus.ACTIVE);
        p.setId(id);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private AcademicYear createAcademicYear(Long id, String name,
                                             LocalDate startDate, LocalDate endDate) {
        AcademicYear ay = new AcademicYear(name, startDate, endDate, false);
        ay.setId(id);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());
        return ay;
    }

    private CurriculumVersion createCurriculumVersion(Long id, Program program, String versionName,
                                                       AcademicYear ay, Boolean isActive) {
        CurriculumVersion cv = new CurriculumVersion(program, versionName, ay, isActive);
        cv.setId(id);
        cv.setCreatedAt(Instant.now());
        cv.setUpdatedAt(Instant.now());
        return cv;
    }
}
