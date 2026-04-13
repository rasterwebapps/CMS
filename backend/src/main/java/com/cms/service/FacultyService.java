package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.exception.ResourceNotFoundException;
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

        FacultyStatus status = request.status() != null ? request.status() : FacultyStatus.ACTIVE;

        Faculty faculty = new Faculty(
            request.employeeCode(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone(),
            department,
            request.designation(),
            request.specialization(),
            request.labExpertise(),
            request.joiningDate(),
            status
        );

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

        faculty.setEmployeeCode(request.employeeCode());
        faculty.setFirstName(request.firstName());
        faculty.setLastName(request.lastName());
        faculty.setEmail(request.email());
        faculty.setPhone(request.phone());
        faculty.setDepartment(department);
        faculty.setDesignation(request.designation());
        faculty.setSpecialization(request.specialization());
        faculty.setLabExpertise(request.labExpertise());
        faculty.setJoiningDate(request.joiningDate());

        if (request.status() != null) {
            faculty.setStatus(request.status());
        }

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

    private FacultyResponse toResponse(Faculty faculty) {
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
            faculty.getCreatedAt(),
            faculty.getUpdatedAt()
        );
    }
}
