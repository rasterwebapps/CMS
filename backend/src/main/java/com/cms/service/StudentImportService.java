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
import com.cms.model.enums.Gender;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.QualificationType;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicQualificationRepository;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.ProgramRepository;
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

    public StudentImportService(StudentRepository studentRepo,
                                 AdmissionRepository admissionRepo,
                                 ProgramRepository programRepo,
                                 CourseRepository courseRepo,
                                 AcademicYearRepository academicYearRepo,
                                 AcademicQualificationRepository qualificationRepo,
                                 FeeFinalizationService feeFinalizationService,
                                 PaymentCollectionService paymentCollectionService) {
        this.studentRepo             = studentRepo;
        this.admissionRepo           = admissionRepo;
        this.programRepo             = programRepo;
        this.courseRepo              = courseRepo;
        this.academicYearRepo        = academicYearRepo;
        this.qualificationRepo       = qualificationRepo;
        this.feeFinalizationService  = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
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
            null,
            row.firstName, row.lastName, row.email,
            row.program,
            row.semester != null ? row.semester : 1,
            row.applicationDate != null ? row.applicationDate : LocalDate.now(),
            StudentStatus.ACTIVE
        );
        s.setPhone(row.phone);
        s.setCourse(row.course);
        s.setDateOfBirth(row.dateOfBirth);
        if (row.gender != null) {
            try { s.setGender(Gender.valueOf(row.gender)); } catch (Exception ignored) {}
        }
        s.setAadharNumber(row.aadharNumber);
        s.setNationality(row.nationality);
        s.setReligion(row.religion);
        s.setCommunityCategory(row.communityCategory);
        s.setCaste(row.caste);
        s.setBloodGroup(row.bloodGroup);
        s.setFatherName(row.fatherName);
        s.setMotherName(row.motherName);
        s.setParentMobile(row.parentMobile);
        if (hasAnyAddress(row)) {
            s.setAddress(new Address(row.postalAddress, row.street, row.city,
                                      row.district, row.state, row.pincode));
        }
        return studentRepo.save(s);
    }

    private void createAdmission(Student s, StudentRow row, String performedBy) {
        AcademicYear ay = row.joiningAcademicYear;
        Admission adm = new Admission(s, ay,
            row.applicationDate != null ? row.applicationDate : LocalDate.now());
        admissionRepo.save(adm);
    }

    private void createFeeAllocation(Student s, FeeRow row,
                                      ImportDefaultsRequest defaults,
                                      String performedBy) {
        int durationYears = s.getProgram() != null && s.getProgram().getDurationYears() != null
            ? s.getProgram().getDurationYears() : 1;

        BigDecimal totalFee = row.totalFee;
        BigDecimal discount = row.discountAmount != null ? row.discountAmount : BigDecimal.ZERO;
        BigDecimal perYear  = totalFee.subtract(discount)
            .divide(BigDecimal.valueOf(durationYears), 2, java.math.RoundingMode.HALF_UP);

        LocalDate baseDate = LocalDate.now();
        List<StudentFeeAllocationRequest.YearFee> yearFees = new ArrayList<>();
        for (int y = 1; y <= durationYears; y++) {
            LocalDate due = baseDate.plusMonths((long) (y - 1) * 12);
            yearFees.add(new StudentFeeAllocationRequest.YearFee(y, perYear, due));
        }

        feeFinalizationService.finalize(
            new StudentFeeAllocationRequest(s.getId(), totalFee, discount,
                row.discountReason, BigDecimal.ZERO, yearFees),
            performedBy);
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

            if (blank(fr.studentEmail)) { error(errors, "Fee History", fr.rowNum, "student_email", "Email required"); fr.hasError = true; }
            if (fr.totalFee == null) { error(errors, "Fee History", fr.rowNum, "total_fee", "Total fee required"); fr.hasError = true; }

            if (fr.totalFee != null && fr.amountPaid != null
                    && fr.amountPaid.compareTo(fr.totalFee) > 0) {
                error(errors, "Fee History", fr.rowNum, "amount_paid", "Amount paid exceeds total fee");
                fr.hasError = true;
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
        String firstName, lastName, email, phone;
        Program program; Course course; AcademicYear joiningAcademicYear;
        LocalDate applicationDate; String studentType; Integer semester;
        LocalDate dateOfBirth; String gender; String aadharNumber;
        String nationality, religion, communityCategory, caste, bloodGroup;
        String fatherName, motherName, parentMobile;
        String postalAddress, street, city, district, state, pincode;
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
    }

    private record ParseResult<T>(List<T> rows, int total, int valid) {}
}
