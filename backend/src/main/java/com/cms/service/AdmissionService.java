package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AdmissionRequest;
import com.cms.dto.AdmissionResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.Student;
import com.cms.model.enums.AdmissionStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final StudentRepository studentRepository;

    public AdmissionService(AdmissionRepository admissionRepository, StudentRepository studentRepository) {
        this.admissionRepository = admissionRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public AdmissionResponse create(AdmissionRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        AdmissionStatus status = request.status() != null ? request.status() : AdmissionStatus.DRAFT;
        Admission admission = new Admission(
            student,
            request.academicYearFrom(),
            request.academicYearTo(),
            request.applicationDate(),
            status
        );
        admission.setDeclarationPlace(request.declarationPlace());
        admission.setDeclarationDate(request.declarationDate());
        admission.setParentConsentGiven(request.parentConsentGiven());
        admission.setApplicantConsentGiven(request.applicantConsentGiven());
        Admission saved = admissionRepository.save(admission);
        return toResponse(saved);
    }

    public List<AdmissionResponse> findAll() {
        return admissionRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public AdmissionResponse findById(Long id) {
        Admission admission = admissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
        return toResponse(admission);
    }

    public AdmissionResponse findByStudentId(Long studentId) {
        Admission admission = admissionRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found for student id: " + studentId));
        return toResponse(admission);
    }

    @Transactional
    public AdmissionResponse update(Long id, AdmissionRequest request) {
        Admission admission = admissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
        admission.setStudent(student);
        admission.setAcademicYearFrom(request.academicYearFrom());
        admission.setAcademicYearTo(request.academicYearTo());
        admission.setApplicationDate(request.applicationDate());
        if (request.status() != null) {
            admission.setStatus(request.status());
        }
        admission.setDeclarationPlace(request.declarationPlace());
        admission.setDeclarationDate(request.declarationDate());
        admission.setParentConsentGiven(request.parentConsentGiven());
        admission.setApplicantConsentGiven(request.applicantConsentGiven());
        Admission updated = admissionRepository.save(admission);
        return toResponse(updated);
    }

    @Transactional
    public AdmissionResponse updateStatus(Long id, AdmissionStatus status) {
        Admission admission = admissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
        admission.setStatus(status);
        Admission updated = admissionRepository.save(admission);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!admissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Admission not found with id: " + id);
        }
        admissionRepository.deleteById(id);
    }

    private AdmissionResponse toResponse(Admission admission) {
        return new AdmissionResponse(
            admission.getId(),
            admission.getStudent().getId(),
            admission.getStudent().getFullName(),
            admission.getAcademicYearFrom(),
            admission.getAcademicYearTo(),
            admission.getApplicationDate(),
            admission.getStatus(),
            admission.getDeclarationPlace(),
            admission.getDeclarationDate(),
            admission.getParentConsentGiven(),
            admission.getApplicantConsentGiven(),
            admission.getCreatedAt(),
            admission.getUpdatedAt()
        );
    }
}
