package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.HolidayDayInfo;
import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.HolidayCategory;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class PersonalTimetableServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassScheduleService classScheduleService;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private CalendarEventRepository calendarEventRepository;

    private PersonalTimetableService service;
    private AcademicYear academicYear;
    private TermInstance termInstance;

    @BeforeEach
    void setUp() {
        service = new PersonalTimetableService(classScheduleRepository, classScheduleService,
            studentTermEnrollmentRepository, courseRegistrationRepository, batchRepository,
            termInstanceRepository, calendarEventRepository);

        academicYear = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        academicYear.setId(1L);
        academicYear.setCreatedAt(Instant.now());
        academicYear.setUpdatedAt(Instant.now());

        termInstance = new TermInstance(academicYear, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        termInstance.setCreatedAt(Instant.now());
        termInstance.setUpdatedAt(Instant.now());
    }

    @Test
    void shouldReturnEmptyForUnknownEntityType() {
        ProfileIdentity identity = new ProfileIdentity("ADMIN", null, null, null, "Admin", null, null, null, null);
        when(classScheduleService.toResponseList(anyList())).thenReturn(List.of());

        MyTimetableResponse response = service.findMyTimetable(identity, 10L, null);

        assertThat(response.holidays()).isEmpty();
    }

    @Test
    void shouldResolveFacultyScheduleDirectlyByFacultyId() {
        ProfileIdentity identity = new ProfileIdentity("FACULTY", 5L, null, null, "Dr. Faculty", null, null, null, null);
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.PUBLISHED, 5L))
            .thenReturn(List.of(new ClassSchedule()));
        when(classScheduleService.toResponseList(anyList())).thenReturn(List.of());

        service.findMyTimetable(identity, 10L, null);

        org.mockito.Mockito.verify(classScheduleRepository)
            .findByTermInstanceIdAndStatusAndFacultyId(10L, ClassScheduleStatus.PUBLISHED, 5L);
    }

    @Test
    void shouldResolveStudentScheduleViaRegistrationsAndBatches() {
        ProfileIdentity identity = new ProfileIdentity("STUDENT", 7L, null, null, "A Student", null, null, null, null);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(100L);
        when(studentTermEnrollmentRepository.findByStudentIdAndTermInstanceId(7L, 10L)).thenReturn(Optional.of(enrollment));

        CourseOffering offering = new CourseOffering();
        offering.setId(200L);
        CourseRegistration reg = new CourseRegistration();
        reg.setId(300L);
        reg.setCourseOffering(offering);
        reg.setStatus(RegistrationStatus.REGISTERED);
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(100L)).thenReturn(List.of(reg));

        Batch batch = new Batch();
        batch.setId(400L);
        when(batchRepository.findByTermInstanceIdAndStudentId(10L, 7L)).thenReturn(List.of(batch));

        ClassSchedule theoryRow = new ClassSchedule();
        theoryRow.setSessionType(ClassSessionType.THEORY);
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndCourseOfferingIdIn(10L, ClassScheduleStatus.PUBLISHED, List.of(200L)))
            .thenReturn(List.of(theoryRow));

        ClassSchedule labRow = new ClassSchedule();
        labRow.setSessionType(ClassSessionType.LAB);
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndBatchIdIn(10L, ClassScheduleStatus.PUBLISHED, List.of(400L)))
            .thenReturn(List.of(labRow));

        when(classScheduleService.toResponseList(anyList())).thenReturn(List.of());

        service.findMyTimetable(identity, 10L, null);

        org.mockito.Mockito.verify(classScheduleService).toResponseList(List.of(theoryRow, labRow));
    }

    @Test
    void shouldReturnHolidaysWhenWeekStartSupplied() {
        ProfileIdentity identity = new ProfileIdentity("FACULTY", 5L, null, null, "Dr. Faculty", null, null, null, null);
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(anyLong(), any(), anyLong()))
            .thenReturn(Collections.emptyList());
        when(classScheduleService.toResponseList(anyList())).thenReturn(List.of());
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));

        LocalDate weekStart = LocalDate.of(2024, 8, 12); // Monday
        CalendarEvent independenceDay = new CalendarEvent();
        independenceDay.setTitle("Independence Day");
        independenceDay.setHolidayCategory(HolidayCategory.GOVERNMENT);
        when(calendarEventRepository.findOverlapping(any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(calendarEventRepository.findOverlapping(1L, CalendarEventType.HOLIDAY, weekStart.plusDays(2), weekStart.plusDays(2)))
            .thenReturn(List.of(independenceDay));

        MyTimetableResponse response = service.findMyTimetable(identity, 10L, weekStart);

        assertThat(response.holidays()).containsExactly(
            new HolidayDayInfo(2, "Independence Day", HolidayCategory.GOVERNMENT));
    }
}
