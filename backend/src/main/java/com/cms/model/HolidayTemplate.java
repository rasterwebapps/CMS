package com.cms.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.HolidayCategory;
import com.cms.model.enums.HolidayRecurrenceType;
import com.cms.model.enums.WeekOfMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A recurring holiday rule (e.g. "Republic Day" every Jan 26, "2nd Saturday off" every month,
 *  "every 2 weeks" from some start date), independent of any single AcademicYear -- a template
 *  outlives any one year's calendar and is materialized into concrete {@link CalendarEvent} rows
 *  (eventType HOLIDAY) by {@code HolidayTemplateSeedingService} whenever a new AcademicYear is
 *  created, or immediately when created inline from the Add Event form's "Repeats" picker (see
 *  {@code CalendarEventService}). Mirrors the shape of a simplified iOS/Google Calendar Repeat
 *  rule: a frequency + interval, frequency-specific pattern fields, and an optional end date. */
@Entity
@Table(name = "holiday_templates")
@EntityListeners(AuditingEntityListener.class)
public class HolidayTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    private HolidayRecurrenceType recurrenceType;

    /** Which CalendarEvent#eventType this template seeds -- originally always HOLIDAY (the
     *  template name predates this field), now any type since a repeating event can be created
     *  inline from the Add Event form regardless of type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CalendarEventType eventType = CalendarEventType.HOLIDAY;

    /** Only meaningful when eventType == HOLIDAY; null otherwise (mirrors CalendarEvent's own
     *  resolveHolidayCategory convention). */
    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_category")
    private HolidayCategory holidayCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 1;

    /** "Every N [recurrenceType units]" -- e.g. recurrenceType=WEEKLY, intervalCount=2 means
     *  "every 2 weeks". Always >= 1; 1 means every single occurrence (the common case). */
    @Column(name = "interval_count", nullable = false)
    private Integer intervalCount = 1;

    /** Required whenever intervalCount > 1 (need a reference point to know which weeks/months/etc
     *  align with the interval), and for DAILY always. Optional at intervalCount == 1 for
     *  WEEKLY/MONTHLY/YEARLY, where the pattern fields alone already fully determine every
     *  occurrence. When a repeating event is created inline from the Add Event form, this is set
     *  to that event's own start date. */
    @Column(name = "anchor_date")
    private LocalDate anchorDate;

    /** Null means repeats forever. A UI "after N occurrences" choice is translated into a concrete
     *  end date at save time rather than stored as a count. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** YEARLY: required, 1-12. MONTHLY fixed-day-of-month pattern: null (uses every month).
     *  Null for DAILY/WEEKLY and MONTHLY's nth-weekday pattern. */
    private Integer month;

    /** YEARLY: required, 1-31 (an invalid combination for a given year, e.g. Feb 29 in a
     *  non-leap year, is simply skipped that year -- see HolidayTemplateDateCalculator).
     *  MONTHLY fixed-day-of-month pattern: required, 1-31, with weekOfMonth/dayOfWeek both null.
     *  Null for DAILY/WEEKLY and MONTHLY's nth-weekday pattern. */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /** MONTHLY nth-weekday pattern only (e.g. "2nd Saturday"), paired with dayOfWeek, with
     *  dayOfMonth null. Null otherwise. */
    @Enumerated(EnumType.STRING)
    @Column(name = "week_of_month")
    private WeekOfMonth weekOfMonth;

    /** Required for WEEKLY (which weekday it repeats on) and for MONTHLY's nth-weekday pattern
     *  (paired with weekOfMonth). Null for DAILY, YEARLY, and MONTHLY's fixed-day-of-month
     *  pattern. Sunday is deliberately not representable here (the app's DayOfWeek enum only has
     *  MONDAY..SATURDAY) since Sunday is already globally treated as non-teaching everywhere else
     *  in the codebase -- a "every Sunday" rule would be a no-op. */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public HolidayTemplate() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HolidayRecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(HolidayRecurrenceType recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public CalendarEventType getEventType() {
        return eventType;
    }

    public void setEventType(CalendarEventType eventType) {
        this.eventType = eventType;
    }

    public HolidayCategory getHolidayCategory() {
        return holidayCategory;
    }

    public void setHolidayCategory(HolidayCategory holidayCategory) {
        this.holidayCategory = holidayCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Integer getIntervalCount() {
        return intervalCount;
    }

    public void setIntervalCount(Integer intervalCount) {
        this.intervalCount = intervalCount;
    }

    public LocalDate getAnchorDate() {
        return anchorDate;
    }

    public void setAnchorDate(LocalDate anchorDate) {
        this.anchorDate = anchorDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public WeekOfMonth getWeekOfMonth() {
        return weekOfMonth;
    }

    public void setWeekOfMonth(WeekOfMonth weekOfMonth) {
        this.weekOfMonth = weekOfMonth;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
