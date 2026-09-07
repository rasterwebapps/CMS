package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
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

import com.cms.model.AcademicYear;
import com.cms.model.ClinicalShiftGroup;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.SessionOccurrence;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClinicalShiftGroupRepository;
import com.cms.repository.ClinicalShiftTheoryBlockRepository;
import com.cms.repository.SessionOccurrenceRepository;

/** Regression coverage for the real incident (2026-09-05): a Clinical Shift Group with no ceiling
 *  on how many weeks it ran kept generating/crediting duty hours for the whole term regardless of
 *  the offering's actual curriculum Clinical hours requirement -- three subjects reported 468h
 *  combined assigned against a 400h combined requirement. {@link
 *  ClinicalShiftOccurrenceService#generateForDate} must refuse to generate a real occurrence once
 *  the group has already delivered its full curriculum requirement, mirroring the same cap {@link
 *  TimetableSkeletonServiceTest#clinicalShiftHoursAreCappedAtWhatTheCurriculumRequirementActuallyNeeds_notTheWholeTerm}
 *  asserts on the hours-crediting side -- the two must never disagree. */
@ExtendWith(MockitoExtension.class)
class ClinicalShiftOccurrenceServiceTest {

    @Mock private ClinicalShiftGroupRepository shiftGroupRepository;
    @Mock private ClinicalShiftTheoryBlockRepository theoryBlockRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;

    private ClinicalShiftOccurrenceService service;

    @BeforeEach
    void setUp() {
        service = new ClinicalShiftOccurrenceService(shiftGroupRepository, theoryBlockRepository,
            batchRepository, sessionOccurrenceRepository);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        // 2024-06-03 is a Monday -- the shift group's own day-of-week, so its first real
        // occurrence lands exactly on the term's start date with no rounding to worry about.
        TermInstance termInstance = new TermInstance(ay, TermType.ODD,
            LocalDate.of(2024, 6, 3), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);

        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setClinicalHours(133); // ceil(133/6) = 23 weekly 6h shifts needed (138h delivered)

        CourseOffering offering = new CourseOffering();
        offering.setId(100L);
        offering.setTermInstance(termInstance);
        offering.setCurriculumSemesterCourse(csc);
        offering.setClinicalShiftDurationMinutes(360);

        ClinicalShiftGroup group = new ClinicalShiftGroup();
        group.setId(1L);
        group.setCourseOffering(offering);
        group.setTermInstance(termInstance);
        group.setDayOfWeek(DayOfWeek.MONDAY);
        group.setClinicalStartTime(LocalTime.of(7, 0));
        group.setIsActive(true);

        when(shiftGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        // lenient(): the cutoff-refusal test throws before either lookup is ever reached.
        lenient().when(batchRepository.findByClinicalShiftGroupId(1L)).thenReturn(List.of());
        lenient().when(theoryBlockRepository.findByShiftGroupIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of());
    }

    @Test
    void generatesNormallyOnTheLastWeekTheHoursRequirementStillNeeds() {
        // Week 23 (the last needed week): 2024-06-03 + 22 weeks = 2024-11-04.
        LocalDate lastNeededWeek = LocalDate.of(2024, 11, 4);

        List<SessionOccurrence> created = service.generateForDate(1L, lastNeededWeek);

        assertThat(created).isEmpty(); // no batches/theory blocks configured -- just proves no exception
    }

    @Test
    void refusesToGenerateOnceCumulativeHoursAlreadyMeetTheCurriculumRequirement() {
        // One week past the 23 needed: 2024-06-03 + 23 weeks = 2024-11-11.
        LocalDate oneWeekPastCutoff = LocalDate.of(2024, 11, 11);

        assertThatThrownBy(() -> service.generateForDate(1L, oneWeekPastCutoff))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already delivers its full 133h Clinical requirement");
    }
}
