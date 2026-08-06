package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.RotationGroupCreateRequest;
import com.cms.dto.RotationGroupCreateRequest.RotationAssignmentInput;
import com.cms.dto.RotationGroupCreateRequest.RotationMemberInput;
import com.cms.dto.RotationGroupCreateRequest.RotationSlotInput;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.RotationGroup;
import com.cms.model.RotationMember;
import com.cms.model.RotationSlot;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.RotationGroupRepository;
import com.cms.repository.RotationMemberAssignmentRepository;
import com.cms.repository.RotationMemberRepository;
import com.cms.repository.RotationSlotRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class RotationGroupServiceTest {

    @Mock private RotationGroupRepository rotationGroupRepository;
    @Mock private RotationSlotRepository rotationSlotRepository;
    @Mock private RotationMemberRepository rotationMemberRepository;
    @Mock private RotationMemberAssignmentRepository rotationMemberAssignmentRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private RotationResolverService rotationResolverService;

    private RotationGroupService service;

    private TermInstance termInstance;
    private ClassSchedule englishCell;
    private ClassSchedule tamilCell;
    private Batch englishBatch1;
    private Batch englishBatch2;
    private Batch tamilBatch1;
    private Batch tamilBatch2;

    @BeforeEach
    void setUp() {
        service = new RotationGroupService(rotationGroupRepository, rotationSlotRepository,
            rotationMemberRepository, rotationMemberAssignmentRepository, classScheduleRepository,
            batchRepository, termInstanceRepository, rotationResolverService);

        AcademicYear ay = new AcademicYear("2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);

        Period period = new Period("Period 3", LocalTime.of(11, 0), LocalTime.of(12, 50), 3);
        period.setId(30L);

        Lab englishLab = new Lab();
        englishLab.setId(500L);
        englishLab.setName("English Lab");
        englishLab.setCapacity(30);
        Lab tamilLab = new Lab();
        tamilLab.setId(501L);
        tamilLab.setName("Tamil Lab");
        tamilLab.setCapacity(30);

        Subject englishSubject = new Subject("English", "ENG101", 2, 2, 0, null, 1);
        englishSubject.setId(1L);
        Subject tamilSubject = new Subject("Tamil", "TAM101", 2, 2, 0, null, 1);
        tamilSubject.setId(2L);

        CourseOffering englishOffering = new CourseOffering();
        englishOffering.setId(200L);
        englishOffering.setSubject(englishSubject);
        englishOffering.setTermInstance(termInstance);

        CourseOffering tamilOffering = new CourseOffering();
        tamilOffering.setId(201L);
        tamilOffering.setSubject(tamilSubject);
        tamilOffering.setTermInstance(termInstance);

        englishCell = new ClassSchedule();
        englishCell.setId(1L);
        englishCell.setSessionType(ClassSessionType.LAB);
        englishCell.setDayOfWeek(DayOfWeek.WEDNESDAY);
        englishCell.setPeriod(period);
        englishCell.setTermInstance(termInstance);
        englishCell.setCourseOffering(englishOffering);
        englishCell.setSubject(englishSubject);
        englishCell.setStatus(ClassScheduleStatus.DRAFT);

        tamilCell = new ClassSchedule();
        tamilCell.setId(2L);
        tamilCell.setSessionType(ClassSessionType.LAB);
        tamilCell.setDayOfWeek(DayOfWeek.WEDNESDAY);
        tamilCell.setPeriod(period);
        tamilCell.setTermInstance(termInstance);
        tamilCell.setCourseOffering(tamilOffering);
        tamilCell.setSubject(tamilSubject);
        tamilCell.setStatus(ClassScheduleStatus.DRAFT);

        englishBatch1 = new Batch(englishOffering, "English Batch 1", 30, termInstance);
        englishBatch1.setId(300L);
        englishBatch1.setLab(englishLab);
        englishBatch2 = new Batch(englishOffering, "English Batch 2", 30, termInstance);
        englishBatch2.setId(301L);
        englishBatch2.setLab(englishLab);

        tamilBatch1 = new Batch(tamilOffering, "Tamil Batch 1", 30, termInstance);
        tamilBatch1.setId(302L);
        tamilBatch1.setLab(tamilLab);
        tamilBatch2 = new Batch(tamilOffering, "Tamil Batch 2", 30, termInstance);
        tamilBatch2.setId(303L);
        tamilBatch2.setLab(tamilLab);
    }

    private RotationGroupCreateRequest twoByTwoRequest(LocalDate anchor) {
        return new RotationGroupCreateRequest(
            10L, "English/Tamil Wed P3-4", anchor,
            List.of(new RotationSlotInput(1L, 0), new RotationSlotInput(2L, 1)),
            List.of(
                new RotationMemberInput(0, "Batch 1", List.of(
                    new RotationAssignmentInput(1L, 300L), new RotationAssignmentInput(2L, 302L))),
                new RotationMemberInput(1, "Batch 2", List.of(
                    new RotationAssignmentInput(1L, 301L), new RotationAssignmentInput(2L, 303L)))
            )
        );
    }

    private void stubCoreLookups() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(englishCell));
        when(classScheduleRepository.findById(2L)).thenReturn(Optional.of(tamilCell));
        when(batchRepository.findById(300L)).thenReturn(Optional.of(englishBatch1));
        when(batchRepository.findById(301L)).thenReturn(Optional.of(englishBatch2));
        when(batchRepository.findById(302L)).thenReturn(Optional.of(tamilBatch1));
        when(batchRepository.findById(303L)).thenReturn(Optional.of(tamilBatch2));
        when(rotationGroupRepository.save(any(RotationGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rotationSlotRepository.save(any(RotationSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rotationMemberRepository.save(any(RotationMember.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsRotationGroupAndNullsOutTheFixedBatchOnBothCells() {
        stubCoreLookups();
        when(rotationSlotRepository.findByRotationGroupIdOrderBySlotOrderAsc(any())).thenReturn(List.of());
        when(rotationMemberRepository.findByRotationGroupIdOrderByMemberOrderAsc(any())).thenReturn(List.of());

        service.create(twoByTwoRequest(LocalDate.of(2026, 8, 5)), "admin");

        ArgumentCaptor<ClassSchedule> savedCells = ArgumentCaptor.forClass(ClassSchedule.class);
        verify(classScheduleRepository, times(2)).save(savedCells.capture());
        assertThat(savedCells.getAllValues()).allSatisfy(cs -> {
            assertThat(cs.getBatch()).isNull();
            assertThat(cs.getBatchName()).isNull();
        });

        verify(rotationMemberAssignmentRepository, times(4)).save(any());
    }

    @Test
    void rejectsWhenMemberCountDoesNotMatchSlotCount() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));

        RotationGroupCreateRequest request = new RotationGroupCreateRequest(
            10L, "Bad", LocalDate.of(2026, 8, 5),
            List.of(new RotationSlotInput(1L, 0), new RotationSlotInput(2L, 1)),
            List.of(new RotationMemberInput(0, "Batch 1", List.of(
                new RotationAssignmentInput(1L, 300L), new RotationAssignmentInput(2L, 302L)))));

        assertThatThrownBy(() -> service.create(request, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly as many physical groups as slots");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void rejectsSlotsThatDoNotShareTheSameDayAndPeriod() {
        tamilCell.setDayOfWeek(DayOfWeek.THURSDAY);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(englishCell));
        when(classScheduleRepository.findById(2L)).thenReturn(Optional.of(tamilCell));

        assertThatThrownBy(() -> service.create(twoByTwoRequest(LocalDate.of(2026, 8, 5)), "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same day and period");
    }

    @Test
    void rejectsACellThatAlreadyBelongsToAnotherRotationGroup() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(englishCell));
        when(rotationSlotRepository.existsByClassScheduleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(twoByTwoRequest(LocalDate.of(2026, 8, 5)), "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already belongs to a rotation group");
    }

    @Test
    void rejectsWhenTwoMembersAtTheSameSlotUseDifferentVenues() {
        Lab otherLab = new Lab();
        otherLab.setId(999L);
        otherLab.setName("Overflow Lab");
        englishBatch2.setLab(otherLab);
        stubCoreLookups();

        assertThatThrownBy(() -> service.create(twoByTwoRequest(LocalDate.of(2026, 8, 5)), "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("must share the exact same venue");
    }

    @Test
    void rejectsAnAssignmentWhoseBatchBelongsToADifferentSubjectThanTheSlot() {
        RotationGroupCreateRequest request = new RotationGroupCreateRequest(
            10L, "Bad subject", LocalDate.of(2026, 8, 5),
            List.of(new RotationSlotInput(1L, 0), new RotationSlotInput(2L, 1)),
            List.of(
                new RotationMemberInput(0, "Batch 1", List.of(
                    new RotationAssignmentInput(1L, 302L), // Tamil batch assigned to the English slot
                    new RotationAssignmentInput(2L, 302L))),
                new RotationMemberInput(1, "Batch 2", List.of(
                    new RotationAssignmentInput(1L, 301L), new RotationAssignmentInput(2L, 303L)))
            )
        );
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(englishCell));
        when(classScheduleRepository.findById(2L)).thenReturn(Optional.of(tamilCell));
        when(batchRepository.findById(302L)).thenReturn(Optional.of(tamilBatch1));
        when(rotationGroupRepository.save(any(RotationGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rotationSlotRepository.save(any(RotationSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rotationMemberRepository.save(any(RotationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.create(request, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to the same subject");
    }

    @Test
    void rejectsAnAnchorDateOnTheWrongDayOfWeek() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(englishCell));
        when(classScheduleRepository.findById(2L)).thenReturn(Optional.of(tamilCell));

        assertThatThrownBy(() -> service.create(twoByTwoRequest(LocalDate.of(2026, 8, 6)), "admin")) // a Thursday
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same day of week");
    }
}
