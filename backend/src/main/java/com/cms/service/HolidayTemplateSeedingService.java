package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AcademicYear;
import com.cms.model.HolidayTemplate;
import com.cms.repository.HolidayTemplateRepository;
import com.cms.service.HolidayTemplateDateCalculator.DateRange;

/** Materializes every active {@link HolidayTemplate} into concrete HOLIDAY {@link
 *  com.cms.model.CalendarEvent} rows for a newly-created AcademicYear -- run exactly once, at
 *  AcademicYear creation time (see {@code AcademicYearService.create}). Not re-run on update, and
 *  a template added after a year already exists never retroactively seeds it -- both are accepted
 *  v1 scope gaps (see the project plan's risk list), not oversights. Each seeded event reuses
 *  {@code CalendarEventService.createSeededHolidayEvent}, which itself runs the same
 *  syncHolidayBlocks auto-block logic a manually-created HOLIDAY event gets. */
@Service
public class HolidayTemplateSeedingService {

    private final HolidayTemplateRepository holidayTemplateRepository;
    private final CalendarEventService calendarEventService;

    public HolidayTemplateSeedingService(HolidayTemplateRepository holidayTemplateRepository,
                                          CalendarEventService calendarEventService) {
        this.holidayTemplateRepository = holidayTemplateRepository;
        this.calendarEventService = calendarEventService;
    }

    @Transactional
    public void seedForAcademicYear(AcademicYear academicYear) {
        List<HolidayTemplate> templates = holidayTemplateRepository.findByIsActiveTrue();
        for (HolidayTemplate template : templates) {
            List<DateRange> occurrences = HolidayTemplateDateCalculator.computeOccurrences(
                template, academicYear.getStartDate(), academicYear.getEndDate());
            for (DateRange range : occurrences) {
                calendarEventService.createSeededHolidayEvent(
                    template, range.start(), range.end(), academicYear);
            }
        }
    }
}
