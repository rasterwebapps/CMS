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
import com.cms.dto.CohortSectionRequest;
import com.cms.dto.VentureSplitRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.AcademicYear;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.CourseOffering;
import com.cms.model.CourseOfferingSectionFaculty;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseOfferingSectionFacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class CohortRoomAllocationServiceTest {

    @Mock private CohortRoomAllocationRepository allocationRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private CohortRepository cohortRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private LabRepository labRepository;
    @Mock private ClinicalVenueRepository clinicalVenueRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private CourseOfferingSectionFacultyRepository courseOfferingSectionFacultyRepository;

    private CohortRoomAllocationService service;

    private Cohort cohort;
    private TermInstance term;
    private Classroom theoryClassroom;
    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        service = new CohortRoomAllocationService(allocationRepository, cohortSectionRepository, cohortRepository,
            termInstanceRepository, classroomRepository, labRepository, clinicalVenueRepository,
            courseOfferingRepository, batchRepository, studentTermEnrollmentRepository, classScheduleRepository,
            courseOfferingSectionFacultyRepository);

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

    private void stubCohortAndTerm(long strength) {
        when(cohortRepository.findByIdWithCourse(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(term));
        when(studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(1L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(strength);
    }

    @Test
    void shouldRejectCommitWhenSectionPlannedSizeExceedsClassroomCapacity() {
        stubCohortAndTerm(65);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));

        CohortSectionRequest section = new CohortSectionRequest("Section 1", 10L, 65);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("seats 60");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectCommitWhenClassroomAllowsConcurrentSharing() {
        // A large lecture/drawing hall flagged shareable can't be committed as one cohort's
        // exclusive Theory section -- that would defeat the point of flagging it shareable (see
        // SpecialClassRequestService for the actual concurrent-sharing behavior this room is for).
        stubCohortAndTerm(60);
        Classroom sharedHall = new Classroom("Drawing Hall", null, null, 120);
        sharedHall.setId(12L);
        sharedHall.setAllowsConcurrentSharing(true);
        when(classroomRepository.findById(12L)).thenReturn(Optional.of(sharedHall));

        CohortSectionRequest section = new CohortSectionRequest("Section 1", 12L, 60);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("allows concurrent sharing");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectCommitWhenSectionsUnderCoverCohortStrength() {
        stubCohortAndTerm(100);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));

        CohortSectionRequest section = new CohortSectionRequest("Section 1", 10L, 60);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("cover only 60 of 100");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectCommitWhenSectionsOverCoverCohortStrength() {
        // Reported bug: sanctioned intake 100, two 60-cap rooms, admin fills both to full
        // capacity (60+60=120) -- must be rejected, not silently accepted as a "buffer".
        stubCohortAndTerm(100);
        Classroom classroomA = theoryClassroom;
        Classroom classroomB = new Classroom("Room 456", null, null, 60);
        classroomB.setId(11L);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(classroomA));
        when(classroomRepository.findById(11L)).thenReturn(Optional.of(classroomB));

        CohortSectionRequest sectionA = new CohortSectionRequest("Section 1", 10L, 60);
        CohortSectionRequest sectionB = new CohortSectionRequest("Section 2", 11L, 60);
        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(
            1L, 1L, PlanningBasis.ENROLLED, List.of(sectionA, sectionB), List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("120 students, 20 more than the 100");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectCommitWhenVentureSplitExceedsVenueCapacity() {
        stubCohortAndTerm(60);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));

        Lab lab = new Lab();
        lab.setId(20L);
        lab.setName("Computer Lab");
        lab.setCapacity(40);
        when(labRepository.findById(20L)).thenReturn(Optional.of(lab));

        CohortSectionRequest section = new CohortSectionRequest("Section 1", 10L, 60);
        VentureSplitRequest split = new VentureSplitRequest(5L, ClassSessionType.LAB, 20L, "Batch A", 45, null);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of(split));

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("only seats 40");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldAcceptNonUniformSplitThatFitsEachVenue() {
        stubCohortAndTerm(60);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));

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
        when(cohortSectionRepository.save(any(CohortSection.class))).thenAnswer(inv -> {
            CohortSection s = inv.getArgument(0);
            s.setId(500L);
            return s;
        });
        when(cohortSectionRepository.findByCohortRoomAllocationId(100L)).thenAnswer(inv -> {
            CohortSection s = new CohortSection(null, term, "Section 1", theoryClassroom, 60);
            s.setId(500L);
            return List.of(s);
        });
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId((long) (Math.random() * 1000));
            return b;
        });
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());

        // 30/30 instead of the ceil-even 40/20 -- admin's edited, non-uniform split.
        CohortSectionRequest section = new CohortSectionRequest("Section 1", 10L, 60);
        VentureSplitRequest splitA = new VentureSplitRequest(5L, ClassSessionType.LAB, 20L, "Batch A", 30, "Section 1");
        VentureSplitRequest splitB = new VentureSplitRequest(5L, ClassSessionType.LAB, 21L, "Batch B", 30, "Section 1");
        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(
            1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of(splitA, splitB));

        CohortRoomAllocationResponse response = service.commit(request, "admin");

        assertThat(response.sections()).hasSize(1);
        assertThat(response.sections().get(0).classroomId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(CohortRoomAllocationStatus.COMMITTED);
        verify(batchRepository, org.mockito.Mockito.times(2)).save(any(Batch.class));
    }

    @Test
    void shouldRejectVentureSplitWithoutSectionLabelWhenMultipleSections() {
        stubCohortAndTerm(100);
        Classroom classroomA = theoryClassroom;
        Classroom classroomB = new Classroom("Room 456", null, null, 40);
        classroomB.setId(11L);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(classroomA));
        when(classroomRepository.findById(11L)).thenReturn(Optional.of(classroomB));

        Lab lab = new Lab();
        lab.setId(20L);
        lab.setCapacity(60);
        when(labRepository.findById(20L)).thenReturn(Optional.of(lab));

        CohortSectionRequest sectionA = new CohortSectionRequest("Section 1", 10L, 60);
        CohortSectionRequest sectionB = new CohortSectionRequest("Section 2", 11L, 40);
        VentureSplitRequest split = new VentureSplitRequest(5L, ClassSessionType.LAB, 20L, "Batch A", 30, null);
        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(
            1L, 1L, PlanningBasis.ENROLLED, List.of(sectionA, sectionB), List.of(split));

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("must specify which section");

        verify(allocationRepository, never()).save(any());
    }

    @Test
    void shouldAcceptMultiSectionCommitWithPerSectionVentureSplits() {
        stubCohortAndTerm(100);
        Classroom classroomA = theoryClassroom;
        Classroom classroomB = new Classroom("Room 456", null, null, 40);
        classroomB.setId(11L);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(classroomA));
        when(classroomRepository.findById(11L)).thenReturn(Optional.of(classroomB));

        Lab labA = new Lab();
        labA.setId(20L);
        labA.setCapacity(60);
        Lab labB = new Lab();
        labB.setId(21L);
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
        when(cohortSectionRepository.save(any(CohortSection.class))).thenAnswer(inv -> {
            CohortSection s = inv.getArgument(0);
            s.setId((long) (Math.random() * 1000));
            return s;
        });
        when(cohortSectionRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId((long) (Math.random() * 1000));
            return b;
        });
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());

        CohortSectionRequest sectionA = new CohortSectionRequest("Section 1", 10L, 60);
        CohortSectionRequest sectionB = new CohortSectionRequest("Section 2", 11L, 40);
        VentureSplitRequest splitA = new VentureSplitRequest(5L, ClassSessionType.LAB, 20L, "Batch A", 60, "Section 1");
        VentureSplitRequest splitB = new VentureSplitRequest(5L, ClassSessionType.LAB, 21L, "Batch B", 40, "Section 2");
        CohortRoomAllocationCommitRequest request = new CohortRoomAllocationCommitRequest(
            1L, 1L, PlanningBasis.ENROLLED, List.of(sectionA, sectionB), List.of(splitA, splitB));

        CohortRoomAllocationResponse response = service.commit(request, "admin");

        assertThat(response.status()).isEqualTo(CohortRoomAllocationStatus.COMMITTED);
        verify(cohortSectionRepository, org.mockito.Mockito.times(2)).save(any(CohortSection.class));
        verify(batchRepository, org.mockito.Mockito.times(2)).save(any(Batch.class));
    }

    @Test
    void shouldCarryForwardOrphanedFacultyAssignmentOntoFreshlyCommittedSection() {
        stubCohortAndTerm(60);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));
        when(allocationRepository.save(any(CohortRoomAllocation.class))).thenAnswer(inv -> {
            CohortRoomAllocation a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });
        when(cohortSectionRepository.save(any(CohortSection.class))).thenAnswer(inv -> {
            CohortSection s = inv.getArgument(0);
            s.setId(400L);
            return s;
        });
        when(cohortSectionRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());

        Faculty faculty = new Faculty();
        faculty.setId(9L);
        CohortSection oldSection = new CohortSection();
        oldSection.setId(300L);
        oldSection.setSectionLabel("Section 1");
        oldSection.setIsActive(false);
        CourseOfferingSectionFaculty orphaned = new CourseOfferingSectionFaculty();
        orphaned.setCourseOffering(offering);
        orphaned.setCohortSection(oldSection);
        orphaned.setCohort(cohort);
        orphaned.setFaculty(faculty);
        when(courseOfferingSectionFacultyRepository.findByCohort_IdAndCohortSection_IsActiveFalseAndCohortSection_SectionLabel(1L, "Section 1"))
            .thenReturn(List.of(orphaned));

        CohortSectionRequest section = new CohortSectionRequest("Section 1", 10L, 60);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of());

        service.commit(request, "admin");

        assertThat(orphaned.getCohortSection().getId()).isEqualTo(400L);
        verify(courseOfferingSectionFacultyRepository).save(orphaned);
    }

    @Test
    void shouldRejectCommitWhenTheoryClassroomAlreadyClaimedThisTerm() {
        stubCohortAndTerm(60);
        when(classroomRepository.findById(10L)).thenReturn(Optional.of(theoryClassroom));
        when(allocationRepository.save(any(CohortRoomAllocation.class)))
            .thenThrow(new DataIntegrityViolationException("ux_cohort_room_alloc_active"));

        CohortSectionRequest section = new CohortSectionRequest("Section 1", 10L, 60);
        CohortRoomAllocationCommitRequest request =
            new CohortRoomAllocationCommitRequest(1L, 1L, PlanningBasis.ENROLLED, List.of(section), List.of());

        assertThatThrownBy(() -> service.commit(request, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already");

        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateSectionsAndBatchesOnRevert() {
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, PlanningBasis.ENROLLED, 60, "admin");
        allocation.setId(100L);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(any(CohortRoomAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

        Batch batch1 = new Batch(offering, "Batch A", 30, term);
        batch1.setId(200L);
        Batch batch2 = new Batch(offering, "Batch B", 30, term);
        batch2.setId(201L);
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of(batch1, batch2));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        CohortSection section = new CohortSection(allocation, term, "Section 1", theoryClassroom, 60);
        section.setId(300L);
        when(cohortSectionRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of(section));
        when(cohortSectionRepository.save(any(CohortSection.class))).thenAnswer(inv -> inv.getArgument(0));

        when(classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(200L, 201L))).thenReturn(List.of());
        when(classScheduleRepository.findByCohortSectionIdInAndIsActiveTrue(List.of(300L))).thenReturn(List.of());

        service.revert(100L, "admin");

        assertThat(allocation.getStatus()).isEqualTo(CohortRoomAllocationStatus.REVERTED);
        assertThat(batch1.getIsActive()).isFalse();
        assertThat(batch2.getIsActive()).isFalse();
        assertThat(section.getIsActive()).isFalse();
    }

    @Test
    void shouldDeactivateRidingDraftClassSchedulesOnRevert() {
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, PlanningBasis.ENROLLED, 60, "admin");
        allocation.setId(100L);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(any(CohortRoomAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

        Batch batch = new Batch(offering, "Batch A", 30, term);
        batch.setId(200L);
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cohortSectionRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());

        ClassSchedule labCell = new ClassSchedule();
        labCell.setId(9000L);
        labCell.setStatus(ClassScheduleStatus.DRAFT);
        labCell.setIsActive(true);
        when(classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(200L))).thenReturn(List.of(labCell));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.revert(100L, "admin");

        assertThat(batch.getIsActive()).isFalse();
        assertThat(labCell.getIsActive()).isFalse();
        verify(classScheduleRepository).save(labCell);
    }

    @Test
    void shouldRejectRevertWhenPublishedSessionsRideOnAllocation() {
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, PlanningBasis.ENROLLED, 60, "admin");
        allocation.setId(100L);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));

        Batch batch = new Batch(offering, "Batch A", 30, term);
        batch.setId(200L);
        when(batchRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of(batch));
        when(cohortSectionRepository.findByCohortRoomAllocationId(100L)).thenReturn(List.of());

        ClassSchedule publishedCell = new ClassSchedule();
        publishedCell.setId(9001L);
        publishedCell.setStatus(ClassScheduleStatus.PUBLISHED);
        publishedCell.setIsActive(true);
        when(classScheduleRepository.findByBatchIdInAndIsActiveTrue(List.of(200L))).thenReturn(List.of(publishedCell));

        assertThatThrownBy(() -> service.revert(100L, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("published");

        assertThat(allocation.getStatus()).isNotEqualTo(CohortRoomAllocationStatus.REVERTED);
        verify(batchRepository, never()).save(any());
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldRejectRevertingAlreadyRevertedAllocation() {
        CohortRoomAllocation allocation = new CohortRoomAllocation(cohort, term, PlanningBasis.ENROLLED, 60, "admin");
        allocation.setId(100L);
        allocation.setStatus(CohortRoomAllocationStatus.REVERTED);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> service.revert(100L, "admin"))
            .isInstanceOf(LifecycleConflictException.class)
            .hasMessageContaining("already reverted");

        verify(batchRepository, never()).save(any());
    }
}
