package com.cms.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

/** A recurring holiday rule (e.g. "Republic Day" every Jan 26, "2nd Saturday off" every month),
 *  independent of any single AcademicYear -- a template outlives any one year's calendar and is
 *  materialized into concrete {@link CalendarEvent} rows (eventType HOLIDAY) by
 *  {@code HolidayTemplateSeedingService} whenever a new AcademicYear is created. See
 *  {@link HolidayRecurrenceType} for why WEEKLY isn't a supported shape here. */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_category")
    private HolidayCategory holidayCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 1;

    /** YEARLY only: 1-12. Null for MONTHLY. */
    private Integer month;

    /** YEARLY only: 1-31 (an invalid combination for a given year, e.g. Feb 29 in a non-leap
     *  year, is simply skipped that year -- see HolidayTemplateDateCalculator). Null for MONTHLY. */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /** MONTHLY only. Null for YEARLY. */
    @Enumerated(EnumType.STRING)
    @Column(name = "week_of_month")
    private WeekOfMonth weekOfMonth;

    /** MONTHLY only. Null for YEARLY. */
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
