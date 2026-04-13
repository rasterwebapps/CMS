package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AddressRequest;
import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Address;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;

    public StudentService(StudentRepository studentRepository, ProgramRepository programRepository) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        StudentStatus status = request.status() != null ? request.status() : StudentStatus.ACTIVE;

        Student student = new Student(
            request.rollNumber(),
            request.firstName(),
            request.lastName(),
            request.email(),
            program,
            request.semester(),
            request.admissionDate(),
            status
        );

        student.setPhone(request.phone());
        student.setLabBatch(request.labBatch());

        // Personal information
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setAadharNumber(request.aadharNumber());

        // Demographics
        student.setNationality(request.nationality());
        student.setReligion(request.religion());
        student.setCommunityCategory(request.communityCategory());
        student.setCaste(request.caste());
        student.setBloodGroup(request.bloodGroup());

        // Family information
        student.setFatherName(request.fatherName());
        student.setMotherName(request.motherName());
        student.setParentMobile(request.parentMobile());

        // Address
        if (request.address() != null) {
            student.setAddress(toAddress(request.address()));
        }

        Student saved = studentRepository.save(student);
        return toResponse(saved);
    }

    public List<StudentResponse> findAll() {
        return studentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public StudentResponse findById(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return toResponse(student);
    }

    public StudentResponse findByRollNumber(String rollNumber) {
        Student student = studentRepository.findByRollNumber(rollNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with roll number: " + rollNumber));
        return toResponse(student);
    }

    public List<StudentResponse> findByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResourceNotFoundException("Program not found with id: " + programId);
        }
        return studentRepository.findByProgramId(programId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<StudentResponse> findByStatus(StudentStatus status) {
        return studentRepository.findByStatus(status).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<StudentResponse> findByLabBatch(String labBatch) {
        return studentRepository.findByLabBatch(labBatch).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        student.setRollNumber(request.rollNumber());
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setPhone(request.phone());
        student.setProgram(program);
        student.setSemester(request.semester());
        student.setAdmissionDate(request.admissionDate());
        student.setLabBatch(request.labBatch());

        if (request.status() != null) {
            student.setStatus(request.status());
        }

        // Personal information
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setAadharNumber(request.aadharNumber());

        // Demographics
        student.setNationality(request.nationality());
        student.setReligion(request.religion());
        student.setCommunityCategory(request.communityCategory());
        student.setCaste(request.caste());
        student.setBloodGroup(request.bloodGroup());

        // Family information
        student.setFatherName(request.fatherName());
        student.setMotherName(request.motherName());
        student.setParentMobile(request.parentMobile());

        // Address
        if (request.address() != null) {
            student.setAddress(toAddress(request.address()));
        }

        Student updated = studentRepository.save(student);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    private Address toAddress(AddressRequest request) {
        return new Address(
            request.postalAddress(),
            request.street(),
            request.city(),
            request.district(),
            request.state(),
            request.pincode()
        );
    }

    private StudentResponse toResponse(Student student) {
        Address address = student.getAddress();
        return new StudentResponse(
            student.getId(),
            student.getRollNumber(),
            student.getFirstName(),
            student.getLastName(),
            student.getFullName(),
            student.getEmail(),
            student.getPhone(),
            student.getProgram().getId(),
            student.getProgram().getName(),
            student.getSemester(),
            student.getAdmissionDate(),
            student.getLabBatch(),
            student.getStatus(),
            student.getDateOfBirth(),
            student.getGender(),
            student.getNationality(),
            student.getReligion(),
            student.getCommunityCategory(),
            student.getCaste(),
            student.getBloodGroup(),
            student.getFatherName(),
            student.getMotherName(),
            student.getParentMobile(),
            address != null ? address.getPostalAddress() : null,
            address != null ? address.getStreet() : null,
            address != null ? address.getCity() : null,
            address != null ? address.getDistrict() : null,
            address != null ? address.getState() : null,
            address != null ? address.getPincode() : null,
            student.getCreatedAt(),
            student.getUpdatedAt()
        );
    }
}
