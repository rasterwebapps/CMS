package com.cms.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AcademicQualification;
import com.cms.model.AcademicYear;
import com.cms.model.Admission;
import com.cms.model.Agent;
import com.cms.model.AgentCommissionGuideline;
import com.cms.model.Course;
import com.cms.model.Department;
import com.cms.model.Enquiry;
import com.cms.model.Equipment;
import com.cms.model.Examination;
import com.cms.model.ExamResult;
import com.cms.model.Experiment;
import com.cms.model.Faculty;
import com.cms.model.FeePayment;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.InventoryItem;
import com.cms.model.Lab;
import com.cms.model.LabCurriculumMapping;
import com.cms.model.LabInChargeAssignment;
import com.cms.model.LabSchedule;
import com.cms.model.LabSlot;
import com.cms.model.MaintenanceRequest;
import com.cms.model.Program;
import com.cms.model.ReferralType;
import com.cms.model.Semester;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.Subject;
import com.cms.model.Syllabus;
import com.cms.model.enums.AdmissionStatus;
import com.cms.model.enums.BloodGroup;
import com.cms.model.enums.CommunityCategory;
import com.cms.model.enums.Designation;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;
import com.cms.model.enums.ExamResultStatus;
import com.cms.model.enums.ExamType;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.Gender;
import com.cms.model.enums.LabInChargeRole;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.model.enums.LocalityType;
import com.cms.model.enums.MaintenancePriority;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.model.enums.MaintenanceType;
import com.cms.model.enums.MappingLevel;
import com.cms.model.enums.OutcomeType;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.PaymentStatus;
import com.cms.model.enums.QualificationType;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AcademicQualificationRepository;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AgentCommissionGuidelineRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.ExperimentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeePaymentRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.InventoryItemRepository;
import com.cms.repository.LabCurriculumMappingRepository;
import com.cms.repository.LabInChargeAssignmentRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.MaintenanceRequestRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.ReferralTypeRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.SemesterRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.SyllabusRepository;

@Component
@Profile("local")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final LabRepository labRepository;
    private final LabSlotRepository labSlotRepository;
    private final LabInChargeAssignmentRepository labInChargeAssignmentRepository;
    private final StudentRepository studentRepository;
    private final AdmissionRepository admissionRepository;
    private final AcademicQualificationRepository academicQualificationRepository;
    private final AgentRepository agentRepository;
    private final AgentCommissionGuidelineRepository agentCommissionGuidelineRepository;
    private final ReferralTypeRepository referralTypeRepository;
    private final EnquiryRepository enquiryRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureYearAmountRepository feeStructureYearAmountRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final EquipmentRepository equipmentRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamResultRepository examResultRepository;
    private final SyllabusRepository syllabusRepository;
    private final ExperimentRepository experimentRepository;
    private final LabCurriculumMappingRepository labCurriculumMappingRepository;
    private final LabScheduleRepository labScheduleRepository;
    private final StudentFeeAllocationRepository studentFeeAllocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;

    public DataLoader(DepartmentRepository departmentRepository,
                      ProgramRepository programRepository,
                      AcademicYearRepository academicYearRepository,
                      SemesterRepository semesterRepository,
                      CourseRepository courseRepository,
                      SubjectRepository subjectRepository,
                      FacultyRepository facultyRepository,
                      LabRepository labRepository,
                      LabSlotRepository labSlotRepository,
                      LabInChargeAssignmentRepository labInChargeAssignmentRepository,
                      StudentRepository studentRepository,
                      AdmissionRepository admissionRepository,
                      AcademicQualificationRepository academicQualificationRepository,
                      AgentRepository agentRepository,
                      AgentCommissionGuidelineRepository agentCommissionGuidelineRepository,
                      ReferralTypeRepository referralTypeRepository,
                      EnquiryRepository enquiryRepository,
                      FeeStructureRepository feeStructureRepository,
                      FeeStructureYearAmountRepository feeStructureYearAmountRepository,
                      FeePaymentRepository feePaymentRepository,
                      EquipmentRepository equipmentRepository,
                      InventoryItemRepository inventoryItemRepository,
                      MaintenanceRequestRepository maintenanceRequestRepository,
                      ExaminationRepository examinationRepository,
                      ExamResultRepository examResultRepository,
                      SyllabusRepository syllabusRepository,
                      ExperimentRepository experimentRepository,
                      LabCurriculumMappingRepository labCurriculumMappingRepository,
                      LabScheduleRepository labScheduleRepository,
                      StudentFeeAllocationRepository studentFeeAllocationRepository,
                      SemesterFeeRepository semesterFeeRepository) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.labRepository = labRepository;
        this.labSlotRepository = labSlotRepository;
        this.labInChargeAssignmentRepository = labInChargeAssignmentRepository;
        this.studentRepository = studentRepository;
        this.admissionRepository = admissionRepository;
        this.academicQualificationRepository = academicQualificationRepository;
        this.agentRepository = agentRepository;
        this.agentCommissionGuidelineRepository = agentCommissionGuidelineRepository;
        this.referralTypeRepository = referralTypeRepository;
        this.enquiryRepository = enquiryRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.feeStructureYearAmountRepository = feeStructureYearAmountRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.equipmentRepository = equipmentRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.examinationRepository = examinationRepository;
        this.examResultRepository = examResultRepository;
        this.syllabusRepository = syllabusRepository;
        this.experimentRepository = experimentRepository;
        this.labCurriculumMappingRepository = labCurriculumMappingRepository;
        this.labScheduleRepository = labScheduleRepository;
        this.studentFeeAllocationRepository = studentFeeAllocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (departmentRepository.count() > 0) {
            log.info("Seed data already present — skipping DataLoader.");
            return;
        }

        log.info("Seeding initial data for local profile...");

        // ── 1. Referral Types ────────────────────────────────────────────────
        seedReferralTypes();

        // ── 2. Departments ───────────────────────────────────────────────────
        List<Department> depts = seedDepartments();
        Department gnDept    = depts.get(0);
        Department moDept    = depts.get(1);
        Department chnDept   = depts.get(2);
        Department msnDept   = depts.get(3);
        Department pnDept    = depts.get(4);

        // ── 3. Programs ──────────────────────────────────────────────────────
        Program bachelorProgram = seedProgram("Bachelor", "BACHELOR", 4, List.of(gnDept, msnDept, pnDept));
        Program masterProgram   = seedProgram("Master",   "MASTER",   2, List.of(gnDept, msnDept));
        Program diplomaProgram  = seedProgram("Diploma",  "DIPLOMA",  3, List.of(gnDept, moDept));
        seedProgram("Certificate", "CERTIFICATE", 1, List.of());
        seedProgram("Doctoral",    "DOCTORAL",    3, List.of());

        // ── 4. Academic Years ────────────────────────────────────────────────
        AcademicYear ay2324 = academicYearRepository.save(new AcademicYear(
                "2023-2024", LocalDate.of(2023, 6, 1), LocalDate.of(2024, 5, 31), false));
        AcademicYear ay2425 = academicYearRepository.save(new AcademicYear(
                "2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true));
        AcademicYear ay2526 = academicYearRepository.save(new AcademicYear(
                "2025-2026", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false));

        // ── 5. Semesters ─────────────────────────────────────────────────────
        Semester sem1 = semesterRepository.save(new Semester(
                "Odd Semester 2023-2024", ay2324, LocalDate.of(2023, 6, 1), LocalDate.of(2023, 11, 30), 1));
        Semester sem2 = semesterRepository.save(new Semester(
                "Even Semester 2023-2024", ay2324, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 31), 2));
        Semester sem3 = semesterRepository.save(new Semester(
                "Odd Semester 2024-2025", ay2425, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), 3));
        Semester sem4 = semesterRepository.save(new Semester(
                "Even Semester 2024-2025", ay2425, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 5, 31), 4));

        // ── 6. Courses ───────────────────────────────────────────────────────
        Course bscCourse = courseRepository.save(
                new Course("B.Sc Nursing – Generalist", "BSC-NURS-GEN", null, bachelorProgram));
        Course mscCourse = courseRepository.save(
                new Course("M.Sc Nursing – Adult Health", "MSC-AHN", "Adult Health Nursing", masterProgram));
        Course gnmCourse = courseRepository.save(
                new Course("GNM – General Nursing & Midwifery", "GNM-GEN", null, diplomaProgram));

        // ── 7. Subjects ──────────────────────────────────────────────────────
        Subject anatomy    = subjectRepository.save(new Subject("Anatomy & Physiology",       "BSC-SUB-001", 4, 3, 1, bscCourse, gnDept, 1));
        Subject biochem    = subjectRepository.save(new Subject("Biochemistry",               "BSC-SUB-002", 3, 2, 1, bscCourse, msnDept, 1));
        Subject nfTheory   = subjectRepository.save(new Subject("Nursing Foundations Theory", "BSC-SUB-003", 4, 4, 0, bscCourse, gnDept, 2));
        Subject nfLab      = subjectRepository.save(new Subject("Nursing Foundations Lab",    "BSC-SUB-004", 2, 0, 2, bscCourse, gnDept, 2));
        Subject microbio   = subjectRepository.save(new Subject("Microbiology & Parasitology","BSC-SUB-005", 3, 2, 1, bscCourse, msnDept, 2));
        Subject medSurg    = subjectRepository.save(new Subject("Medical-Surgical Nursing I", "BSC-SUB-006", 4, 3, 1, bscCourse, msnDept, 3));
        Subject community  = subjectRepository.save(new Subject("Community Health Nursing",   "BSC-SUB-007", 3, 2, 1, bscCourse, chnDept, 4));
        Subject advNursing = subjectRepository.save(new Subject("Advanced Nursing Practice",  "MSC-SUB-001", 4, 3, 1, mscCourse, gnDept, 1));
        Subject resMethod  = subjectRepository.save(new Subject("Research Methodology",       "MSC-SUB-002", 3, 3, 0, mscCourse, msnDept, 1));
        Subject basicNurs  = subjectRepository.save(new Subject("Basic Nursing Concepts",     "GNM-SUB-001", 4, 3, 1, gnmCourse, gnDept, 1));

        // ── 8. Faculty ───────────────────────────────────────────────────────
        Faculty f1 = facultyRepository.save(new Faculty("FAC001", "Priya",     "Sharma",    "priya.sharma@cms.edu",     "9876500001", gnDept,  Designation.HOD,                  "General Nursing",      "Nursing Simulation",           LocalDate.of(2015, 6, 1),  FacultyStatus.ACTIVE));
        Faculty f2 = facultyRepository.save(new Faculty("FAC002", "Lakshmi",   "Devi",      "lakshmi.devi@cms.edu",     "9876500002", moDept,  Designation.HOD,                  "Midwifery",            "Obstetric Lab",                LocalDate.of(2014, 7, 1),  FacultyStatus.ACTIVE));
        Faculty f3 = facultyRepository.save(new Faculty("FAC003", "Anitha",    "Rao",       "anitha.rao@cms.edu",       "9876500003", chnDept, Designation.PROFESSOR,            "Community Health",     "Community Lab",                LocalDate.of(2016, 8, 1),  FacultyStatus.ACTIVE));
        Faculty f4 = facultyRepository.save(new Faculty("FAC004", "Rajesh",    "Kumar",     "rajesh.kumar@cms.edu",     "9876500004", msnDept, Designation.ASSOCIATE_PROFESSOR,  "Medical-Surgical",     "Anatomy Lab",                  LocalDate.of(2018, 1, 15), FacultyStatus.ACTIVE));
        Faculty f5 = facultyRepository.save(new Faculty("FAC005", "Kavitha",   "Nair",      "kavitha.nair@cms.edu",     "9876500005", gnDept,  Designation.ASSISTANT_PROFESSOR,  "Fundamentals of Nursing", null,                       LocalDate.of(2020, 6, 1),  FacultyStatus.ACTIVE));
        Faculty f6 = facultyRepository.save(new Faculty("FAC006", "Deepa",     "Thomas",    "deepa.thomas@cms.edu",     "9876500006", pnDept,  Designation.LECTURER,             "Pediatric Nursing",    null,                           LocalDate.of(2021, 3, 1),  FacultyStatus.ACTIVE));
        Faculty f7 = facultyRepository.save(new Faculty("FAC007", "Suresh",    "Babu",      "suresh.babu@cms.edu",      "9876500007", gnDept,  Designation.LAB_INSTRUCTOR,       "Nursing Simulation",   "Nursing Foundation Lab",       LocalDate.of(2019, 9, 1),  FacultyStatus.ACTIVE));
        Faculty f8 = facultyRepository.save(new Faculty("FAC008", "Ramya",     "Krishnan",  "ramya.krishnan@cms.edu",   "9876500008", msnDept, Designation.ASSISTANT_PROFESSOR,  "Clinical Nursing",     "Skills Lab",                   LocalDate.of(2022, 1, 10), FacultyStatus.ACTIVE));

        // ── 9. Labs ──────────────────────────────────────────────────────────
        Lab nfLab2    = labRepository.save(new Lab("Nursing Foundation Lab", LabType.OTHER,    gnDept,  "Block A", "A-101", 30, LabStatus.ACTIVE));
        Lab anatLab   = labRepository.save(new Lab("Anatomy Lab",            LabType.BIOLOGY,  msnDept, "Block B", "B-201", 25, LabStatus.ACTIVE));
        Lab compLab   = labRepository.save(new Lab("Computer Lab",           LabType.COMPUTER, gnDept,  "Block C", "C-301", 40, LabStatus.ACTIVE));
        Lab chnLabObj = labRepository.save(new Lab("Community Health Lab",   LabType.OTHER,    chnDept, "Block D", "D-101", 20, LabStatus.ACTIVE));

        // ── 10. Lab Slots ────────────────────────────────────────────────────
        LabSlot slot1 = labSlotRepository.save(new LabSlot("Morning Slot 1",   LocalTime.of(8,  0), LocalTime.of(10, 0), 1, true));
        LabSlot slot2 = labSlotRepository.save(new LabSlot("Morning Slot 2",   LocalTime.of(10, 0), LocalTime.of(12, 0), 2, true));
        LabSlot slot3 = labSlotRepository.save(new LabSlot("Afternoon Slot 1", LocalTime.of(13, 0), LocalTime.of(15, 0), 3, true));
        LabSlot slot4 = labSlotRepository.save(new LabSlot("Afternoon Slot 2", LocalTime.of(15, 0), LocalTime.of(17, 0), 4, true));

        // ── 11. Lab Incharge Assignments ────────────────────────────────────
        labInChargeAssignmentRepository.save(new LabInChargeAssignment(nfLab2,  f7.getId(), "Suresh Babu",    LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 6, 1)));
        labInChargeAssignmentRepository.save(new LabInChargeAssignment(anatLab, f4.getId(), "Rajesh Kumar",   LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 6, 1)));
        labInChargeAssignmentRepository.save(new LabInChargeAssignment(compLab, f1.getId(), "Priya Sharma",   LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 6, 1)));
        labInChargeAssignmentRepository.save(new LabInChargeAssignment(nfLab2,  f8.getId(), "Ramya Krishnan", LabInChargeRole.TECHNICIAN,   LocalDate.of(2024, 6, 1)));

        // ── 12. Students ─────────────────────────────────────────────────────
        List<Student> students = seedStudents(bachelorProgram, diplomaProgram, masterProgram);
        Student s1  = students.get(0);
        Student s2  = students.get(1);
        Student s3  = students.get(2);
        Student s4  = students.get(3);
        Student s5  = students.get(4);
        Student s6  = students.get(5);
        Student s7  = students.get(6);
        Student s8  = students.get(7);

        // ── 13. Admissions + Qualifications ─────────────────────────────────
        Admission adm1 = admissionRepository.save(new Admission(s1, 2024, 2025, LocalDate.of(2024, 6, 10), AdmissionStatus.APPROVED));
        adm1.setDeclarationPlace("Chennai");
        adm1.setDeclarationDate(LocalDate.of(2024, 6, 10));
        adm1.setParentConsentGiven(true);
        adm1.setApplicantConsentGiven(true);
        adm1 = admissionRepository.save(adm1);
        academicQualificationRepository.save(new AcademicQualification(adm1, QualificationType.SSLC,  "St. Mary's Matric School",      "Science",        500, new BigDecimal("91.20"), "March 2020", "Tamil Nadu SSLC Board"));
        academicQualificationRepository.save(new AcademicQualification(adm1, QualificationType.HSC,   "St. Mary's Higher Sec School",  "Biology Group",  600, new BigDecimal("88.50"), "March 2022", "Tamil Nadu HSC Board"));

        Admission adm2 = admissionRepository.save(new Admission(s2, 2024, 2025, LocalDate.of(2024, 6, 12), AdmissionStatus.APPROVED));
        adm2.setDeclarationPlace("Coimbatore");
        adm2.setDeclarationDate(LocalDate.of(2024, 6, 12));
        adm2.setParentConsentGiven(true);
        adm2.setApplicantConsentGiven(true);
        adm2 = admissionRepository.save(adm2);
        academicQualificationRepository.save(new AcademicQualification(adm2, QualificationType.SSLC,  "GRK Matric School",             "Science",        500, new BigDecimal("95.00"), "March 2020", "Tamil Nadu SSLC Board"));
        academicQualificationRepository.save(new AcademicQualification(adm2, QualificationType.HSC,   "GRK Higher Sec School",         "Biology Group",  600, new BigDecimal("92.00"), "March 2022", "Tamil Nadu HSC Board"));

        Admission adm3 = admissionRepository.save(new Admission(s8, 2024, 2025, LocalDate.of(2024, 6, 15), AdmissionStatus.APPROVED));
        adm3.setDeclarationPlace("Madurai");
        adm3.setDeclarationDate(LocalDate.of(2024, 6, 15));
        adm3.setParentConsentGiven(true);
        adm3.setApplicantConsentGiven(true);
        adm3 = admissionRepository.save(adm3);
        academicQualificationRepository.save(new AcademicQualification(adm3, QualificationType.DEGREE, "GRK Nursing College",          "B.Sc Nursing",   1000, new BigDecimal("78.60"), "April 2022", "Tamil Nadu Dr. MGR Medical University"));

        // ── 14. Agents ───────────────────────────────────────────────────────
        Agent ag1 = agentRepository.save(new Agent("Srinivas Education Services", "9876543210", "srinivas.edu@gmail.com", "Coimbatore", "RS Puram", true));
        Agent ag2 = agentRepository.save(new Agent("Global Nursing Academy",      "9123456780", "global.nursing@gmail.com","Chennai",    "Anna Nagar", true));
        Agent ag3 = agentRepository.save(new Agent("Tamil Nadu Education Hub",    "9988776655", "tneduhub@gmail.com",      "Madurai",    "SS Colony", true));

        // ── 15. Agent Commission Guidelines ─────────────────────────────────
        agentCommissionGuidelineRepository.save(new AgentCommissionGuideline(ag1, bachelorProgram, LocalityType.LOCAL,    new BigDecimal("15000.00")));
        agentCommissionGuidelineRepository.save(new AgentCommissionGuideline(ag1, bachelorProgram, LocalityType.DISTRICT, new BigDecimal("12000.00")));
        agentCommissionGuidelineRepository.save(new AgentCommissionGuideline(ag1, diplomaProgram, LocalityType.LOCAL,    new BigDecimal("8000.00")));
        agentCommissionGuidelineRepository.save(new AgentCommissionGuideline(ag2, bachelorProgram, LocalityType.STATE,    new BigDecimal("10000.00")));
        agentCommissionGuidelineRepository.save(new AgentCommissionGuideline(ag3, diplomaProgram, LocalityType.DISTRICT, new BigDecimal("7000.00")));

        // ── 16. Fee Structures ───────────────────────────────────────────────
        FeeStructure bscTuition = new FeeStructure(bachelorProgram, ay2425, FeeType.TUITION, new BigDecimal("95000.00"), true, true);
        bscTuition.setDescription("B.Sc Nursing 4-Year Tuition Fee 2024-25");
        bscTuition = feeStructureRepository.save(bscTuition);
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscTuition, 1, "Year 1", new BigDecimal("25000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscTuition, 2, "Year 2", new BigDecimal("25000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscTuition, 3, "Year 3", new BigDecimal("25000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscTuition, 4, "Year 4", new BigDecimal("20000.00")));

        FeeStructure bscLabFee = new FeeStructure(bachelorProgram, ay2425, FeeType.LAB_FEE, new BigDecimal("12000.00"), true, true);
        bscLabFee.setDescription("B.Sc Nursing Lab Fee 2024-25");
        bscLabFee = feeStructureRepository.save(bscLabFee);
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscLabFee, 1, "Year 1", new BigDecimal("3000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscLabFee, 2, "Year 2", new BigDecimal("3000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscLabFee, 3, "Year 3", new BigDecimal("3000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(bscLabFee, 4, "Year 4", new BigDecimal("3000.00")));

        FeeStructure gnmTuition = new FeeStructure(diplomaProgram, ay2425, FeeType.TUITION, new BigDecimal("65000.00"), true, true);
        gnmTuition.setDescription("GNM 3-Year Tuition Fee 2024-25");
        gnmTuition = feeStructureRepository.save(gnmTuition);
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(gnmTuition, 1, "Year 1", new BigDecimal("22000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(gnmTuition, 2, "Year 2", new BigDecimal("22000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(gnmTuition, 3, "Year 3", new BigDecimal("21000.00")));

        FeeStructure mscTuition = new FeeStructure(masterProgram, ay2425, FeeType.TUITION, new BigDecimal("45000.00"), true, true);
        mscTuition.setDescription("M.Sc Nursing 2-Year Tuition Fee 2024-25");
        mscTuition = feeStructureRepository.save(mscTuition);
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(mscTuition, 1, "Year 1", new BigDecimal("23000.00")));
        feeStructureYearAmountRepository.save(new FeeStructureYearAmount(mscTuition, 2, "Year 2", new BigDecimal("22000.00")));

        // ── 17. Fee Payments ─────────────────────────────────────────────────
        feePaymentRepository.save(new FeePayment(s1, bscTuition, "RCP-2024-0001",
                new BigDecimal("25000.00"), LocalDate.of(2024, 6, 15), PaymentMode.CASH, PaymentStatus.PAID));
        feePaymentRepository.save(new FeePayment(s2, bscTuition, "RCP-2024-0002",
                new BigDecimal("25000.00"), LocalDate.of(2024, 6, 18), PaymentMode.UPI, PaymentStatus.PAID));
        feePaymentRepository.save(new FeePayment(s6, gnmTuition, "RCP-2024-0003",
                new BigDecimal("22000.00"), LocalDate.of(2024, 6, 20), PaymentMode.NET_BANKING, PaymentStatus.PAID));
        feePaymentRepository.save(new FeePayment(s8, mscTuition, "RCP-2024-0004",
                new BigDecimal("23000.00"), LocalDate.of(2024, 6, 22), PaymentMode.DEMAND_DRAFT, PaymentStatus.PAID));

        // ── 18. Student Fee Allocations ──────────────────────────────────────
        StudentFeeAllocation alloc1 = studentFeeAllocationRepository.save(
                new StudentFeeAllocation(s1, bachelorProgram, new BigDecimal("95000.00"),
                        new BigDecimal("5000.00"), "Merit Scholarship", null, new BigDecimal("90000.00"),
                        FeeAllocationStatus.FINALIZED));
        semesterFeeRepository.save(new SemesterFee(alloc1, 1, "Year 1", new BigDecimal("25000.00"), LocalDate.of(2024, 6, 30)));
        semesterFeeRepository.save(new SemesterFee(alloc1, 2, "Year 2", new BigDecimal("25000.00"), LocalDate.of(2025, 6, 30)));
        semesterFeeRepository.save(new SemesterFee(alloc1, 3, "Year 3", new BigDecimal("25000.00"), LocalDate.of(2026, 6, 30)));
        semesterFeeRepository.save(new SemesterFee(alloc1, 4, "Year 4", new BigDecimal("15000.00"), LocalDate.of(2027, 6, 30)));

        StudentFeeAllocation alloc2 = studentFeeAllocationRepository.save(
                new StudentFeeAllocation(s6, diplomaProgram, new BigDecimal("65000.00"),
                        null, null, null, new BigDecimal("65000.00"), FeeAllocationStatus.FINALIZED));
        semesterFeeRepository.save(new SemesterFee(alloc2, 1, "Year 1", new BigDecimal("22000.00"), LocalDate.of(2024, 6, 30)));
        semesterFeeRepository.save(new SemesterFee(alloc2, 2, "Year 2", new BigDecimal("22000.00"), LocalDate.of(2025, 6, 30)));
        semesterFeeRepository.save(new SemesterFee(alloc2, 3, "Year 3", new BigDecimal("21000.00"), LocalDate.of(2026, 6, 30)));

        // ── 19. Equipment ────────────────────────────────────────────────────
        Equipment eq1 = equipmentRepository.save(new Equipment("Desktop Computer",     "ASSET001", EquipmentCategory.COMPUTER,    compLab,  EquipmentStatus.AVAILABLE));
        Equipment eq2 = equipmentRepository.save(new Equipment("Anatomy Model (Full)", "ASSET002", EquipmentCategory.ELECTRONIC,  anatLab,  EquipmentStatus.AVAILABLE));
        Equipment eq3 = equipmentRepository.save(new Equipment("Stethoscope Set",      "ASSET003", EquipmentCategory.MECHANICAL,  nfLab2,   EquipmentStatus.IN_USE));
        Equipment eq4 = equipmentRepository.save(new Equipment("Blood Pressure Monitor","ASSET004", EquipmentCategory.ELECTRONIC, nfLab2,   EquipmentStatus.AVAILABLE));
        Equipment eq5 = equipmentRepository.save(new Equipment("Projector System",     "ASSET005", EquipmentCategory.ELECTRONIC,  compLab,  EquipmentStatus.UNDER_MAINTENANCE));

        // ── 20. Inventory Items ──────────────────────────────────────────────
        InventoryItem inv1 = new InventoryItem("Surgical Gloves",   "INV001", nfLab2,  200);
        inv1.setMinimumQuantity(50); inv1.setUnit("Pairs"); inv1.setLastRestocked(LocalDate.of(2024, 10, 1));
        inventoryItemRepository.save(inv1);

        InventoryItem inv2 = new InventoryItem("Cotton Rolls",      "INV002", nfLab2,  15);
        inv2.setMinimumQuantity(5); inv2.setUnit("Rolls"); inv2.setLastRestocked(LocalDate.of(2024, 10, 1));
        inventoryItemRepository.save(inv2);

        InventoryItem inv3 = new InventoryItem("Bandages Box",      "INV003", nfLab2,  30);
        inv3.setMinimumQuantity(10); inv3.setUnit("Boxes"); inv3.setLastRestocked(LocalDate.of(2024, 10, 5));
        inventoryItemRepository.save(inv3);

        InventoryItem inv4 = new InventoryItem("Printing Paper",    "INV004", compLab, 20);
        inv4.setMinimumQuantity(5); inv4.setUnit("Reams"); inv4.setLastRestocked(LocalDate.of(2024, 11, 1));
        inventoryItemRepository.save(inv4);

        InventoryItem inv5 = new InventoryItem("Hand Sanitizer",    "INV005", compLab, 10);
        inv5.setMinimumQuantity(3); inv5.setUnit("Bottles"); inv5.setLastRestocked(LocalDate.of(2024, 11, 1));
        inventoryItemRepository.save(inv5);

        // ── 21. Maintenance Requests ─────────────────────────────────────────
        MaintenanceRequest mr1 = new MaintenanceRequest(eq5, "Projector Lamp Replacement",
                MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH, MaintenanceStatus.IN_PROGRESS,
                LocalDate.of(2025, 1, 10));
        mr1.setDescription("Projector lamp is not working. Needs replacement.");
        mr1.setRequestedBy(f1);
        mr1.setAssignedTo(f7);
        mr1.setEstimatedCost(new BigDecimal("3500.00"));
        maintenanceRequestRepository.save(mr1);

        MaintenanceRequest mr2 = new MaintenanceRequest(eq1, "Annual Computer Servicing",
                MaintenanceType.PREVENTIVE, MaintenancePriority.LOW, MaintenanceStatus.SCHEDULED,
                LocalDate.of(2025, 2, 1));
        mr2.setDescription("Annual preventive maintenance for all computers in Computer Lab.");
        mr2.setRequestedBy(f1);
        mr2.setScheduledDate(LocalDate.of(2025, 2, 15));
        mr2.setEstimatedCost(new BigDecimal("1200.00"));
        maintenanceRequestRepository.save(mr2);

        MaintenanceRequest mr3 = new MaintenanceRequest(eq3, "Stethoscope Calibration",
                MaintenanceType.ROUTINE, MaintenancePriority.MEDIUM, MaintenanceStatus.COMPLETED,
                LocalDate.of(2024, 12, 5));
        mr3.setDescription("Routine calibration and cleaning of stethoscope sets.");
        mr3.setRequestedBy(f7);
        mr3.setCompletionDate(LocalDate.of(2024, 12, 10));
        mr3.setActualCost(new BigDecimal("800.00"));
        maintenanceRequestRepository.save(mr3);

        // ── 22. Examinations + Exam Results ──────────────────────────────────
        Examination ex1 = examinationRepository.save(new Examination("Anatomy Mid-Term",               anatomy,    ExamType.THEORY,    LocalDate.of(2024, 9, 15),  180, 100, sem3));
        Examination ex2 = examinationRepository.save(new Examination("Anatomy End-Term",               anatomy,    ExamType.THEORY,    LocalDate.of(2024, 11, 20), 180, 100, sem3));
        Examination ex3 = examinationRepository.save(new Examination("Nursing Foundations Practical",  nfLab,      ExamType.PRACTICAL, LocalDate.of(2024, 10, 5),  120, 50,  sem3));
        Examination ex4 = examinationRepository.save(new Examination("Advanced Nursing Practice Viva", advNursing, ExamType.VIVA,      LocalDate.of(2024, 10, 20), 60,  50,  sem3));
        Examination ex5 = examinationRepository.save(new Examination("Basic Nursing Mid-Term",         basicNurs,  ExamType.THEORY,    LocalDate.of(2024, 9, 20),  120, 75,  sem3));

        examResultRepository.save(new ExamResult(ex1, s1,  new BigDecimal("72"), "B",  ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex1, s2,  new BigDecimal("85"), "A",  ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex1, s3,  new BigDecimal("68"), "C",  ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex1, s4,  new BigDecimal("90"), "A+", ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex2, s1,  new BigDecimal("78"), "B+", ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex2, s2,  new BigDecimal("91"), "A+", ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex3, s3,  new BigDecimal("44"), "A",  ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex3, s4,  new BigDecimal("48"), "A+", ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex4, s8,  new BigDecimal("43"), "A",  ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex5, s6,  new BigDecimal("60"), "B+", ExamResultStatus.PUBLISHED));
        examResultRepository.save(new ExamResult(ex5, s7,  new BigDecimal("55"), "B",  ExamResultStatus.PUBLISHED));

        // ── 23. Enquiries ────────────────────────────────────────────────────
        seedEnquiries(bachelorProgram, diplomaProgram, masterProgram, bscCourse, gnmCourse, ag1);

        // ── 24. Syllabi + Experiments + CO-PO Mappings ───────────────────────
        Syllabus syl1 = syllabusRepository.save(new Syllabus(anatomy, 1, 45, 15, 10,
                "To understand human body structure and physiological functions.",
                "Unit 1: Organization of Human Body\nUnit 2: Skeletal System\nUnit 3: Muscular System\nUnit 4: Cardiovascular System\nUnit 5: Nervous System",
                "Anatomy & Physiology – Ross & Wilson; Human Anatomy – Gray's",
                "Gray's Anatomy; Principles of Anatomy – Tortora",
                "CO1: Describe body structures\nCO2: Explain physiological functions\nCO3: Apply anatomical knowledge in nursing practice",
                true));

        Syllabus syl2 = syllabusRepository.save(new Syllabus(nfLab, 1, 0, 60, 0,
                "To develop practical nursing skills in a simulated environment.",
                "Unit 1: Bed Making\nUnit 2: Patient Hygiene\nUnit 3: Vital Signs\nUnit 4: Drug Administration\nUnit 5: Wound Care",
                "Fundamentals of Nursing – Potter & Perry; Clinical Nursing Procedures – Shirdi",
                "Taylor's Fundamentals of Nursing; Kozier & Erb's Fundamentals",
                "CO1: Perform basic nursing procedures\nCO2: Apply aseptic technique\nCO3: Document nursing care",
                true));

        Syllabus syl3 = syllabusRepository.save(new Syllabus(basicNurs, 1, 40, 20, 5,
                "To introduce core nursing concepts and professional values.",
                "Unit 1: Introduction to Nursing\nUnit 2: Health and Illness\nUnit 3: Patient Assessment\nUnit 4: Communication\nUnit 5: Safety and Infection Control",
                "Fundamentals of Nursing – Craven; Basic Concepts of Nursing – Henderson",
                "Nursing Theories – Meleis; Professional Nursing – Chitty",
                "CO1: Define nursing concepts\nCO2: Demonstrate basic patient assessment\nCO3: Practice therapeutic communication",
                true));

        Experiment exp1 = experimentRepository.save(new Experiment(nfLab,
                1, "Vital Signs Measurement", "Students practice measuring temperature, pulse, BP and SpO2",
                "To measure and record vital signs accurately",
                "Thermometer, BP cuff, stethoscope, pulse oximeter",
                "1. Measure temperature orally\n2. Palpate radial pulse for 1 min\n3. Measure BP using sphygmomanometer\n4. Measure SpO2",
                "Accurate vital sign readings within normal ranges",
                "Clinical accuracy in vital sign measurement; Documentation skills",
                45, true));

        Experiment exp2 = experimentRepository.save(new Experiment(nfLab,
                2, "Bed Making Technique", "Students practice occupied and unoccupied bed making",
                "To perform bed making maintaining patient comfort and infection control",
                "Hospital bed, linen set, draw sheet, mackintosh",
                "1. Gather required linen\n2. Strip bed\n3. Apply bottom sheet using mitered corners\n4. Position patient\n5. Complete top linen",
                "Neat and wrinkle-free bed maintaining patient comfort and safety",
                "Manual dexterity in bed making; Infection control practices",
                45, true));

        Experiment exp3 = experimentRepository.save(new Experiment(anatomy,
                1, "Skeletal System Identification", "Students identify bones using anatomical models",
                "To identify and label major bones of the human skeletal system",
                "Full skeletal model, bone chart, labeling pins",
                "1. Examine skull bones\n2. Identify vertebral column bones\n3. Label long bones\n4. Sketch and label diagram",
                "Correct identification of ≥ 90% of bones presented",
                "Structural knowledge of skeletal system; Anatomical terminology",
                60, true));

        labCurriculumMappingRepository.save(new LabCurriculumMapping(exp1, OutcomeType.COURSE_OUTCOME,   "CO1", "Measure vital signs accurately",          MappingLevel.HIGH,   "Direct skill practice"));
        labCurriculumMappingRepository.save(new LabCurriculumMapping(exp1, OutcomeType.PROGRAM_OUTCOME,  "PO3", "Apply clinical nursing skills",           MappingLevel.HIGH,   "Core clinical competency"));
        labCurriculumMappingRepository.save(new LabCurriculumMapping(exp2, OutcomeType.COURSE_OUTCOME,   "CO1", "Perform basic nursing procedures",        MappingLevel.HIGH,   "Direct lab practice"));
        labCurriculumMappingRepository.save(new LabCurriculumMapping(exp2, OutcomeType.PROGRAM_OUTCOME,  "PO2", "Demonstrate infection control practices", MappingLevel.HIGH,   "Key PO for nursing safety"));
        labCurriculumMappingRepository.save(new LabCurriculumMapping(exp3, OutcomeType.COURSE_OUTCOME,   "CO1", "Describe body structures",                MappingLevel.HIGH,   "Anatomy identification"));
        labCurriculumMappingRepository.save(new LabCurriculumMapping(exp3, OutcomeType.PROGRAM_SPECIFIC_OUTCOME, "PSO1", "Apply anatomy in clinical care",  MappingLevel.MEDIUM, "Foundation for clinical nursing"));

        // ── 25. Lab Schedules ────────────────────────────────────────────────
        labScheduleRepository.save(new LabSchedule(nfLab2, nfLab,  f7, slot1, "Batch A", DayOfWeek.MONDAY,    sem3, true));
        labScheduleRepository.save(new LabSchedule(nfLab2, nfLab,  f8, slot2, "Batch B", DayOfWeek.WEDNESDAY, sem3, true));
        labScheduleRepository.save(new LabSchedule(anatLab, anatomy, f4, slot1, "Batch A", DayOfWeek.TUESDAY,  sem3, true));
        labScheduleRepository.save(new LabSchedule(compLab, basicNurs, f7, slot3, "Batch A", DayOfWeek.FRIDAY, sem3, true));

        log.info("Seed data loaded successfully.");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void seedReferralTypes() {
        if (referralTypeRepository.count() > 0) return;
        referralTypeRepository.save(new ReferralType("Walk-In",        "WALK_IN",       new BigDecimal("0"),     false, "Direct walk-in enquiry",        true));
        referralTypeRepository.save(new ReferralType("Phone",          "PHONE",         new BigDecimal("0"),     false, "Phone enquiry",                  true));
        referralTypeRepository.save(new ReferralType("Online",         "ONLINE",        new BigDecimal("0"),     false, "Online enquiry",                 true));
        referralTypeRepository.save(new ReferralType("Agent Referral", "AGENT_REFERRAL",new BigDecimal("5000"),  true,  "Referred by external agent",     true));
        referralTypeRepository.save(new ReferralType("Staff",          "STAFF",         new BigDecimal("2000"),  true,  "Referred by staff member",       true));
        referralTypeRepository.save(new ReferralType("Alumni",         "ALUMNI",        new BigDecimal("1000"),  true,  "Referred by alumni",             true));
        referralTypeRepository.save(new ReferralType("Parent",         "PARENT",        new BigDecimal("0"),     false, "Referred by parent",             true));
        referralTypeRepository.save(new ReferralType("Advertisement",  "ADVERTISEMENT", new BigDecimal("0"),     false, "Through advertisement",          true));
    }

    private List<Department> seedDepartments() {
        Department gn  = departmentRepository.save(new Department("General Nursing",            "GN",  "Core clinical nursing education and practice", "Dr. Priya Sharma"));
        Department mo  = departmentRepository.save(new Department("Midwifery & Obstetrics",     "MO",  "Maternal and newborn care education",           "Dr. Lakshmi Devi"));
        Department chn = departmentRepository.save(new Department("Community Health Nursing",   "CHN", "Public health and community nursing",           "Dr. Anitha Rao"));
        Department msn = departmentRepository.save(new Department("Medical-Surgical Nursing",   "MSN", "Clinical nursing in medical and surgical wards", "Dr. Rajesh Kumar"));
        Department pn  = departmentRepository.save(new Department("Pediatric Nursing",          "PN",  "Child health nursing and neonatal care",         "Dr. Meena Pillai"));
        return List.of(gn, mo, chn, msn, pn);
    }

    private Program seedProgram(String name, String code, int durationYears,
                                List<Department> departments) {
        Program program = new Program(name, code, durationYears);
        program.getDepartments().addAll(departments);
        return programRepository.save(program);
    }

    private List<Student> seedStudents(Program bachelorProgram, Program diplomaProgram, Program masterProgram) {
        Student s1 = new Student("2024BSC001", "Aishwarya", "Rajput",   "aishwarya.rajput@student.cms.edu",   bachelorProgram, 1, LocalDate.of(2024, 6, 10), StudentStatus.ACTIVE);
        s1.setPhone("8765400001"); s1.setDateOfBirth(LocalDate.of(2004, 3, 15)); s1.setGender(Gender.FEMALE);
        s1.setNationality("Indian"); s1.setReligion("Hindu"); s1.setCommunityCategory(CommunityCategory.BC);
        s1.setBloodGroup(BloodGroup.O_POSITIVE); s1.setFatherName("Ramesh Rajput"); s1.setMotherName("Sunita Rajput");
        s1.setParentMobile("9876540001"); s1.setLabBatch("Batch A");

        Student s2 = new Student("2024BSC002", "Bhavana",   "Menon",    "bhavana.menon@student.cms.edu",      bachelorProgram, 1, LocalDate.of(2024, 6, 12), StudentStatus.ACTIVE);
        s2.setPhone("8765400002"); s2.setDateOfBirth(LocalDate.of(2004, 7, 22)); s2.setGender(Gender.FEMALE);
        s2.setNationality("Indian"); s2.setReligion("Hindu"); s2.setCommunityCategory(CommunityCategory.OC);
        s2.setBloodGroup(BloodGroup.A_POSITIVE); s2.setFatherName("Gopal Menon"); s2.setMotherName("Geetha Menon");
        s2.setParentMobile("9876540002"); s2.setLabBatch("Batch A");

        Student s3 = new Student("2023BSC003", "Chandrika", "Pillai",   "chandrika.pillai@student.cms.edu",   bachelorProgram, 2, LocalDate.of(2023, 6, 10), StudentStatus.ACTIVE);
        s3.setPhone("8765400003"); s3.setDateOfBirth(LocalDate.of(2003, 11, 5)); s3.setGender(Gender.FEMALE);
        s3.setNationality("Indian"); s3.setReligion("Christian"); s3.setCommunityCategory(CommunityCategory.OC);
        s3.setBloodGroup(BloodGroup.B_POSITIVE); s3.setFatherName("Suresh Pillai"); s3.setMotherName("Vimala Pillai");
        s3.setParentMobile("9876540003"); s3.setLabBatch("Batch B");

        Student s4 = new Student("2023BSC004", "Divya",     "Nair",     "divya.nair@student.cms.edu",         bachelorProgram, 2, LocalDate.of(2023, 6, 12), StudentStatus.ACTIVE);
        s4.setPhone("8765400004"); s4.setDateOfBirth(LocalDate.of(2003, 4, 18)); s4.setGender(Gender.FEMALE);
        s4.setNationality("Indian"); s4.setReligion("Hindu"); s4.setCommunityCategory(CommunityCategory.OC);
        s4.setBloodGroup(BloodGroup.AB_POSITIVE); s4.setFatherName("Sathish Nair"); s4.setMotherName("Rekha Nair");
        s4.setParentMobile("9876540004"); s4.setLabBatch("Batch B");

        Student s5 = new Student("2022BSC005", "Ezhilarasi","Thangaraj", "ezhilarasi.t@student.cms.edu",       bachelorProgram, 3, LocalDate.of(2022, 6, 8),  StudentStatus.ACTIVE);
        s5.setPhone("8765400005"); s5.setDateOfBirth(LocalDate.of(2002, 9, 12)); s5.setGender(Gender.FEMALE);
        s5.setNationality("Indian"); s5.setReligion("Hindu"); s5.setCommunityCategory(CommunityCategory.MBC);
        s5.setBloodGroup(BloodGroup.O_NEGATIVE); s5.setFatherName("Thangaraj S"); s5.setMotherName("Kavitha T");
        s5.setParentMobile("9876540005"); s5.setLabBatch("Batch A");

        Student s6 = new Student("2024GNM001", "Fathima",   "Begum",    "fathima.begum@student.cms.edu",      diplomaProgram, 1, LocalDate.of(2024, 6, 14), StudentStatus.ACTIVE);
        s6.setPhone("8765400006"); s6.setDateOfBirth(LocalDate.of(2004, 1, 25)); s6.setGender(Gender.FEMALE);
        s6.setNationality("Indian"); s6.setReligion("Muslim"); s6.setCommunityCategory(CommunityCategory.BC);
        s6.setBloodGroup(BloodGroup.B_POSITIVE); s6.setFatherName("Abdul Begum"); s6.setMotherName("Noor Begum");
        s6.setParentMobile("9876540006"); s6.setLabBatch("Batch A");

        Student s7 = new Student("2024GNM002", "Geetha",    "Kumari",   "geetha.kumari@student.cms.edu",      diplomaProgram, 1, LocalDate.of(2024, 6, 16), StudentStatus.ACTIVE);
        s7.setPhone("8765400007"); s7.setDateOfBirth(LocalDate.of(2004, 6, 8));  s7.setGender(Gender.FEMALE);
        s7.setNationality("Indian"); s7.setReligion("Hindu"); s7.setCommunityCategory(CommunityCategory.SC);
        s7.setBloodGroup(BloodGroup.A_POSITIVE); s7.setFatherName("Murugan K"); s7.setMotherName("Selvi K");
        s7.setParentMobile("9876540007"); s7.setLabBatch("Batch B");

        Student s8 = new Student("2024MSC001", "Harini",    "Sundaram",  "harini.sundaram@student.cms.edu",   masterProgram, 1, LocalDate.of(2024, 6, 15), StudentStatus.ACTIVE);
        s8.setPhone("8765400008"); s8.setDateOfBirth(LocalDate.of(2000, 5, 30)); s8.setGender(Gender.FEMALE);
        s8.setNationality("Indian"); s8.setReligion("Hindu"); s8.setCommunityCategory(CommunityCategory.OC);
        s8.setBloodGroup(BloodGroup.O_POSITIVE); s8.setFatherName("Sundaram V"); s8.setMotherName("Padma S");
        s8.setParentMobile("9876540008");

        Student s9 = new Student("2021BSC006", "Indira",    "Mohan",    "indira.mohan@student.cms.edu",       bachelorProgram, 4, LocalDate.of(2021, 6, 7),  StudentStatus.ON_LEAVE);
        s9.setPhone("8765400009"); s9.setDateOfBirth(LocalDate.of(2001, 8, 17)); s9.setGender(Gender.FEMALE);
        s9.setNationality("Indian"); s9.setReligion("Hindu"); s9.setCommunityCategory(CommunityCategory.BC);
        s9.setBloodGroup(BloodGroup.A_NEGATIVE); s9.setFatherName("Mohan D"); s9.setMotherName("Devi M");
        s9.setParentMobile("9876540009");

        Student s10 = new Student("2024BSC007", "Jayanthi", "Krishnan", "jayanthi.krishnan@student.cms.edu",  bachelorProgram, 1, LocalDate.of(2024, 6, 18), StudentStatus.ACTIVE);
        s10.setPhone("8765400010"); s10.setDateOfBirth(LocalDate.of(2004, 12, 2)); s10.setGender(Gender.FEMALE);
        s10.setNationality("Indian"); s10.setReligion("Hindu"); s10.setCommunityCategory(CommunityCategory.MBC);
        s10.setBloodGroup(BloodGroup.B_NEGATIVE); s10.setFatherName("Krishnan R"); s10.setMotherName("Meena K");
        s10.setParentMobile("9876540010"); s10.setLabBatch("Batch A");

        return studentRepository.saveAll(List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10));
    }

    private void seedEnquiries(Program bachelorProgram, Program diplomaProgram, Program masterProgram,
                               Course bscCourse, Course gnmCourse, Agent ag1) {
        ReferralType walkIn   = referralTypeRepository.findByCode("WALK_IN").orElseThrow();
        ReferralType phone    = referralTypeRepository.findByCode("PHONE").orElseThrow();
        ReferralType agentRef = referralTypeRepository.findByCode("AGENT_REFERRAL").orElseThrow();
        ReferralType online   = referralTypeRepository.findByCode("ONLINE").orElseThrow();

        Enquiry e1 = new Enquiry("Aarav Patel",    "aarav.patel@email.com",    "9000000001", bachelorProgram, LocalDate.of(2025, 1, 5),  walkIn,   EnquiryStatus.ENQUIRED);
        e1.setCourse(bscCourse); e1.setRemarks("Interested in B.Sc Nursing. Walk-in visit from Coimbatore.");
        enquiryRepository.save(e1);

        Enquiry e2 = new Enquiry("Priya Krishnan", "priya.k@email.com",        "9000000002", diplomaProgram, LocalDate.of(2025, 1, 8),  phone,    EnquiryStatus.INTERESTED);
        e2.setCourse(gnmCourse); e2.setRemarks("Called to enquire about GNM program. Interested in scholarship options.");
        e2.setFeeDiscussedAmount(new BigDecimal("65000.00"));
        enquiryRepository.save(e2);

        Enquiry e3 = new Enquiry("Vikram Reddy",   "vikram.r@email.com",       "9000000003", bachelorProgram, LocalDate.of(2025, 1, 12), agentRef, EnquiryStatus.FEES_FINALIZED);
        e3.setCourse(bscCourse); e3.setAgent(ag1); e3.setRemarks("Agent referral from Srinivas Education Services.");
        e3.setFeeDiscussedAmount(new BigDecimal("95000.00"));
        e3.setFinalizedTotalFee(new BigDecimal("95000.00"));
        e3.setFinalizedDiscountAmount(new BigDecimal("5000.00"));
        e3.setFinalizedDiscountReason("Agent referral discount");
        e3.setFinalizedNetFee(new BigDecimal("90000.00"));
        e3.setFinalizedBy("admin");
        enquiryRepository.save(e3);

        Enquiry e4 = new Enquiry("Sneha Iyer",     "sneha.iyer@email.com",     "9000000004", diplomaProgram, LocalDate.of(2024, 12, 10), online,  EnquiryStatus.CONVERTED);
        e4.setCourse(gnmCourse); e4.setRemarks("Online enquiry, documents submitted and converted to student.");
        e4.setConvertedStudentId(6L);
        enquiryRepository.save(e4);

        Enquiry e5 = new Enquiry("Raju Sharma",    "raju.sharma@email.com",    "9000000005", masterProgram, LocalDate.of(2025, 1, 20), walkIn,   EnquiryStatus.NOT_INTERESTED);
        e5.setRemarks("Visited campus but not interested due to distance from home.");
        enquiryRepository.save(e5);

        Enquiry e6 = new Enquiry("Meena Selvam",   "meena.selvam@email.com",   "9000000006", bachelorProgram, LocalDate.of(2025, 2, 3),  phone,    EnquiryStatus.DOCUMENTS_SUBMITTED);
        e6.setCourse(bscCourse); e6.setRemarks("All documents submitted. Pending approval.");
        e6.setFeeDiscussedAmount(new BigDecimal("95000.00"));
        enquiryRepository.save(e6);

        Enquiry e7 = new Enquiry("Arjun Verma",    "arjun.verma@email.com",    "9000000007", diplomaProgram, LocalDate.of(2025, 2, 10), walkIn,   EnquiryStatus.INTERESTED);
        e7.setCourse(gnmCourse); e7.setRemarks("Walk-in from Trichy. Very interested in GNM course.");
        enquiryRepository.save(e7);
    }
}
