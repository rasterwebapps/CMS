package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CurriculumElectiveGroupDto;
import com.cms.dto.CurriculumElectiveGroupRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumVersion;
import com.cms.repository.CurriculumElectiveGroupRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;

@Service
@Transactional(readOnly = true)
public class CurriculumElectiveGroupService {

    private final CurriculumElectiveGroupRepository groupRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final CurriculumSemesterCourseRepository semesterCourseRepository;

    public CurriculumElectiveGroupService(CurriculumElectiveGroupRepository groupRepository,
                                           CurriculumVersionRepository curriculumVersionRepository,
                                           CurriculumSemesterCourseRepository semesterCourseRepository) {
        this.groupRepository = groupRepository;
        this.curriculumVersionRepository = curriculumVersionRepository;
        this.semesterCourseRepository = semesterCourseRepository;
    }

    @Transactional
    public CurriculumElectiveGroupDto createGroup(CurriculumElectiveGroupRequest request) {
        CurriculumVersion cv = curriculumVersionRepository.findById(request.curriculumVersionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum version not found with id: " + request.curriculumVersionId()));

        CurriculumElectiveGroup group = new CurriculumElectiveGroup(
            cv, request.termNumber(), request.groupName(), request.groupCode());
        return toDto(groupRepository.save(group));
    }

    public List<CurriculumElectiveGroupDto> getGroups(Long curriculumVersionId, Integer termNumber) {
        return groupRepository.findByCurriculumVersionIdAndTermNumber(curriculumVersionId, termNumber)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public void deleteGroup(Long id) {
        CurriculumElectiveGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum elective group not found with id: " + id));
        if (semesterCourseRepository.existsByElectiveGroupId(group.getId())) {
            throw new IllegalStateException(
                "Cannot delete an elective group that still has subjects assigned to it");
        }
        groupRepository.delete(group);
    }

    private CurriculumElectiveGroupDto toDto(CurriculumElectiveGroup g) {
        return new CurriculumElectiveGroupDto(
            g.getId(),
            g.getCurriculumVersion().getId(),
            g.getTermNumber(),
            g.getGroupName(),
            g.getGroupCode(),
            g.getCreatedAt(),
            g.getUpdatedAt()
        );
    }
}
