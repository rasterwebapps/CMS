package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FacultyAvailabilityRequest;
import com.cms.dto.FacultyAvailabilityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;

@ExtendWith(MockitoExtension.class)
class FacultyAvailabilityServiceTest {

    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;

    private FacultyAvailabilityService service;
    private Faculty faculty;

    @BeforeEach
    void setUp() {
        service = new FacultyAvailabilityService(facultyAvailabilityRepository, facultyRepository, classScheduleRepository);

        Speciality speciality = new Speciality("General Nursing", "GN", "dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Professor", "PROFESSOR", null);
        designation.setId(1L);
        faculty = new Faculty("EMP001", "Divya", "Krishnan", "divya@test.com", "9999999999",
            speciality, designation, "Medical-Surgical", "Skills Lab", null, com.cms.model.enums.FacultyStatus.ACTIVE);
        faculty.setId(1L);
    }

    private ClassSchedule scheduledClass(TermInstance termInstance, String subjectName) {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName(subjectName);

        ClassSchedule cs = new ClassSchedule();
        cs.setId(1L);
        cs.setFaculty(faculty);
        cs.setSubject(subject);
        cs.setTermInstance(termInstance);
        cs.setDayOfWeek(DayOfWeek.MONDAY);
        cs.setIsActive(true);
        return cs;
    }

    private TermInstance termInstance(LocalDate start, LocalDate end, TermInstanceStatus status) {
        AcademicYear ay = new AcademicYear("2026-2027", start, end, false);
        ay.setId(1L);
        TermInstance ti = new TermInstance(ay, TermType.ODD, start, end, status);
        ti.setId(1L);
        return ti;
    }

    private FacultyAvailabilityRequest indefiniteRequest() {
        return new FacultyAvailabilityRequest(1L, DayOfWeek.MONDAY,
            LocalTime.of(11, 45), LocalTime.of(12, 35), "Admin duty", null, null);
    }

    @Test
    void addBlock_rejectsWhenEndTimeNotAfterStartTime() {
        FacultyAvailabilityRequest request = new FacultyAvailabilityRequest(1L, DayOfWeek.MONDAY,
            LocalTime.of(10, 0), LocalTime.of(9, 0), "reason", null, null);

        assertThatThrownBy(() -> service.addBlock(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End time must be after start time");
    }

    @Test
    void addBlock_rejectsWhenOnlyOneOfStartEndDateProvided() {
        FacultyAvailabilityRequest request = new FacultyAvailabilityRequest(1L, DayOfWeek.MONDAY,
            LocalTime.of(9, 0), LocalTime.of(10, 0), "reason", LocalDate.of(2026, 8, 1), null);

        assertThatThrownBy(() -> service.addBlock(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("together");
    }

    @Test
    void addBlock_rejectsWhenEndDateBeforeStartDate() {
        FacultyAvailabilityRequest request = new FacultyAvailabilityRequest(1L, DayOfWeek.MONDAY,
            LocalTime.of(9, 0), LocalTime.of(10, 0), "reason", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> service.addBlock(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End date must not be before start date");
    }

    @Test
    void addBlock_rejectsIndefiniteBlockWhenClassAlreadyScheduledThen() {
        TermInstance term = termInstance(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), TermInstanceStatus.OPEN);
        ClassSchedule existing = scheduledClass(term, "Nursing Foundation I");

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.findActiveConflictingForFaculty(1L, DayOfWeek.MONDAY, LocalTime.of(11, 45), LocalTime.of(12, 35)))
            .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.addBlock(indefiniteRequest()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Nursing Foundation I");

        verify(facultyAvailabilityRepository, never()).save(any());
    }

    @Test
    void addBlock_allowsRangedBlockWhenRangeDoesNotOverlapConflictingClasssTerm() {
        TermInstance term = termInstance(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), TermInstanceStatus.OPEN);
        ClassSchedule existing = scheduledClass(term, "Nursing Foundation I");
        FacultyAvailabilityRequest request = new FacultyAvailabilityRequest(1L, DayOfWeek.MONDAY,
            LocalTime.of(11, 45), LocalTime.of(12, 35), "Pre-term duty",
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 9, 30));

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.findActiveConflictingForFaculty(1L, DayOfWeek.MONDAY, LocalTime.of(11, 45), LocalTime.of(12, 35)))
            .thenReturn(List.of(existing));
        when(facultyAvailabilityRepository.save(any(FacultyAvailability.class))).thenAnswer(inv -> {
            FacultyAvailability fa = inv.getArgument(0);
            fa.setId(1L);
            return fa;
        });

        FacultyAvailabilityResponse response = service.addBlock(request);

        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void addBlock_rejectsRangedBlockWhenRangeOverlapsConflictingClasssTerm() {
        TermInstance term = termInstance(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), TermInstanceStatus.OPEN);
        ClassSchedule existing = scheduledClass(term, "Nursing Foundation I");
        FacultyAvailabilityRequest request = new FacultyAvailabilityRequest(1L, DayOfWeek.MONDAY,
            LocalTime.of(11, 45), LocalTime.of(12, 35), "In-term duty",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.findActiveConflictingForFaculty(1L, DayOfWeek.MONDAY, LocalTime.of(11, 45), LocalTime.of(12, 35)))
            .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.addBlock(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Nursing Foundation I");

        verify(facultyAvailabilityRepository, never()).save(any());
    }

    @Test
    void addBlock_savesWhenNoConflict() {
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(classScheduleRepository.findActiveConflictingForFaculty(1L, DayOfWeek.MONDAY, LocalTime.of(11, 45), LocalTime.of(12, 35)))
            .thenReturn(List.of());
        when(facultyAvailabilityRepository.save(any(FacultyAvailability.class))).thenAnswer(inv -> {
            FacultyAvailability fa = inv.getArgument(0);
            fa.setId(1L);
            return fa;
        });

        FacultyAvailabilityResponse response = service.addBlock(indefiniteRequest());

        assertThat(response.reason()).isEqualTo("Admin duty");
        assertThat(response.startDate()).isNull();
        assertThat(response.endDate()).isNull();
    }

    @Test
    void addBlock_throwsWhenFacultyNotFound() {
        when(facultyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addBlock(indefiniteRequest()))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(classScheduleRepository, never()).findActiveConflictingForFaculty(any(), any(), any(), any());
    }

    @Test
    void removeBlock_throwsWhenNotFound() {
        when(facultyAvailabilityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.removeBlock(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
