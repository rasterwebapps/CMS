package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CalendarEventRequest;
import com.cms.dto.CalendarEventResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.CalendarEvent;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.HolidayCategory;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CalendarEventRepository;

@Service
@Transactional(readOnly = true)
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final AcademicYearRepository academicYearRepository;

    public CalendarEventService(CalendarEventRepository calendarEventRepository,
                                AcademicYearRepository academicYearRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));
        validateDateRange(request, academicYear);
        HolidayCategory holidayCategory = resolveHolidayCategory(request);

        CalendarEvent event = new CalendarEvent();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setEventType(request.eventType());
        event.setHolidayCategory(holidayCategory);
        event.setAcademicYear(academicYear);

        return toResponse(calendarEventRepository.save(event));
    }

    public List<CalendarEventResponse> findAll() {
        return calendarEventRepository.findAll().stream().map(this::toResponse).toList();
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
            .map(this::toResponse).toList();
    }

    public List<CalendarEventResponse> findByAcademicYearIdAndEventType(
            Long academicYearId, CalendarEventType eventType) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return calendarEventRepository
            .findByAcademicYearIdAndEventTypeOrderByStartDate(academicYearId, eventType).stream()
            .map(this::toResponse).toList();
    }

    @Transactional
    public CalendarEventResponse update(Long id, CalendarEventRequest request) {
        CalendarEvent event = calendarEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Calendar event not found with id: " + id));

        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found with id: " + request.academicYearId()));
        validateDateRange(request, academicYear);
        HolidayCategory holidayCategory = resolveHolidayCategory(request);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setEventType(request.eventType());
        event.setHolidayCategory(holidayCategory);
        event.setAcademicYear(academicYear);

        return toResponse(calendarEventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        if (!calendarEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Calendar event not found with id: " + id);
        }
        calendarEventRepository.deleteById(id);
    }

    /** Mirrors the containment check {@code TermInstanceService.assertTermWithinAcademicYear}
     *  already enforces for terms -- an event dated outside its own linked Academic Year was
     *  previously accepted silently, which is exactly the class of bug that produced
     *  inconsistent term/calendar data before this was caught. */
    private void validateDateRange(CalendarEventRequest request, AcademicYear academicYear) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        if (request.startDate().isBefore(academicYear.getStartDate())
                || request.endDate().isAfter(academicYear.getEndDate())) {
            throw new IllegalArgumentException(
                "Event dates must fall within the academic year's dates (" +
                    academicYear.getStartDate() + " to " + academicYear.getEndDate() + ")");
        }
    }

    /** Holiday category (government/local/institutional) is only a meaningful classification for
     *  eventType == HOLIDAY — silently dropped for every other type so a non-holiday event can
     *  never carry a stray category from a prior edit. Optional (not required) even for holidays,
     *  since not every institution needs the classification. */
    private HolidayCategory resolveHolidayCategory(CalendarEventRequest request) {
        return request.eventType() == CalendarEventType.HOLIDAY ? request.holidayCategory() : null;
    }

    private CalendarEventResponse toResponse(CalendarEvent event) {
        AcademicYear ay = event.getAcademicYear();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            ay.getId(), ay.getName(), ay.getStartDate(), ay.getEndDate(),
            ay.getIsCurrent(), ay.getCreatedAt(), ay.getUpdatedAt());

        return new CalendarEventResponse(
            event.getId(), event.getTitle(), event.getDescription(),
            event.getStartDate(), event.getEndDate(), event.getEventType(), event.getHolidayCategory(),
            ayResponse, event.getCreatedAt(), event.getUpdatedAt());
    }
}
