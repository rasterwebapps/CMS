package com.cms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FacultyAvailabilityRequest;
import com.cms.dto.FacultyAvailabilityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;

@Service
@Transactional(readOnly = true)
public class FacultyAvailabilityService {

    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final FacultyRepository facultyRepository;
    private final ClassScheduleRepository classScheduleRepository;

    public FacultyAvailabilityService(FacultyAvailabilityRepository facultyAvailabilityRepository,
                                       FacultyRepository facultyRepository,
                                       ClassScheduleRepository classScheduleRepository) {
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.facultyRepository = facultyRepository;
        this.classScheduleRepository = classScheduleRepository;
    }

    public List<FacultyAvailabilityResponse> listForFaculty(Long facultyId) {
        return facultyAvailabilityRepository.findByFacultyIdOrderByDayOfWeekAscStartTimeAsc(facultyId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FacultyAvailabilityResponse addBlock(FacultyAvailabilityRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

        List<ClassSchedule> conflicting = classScheduleRepository.findActiveConflictingForFaculty(
            request.facultyId(), request.dayOfWeek(), request.startTime(), request.endTime());
        if (!conflicting.isEmpty()) {
            String details = conflicting.stream()
                .map(cs -> cs.getSubject().getName() + " ("
                    + cs.getTermInstance().getAcademicYear().getName() + " " + cs.getTermInstance().getTermType() + ")")
                .distinct()
                .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                "Cannot block this period — " + faculty.getFullName() + " is already scheduled to teach "
                    + conflicting.size() + " class(es) then: " + details
                    + ". Reassign or swap these first (Faculty Absence substitute, or Staff Session Swap), "
                    + "then block this availability.");
        }

        FacultyAvailability block = new FacultyAvailability();
        block.setFaculty(faculty);
        block.setDayOfWeek(request.dayOfWeek());
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());
        block.setReason(request.reason());
        return toResponse(facultyAvailabilityRepository.save(block));
    }

    @Transactional
    public void removeBlock(Long id) {
        if (!facultyAvailabilityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Faculty availability block not found with id: " + id);
        }
        facultyAvailabilityRepository.deleteById(id);
    }

    private FacultyAvailabilityResponse toResponse(FacultyAvailability fa) {
        return new FacultyAvailabilityResponse(
            fa.getId(),
            fa.getFaculty().getId(),
            fa.getFaculty().getFullName(),
            fa.getDayOfWeek(),
            fa.getStartTime(),
            fa.getEndTime(),
            fa.getReason(),
            fa.getCreatedAt(),
            fa.getUpdatedAt()
        );
    }
}
