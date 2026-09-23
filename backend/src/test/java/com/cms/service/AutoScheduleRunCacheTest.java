package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.Room;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;

class AutoScheduleRunCacheTest {

    private final Period period = period();

    @Test
    void shouldCarryTheLabCommittedAtStaffingIntoTheCachedCell() {
        // placeCell saves a LAB row with no faculty and no lab; staffCell commits both later, on its
        // own copy of the row. The cache used to copy only the faculty across, so for the rest of
        // the run the room-free check saw an empty lab and stacked the next batch into it at the
        // same slot -- four 30-seat batches in one Computer lab on local dev.
        ClassScheduleRepository repository = mock(ClassScheduleRepository.class);
        when(repository.findByTermInstanceId(10L)).thenReturn(List.of(placedLabCell()));

        Lab lab = new Lab();
        lab.setId(1L);
        Faculty faculty = new Faculty();
        faculty.setId(31L);
        ClassSchedule staffed = placedLabCell();
        staffed.setLab(lab);
        staffed.setFaculty(faculty);

        List<ClassSchedule> seenByTheSameRun = AutoScheduleRunCache.run(10L, repository, () -> {
            AutoScheduleRunCache cache = AutoScheduleRunCache.current().orElseThrow();
            cache.recordStaffing(staffed);
            return cache.overlapping(DayOfWeek.MONDAY, period.getStartTime(), period.getEndTime(),
                ClassScheduleStatus.DRAFT, null);
        });

        assertThat(seenByTheSameRun).singleElement().satisfies(cell -> {
            assertThat(TimetableStaffingService.venueIdOf(cell)).isEqualTo(1L);
            assertThat(cell.getFaculty()).isSameAs(faculty);
        });
    }

    @Test
    void shouldForceLoadTheClassroomsRoomWhenStaffingSoALaterConflictCheckNeverHitsALazyProxy() {
        // TimetableStaffingService#staffCell runs in its own REQUIRES_NEW transaction, so a
        // classroom/room proxy still uninitialized when it's copied into this run-long cache can
        // never be resolved later -- the owning (now-suspended) session isn't the one active on
        // the thread at that point, regardless of whether it's technically still open. Forcing the
        // load here, while staffCell's own session is still current, is what makes the cached
        // cell's room safe to read from a completely different transaction afterward (see
        // TimetableStaffingService#physicalRoomOf, the actual reader).
        ClassScheduleRepository repository = mock(ClassScheduleRepository.class);
        when(repository.findByTermInstanceId(10L)).thenReturn(List.of(placedTheoryCell()));

        Room room = mock(Room.class);
        Classroom classroom = mock(Classroom.class);
        when(classroom.getRoom()).thenReturn(room);
        when(classroom.getId()).thenReturn(1L);
        ClassSchedule staffed = placedTheoryCell();
        staffed.setClassroom(classroom);

        AutoScheduleRunCache.run(10L, repository, () -> {
            AutoScheduleRunCache.current().orElseThrow().recordStaffing(staffed);
            return null;
        });

        verify(classroom).getRoom();
    }

    @Test
    void shouldForceLoadTheClassroomsRoomOnTheInitialCacheLoadToo() {
        ClassSchedule alreadyStaffedBeforeThisRun = placedTheoryCell();
        Room room = mock(Room.class);
        Classroom classroom = mock(Classroom.class);
        when(classroom.getRoom()).thenReturn(room);
        alreadyStaffedBeforeThisRun.setClassroom(classroom);

        ClassScheduleRepository repository = mock(ClassScheduleRepository.class);
        when(repository.findByTermInstanceId(10L)).thenReturn(List.of(alreadyStaffedBeforeThisRun));

        AutoScheduleRunCache.run(10L, repository, () -> null);

        verify(classroom).getRoom();
    }

    private ClassSchedule placedTheoryCell() {
        ClassSchedule cs = new ClassSchedule();
        cs.setId(501L);
        cs.setSessionType(ClassSessionType.THEORY);
        cs.setStatus(ClassScheduleStatus.DRAFT);
        cs.setIsActive(true);
        cs.setDayOfWeek(DayOfWeek.MONDAY);
        cs.setPeriod(period);
        return cs;
    }

    private ClassSchedule placedLabCell() {
        ClassSchedule cs = new ClassSchedule();
        cs.setId(500L);
        cs.setSessionType(ClassSessionType.LAB);
        cs.setStatus(ClassScheduleStatus.DRAFT);
        cs.setIsActive(true);
        cs.setDayOfWeek(DayOfWeek.MONDAY);
        cs.setPeriod(period);
        return cs;
    }

    private static Period period() {
        Period p = new Period("Period 7", LocalTime.of(14, 0), LocalTime.of(14, 50), 7);
        p.setId(7L);
        return p;
    }
}
