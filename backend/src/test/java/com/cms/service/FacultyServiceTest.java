package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocument;
import com.cms.model.Speciality;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.DesignationRepository;
import com.cms.repository.FacultyDocumentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SpecialityRepository;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {

    @Mock private FacultyRepository facultyRepository;
    @Mock private SpecialityRepository specialityRepository;
    @Mock private DesignationRepository designationRepository;
    @Mock private FacultyDocumentRepository facultyDocumentRepository;
    @Mock private FacultyDocumentTypeRequirementService requirementService;

    private FacultyService facultyService;

    private Speciality testSpeciality;
    private DesignationMaster professor;
    private DesignationMaster assistantProfessor;
    private DesignationMaster associateProfessor;

    @BeforeEach
    void setUp() {
        facultyService = new FacultyService(
            facultyRepository,
            specialityRepository,
            designationRepository,
            facultyDocumentRepository,
            requirementService
        );
        testSpeciality = createSpeciality(1L, "Computer Science", "CS");
        professor          = createDesignation(1L, "Professor",           "PROFESSOR");
        assistantProfessor = createDesignation(2L, "Assistant Professor", "ASSISTANT_PROFESSOR");
        associateProfessor = createDesignation(3L, "Associate Professor", "ASSOCIATE_PROFESSOR");
    }

    @Test
    void shouldCreateFaculty() {
        FacultyRequest request = basicFacultyRequest(
            "EMP001", "John", "Doe", "john.doe@college.edu", "1234567890",
            1L, 1L, "Artificial Intelligence", "Machine Learning Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ACTIVE);

        Faculty savedFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john.doe@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(testSpeciality));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(savedFaculty);

        FacultyResponse response = facultyService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john.doe@college.edu");
        assertThat(response.specialityId()).isEqualTo(1L);
        assertThat(response.specialityName()).isEqualTo("Computer Science");
        assertThat(response.designationId()).isEqualTo(1L);
        assertThat(response.designationName()).isEqualTo("Professor");
        assertThat(response.status()).isEqualTo(FacultyStatus.ACTIVE);

        ArgumentCaptor<Faculty> captor = ArgumentCaptor.forClass(Faculty.class);
        verify(facultyRepository).save(captor.capture());
        Faculty captured = captor.getValue();
        assertThat(captured.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(captured.getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldCreateFacultyWithDefaultActiveStatus() {
        FacultyRequest request = basicFacultyRequest(
            "EMP002", "Jane", "Smith", "jane.smith@college.edu", "0987654321",
            1L, 2L, "Data Science", "Big Data Lab",
            LocalDate.of(2021, 6, 1), null);

        Faculty savedFaculty = createFaculty(2L, "EMP002", "Jane", "Smith", "jane.smith@college.edu",
            testSpeciality, assistantProfessor, FacultyStatus.ACTIVE);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(testSpeciality));
        when(designationRepository.findById(2L)).thenReturn(Optional.of(assistantProfessor));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(savedFaculty);

        FacultyResponse response = facultyService.create(request);

        assertThat(response.status()).isEqualTo(FacultyStatus.ACTIVE);

        ArgumentCaptor<Faculty> captor = ArgumentCaptor.forClass(Faculty.class);
        verify(facultyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FacultyStatus.ACTIVE);
    }

    @Test
    void shouldThrowExceptionWhenCreatingFacultyWithNonExistentSpeciality() {
        FacultyRequest request = basicFacultyRequest(
            "EMP001", "John", "Doe", "john.doe@college.edu", "1234567890",
            999L, 1L, "AI", "ML Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ACTIVE);

        when(specialityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");

        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldFindAllFaculty() {
        Faculty faculty1 = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);
        Faculty faculty2 = createFaculty(2L, "EMP002", "Jane", "Smith", "jane@college.edu",
            testSpeciality, assistantProfessor, FacultyStatus.ACTIVE);

        FacultyDocument uploaded = new FacultyDocument(faculty1, DocumentType.PAN_CARD, DocumentVerificationStatus.UPLOADED);
        uploaded.setFileName("pan.pdf");
        FacultyDocument rejected = new FacultyDocument(faculty1, DocumentType.UG_DEGREE, DocumentVerificationStatus.REJECTED);
        rejected.setFileName("degree.pdf");

        when(facultyRepository.findAll()).thenReturn(List.of(faculty1, faculty2));
        when(facultyDocumentRepository.findByFacultyId(1L)).thenReturn(List.of(uploaded, rejected));
        when(requirementService.getRequiredDocumentTypesForFaculty(1L))
            .thenReturn(Set.of(DocumentType.PAN_CARD.name(), DocumentType.UG_DEGREE.name(), DocumentType.PG_DEGREE.name()));

        List<FacultyResponse> responses = facultyService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).employeeCode()).isEqualTo("EMP001");
        assertThat(responses.get(0).documentReview().pendingVerificationCount()).isEqualTo(1);
        assertThat(responses.get(0).documentReview().rejectedCount()).isEqualTo(1);
        assertThat(responses.get(0).documentReview().missingRequiredCount()).isEqualTo(1);
        assertThat(responses.get(1).employeeCode()).isEqualTo("EMP002");
        verify(facultyRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoFaculty() {
        when(facultyRepository.findAll()).thenReturn(List.of());

        List<FacultyResponse> responses = facultyService.findAll();

        assertThat(responses).isEmpty();
        verify(facultyRepository).findAll();
    }

    @Test
    void shouldFindFacultyById() {
        Faculty faculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));

        FacultyResponse response = facultyService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.fullName()).isEqualTo("John Doe");
        verify(facultyRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenFacultyNotFoundById() {
        when(facultyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");

        verify(facultyRepository).findById(999L);
    }

    @Test
    void shouldFindFacultyBySpecialityId() {
        Faculty faculty1 = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);
        Faculty faculty2 = createFaculty(2L, "EMP002", "Jane", "Smith", "jane@college.edu",
            testSpeciality, assistantProfessor, FacultyStatus.ACTIVE);

        when(specialityRepository.existsById(1L)).thenReturn(true);
        when(facultyRepository.findBySpecialityId(1L)).thenReturn(List.of(faculty1, faculty2));

        List<FacultyResponse> responses = facultyService.findBySpecialityId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).specialityId()).isEqualTo(1L);
        assertThat(responses.get(1).specialityId()).isEqualTo(1L);
        verify(facultyRepository).findBySpecialityId(1L);
    }

    @Test
    void shouldThrowExceptionWhenFindingByNonExistentSpecialityId() {
        when(specialityRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> facultyService.findBySpecialityId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");

        verify(facultyRepository, never()).findBySpecialityId(any());
    }

    @Test
    void shouldFindFacultyByStatus() {
        Faculty faculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ON_LEAVE);

        when(facultyRepository.findByStatus(FacultyStatus.ON_LEAVE)).thenReturn(List.of(faculty));

        List<FacultyResponse> responses = facultyService.findByStatus(FacultyStatus.ON_LEAVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(FacultyStatus.ON_LEAVE);
        verify(facultyRepository).findByStatus(FacultyStatus.ON_LEAVE);
    }

    @Test
    void shouldUpdateFaculty() {
        Faculty existingFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);

        Speciality newSpeciality = createSpeciality(2L, "Mathematics", "MATH");

        FacultyRequest updateRequest = basicFacultyRequest(
            "EMP001-UPD", "John Updated", "Doe Updated", "john.updated@college.edu", "9999999999",
            2L, 3L, "Applied Mathematics", "Math Lab",
            LocalDate.of(2019, 1, 1), FacultyStatus.ON_LEAVE);

        Faculty updatedFaculty = createFaculty(1L, "EMP001-UPD", "John Updated", "Doe Updated",
            "john.updated@college.edu", newSpeciality, associateProfessor, FacultyStatus.ON_LEAVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(existingFaculty));
        when(specialityRepository.findById(2L)).thenReturn(Optional.of(newSpeciality));
        when(designationRepository.findById(3L)).thenReturn(Optional.of(associateProfessor));
        when(facultyRepository.existsByEmployeeCodeIgnoreCaseAndIdNot("EMP001-UPD", 1L)).thenReturn(false);
        when(facultyRepository.existsByEmailIgnoreCaseAndIdNot("john.updated@college.edu", 1L)).thenReturn(false);
        when(facultyRepository.save(any(Faculty.class))).thenReturn(updatedFaculty);

        FacultyResponse response = facultyService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001-UPD");
        assertThat(response.fullName()).isEqualTo("John Updated Doe Updated");
        assertThat(response.specialityId()).isEqualTo(2L);
        assertThat(response.designationId()).isEqualTo(3L);
        assertThat(response.designationName()).isEqualTo("Associate Professor");
        assertThat(response.status()).isEqualTo(FacultyStatus.ON_LEAVE);

        verify(facultyRepository).findById(1L);
        verify(specialityRepository).findById(2L);
        verify(designationRepository).findById(3L);
        verify(facultyRepository).save(any(Faculty.class));
    }

    @Test
    void shouldPersistPlannedWeeklyHoursOverrideOnCreate() {
        FacultyRequest request = facultyRequestWithOverride(
            "EMP001", "John", "Doe", "john.doe@college.edu", "1234567890",
            1L, 1L, "Artificial Intelligence", "Machine Learning Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ACTIVE, 22);

        Faculty savedFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john.doe@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);
        savedFaculty.setPlannedWeeklyHoursOverride(22);

        when(specialityRepository.findById(1L)).thenReturn(Optional.of(testSpeciality));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(savedFaculty);

        FacultyResponse response = facultyService.create(request);

        assertThat(response.plannedWeeklyHoursOverride()).isEqualTo(22);

        ArgumentCaptor<Faculty> captor = ArgumentCaptor.forClass(Faculty.class);
        verify(facultyRepository).save(captor.capture());
        assertThat(captor.getValue().getPlannedWeeklyHoursOverride()).isEqualTo(22);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateEmployeeCode() {
        Faculty existingFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);

        FacultyRequest request = basicFacultyRequest(
            "EMP002", "John", "Doe", "john@college.edu", "123",
            1L, 1L, "AI", "ML Lab", LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(existingFaculty));
        when(specialityRepository.findById(1L)).thenReturn(Optional.of(testSpeciality));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(facultyRepository.existsByEmployeeCodeIgnoreCaseAndIdNot("EMP002", 1L)).thenReturn(true);

        assertThatThrownBy(() -> facultyService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A faculty with employee code 'EMP002' already exists");

        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateEmail() {
        Faculty existingFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);

        FacultyRequest request = basicFacultyRequest(
            "EMP001", "John", "Doe", "other@college.edu", "123",
            1L, 1L, "AI", "ML Lab", LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(existingFaculty));
        when(specialityRepository.findById(1L)).thenReturn(Optional.of(testSpeciality));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(facultyRepository.existsByEmployeeCodeIgnoreCaseAndIdNot("EMP001", 1L)).thenReturn(false);
        when(facultyRepository.existsByEmailIgnoreCaseAndIdNot("other@college.edu", 1L)).thenReturn(true);

        assertThatThrownBy(() -> facultyService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A faculty with email 'other@college.edu' already exists");

        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentFaculty() {
        FacultyRequest request = basicFacultyRequest(
            "EMP001", "John", "Doe", "john@college.edu", "123",
            1L, 1L, "AI", "ML Lab", LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE);

        when(facultyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");

        verify(facultyRepository).findById(999L);
        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentSpeciality() {
        Faculty existingFaculty = createFaculty(1L, "EMP001", "John", "Doe", "john@college.edu",
            testSpeciality, professor, FacultyStatus.ACTIVE);

        FacultyRequest request = basicFacultyRequest(
            "EMP001", "John", "Doe", "john@college.edu", "123",
            999L, 1L, "AI", "ML Lab", LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE);

        when(facultyRepository.findById(1L)).thenReturn(Optional.of(existingFaculty));
        when(specialityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Speciality not found with id: 999");

        verify(facultyRepository, never()).save(any(Faculty.class));
    }

    @Test
    void shouldDeleteFaculty() {
        when(facultyRepository.existsById(1L)).thenReturn(true);

        facultyService.delete(1L);

        verify(facultyRepository).existsById(1L);
        verify(facultyRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentFaculty() {
        when(facultyRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> facultyService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Faculty not found with id: 999");

        verify(facultyRepository).existsById(999L);
        verify(facultyRepository, never()).deleteById(any());
    }

    private static FacultyRequest basicFacultyRequest(
            String employeeCode, String firstName, String lastName, String email, String phone,
            Long specialityId, Long designationId, String specialization, String labExpertise,
            LocalDate joiningDate, FacultyStatus status) {
        final com.cms.model.enums.FacultyType facultyType = null;
        final com.cms.model.enums.FacultyQualification highestQualification = null;
        final com.cms.model.enums.Gender gender = null;
        final com.cms.model.enums.MaritalStatus maritalStatus = null;
        final com.cms.model.enums.BankAccountType bankAccountType = null;
        final com.cms.dto.AddressRequest address = null;
        final java.math.BigDecimal years = null;
        return new FacultyRequest(
            employeeCode, firstName, lastName, email, phone, specialityId, designationId,
            specialization, labExpertise, joiningDate, status,
            facultyType, highestQualification, null, null, null, null, gender, maritalStatus,
            null, null, null, null, null, null, null, null, bankAccountType, address,
            years, years, years, years, years, years, years, null
        );
    }

    private static FacultyRequest facultyRequestWithOverride(
            String employeeCode, String firstName, String lastName, String email, String phone,
            Long specialityId, Long designationId, String specialization, String labExpertise,
            LocalDate joiningDate, FacultyStatus status, Integer plannedWeeklyHoursOverride) {
        final com.cms.model.enums.FacultyType facultyType = null;
        final com.cms.model.enums.FacultyQualification highestQualification = null;
        final com.cms.model.enums.Gender gender = null;
        final com.cms.model.enums.MaritalStatus maritalStatus = null;
        final com.cms.model.enums.BankAccountType bankAccountType = null;
        final com.cms.dto.AddressRequest address = null;
        final java.math.BigDecimal years = null;
        return new FacultyRequest(
            employeeCode, firstName, lastName, email, phone, specialityId, designationId,
            specialization, labExpertise, joiningDate, status,
            facultyType, highestQualification, null, null, null, null, gender, maritalStatus,
            null, null, null, null, null, null, null, null, bankAccountType, address,
            years, years, years, years, years, years, years, plannedWeeklyHoursOverride
        );
    }

    private static Speciality createSpeciality(Long id, String name, String code) {
        Speciality speciality = new Speciality(name, code, "Description", null, null);
        speciality.setId(id);
        Instant now = Instant.now();
        speciality.setCreatedAt(now);
        speciality.setUpdatedAt(now);
        return speciality;
    }

    private static DesignationMaster createDesignation(Long id, String name, String code) {
        DesignationMaster d = new DesignationMaster(name, code, null);
        d.setId(id);
        Instant now = Instant.now();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        return d;
    }

    private Faculty createFaculty(Long id, String employeeCode, String firstName, String lastName,
                                   String email, Speciality speciality, DesignationMaster designation,
                                   FacultyStatus status) {
        Faculty faculty = new Faculty(
            employeeCode, firstName, lastName, email, "1234567890",
            speciality, designation, "Specialization", "Lab Expertise",
            LocalDate.of(2020, 1, 15), status
        );
        faculty.setId(id);
        Instant now = Instant.now();
        faculty.setCreatedAt(now);
        faculty.setUpdatedAt(now);
        return faculty;
    }
}
