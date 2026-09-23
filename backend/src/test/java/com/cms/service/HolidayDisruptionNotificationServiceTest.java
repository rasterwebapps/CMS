package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.Batch;
import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.Notification;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.ClassSessionType;
import com.cms.repository.FacultyRepository;
import com.cms.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class HolidayDisruptionNotificationServiceTest {

    @Mock
    private ClassScheduleOccurrenceService occurrenceService;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private NotificationRepository notificationRepository;

    private HolidayDisruptionNotificationService service;

    @BeforeEach
    void setUp() {
        service = new HolidayDisruptionNotificationService(occurrenceService, facultyRepository, notificationRepository);
    }

    private Faculty faculty(Long id, String first, String last) {
        Faculty f = new Faculty();
        f.setId(id);
        f.setFirstName(first);
        f.setLastName(last);
        return f;
    }

    private BlockedPeriod oneOffBlock(Long id, LocalDate date, String reason) {
        BlockedPeriod b = new BlockedPeriod();
        b.setId(id);
        b.setBlockType(BlockType.ONE_OFF);
        b.setSpecificDate(date);
        b.setReason(reason);
        Period period = new Period();
        period.setId(5L);
        period.setName("Period 1");
        b.setPeriod(period);
        return b;
    }

    private ClassSchedule theorySchedule(Faculty teacher, Subject subject) {
        ClassSchedule cs = new ClassSchedule();
        cs.setId(1L);
        cs.setFaculty(teacher);
        cs.setSubject(subject);
        cs.setSessionType(ClassSessionType.THEORY);
        Period period = new Period();
        period.setId(5L);
        period.setName("Period 1");
        cs.setPeriod(period);
        return cs;
    }

    @Test
    void doesNothingWhenNothingIsDisrupted() {
        BlockedPeriod block = oneOffBlock(1L, LocalDate.of(2026, 10, 2), "Government Holiday");
        when(occurrenceService.schedulesDisruptedBy(block)).thenReturn(List.of());

        service.notifyIfDisrupts(block);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifiesTheAssignedFacultyAndTheSubjectsHod() {
        Faculty teacher = faculty(10L, "Asha", "Rao");
        Faculty hod = faculty(20L, "Priya", "Nair");
        Speciality speciality = new Speciality();
        speciality.setId(1L);
        speciality.setHodFacultyId(20L);
        Subject subject = new Subject();
        subject.setId(30L);
        subject.setName("OBG Nursing II");
        subject.setSpeciality(speciality);

        BlockedPeriod block = oneOffBlock(1L, LocalDate.of(2026, 10, 2), "Government Holiday");
        ClassSchedule cs = theorySchedule(teacher, subject);

        when(occurrenceService.schedulesDisruptedBy(block)).thenReturn(List.of(cs));
        when(facultyRepository.findById(20L)).thenReturn(Optional.of(hod));

        service.notifyIfDisrupts(block);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<Long> recipients = captor.getAllValues().stream().map(Notification::getRecipientFacultyId).toList();
        assertThat(recipients).containsExactlyInAnyOrder(10L, 20L);
        assertThat(captor.getAllValues()).allSatisfy(n -> {
            assertThat(n.getCategoryKey()).isEqualTo("holidayDisruption");
            assertThat(n.getSourceType()).isEqualTo("BLOCKED_PERIOD");
            assertThat(n.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void notifiesOnlyOnceWhenFacultyIsAlsoTheHod() {
        Faculty teacherWhoIsAlsoHod = faculty(10L, "Asha", "Rao");
        Speciality speciality = new Speciality();
        speciality.setId(1L);
        speciality.setHodFacultyId(10L);
        Subject subject = new Subject();
        subject.setId(30L);
        subject.setName("OBG Nursing II");
        subject.setSpeciality(speciality);

        BlockedPeriod block = oneOffBlock(1L, LocalDate.of(2026, 10, 2), "Government Holiday");
        ClassSchedule cs = theorySchedule(teacherWhoIsAlsoHod, subject);

        when(occurrenceService.schedulesDisruptedBy(block)).thenReturn(List.of(cs));
        when(facultyRepository.findById(10L)).thenReturn(Optional.of(teacherWhoIsAlsoHod));

        service.notifyIfDisrupts(block);

        // Only one notification even though the HOD lookup resolves to the same person as the
        // assigned faculty -- deduped after resolution, not by skipping the lookup.
        verify(notificationRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void labSessionEscalatesToTheBatchCoordinatorInsteadOfAnHod() {
        Faculty teacher = faculty(10L, "Asha", "Rao");
        Faculty coordinator = faculty(40L, "Meera", "Iyer");
        Batch batch = new Batch();
        batch.setId(50L);
        batch.setCoordinatorFaculty(coordinator);

        Subject subject = new Subject();
        subject.setId(30L);
        subject.setName("Fundamentals of Nursing Lab");

        ClassSchedule cs = theorySchedule(teacher, subject);
        cs.setSessionType(ClassSessionType.LAB);
        cs.setBatch(batch);

        BlockedPeriod block = oneOffBlock(1L, LocalDate.of(2026, 10, 2), "Government Holiday");
        when(occurrenceService.schedulesDisruptedBy(block)).thenReturn(List.of(cs));

        service.notifyIfDisrupts(block);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Long> recipients = captor.getAllValues().stream().map(Notification::getRecipientFacultyId).toList();
        assertThat(recipients).containsExactlyInAnyOrder(10L, 40L);
        verify(facultyRepository, never()).findById(any());
    }

    @Test
    void batchesMultiplePeriodsOnTheSameDateIntoOneNotificationPerRecipient() {
        Faculty teacher = faculty(10L, "Asha", "Rao");
        Subject subject = new Subject();
        subject.setId(30L);
        subject.setName("OBG Nursing II");

        ClassSchedule period1 = theorySchedule(teacher, subject);
        ClassSchedule period2 = theorySchedule(teacher, subject);
        period2.setId(2L);

        BlockedPeriod block1 = oneOffBlock(1L, LocalDate.of(2026, 10, 2), "Government Holiday");
        BlockedPeriod block2 = oneOffBlock(2L, LocalDate.of(2026, 10, 2), "Government Holiday");
        CalendarEvent event = new CalendarEvent();
        event.setId(99L);
        event.setTitle("Gandhi Jayanti");

        when(occurrenceService.schedulesDisruptedBy(block1)).thenReturn(List.of(period1));
        when(occurrenceService.schedulesDisruptedBy(block2)).thenReturn(List.of(period2));

        service.notifyIfDisrupts(event, List.of(block1, block2));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(1)).save(captor.capture());
        assertThat(captor.getValue().getRecipientFacultyId()).isEqualTo(10L);
        assertThat(captor.getValue().getSourceType()).isEqualTo("CALENDAR_EVENT");
        assertThat(captor.getValue().getSourceId()).isEqualTo(99L);
        assertThat(captor.getValue().getMessage()).contains("your OBG Nursing II (Period 1)");
    }
}
