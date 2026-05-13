package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AddressRequest;
import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Address;
import com.cms.model.Department;
import com.cms.model.Faculty;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.FacultyRepository;

@Service
@Transactional(readOnly = true)
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;

    public FacultyService(FacultyRepository facultyRepository, DepartmentRepository departmentRepository) {
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public FacultyResponse create(FacultyRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        String employeeCode = requireTrimmed(request.employeeCode(), "Faculty employee code is required");
        String email = requireTrimmed(request.email(), "Faculty email is required");

        if (facultyRepository.existsByEmployeeCodeIgnoreCase(employeeCode)) {
            throw new IllegalArgumentException(
                "A faculty with employee code '" + employeeCode + "' already exists");
        }
        if (facultyRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                "A faculty with email '" + email + "' already exists");
        }

        FacultyStatus status = request.status() != null ? request.status() : FacultyStatus.ACTIVE;

        Faculty faculty = new Faculty(
            employeeCode,
            trim(request.firstName()),
            trim(request.lastName()),
            email,
            trim(request.phone()),
            department,
            request.designation(),
            trim(request.specialization()),
            trim(request.labExpertise()),
            request.joiningDate(),
            status
        );

        applyExtendedFields(faculty, request);

        Faculty saved = facultyRepository.save(faculty);
        return toResponse(saved);
    }

    public List<FacultyResponse> findAll() {
        return facultyRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public FacultyResponse findById(Long id) {
        Faculty faculty = facultyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + id));
        return toResponse(faculty);
    }

    public List<FacultyResponse> findByDepartmentId(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return facultyRepository.findByDepartmentId(departmentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FacultyResponse> findByStatus(FacultyStatus status) {
        return facultyRepository.findByStatus(status).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FacultyResponse update(Long id, FacultyRequest request) {
        Faculty faculty = facultyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + id));

        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        String employeeCode = requireTrimmed(request.employeeCode(), "Faculty employee code is required");
        String email = requireTrimmed(request.email(), "Faculty email is required");

        if (facultyRepository.existsByEmployeeCodeIgnoreCaseAndIdNot(employeeCode, id)) {
            throw new IllegalArgumentException(
                "A faculty with employee code '" + employeeCode + "' already exists");
        }
        if (facultyRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException(
                "A faculty with email '" + email + "' already exists");
        }

        faculty.setEmployeeCode(employeeCode);
        faculty.setFirstName(trim(request.firstName()));
        faculty.setLastName(trim(request.lastName()));
        faculty.setEmail(email);
        faculty.setPhone(trim(request.phone()));
        faculty.setDepartment(department);
        faculty.setDesignation(request.designation());
        faculty.setSpecialization(trim(request.specialization()));
        faculty.setLabExpertise(trim(request.labExpertise()));
        faculty.setJoiningDate(request.joiningDate());

        if (request.status() != null) {
            faculty.setStatus(request.status());
        }

        applyExtendedFields(faculty, request);

        Faculty updated = facultyRepository.save(faculty);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!facultyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + id);
        }
        facultyRepository.deleteById(id);
    }

    private void applyExtendedFields(Faculty faculty, FacultyRequest r) {
        faculty.setFacultyType(r.facultyType());
        faculty.setHighestQualification(r.highestQualification());
        faculty.setPanNumber(trim(r.panNumber()));
        faculty.setAadhaarNumber(trim(r.aadhaarNumber()));
        faculty.setDateOfBirth(r.dateOfBirth());
        faculty.setGender(r.gender());
        faculty.setMaritalStatus(r.maritalStatus());
        faculty.setNationality(trim(r.nationality()));
        faculty.setReligion(trim(r.religion()));
        faculty.setBloodGroup(trim(r.bloodGroup()));

        faculty.setBankAccountNumber(trim(r.bankAccountNumber()));
        faculty.setBankIfscCode(trim(r.bankIfscCode()));
        faculty.setBankBranch(trim(r.bankBranch()));
        faculty.setBankName(trim(r.bankName()));
        faculty.setBankAccountHolder(trim(r.bankAccountHolder()));
        faculty.setBankAccountType(r.bankAccountType());

        AddressRequest a = r.address();
        if (a == null) {
            faculty.setAddress(null);
        } else {
            faculty.setAddress(new Address(
                trim(a.postalAddress()),
                trim(a.street()),
                trim(a.city()),
                trim(a.district()),
                trim(a.state()),
                trim(a.pincode())
            ));
        }

        faculty.setTeachingExperienceUgYears(r.teachingExperienceUgYears());
        faculty.setTeachingExperiencePgYears(r.teachingExperiencePgYears());
        faculty.setTeachingExperiencePhdYears(r.teachingExperiencePhdYears());
        faculty.setClinicalExperienceUgYears(r.clinicalExperienceUgYears());
        faculty.setClinicalExperiencePgYears(r.clinicalExperiencePgYears());
        faculty.setClinicalExperiencePhdYears(r.clinicalExperiencePhdYears());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }

    private FacultyResponse toResponse(Faculty faculty) {
        Address addr = faculty.getAddress();
        AddressRequest addressDto = addr == null ? null : new AddressRequest(
            addr.getPostalAddress(),
            addr.getStreet(),
            addr.getCity(),
            addr.getDistrict(),
            addr.getState(),
            addr.getPincode()
        );
        return new FacultyResponse(
            faculty.getId(),
            faculty.getEmployeeCode(),
            faculty.getFirstName(),
            faculty.getLastName(),
            faculty.getFullName(),
            faculty.getEmail(),
            faculty.getPhone(),
            faculty.getDepartment().getId(),
            faculty.getDepartment().getName(),
            faculty.getDesignation(),
            faculty.getSpecialization(),
            faculty.getLabExpertise(),
            faculty.getJoiningDate(),
            faculty.getStatus(),
            faculty.getFacultyType(),
            faculty.getHighestQualification(),
            faculty.getPanNumber(),
            faculty.getAadhaarNumber(),
            faculty.getDateOfBirth(),
            faculty.getGender(),
            faculty.getMaritalStatus(),
            faculty.getNationality(),
            faculty.getReligion(),
            faculty.getBloodGroup(),
            faculty.getBankAccountNumber(),
            faculty.getBankIfscCode(),
            faculty.getBankBranch(),
            faculty.getBankName(),
            faculty.getBankAccountHolder(),
            faculty.getBankAccountType(),
            addressDto,
            faculty.getTeachingExperienceUgYears(),
            faculty.getTeachingExperiencePgYears(),
            faculty.getTeachingExperiencePhdYears(),
            faculty.getClinicalExperienceUgYears(),
            faculty.getClinicalExperiencePgYears(),
            faculty.getClinicalExperiencePhdYears(),
            faculty.getCreatedAt(),
            faculty.getUpdatedAt()
        );
    }
}
