package com.cms.service;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AddressRequest;
import com.cms.dto.FacultyDocumentReviewSummary;
import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Address;
import com.cms.model.DesignationMaster;
import com.cms.model.Speciality;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocument;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.DesignationRepository;
import com.cms.repository.FacultySpecification;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.FacultyDocumentRepository;
import com.cms.repository.FacultyRepository;

@Service
@Transactional(readOnly = true)
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final SpecialityRepository specialityRepository;
    private final DesignationRepository designationRepository;
    private final FacultyDocumentRepository facultyDocumentRepository;
    private final FacultyDocumentTypeRequirementService requirementService;

    public FacultyService(FacultyRepository facultyRepository,
                          SpecialityRepository specialityRepository,
                          DesignationRepository designationRepository,
                          FacultyDocumentRepository facultyDocumentRepository,
                          FacultyDocumentTypeRequirementService requirementService) {
        this.facultyRepository = facultyRepository;
        this.specialityRepository = specialityRepository;
        this.designationRepository = designationRepository;
        this.facultyDocumentRepository = facultyDocumentRepository;
        this.requirementService = requirementService;
    }

    @Transactional
    public FacultyResponse create(FacultyRequest request) {
        Speciality speciality = specialityRepository.findById(request.specialityId())
            .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + request.specialityId()));
        DesignationMaster designation = designationRepository.findById(request.designationId())
            .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + request.designationId()));
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
        String nrtsNumber = trim(request.nrtsNumber());
        if (nrtsNumber != null && facultyRepository.existsByNrtsNumberIgnoreCase(nrtsNumber)) {
            throw new IllegalArgumentException(
                "A faculty with NRTS number '" + nrtsNumber + "' already exists");
        }

        FacultyStatus status = request.status() != null ? request.status() : FacultyStatus.ACTIVE;

        Faculty faculty = new Faculty(
            employeeCode,
            trim(request.firstName()),
            trim(request.lastName()),
            email,
            trim(request.phone()),
            speciality,
            designation,
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

    public List<FacultyResponse> findAll(String search, Long specialityId,
                                         FacultyStatus status, String documentReview, Sort sort) {
        Specification<Faculty> spec = FacultySpecification.distinct();
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and(FacultySpecification.bySearch(search.trim()));
        }
        if (specialityId != null) {
            spec = spec.and(FacultySpecification.bySpecialityId(specialityId));
        }
        if (status != null) {
            spec = spec.and(FacultySpecification.byStatus(status));
        }
        if (documentReview != null && !"ALL".equalsIgnoreCase(documentReview)) {
            Specification<Faculty> docFilter = FacultySpecification.byDocumentReview(documentReview);
            if (docFilter != null) {
                spec = spec.and(docFilter);
            }
        }
        return facultyRepository.findAll(spec, sort).stream().map(this::toResponse).toList();
    }

    public Page<FacultyResponse> findPage(String search, Long specialityId,
                                          FacultyStatus status, String documentReview,
                                          Pageable pageable) {
        Specification<Faculty> spec = FacultySpecification.distinct();
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and(FacultySpecification.bySearch(search.trim()));
        }
        if (specialityId != null) {
            spec = spec.and(FacultySpecification.bySpecialityId(specialityId));
        }
        if (status != null) {
            spec = spec.and(FacultySpecification.byStatus(status));
        }
        if (documentReview != null && !"ALL".equalsIgnoreCase(documentReview)) {
            Specification<Faculty> docFilter = FacultySpecification.byDocumentReview(documentReview);
            if (docFilter != null) {
                spec = spec.and(docFilter);
            }
        }
        return facultyRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public FacultyResponse findById(Long id) {
        Faculty faculty = facultyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + id));
        return toResponse(faculty);
    }

    public List<FacultyResponse> findBySpecialityId(Long specialityId) {
        if (!specialityRepository.existsById(specialityId)) {
            throw new ResourceNotFoundException("Speciality not found with id: " + specialityId);
        }
        return facultyRepository.findBySpecialityId(specialityId).stream()
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

        Speciality speciality = specialityRepository.findById(request.specialityId())
            .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + request.specialityId()));
        DesignationMaster designation = designationRepository.findById(request.designationId())
            .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + request.designationId()));
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
        String nrtsNumber = trim(request.nrtsNumber());
        if (nrtsNumber != null && facultyRepository.existsByNrtsNumberIgnoreCaseAndIdNot(nrtsNumber, id)) {
            throw new IllegalArgumentException(
                "A faculty with NRTS number '" + nrtsNumber + "' already exists");
        }

        faculty.setEmployeeCode(employeeCode);
        faculty.setFirstName(trim(request.firstName()));
        faculty.setLastName(trim(request.lastName()));
        faculty.setEmail(email);
        faculty.setPhone(trim(request.phone()));
        faculty.setSpeciality(speciality);
        faculty.setDesignation(designation);
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

    public boolean nrtsNumberExists(String nrtsNumber, Long excludeId) {
        String value = trim(nrtsNumber);
        if (value == null) return false;
        if (excludeId != null) return facultyRepository.existsByNrtsNumberIgnoreCaseAndIdNot(value, excludeId);
        return facultyRepository.existsByNrtsNumberIgnoreCase(value);
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
        faculty.setNrtsNumber(trim(r.nrtsNumber()));
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
                a.countryId(),
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
        faculty.setCommissionAmount(r.commissionAmount());
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
            addr.getCountryId(),
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
            faculty.getSpeciality().getId(),
            faculty.getSpeciality().getName(),
            faculty.getDesignation() != null ? faculty.getDesignation().getId() : null,
            faculty.getDesignation() != null ? faculty.getDesignation().getName() : null,
            faculty.getSpecialization(),
            faculty.getLabExpertise(),
            faculty.getJoiningDate(),
            faculty.getStatus(),
            faculty.getFacultyType(),
            faculty.getHighestQualification(),
            faculty.getNrtsNumber(),
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
            faculty.getBio(),
            faculty.getEmergencyContactName(),
            faculty.getEmergencyContactRelationship(),
            faculty.getEmergencyContactPhone(),
            faculty.getTeachingExperienceUgYears(),
            faculty.getTeachingExperiencePgYears(),
            faculty.getTeachingExperiencePhdYears(),
            faculty.getClinicalExperienceUgYears(),
            faculty.getClinicalExperiencePgYears(),
            faculty.getClinicalExperiencePhdYears(),
            faculty.getCreatedAt(),
            faculty.getUpdatedAt(),
            documentReviewSummary(faculty),
            faculty.getCommissionAmount()
        );
    }

    private FacultyDocumentReviewSummary documentReviewSummary(Faculty faculty) {
        if (faculty.getId() == null) {
            return emptyDocumentReviewSummary();
        }

        List<FacultyDocument> documents = facultyDocumentRepository.findByFacultyId(faculty.getId());
        if (documents == null) {
            documents = List.of();
        }
        Set<String> requiredTypes = requirementService.getRequiredDocumentTypesForFaculty(faculty.getId());
        if (requiredTypes == null) {
            requiredTypes = Set.of();
        }
        Map<DocumentType, FacultyDocument> byType = documents.stream()
            .collect(Collectors.toMap(FacultyDocument::getDocumentType, Function.identity(), (first, ignored) -> first));

        int totalDocumentCount = (int) documents.stream().filter(this::hasFile).count();
        int pendingVerificationCount = (int) documents.stream()
            .filter(this::hasFile)
            .filter(doc -> doc.getStatus() == DocumentVerificationStatus.UPLOADED)
            .count();
        int rejectedCount = (int) documents.stream()
            .filter(this::hasFile)
            .filter(doc -> doc.getStatus() == DocumentVerificationStatus.REJECTED)
            .count();
        int missingRequiredCount = 0;
        int verifiedRequiredCount = 0;

        for (String requiredType : requiredTypes) {
            DocumentType documentType = DocumentType.valueOf(requiredType);
            FacultyDocument document = byType.get(documentType);
            if (document == null || !hasFile(document) || document.getStatus() == DocumentVerificationStatus.NOT_UPLOADED) {
                missingRequiredCount++;
            } else if (document.getStatus() == DocumentVerificationStatus.VERIFIED) {
                verifiedRequiredCount++;
            }
        }

        return new FacultyDocumentReviewSummary(
            totalDocumentCount,
            requiredTypes.size(),
            pendingVerificationCount,
            rejectedCount,
            missingRequiredCount,
            verifiedRequiredCount,
            totalDocumentCount > 0,
            pendingVerificationCount > 0,
            rejectedCount > 0,
            !requiredTypes.isEmpty() && missingRequiredCount == 0 && verifiedRequiredCount == requiredTypes.size()
        );
    }

    private boolean hasFile(FacultyDocument document) {
        return document.getFileName() != null;
    }

    private FacultyDocumentReviewSummary emptyDocumentReviewSummary() {
        return new FacultyDocumentReviewSummary(0, 0, 0, 0, 0, 0, false, false, false, false);
    }
}
