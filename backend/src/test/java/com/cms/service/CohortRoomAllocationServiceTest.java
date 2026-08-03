package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.cms.dto.CohortRoomAllocationCommitRequest;
import com.cms.dto.CohortRoomAllocationResponse;
import com.cms.dto.VentureSplitRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CourseOffering;
import com.cms.model.Lab;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class CohortRoomAllocationServiceTest {

    @Mock private CohortRoomAllocationRepository allocationRepository;
    @Mock private CohortRepository cohortRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    private CohortRoomAllocationService service;

    private Cohort cohort;
    private TermInstance term;
    private Classroom theoryClassroom;
    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        service = new CohortRoomAllocationService(allocationRepository, cohortRepository, termInstanceRepository,
            classroomRepository, labRepository, clinicalVenueRepository, courseOfferingRepository, batchRepository,
            studentTermEnrollmentRepository);

        cohort = new Cohort();
        cohort.setId(1L);
        cohort.setDisplayName("BSc Nursing 2026");

        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setName("2026-27");

        term = new TermInstance();
        term.setId(1L);
        term.setAcademicYear(year);
        term.setTermType(TermType.ODD);

        theoryClassroom = new Classroom("Room 123", null, null, 60);
        theoryClassroom.setId(10L);

        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Anatomy");

        offering = new CourseOffering();
        offering.setId(5L);
        offering.setTermInstance(term);
        offering.setSubject(subject);
    }

    @Test
    void shouldRejectCommitWhenTheoryClassroomTooSmall() {
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(term));
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(1L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(65L);

        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(1L, 1L, 10L, List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("seats 60");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectCommitWhenVentureSplitExceedsVenueCapacity() {
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(term));
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(1L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(60L);

        Lab lab = new Lab();
        lab.setId(20L);
        lab.setName("Computer Lab");
        lab.setCapacity(40);
        when(labRepository.findById(20L)).thenReturn(Optional.of(lab));

        VentureSplitRequest split = new VentureSplitRequest(5L, ClassSessionType.LAB, 20L, "Batch A", 45);
        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(1L, 1L, 10L, List.of(split));

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("only seats 40");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldAcceptNonUniformSplitThatFitsEachVenue() {
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(term));
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(1L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(60L);

        Lab labA = new Lab();
        labA.setId(20L);
        labA.setName("Lab A");
        labA.setCapacity(40);
        Lab labB = new Lab();
        labB.setId(21L);
        labB.setName("Lab B");
        labB.setCapacity(40);
        when(labRepository.findById(20L)).thenReturn(Optional.of(labA));
        when(labRepository.findById(21L)).thenReturn(Optional.of(labB));
        when(labRepository.getReferenceById(20L)).thenReturn(labA);
        when(labRepository.getReferenceById(21L)).thenReturn(labB);
        when(courseOfferingRepository.findById(5L)).thenReturn(Optional.of(offering));

        when(allocationRepository.save(any(CohortRoomAllocation.class))).thenAnswer(inv -> {
            CohortRoomAllocation a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId((long) (Math.random() * 1000));
            return b;
        });
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());

        // 30/30 instead of the ceil-even 40/20 -- admin's edited, non-uniform split.
        VentureSplitRequest splitA = new VentureSplitRequest(5L, ClassSessionType.LAB, 20L, "Batch A", 30);
        VentureSplitRequest splitB = new VentureSplitRequest(5L, ClassSessionType.LAB, 21L, "Batch B", 30);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, 10L, List.of(splitA, splitB));

        CohortRoomAllocationResponse response = service.commit(request, "admin");

        assertThat(response.theoryClassroomId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(CohortRoomAllocationStatus.COMMITTED);
        verify(batchRepository, org.mockito.Mockito.times(2)).save(any(Batch.class));
    }

    @Test
    void shouldRejectCommitWhenTheoryClassroomAlreadyClaimedThisTerm() {
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(term));
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(1L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(60L);
        when(allocationRepository.save(any(CohortRoomAllocation.class)))
            .thenThrow(new DataIntegrityViolationException("ux_theory_classroom_per_term"));

        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(1L, 1L, 10L, List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already");

        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateBatchesOnRevert() {
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, theoryClassroom, "admin");
        allocation.setId(100L);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(any(CohortRoomAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

        Batch batch1 = new Batch(offering, "Batch A", 30, term);
        batch1.setId(200L);
        Batch batch2 = new Batch(offering, "Batch B", 30, term);
        batch2.setId(201L);
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of(batch1, batch2));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        service.revert(100L, "admin");

        assertThat(allocation.getStatus()).isEqualTo(CohortRoomAllocationStatus.REVERTED);
        assertThat(batch1.getIsActive()).isFalse();
        assertThat(batch2.getIsActive()).isFalse();
    }

    @Test
    void shouldRejectRevertingAlreadyRevertedAllocation() {
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, theoryClassroom, "admin");
        allocation.setId(100L);
        allocation.setStatus(CohortRoomAllocationStatus.REVERTED);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> service.revert(100L, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already reverted");

        verify(batchRepository, never()).save(any());
    }
}
