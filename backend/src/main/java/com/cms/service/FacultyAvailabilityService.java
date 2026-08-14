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
        boolean hasStart = request.startDate() != null;
        boolean hasEnd = request.endDate() != null;
        if (hasStart != hasEnd) {
            throw new IllegalArgumentException("Start date and end date must be provided together, or not at all");
        }
        if (hasStart && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

        List<ClassSchedule> allOverlapping = classScheduleRepository.findActiveConflictingForFaculty(
            request.facultyId(), request.dayOfWeek(), request.startTime(), request.endTime());
        // A block with no date range is indefinite -- it covers a class's whole term regardless.
        // A ranged block only actually conflicts with classes in terms whose own date range
        // overlaps the block's -- e.g. blocking only Aug-Oct doesn't conflict with a class that
        // only ever meets in a term that runs Jan-May.
        List<ClassSchedule> conflicting = !hasStart ? allOverlapping : allOverlapping.stream()
            .filter(cs -> !request.startDate().isAfter(cs.getTermInstance().getEndDate())
                && !request.endDate().isBefore(cs.getTermInstance().getStartDate()))
            .toList();
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
        block.setStartDate(request.startDate());
        block.setEndDate(request.endDate());
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
            fa.getStartDate(),
            fa.getEndDate(),
            fa.getCreatedAt(),
            fa.getUpdatedAt()
        );
    }
}
