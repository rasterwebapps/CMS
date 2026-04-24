package com.cms.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.*;
import com.cms.model.enums.*;
import com.cms.repository.*;

/**
 * Seeds demo data for SKS College of Nursing when running with the 'local' profile.
 * This ensures all screens have meaningful data for development and testing.
 */
@Configuration
@Profile("local")
public class LocalDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSeeder.class);

    @Bean
    @Transactional
    CommandLineRunner seedData(
            DepartmentRepository departmentRepo,
            ProgramRepository programRepo,
            CourseRepository courseRepo,
            SubjectRepository subjectRepo,
            AcademicYearRepository academicYearRepo,
            SemesterRepository semesterRepo,
            FacultyRepository facultyRepo,
            StudentRepository studentRepo,
            LabRepository labRepo,
            EquipmentRepository equipmentRepo,
            InventoryItemRepository inventoryRepo,
            MaintenanceRequestRepository maintenanceRepo,
            ExaminationRepository examRepo,
            ExamResultRepository examResultRepo,
            AttendanceRepository attendanceRepo,
            AgentRepository agentRepo,
            EnquiryRepository enquiryRepo,
            FeeStructureRepository feeStructureRepo,
            FeePaymentRepository feePaymentRepo,
            ReferralTypeRepository referralTypeRepo,
            LabScheduleRepository labScheduleRepo,
            LabSlotRepository labSlotRepo,
            SyllabusRepository syllabusRepo,
            ExperimentRepository experimentRepo,
            SystemConfigurationRepository sysConfigRepo) {
        
        return args -> {
            if (departmentRepo.count() > 0) {
                log.info("Database already seeded, skipping...");
                return;
            }
            
            log.info("🌱 Seeding demo data for SKS College of Nursing...");

            // ═══════════════════════════════════════════════════════════════
            // 1. DEPARTMENTS
            // ═══════════════════════════════════════════════════════════════
            Department deptMSN = departmentRepo.save(new Department("Medical-Surgical Nursing", "MSN",
                "Department of Medical-Surgical Nursing — covers adult health, perioperative care, critical care, and oncology nursing.", "Dr. S. Tamilarasi"));
            Department deptCHN = departmentRepo.save(new Department("Community Health Nursing", "CHN",
                "Department of Community Health Nursing — focuses on public health, epidemiology, family health, and primary healthcare delivery.", "Dr. K. Vasanthi"));
            Department deptCHD = departmentRepo.save(new Department("Child Health (Paediatric) Nursing", "CHD",
                "Department of Child Health Nursing — covers neonatal care, growth & development, paediatric diseases, and child nutrition.", "Dr. R. Meenakshi"));
            Department deptOBG = departmentRepo.save(new Department("Obstetrics & Gynaecological Nursing", "OBG",
                "Department of Obstetrics & Gynaecological Nursing — antenatal, intranatal, postnatal care, reproductive health, and midwifery.", "Dr. P. Selvarani"));
            Department deptMHN = departmentRepo.save(new Department("Mental Health (Psychiatric) Nursing", "MHN",
                "Department of Mental Health Nursing — psychiatric disorders, therapeutic communication, psychopharmacology, and rehabilitation.", "Dr. M. Kavitha"));
            Department deptNFD = departmentRepo.save(new Department("Nursing Foundation", "NFD",
                "Department of Nursing Foundation — fundamental nursing skills, nursing ethics, nursing process, and basic patient care.", "Mrs. L. Jayalakshmi"));
            Department deptNEA = departmentRepo.save(new Department("Nursing Education & Administration", "NEA",
                "Department of Nursing Education & Administration — teaching methodologies, curriculum development, hospital management.", "Dr. A. Padmavathi"));
            log.info("✓ Created 7 departments");

            // ═══════════════════════════════════════════════════════════════
            // 2. PROGRAMS
            // ═══════════════════════════════════════════════════════════════
            Program progBAC = programRepo.save(createProgram("Bachelors", "BAC", 4));
            Program progMAS = programRepo.save(createProgram("Masters", "MAS", 2));
            Program progDIP = programRepo.save(createProgram("Diploma", "DIP", 2));
            Program progDOC = programRepo.save(createProgram("Doctoral", "DOC", 2));
            Program progSPE = programRepo.save(createProgram("Specialities", "SPE", 2));
            log.info("✓ Created 5 programs");

            // ═══════════════════════════════════════════════════════════════
            // 3. COURSES
            // ═══════════════════════════════════════════════════════════════
            Course courseBSCN = courseRepo.save(createCourse("B.Sc. Nursing", "BSCN-C", null, progBAC));
            Course courseMSCMSN = courseRepo.save(createCourse("M.Sc. Medical-Surgical Nursing", "MSCMSN-C", "Medical-Surgical", progMAS));
            Course courseGNM = courseRepo.save(createCourse("General Nursing and Midwifery", "GNM-C", null, progDIP));
            log.info("✓ Created 4 courses");

            // ═══════════════════════════════════════════════════════════════
            // 4. ACADEMIC YEARS & SEMESTERS
            // ═══════════════════════════════════════════════════════════════
            AcademicYear ay2025 = academicYearRepo.save(new AcademicYear("2025-2026", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true));
            AcademicYear ay2024 = academicYearRepo.save(new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false));

            Semester sem1 = semesterRepo.save(createSemester("Semester 1", 1, ay2025, LocalDate.of(2025, 6, 15), LocalDate.of(2025, 11, 30)));
            Semester sem2 = semesterRepo.save(createSemester("Semester 2", 2, ay2025, LocalDate.of(2025, 12, 1), LocalDate.of(2026, 5, 15)));
            log.info("✓ Created 2 academic years and 2 semesters");

            // ═══════════════════════════════════════════════════════════════
            // 5. SUBJECTS
            // ═══════════════════════════════════════════════════════════════
            Subject subAnatomy = subjectRepo.save(createSubject("Anatomy", "BSN101", 4, 3, 1, courseBSCN, deptNFD, 1));
            Subject subPhysiology = subjectRepo.save(createSubject("Physiology", "BSN102", 4, 3, 1, courseBSCN, deptNFD, 1));
            Subject subNursingFoundation = subjectRepo.save(createSubject("Nursing Foundation", "BSN103", 6, 3, 3, courseBSCN, deptNFD, 1));
            Subject subBiochemistry = subjectRepo.save(createSubject("Biochemistry", "BSN104", 3, 2, 1, courseBSCN, deptNFD, 2));
            Subject subNutrition = subjectRepo.save(createSubject("Nutrition & Dietetics", "BSN105", 3, 2, 1, courseBSCN, deptCHN, 2));
            Subject subMicrobiology = subjectRepo.save(createSubject("Microbiology", "BSN106", 4, 3, 1, courseBSCN, deptMSN, 2));
            Subject subMSN1 = subjectRepo.save(createSubject("Medical-Surgical Nursing I", "BSN201", 6, 3, 3, courseBSCN, deptMSN, 3));
            Subject subPharmacology = subjectRepo.save(createSubject("Pharmacology", "BSN202", 4, 3, 1, courseBSCN, deptMSN, 3));
            Subject subChildHealth = subjectRepo.save(createSubject("Child Health Nursing", "BSN302", 5, 2, 3, courseBSCN, deptCHD, 5));
            Subject subMentalHealth = subjectRepo.save(createSubject("Mental Health Nursing", "BSN303", 5, 2, 3, courseBSCN, deptMHN, 6));
            Subject subOBGNursing = subjectRepo.save(createSubject("OBG Nursing", "BSN304", 5, 2, 3, courseBSCN, deptOBG, 6));
            Subject subNursingResearch = subjectRepo.save(createSubject("Nursing Research & Statistics", "BSN402", 3, 3, 0, courseBSCN, deptNEA, 7));
            log.info("✓ Created 12 subjects");

            // ═══════════════════════════════════════════════════════════════
            // 6. FACULTY
            // ═══════════════════════════════════════════════════════════════
            Faculty f1 = facultyRepo.save(createFaculty("Dr. S.", "Tamilarasi", "tamilarasi@sks.edu", "9876543210", "FAC001", deptMSN, Designation.PROFESSOR, "Medical-Surgical Nursing"));
            Faculty f2 = facultyRepo.save(createFaculty("Dr. K.", "Vasanthi", "vasanthi@sks.edu", "9876543211", "FAC002", deptCHN, Designation.PROFESSOR, "Public Health"));
            Faculty f3 = facultyRepo.save(createFaculty("Dr. R.", "Meenakshi", "meenakshi@sks.edu", "9876543212", "FAC003", deptCHD, Designation.ASSOCIATE_PROFESSOR, "Child Health Nursing"));
            Faculty f4 = facultyRepo.save(createFaculty("Dr. P.", "Selvarani", "selvarani@sks.edu", "9876543213", "FAC004", deptOBG, Designation.PROFESSOR, "OBG Nursing"));
            Faculty f5 = facultyRepo.save(createFaculty("Dr. M.", "Kavitha", "kavitha@sks.edu", "9876543214", "FAC005", deptMHN, Designation.ASSOCIATE_PROFESSOR, "Psychiatric Nursing"));
            Faculty f6 = facultyRepo.save(createFaculty("Mrs. L.", "Jayalakshmi", "jayalakshmi@sks.edu", "9876543215", "FAC006", deptNFD, Designation.ASSISTANT_PROFESSOR, "Nursing Foundation"));
            Faculty f7 = facultyRepo.save(createFaculty("Dr. A.", "Padmavathi", "padmavathi@sks.edu", "9876543216", "FAC007", deptNEA, Designation.PROFESSOR, "Nursing Administration"));
            Faculty f8 = facultyRepo.save(createFaculty("Mrs. B.", "Lakshmi", "lakshmi@sks.edu", "9876543217", "FAC008", deptMSN, Designation.LECTURER, "Medical-Surgical Nursing"));
            Faculty f9 = facultyRepo.save(createFaculty("Mr. C.", "Rajan", "rajan@sks.edu", "9876543218", "FAC009", deptCHN, Designation.LECTURER, "Community Health"));
            Faculty f10 = facultyRepo.save(createFaculty("Mrs. D.", "Priya", "priya@sks.edu", "9876543219", "FAC010", deptCHD, Designation.LECTURER, "Child Health Nursing"));
            log.info("✓ Created 10 faculty members");

            // ═══════════════════════════════════════════════════════════════
            // 7. STUDENTS
            // ═══════════════════════════════════════════════════════════════
            Student s1 = studentRepo.save(createStudent("Arun", "Kumar", "arun.kumar@sks.edu", "9876500001", "STU2025001", progBAC, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s2 = studentRepo.save(createStudent("Divya", "Sharma", "divya.sharma@sks.edu", "9876500002", "STU2025002", progBAC, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s3 = studentRepo.save(createStudent("Karthik", "Raja", "karthik.raja@sks.edu", "9876500003", "STU2025003", progBAC, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s4 = studentRepo.save(createStudent("Lakshmi", "Priya", "lakshmi.priya@sks.edu", "9876500004", "STU2025004", progBAC, 2, StudentStatus.ACTIVE, LocalDate.of(2024, 6, 15)));
            Student s5 = studentRepo.save(createStudent("Mohan", "Raj", "mohan.raj@sks.edu", "9876500005", "STU2025005", progDIP, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s6 = studentRepo.save(createStudent("Nandhini", "S", "nandhini.s@sks.edu", "9876500006", "STU2025006", progDIP, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s7 = studentRepo.save(createStudent("Pradeep", "V", "pradeep.v@sks.edu", "9876500007", "STU2024001", progMAS, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s8 = studentRepo.save(createStudent("Revathi", "M", "revathi.m@sks.edu", "9876500008", "STU2024002", progDIP, 1, StudentStatus.ACTIVE, LocalDate.of(2025, 6, 15)));
            Student s9 = studentRepo.save(createStudent("Saranya", "K", "saranya.k@sks.edu", "9876500009", "STU2023001", progBAC, 3, StudentStatus.ACTIVE, LocalDate.of(2023, 6, 15)));
            Student s10 = studentRepo.save(createStudent("Vignesh", "P", "vignesh.p@sks.edu", "9876500010", "STU2022001", progBAC, 4, StudentStatus.ACTIVE, LocalDate.of(2022, 6, 15)));
            Student s11 = studentRepo.save(createStudent("Anitha", "R", "anitha.r@sks.edu", "9876500011", "STU2021001", progBAC, 4, StudentStatus.GRADUATED, LocalDate.of(2021, 6, 15)));
            Student s12 = studentRepo.save(createStudent("Bala", "S", "bala.s@sks.edu", "9876500012", "STU2023002", progDIP, 2, StudentStatus.INACTIVE, LocalDate.of(2023, 6, 15)));
            log.info("✓ Created 12 students");

            // ═══════════════════════════════════════════════════════════════
            // 8. LABS
            // ═══════════════════════════════════════════════════════════════
            Lab lab1 = labRepo.save(createLab("Nursing Foundation Skills Lab", LabType.OTHER, "A-Block", "GF-01", 40, deptNFD));
            Lab lab2 = labRepo.save(createLab("Anatomy & Physiology Lab", LabType.BIOLOGY, "B-Block", "1F-01", 35, deptNFD));
            Lab lab3 = labRepo.save(createLab("Medical-Surgical Simulation Lab", LabType.OTHER, "A-Block", "1F-02", 30, deptMSN));
            Lab lab4 = labRepo.save(createLab("OBG Skills Lab", LabType.OTHER, "C-Block", "GF-01", 25, deptOBG));
            Lab lab5 = labRepo.save(createLab("Paediatric Simulation Lab", LabType.OTHER, "C-Block", "1F-01", 25, deptCHD));
            Lab lab6 = labRepo.save(createLab("Community Health Resource Center", LabType.OTHER, "D-Block", "GF-01", 50, deptCHN));
            Lab lab7 = labRepo.save(createLab("Mental Health Practice Lab", LabType.OTHER, "B-Block", "2F-01", 20, deptMHN));
            Lab lab8 = labRepo.save(createLab("Computer & E-Learning Lab", LabType.COMPUTER, "E-Block", "GF-01", 60, deptNEA));
            Lab lab9 = labRepo.save(createLab("Microbiology Lab", LabType.BIOLOGY, "B-Block", "GF-01", 30, deptMSN));
            Lab lab10 = labRepo.save(createLab("Nutrition & Dietetics Lab", LabType.CHEMISTRY, "D-Block", "1F-01", 30, deptCHN));
            log.info("✓ Created 10 labs");

            // ═══════════════════════════════════════════════════════════════
            // 9. EQUIPMENT
            // ═══════════════════════════════════════════════════════════════
            Equipment eq1 = equipmentRepo.save(createEquipment("Adult CPR Manikin", "EQ-CPR001", EquipmentCategory.MECHANICAL, lab1, EquipmentStatus.AVAILABLE, "Laerdal Medical", "Resusci Anne QCPR"));
            Equipment eq2 = equipmentRepo.save(createEquipment("Anatomical Skeleton Model", "EQ-ANT001", EquipmentCategory.MECHANICAL, lab2, EquipmentStatus.AVAILABLE, "3B Scientific", "Classic Skeleton Stan"));
            Equipment eq3 = equipmentRepo.save(createEquipment("ECG Machine", "EQ-ECG001", EquipmentCategory.ELECTRONIC, lab3, EquipmentStatus.AVAILABLE, "BPL Medical", "Cardiart 6108T"));
            Equipment eq4 = equipmentRepo.save(createEquipment("Birthing Simulator", "EQ-OBG001", EquipmentCategory.MECHANICAL, lab4, EquipmentStatus.AVAILABLE, "Gaumard", "NOELLE S550"));
            Equipment eq5 = equipmentRepo.save(createEquipment("Infant Warmer", "EQ-INF001", EquipmentCategory.ELECTRONIC, lab5, EquipmentStatus.UNDER_MAINTENANCE, "Phoenix Medical", "Neo-101"));
            Equipment eq6 = equipmentRepo.save(createEquipment("Blood Pressure Monitor Set", "EQ-BP001", EquipmentCategory.ELECTRONIC, lab1, EquipmentStatus.AVAILABLE, "Omron", "HEM-7120"));
            Equipment eq7 = equipmentRepo.save(createEquipment("Microscope", "EQ-MIC001", EquipmentCategory.ELECTRONIC, lab9, EquipmentStatus.AVAILABLE, "Olympus", "CX23"));
            Equipment eq8 = equipmentRepo.save(createEquipment("IV Arm Training Model", "EQ-IV001", EquipmentCategory.MECHANICAL, lab3, EquipmentStatus.AVAILABLE, "Nasco", "Life/form Venipuncture"));
            Equipment eq9 = equipmentRepo.save(createEquipment("Foetal Heart Rate Monitor", "EQ-FHR001", EquipmentCategory.ELECTRONIC, lab4, EquipmentStatus.AVAILABLE, "Edan", "SD3 Plus"));
            Equipment eq10 = equipmentRepo.save(createEquipment("Pulse Oximeter", "EQ-POX001", EquipmentCategory.ELECTRONIC, lab3, EquipmentStatus.OUT_OF_ORDER, "Nellcor", "PM10N"));
            log.info("✓ Created 10 equipment items");

            // ═══════════════════════════════════════════════════════════════
            // 10. INVENTORY ITEMS
            // ═══════════════════════════════════════════════════════════════
            inventoryRepo.save(createInventory("Disposable Syringes (5ml)", "INV-SYR5ML", lab1, 500, 100, "pieces"));
            inventoryRepo.save(createInventory("Disposable Gloves (Box of 100)", "INV-GLV001", lab1, 50, 10, "boxes"));
            inventoryRepo.save(createInventory("Gauze Rolls", "INV-GAU001", lab3, 200, 40, "rolls"));
            inventoryRepo.save(createInventory("Bandages (Assorted)", "INV-BND001", lab1, 150, 30, "pieces"));
            inventoryRepo.save(createInventory("Cotton Swabs (Box)", "INV-CSW001", lab3, 80, 20, "boxes"));
            inventoryRepo.save(createInventory("Stethoscope", "INV-STH001", lab1, 25, 5, "pieces"));
            inventoryRepo.save(createInventory("Thermometer Digital", "INV-THM001", lab1, 30, 6, "pieces"));
            inventoryRepo.save(createInventory("Glass Slides (Box of 50)", "INV-GLS001", lab9, 40, 10, "boxes"));
            inventoryRepo.save(createInventory("IV Cannula (Various Sizes)", "INV-IVC001", lab3, 300, 50, "pieces"));
            inventoryRepo.save(createInventory("Betadine Solution (500ml)", "INV-BET001", lab3, 25, 5, "bottles"));
            log.info("✓ Created 10 inventory items");

            // ═══════════════════════════════════════════════════════════════
            // 11. MAINTENANCE REQUESTS
            // ═══════════════════════════════════════════════════════════════
            maintenanceRepo.save(createMaintenance("Infant Warmer Calibration", "Temperature calibration required", eq5, MaintenanceType.CORRECTIVE, MaintenanceStatus.IN_PROGRESS, MaintenancePriority.HIGH));
            maintenanceRepo.save(createMaintenance("ECG Machine Annual Service", "Annual servicing of ECG machine", eq3, MaintenanceType.PREVENTIVE, MaintenanceStatus.SCHEDULED, MaintenancePriority.MEDIUM));
            maintenanceRepo.save(createMaintenance("Pulse Oximeter Repair", "Display not working", eq10, MaintenanceType.CORRECTIVE, MaintenanceStatus.PENDING, MaintenancePriority.HIGH));
            maintenanceRepo.save(createMaintenance("CPR Manikin Lung Replacement", "Lung replacement needed", eq1, MaintenanceType.CORRECTIVE, MaintenanceStatus.COMPLETED, MaintenancePriority.MEDIUM));
            maintenanceRepo.save(createMaintenance("Microscope Lens Cleaning", "Routine lens cleaning", eq7, MaintenanceType.ROUTINE, MaintenanceStatus.COMPLETED, MaintenancePriority.LOW));
            log.info("✓ Created 5 maintenance requests");

            // ═══════════════════════════════════════════════════════════════
            // 12. REFERRAL TYPES
            // ═══════════════════════════════════════════════════════════════
            ReferralType rt1 = referralTypeRepo.save(createReferralType("Walk-In", "WALK_IN", BigDecimal.ZERO, false, "Direct walk-in enquiry without any referral"));
            ReferralType rt2 = referralTypeRepo.save(createReferralType("Phone Enquiry", "PHONE", BigDecimal.ZERO, false, "Enquiry received via phone call"));
            ReferralType rt3 = referralTypeRepo.save(createReferralType("Website", "WEBSITE", BigDecimal.ZERO, false, "Enquiry from college website"));
            ReferralType rt4 = referralTypeRepo.save(createReferralType("Agent Referral", "AGENT_REFERRAL", new BigDecimal("5000.00"), true, "Referred by a registered recruitment agent"));
            ReferralType rt5 = referralTypeRepo.save(createReferralType("Alumni Referral", "ALUMNI", BigDecimal.ZERO, false, "Referred by college alumni"));
            ReferralType rt6 = referralTypeRepo.save(createReferralType("Social Media", "SOCIAL_MEDIA", BigDecimal.ZERO, false, "Enquiry from social media platforms"));
            log.info("✓ Created 6 referral types");

            // ═══════════════════════════════════════════════════════════════
            // 13. AGENTS
            // ═══════════════════════════════════════════════════════════════
            Agent agent1 = agentRepo.save(createAgent("Southern Nursing Academy", "contact@southernnursing.com", "9845012345", "Chennai", "South Chennai", true));
            Agent agent2 = agentRepo.save(createAgent("Career Path Consultants", "info@careerpath.in", "9845012346", "Coimbatore", "RS Puram", true));
            Agent agent3 = agentRepo.save(createAgent("TN Education Services", "tnedu@services.com", "9845012347", "Madurai", "Anna Nagar", true));
            Agent agent4 = agentRepo.save(createAgent("Nursing Admissions Hub", "admissions@nursinghub.in", "9845012348", "Trichy", "Cantonment", false));
            Agent agent5 = agentRepo.save(createAgent("Salem Study Circle", "salem.circle@edu.in", "9845012349", "Salem", "Hasthampatti", true));
            log.info("✓ Created 5 agents");

            // ═══════════════════════════════════════════════════════════════
            // 14. ENQUIRIES
            // ═══════════════════════════════════════════════════════════════
            enquiryRepo.save(createEnquiry("Harini B", "harini.b@email.com", "9876567890", progBAC, rt1, EnquiryStatus.FEES_FINALIZED, null, LocalDate.of(2026, 4, 3)));
            enquiryRepo.save(createEnquiry("Swetha P", "swetha.p@email.com", "9876578901", progDIP, rt2, EnquiryStatus.ENQUIRED, null, LocalDate.of(2026, 4, 6)));
            enquiryRepo.save(createEnquiry("Malar K", "malar.k@email.com", "9876589012", progDIP, rt4, EnquiryStatus.INTERESTED, agent1, LocalDate.of(2026, 4, 9)));
            enquiryRepo.save(createEnquiry("Jayanthi R", "jayanthi.r@email.com", "9876590123", progBAC, rt1, EnquiryStatus.NOT_INTERESTED, null, LocalDate.of(2026, 4, 12)));
            enquiryRepo.save(createEnquiry("Vijay S", "vijay.s@email.com", "9876501234", progMAS, rt3, EnquiryStatus.ENQUIRED, null, LocalDate.of(2026, 4, 14)));
            enquiryRepo.save(createEnquiry("Anbu M", "anbu.m@email.com", "9876512345", progBAC, rt6, EnquiryStatus.INTERESTED, null, LocalDate.of(2026, 4, 15)));
            enquiryRepo.save(createEnquiry("Deepa L", "deepa.l@email.com", "9876523456", progDIP, rt4, EnquiryStatus.FEES_FINALIZED, agent2, LocalDate.of(2026, 4, 10)));
            enquiryRepo.save(createEnquiry("Ganesh T", "ganesh.t@email.com", "9876534567", progBAC, rt5, EnquiryStatus.FEES_FINALIZED, null, LocalDate.of(2026, 4, 5)));
            log.info("✓ Created 8 enquiries");

            // ═══════════════════════════════════════════════════════════════
            // 15. FEE STRUCTURES
            // ═══════════════════════════════════════════════════════════════
            FeeStructure fs1 = feeStructureRepo.save(createFeeStructure(progBAC, ay2025, FeeType.TUITION, new BigDecimal("75000.00")));
            FeeStructure fs2 = feeStructureRepo.save(createFeeStructure(progBAC, ay2025, FeeType.LAB_FEE, new BigDecimal("10000.00")));
            FeeStructure fs3 = feeStructureRepo.save(createFeeStructure(progMAS, ay2025, FeeType.TUITION, new BigDecimal("100000.00")));
            FeeStructure fs4 = feeStructureRepo.save(createFeeStructure(progDIP, ay2025, FeeType.TUITION, new BigDecimal("100000.00")));
            FeeStructure fs5 = feeStructureRepo.save(createFeeStructure(progDIP, ay2025, FeeType.TUITION, new BigDecimal("55000.00")));
            FeeStructure fs6 = feeStructureRepo.save(createFeeStructure(progDIP, ay2025, FeeType.LAB_FEE, new BigDecimal("10000.00")));
            log.info("✓ Created 6 fee structures");

            // ═══════════════════════════════════════════════════════════════
            // 16. FEE PAYMENTS
            // ═══════════════════════════════════════════════════════════════
            feePaymentRepo.save(createFeePayment(s1, fs1, "RCP-2025-001", new BigDecimal("37500.00"), PaymentMode.UPI, PaymentStatus.COMPLETED, LocalDate.of(2025, 6, 20), "TXN001"));
            feePaymentRepo.save(createFeePayment(s1, fs1, "RCP-2025-002", new BigDecimal("37500.00"), PaymentMode.BANK_TRANSFER, PaymentStatus.COMPLETED, LocalDate.of(2025, 12, 15), "TXN002"));
            feePaymentRepo.save(createFeePayment(s2, fs1, "RCP-2025-003", new BigDecimal("75000.00"), PaymentMode.CHEQUE, PaymentStatus.COMPLETED, LocalDate.of(2025, 6, 18), "CHQ-12345"));
            feePaymentRepo.save(createFeePayment(s3, fs1, "RCP-2025-004", new BigDecimal("37500.00"), PaymentMode.CASH, PaymentStatus.COMPLETED, LocalDate.of(2025, 6, 22), "REC-001"));
            feePaymentRepo.save(createFeePayment(s5, fs5, "RCP-2025-005", new BigDecimal("27500.00"), PaymentMode.UPI, PaymentStatus.COMPLETED, LocalDate.of(2025, 6, 25), "TXN003"));
            feePaymentRepo.save(createFeePayment(s7, fs3, "RCP-2025-006", new BigDecimal("50000.00"), PaymentMode.BANK_TRANSFER, PaymentStatus.COMPLETED, LocalDate.of(2025, 6, 28), "TXN004"));
            log.info("✓ Created 6 fee payments");

            // ═══════════════════════════════════════════════════════════════
            // 17. EXAMINATIONS
            // ═══════════════════════════════════════════════════════════════
            Examination exam1 = examRepo.save(createExamination("Mid-Semester Theory Exam", subAnatomy, ExamType.THEORY, LocalDate.of(2025, 9, 15), 120, 100, sem1));
            Examination exam2 = examRepo.save(createExamination("End-Semester Theory Exam", subAnatomy, ExamType.THEORY, LocalDate.of(2025, 11, 20), 180, 100, sem1));
            Examination exam3 = examRepo.save(createExamination("Practical Exam", subNursingFoundation, ExamType.PRACTICAL, LocalDate.of(2025, 11, 25), 60, 50, sem1));
            Examination exam4 = examRepo.save(createExamination("Mid-Semester Theory", subPhysiology, ExamType.THEORY, LocalDate.of(2025, 9, 18), 120, 100, sem1));
            Examination exam5 = examRepo.save(createExamination("Viva Voce", subNursingFoundation, ExamType.VIVA, LocalDate.of(2025, 11, 28), 30, 25, sem1));
            log.info("✓ Created 5 examinations");

            // ═══════════════════════════════════════════════════════════════
            // 18. EXAM RESULTS
            // ═══════════════════════════════════════════════════════════════
            examResultRepo.save(createExamResult(exam1, s1, new BigDecimal("78.5"), "B+", ExamResultStatus.PUBLISHED));
            examResultRepo.save(createExamResult(exam1, s2, new BigDecimal("82.0"), "A", ExamResultStatus.PUBLISHED));
            examResultRepo.save(createExamResult(exam1, s3, new BigDecimal("71.5"), "B", ExamResultStatus.PUBLISHED));
            examResultRepo.save(createExamResult(exam4, s1, new BigDecimal("75.0"), "B+", ExamResultStatus.PUBLISHED));
            examResultRepo.save(createExamResult(exam4, s2, new BigDecimal("88.0"), "A+", ExamResultStatus.PUBLISHED));
            examResultRepo.save(createExamResult(exam3, s1, new BigDecimal("42.0"), "B", ExamResultStatus.PUBLISHED));
            examResultRepo.save(createExamResult(exam3, s2, new BigDecimal("45.0"), "A", ExamResultStatus.PUBLISHED));
            log.info("✓ Created 7 exam results");

            // ═══════════════════════════════════════════════════════════════
            // 19. ATTENDANCE RECORDS
            // ═══════════════════════════════════════════════════════════════
            attendanceRepo.save(createAttendance(s1, subAnatomy, LocalDate.of(2025, 7, 1), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s1, subAnatomy, LocalDate.of(2025, 7, 2), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s1, subAnatomy, LocalDate.of(2025, 7, 3), AttendanceStatus.ABSENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s2, subAnatomy, LocalDate.of(2025, 7, 1), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s2, subAnatomy, LocalDate.of(2025, 7, 2), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s2, subAnatomy, LocalDate.of(2025, 7, 3), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s3, subAnatomy, LocalDate.of(2025, 7, 1), AttendanceStatus.ABSENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s3, subAnatomy, LocalDate.of(2025, 7, 2), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s1, subPhysiology, LocalDate.of(2025, 7, 1), AttendanceStatus.PRESENT, AttendanceType.THEORY));
            attendanceRepo.save(createAttendance(s2, subPhysiology, LocalDate.of(2025, 7, 1), AttendanceStatus.LATE, AttendanceType.THEORY));
            log.info("✓ Created 10 attendance records");

            // ═══════════════════════════════════════════════════════════════
            // 20. LAB SLOTS
            // ═══════════════════════════════════════════════════════════════
            LabSlot slot1 = labSlotRepo.save(createLabSlot("Morning Slot 1", LocalTime.of(9, 0), LocalTime.of(12, 0), 1));
            LabSlot slot2 = labSlotRepo.save(createLabSlot("Morning Slot 2", LocalTime.of(10, 0), LocalTime.of(13, 0), 2));
            LabSlot slot3 = labSlotRepo.save(createLabSlot("Afternoon Slot", LocalTime.of(14, 0), LocalTime.of(17, 0), 3));
            log.info("✓ Created 3 lab slots");

            // ═══════════════════════════════════════════════════════════════
            // 21. LAB SCHEDULES
            // ═══════════════════════════════════════════════════════════════
            labScheduleRepo.save(createLabSchedule(lab1, subNursingFoundation, f6, slot1, "Batch A", DayOfWeek.MONDAY, sem1));
            labScheduleRepo.save(createLabSchedule(lab2, subAnatomy, f3, slot2, "Batch A", DayOfWeek.TUESDAY, sem1));
            labScheduleRepo.save(createLabSchedule(lab3, subMSN1, f1, slot3, "Batch B", DayOfWeek.WEDNESDAY, sem1));
            labScheduleRepo.save(createLabSchedule(lab9, subMicrobiology, f8, slot1, "Batch A", DayOfWeek.THURSDAY, sem1));
            labScheduleRepo.save(createLabSchedule(lab4, subOBGNursing, f4, slot2, "Batch B", DayOfWeek.FRIDAY, sem1));
            log.info("✓ Created 5 lab schedules");

            // ═══════════════════════════════════════════════════════════════
            // 22. SYLLABI
            // ═══════════════════════════════════════════════════════════════
            syllabusRepo.save(createSyllabus(subAnatomy, 1, 45, 15, 0, "Study of human body structure, systems, organs, and tissues.", true));
            syllabusRepo.save(createSyllabus(subPhysiology, 1, 45, 15, 0, "Study of body functions including homeostasis and system-wise organ functions.", true));
            syllabusRepo.save(createSyllabus(subNursingFoundation, 1, 45, 60, 15, "Fundamental nursing procedures, patient care, vital signs, and nursing ethics.", true));
            syllabusRepo.save(createSyllabus(subMSN1, 1, 45, 60, 15, "Care of adult patients with medical and surgical conditions.", true));
            log.info("✓ Created 4 syllabi");

            // ═══════════════════════════════════════════════════════════════
            // 23. EXPERIMENTS
            // ═══════════════════════════════════════════════════════════════
            experimentRepo.save(createExperiment(subNursingFoundation, 1, "Vital Signs Measurement", "Measurement of Temperature, Pulse, Respiration, and Blood Pressure", 45));
            experimentRepo.save(createExperiment(subNursingFoundation, 2, "Bed Making Techniques", "Occupied and unoccupied bed making procedures", 30));
            experimentRepo.save(createExperiment(subNursingFoundation, 3, "Patient Positioning", "Various patient positions — Fowler's, Trendelenburg, Lateral, Prone", 30));
            experimentRepo.save(createExperiment(subMSN1, 1, "IV Cannulation Practice", "Intravenous cannulation on simulation arm", 45));
            experimentRepo.save(createExperiment(subMSN1, 2, "Wound Dressing", "Aseptic wound dressing technique", 30));
            experimentRepo.save(createExperiment(subAnatomy, 1, "Skeletal System Study", "Study and identification of bones using skeleton model", 60));
            experimentRepo.save(createExperiment(subBiochemistry, 1, "Microscopy - Cell Structure", "Study of cell structures using microscope", 45));
            log.info("✓ Created 7 experiments");

            // ═══════════════════════════════════════════════════════════════
            // 24. SYSTEM CONFIGURATIONS
            // ═══════════════════════════════════════════════════════════════
            sysConfigRepo.save(new SystemConfiguration(null, "college.name", "SKS College of Nursing", "College name displayed across the application", "GENERAL"));
            sysConfigRepo.save(new SystemConfiguration(null, "college.address", "Salem, Tamil Nadu 636001", "College address", "GENERAL"));
            sysConfigRepo.save(new SystemConfiguration(null, "college.email", "info@sksnursing.edu.in", "Official contact email", "GENERAL"));
            sysConfigRepo.save(new SystemConfiguration(null, "college.phone", "0427-2411234", "Official contact phone", "GENERAL"));
            sysConfigRepo.save(new SystemConfiguration(null, "fee.late_penalty_percentage", "2", "Late fee penalty percentage per month", "FINANCE"));
            sysConfigRepo.save(new SystemConfiguration(null, "attendance.minimum_percentage", "75", "Minimum attendance percentage required", "ACADEMIC"));
            log.info("✓ Created 6 system configurations");

            log.info("════════════════════════════════════════════════════════");
            log.info("🎉 Demo data seeding completed successfully!");
            log.info("════════════════════════════════════════════════════════");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    private Program createProgram(String name, String code, int duration) {
        Program p = new Program();
        p.setName(name);
        p.setCode(code);
        p.setDurationYears(duration);
        return p;
    }

    private Course createCourse(String name, String code, String specialization, Program program) {
        Course c = new Course();
        c.setName(name);
        c.setCode(code);
        c.setSpecialization(specialization);
        c.setProgram(program);
        return c;
    }

    private Subject createSubject(String name, String code, int credits, int theory, int lab, Course course, Department dept, int semester) {
        Subject s = new Subject();
        s.setName(name);
        s.setCode(code);
        s.setCredits(credits);
        s.setTheoryCredits(theory);
        s.setLabCredits(lab);
        s.setCourse(course);
        s.setDepartment(dept);
        s.setSemester(semester);
        return s;
    }

    private Semester createSemester(String name, int number, AcademicYear ay, LocalDate start, LocalDate end) {
        Semester s = new Semester();
        s.setName(name);
        s.setSemesterNumber(number);
        s.setAcademicYear(ay);
        s.setStartDate(start);
        s.setEndDate(end);
        return s;
    }

    private Faculty createFaculty(String firstName, String lastName, String email, String phone, String empCode, Department dept, Designation designation, String specialization) {
        Faculty f = new Faculty();
        f.setFirstName(firstName);
        f.setLastName(lastName);
        f.setEmail(email);
        f.setPhone(phone);
        f.setEmployeeCode(empCode);
        f.setDepartment(dept);
        f.setDesignation(designation);
        f.setSpecialization(specialization);
        f.setJoiningDate(LocalDate.of(2020, 6, 1));
        f.setStatus(FacultyStatus.ACTIVE);
        return f;
    }

    private Student createStudent(String firstName, String lastName, String email, String phone, String rollNo, Program program, int semester, StudentStatus status, LocalDate admissionDate) {
        Student s = new Student();
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setEmail(email);
        s.setPhone(phone);
        s.setRollNumber(rollNo);
        s.setProgram(program);
        s.setSemester(semester);
        s.setStatus(status);
        s.setAdmissionDate(admissionDate);
        s.setDateOfBirth(LocalDate.of(2000, 1, 1));
        s.setGender(Gender.FEMALE);
        return s;
    }

    private Lab createLab(String name, LabType labType, String building, String roomNumber, int capacity, Department dept) {
        Lab l = new Lab();
        l.setName(name);
        l.setLabType(labType);
        l.setBuilding(building);
        l.setRoomNumber(roomNumber);
        l.setCapacity(capacity);
        l.setDepartment(dept);
        l.setStatus(LabStatus.AVAILABLE);
        return l;
    }

    private Equipment createEquipment(String name, String assetCode, EquipmentCategory category, Lab lab, EquipmentStatus status, String manufacturer, String model) {
        Equipment e = new Equipment();
        e.setName(name);
        e.setAssetCode(assetCode);
        e.setCategory(category);
        e.setLab(lab);
        e.setStatus(status);
        e.setManufacturer(manufacturer);
        e.setModel(model);
        e.setPurchaseDate(LocalDate.of(2023, 1, 15));
        return e;
    }

    private InventoryItem createInventory(String name, String code, Lab lab, int qty, int minQty, String unit) {
        InventoryItem i = new InventoryItem();
        i.setName(name);
        i.setItemCode(code);
        i.setLab(lab);
        i.setQuantity(qty);
        i.setMinimumQuantity(minQty);
        i.setUnit(unit);
        return i;
    }

    private MaintenanceRequest createMaintenance(String title, String desc, Equipment eq, MaintenanceType type, MaintenanceStatus status, MaintenancePriority priority) {
        MaintenanceRequest m = new MaintenanceRequest();
        m.setTitle(title);
        m.setDescription(desc);
        m.setEquipment(eq);
        m.setMaintenanceType(type);
        m.setStatus(status);
        m.setPriority(priority);
        m.setRequestDate(LocalDate.now().minusDays(5));
        if (status == MaintenanceStatus.COMPLETED) {
            m.setCompletionDate(LocalDate.now().minusDays(1));
        }
        return m;
    }

    private Agent createAgent(String name, String email, String phone, String area, String locality, boolean isActive) {
        Agent a = new Agent();
        a.setName(name);
        a.setEmail(email);
        a.setPhone(phone);
        a.setArea(area);
        a.setLocality(locality);
        a.setIsActive(isActive);
        return a;
    }

    private ReferralType createReferralType(String name, String code, BigDecimal commissionAmount, boolean hasCommission, String description) {
        ReferralType rt = new ReferralType();
        rt.setName(name);
        rt.setCode(code);
        rt.setCommissionAmount(commissionAmount);
        rt.setHasCommission(hasCommission);
        rt.setDescription(description);
        rt.setIsActive(true);
        return rt;
    }

    private Enquiry createEnquiry(String name, String email, String phone, Program program, ReferralType referralType, EnquiryStatus status, Agent agent, LocalDate date) {
        Enquiry e = new Enquiry();
        e.setName(name);
        e.setEmail(email);
        e.setPhone(phone);
        e.setProgram(program);
        e.setReferralType(referralType);
        e.setStatus(status);
        e.setAgent(agent);
        e.setEnquiryDate(date);
        return e;
    }

    private FeeStructure createFeeStructure(Program program, AcademicYear ay, FeeType feeType, BigDecimal amount) {
        FeeStructure f = new FeeStructure();
        f.setProgram(program);
        f.setAcademicYear(ay);
        f.setFeeType(feeType);
        f.setAmount(amount);
        f.setIsMandatory(true);
        f.setIsActive(true);
        return f;
    }

    private FeePayment createFeePayment(Student student, FeeStructure feeStructure, String receiptNumber, BigDecimal amount, PaymentMode mode, PaymentStatus status, LocalDate date, String txnRef) {
        FeePayment p = new FeePayment();
        p.setStudent(student);
        p.setFeeStructure(feeStructure);
        p.setReceiptNumber(receiptNumber);
        p.setAmountPaid(amount);
        p.setPaymentMode(mode);
        p.setStatus(status);
        p.setPaymentDate(date);
        p.setTransactionReference(txnRef);
        return p;
    }

    private Examination createExamination(String name, Subject subject, ExamType type, LocalDate date, int duration, int maxMarks, Semester semester) {
        Examination e = new Examination();
        e.setName(name);
        e.setSubject(subject);
        e.setExamType(type);
        e.setDate(date);
        e.setDuration(duration);
        e.setMaxMarks(maxMarks);
        e.setSemester(semester);
        return e;
    }

    private ExamResult createExamResult(Examination exam, Student student, BigDecimal marks, String grade, ExamResultStatus status) {
        ExamResult r = new ExamResult();
        r.setExamination(exam);
        r.setStudent(student);
        r.setMarksObtained(marks);
        r.setGrade(grade);
        r.setStatus(status);
        return r;
    }

    private Attendance createAttendance(Student student, Subject subject, LocalDate date, AttendanceStatus status, AttendanceType type) {
        Attendance a = new Attendance();
        a.setStudent(student);
        a.setSubject(subject);
        a.setDate(date);
        a.setStatus(status);
        a.setType(type);
        return a;
    }

    private LabSlot createLabSlot(String name, LocalTime startTime, LocalTime endTime, int slotOrder) {
        LabSlot ls = new LabSlot();
        ls.setName(name);
        ls.setStartTime(startTime);
        ls.setEndTime(endTime);
        ls.setSlotOrder(slotOrder);
        ls.setIsActive(true);
        return ls;
    }

    private LabSchedule createLabSchedule(Lab lab, Subject subject, Faculty faculty, LabSlot labSlot, String batchName, DayOfWeek dayOfWeek, Semester semester) {
        LabSchedule ls = new LabSchedule();
        ls.setLab(lab);
        ls.setSubject(subject);
        ls.setFaculty(faculty);
        ls.setLabSlot(labSlot);
        ls.setBatchName(batchName);
        ls.setDayOfWeek(dayOfWeek);
        ls.setSemester(semester);
        ls.setIsActive(true);
        return ls;
    }

    private Syllabus createSyllabus(Subject subject, int version, int theoryHours, int labHours, int tutorialHours, String content, boolean isActive) {
        Syllabus s = new Syllabus();
        s.setSubject(subject);
        s.setVersion(version);
        s.setTheoryHours(theoryHours);
        s.setLabHours(labHours);
        s.setTutorialHours(tutorialHours);
        s.setContent(content);
        s.setIsActive(isActive);
        return s;
    }

    private Experiment createExperiment(Subject subject, int experimentNumber, String name, String description, int durationMinutes) {
        Experiment e = new Experiment();
        e.setSubject(subject);
        e.setExperimentNumber(experimentNumber);
        e.setName(name);
        e.setDescription(description);
        e.setEstimatedDurationMinutes(durationMinutes);
        e.setIsActive(true);
        return e;
    }
}

