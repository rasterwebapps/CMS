package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.cms.model.FeeState;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureGroup;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.Program;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FeePaymentRepository;
import com.cms.repository.FeeStateRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class FeeStructureService {

    private static final Set<FeeType> COURSE_FEE_TYPES = Set.of(
        FeeType.TUITION,
        FeeType.LABORATORY_FEE,
        FeeType.CLINICAL_FEE,
        FeeType.LIBRARY_FEE,
        FeeType.EXAMINATION_FEE,
        FeeType.BOOK_AND_PACKET_FEE,
        FeeType.UNIFORM_AND_SHOES_FEE,
        FeeType.UNIVERSITY_REGISTRATION_FEE,
        FeeType.MISCELLANEOUS,
        FeeType.LATE_FEE
    );

    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureGroupRepository groupRepository;
    private final FeeStateRepository feeStateRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureYearAmountRepository yearAmountRepository;
    private final CourseRepository courseRepository;
    private final FeePaymentRepository feePaymentRepository;

    public FeeStructureService(FeeStructureRepository feeStructureRepository,
                                FeeStructureGroupRepository groupRepository,
                                FeeStateRepository feeStateRepository,
                                ProgramRepository programRepository,
                                AcademicYearRepository academicYearRepository,
                                FeeStructureYearAmountRepository yearAmountRepository,
                                CourseRepository courseRepository,
                                FeePaymentRepository feePaymentRepository) {
        this.feeStructureRepository = feeStructureRepository;
        this.groupRepository = groupRepository;
        this.feeStateRepository = feeStateRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearAmountRepository = yearAmountRepository;
        this.courseRepository = courseRepository;
        this.feePaymentRepository = feePaymentRepository;
    }

    // ── Bulk create ──────────────────────────────────────────────────────────

    @Transactional
    public List<FeeStructureResponse> bulkCreate(BulkFeeStructureRequest request) {
        validateNoDuplicateFeeTypes(request.items());
        validateCourseTotalGreaterThanZero(request.items());

        Program program = resolveProgram(request.programId());
        AcademicYear academicYear = resolveAcademicYear(request.academicYearId());
        Course course = resolveCourse(request.courseId());
        FeeState feeState = resolveFeeState(request.feeStateId());

        groupRepository.findExact(
            request.programId(), request.academicYearId(), request.courseId(),
            request.quota(), request.feeStateId(), request.gender()
        ).ifPresent(g -> { throw new IllegalArgumentException(
            "A fee structure already exists for this combination. Use the edit function to update it."); });

        FeeStructureGroup group = groupRepository.save(
            new FeeStructureGroup(program, academicYear, course,
                request.quota(), feeState, request.gender()));

        return saveItems(group, request.items());
    }

    // ── Bulk update (upsert items, delete removed fee types) ─────────────────

    @Transactional
    public List<FeeStructureResponse> bulkUpdate(BulkFeeStructureRequest request) {
        validateNoDuplicateFeeTypes(request.items());
        validateCourseTotalGreaterThanZero(request.items());

        resolveFeeState(request.feeStateId());

        FeeStructureGroup group = groupRepository.findExact(
            request.programId(), request.academicYearId(), request.courseId(),
            request.quota(), request.feeStateId(), request.gender()
        ).orElseThrow(() -> new ResourceNotFoundException(
            "No fee structure group found for the given combination"));

        List<FeeStructure> existingItems = feeStructureRepository.findByFeeStructureGroupId(group.getId());

        Map<FeeType, FeeStructure> existingByType = new EnumMap<>(FeeType.class);
        for (FeeStructure fs : existingItems) {
            existingByType.put(fs.getFeeType(), fs);
        }

        Set<FeeType> incomingTypes = new HashSet<>();
        for (FeeStructureItemRequest item : request.items()) {
            incomingTypes.add(item.feeType());
        }

        // Fail fast: cannot remove a fee type that has recorded payments
        for (FeeStructure fs : existingItems) {
            if (!incomingTypes.contains(fs.getFeeType())
                    && feePaymentRepository.existsByFeeStructureId(fs.getId())) {
                throw new IllegalStateException(
                    "Cannot remove fee type '" + fs.getFeeType()
                    + "' because payments have been recorded against it.");
            }
        }

        List<FeeStructureResponse> responses = new ArrayList<>();
        for (FeeStructureItemRequest item : request.items()) {
            BigDecimal amount = resolveItemAmount(item);
            Boolean isMandatory = item.isMandatory() != null ? item.isMandatory() : true;
            Boolean isActive = item.isActive() != null ? item.isActive() : true;

            FeeStructure fs = existingByType.get(item.feeType());
            if (fs != null) {
                fs.setAmount(amount);
                fs.setDescription(item.description());
                fs.setIsMandatory(isMandatory);
                fs.setIsActive(isActive);
                FeeStructure updated = feeStructureRepository.save(fs);
                yearAmountRepository.deleteByFeeStructureId(updated.getId());
                List<FeeStructureYearAmount> yearAmounts = saveItemYearAmounts(updated, item);
                responses.add(toResponse(updated, yearAmounts));
            } else {
                FeeStructure newFs = new FeeStructure(group, item.feeType(), amount, isMandatory, isActive);
                newFs.setDescription(item.description());
                FeeStructure saved = feeStructureRepository.save(newFs);
                List<FeeStructureYearAmount> yearAmounts = saveItemYearAmounts(saved, item);
                responses.add(toResponse(saved, yearAmounts));
            }
        }

        for (FeeStructure fs : existingItems) {
            if (!incomingTypes.contains(fs.getFeeType())) {
                yearAmountRepository.deleteByFeeStructureId(fs.getId());
                feeStructureRepository.deleteById(fs.getId());
            }
        }

        return responses;
    }

    // ── Single-item CRUD (for completeness; bulk endpoints are the main path) ─

    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        BigDecimal amount = resolveRequestAmount(request);
        validateAmountGreaterThanZero(amount);

        Program program = resolveProgram(request.programId());
        AcademicYear academicYear = resolveAcademicYear(request.academicYearId());
        Course course = resolveCourse(request.courseId());
        FeeState feeState = resolveFeeState(request.feeStateId());

        FeeStructureGroup group = groupRepository.findExact(
            request.programId(), request.academicYearId(), request.courseId(),
            request.quota(), request.feeStateId(), request.gender()
        ).orElseGet(() -> groupRepository.save(
            new FeeStructureGroup(program, academicYear, course,
                request.quota(), feeState, request.gender())));

        Boolean isMandatory = request.isMandatory() != null ? request.isMandatory() : true;
        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        FeeStructure feeStructure = new FeeStructure(group, request.feeType(), amount, isMandatory, isActive);
        feeStructure.setDescription(request.description());
        FeeStructure saved = feeStructureRepository.save(feeStructure);

        List<FeeStructureYearAmount> yearAmounts = saveYearAmounts(saved, request);
        return toResponse(saved, yearAmounts);
    }

    public FeeStructureResponse findById(Long id) {
        FeeStructure feeStructure = feeStructureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + id));
        List<FeeStructureYearAmount> yearAmounts = yearAmountRepository.findByFeeStructureIdOrderByYearNumber(id);
        return toResponse(feeStructure, yearAmounts);
    }

    public List<FeeStructureResponse> findAll() {
        return feeStructureRepository.findAll().stream()
            .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
            .toList();
    }

    public List<FeeStructureResponse> findByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return groupRepository.findByProgramId(programId).stream()
            .flatMap(g -> feeStructureRepository.findByFeeStructureGroupId(g.getId()).stream()
                .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId()))))
            .toList();
    }

    public List<FeeStructureResponse> findByProgramIdAndAcademicYearId(Long programId, Long academicYearId) {
        return groupRepository.findByProgramIdAndAcademicYearId(programId, academicYearId).stream()
            .flatMap(g -> feeStructureRepository.findByFeeStructureGroupIdAndIsActiveTrue(g.getId()).stream()
                .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId()))))
            .toList();
    }

    public List<FeeStructureResponse> findByProgramIdAndCourseId(Long programId, Long courseId) {
        AcademicYear currentAcademicYear = academicYearRepository.findByIsCurrentTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No current academic year found"));
        return findByProgramIdAndCourseIdAndAcademicYearId(programId, courseId, currentAcademicYear.getId());
    }

    public List<FeeStructureResponse> findByProgramIdAndCourseIdAndAcademicYearId(
            Long programId, Long courseId, Long academicYearId) {
        return groupRepository.findByProgramIdAndAcademicYearId(programId, academicYearId).stream()
            .filter(g -> courseId == null ? g.getCourse() == null : (g.getCourse() != null && g.getCourse().getId().equals(courseId)))
            .flatMap(g -> feeStructureRepository.findByFeeStructureGroupIdAndIsActiveTrue(g.getId()).stream()
                .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId()))))
            .toList();
    }

    @Transactional
    public FeeStructureResponse update(Long id, FeeStructureRequest request) {
        BigDecimal amount = resolveRequestAmount(request);
        validateAmountGreaterThanZero(amount);

        FeeStructure feeStructure = feeStructureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found with id: " + id));

        if (feeStructureRepository.existsByFeeTypeAndFeeStructureGroupIdAndIdNot(
                request.feeType(), feeStructure.getFeeStructureGroup().getId(), id)) {
            throw new IllegalArgumentException(
                "A fee structure with fee type '" + request.feeType() + "' already exists in this group");
        }

        feeStructure.setFeeType(request.feeType());
        feeStructure.setAmount(amount);
        feeStructure.setDescription(request.description());
        if (request.isMandatory() != null) feeStructure.setIsMandatory(request.isMandatory());
        if (request.isActive() != null) feeStructure.setIsActive(request.isActive());

        FeeStructure updated = feeStructureRepository.save(feeStructure);
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

    // ── Grouped list view ────────────────────────────────────────────────────

    public List<GroupedFeeStructureResponse> findGrouped(
            Long programId, Long academicYearId, Long courseId,
            AdmissionQuota quota, Long feeStateId, Gender gender) {

        List<FeeStructureGroup> groups;
        if (programId != null && academicYearId != null) {
            groups = groupRepository.findByProgramIdAndAcademicYearId(programId, academicYearId);
        } else if (programId != null) {
            groups = groupRepository.findByProgramId(programId);
        } else if (academicYearId != null) {
            groups = groupRepository.findByAcademicYearId(academicYearId);
        } else {
            groups = groupRepository.findAll();
        }

        // Apply remaining filters in memory (avoids a complex JPQL for optional multi-param filtering)
        if (courseId != null) {
            final Long cId = courseId;
            groups = groups.stream()
                .filter(g -> g.getCourse() != null && g.getCourse().getId().equals(cId))
                .toList();
        }
        if (quota != null) {
            groups = groups.stream().filter(g -> g.getQuota() == quota).toList();
        }
        if (feeStateId != null) {
            groups = groups.stream().filter(g -> g.getFeeState().getId().equals(feeStateId)).toList();
        }
        if (gender != null) {
            groups = groups.stream().filter(g -> g.getGender() == gender).toList();
        }

        List<GroupedFeeStructureResponse> result = new ArrayList<>();
        for (FeeStructureGroup group : groups) {
            List<FeeStructure> items = feeStructureRepository.findByFeeStructureGroupId(group.getId());
            List<FeeStructureResponse> itemResponses = items.stream()
                .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
                .toList();
            // Derive total from itemResponses so yearAmounts take precedence over the flat amount field,
            // and inactive fee types are excluded (matching frontend itemAmount() logic).
            BigDecimal total = itemResponses.stream()
                .filter(r -> Boolean.TRUE.equals(r.isActive()))
                .map(r -> r.yearAmounts() != null && !r.yearAmounts().isEmpty()
                    ? r.yearAmounts().stream().map(YearAmountResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : r.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(toGroupedResponse(group, total, itemResponses));
        }
        return result;
    }

    @Transactional
    public void deleteGroup(Long programId, Long academicYearId, Long courseId,
                             AdmissionQuota quota, Long feeStateId, Gender gender) {
        FeeStructureGroup group = groupRepository.findExact(
            programId, academicYearId, courseId, quota, feeStateId, gender
        ).orElseThrow(() -> new ResourceNotFoundException(
            "No fee structure group found for the given combination"));

        List<FeeStructure> items = feeStructureRepository.findByFeeStructureGroupId(group.getId());
        for (FeeStructure fs : items) {
            if (feePaymentRepository.existsByFeeStructureId(fs.getId())) {
                throw new IllegalStateException(
                    "Cannot delete group because payments exist against fee type '" + fs.getFeeType() + "'.");
            }
        }
        for (FeeStructure fs : items) {
            yearAmountRepository.deleteByFeeStructureId(fs.getId());
        }
        feeStructureRepository.deleteByFeeStructureGroupId(group.getId());
        groupRepository.deleteById(group.getId());
    }

    // ── Fee lookup for enquiry (authoritative with fallback) ─────────────────

    /**
     * Looks up the fee structure items for the given combination.
     * When {@code academicYearId} is null, falls back to the current academic year.
     * Falls back to the fallback fee state if no exact match exists.
     * Returns empty if neither match is found.
     */
    public Optional<List<FeeStructureResponse>> findForEnquiry(
            Long programId, Long courseId,
            AdmissionQuota quota, Long feeStateId, Gender gender,
            Long academicYearId) {

        AcademicYear year;
        if (academicYearId != null) {
            Optional<AcademicYear> found = academicYearRepository.findById(academicYearId);
            if (found.isEmpty()) return Optional.empty();
            year = found.get();
        } else {
            year = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year found"));
        }

        // Try exact match
        Optional<FeeStructureGroup> group = groupRepository.findExact(
            programId, year.getId(), courseId, quota, feeStateId, gender);

        // Fall back to the fallback fee state if no exact match
        if (group.isEmpty()) {
            Optional<FeeState> fallbackState = feeStateRepository.findByIsFallbackTrue();
            if (fallbackState.isPresent() && !fallbackState.get().getId().equals(feeStateId)) {
                group = groupRepository.findExact(
                    programId, year.getId(), courseId, quota,
                    fallbackState.get().getId(), gender);
            }
        }

        if (group.isEmpty()) {
            return Optional.empty();
        }

        List<FeeStructureResponse> items = feeStructureRepository
            .findByFeeStructureGroupIdAndIsActiveTrue(group.get().getId())
            .stream()
            .map(fs -> toResponse(fs, yearAmountRepository.findByFeeStructureIdOrderByYearNumber(fs.getId())))
            .toList();

        return Optional.of(items);
    }

    public Optional<List<FeeStructureResponse>> findForEnquiry(
            Long programId, Long courseId,
            AdmissionQuota quota, Long feeStateId, Gender gender) {
        return findForEnquiry(programId, courseId, quota, feeStateId, gender, (Long) null);
    }

    public Optional<List<FeeStructureResponse>> findForEnquiry(
            Long programId, Long courseId,
            AdmissionQuota quota, Long feeStateId, Gender gender,
            StudentType studentType) {
        return findForEnquiry(programId, courseId, quota, feeStateId, gender, (Long) null)
            .map(items -> items.stream()
                .filter(item -> studentType != StudentType.DAY_SCHOLAR || item.feeType() != FeeType.HOSTEL_FEE)
                .toList());
    }

    public Optional<List<FeeStructureResponse>> findForEnquiry(
            Long programId, Long courseId,
            AdmissionQuota quota, Long feeStateId, Gender gender,
            StudentType studentType, Long academicYearId) {
        return findForEnquiry(programId, courseId, quota, feeStateId, gender, academicYearId)
            .map(items -> items.stream()
                .filter(item -> studentType != StudentType.DAY_SCHOLAR || item.feeType() != FeeType.HOSTEL_FEE)
                .toList());
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private List<FeeStructureResponse> saveItems(FeeStructureGroup group,
                                                   List<FeeStructureItemRequest> items) {
        List<FeeStructureResponse> responses = new ArrayList<>();
        for (FeeStructureItemRequest item : items) {
            Boolean isMandatory = item.isMandatory() != null ? item.isMandatory() : true;
            Boolean isActive = item.isActive() != null ? item.isActive() : true;
            BigDecimal amount = resolveItemAmount(item);

            FeeStructure fs = new FeeStructure(group, item.feeType(), amount, isMandatory, isActive);
            fs.setDescription(item.description());
            FeeStructure saved = feeStructureRepository.save(fs);

            List<FeeStructureYearAmount> yearAmounts = saveItemYearAmounts(saved, item);
            responses.add(toResponse(saved, yearAmounts));
        }
        return responses;
    }

    private void validateNoDuplicateFeeTypes(List<FeeStructureItemRequest> items) {
        Set<FeeType> seen = new HashSet<>();
        for (FeeStructureItemRequest item : items) {
            if (!seen.add(item.feeType())) {
                throw new IllegalArgumentException("Duplicate fee type in request: " + item.feeType());
            }
        }
    }

    private void validateCourseTotalGreaterThanZero(List<FeeStructureItemRequest> items) {
        BigDecimal total = items.stream()
            .filter(item -> COURSE_FEE_TYPES.contains(item.feeType()))
            .map(this::resolveItemAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total course fee must be greater than zero");
        }
    }

    private void validateAmountGreaterThanZero(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fee amount must be greater than zero");
        }
    }

    private BigDecimal resolveItemAmount(FeeStructureItemRequest item) {
        if (item.yearAmounts() != null && !item.yearAmounts().isEmpty()) {
            return item.yearAmounts().stream()
                .map(ya -> normalizeAmount(ya.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return normalizeAmount(item.amount());
    }

    private BigDecimal resolveRequestAmount(FeeStructureRequest request) {
        if (request.yearAmounts() != null && !request.yearAmounts().isEmpty()) {
            return request.yearAmounts().stream()
                .map(ya -> normalizeAmount(ya.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return normalizeAmount(request.amount());
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal normalized = amount != null ? amount : BigDecimal.ZERO;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be zero or positive");
        }
        return normalized;
    }

    private List<FeeStructureYearAmount> saveYearAmounts(FeeStructure fs, FeeStructureRequest request) {
        return buildYearAmounts(fs, request.yearAmounts());
    }

    private List<FeeStructureYearAmount> saveItemYearAmounts(FeeStructure fs, FeeStructureItemRequest item) {
        return buildYearAmounts(fs, item.yearAmounts());
    }

    private List<FeeStructureYearAmount> buildYearAmounts(FeeStructure fs,
            List<com.cms.dto.YearAmountRequest> yearAmountRequests) {
        List<FeeStructureYearAmount> yearAmounts = new ArrayList<>();
        if (yearAmountRequests != null && !yearAmountRequests.isEmpty()) {
            for (var ya : yearAmountRequests) {
                yearAmounts.add(yearAmountRepository.save(
                    new FeeStructureYearAmount(fs, ya.yearNumber(), ya.yearLabel(), normalizeAmount(ya.amount()))));
            }
        }
        return yearAmounts;
    }

    private Program resolveProgram(Long programId) {
        return programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + programId));
    }

    private AcademicYear resolveAcademicYear(Long academicYearId) {
        return academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + academicYearId));
    }

    private Course resolveCourse(Long courseId) {
        if (courseId == null) return null;
        return courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    private FeeState resolveFeeState(Long feeStateId) {
        return feeStateRepository.findById(feeStateId)
            .orElseThrow(() -> new ResourceNotFoundException("Fee state not found with id: " + feeStateId));
    }

    private FeeStructureResponse toResponse(FeeStructure fs, List<FeeStructureYearAmount> yearAmounts) {
        List<YearAmountResponse> yaResponses = yearAmounts.stream()
            .map(ya -> new YearAmountResponse(ya.getId(), ya.getYearNumber(), ya.getYearLabel(), ya.getAmount()))
            .toList();

        FeeStructureGroup group = fs.getFeeStructureGroup();
        return new FeeStructureResponse(
            fs.getId(),
            group.getId(),
            group.getProgram().getId(),
            group.getProgram().getName(),
            group.getCourse() != null ? group.getCourse().getId() : null,
            group.getCourse() != null ? group.getCourse().getName() : null,
            group.getAcademicYear().getId(),
            group.getAcademicYear().getName(),
            group.getQuota(),
            group.getFeeState().getId(),
            group.getFeeState().getName(),
            group.getGender(),
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

    private GroupedFeeStructureResponse toGroupedResponse(FeeStructureGroup group,
            BigDecimal total, List<FeeStructureResponse> items) {
        return new GroupedFeeStructureResponse(
            group.getId(),
            group.getProgram().getId(),
            group.getProgram().getName(),
            group.getCourse() != null ? group.getCourse().getId() : null,
            group.getCourse() != null ? group.getCourse().getName() : null,
            group.getAcademicYear().getId(),
            group.getAcademicYear().getName(),
            group.getQuota(),
            group.getFeeState().getId(),
            group.getFeeState().getName(),
            group.getGender(),
            total,
            items
        );
    }
}
