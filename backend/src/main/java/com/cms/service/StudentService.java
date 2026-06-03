package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AddressRequest;
import com.cms.dto.BulkRollNumberItem;
import com.cms.dto.ProgramTransferAnalysis;
import com.cms.dto.ProgramTransferDocumentInfo;
import com.cms.dto.ProgramTransferRecord;
import com.cms.dto.ProgramTransferRequest;
import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Address;
import com.cms.model.Course;
import com.cms.model.Department;
import com.cms.model.EnquiryDocument;
import com.cms.model.EnquiryDocumentHistory;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentProgramTransfer;
import com.cms.model.FeeDemand;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.EnquiryDocumentHistoryRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.LibraryIssueRepository;
import com.cms.repository.StudentProgramTransferRepository;
import com.cms.repository.StudentRepository;
import com.cms.util.CurrentUserResolver;
import com.cms.model.enums.IssueStatus;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final AdmissionRepository admissionRepository;
    private final EnquiryDocumentRepository enquiryDocumentRepository;
    private final EnquiryDocumentHistoryRepository documentHistoryRepository;
    private final StudentProgramTransferRepository transferRepository;
    private final FeeDemandRepository feeDemandRepository;
    private final LibraryIssueRepository libraryIssueRepository;
    private final CurrentUserResolver currentUserResolver;

    private static final List<IssueStatus> ACTIVE_ISSUE_STATUSES =
        List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE);

    public StudentService(StudentRepository studentRepository, ProgramRepository programRepository,
                          CourseRepository courseRepository, DepartmentRepository departmentRepository,
                          AdmissionRepository admissionRepository,
                          EnquiryDocumentRepository enquiryDocumentRepository,
                          EnquiryDocumentHistoryRepository documentHistoryRepository,
                          StudentProgramTransferRepository transferRepository,
                          FeeDemandRepository feeDemandRepository,
                          LibraryIssueRepository libraryIssueRepository,
                          CurrentUserResolver currentUserResolver) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.admissionRepository = admissionRepository;
        this.enquiryDocumentRepository = enquiryDocumentRepository;
        this.documentHistoryRepository = documentHistoryRepository;
        this.transferRepository = transferRepository;
        this.feeDemandRepository = feeDemandRepository;
        this.libraryIssueRepository = libraryIssueRepository;
        this.currentUserResolver = currentUserResolver;
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
            request.yearOfStudy(),
            request.admissionDate(),
            status
        );

        student.setPhone(request.phone());
        // University identification numbers
        student.setUniversityRegistrationNumber(request.universityRegistrationNumber());
        student.setUmisNumber(request.umisNumber());
        // Course and specialization department
        if (request.courseId() != null) {
            Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
            student.setCourse(course);
        }
        if (request.specializationDepartmentId() != null) {
            Department department = departmentRepository.findById(request.specializationDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.specializationDepartmentId()));
            student.setSpecializationDepartment(department);
        }

        student.setLabBatch(request.labBatch());
        student.setAdmissionCategory(request.admissionCategory());

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
        student.setPhysicalDisability(Boolean.TRUE.equals(request.physicalDisability()));

        // Family information
        student.setFatherName(request.fatherName());
        student.setFatherPhone(request.fatherPhone());
        student.setFatherEmail(request.fatherEmail());
        student.setMotherName(request.motherName());
        student.setMotherPhone(request.motherPhone());
        student.setMotherEmail(request.motherEmail());
        student.setParentMobile(request.parentMobile());
        student.setFirstGraduate(Boolean.TRUE.equals(request.isFirstGraduate()));
        student.setFatherEducation(request.fatherEducation());
        student.setMotherEducation(request.motherEducation());

        // Address
        if (request.address() != null) {
            student.setAddress(toAddress(request.address()));
        }

        Student saved = studentRepository.save(student);
        return toResponse(saved);
    }

    public List<StudentResponse> findAll() {
        List<Student> students = studentRepository.findAll();
        return enrichAndMap(students);
    }

    /** Explorer: filter by academicYearId and/or feeStatus. Null params = no filter. */
    public List<StudentResponse> findExplorer(Long academicYearId, String feeStatus) {
        List<Student> students = academicYearId != null
            ? studentRepository.findByCohortAdmissionAcademicYearId(academicYearId)
            : studentRepository.findAll();
        List<StudentResponse> responses = enrichAndMap(students);
        if (feeStatus != null && !feeStatus.isBlank()) {
            responses = responses.stream()
                .filter(r -> feeStatus.equalsIgnoreCase(r.feeStatus()))
                .toList();
        }
        return responses;
    }

    /** Load demands for all students in one query, compute fee status, then map to response. */
    private List<StudentResponse> enrichAndMap(List<Student> students) {
        if (students.isEmpty()) return List.of();
        Collection<Long> ids = students.stream().map(Student::getId).toList();
        Map<Long, String> feeStatusByStudent = computeFeeStatusMap(ids);
        return students.stream()
            .map(s -> toResponseWithExtras(s, feeStatusByStudent.getOrDefault(s.getId(), "NOT_ASSIGNED")))
            .toList();
    }

    /** Bulk compute fee status for a set of student IDs. Returns "PAID", "PARTIAL", "UNPAID", "NOT_ASSIGNED". */
    private Map<Long, String> computeFeeStatusMap(Collection<Long> studentIds) {
        List<FeeDemand> demands = feeDemandRepository.findByStudentIdIn(studentIds);
        Map<Long, List<DemandStatus>> byStudent = new HashMap<>();
        for (FeeDemand d : demands) {
            Long sid = d.getStudentTermEnrollment().getStudent().getId();
            byStudent.computeIfAbsent(sid, k -> new ArrayList<>()).add(d.getStatus());
        }
        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, List<DemandStatus>> entry : byStudent.entrySet()) {
            result.put(entry.getKey(), aggregateFeeStatus(entry.getValue()));
        }
        return result;
    }

    private String aggregateFeeStatus(List<DemandStatus> statuses) {
        if (statuses.isEmpty()) return "NOT_ASSIGNED";
        boolean allPaidOrWaived = statuses.stream().allMatch(s -> s == DemandStatus.PAID || s == DemandStatus.WAIVED);
        if (allPaidOrWaived) return "PAID";
        boolean anyPartial = statuses.stream().anyMatch(s -> s == DemandStatus.PARTIAL);
        if (anyPartial) return "PARTIAL";
        return "UNPAID";
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

        // Only update rollNumber if provided (non-null) to avoid accidentally clearing it
        if (request.rollNumber() != null) {
            student.setRollNumber(request.rollNumber());
        }
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setPhone(request.phone());
        student.setProgram(program);
        // University identification numbers
        student.setUniversityRegistrationNumber(request.universityRegistrationNumber());
        student.setUmisNumber(request.umisNumber());
        // Course and specialization department
        if (request.courseId() != null) {
            Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));
            student.setCourse(course);
        } else {
            student.setCourse(null);
        }
        if (request.specializationDepartmentId() != null) {
            Department department = departmentRepository.findById(request.specializationDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.specializationDepartmentId()));
            student.setSpecializationDepartment(department);
        } else {
            student.setSpecializationDepartment(null);
        }

        student.setSemester(request.yearOfStudy());
        student.setAdmissionDate(request.admissionDate());
        student.setLabBatch(request.labBatch());
        student.setAdmissionCategory(request.admissionCategory());

        if (request.status() != null) {
            StudentStatus newStatus = request.status();
            if (newStatus != StudentStatus.ACTIVE && newStatus != StudentStatus.ON_LEAVE) {
                long activeIssues = libraryIssueRepository.countByStudentIdAndStatusIn(id, ACTIVE_ISSUE_STATUSES);
                if (activeIssues > 0) {
                    throw new IllegalStateException(
                        "Cannot change student status to " + newStatus + " — they have " + activeIssues +
                        " library book(s) currently issued. Please return all books first.");
                }
            }
            student.setStatus(newStatus);
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
        student.setPhysicalDisability(Boolean.TRUE.equals(request.physicalDisability()));

        // Family information
        student.setFatherName(request.fatherName());
        student.setFatherPhone(request.fatherPhone());
        student.setFatherEmail(request.fatherEmail());
        student.setMotherName(request.motherName());
        student.setMotherPhone(request.motherPhone());
        student.setMotherEmail(request.motherEmail());
        student.setParentMobile(request.parentMobile());
        student.setFirstGraduate(Boolean.TRUE.equals(request.isFirstGraduate()));
        student.setFatherEducation(request.fatherEducation());
        student.setMotherEducation(request.motherEducation());

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
        long activeIssues = libraryIssueRepository.countByStudentIdAndStatusIn(id, ACTIVE_ISSUE_STATUSES);
        if (activeIssues > 0) {
            throw new IllegalStateException(
                "Cannot delete student — they have " + activeIssues + " library book(s) currently issued. Please return all books before deleting.");
        }
        studentRepository.deleteById(id);
    }

    @Transactional
    public StudentResponse assignRollNumber(Long studentId, String rollNumber) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        if (student.getRollNumber() != null) {
            throw new IllegalStateException("Student already has a roll number: " + student.getRollNumber());
        }
        if (studentRepository.existsByRollNumber(rollNumber)) {
            throw new IllegalStateException("Roll number already in use: " + rollNumber);
        }
        student.setRollNumber(rollNumber);
        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public List<StudentResponse> bulkAssignRollNumbers(List<BulkRollNumberItem> assignments) {
        List<StudentResponse> results = new ArrayList<>();
        for (BulkRollNumberItem item : assignments) {
            results.add(assignRollNumber(item.studentId(), item.rollNumber()));
        }
        return results;
    }

    public List<StudentResponse> findStudentsWithoutRollNumber(Long courseId, Long programId) {
        if (courseId != null) {
            return studentRepository.findByCourseIdAndRollNumberIsNull(courseId).stream()
                .map(this::toResponse)
                .toList();
        }
        if (programId != null) {
            return studentRepository.findByProgramIdAndRollNumberIsNull(programId).stream()
                .map(this::toResponse)
                .toList();
        }
        return studentRepository.findByRollNumberIsNull().stream()
            .map(this::toResponse)
            .toList();
    }

    private Address toAddress(AddressRequest request) {
        return new Address(
            request.countryId(),
            request.postalAddress(),
            request.street(),
            request.city(),
            request.district(),
            request.state(),
            request.pincode()
        );
    }

    private StudentResponse toResponse(Student student) {
        return toResponseWithExtras(student, null);
    }

    private StudentResponse toResponseWithExtras(Student student, String feeStatus) {
        Address address = student.getAddress();
        Long ayId = null;
        String ayName = null;
        if (student.getCohort() != null && student.getCohort().getAdmissionAcademicYear() != null) {
            ayId   = student.getCohort().getAdmissionAcademicYear().getId();
            ayName = student.getCohort().getAdmissionAcademicYear().getName();
        }
        return new StudentResponse(
            student.getId(),
            student.getRollNumber(),
            student.getAdmissionNumber(),
            student.getUniversityRegistrationNumber(),
            student.getUmisNumber(),
            student.getFirstName(),
            student.getLastName(),
            student.getFullName(),
            student.getEmail(),
            student.getPhone(),
            student.getProgram().getId(),
            student.getProgram().getName(),
            student.getCourse() != null ? student.getCourse().getId() : null,
            student.getCourse() != null ? student.getCourse().getName() : null,
            student.getSpecializationDepartment() != null ? student.getSpecializationDepartment().getId() : null,
            student.getSpecializationDepartment() != null ? student.getSpecializationDepartment().getName() : null,
            student.getYearOfStudy(),
            student.getAdmissionDate(),
            student.getLabBatch(),
            student.getStatus(),
            student.getAdmissionCategory(),
            student.getDateOfBirth(),
            student.getGender(),
            student.getNationality(),
            student.getReligion(),
            student.getCommunityCategory(),
            student.getCaste(),
            student.getBloodGroup(),
            student.isPhysicalDisability(),
            student.getFatherName(),
            student.getFatherPhone(),
            student.getFatherEmail(),
            student.getMotherName(),
            student.getMotherPhone(),
            student.getMotherEmail(),
            student.getParentMobile(),
            student.isFirstGraduate(),
            student.getFatherEducation(),
            student.getMotherEducation(),
            address != null ? address.getCountryId() : null,
            address != null ? address.getPostalAddress() : null,
            address != null ? address.getStreet() : null,
            address != null ? address.getCity() : null,
            address != null ? address.getDistrict() : null,
            address != null ? address.getState() : null,
            address != null ? address.getPincode() : null,
            student.getBio(),
            student.getEmergencyContactName(),
            student.getEmergencyContactRelationship(),
            student.getEmergencyContactPhone(),
            ayId,
            ayName,
            feeStatus,
            student.getCreatedAt(),
            student.getUpdatedAt()
        );
    }

    public ProgramTransferAnalysis analyzeProgramTransfer(Long studentId, Long newProgramId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Program newProgram = programRepository.findById(newProgramId)
            .orElseThrow(() -> new ResourceNotFoundException("Program not found: " + newProgramId));

        Program oldProgram = student.getProgram();
        Set<DocumentType> oldRequired = oldProgram.getRequiredDocumentTypes();
        Set<DocumentType> newRequired = newProgram.getRequiredDocumentTypes();

        Long admissionId = admissionRepository.findByStudentId(studentId)
            .map(a -> a.getId()).orElse(null);
        Map<DocumentType, EnquiryDocument> uploadedByType = admissionId == null
            ? Map.of()
            : enquiryDocumentRepository.findByAdmission_Id(admissionId).stream()
                .filter(d -> d.getStatus() != DocumentVerificationStatus.RETURNED)
                .collect(Collectors.toMap(EnquiryDocument::getDocumentType, d -> d, (a, b) -> a));

        List<ProgramTransferDocumentInfo> retained = new ArrayList<>();
        List<ProgramTransferDocumentInfo> irrelevant = new ArrayList<>();
        List<ProgramTransferDocumentInfo> missing = new ArrayList<>();

        for (DocumentType type : oldRequired) {
            EnquiryDocument doc = uploadedByType.get(type);
            if (doc != null && newRequired.contains(type)) {
                retained.add(toDocInfo(doc));
            } else if (doc != null) {
                irrelevant.add(toDocInfo(doc));
            }
        }
        for (DocumentType type : newRequired) {
            if (!oldRequired.contains(type) || !uploadedByType.containsKey(type)) {
                EnquiryDocument doc = uploadedByType.get(type);
                if (doc == null || doc.getStatus() == DocumentVerificationStatus.NOT_UPLOADED) {
                    missing.add(new ProgramTransferDocumentInfo(
                        doc != null ? doc.getId() : null, type,
                        type.getDisplayName(), DocumentVerificationStatus.NOT_UPLOADED));
                }
            }
        }

        return new ProgramTransferAnalysis(
            studentId, student.getFullName(),
            oldProgram.getId(), oldProgram.getName(),
            newProgram.getId(), newProgram.getName(),
            retained, irrelevant, missing
        );
    }

    @Transactional
    public ProgramTransferRecord executeProgramTransfer(Long studentId, ProgramTransferRequest request) {
        if (!request.consentConfirmed()) {
            throw new IllegalArgumentException("Consent must be confirmed before executing program transfer");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Program newProgram = programRepository.findById(request.newProgramId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found: " + request.newProgramId()));
        Program oldProgram = student.getProgram();

        if (oldProgram.getId().equals(newProgram.getId())) {
            throw new IllegalArgumentException("New program must differ from the current program");
        }

        if (request.documentIdsToReturn() != null) {
            for (Long docId : request.documentIdsToReturn()) {
                EnquiryDocument doc = enquiryDocumentRepository.findById(docId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
                DocumentVerificationStatus prev = doc.getStatus();
                doc.setStatus(DocumentVerificationStatus.RETURNED);
                doc.setRemarks("Returned to student — program transfer to " + newProgram.getName());
                enquiryDocumentRepository.save(doc);

                EnquiryDocumentHistory hist = new EnquiryDocumentHistory();
                hist.setEnquiryDocument(doc);
                hist.setEnquiry(doc.getEnquiry());
                hist.setAdmission(doc.getAdmission());
                hist.setDocumentType(doc.getDocumentType());
                hist.setPreviousStatus(prev);
                hist.setNewStatus(DocumentVerificationStatus.RETURNED);
                hist.setFileName(doc.getFileName());
                hist.setFileSize(doc.getFileSize());
                hist.setContentType(doc.getContentType());
                hist.setRemarks(doc.getRemarks());
                hist.setChangedBy(currentUserResolver.resolve());
                documentHistoryRepository.save(hist);
            }
        }

        student.setProgram(newProgram);
        studentRepository.save(student);

        StudentProgramTransfer transfer = new StudentProgramTransfer();
        transfer.setStudent(student);
        transfer.setOldProgram(oldProgram);
        transfer.setNewProgram(newProgram);
        transfer.setTransferredAt(Instant.now());
        transfer.setTransferredBy(currentUserResolver.resolve());
        transfer.setConsentConfirmed(true);
        transfer.setNotes(request.notes());
        StudentProgramTransfer saved = transferRepository.save(transfer);

        return new ProgramTransferRecord(
            saved.getId(), studentId, student.getFullName(),
            oldProgram.getId(), oldProgram.getName(),
            newProgram.getId(), newProgram.getName(),
            saved.getTransferredAt(), saved.getTransferredBy(),
            saved.isConsentConfirmed(), saved.getNotes()
        );
    }

    public List<ProgramTransferRecord> getTransferHistory(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        return transferRepository.findByStudentIdOrderByTransferredAtDesc(studentId).stream()
            .map(t -> new ProgramTransferRecord(
                t.getId(), studentId, t.getStudent().getFullName(),
                t.getOldProgram().getId(), t.getOldProgram().getName(),
                t.getNewProgram().getId(), t.getNewProgram().getName(),
                t.getTransferredAt(), t.getTransferredBy(),
                t.isConsentConfirmed(), t.getNotes()
            ))
            .toList();
    }

    private ProgramTransferDocumentInfo toDocInfo(EnquiryDocument doc) {
        return new ProgramTransferDocumentInfo(
            doc.getId(), doc.getDocumentType(),
            doc.getDocumentType().getDisplayName(), doc.getStatus()
        );
    }
}
