package com.cms.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.ImportDefaultsRequest;
import com.cms.dto.ImportExecuteResult;
import com.cms.dto.ImportRowError;
import com.cms.dto.ImportValidationResult;
import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicQualification;
import com.cms.model.AcademicYear;
import com.cms.model.Admission;
import com.cms.model.Address;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.Cohort;
import com.cms.model.FeeState;
import com.cms.model.enums.AdmissionCategory;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;
import com.cms.model.enums.PaymentMode;
import com.cms.model.Enquiry;
import com.cms.model.ReferralType;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.QualificationType;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicQualificationRepository;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeStateRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.ReferralTypeRepository;
import com.cms.repository.StudentRepository;

@Service
public class StudentImportService {

    private static final DateTimeFormatter DATE_FMT_DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StudentRepository               studentRepo;
    private final AdmissionRepository             admissionRepo;
    private final ProgramRepository               programRepo;
    private final CourseRepository                courseRepo;
    private final AcademicYearRepository          academicYearRepo;
    private final AcademicQualificationRepository qualificationRepo;
    private final FeeFinalizationService          feeFinalizationService;
    private final PaymentCollectionService        paymentCollectionService;
    private final EnquiryRepository               enquiryRepo;
    private final ReferralTypeRepository          referralTypeRepo;
    private final CohortRepository                cohortRepo;
    private final FeeStateRepository              feeStateRepo;

    public StudentImportService(StudentRepository studentRepo,
                                 AdmissionRepository admissionRepo,
                                 ProgramRepository programRepo,
                                 CourseRepository courseRepo,
                                 AcademicYearRepository academicYearRepo,
                                 AcademicQualificationRepository qualificationRepo,
                                 FeeFinalizationService feeFinalizationService,
                                 PaymentCollectionService paymentCollectionService,
                                 EnquiryRepository enquiryRepo,
                                 ReferralTypeRepository referralTypeRepo,
                                 CohortRepository cohortRepo,
                                 FeeStateRepository feeStateRepo) {
        this.studentRepo             = studentRepo;
        this.admissionRepo           = admissionRepo;
        this.programRepo             = programRepo;
        this.courseRepo              = courseRepo;
        this.academicYearRepo        = academicYearRepo;
        this.qualificationRepo       = qualificationRepo;
        this.feeFinalizationService  = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
        this.enquiryRepo             = enquiryRepo;
        this.referralTypeRepo        = referralTypeRepo;
        this.cohortRepo              = cohortRepo;
        this.feeStateRepo            = feeStateRepo;
    }

    // ── Validate only (no DB writes) ─────────────────────────────────────────

    public ImportValidationResult validate(MultipartFile file,
                                            ImportDefaultsRequest defaults) throws Exception {
        try (InputStream is = file.getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is)) {

            List<ImportRowError> errors   = new ArrayList<>();
            List<ImportRowError> warnings = new ArrayList<>();

            ParseResult<StudentRow> students = parseStudentSheet(wb, defaults, errors, warnings, false);
            ParseResult<QualRow>    quals    = parseQualSheet(wb, errors, false);
            ParseResult<FeeRow>     fees     = parseFeeSheet(wb, errors, false);

            return new ImportValidationResult(
                students.total, students.valid,
                quals.total, quals.valid,
                fees.total, fees.valid,
                errors, warnings
            );
        }
    }

    // ── Execute import (writes to DB) ─────────────────────────────────────────

    @Transactional
    public ImportExecuteResult execute(MultipartFile file,
                                        ImportDefaultsRequest defaults,
                                        String performedBy) throws Exception {
        try (InputStream is = file.getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is)) {

            List<ImportRowError> errors = new ArrayList<>();
            List<ImportRowError> warnings = new ArrayList<>();
            boolean skipErrors = Boolean.TRUE.equals(defaults.skipErroredRows());

            // ── Pass 1: import students + admissions ──────────────────────
            ParseResult<StudentRow> studResult =
                parseStudentSheet(wb, defaults, errors, warnings, true);

            int studentsImported   = 0;
            int studentsSkipped    = 0;
            int admissionsCreated  = 0;
            Map<String, Student> emailToStudent = new HashMap<>();

            for (StudentRow row : studResult.rows) {
                if (row.hasError) { studentsSkipped++; continue; }
                try {
                    Student s = createStudent(row);
                    emailToStudent.put(s.getEmail(), s);
                    createAdmission(s, row, performedBy);
                    studentsImported++;
                    admissionsCreated++;
                } catch (Exception ex) {
                    errors.add(new ImportRowError("Students", row.rowNum, "—",
                        ex.getMessage(), "ERROR"));
                    studentsSkipped++;
                    if (!skipErrors) throw new RuntimeException("Import aborted: " + ex.getMessage(), ex);
                }
            }

            // ── Pass 2: qualifications ────────────────────────────────────
            ParseResult<QualRow> qualResult = parseQualSheet(wb, errors, true);
            int qualImported = 0;
            for (QualRow row : qualResult.rows) {
                if (row.hasError) continue;
                Student s = findOrLookupStudent(row.studentEmail, emailToStudent);
                if (s == null) {
                    errors.add(new ImportRowError("Qualifications", row.rowNum, "student_email",
                        "Student not found: " + row.studentEmail, "ERROR"));
                    continue;
                }
                Admission adm = admissionRepo.findByStudentId(s.getId()).orElse(null);
                if (adm == null) continue;
                try {
                    qualificationRepo.save(new AcademicQualification(
                        adm, row.type, row.schoolName, row.majorSubject,
                        row.totalMarks, row.percentage, row.monthYear, row.universityOrBoard));
                    qualImported++;
                } catch (Exception ex) {
                    errors.add(new ImportRowError("Qualifications", row.rowNum, "—",
                        ex.getMessage(), "ERROR"));
                }
            }

            // ── Pass 3: fee allocations + historical payments ─────────────
            ParseResult<FeeRow> feeResult = parseFeeSheet(wb, errors, true);
            int feeAllocCreated = 0;
            int paymentsImported = 0;
            Map<String, Boolean> allocatedEmails = new HashMap<>();

            for (FeeRow row : feeResult.rows) {
                if (row.hasError) continue;
                Student s = findOrLookupStudent(row.studentEmail, emailToStudent);
                if (s == null) {
                    errors.add(new ImportRowError("Fee History", row.rowNum, "student_email",
                        "Student not found: " + row.studentEmail, "ERROR"));
                    continue;
                }
                try {
                    // Create allocation once per student
                    if (!allocatedEmails.containsKey(row.studentEmail)
                            && !feeFinalizationService.allocationExists(s.getId())) {
                        createFeeAllocation(s, row, defaults, performedBy);
                        allocatedEmails.put(row.studentEmail, true);
                        feeAllocCreated++;
                    }
                    // Record historical payment if provided
                    if (row.amountPaid != null && row.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                        PaymentMode mode = safePaymentMode(row.paymentMode, "CASH");
                        paymentCollectionService.collectPayment(s.getId(),
                            new CollectPaymentRequest(row.amountPaid,
                                row.paymentDate != null ? row.paymentDate : LocalDate.now(),
                                mode,
                                row.receiptNumber,
                                row.remarks));
                        paymentsImported++;
                    }
                } catch (Exception ex) {
                    errors.add(new ImportRowError("Fee History", row.rowNum, "—",
                        ex.getMessage(), "ERROR"));
                    if (!skipErrors) throw new RuntimeException("Import aborted: " + ex.getMessage(), ex);
                }
            }

            return new ImportExecuteResult(
                studentsImported, studentsSkipped,
                admissionsCreated, qualImported,
                feeAllocCreated, paymentsImported,
                errors
            );
        }
    }

    // ── Student / Admission creation ──────────────────────────────────────────

    private Student createStudent(StudentRow row) {
        Student s = new Student(
            row.rollNumber,
            row.firstName, row.lastName, row.email,
            row.program,
            row.semester != null ? row.semester : 1,
            row.applicationDate != null ? row.applicationDate : LocalDate.now(),
            StudentStatus.ACTIVE
        );
        // Registration numbers
        s.setAdmissionNumber(row.admissionNumber);
        s.setUniversityRegistrationNumber(row.universityRegistrationNumber);
        s.setUmisNumber(row.umisNumber);

        // Contact
        s.setPhone(row.phone);

        // Academic
        s.setCourse(row.course);
        s.setCohort(row.cohort);
        s.setLabBatch(row.labBatch);

        // Admission classification
        if (!blank(row.admissionCategory)) {
            try { s.setAdmissionCategory(AdmissionCategory.valueOf(row.admissionCategory.trim().toUpperCase())); }
            catch (Exception ignored) {}
        }

        // Personal
        s.setDateOfBirth(row.dateOfBirth);
        if (!blank(row.gender)) {
            try { s.setGender(Gender.valueOf(row.gender.trim().toUpperCase())); } catch (Exception ignored) {}
        }
        s.setAadharNumber(row.aadharNumber);
        s.setNationality(row.nationality);
        s.setReligion(row.religion);
        s.setCommunityCategory(row.communityCategory);
        s.setCaste(row.caste);
        s.setBloodGroup(row.bloodGroup);
        s.setPhysicalDisability(Boolean.TRUE.equals(row.physicalDisability));

        // Family
        s.setFatherName(row.fatherName);
        s.setFatherPhone(row.fatherPhone);
        s.setFatherEmail(row.fatherEmail);
        s.setMotherName(row.motherName);
        s.setMotherPhone(row.motherPhone);
        s.setMotherEmail(row.motherEmail);
        s.setParentMobile(row.parentMobile);
        s.setFirstGraduate(Boolean.TRUE.equals(row.isFirstGraduate));
        s.setFatherEducation(row.fatherEducation);
        s.setMotherEducation(row.motherEducation);

        // Emergency contact
        s.setEmergencyContactName(row.emergencyContactName);
        s.setEmergencyContactRelationship(row.emergencyContactRelationship);
        s.setEmergencyContactPhone(row.emergencyContactPhone);

        // Bio (truncate to 500 chars silently)
        if (!blank(row.bio)) {
            s.setBio(row.bio.length() > 500 ? row.bio.substring(0, 500) : row.bio);
        }

        // Address
        if (hasAnyAddress(row)) {
            s.setAddress(new Address(1L, row.postalAddress, row.street, row.city,
                                      row.district, row.state, row.pincode));
        }
        return studentRepo.save(s);
    }

    private void createAdmission(Student s, StudentRow row, String performedBy) {
        // Every admission must originate from an enquiry. For imports, create a
        // synthetic ADMITTED enquiry representing the historical pre-system record.
        ReferralType walkIn = referralTypeRepo.findByCode("WALK_IN")
            .orElseThrow(() -> new IllegalStateException(
                "WALK_IN referral type not found. Ensure seed data is present."));

        LocalDate appDate = row.applicationDate != null ? row.applicationDate : LocalDate.now();

        Enquiry enquiry = new Enquiry();
        enquiry.setName(s.getFirstName() + " " + s.getLastName());
        enquiry.setEmail(s.getEmail());
        enquiry.setPhone(s.getPhone());
        enquiry.setProgram(s.getProgram());
        enquiry.setCourse(s.getCourse());
        enquiry.setEnquiryDate(appDate);
        enquiry.setStatus(EnquiryStatus.ADMITTED);
        enquiry.setDateOfBirth(s.getDateOfBirth() != null ? s.getDateOfBirth() : LocalDate.of(2000, 1, 1));
        enquiry.setGender(s.getGender());
        enquiry.setReferralType(walkIn);
        enquiry.setConvertedStudentId(s.getId());
        if (!blank(row.admissionCategory)) {
            try { enquiry.setAdmissionQuota(AdmissionQuota.valueOf(row.admissionCategory.trim().toUpperCase())); }
            catch (Exception ignored) {}
        }
        // Infer fee state from student's home state: Tamil Nadu → TN slab; anything else → fallback slab.
        // This ensures the enquiry is correctly bucketed for fee structure group reporting.
        String addressState = s.getAddress() != null ? s.getAddress().getState() : null;
        FeeState feeState = resolveFeeState(addressState);
        if (feeState != null) {
            enquiry.setFeeState(feeState);
        }
        enquiry.setRemarks("Auto-created during bulk import");
        Enquiry savedEnquiry = enquiryRepo.save(enquiry);

        AcademicYear ay = row.joiningAcademicYear;
        Admission adm = new Admission(s, ay, appDate);
        adm.setEnquiryId(savedEnquiry.getId());
        admissionRepo.save(adm);
    }

    private void createFeeAllocation(Student s, FeeRow row,
                                      ImportDefaultsRequest defaults,
                                      String performedBy) {
        int durationYears = s.getProgram() != null && s.getProgram().getDurationYears() != null
            ? s.getProgram().getDurationYears() : 1;

        BigDecimal totalFee = row.totalFee;
        BigDecimal discount = row.discountAmount != null ? row.discountAmount : BigDecimal.ZERO;

        List<StudentFeeAllocationRequest.YearFee> yearFees = buildYearFees(row, durationYears);

        feeFinalizationService.finalize(
            new StudentFeeAllocationRequest(s.getId(), totalFee, discount,
                row.discountReason, BigDecimal.ZERO, yearFees),
            performedBy);
    }

    private List<StudentFeeAllocationRequest.YearFee> buildYearFees(FeeRow row, int durationYears) {
        BigDecimal[] provided = {
            row.year1Fee, row.year2Fee, row.year3Fee,
            row.year4Fee, row.year5Fee, row.year6Fee
        };

        // Check if the admin supplied at least one year-wise amount
        boolean hasYearFees = false;
        for (BigDecimal amt : provided) {
            if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) { hasYearFees = true; break; }
        }

        List<StudentFeeAllocationRequest.YearFee> yearFees = new ArrayList<>();

        if (hasYearFees) {
            // Use the exact amounts provided; include each year that has a value > 0.
            // This intentionally supports students whose per-year fee differed across
            // academic years (e.g. 2020-21 batch vs 2023-24 batch different fee slabs).
            for (int i = 0; i < provided.length; i++) {
                if (provided[i] != null && provided[i].compareTo(BigDecimal.ZERO) > 0) {
                    yearFees.add(new StudentFeeAllocationRequest.YearFee(i + 1, provided[i]));
                }
            }
        } else {
            // Fallback: split net fee evenly across programme duration
            BigDecimal netFee  = row.totalFee.subtract(
                row.discountAmount != null ? row.discountAmount : BigDecimal.ZERO);
            BigDecimal perYear = netFee.divide(
                BigDecimal.valueOf(durationYears), 2, java.math.RoundingMode.HALF_UP);
            for (int y = 1; y <= durationYears; y++) {
                yearFees.add(new StudentFeeAllocationRequest.YearFee(y, perYear));
            }
        }

        return yearFees;
    }

    // ── Sheet parsers ─────────────────────────────────────────────────────────

    private ParseResult<StudentRow> parseStudentSheet(XSSFWorkbook wb,
                                                        ImportDefaultsRequest def,
                                                        List<ImportRowError> errors,
                                                        List<ImportRowError> warnings,
                                                        boolean resolveRefs) {
        XSSFSheet sheet = wb.getSheet("Students");
        if (sheet == null) {
            errors.add(new ImportRowError("Students", 0, "sheet", "Sheet 'Students' not found", "ERROR"));
            return new ParseResult<>(new ArrayList<>(), 0, 0);
        }

        AcademicYear defaultYear = def.defaultJoiningAcademicYearId() != null
            ? academicYearRepo.findById(def.defaultJoiningAcademicYearId()).orElse(null)
            : academicYearRepo.findByIsCurrentTrue().orElse(null);

        List<StudentRow> rows = new ArrayList<>();
        int total = 0, valid = 0;
        Map<String, Integer> header = headerMap(sheet.getRow(0));

        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || isBlankRow(r)) continue;
            total++;
            StudentRow sr = new StudentRow();
            sr.rowNum = i + 1;

            sr.firstName  = str(r, header, "first_name");
            sr.lastName   = str(r, header, "last_name");
            sr.email      = str(r, header, "email");
            sr.phone      = str(r, header, "phone");
            // Strip "CODE — Name" display format if user copied from the display column
            String programCode = codeOnly(str(r, header, "program_code"));
            String courseCode  = codeOnly(str(r, header, "course_code"));
            String yearName    = str(r, header, "joining_academic_year");

            // Required fields
            if (blank(sr.firstName)) { error(errors, "Students", sr.rowNum, "first_name", "First name required"); sr.hasError = true; }
            if (blank(sr.lastName))  { error(errors, "Students", sr.rowNum, "last_name",  "Last name required");  sr.hasError = true; }
            if (blank(sr.email))     { error(errors, "Students", sr.rowNum, "email",       "Email required");       sr.hasError = true; }
            if (blank(programCode))  { error(errors, "Students", sr.rowNum, "program_code","Program code required"); sr.hasError = true; }

            if (!sr.hasError && resolveRefs) {
                if (studentRepo.existsByEmail(sr.email)) {
                    error(errors, "Students", sr.rowNum, "email", "Email already exists: " + sr.email);
                    sr.hasError = true;
                }
            }

            if (!blank(programCode) && resolveRefs) {
                Optional<Program> prog = programRepo.findByCode(programCode.trim());
                if (prog.isEmpty()) { error(errors, "Students", sr.rowNum, "program_code", "Program not found: " + programCode); sr.hasError = true; }
                else sr.program = prog.get();
            }
            if (!blank(courseCode) && resolveRefs) {
                courseRepo.findByCode(courseCode.trim()).ifPresent(c -> sr.course = c);
            }

            // Academic year
            if (!blank(yearName) && resolveRefs) {
                Optional<AcademicYear> ay = academicYearRepo.findByName(yearName.trim());
                if (ay.isEmpty()) warn(warnings, "Students", sr.rowNum, "joining_academic_year",
                    "Academic year not found '" + yearName + "', using default");
                else sr.joiningAcademicYear = ay.get();
            }
            if (sr.joiningAcademicYear == null) sr.joiningAcademicYear = defaultYear;
            if (sr.joiningAcademicYear == null) {
                error(errors, "Students", sr.rowNum, "joining_academic_year", "No academic year available");
                sr.hasError = true;
            }

            // ── Cohort lookup — requires course + academic year ───────────
            // Cohort must already exist before import (create via Masters → Cohorts).
            if (resolveRefs && !sr.hasError && sr.joiningAcademicYear != null) {
                if (sr.course != null) {
                    Optional<Cohort> cohort = cohortRepo.findByCourseIdAndAdmissionAcademicYearId(
                        sr.course.getId(), sr.joiningAcademicYear.getId());
                    if (cohort.isEmpty()) {
                        error(errors, "Students", sr.rowNum, "course_code",
                            "No cohort found for course '" + sr.course.getCode()
                            + "' in academic year '" + sr.joiningAcademicYear.getName()
                            + "'. Create the cohort first under Masters → Cohorts, then re-import.");
                        sr.hasError = true;
                    } else {
                        sr.cohort = cohort.get();
                    }
                } else {
                    // Course not provided — cohort cannot be resolved
                    warn(errors, "Students", sr.rowNum, "course_code",
                        "No course_code provided — cohort cannot be assigned. Student will have cohort = null.");
                }
            }

            sr.applicationDate   = date(r, header, "application_date");
            sr.studentType       = coalesce(str(r, header, "student_type"),     def.defaultStudentType());
            sr.semester          = intVal(r, header, "semester", def.defaultSemester() != null ? def.defaultSemester() : 1);
            sr.dateOfBirth       = date(r, header, "date_of_birth");
            sr.gender            = str(r, header, "gender");
            sr.aadharNumber      = str(r, header, "aadhar_number");
            sr.nationality       = coalesce(str(r, header, "nationality"), def.defaultNationality());
            sr.religion          = str(r, header, "religion");
            sr.communityCategory = str(r, header, "community_category");
            sr.caste             = str(r, header, "caste");
            sr.bloodGroup        = str(r, header, "blood_group");
            sr.fatherName        = str(r, header, "father_name");
            sr.motherName        = str(r, header, "mother_name");
            sr.parentMobile      = str(r, header, "parent_mobile");
            sr.postalAddress     = str(r, header, "postal_address");
            sr.street            = str(r, header, "street");
            sr.city              = str(r, header, "city");
            sr.district          = str(r, header, "district");
            sr.state             = coalesce(str(r, header, "state"), def.defaultState());
            sr.pincode           = str(r, header, "pincode");

            // ── Registration numbers (unique conflict checks against DB) ──
            sr.admissionNumber              = str(r, header, "admission_number");
            sr.rollNumber                   = str(r, header, "roll_number");
            sr.universityRegistrationNumber = str(r, header, "university_registration_number");
            sr.umisNumber                   = str(r, header, "umis_number");

            if (resolveRefs) {
                if (!blank(sr.admissionNumber) && studentRepo.existsByAdmissionNumber(sr.admissionNumber)) {
                    error(errors, "Students", sr.rowNum, "admission_number",
                        "Admission number already exists: " + sr.admissionNumber);
                    sr.hasError = true;
                }
                if (!blank(sr.rollNumber) && studentRepo.existsByRollNumber(sr.rollNumber)) {
                    error(errors, "Students", sr.rowNum, "roll_number",
                        "Roll number already exists: " + sr.rollNumber);
                    sr.hasError = true;
                }
                if (!blank(sr.universityRegistrationNumber)
                        && studentRepo.existsByUniversityRegistrationNumber(sr.universityRegistrationNumber)) {
                    error(errors, "Students", sr.rowNum, "university_registration_number",
                        "University registration number already exists: " + sr.universityRegistrationNumber);
                    sr.hasError = true;
                }
                if (!blank(sr.umisNumber) && studentRepo.existsByUmisNumber(sr.umisNumber)) {
                    error(errors, "Students", sr.rowNum, "umis_number",
                        "UMIS number already exists: " + sr.umisNumber);
                    sr.hasError = true;
                }
            }

            // ── Admission category (default from Step 2) ──────────────────
            sr.admissionCategory = coalesce(str(r, header, "admission_category"),
                def.defaultAdmissionCategory(), "MANAGEMENT");
            if (!blank(sr.admissionCategory)) {
                try { AdmissionCategory.valueOf(sr.admissionCategory.trim().toUpperCase()); }
                catch (Exception e) {
                    warn(errors, "Students", sr.rowNum, "admission_category",
                        "Invalid admission_category '" + sr.admissionCategory + "', defaulting to MANAGEMENT");
                    sr.admissionCategory = "MANAGEMENT";
                }
            }

            // ── Extended family contacts ───────────────────────────────────
            sr.fatherPhone = str(r, header, "father_phone");
            sr.fatherEmail = str(r, header, "father_email");
            sr.motherPhone = str(r, header, "mother_phone");
            sr.motherEmail = str(r, header, "mother_email");

            // ── Scholarship / socioeconomic ───────────────────────────────
            sr.isFirstGraduate  = strictBoolean(r, header, "is_first_graduate",  errors, "Students", sr.rowNum);
            sr.fatherEducation  = str(r, header, "father_education");
            sr.motherEducation  = str(r, header, "mother_education");

            // ── Medical ───────────────────────────────────────────────────
            sr.physicalDisability = strictBoolean(r, header, "physical_disability", errors, "Students", sr.rowNum);

            // ── Emergency contact ─────────────────────────────────────────
            sr.emergencyContactName         = str(r, header, "emergency_contact_name");
            sr.emergencyContactRelationship = str(r, header, "emergency_contact_relationship");
            sr.emergencyContactPhone        = str(r, header, "emergency_contact_phone");

            // ── Academic placement / profile ──────────────────────────────
            sr.labBatch = str(r, header, "lab_batch");
            sr.bio      = str(r, header, "bio");

            rows.add(sr);
            if (!sr.hasError) valid++;
        }
        return new ParseResult<>(rows, total, valid);
    }

    private ParseResult<QualRow> parseQualSheet(XSSFWorkbook wb,
                                                  List<ImportRowError> errors,
                                                  boolean resolveRefs) {
        XSSFSheet sheet = wb.getSheet("Qualifications");
        if (sheet == null) return new ParseResult<>(new ArrayList<>(), 0, 0);

        List<QualRow> rows = new ArrayList<>();
        int total = 0, valid = 0;
        Map<String, Integer> header = headerMap(sheet.getRow(0));

        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || isBlankRow(r)) continue;
            total++;
            QualRow qr = new QualRow();
            qr.rowNum = i + 1;

            qr.studentEmail = str(r, header, "student_email");
            String typeStr  = str(r, header, "qualification_type");

            if (blank(qr.studentEmail)) { error(errors, "Qualifications", qr.rowNum, "student_email", "Email required"); qr.hasError = true; }
            if (blank(typeStr)) { error(errors, "Qualifications", qr.rowNum, "qualification_type", "Type required"); qr.hasError = true; }

            if (!blank(typeStr)) {
                try { qr.type = QualificationType.valueOf(typeStr.trim().toUpperCase()); }
                catch (Exception e) { error(errors, "Qualifications", qr.rowNum, "qualification_type", "Invalid type: " + typeStr); qr.hasError = true; }
            }

            qr.schoolName       = str(r, header, "school_name");
            qr.majorSubject     = str(r, header, "major_subject");
            qr.totalMarks       = intOrNull(r, header, "total_marks");
            qr.percentage       = decimal(r, header, "percentage");
            qr.monthYear        = str(r, header, "month_year_of_passing");
            qr.universityOrBoard = str(r, header, "university_or_board");

            rows.add(qr);
            if (!qr.hasError) valid++;
        }
        return new ParseResult<>(rows, total, valid);
    }

    private ParseResult<FeeRow> parseFeeSheet(XSSFWorkbook wb,
                                               List<ImportRowError> errors,
                                               boolean resolveRefs) {
        XSSFSheet sheet = wb.getSheet("Fee History");
        if (sheet == null) return new ParseResult<>(new ArrayList<>(), 0, 0);

        List<FeeRow> rows = new ArrayList<>();
        int total = 0, valid = 0;
        Map<String, Integer> header = headerMap(sheet.getRow(0));

        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null || isBlankRow(r)) continue;
            total++;
            FeeRow fr = new FeeRow();
            fr.rowNum = i + 1;

            fr.studentEmail    = str(r, header, "student_email");
            fr.totalFee        = decimal(r, header, "total_fee");
            fr.discountAmount  = decimal(r, header, "discount_amount");
            fr.discountReason  = str(r, header, "discount_reason");
            fr.amountPaid      = decimal(r, header, "amount_paid");
            fr.paymentDate     = date(r, header, "payment_date");
            fr.paymentMode     = str(r, header, "payment_mode");
            fr.receiptNumber   = str(r, header, "receipt_number");
            fr.remarks         = str(r, header, "remarks");
            // Year-wise fee breakdown (optional — all blank = even split)
            fr.year1Fee = decimal(r, header, "year_1_fee");
            fr.year2Fee = decimal(r, header, "year_2_fee");
            fr.year3Fee = decimal(r, header, "year_3_fee");
            fr.year4Fee = decimal(r, header, "year_4_fee");
            fr.year5Fee = decimal(r, header, "year_5_fee");
            fr.year6Fee = decimal(r, header, "year_6_fee");

            if (blank(fr.studentEmail)) { error(errors, "Fee History", fr.rowNum, "student_email", "Email required"); fr.hasError = true; }
            if (fr.totalFee == null) { error(errors, "Fee History", fr.rowNum, "total_fee", "Total fee required"); fr.hasError = true; }

            if (fr.totalFee != null && fr.amountPaid != null
                    && fr.amountPaid.compareTo(fr.totalFee) > 0) {
                error(errors, "Fee History", fr.rowNum, "amount_paid", "Amount paid exceeds total fee");
                fr.hasError = true;
            }

            // Warn if year-wise fees are provided but their sum doesn't match net fee
            if (!fr.hasError && fr.totalFee != null) {
                BigDecimal[] yf = { fr.year1Fee, fr.year2Fee, fr.year3Fee,
                                    fr.year4Fee, fr.year5Fee, fr.year6Fee };
                BigDecimal yearSum = BigDecimal.ZERO;
                boolean hasAny = false;
                for (BigDecimal v : yf) {
                    if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                        yearSum = yearSum.add(v);
                        hasAny = true;
                    }
                }
                if (hasAny) {
                    BigDecimal netFee = fr.totalFee.subtract(
                        fr.discountAmount != null ? fr.discountAmount : BigDecimal.ZERO);
                    if (yearSum.compareTo(netFee) != 0) {
                        warn(errors, "Fee History", fr.rowNum, "year_fees",
                            "Year-wise fee sum " + yearSum + " does not equal net fee " + netFee
                            + " (total " + fr.totalFee + " - discount "
                            + (fr.discountAmount != null ? fr.discountAmount : BigDecimal.ZERO) + ")");
                    }
                }
            }

            rows.add(fr);
            if (!fr.hasError) valid++;
        }
        return new ParseResult<>(rows, total, valid);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Student findOrLookupStudent(String email, Map<String, Student> cache) {
        if (cache.containsKey(email)) return cache.get(email);
        return studentRepo.findByEmail(email).orElse(null);
    }

    private Map<String, Integer> headerMap(Row hdrRow) {
        Map<String, Integer> map = new HashMap<>();
        if (hdrRow == null) return map;
        for (int i = 0; i < hdrRow.getLastCellNum(); i++) {
            Cell c = hdrRow.getCell(i);
            if (c != null) {
                String key = c.getStringCellValue()
                    .toLowerCase()
                    .replace(" *", "")
                    .trim()
                    .replace(" ", "_");
                map.put(key, i);
            }
        }
        return map;
    }

    private String str(Row row, Map<String, Integer> header, String col) {
        Integer idx = header.get(col);
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        String val = switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default      -> null;
        };
        return blank(val) ? null : val;
    }

    private LocalDate date(Row row, Map<String, Integer> header, String col) {
        String s = str(row, header, col);
        if (s == null) return null;
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{DATE_FMT_DMY, DATE_FMT_ISO}) {
            try { return LocalDate.parse(s, fmt); } catch (DateTimeParseException ignored) {}
        }
        // Try POI date
        Integer idx = header.get(col);
        if (idx != null) {
            Cell c = row.getCell(idx);
            if (c != null && c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                return c.getLocalDateTimeCellValue().toLocalDate();
            }
        }
        return null;
    }

    private BigDecimal decimal(Row row, Map<String, Integer> header, String col) {
        Integer idx = header.get(col);
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        try {
            if (c.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(c.getNumericCellValue());
            String s = c.getStringCellValue().trim();
            return s.isEmpty() ? null : new BigDecimal(s);
        } catch (Exception e) { return null; }
    }

    private Integer intVal(Row row, Map<String, Integer> header, String col, int defaultVal) {
        Integer idx = header.get(col);
        if (idx == null) return defaultVal;
        Cell c = row.getCell(idx);
        if (c == null) return defaultVal;
        try {
            if (c.getCellType() == CellType.NUMERIC) return (int) c.getNumericCellValue();
            return Integer.parseInt(c.getStringCellValue().trim());
        } catch (Exception e) { return defaultVal; }
    }

    private Integer intOrNull(Row row, Map<String, Integer> header, String col) {
        BigDecimal bd = decimal(row, header, col);
        return bd != null ? bd.intValue() : null;
    }

    private PaymentMode safePaymentMode(String value, String fallback) {
        if (value == null) value = fallback;
        try { return PaymentMode.valueOf(value.toUpperCase().trim()); }
        catch (Exception e) { return PaymentMode.valueOf(fallback); }
    }

    private boolean isBlankRow(Row r) {
        for (int i = r.getFirstCellNum(); i < r.getLastCellNum(); i++) {
            Cell c = r.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK
                    && !c.getStringCellValue().isBlank()) return false;
        }
        return true;
    }

    /** Strips the " — Name" display suffix if the user copied from the display column. */
    private String codeOnly(String value) {
        if (value == null) return null;
        int dash = value.indexOf(" — ");
        return dash > 0 ? value.substring(0, dash).trim() : value.trim();
    }

    /**
     * Parses a boolean cell strictly — only "TRUE" or "FALSE" (case-insensitive) are accepted.
     * Returns null (not an error) when the cell is blank.
     * Adds a WARNING (not an error) when a non-blank unrecognised value is found so the
     * student row is not rejected but the admin is notified.
     */
    private Boolean strictBoolean(Row row, Map<String, Integer> header, String col,
                                   List<ImportRowError> errors, String sheet, int rowNum) {
        String val = str(row, header, col);
        if (val == null) return null;
        String upper = val.trim().toUpperCase();
        if ("TRUE".equals(upper))  return Boolean.TRUE;
        if ("FALSE".equals(upper)) return Boolean.FALSE;
        warn(errors, sheet, rowNum, col,
            "Invalid boolean value '" + val + "' for " + col + " — expected TRUE or FALSE. Defaulting to FALSE.");
        return Boolean.FALSE;
    }

    /**
     * Infers a FeeState from the student's address state string.
     * Matches by name (case-insensitive, exact). Falls back to the fee state
     * flagged isFallback=true (e.g. "Other State") when no name match is found.
     * Returns null only when the fee_states table is empty.
     */
    private FeeState resolveFeeState(String addressState) {
        List<FeeState> all = feeStateRepo.findByIsActiveTrueOrderBySortOrderAsc();
        if (!blank(addressState)) {
            String normalized = addressState.trim().toLowerCase();
            for (FeeState fs : all) {
                if (!fs.isFallback() && fs.getName().trim().toLowerCase().equals(normalized)) {
                    return fs;
                }
            }
        }
        return all.stream().filter(FeeState::isFallback).findFirst().orElse(null);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private String coalesce(String... vals) {
        for (String v : vals) if (!blank(v)) return v;
        return null;
    }

    private boolean hasAnyAddress(StudentRow r) {
        return !blank(r.postalAddress) || !blank(r.street) || !blank(r.city)
            || !blank(r.district) || !blank(r.state) || !blank(r.pincode);
    }

    private void error(List<ImportRowError> list, String sheet, int row, String col, String msg) {
        list.add(new ImportRowError(sheet, row, col, msg, "ERROR"));
    }

    private void warn(List<ImportRowError> list, String sheet, int row, String col, String msg) {
        list.add(new ImportRowError(sheet, row, col, msg, "WARNING"));
    }

    // ── Internal row models ───────────────────────────────────────────────────

    private static class StudentRow {
        int rowNum; boolean hasError;

        // Core
        String firstName, lastName, email, phone;

        // Academic
        Program program; Course course; AcademicYear joiningAcademicYear; Cohort cohort;
        LocalDate applicationDate; String studentType; Integer semester;

        // Personal / demographics
        LocalDate dateOfBirth; String gender; String aadharNumber;
        String nationality, religion, communityCategory, caste, bloodGroup;

        // Family (basic)
        String fatherName, motherName, parentMobile;

        // Address
        String postalAddress, street, city, district, state, pincode;

        // Registration numbers
        String admissionNumber, rollNumber, universityRegistrationNumber, umisNumber;

        // Admission classification
        String admissionCategory;

        // Extended family contacts
        String fatherPhone, fatherEmail, motherPhone, motherEmail;

        // Scholarship / socioeconomic
        Boolean isFirstGraduate;
        String fatherEducation, motherEducation;

        // Medical
        Boolean physicalDisability;

        // Emergency contact
        String emergencyContactName, emergencyContactRelationship, emergencyContactPhone;

        // Placement / profile
        String labBatch, bio;
    }

    private static class QualRow {
        int rowNum; boolean hasError;
        String studentEmail; QualificationType type;
        String schoolName, majorSubject; Integer totalMarks;
        BigDecimal percentage; String monthYear, universityOrBoard;
    }

    private static class FeeRow {
        int rowNum; boolean hasError;
        String studentEmail; BigDecimal totalFee, discountAmount;
        String discountReason; BigDecimal amountPaid;
        LocalDate paymentDate; String paymentMode, receiptNumber, remarks;
        // Year-wise fee breakdown — if provided, used instead of even split
        BigDecimal year1Fee, year2Fee, year3Fee, year4Fee, year5Fee, year6Fee;
    }

    private record ParseResult<T>(List<T> rows, int total, int valid) {}
}
