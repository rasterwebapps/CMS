package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CalendarEventRequest;
import com.cms.dto.CalendarEventResponse;
import com.cms.dto.SemesterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.CalendarEvent;
import com.cms.model.Semester;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.SemesterStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.SemesterRepository;

@Service
@Transactional(readOnly = true)
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    public CalendarEventService(CalendarEventRepository calendarEventRepository,
                                AcademicYearRepository academicYearRepository,
                                SemesterRepository semesterRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        validateDateRange(request);

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));

        Semester semester = null;
        if (request.semesterId() != null) {
            semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Semester not found with id: " + request.semesterId()));
        }

        CalendarEvent event = new CalendarEvent();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setEventType(request.eventType());
        event.setAcademicYear(academicYear);
        event.setSemester(semester);

        return toResponse(calendarEventRepository.save(event));
    }

    public List<CalendarEventResponse> findAll() {
        return calendarEventRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public CalendarEventResponse findById(Long id) {
        return toResponse(calendarEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Calendar event not found with id: " + id)));
    }

    public List<CalendarEventResponse> findByAcademicYearId(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return calendarEventRepository.findByAcademicYearIdOrderByStartDate(academicYearId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<CalendarEventResponse> findByAcademicYearIdAndEventType(
            Long academicYearId, CalendarEventType eventType) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return calendarEventRepository
            .findByAcademicYearIdAndEventTypeOrderByStartDate(academicYearId, eventType).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<CalendarEventResponse> findBySemesterId(Long semesterId) {
        if (!semesterRepository.existsById(semesterId)) {
            throw new ResourceNotFoundException("Semester not found with id: " + semesterId);
        }
        return calendarEventRepository.findBySemesterIdOrderByStartDate(semesterId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request) {
        CalendarEvent event = calendarEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Calendar event not found with id: " + id));

        validateDateRange(request);

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));

        Semester semester = null;
        if (request.semesterId() != null) {
            semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Semester not found with id: " + request.semesterId()));
        }

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setEventType(request.eventType());
        event.setAcademicYear(academicYear);
        event.setSemester(semester);

        return toResponse(calendarEventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        if (!calendarEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Calendar event not found with id: " + id);
        }
        calendarEventRepository.deleteById(id);
    }

    private void validateDateRange(CalendarEventRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
    }

    private CalendarEventResponse toResponse(CalendarEvent event) {
        AcademicYear ay = event.getAcademicYear();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            ay.getId(), ay.getName(), ay.getStartDate(), ay.getEndDate(),
            ay.getIsCurrent(), ay.getCreatedAt(), ay.getUpdatedAt());

        SemesterResponse semesterResponse = null;
        if (event.getSemester() != null) {
            Semester s = event.getSemester();
            AcademicYear sAy = s.getAcademicYear();
            AcademicYearResponse sAyResponse = new AcademicYearResponse(
                sAy.getId(), sAy.getName(), sAy.getStartDate(), sAy.getEndDate(),
                sAy.getIsCurrent(), sAy.getCreatedAt(), sAy.getUpdatedAt());
            SemesterStatus status = s.getStatus() != null
                ? s.getStatus()
                : Semester.deriveStatus(s.getStartDate(), s.getEndDate());
            semesterResponse = new SemesterResponse(
                s.getId(), s.getName(), sAyResponse, s.getStartDate(), s.getEndDate(),
                s.getSemesterNumber(), status, s.getCreatedAt(), s.getUpdatedAt());
        }

        return new CalendarEventResponse(
            event.getId(), event.getTitle(), event.getDescription(),
            event.getStartDate(), event.getEndDate(), event.getEventType(),
            ayResponse, semesterResponse, event.getCreatedAt(), event.getUpdatedAt());
    }
}
