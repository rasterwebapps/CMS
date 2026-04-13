package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.FeeStructure;
import com.cms.model.Program;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;

    public FeeStructureService(FeeStructureRepository feeStructureRepository,
                                ProgramRepository programRepository,
                                AcademicYearRepository academicYearRepository) {
        this.feeStructureRepository = feeStructureRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + request.academicYearId()));

        Boolean isMandatory = request.isMandatory() != null ? request.isMandatory() : true;
        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        FeeStructure feeStructure = new FeeStructure(
            program, academicYear, request.feeType(), request.amount(), isMandatory, isActive
        );
        feeStructure.setDescription(request.description());

        FeeStructure saved = feeStructureRepository.save(feeStructure);
        return toResponse(saved);
    }

    public List<FeeStructureResponse> findAll() {
        return feeStructureRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public FeeStructureResponse findById(Long id) {
        FeeStructure feeStructure = feeStructureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + id));
        return toResponse(feeStructure);
    }

    public List<FeeStructureResponse> findByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return feeStructureRepository.findByProgramId(programId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FeeStructureResponse> findByProgramIdAndAcademicYearId(Long programId, Long academicYearId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return feeStructureRepository.findByProgramIdAndAcademicYearIdAndIsActiveTrue(programId, academicYearId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FeeStructureResponse update(Long id, FeeStructureRequest request) {
        FeeStructure feeStructure = feeStructureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + id));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + request.academicYearId()));

        feeStructure.setProgram(program);
        feeStructure.setAcademicYear(academicYear);
        feeStructure.setFeeType(request.feeType());
        feeStructure.setAmount(request.amount());
        feeStructure.setDescription(request.description());

        if (request.isMandatory() != null) {
            feeStructure.setIsMandatory(request.isMandatory());
        }
        if (request.isActive() != null) {
            feeStructure.setIsActive(request.isActive());
        }

        FeeStructure updated = feeStructureRepository.save(feeStructure);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!feeStructureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fee structure not found with id: " + id);
        }
        feeStructureRepository.deleteById(id);
    }

    private FeeStructureResponse toResponse(FeeStructure fs) {
        return new FeeStructureResponse(
            fs.getId(),
            fs.getProgram().getId(),
            fs.getProgram().getName(),
            fs.getAcademicYear().getId(),
            fs.getAcademicYear().getName(),
            fs.getFeeType(),
            fs.getAmount(),
            fs.getDescription(),
            fs.getIsMandatory(),
            fs.getIsActive(),
            fs.getCreatedAt(),
            fs.getUpdatedAt()
        );
    }
}
