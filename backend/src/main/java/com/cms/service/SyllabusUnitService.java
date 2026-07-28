package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SyllabusUnitDto;
import com.cms.dto.SyllabusUnitRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.SyllabusUnit;
import com.cms.model.enums.AttendanceType;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.SyllabusUnitRepository;

@Service
@Transactional(readOnly = true)
public class SyllabusUnitService {

    private final SyllabusUnitRepository syllabusUnitRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;

    public SyllabusUnitService(SyllabusUnitRepository syllabusUnitRepository,
                                CurriculumSemesterCourseRepository curriculumSemesterCourseRepository) {
        this.syllabusUnitRepository = syllabusUnitRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
    }

    @Transactional
    public SyllabusUnitDto create(SyllabusUnitRequest request) {
        CurriculumSemesterCourse course = curriculumSemesterCourseRepository.findById(request.curriculumTermCourseId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum term course not found with id: " + request.curriculumTermCourseId()));

        if (syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumber(
                request.curriculumTermCourseId(), request.unitNumber())) {
            throw new IllegalArgumentException(
                "Unit number " + request.unitNumber() + " already exists for this subject");
        }
        validateHourBudget(course, request.componentType(), request.plannedHours(), null);

        SyllabusUnit unit = new SyllabusUnit(course, request.unitNumber(), request.title().trim(),
            request.componentType(), request.plannedHours(), request.description(), request.sortOrder());
        return toDto(syllabusUnitRepository.save(unit));
    }

    public List<SyllabusUnitDto> getUnitsForCourse(Long curriculumTermCourseId) {
        return syllabusUnitRepository
            .findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(curriculumTermCourseId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public SyllabusUnitDto update(Long id, SyllabusUnitRequest request) {
        SyllabusUnit unit = findOrThrow(id);
        CurriculumSemesterCourse course = unit.getCurriculumSemesterCourse();

        if (syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumberAndIdNot(
                request.curriculumTermCourseId(), request.unitNumber(), id)) {
            throw new IllegalArgumentException(
                "Unit number " + request.unitNumber() + " already exists for this subject");
        }
        validateHourBudget(course, request.componentType(), request.plannedHours(), id);

        unit.setUnitNumber(request.unitNumber());
        unit.setTitle(request.title().trim());
        unit.setComponentType(request.componentType());
        unit.setPlannedHours(request.plannedHours());
        unit.setDescription(request.description());
        unit.setSortOrder(request.sortOrder());
        return toDto(syllabusUnitRepository.save(unit));
    }

    @Transactional
    public void delete(Long id) {
        if (!syllabusUnitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Syllabus unit not found with id: " + id);
        }
        syllabusUnitRepository.deleteById(id);
    }

    public boolean unitNumberExists(Long curriculumTermCourseId, Integer unitNumber, Long excludeId) {
        if (excludeId != null) {
            return syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumberAndIdNot(
                curriculumTermCourseId, unitNumber, excludeId);
        }
        return syllabusUnitRepository.existsByCurriculumSemesterCourseIdAndUnitNumber(
            curriculumTermCourseId, unitNumber);
    }

    /** A unit's plannedHours counts against exactly one of the parent's theory/lab/clinical hour
     *  totals (per componentType) -- the sum of every OTHER active unit of that same type, plus
     *  this one, may never exceed the subject's declared total for that bucket. excludeUnitId
     *  omits the unit being edited from its own "existing" sum (null on create). */
    private void validateHourBudget(CurriculumSemesterCourse course, AttendanceType componentType,
                                     Integer plannedHours, Long excludeUnitId) {
        if (plannedHours == null || plannedHours <= 0) {
            return;
        }

        int declaredTotal = switch (componentType) {
            case THEORY -> orZero(course.getTheoryHours());
            case LAB -> orZero(course.getLabHours());
            case CLINICAL -> orZero(course.getClinicalHours());
        };

        int existingTotal = syllabusUnitRepository
            .findByCurriculumSemesterCourseIdAndComponentType(course.getId(), componentType)
            .stream()
            .filter(u -> !u.getId().equals(excludeUnitId))
            .mapToInt(u -> orZero(u.getPlannedHours()))
            .sum();

        int newTotal = existingTotal + plannedHours;
        if (newTotal > declaredTotal) {
            throw new IllegalArgumentException(
                "Total " + componentType.name() + " hours for this subject would be " + newTotal
                    + "h, exceeding the declared " + componentType.name() + " total of " + declaredTotal + "h");
        }
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }

    private SyllabusUnit findOrThrow(Long id) {
        return syllabusUnitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus unit not found with id: " + id));
    }

    private SyllabusUnitDto toDto(SyllabusUnit u) {
        return new SyllabusUnitDto(
            u.getId(),
            u.getCurriculumSemesterCourse().getId(),
            u.getUnitNumber(),
            u.getTitle(),
            u.getComponentType(),
            u.getPlannedHours(),
            u.getDescription(),
            u.getSortOrder(),
            u.getIsActive(),
            u.getCreatedAt(),
            u.getUpdatedAt()
        );
    }
}
