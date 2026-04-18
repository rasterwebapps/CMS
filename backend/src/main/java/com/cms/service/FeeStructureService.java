package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
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
import com.cms.model.enums.FeeType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeePaymentRepository;
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
    private final FeePaymentRepository feePaymentRepository;

    public FeeStructureService(FeeStructureRepository feeStructureRepository,
                                ProgramRepository programRepository,
                                AcademicYearRepository academicYearRepository,
                                FeeStructureYearAmountRepository yearAmountRepository,
                                CourseRepository courseRepository,
                                FeePaymentRepository feePaymentRepository) {
        this.feeStructureRepository = feeStructureRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearAmountRepository = yearAmountRepository;
        this.courseRepository = courseRepository;
        this.feePaymentRepository = feePaymentRepository;
    }

    @Transactional
    public List<FeeStructureResponse> bulkCreate(BulkFeeStructureRequest request) {
        validateNoDuplicateFeeTypes(request.items());

        programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + request.academicYearId()));

        if (request.courseId() != null) {
            courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        // Enforce one fee structure group per (program, academic year, course)
        List<FeeStructure> existing;
        if (request.courseId() != null) {
            existing = feeStructureRepository.findByProgramIdAndCourseIdAndAcademicYearId(
                request.programId(), request.courseId(), request.academicYearId());
        } else {
            existing = feeStructureRepository.findByProgramIdAndAcademicYearIdAndCourseIsNull(
                request.programId(), request.academicYearId());
        }
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException(
                "A fee structure already exists for this program, course, and academic year combination. Use the edit function to update it.");
        }

        return bulkCreateInternal(request);
    }

    private void validateNoDuplicateFeeTypes(List<FeeStructureItemRequest> items) {
        Set<com.cms.model.enums.FeeType> seenTypes = new HashSet<>();
        for (FeeStructureItemRequest item : items) {
            if (!seenTypes.add(item.feeType())) {
                throw new IllegalArgumentException("Duplicate fee type in bulk request: " + item.feeType());
            }
        }
    }

    private List<FeeStructureResponse> bulkCreateInternal(BulkFeeStructureRequest request) {
        validateNoDuplicateFeeTypes(request.items());
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

            List<FeeStructureYearAmount> yearAmounts = saveItemYearAmounts(saved, item);

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

        boolean duplicateFeeType;
        if (request.courseId() != null) {
            duplicateFeeType = feeStructureRepository
                .existsByFeeTypeAndProgramIdAndAcademicYearIdAndCourseIdAndIdNot(
                    request.feeType(), request.programId(), request.academicYearId(), request.courseId(), id);
        } else {
            duplicateFeeType = feeStructureRepository
                .existsByFeeTypeAndProgramIdAndAcademicYearIdAndCourseIsNullAndIdNot(
                    request.feeType(), request.programId(), request.academicYearId(), id);
        }
        if (duplicateFeeType) {
            throw new IllegalArgumentException(
                "A fee structure with fee type '" + request.feeType()
                + "' already exists for this program and academic year combination");
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
        if (feePaymentRepository.existsByFeeStructureId(id)) {
            throw new IllegalStateException(
                "Cannot delete fee structure because payments have been recorded against it.");
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

    /**
     * Updates an existing fee structure group in place using an upsert strategy:
     * <ul>
     *   <li>Fee types present in both the existing group and the request are updated in place,
     *       preserving their database IDs so that any fee payment references remain valid.</li>
     *   <li>Fee types only in the request are inserted as new records.</li>
     *   <li>Fee types only in the existing group are deleted, but only if no payments have been
     *       recorded against them; otherwise an {@link IllegalStateException} is thrown.</li>
     * </ul>
     */
    @Transactional
    public List<FeeStructureResponse> bulkUpdate(BulkFeeStructureRequest request) {
        validateNoDuplicateFeeTypes(request.items());

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + request.academicYearId()));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
        }

        // Load existing fee structures in this group
        List<FeeStructure> existingGroup;
        if (request.courseId() != null) {
            existingGroup = feeStructureRepository.findByProgramIdAndCourseIdAndAcademicYearId(
                request.programId(), request.courseId(), request.academicYearId());
        } else {
            existingGroup = feeStructureRepository.findByProgramIdAndAcademicYearIdAndCourseIsNull(
                request.programId(), request.academicYearId());
        }

        // Build lookup: feeType → existing FeeStructure
        Map<FeeType, FeeStructure> existingByType = new EnumMap<>(FeeType.class);
        for (FeeStructure fs : existingGroup) {
            existingByType.put(fs.getFeeType(), fs);
        }

        // Determine which fee types are being removed
        Set<FeeType> incomingTypes = new HashSet<>();
        for (FeeStructureItemRequest item : request.items()) {
            incomingTypes.add(item.feeType());
        }

        // Fail fast: reject removal of any fee type that has recorded payments
        for (FeeStructure fs : existingGroup) {
            if (!incomingTypes.contains(fs.getFeeType())
                    && feePaymentRepository.existsByFeeStructureId(fs.getId())) {
                throw new IllegalStateException(
                    "Cannot remove fee type '" + fs.getFeeType()
                    + "' from the fee structure because payments have been recorded against it.");
            }
        }

        // Process each incoming item: update in place or create new
        List<FeeStructureResponse> responses = new ArrayList<>();
        for (FeeStructureItemRequest item : request.items()) {
            Boolean isMandatory = item.isMandatory() != null ? item.isMandatory() : true;
            Boolean isActive = item.isActive() != null ? item.isActive() : true;

            FeeStructure fs = existingByType.get(item.feeType());
            if (fs != null) {
                // Update existing record in place — ID is preserved, no FK violation
                fs.setAmount(item.amount());
                fs.setDescription(item.description());
                fs.setIsMandatory(isMandatory);
                fs.setIsActive(isActive);
                FeeStructure updated = feeStructureRepository.save(fs);
                yearAmountRepository.deleteByFeeStructureId(updated.getId());
                List<FeeStructureYearAmount> yearAmounts = saveItemYearAmounts(updated, item);
                responses.add(toResponse(updated, yearAmounts));
            } else {
                // New fee type — insert a fresh record
                FeeStructure newFs = new FeeStructure(
                    program, academicYear, item.feeType(), item.amount(), isMandatory, isActive);
                newFs.setDescription(item.description());
                newFs.setCourse(course);
                FeeStructure saved = feeStructureRepository.save(newFs);
                List<FeeStructureYearAmount> yearAmounts = saveItemYearAmounts(saved, item);
                responses.add(toResponse(saved, yearAmounts));
            }
        }

        // Delete fee structures for removed fee types (already confirmed no payments above)
        for (FeeStructure fs : existingGroup) {
            if (!incomingTypes.contains(fs.getFeeType())) {
                yearAmountRepository.deleteByFeeStructureId(fs.getId());
                feeStructureRepository.deleteById(fs.getId());
            }
        }

        return responses;
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
        // Fail fast: reject deletion if any fee structure in the group has recorded payments
        for (FeeStructure fs : toDelete) {
            if (feePaymentRepository.existsByFeeStructureId(fs.getId())) {
                throw new IllegalStateException(
                    "Cannot delete fee structure group because payments have been recorded against one or more fee types.");
            }
        }
        for (FeeStructure fs : toDelete) {
            yearAmountRepository.deleteByFeeStructureId(fs.getId());
            feeStructureRepository.deleteById(fs.getId());
        }
    }

    private List<FeeStructureYearAmount> saveYearAmounts(FeeStructure feeStructure, FeeStructureRequest request) {
        return buildYearAmounts(feeStructure, request.yearAmounts());
    }

    private List<FeeStructureYearAmount> saveItemYearAmounts(FeeStructure feeStructure, FeeStructureItemRequest item) {
        return buildYearAmounts(feeStructure, item.yearAmounts());
    }

    private List<FeeStructureYearAmount> buildYearAmounts(FeeStructure feeStructure,
            List<com.cms.dto.YearAmountRequest> yearAmountRequests) {
        List<FeeStructureYearAmount> yearAmounts = new ArrayList<>();
        if (yearAmountRequests != null && !yearAmountRequests.isEmpty()) {
            for (var ya : yearAmountRequests) {
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
