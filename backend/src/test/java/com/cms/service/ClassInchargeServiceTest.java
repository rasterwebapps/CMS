package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ClassInchargeAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Classroom;
import com.cms.model.Cohort;
import com.cms.model.CohortRoomAllocation;
import com.cms.model.CohortSection;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Speciality;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class ClassInchargeServiceTest {

    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private FacultyRepository facultyRepository;

    private ClassInchargeService service;
    private Faculty faculty;

    @BeforeEach
    void setUp() {
        service = new ClassInchargeService(cohortSectionRepository, termInstanceRepository, facultyRepository);

        Speciality speciality = new Speciality("General Nursing", "GN", "dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Professor", "PROFESSOR", null);
        designation.setId(1L);
        faculty = new Faculty("EMP001", "Divya", "Krishnan", "divya@test.com", "9999999999",
            speciality, designation, "Medical-Surgical", "Skills Lab", null, FacultyStatus.ACTIVE);
        faculty.setId(1L);
    }

    private CohortSection section(Long id, String cohortName, String sectionLabel, String classroomName) {
        Cohort cohort = new Cohort();
        cohort.setDisplayName(cohortName);
        CohortRoomAllocation allocation = new CohortRoomAllocation();
        allocation.setCohort(cohort);
        Classroom classroom = new Classroom(classroomName, "Block A", "101", 60);

        CohortSection section = new CohortSection(allocation, null, sectionLabel, classroom, 60);
        section.setId(id);
        return section;
    }

    @Test
    void getForTermInstance_throwsWhenTermInstanceNotFound() {
        when(termInstanceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getForTermInstance(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getForTermInstance_sortsByCohortNameThenSectionLabel() {
        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        CohortSection sectionZebraB = section(1L, "Zebra Cohort", "B", "Room 1");
        CohortSection sectionZebraA = section(2L, "Zebra Cohort", "A", "Room 2");
        CohortSection sectionAlpha = section(3L, "Alpha Cohort", "A", "Room 3");
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(1L))
            .thenReturn(List.of(sectionZebraB, sectionZebraA, sectionAlpha));

        List<ClassInchargeAssignment> result = service.getForTermInstance(1L);

        assertThat(result).extracting(ClassInchargeAssignment::cohortName, ClassInchargeAssignment::sectionLabel)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Alpha Cohort", "A"),
                org.assertj.core.groups.Tuple.tuple("Zebra Cohort", "A"),
                org.assertj.core.groups.Tuple.tuple("Zebra Cohort", "B"));
    }

    @Test
    void getForTermInstance_reportsUnassignedIncharge() {
        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        when(cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(1L))
            .thenReturn(List.of(section(1L, "Cohort A", "A", "Room 1")));

        List<ClassInchargeAssignment> result = service.getForTermInstance(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).facultyId()).isNull();
        assertThat(result.get(0).facultyName()).isNull();
    }

    @Test
    void upsert_throwsWhenSectionNotFound() {
        when(cohortSectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(99L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void upsert_throwsWhenFacultyNotFound() {
        CohortSection section = section(1L, "Cohort A", "A", "Room 1");
        when(cohortSectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(facultyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(1L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void upsert_assignsFacultyAsIncharge() {
        CohortSection section = section(1L, "Cohort A", "A", "Room 1");
        when(cohortSectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));
        when(cohortSectionRepository.save(any(CohortSection.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassInchargeAssignment result = service.upsert(1L, 1L);

        assertThat(result.facultyId()).isEqualTo(1L);
        assertThat(result.facultyName()).isEqualTo(faculty.getFullName());
        assertThat(section.getClassInchargeFaculty()).isEqualTo(faculty);
    }

    @Test
    void upsert_clearsInchargeWhenFacultyIdNull() {
        CohortSection section = section(1L, "Cohort A", "A", "Room 1");
        section.setClassInchargeFaculty(faculty);
        when(cohortSectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(cohortSectionRepository.save(any(CohortSection.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassInchargeAssignment result = service.upsert(1L, null);

        assertThat(result.facultyId()).isNull();
        assertThat(section.getClassInchargeFaculty()).isNull();
    }
}
