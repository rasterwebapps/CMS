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
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
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

            XSSFSheet ref   = wb.createSheet("Reference");
            XSSFSheet stud  = wb.createSheet("Students");
            XSSFSheet qual  = wb.createSheet("Qualifications");
            XSSFSheet fees  = wb.createSheet("Fee History");

            XSSFCellStyle reqStyle  = reqHeaderStyle(wb);
            XSSFCellStyle optStyle  = optHeaderStyle(wb);
            XSSFCellStyle typeStyle = typeRowStyle(wb);

            // ── Reference sheet ──────────────────────────────────────────
            populateReferenceSheet(wb, ref);

            // ── Students sheet ───────────────────────────────────────────
            String[] studHeaders = {
                "first_name","last_name","email","phone",
                "program_code","course_code","joining_academic_year",
                "application_date","student_type",
                "date_of_birth","gender","aadhar_number",
                "nationality","religion","community_category","caste","blood_group",
                "father_name","mother_name","parent_mobile",
                "postal_address","street","city","district","state","pincode"
            };
            boolean[] studReq = {
                true,true,true,false,
                true,false,false,
                false,false,
                false,false,false,
                false,false,false,false,false,
                false,false,false,
                false,false,false,false,false,false
            };
            String[] studTypes = {
                "Text","Text","Email","Text",
                "Code (see Reference)","Code (see Reference)","Text e.g. 2024-25",
                "Date DD-MM-YYYY","Enum (see Reference)",
                "Date DD-MM-YYYY","Enum (see Reference)","Text 12 digits",
                "Text","Text","Enum (see Reference)","Text","Enum (see Reference)",
                "Text","Text","Text",
                "Text","Text","Text","Text","Text","Text"
            };
            buildSheet(wb, stud, studHeaders, studReq, studTypes, reqStyle, optStyle, typeStyle);
            // Reference col layout: A=prog display, B=year display, C=prog code, D=year name,
            //   E=course display, F=course code, G=student_type, H=gender,
            //   I=community, J=blood_group, K=qual_type, L=payment_mode
            addDropdownValidation(wb, stud, 4,  "Reference!$C$2:$C$200");  // program_code (code-only)
            addDropdownValidation(wb, stud, 5,  "Reference!$F$2:$F$200");  // course_code (code-only)
            addDropdownValidation(wb, stud, 8,  "Reference!$G$2:$G$5");    // student_type
            addDropdownValidation(wb, stud, 10, "Reference!$H$2:$H$5");    // gender
            addDropdownValidation(wb, stud, 14, "Reference!$I$2:$I$10");   // community_category
            addDropdownValidation(wb, stud, 16, "Reference!$J$2:$J$10");   // blood_group

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
            String[] feeHeaders = {
                "student_email","total_fee","discount_amount","discount_reason",
                "net_fee","amount_paid","payment_date","payment_mode",
                "receipt_number","remarks"
            };
            boolean[] feeReq = { true, true, false, false, false, false, false, false, false, false };
            String[] feeTypes = {
                "Email (must match Students sheet)",
                "Decimal e.g. 450000","Decimal","Text",
                "Decimal (auto-computed if blank)",
                "Decimal (historical payment amount)",
                "Date DD-MM-YYYY","Enum (see Reference)",
                "Text (from old system)","Text"
            };
            buildSheet(wb, fees, feeHeaders, feeReq, feeTypes, reqStyle, optStyle, typeStyle);
            addDropdownValidation(wb, fees, 7, "Reference!$L$2:$L$12");   // payment_mode

            // Auto-size columns on all user-facing sheets
            for (XSSFSheet sh : new XSSFSheet[]{stud, qual, fees}) {
                for (int i = 0; i < 30; i++) sh.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Reference sheet population ───────────────────────────────────────────

    private void populateReferenceSheet(XSSFWorkbook wb, XSSFSheet ref) {
        XSSFCellStyle hdr = refHeaderStyle(wb);

        List<Program>     programs = programRepository.findAll();
        List<Course>      courses  = courseRepository.findAll();
        List<AcademicYear> years   = academicYearRepository.findAll();

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
        String[] pm = {"CASH","CHEQUE","UPI","BANK_TRANSFER","NET_BANKING","CARD","DEMAND_DRAFT","SCHOLARSHIP"};
        for (int i = 0; i < pm.length; i++) setCell(ref, i + 1, 11, pm[i], null);

        for (int c = 0; c < 12; c++) ref.autoSizeColumn(c);
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
