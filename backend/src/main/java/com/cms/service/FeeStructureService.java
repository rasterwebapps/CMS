package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.BulkFeeStructureRequest;
import com.cms.dto.FeeStructureItemRequest;
import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.dto.GroupedFeeStructureResponse;
import com.cms.dto.YearAmountResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.Program;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureYearAmountRepository yearAmountRepository;
    private final CourseRepository courseRepository;

    public FeeStructureService(FeeStructureRepository feeStructureRepository,
                                ProgramRepository programRepository,
                                AcademicYearRepository academicYearRepository,
                                FeeStructureYearAmountRepository yearAmountRepository,
                                CourseRepository courseRepository) {
        this.feeStructureRepository = feeStructureRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearAmountRepository = yearAmountRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public List<FeeStructureResponse> bulkCreate(BulkFeeStructureRequest request) {
        Set<com.cms.model.enums.FeeType> seenTypes = new HashSet<>();
        for (FeeStructureItemRequest item : request.items()) {
            if (!seenTypes.add(item.feeType())) {
                throw new IllegalArgumentException("Duplicate fee type in bulk request: " + item.feeType());
            }
        }

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + request.academicYearId()));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        List<FeeStructureResponse> responses = new ArrayList<>();
        for (FeeStructureItemRequest item : request.items()) {
            Boolean isMandatory = item.isMandatory() != null ? item.isMandatory() : true;
            Boolean isActive = item.isActive() != null ? item.isActive() : true;

            FeeStructure feeStructure = new FeeStructure(
                program, academicYear, item.feeType(), item.amount(), isMandatory, isActive
            );
            feeStructure.setDescription(item.description());
            feeStructure.setCourse(course);

            FeeStructure saved = feeStructureRepository.save(feeStructure);

            List<FeeStructureYearAmount> yearAmounts = new ArrayList<>();
            if (item.yearAmounts() != null && !item.yearAmounts().isEmpty()) {
                for (var ya : item.yearAmounts()) {
                    FeeStructureYearAmount yearAmount = new FeeStructureYearAmount(
                        saved, ya.yearNumber(), ya.yearLabel(), ya.amount()
                    );
                    yearAmounts.add(yearAmountRepository.save(yearAmount));
                }
            }

            responses.add(toResponse(saved, yearAmounts));
        }
        return responses;
    }

    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + request.academicYearId()));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        Boolean isMandatory = request.isMandatory() != null ? request.isMandatory() : true;
        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        FeeStructure feeStructure = new FeeStructure(
            program, academicYear, request.feeType(), request.amount(), isMandatory, isActive
        );
        feeStructure.setDescription(request.description());
        feeStructure.setCourse(course);

        FeeStructure saved = feeStructureRepository.save(feeStructure);

        // Save year-wise amounts if provided
        List<FeeStructureYearAmount> yearAmounts = saveYearAmounts(saved, request);

        return toResponse(saved, yearAmounts);
    }

    public List<FeeStructureResponse> findAll() {
        return feeStructureRepository.findAll().stream()
            .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
            .toList();
    }

    public FeeStructureResponse findById(Long id) {
        FeeStructure feeStructure = feeStructureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + id));
        List<FeeStructureYearAmount> yearAmounts = yearAmountRepository.findByFeeStructureIdOrderByYearNumber(id);
        return toResponse(feeStructure, yearAmounts);
    }

    public List<FeeStructureResponse> findByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return feeStructureRepository.findByProgramId(programId).stream()
            .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
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
            .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
            .toList();
    }

    public List<FeeStructureResponse> findByProgramIdAndCourseId(Long programId, Long courseId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return feeStructureRepository.findByProgramIdAndCourseId(programId, courseId).stream()
            .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
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

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        feeStructure.setProgram(program);
        feeStructure.setAcademicYear(academicYear);
        feeStructure.setCourse(course);
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

        // Replace year amounts
        yearAmountRepository.deleteByFeeStructureId(id);
        List<FeeStructureYearAmount> yearAmounts = saveYearAmounts(updated, request);

        return toResponse(updated, yearAmounts);
    }

    @Transactional
    public void delete(Long id) {
        if (!feeStructureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fee structure not found with id: " + id);
        }
        yearAmountRepository.deleteByFeeStructureId(id);
        feeStructureRepository.deleteById(id);
    }

    public List<GroupedFeeStructureResponse> findGrouped(Long programId, Long academicYearId, Long courseId) {
        List<FeeStructure> allStructures;
        if (programId != null && academicYearId != null && courseId != null) {
            allStructures = feeStructureRepository.findByProgramIdAndCourseIdAndAcademicYearId(programId, courseId, academicYearId);
        } else if (programId != null && academicYearId != null) {
            allStructures = feeStructureRepository.findByProgramIdAndAcademicYearId(programId, academicYearId);
        } else if (programId != null && courseId != null) {
            allStructures = feeStructureRepository.findByProgramIdAndCourseId(programId, courseId);
        } else if (programId != null) {
            allStructures = feeStructureRepository.findByProgramId(programId);
        } else if (academicYearId != null) {
            allStructures = feeStructureRepository.findByAcademicYearId(academicYearId);
        } else {
            allStructures = feeStructureRepository.findAll();
        }

        Map<String, List<FeeStructure>> grouped = new LinkedHashMap<>();
        for (FeeStructure fs : allStructures) {
            String key = fs.getProgram().getId() + "_" + fs.getAcademicYear().getId() + "_"
                + (fs.getCourse() != null ? fs.getCourse().getId() : "null");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(fs);
        }

        List<GroupedFeeStructureResponse> result = new ArrayList<>();
        for (List<FeeStructure> group : grouped.values()) {
            FeeStructure first = group.get(0);
            BigDecimal total = group.stream()
                .map(FeeStructure::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<FeeStructureResponse> items = group.stream()
                .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
                .toList();
            result.add(new GroupedFeeStructureResponse(
                first.getProgram().getId(),
                first.getProgram().getName(),
                first.getCourse() != null ? first.getCourse().getId() : null,
                first.getCourse() != null ? first.getCourse().getName() : null,
                first.getAcademicYear().getId(),
                first.getAcademicYear().getName(),
                total,
                items
            ));
        }
        return result;
    }

    @Transactional
    public List<FeeStructureResponse> bulkUpdate(BulkFeeStructureRequest request) {
        deleteGroupInternal(request.programId(), request.academicYearId(), request.courseId());
        return bulkCreate(request);
    }

    @Transactional
    public void deleteGroup(Long programId, Long academicYearId, Long courseId) {
        deleteGroupInternal(programId, academicYearId, courseId);
    }

    private void deleteGroupInternal(Long programId, Long academicYearId, Long courseId) {
        List<FeeStructure> toDelete;
        if (courseId != null) {
            toDelete = feeStructureRepository.findByProgramIdAndCourseIdAndAcademicYearId(programId, courseId, academicYearId);
        } else {
            toDelete = feeStructureRepository.findByProgramIdAndAcademicYearIdAndCourseIsNull(programId, academicYearId);
        }
        for (FeeStructure fs : toDelete) {
            yearAmountRepository.deleteByFeeStructureId(fs.getId());
            feeStructureRepository.deleteById(fs.getId());
        }
    }

    private List<FeeStructureYearAmount> saveYearAmounts(FeeStructure feeStructure, FeeStructureRequest request) {
        List<FeeStructureYearAmount> yearAmounts = new ArrayList<>();
        if (request.yearAmounts() != null && !request.yearAmounts().isEmpty()) {
            for (var ya : request.yearAmounts()) {
                FeeStructureYearAmount yearAmount = new FeeStructureYearAmount(
                    feeStructure, ya.yearNumber(), ya.yearLabel(), ya.amount()
                );
                yearAmounts.add(yearAmountRepository.save(yearAmount));
            }
        }
        return yearAmounts;
    }

    private FeeStructureResponse toResponse(FeeStructure fs, List<FeeStructureYearAmount> yearAmounts) {
        List<YearAmountResponse> yaResponses = yearAmounts.stream()
            .map(ya -> new YearAmountResponse(ya.getId(), ya.getYearNumber(), ya.getYearLabel(), ya.getAmount()))
            .toList();

        return new FeeStructureResponse(
            fs.getId(),
            fs.getProgram().getId(),
            fs.getProgram().getName(),
            fs.getCourse() != null ? fs.getCourse().getId() : null,
            fs.getCourse() != null ? fs.getCourse().getName() : null,
            fs.getAcademicYear().getId(),
            fs.getAcademicYear().getName(),
            fs.getFeeType(),
            fs.getAmount(),
            fs.getDescription(),
            fs.getIsMandatory(),
            fs.getIsActive(),
            yaResponses,
            fs.getCreatedAt(),
            fs.getUpdatedAt()
        );
    }
}
