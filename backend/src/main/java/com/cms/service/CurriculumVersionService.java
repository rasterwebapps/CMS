package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CurriculumVersionDto;
import com.cms.dto.CurriculumVersionRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class CurriculumVersionService {

    private final CurriculumVersionRepository curriculumVersionRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;

    public CurriculumVersionService(CurriculumVersionRepository curriculumVersionRepository,
                                     ProgramRepository programRepository,
                                     AcademicYearRepository academicYearRepository) {
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public CurriculumVersionDto createCurriculumVersion(CurriculumVersionRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.effectiveFromAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.effectiveFromAcademicYearId()));

        Boolean isActive = request.isActive() != null ? request.isActive() : true;
        CurriculumVersion cv = new CurriculumVersion(program, request.versionName(), academicYear, isActive);
        return toDto(curriculumVersionRepository.save(cv));
    }

    public List<CurriculumVersionDto> getCurriculumVersionsByProgram(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return curriculumVersionRepository.findByProgramId(programId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public CurriculumVersionDto getById(Long id) {
        CurriculumVersion cv = curriculumVersionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + id));
        return toDto(cv);
    }

    @Transactional
    public CurriculumVersionDto update(Long id, CurriculumVersionRequest request) {
        CurriculumVersion cv = curriculumVersionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + id));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.effectiveFromAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.effectiveFromAcademicYearId()));

        cv.setProgram(program);
        cv.setVersionName(request.versionName());
        cv.setEffectiveFromAcademicYear(academicYear);
        if (request.isActive() != null) {
            cv.setIsActive(request.isActive());
        }

        return toDto(curriculumVersionRepository.save(cv));
    }

    @Transactional
    public CurriculumVersionDto deactivateCurriculumVersion(Long id) {
        CurriculumVersion cv = curriculumVersionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + id));
        // TODO: check if cohorts are attached (Phase 2)
        cv.setIsActive(false);
        return toDto(curriculumVersionRepository.save(cv));
    }

    @Transactional
    public CurriculumVersionDto cloneCurriculumVersion(Long sourceId, String newVersionName,
                                                        Long newEffectiveAcademicYearId) {
        CurriculumVersion source = curriculumVersionRepository.findById(sourceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + sourceId));

        AcademicYear newAcademicYear = academicYearRepository.findById(newEffectiveAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + newEffectiveAcademicYearId));

        CurriculumVersion clone = new CurriculumVersion(
            source.getProgram(), newVersionName, newAcademicYear, true);
        return toDto(curriculumVersionRepository.save(clone));
    }

    private CurriculumVersionDto toDto(CurriculumVersion cv) {
        return new CurriculumVersionDto(
            cv.getId(),
            cv.getProgram().getId(),
            cv.getProgram().getName(),
            cv.getVersionName(),
            cv.getEffectiveFromAcademicYear().getId(),
            cv.getEffectiveFromAcademicYear().getName(),
            cv.getIsActive(),
            cv.getCreatedAt(),
            cv.getUpdatedAt()
        );
    }
}
