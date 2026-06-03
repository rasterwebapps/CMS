package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class ExcelTemplateService {

    private final ProgramRepository      programRepository;
    private final CourseRepository       courseRepository;
    private final AcademicYearRepository academicYearRepository;

    public ExcelTemplateService(ProgramRepository programRepository,
                                 CourseRepository courseRepository,
                                 AcademicYearRepository academicYearRepository) {
        this.programRepository     = programRepository;
        this.courseRepository      = courseRepository;
        this.academicYearRepository = academicYearRepository;
    }

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Pre-load master data once — shared by reference sheet and sample rows
            List<Program>      programs = programRepository.findAll();
            List<Course>       courses  = courseRepository.findAll();
            List<AcademicYear> years    = academicYearRepository.findAll();

            XSSFSheet ref   = wb.createSheet("Reference");
            XSSFSheet stud  = wb.createSheet("Students");
            XSSFSheet qual  = wb.createSheet("Qualifications");
            XSSFSheet fees  = wb.createSheet("Fee History");

            XSSFCellStyle reqStyle    = reqHeaderStyle(wb);
            XSSFCellStyle optStyle    = optHeaderStyle(wb);
            XSSFCellStyle typeStyle   = typeRowStyle(wb);
            XSSFCellStyle sampleStyle = sampleDataStyle(wb);

            // ── Reference sheet ──────────────────────────────────────────
            populateReferenceSheet(wb, ref, programs, courses, years);

            // ── Students sheet ───────────────────────────────────────────
            // Col indices 0-25: existing fields
            // Col indices 26-43: new fields added for full legacy data migration
            String[] studHeaders = {
                // ── Core identity (required) ──────────────────────────────
                "first_name","last_name","email","phone",
                // ── Academic programme ────────────────────────────────────
                "program_code","course_code","joining_academic_year",
                "application_date","student_type",
                // ── Personal / demographics ───────────────────────────────
                "date_of_birth","gender","aadhar_number",
                "nationality","religion","community_category","caste","blood_group",
                // ── Family (basic) ────────────────────────────────────────
                "father_name","mother_name","parent_mobile",
                // ── Address ───────────────────────────────────────────────
                "postal_address","street","city","district","state","pincode",
                // ── Registration numbers ──────────────────────────────────
                "admission_number","roll_number",
                "university_registration_number","umis_number",
                // ── Admission classification ──────────────────────────────
                "admission_category",
                // ── Extended family contacts ──────────────────────────────
                "father_phone","father_email",
                "mother_phone","mother_email",
                // ── Scholarship / socioeconomic ───────────────────────────
                "is_first_graduate",
                "father_education","mother_education",
                // ── Medical / disability ──────────────────────────────────
                "physical_disability",
                // ── Emergency contact ─────────────────────────────────────
                "emergency_contact_name","emergency_contact_relationship",
                "emergency_contact_phone",
                // ── Academic placement ────────────────────────────────────
                "lab_batch",
                // ── Profile ───────────────────────────────────────────────
                "bio"
            };
            boolean[] studReq = {
                // first_name, last_name, email, phone
                true,true,true,false,
                // program_code, course_code, joining_academic_year, application_date, student_type
                true,false,false,false,false,
                // date_of_birth, gender, aadhar_number
                false,false,false,
                // nationality, religion, community_category, caste, blood_group
                false,false,false,false,false,
                // father_name, mother_name, parent_mobile
                false,false,false,
                // postal_address, street, city, district, state, pincode
                false,false,false,false,false,false,
                // admission_number, roll_number, university_registration_number, umis_number
                false,false,false,false,
                // admission_category
                false,
                // father_phone, father_email, mother_phone, mother_email
                false,false,false,false,
                // is_first_graduate
                false,
                // father_education, mother_education
                false,false,
                // physical_disability
                false,
                // emergency_contact_name, emergency_contact_relationship, emergency_contact_phone
                false,false,false,
                // lab_batch
                false,
                // bio
                false
            };
            String[] studTypes = {
                // first_name, last_name, email, phone
                "Text","Text","Email","Text",
                // program_code, course_code, joining_academic_year, application_date, student_type
                "Code (see Reference)","Code (see Reference)","Text e.g. 2024-25",
                "Date DD-MM-YYYY","Enum (see Reference)",
                // date_of_birth, gender, aadhar_number
                "Date DD-MM-YYYY","Enum (see Reference)","Text 12 digits",
                // nationality, religion, community_category, caste, blood_group
                "Text","Text","Enum (see Reference)","Text","Enum (see Reference)",
                // father_name, mother_name, parent_mobile
                "Text","Text","Text",
                // postal_address, street, city, district, state, pincode
                "Text","Text","Text","Text","Text","Text",
                // admission_number, roll_number, university_registration_number, umis_number
                "Text (from old system)","Text (from old system)",
                "Text (university-issued)","Text (UMIS-issued)",
                // admission_category
                "Enum (see Reference)",
                // father_phone, father_email, mother_phone, mother_email
                "Text","Email","Text","Email",
                // is_first_graduate
                "TRUE or FALSE",
                // father_education, mother_education
                "Enum (see Reference)","Enum (see Reference)",
                // physical_disability
                "TRUE or FALSE",
                // emergency_contact_name, emergency_contact_relationship, emergency_contact_phone
                "Text","Text e.g. Father / Guardian","Text",
                // lab_batch
                "Text e.g. Batch-A",
                // bio (max 500 chars)
                "Text max 500 chars"
            };
            buildSheet(wb, stud, studHeaders, studReq, studTypes, reqStyle, optStyle, typeStyle);
            // Reference col layout:
            //   A=prog display, B=year display, C=prog code, D=year name,
            //   E=course display, F=course code, G=student_type, H=gender,
            //   I=community, J=blood_group, K=qual_type, L=payment_mode,
            //   M=admission_category, N=education_level, O=boolean_values
            addDropdownValidation(wb, stud, 4,  "Reference!$C$2:$C$200");  // program_code
            addDropdownValidation(wb, stud, 5,  "Reference!$F$2:$F$200");  // course_code
            addDropdownValidation(wb, stud, 8,  "Reference!$G$2:$G$5");    // student_type
            addDropdownValidation(wb, stud, 10, "Reference!$H$2:$H$5");    // gender
            addDropdownValidation(wb, stud, 14, "Reference!$I$2:$I$10");   // community_category
            addDropdownValidation(wb, stud, 16, "Reference!$J$2:$J$10");   // blood_group
            addDropdownValidation(wb, stud, 30, "Reference!$M$2:$M$3");    // admission_category
            addDropdownValidation(wb, stud, 35, "Reference!$O$2:$O$3");    // is_first_graduate
            addDropdownValidation(wb, stud, 36, "Reference!$N$2:$N$9");    // father_education
            addDropdownValidation(wb, stud, 37, "Reference!$N$2:$N$9");    // mother_education
            addDropdownValidation(wb, stud, 38, "Reference!$O$2:$O$3");    // physical_disability

            // ── Qualifications sheet ─────────────────────────────────────
            String[] qualHeaders = {
                "student_email","qualification_type",
                "school_name","major_subject","total_marks","percentage",
                "month_year_of_passing","university_or_board"
            };
            boolean[] qualReq = { true, true, false, false, false, false, false, false };
            String[] qualTypes = {
                "Email (must match Students sheet)","Enum (see Reference)",
                "Text","Text","Number","Decimal e.g. 88.50",
                "Text e.g. March 2022","Text"
            };
            buildSheet(wb, qual, qualHeaders, qualReq, qualTypes, reqStyle, optStyle, typeStyle);
            addDropdownValidation(wb, qual, 1, "Reference!$K$2:$K$10");   // qualification_type

            // ── Fee History sheet ────────────────────────────────────────
            // One row per payment. First row per student also sets the fee allocation.
            // year_1_fee..year_6_fee: if provided, used as exact year-wise breakdown.
            // If all blank, net_fee is split evenly across programme duration years.
            String[] feeHeaders = {
                "student_email","total_fee","discount_amount","discount_reason",
                "net_fee","amount_paid","payment_date","payment_mode",
                "receipt_number","remarks",
                "year_1_fee","year_2_fee","year_3_fee",
                "year_4_fee","year_5_fee","year_6_fee"
            };
            boolean[] feeReq = {
                true, true, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false
            };
            String[] feeTypes = {
                "Email (must match Students sheet)",
                "Decimal e.g. 450000","Decimal","Text",
                "Decimal (auto-computed if blank)",
                "Decimal (this payment amount)",
                "Date DD-MM-YYYY","Enum (see Reference)",
                "Text (from old system)","Text",
                "Decimal — Year 1 fee","Decimal — Year 2 fee","Decimal — Year 3 fee",
                "Decimal — Year 4 fee","Decimal — Year 5 fee","Decimal — Year 6 fee"
            };
            buildSheet(wb, fees, feeHeaders, feeReq, feeTypes, reqStyle, optStyle, typeStyle);
            addDropdownValidation(wb, fees, 7, "Reference!$L$2:$L$12");   // payment_mode

            // ── Sample data rows (green — delete before importing real data) ──
            addStudentSampleRows(stud, programs, courses, years, sampleStyle);
            addQualificationSampleRows(qual, sampleStyle);
            addFeeHistorySampleRows(fees, sampleStyle);

            // Auto-size columns on all user-facing sheets
            // Students: 44 cols (0-43), Qualifications: 8 cols, Fee History: 16 cols
            for (int i = 0; i < 44; i++) stud.autoSizeColumn(i);
            for (int i = 0; i < 8;  i++) qual.autoSizeColumn(i);
            for (int i = 0; i < 16; i++) fees.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Reference sheet population ───────────────────────────────────────────

    private void populateReferenceSheet(XSSFWorkbook wb, XSSFSheet ref,
                                         List<Program> programs,
                                         List<Course> courses,
                                         List<AcademicYear> years) {
        XSSFCellStyle hdr = refHeaderStyle(wb);
        int r;

        // ── Col A (0): Program — display "CODE — Name" ─────────────────
        setCell(ref, 0, 0, "Programs (display)", hdr);
        r = 1;
        for (Program p : programs) setCell(ref, r++, 0, p.getCode() + " — " + p.getName(), null);

        // ── Col B (1): Academic year — display ─────────────────────────
        setCell(ref, 0, 1, "Academic Years (display)", hdr);
        r = 1;
        for (AcademicYear ay : years) setCell(ref, r++, 1, ay.getName(), null);

        // ── Col C (2): Program code only → used by dropdown ────────────
        setCell(ref, 0, 2, "program_code ▸ paste here", hdr);
        r = 1;
        for (Program p : programs) setCell(ref, r++, 2, p.getCode(), null);

        // ── Col D (3): Academic year name only → used by dropdown ──────
        setCell(ref, 0, 3, "joining_academic_year ▸ paste here", hdr);
        r = 1;
        for (AcademicYear ay : years) setCell(ref, r++, 3, ay.getName(), null);

        // ── Col E (4): Course — display "code — name" ──────────────────
        setCell(ref, 0, 4, "Courses (display)", hdr);
        r = 1;
        for (Course c : courses) setCell(ref, r++, 4, c.getCode() + " — " + c.getName(), null);

        // ── Col F (5): Course code only → used by dropdown ─────────────
        setCell(ref, 0, 5, "course_code ▸ paste here", hdr);
        r = 1;
        for (Course c : courses) setCell(ref, r++, 5, c.getCode(), null);

        // ── Col G (6): student_type ────────────────────────────────────
        setCell(ref, 0, 6, "student_type", hdr);
        setCell(ref, 1, 6, "DAY_SCHOLAR", null);
        setCell(ref, 2, 6, "HOSTELER", null);

        // ── Col H (7): gender ──────────────────────────────────────────
        setCell(ref, 0, 7, "gender", hdr);
        setCell(ref, 1, 7, "MALE", null);
        setCell(ref, 2, 7, "FEMALE", null);
        setCell(ref, 3, 7, "OTHER", null);

        // ── Col I (8): community_category ─────────────────────────────
        setCell(ref, 0, 8, "community_category", hdr);
        String[] comm = {"SC","ST","BC","MBC","DNC","OC","OTHERS"};
        for (int i = 0; i < comm.length; i++) setCell(ref, i + 1, 8, comm[i], null);

        // ── Col J (9): blood_group ─────────────────────────────────────
        setCell(ref, 0, 9, "blood_group", hdr);
        String[] bg = {"A_POSITIVE","A_NEGATIVE","B_POSITIVE","B_NEGATIVE",
                        "O_POSITIVE","O_NEGATIVE","AB_POSITIVE","AB_NEGATIVE"};
        for (int i = 0; i < bg.length; i++) setCell(ref, i + 1, 9, bg[i], null);

        // ── Col K (10): qualification_type ─────────────────────────────
        setCell(ref, 0, 10, "qualification_type", hdr);
        String[] qt = {"SSLC","HSC","DIPLOMA","UG","PG","OTHER"};
        for (int i = 0; i < qt.length; i++) setCell(ref, i + 1, 10, qt[i], null);

        // ── Col L (11): payment_mode ───────────────────────────────────
        setCell(ref, 0, 11, "payment_mode", hdr);
        String[] pm = {"CASH","UPI","BANK_TRANSFER","CARD","CHEQUE","DEMAND_DRAFT","SCHOLARSHIP"};
        for (int i = 0; i < pm.length; i++) setCell(ref, i + 1, 11, pm[i], null);

        // ── Col M (12): admission_category ────────────────────────────
        setCell(ref, 0, 12, "admission_category", hdr);
        setCell(ref, 1, 12, "MANAGEMENT", null);
        setCell(ref, 2, 12, "COUNSELLING", null);

        // ── Col N (13): education_level (father / mother) ──────────────
        setCell(ref, 0, 13, "education_level", hdr);
        String[] edu = {"ILLITERATE","PRIMARY","SECONDARY","HSC","UG","PG","DOCTORATE"};
        for (int i = 0; i < edu.length; i++) setCell(ref, i + 1, 13, edu[i], null);

        // ── Col O (14): boolean_values ────────────────────────────────
        setCell(ref, 0, 14, "boolean_values", hdr);
        setCell(ref, 1, 14, "TRUE", null);
        setCell(ref, 2, 14, "FALSE", null);

        for (int c = 0; c < 15; c++) ref.autoSizeColumn(c);
    }

    // ── Sample data rows ──────────────────────────────────────────────────────

    private void addStudentSampleRows(XSSFSheet stud,
                                       List<Program> programs,
                                       List<Course> courses,
                                       List<AcademicYear> years,
                                       XSSFCellStyle style) {
        // Use first real codes so dropdown validation passes on the sample rows
        String prog = programs.isEmpty() ? "PROG-CODE" : programs.get(0).getCode();
        String crs  = courses.isEmpty()  ? "COURSE-01" : courses.get(0).getCode();
        String ay   = years.isEmpty()    ? "2023-24"   : years.get(0).getName();

        // Student 1 — complete record (all fields filled)
        // Col order matches studHeaders array exactly (44 columns, 0-43)
        String[] s1 = {
            // 0-3  core identity
            "Priya", "Sharma", "priya.sharma@sample.com", "9876543210",
            // 4-8  academic
            prog, crs, ay, "15-06-2023", "DAY_SCHOLAR",
            // 9-11 personal
            "12-03-2005", "FEMALE", "123456789012",
            // 12-16 demographics
            "Indian", "Hindu", "BC", "Nadar", "B_POSITIVE",
            // 17-19 family basic
            "Ramesh Sharma", "Lakshmi Sharma", "9876543211",
            // 20-25 address
            "45 Gandhi Nagar", "Main Road", "Coimbatore", "Coimbatore", "Tamil Nadu", "641001",
            // 26-29 registration numbers
            "ADM-2023-001", "GNM-2023-001", "TN/2023/BSN/001", "UMIS2023001",
            // 30 admission category
            "MANAGEMENT",
            // 31-34 extended family
            "9876543212", "ramesh.sharma@sample.com", "9876543213", "lakshmi.sharma@sample.com",
            // 35 first graduate
            "FALSE",
            // 36-37 parent education
            "HSC", "PRIMARY",
            // 38 disability
            "FALSE",
            // 39-41 emergency contact
            "Ramesh Sharma", "Father", "9876543212",
            // 42-43 placement / profile
            "Batch-A", "Dedicated nursing student from Coimbatore."
        };

        // Student 2 — minimal record (required fields + common optionals; several fields left blank)
        String[] s2 = {
            // 0-3  core identity
            "Kavitha", "Murugan", "kavitha.murugan@sample.com", "9123456789",
            // 4-8  academic
            prog, crs, ay, "20-06-2023", "HOSTELER",
            // 9-11 personal
            "25-07-2004", "FEMALE", "234567890123",
            // 12-16 demographics
            "Indian", "Christian", "SC", "Paraiyar", "O_POSITIVE",
            // 17-19 family basic
            "Murugan V", "Selvi Murugan", "9123456790",
            // 20-25 address
            "12 Anna Street", "Cross Road", "Chennai", "Chennai", "Tamil Nadu", "600001",
            // 26-29 registration numbers (URN and UMIS left blank — not yet assigned)
            "ADM-2023-002", "GNM-2023-002", "", "",
            // 30 admission category
            "COUNSELLING",
            // 31-34 extended family (emails not available)
            "9123456791", "", "9123456792", "",
            // 35 first graduate
            "TRUE",
            // 36-37 parent education
            "SECONDARY", "ILLITERATE",
            // 38 disability
            "FALSE",
            // 39-41 emergency contact
            "Selvi Murugan", "Mother", "9123456792",
            // 42-43 placement / profile (bio left blank)
            "Batch-B", ""
        };

        XSSFRow r1 = writeSampleRow(stud, 2, s1, style);
        writeSampleRow(stud, 3, s2, style);

        // Add a visible comment on the first cell of the first sample row
        XSSFDrawing drawing = stud.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 2, 4, 5);
        XSSFComment comment = drawing.createCellComment(anchor);
        comment.setString(new XSSFRichTextString(
            "SAMPLE DATA\n" +
            "These two rows are examples — delete them before\n" +
            "importing your actual student records.\n\n" +
            "Green rows = sample  |  Pink header = required  |  Blue header = optional"));
        comment.setAuthor("CMS Import Template");
        r1.getCell(0).setCellComment(comment);
    }

    private void addQualificationSampleRows(XSSFSheet qual, XSSFCellStyle style) {
        // Two qualifications per sample student = 4 rows total
        String[][] rows = {
            // Priya Sharma — SSLC
            {"priya.sharma@sample.com", "SSLC",
             "St. Mary's Higher Secondary School", "General",
             "500", "87.60", "March 2021", "Tamil Nadu State Board"},
            // Priya Sharma — HSC
            {"priya.sharma@sample.com", "HSC",
             "St. Mary's Higher Secondary School", "Biology",
             "600", "91.33", "March 2023", "Tamil Nadu State Board"},
            // Kavitha Murugan — SSLC
            {"kavitha.murugan@sample.com", "SSLC",
             "Government High School", "General",
             "500", "79.20", "March 2020", "Tamil Nadu State Board"},
            // Kavitha Murugan — HSC
            {"kavitha.murugan@sample.com", "HSC",
             "Government Higher Secondary School", "Biology",
             "600", "82.50", "March 2022", "Tamil Nadu State Board"},
        };
        for (int i = 0; i < rows.length; i++) writeSampleRow(qual, i + 2, rows[i], style);
    }

    private void addFeeHistorySampleRows(XSSFSheet fees, XSSFCellStyle style) {
        // HOW MULTIPLE ROWS WORK:
        //   First row per student  → sets the fee allocation (total_fee, discount, year_* breakdown)
        //                            AND records the first payment if amount_paid is filled.
        //   Subsequent rows        → record additional payment receipts only.
        //                            total_fee / year_* are IGNORED on rows 2, 3, etc.
        //
        // year_1_fee..year_6_fee = how the TOTAL fee is split across programme years
        //   (e.g. Year 1: ₹50k, Year 2: ₹50k, Year 3: ₹50k for a 3-year course).
        //   Leave blank to split evenly. These are NOT per-payment amounts.
        //
        // Priya paid in two separate receipts; Kavitha paid in full (one receipt).
        String[][] rows = {
            // Priya row 1 — sets fee allocation AND records first payment
            {"priya.sharma@sample.com",
             "150000", "", "", "150000",                          // total, disc, reason, net
             "75000", "20-06-2023", "CASH", "RCT-2023-001",      // paid, date, mode, receipt
             "First instalment",                                   // remarks
             "50000", "50000", "50000", "", "", ""},              // year_1..year_3 (yr4-6 blank = 3-yr course)

            // Priya row 2 — second payment only; fee allocation already created from row 1
            //   total_fee / year_* are left blank intentionally — they are ignored here
            {"priya.sharma@sample.com",
             "", "", "", "",
             "75000", "15-12-2023", "UPI", "RCT-2023-002",
             "Second instalment",
             "", "", "", "", "", ""},

            // Kavitha row 1 — fee allocation (with discount) AND single full payment
            {"kavitha.murugan@sample.com",
             "180000", "10000", "SC Category Concession", "170000",
             "170000", "25-06-2023", "BANK_TRANSFER", "RCT-2023-003",
             "Full payment in one receipt",
             "60000", "60000", "50000", "", "", ""},
        };
        XSSFRow firstRow = writeSampleRow(fees, 2, rows[0], style);
        for (int i = 1; i < rows.length; i++) writeSampleRow(fees, i + 2, rows[i], style);

        // Cell comment on A3 explaining the sheet structure
        XSSFDrawing drawing = fees.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 2, 6, 8);
        XSSFComment comment = drawing.createCellComment(anchor);
        comment.setString(new XSSFRichTextString(
            "SAMPLE DATA — delete before importing real data\n\n" +
            "Each row = one payment receipt.\n" +
            "Multiple rows for the same student = multiple receipts (e.g. Priya paid in 2 instalments).\n\n" +
            "FIRST ROW per student: fill total_fee + year_1_fee..year_6_fee to set the fee structure.\n" +
            "SUBSEQUENT ROWS: leave total_fee / year_* blank — only payment columns are used.\n\n" +
            "year_1_fee..year_6_fee = how total fee is split across programme years, NOT per-payment amounts.\n" +
            "Leave all year_* blank to split evenly across the programme duration."));
        comment.setAuthor("CMS Import Template");
        firstRow.getCell(0).setCellComment(comment);
    }

    private XSSFRow writeSampleRow(XSSFSheet sheet, int rowIndex, String[] values, XSSFCellStyle style) {
        XSSFRow row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            cell.setCellStyle(style);
        }
        return row;
    }

    // ── Sheet builder ─────────────────────────────────────────────────────────

    private void buildSheet(XSSFWorkbook wb, XSSFSheet sheet,
                             String[] headers, boolean[] required, String[] types,
                             XSSFCellStyle reqStyle, XSSFCellStyle optStyle,
                             XSSFCellStyle typeStyle) {
        XSSFRow hdrRow  = sheet.createRow(0);
        XSSFRow typeRow = sheet.createRow(1);
        sheet.createFreezePane(0, 2);

        for (int i = 0; i < headers.length; i++) {
            XSSFCell hc = hdrRow.createCell(i);
            hc.setCellValue(headers[i] + (required[i] ? " *" : ""));
            hc.setCellStyle(required[i] ? reqStyle : optStyle);

            XSSFCell tc = typeRow.createCell(i);
            tc.setCellValue(types[i]);
            tc.setCellStyle(typeStyle);
        }

        hdrRow.setHeight((short) 600);
    }

    private void addDropdownValidation(XSSFWorkbook wb, XSSFSheet sheet,
                                        int colIndex, String formula) {
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
        XSSFDataValidationConstraint constraint =
            (XSSFDataValidationConstraint) dvHelper.createFormulaListConstraint(formula);
        CellRangeAddressList range = new CellRangeAddressList(2, 5000, colIndex, colIndex);
        XSSFDataValidation validation =
            (XSSFDataValidation) dvHelper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(XSSFDataValidation.ErrorStyle.WARNING);
        validation.createErrorBox("Invalid value", "Please select a value from the dropdown list.");
        sheet.addValidationData(validation);
    }

    // ── Cell / style helpers ──────────────────────────────────────────────────

    private void setCell(XSSFSheet sheet, int row, int col, String value, XSSFCellStyle style) {
        XSSFRow r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);
        XSSFCell c = r.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private XSSFCellStyle sampleDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        setBorder(s);
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle reqHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle optHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle typeRowStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s);
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle refHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(s);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 9);
        s.setFont(f);
        return s;
    }

    private void setBorder(XSSFCellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}
