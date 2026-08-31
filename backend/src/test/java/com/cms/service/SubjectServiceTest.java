package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.FacultyOptionResponse;
import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.dto.VenueOptionResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClinicalVenue;
import com.cms.model.Lab;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SpecialityRepository specialityRepository;

    @Mock
    private CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private LabRepository labRepository;

    @Mock
    private ClinicalVenueRepository clinicalVenueRepository;

    @Mock
    private FacultyRepository facultyRepository;

    private SubjectService subjectService;

    private Speciality speciality;
    private Subject testSubject;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        subjectService = new SubjectService(subjectRepository, courseRepository, specialityRepository,
            curriculumSemesterCourseRepository, courseOfferingRepository, classScheduleRepository, batchRepository,
            labRepository, clinicalVenueRepository, facultyRepository);

        speciality = new Speciality();
        speciality.setId(1L);
        speciality.setName("Medical-Surgical Nursing");
        speciality.setCode("MSN");
        speciality.setCreatedAt(now);
        speciality.setUpdatedAt(now);

        testSubject = new Subject("Anatomy", "ANAT101", 4, 3, 1, speciality, 1);
        testSubject.setId(1L);
        testSubject.setCreatedAt(now);
        testSubject.setUpdatedAt(now);
    }

    @Test
    void shouldCreateSubject() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 1L, 1, null, null, null, null, null, null);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        SubjectResponse response = subjectService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Anatomy");
        assertThat(response.code()).isEqualTo("ANAT101");
        assertThat(response.credits()).isEqualTo(4);
        assertThat(response.theoryCredits()).isEqualTo(3);
        assertThat(response.labCredits()).isEqualTo(1);
        assertThat(response.termNumber()).isEqualTo(1);
        assertThat(response.speciality().id()).isEqualTo(1L);

        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Anatomy");
    }

    @Test
    void shouldRoundTripEligibleLabsAndClinicalVenuesThroughCreateAndResponse() {
        SubjectRequest request = new SubjectRequest("OBG Nursing", "OBG101", 4, 3, 1, 1L, 1, null, null, null,
            List.of(11L), List.of(21L), List.of(31L));

        Lab obgLab = new Lab();
        obgLab.setId(11L);
        obgLab.setName("OBG Lab");
        obgLab.setCapacity(30);
        ClinicalVenue obgWard = new ClinicalVenue();
        obgWard.setId(21L);
        obgWard.setName("OBG Ward");
        obgWard.setCapacity(20);
        com.cms.model.Faculty widenedFaculty = new com.cms.model.Faculty();
        widenedFaculty.setId(31L);
        widenedFaculty.setFirstName("Nandini");
        widenedFaculty.setLastName("Pillai");

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
        when(labRepository.findAllById(List.of(11L))).thenReturn(List.of(obgLab));
        when(clinicalVenueRepository.findAllById(List.of(21L))).thenReturn(List.of(obgWard));
        when(facultyRepository.findAllById(List.of(31L))).thenReturn(List.of(widenedFaculty));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(10L);
            s.setCreatedAt(now);
            s.setUpdatedAt(now);
            return s;
        });

        SubjectResponse response = subjectService.create(request);

        assertThat(response.eligibleLabs()).extracting(VenueOptionResponse::id).containsExactly(11L);
        assertThat(response.eligibleLabs()).extracting(VenueOptionResponse::name).containsExactly("OBG Lab");
        assertThat(response.eligibleClinicalVenues()).extracting(VenueOptionResponse::id).containsExactly(21L);
        assertThat(response.eligibleFaculty()).extracting(FacultyOptionResponse::id).containsExactly(31L);
        assertThat(response.eligibleFaculty()).extracting(FacultyOptionResponse::fullName).containsExactly("Nandini Pillai");

        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository).save(captor.capture());
        assertThat(captor.getValue().getEligibleLabs()).extracting(Lab::getId).containsExactly(11L);
        assertThat(captor.getValue().getEligibleClinicalVenues()).extracting(ClinicalVenue::getId).containsExactly(21L);
        assertThat(captor.getValue().getEligibleFaculty()).extracting(com.cms.model.Faculty::getId).containsExactly(31L);
    }

    @Test
    void shouldClearEligibleLabsWhenRequestOmitsThem() {
        testSubject.setEligibleLabs(new java.util.HashSet<>(List.of(new Lab())));
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 1L, 1, null, null, null, null, null, null);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
        when(subjectRepository.existsByNameIgnoreCaseAndIdNot("Anatomy", 1L)).thenReturn(false);
        when(subjectRepository.existsByCodeIgnoreCaseAndIdNot("ANAT101", 1L)).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.update(1L, request);

        assertThat(response.eligibleLabs()).isEmpty();
    }

    @Test
    void shouldCreateSubjectWithoutSpeciality() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, null, 1, null, null, null, null, null, null);
        Subject subjectNoDept = new Subject("Anatomy", "ANAT101", 4, 3, 1, null, 1);
        subjectNoDept.setId(2L);
        subjectNoDept.setCreatedAt(now);
        subjectNoDept.setUpdatedAt(now);

        when(subjectRepository.save(any(Subject.class))).thenReturn(subjectNoDept);

        SubjectResponse response = subjectService.create(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.speciality()).isNull();
        verify(specialityRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenSpecialityNotFoundOnCreate() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 999L, 1, null, null, null, null, null, null);
        when(specialityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");
    }

    @Test
    void shouldFindAllSubjects() {
        when(subjectRepository.findAllByOrderByNameAsc()).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findAll(false);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Anatomy");
        verify(subjectRepository).findAllByOrderByNameAsc();
    }

    @Test
    void shouldFindActiveSubjectsOnly() {
        when(subjectRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findAll(true);

        assertThat(results).hasSize(1);
        verify(subjectRepository).findByIsActiveTrueOrderByNameAsc();
        verify(subjectRepository, never()).findAllByOrderByNameAsc();
    }

    @Test
    void shouldFindSubjectById() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

        SubjectResponse response = subjectService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Anatomy");
    }

    @Test
    void shouldThrowWhenSubjectNotFoundById() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldFindSubjectsUsedInCourseCurricula() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(curriculumSemesterCourseRepository.findDistinctSubjectIdsByCourseId(1L)).thenReturn(List.of(1L));
        when(subjectRepository.findAllById(List.of(1L))).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findByCourseId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Anatomy");
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnFindByCourseId() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.findByCourseId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldFindSubjectsBySpecialityId() {
        when(specialityRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.findBySpecialityId(1L)).thenReturn(List.of(testSubject));

        List<SubjectResponse> results = subjectService.findBySpecialityId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Anatomy");
    }

    @Test
    void shouldThrowWhenSpecialityNotFoundOnFindBySpecialityId() {
        when(specialityRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.findBySpecialityId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");
    }

    @Test
    void shouldUpdateSubject() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 1L, 2, null, null, null, null, null, null);

        Subject updatedSubject = new Subject("Physiology", "PHYS101", 5, 4, 1, speciality, 2);
        updatedSubject.setId(1L);
        updatedSubject.setCreatedAt(now);
        updatedSubject.setUpdatedAt(now);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
        when(subjectRepository.existsByNameIgnoreCaseAndIdNot("Physiology", 1L)).thenReturn(false);
        when(subjectRepository.existsByCodeIgnoreCaseAndIdNot("PHYS101", 1L)).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(updatedSubject);

        SubjectResponse response = subjectService.update(1L, request);

        assertThat(response.name()).isEqualTo("Physiology");
        assertThat(response.code()).isEqualTo("PHYS101");
        assertThat(response.credits()).isEqualTo(5);
        assertThat(response.termNumber()).isEqualTo(2);
    }

    @Test
    void shouldUpdateSubjectWithoutSpeciality() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, null, 2, null, null, null, null, null, null);

        Subject updatedSubject = new Subject("Physiology", "PHYS101", 5, 4, 1, null, 2);
        updatedSubject.setId(1L);
        updatedSubject.setCreatedAt(now);
        updatedSubject.setUpdatedAt(now);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.existsByNameIgnoreCaseAndIdNot("Physiology", 1L)).thenReturn(false);
        when(subjectRepository.existsByCodeIgnoreCaseAndIdNot("PHYS101", 1L)).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(updatedSubject);

        SubjectResponse response = subjectService.update(1L, request);

        assertThat(response.speciality()).isNull();
        verify(specialityRepository, never()).findById(any());
    }

    @Test
    void shouldThrowWhenUpdatingSubjectWithDuplicateName() {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 5, 4, 1, null, 1, null, null, null, null, null, null);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.existsByNameIgnoreCaseAndIdNot("Anatomy", 1L)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Anatomy")
            .hasMessageContaining("already exists");

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void shouldThrowWhenUpdatingSubjectWithDuplicateCode() {
        SubjectRequest request = new SubjectRequest("Physiology", "ANAT101", 5, 4, 1, null, 1, null, null, null, null, null, null);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.existsByNameIgnoreCaseAndIdNot("Physiology", 1L)).thenReturn(false);
        when(subjectRepository.existsByCodeIgnoreCaseAndIdNot("ANAT101", 1L)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ANAT101")
            .hasMessageContaining("already exists");

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void shouldThrowWhenSubjectNotFoundOnUpdate() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, null, 2, null, null, null, null, null, null);
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");
    }

    @Test
    void shouldThrowWhenSpecialityNotFoundOnUpdate() {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 999L, 2, null, null, null, null, null, null);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(specialityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");
    }

    @Test
    void shouldDeleteSubject() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(curriculumSemesterCourseRepository.existsBySubjectId(1L)).thenReturn(false);
        when(courseOfferingRepository.existsBySubjectId(1L)).thenReturn(false);

        subjectService.delete(1L);

        verify(subjectRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenSubjectNotFoundOnDelete() {
        when(subjectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Subject not found with id: 999");

        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDeletingSubjectMappedIntoCurriculum() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(curriculumSemesterCourseRepository.existsBySubjectId(1L)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("curriculum");

        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDeletingSubjectWithCourseOfferings() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(curriculumSemesterCourseRepository.existsBySubjectId(1L)).thenReturn(false);
        when(courseOfferingRepository.existsBySubjectId(1L)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("course offerings");

        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void updateStatus_deactivatesWhenNothingIsAttached() {
        testSubject.setIsActive(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(classScheduleRepository.existsByCourseOffering_Subject_Id(1L)).thenReturn(false);
        when(batchRepository.existsAnyStudentInBatchesForSubject(1L)).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        ActiveStatusUpdateResponse response = subjectService.updateStatus(1L, new ActiveStatusUpdateRequest(false, null));

        assertThat(response.isActive()).isFalse();
        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    void updateStatus_blocksDeactivationWhenSessionsArePlaced() {
        testSubject.setIsActive(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(classScheduleRepository.existsByCourseOffering_Subject_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.updateStatus(1L, new ActiveStatusUpdateRequest(false, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Skeleton Builder");

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void updateStatus_blocksDeactivationWhenBatchesHaveStudents() {
        testSubject.setIsActive(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(classScheduleRepository.existsByCourseOffering_Subject_Id(1L)).thenReturn(false);
        when(batchRepository.existsAnyStudentInBatchesForSubject(1L)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.updateStatus(1L, new ActiveStatusUpdateRequest(false, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rostered");

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void updateStatus_reactivatesWithNoGuardEvenWhenSessionsArePlaced() {
        testSubject.setIsActive(false);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        ActiveStatusUpdateResponse response = subjectService.updateStatus(1L, new ActiveStatusUpdateRequest(true, null));

        assertThat(response.isActive()).isTrue();
        verify(classScheduleRepository, never()).existsByCourseOffering_Subject_Id(any());
        verify(batchRepository, never()).existsAnyStudentInBatchesForSubject(any());
    }

    @Test
    void addEligibleVenue_lab_addsToEverySubjectWithoutTouchingOtherFields() {
        Lab lab = new Lab();
        lab.setId(50L);
        lab.setName("Anatomy Lab");
        Subject other = new Subject("Physiology", "PHY101", 4, 3, 1, speciality, 1);
        other.setId(2L);
        when(labRepository.findById(50L)).thenReturn(Optional.of(lab));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(other));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        subjectService.addEligibleVenue(new com.cms.dto.AddEligibleVenueRequest(List.of(1L, 2L), "LAB", 50L));

        assertThat(testSubject.getEligibleLabs()).contains(lab);
        assertThat(other.getEligibleLabs()).contains(lab);
        assertThat(testSubject.getName()).isEqualTo("Anatomy");
    }

    @Test
    void addEligibleVenue_clinical_addsToSubject() {
        ClinicalVenue venue = new ClinicalVenue();
        venue.setId(60L);
        venue.setName("Community Health Center II");
        when(clinicalVenueRepository.findById(60L)).thenReturn(Optional.of(venue));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        subjectService.addEligibleVenue(new com.cms.dto.AddEligibleVenueRequest(List.of(1L), "CLINICAL", 60L));

        assertThat(testSubject.getEligibleClinicalVenues()).contains(venue);
    }

    @Test
    void addEligibleVenue_alreadyEligible_isIdempotentNoOp() {
        ClinicalVenue venue = new ClinicalVenue();
        venue.setId(60L);
        testSubject.setEligibleClinicalVenues(new java.util.HashSet<>(List.of(venue)));
        when(clinicalVenueRepository.findById(60L)).thenReturn(Optional.of(venue));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        subjectService.addEligibleVenue(new com.cms.dto.AddEligibleVenueRequest(List.of(1L), "CLINICAL", 60L));

        assertThat(testSubject.getEligibleClinicalVenues()).hasSize(1);
    }

    @Test
    void addEligibleVenue_unknownVenueType_throws() {
        assertThatThrownBy(() -> subjectService.addEligibleVenue(
            new com.cms.dto.AddEligibleVenueRequest(List.of(1L), "EQUIPMENT", 60L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown venue type");
    }

    @Test
    void addEligibleVenue_subjectNotFound_throws() {
        ClinicalVenue venue = new ClinicalVenue();
        venue.setId(60L);
        when(clinicalVenueRepository.findById(60L)).thenReturn(Optional.of(venue));
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.addEligibleVenue(
            new com.cms.dto.AddEligibleVenueRequest(List.of(99L), "CLINICAL", 60L)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
