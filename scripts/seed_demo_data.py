#!/usr/bin/env python3
"""Populate the CMS backend with realistic data for SKS College of Nursing, Salem.

This script authenticates against Keycloak using the imported local admin user and
creates data through the secured `/api/v1` endpoints in a dependency-safe order.

All data reflects an actual Indian Nursing College scenario — departments map to
nursing specializations, programs cover B.Sc. Nursing / M.Sc. Nursing / GNM,
subjects follow the Indian Nursing Council (INC) syllabus, and labs represent
real nursing simulation and skills labs.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from decimal import Decimal
from typing import Any

API_URL = os.environ.get('CMS_API_URL', 'http://localhost:8080/api/v1')
KEYCLOAK_URL = os.environ.get('CMS_KEYCLOAK_URL', 'http://localhost:8280')
REALM = os.environ.get('CMS_REALM', 'cms')
CLIENT_ID = os.environ.get('CMS_CLIENT_ID', 'cms-frontend')
USERNAME = os.environ.get('CMS_USERNAME', 'admin')
PASSWORD = os.environ.get('CMS_PASSWORD', 'admin123')

# ---------------------------------------------------------------------------
# SKS College of Nursing, Salem — Reference Data
# ---------------------------------------------------------------------------

DEPARTMENTS = [
    {'name': 'Medical-Surgical Nursing', 'code': 'MSN', 'description': 'Department of Medical-Surgical Nursing — covers adult health, perioperative care, critical care, and oncology nursing.', 'hodName': 'Dr. S. Tamilarasi'},
    {'name': 'Community Health Nursing', 'code': 'CHN', 'description': 'Department of Community Health Nursing — focuses on public health, epidemiology, family health, and primary healthcare delivery.', 'hodName': 'Dr. K. Vasanthi'},
    {'name': 'Child Health (Paediatric) Nursing', 'code': 'CHD', 'description': 'Department of Child Health Nursing — covers neonatal care, growth & development, paediatric diseases, and child nutrition.', 'hodName': 'Dr. R. Meenakshi'},
    {'name': 'Obstetrics & Gynaecological Nursing', 'code': 'OBG', 'description': 'Department of Obstetrics & Gynaecological Nursing — antenatal, intranatal, postnatal care, reproductive health, and midwifery.', 'hodName': 'Dr. P. Selvarani'},
    {'name': 'Mental Health (Psychiatric) Nursing', 'code': 'MHN', 'description': 'Department of Mental Health Nursing — psychiatric disorders, therapeutic communication, psychopharmacology, and rehabilitation.', 'hodName': 'Dr. M. Kavitha'},
    {'name': 'Nursing Foundation', 'code': 'NFD', 'description': 'Department of Nursing Foundation — fundamental nursing skills, nursing ethics, nursing process, and basic patient care.', 'hodName': 'Mrs. L. Jayalakshmi'},
    {'name': 'Nursing Education & Administration', 'code': 'NEA', 'description': 'Department of Nursing Education & Administration — teaching methodologies, curriculum development, hospital management.', 'hodName': 'Dr. A. Padmavathi'},
]

PROGRAMS = [
    # (name, code, programLevel, durationYears, departmentIndices)
    ('B.Sc. Nursing', 'BSCN', 'UNDERGRADUATE', 4, [5]),
    ('M.Sc. Nursing — Medical-Surgical', 'MSCMSN', 'POSTGRADUATE', 2, [0]),
    ('M.Sc. Nursing — Community Health', 'MSCCHN', 'POSTGRADUATE', 2, [1]),
    ('M.Sc. Nursing — Child Health', 'MSCCHD', 'POSTGRADUATE', 2, [2]),
    ('M.Sc. Nursing — OBG', 'MSCOBG', 'POSTGRADUATE', 2, [3]),
    ('General Nursing and Midwifery (GNM)', 'GNM', 'DIPLOMA', 3, [5]),
]

COURSES = [
    # (name, code, specialization, programIndex)
    ('B.Sc. Nursing', 'BSCN-C', None, 0),
    ('M.Sc. Nursing — Medical-Surgical', 'MSCMSN-C', 'Medical-Surgical', 1),
    ('M.Sc. Nursing — Community Health', 'MSCCHN-C', 'Community Health', 2),
    ('M.Sc. Nursing — Child Health', 'MSCCHD-C', 'Child Health', 3),
    ('M.Sc. Nursing — OBG', 'MSCOBG-C', 'Obs Gyn', 4),
    ('General Nursing and Midwifery (GNM)', 'GNM-C', None, 5),
]

SUBJECTS = [
    # (name, code, credits, theoryCredits, labCredits, courseIndex, semester)
    # B.Sc. Nursing — Year 1
    ('Anatomy', 'BSN101', 4, 3, 1, 0, 1),
    ('Physiology', 'BSN102', 4, 3, 1, 0, 1),
    ('Nursing Foundation', 'BSN103', 6, 3, 3, 0, 1),
    ('Biochemistry', 'BSN104', 3, 2, 1, 0, 2),
    ('Nutrition & Dietetics', 'BSN105', 3, 2, 1, 0, 2),
    ('Microbiology', 'BSN106', 4, 3, 1, 0, 2),
    # B.Sc. Nursing — Year 2
    ('Medical-Surgical Nursing I', 'BSN201', 6, 3, 3, 0, 3),
    ('Pharmacology', 'BSN202', 4, 3, 1, 0, 3),
    ('Pathology & Genetics', 'BSN203', 3, 2, 1, 0, 3),
    ('Community Health Nursing I', 'BSN204', 5, 2, 3, 0, 4),
    # B.Sc. Nursing — Year 3
    ('Medical-Surgical Nursing II', 'BSN301', 6, 3, 3, 0, 5),
    ('Child Health Nursing', 'BSN302', 5, 2, 3, 0, 5),
    ('Mental Health Nursing', 'BSN303', 5, 2, 3, 0, 6),
    ('OBG Nursing', 'BSN304', 5, 2, 3, 0, 6),
    # B.Sc. Nursing — Year 4
    ('Community Health Nursing II', 'BSN401', 5, 2, 3, 0, 7),
    ('Nursing Research & Statistics', 'BSN402', 3, 3, 0, 0, 7),
    ('Nursing Education', 'BSN403', 3, 2, 1, 0, 8),
    ('Nursing Administration', 'BSN404', 3, 2, 1, 0, 8),
    # M.Sc. Nursing — Medical-Surgical
    ('Advanced Medical-Surgical Nursing', 'MSN501', 6, 3, 3, 1, 1),
    ('Nursing Research Methodology', 'MSN502', 4, 4, 0, 1, 1),
    ('Clinical Speciality — Critical Care', 'MSN503', 6, 2, 4, 1, 2),
    # GNM — Year 1
    ('Fundamentals of Nursing', 'GNM101', 5, 2, 3, 5, 1),
    ('Anatomy & Physiology', 'GNM102', 4, 3, 1, 5, 1),
    ('First Aid & Health Education', 'GNM103', 3, 2, 1, 5, 2),
]

FACULTY_MEMBERS = [
    # (employeeCode, firstName, lastName, email, phone, deptIdx, designation, specialization, labExpertise)
    ('NUR001', 'Tamilarasi', 'S', 'tamilarasi.s@sksnursing.edu.in', '9443012345', 0, 'PROFESSOR', 'Critical Care Nursing', 'Advanced Cardiac Life Support Training'),
    ('NUR002', 'Vasanthi', 'K', 'vasanthi.k@sksnursing.edu.in', '9443023456', 1, 'PROFESSOR', 'Public Health & Epidemiology', 'Community Health Field Training'),
    ('NUR003', 'Meenakshi', 'R', 'meenakshi.r@sksnursing.edu.in', '9443034567', 2, 'ASSOCIATE_PROFESSOR', 'Neonatal Intensive Care', 'Paediatric Simulation Lab'),
    ('NUR004', 'Selvarani', 'P', 'selvarani.p@sksnursing.edu.in', '9443045678', 3, 'PROFESSOR', 'Midwifery & Reproductive Health', 'Obstetric Simulation Training'),
    ('NUR005', 'Kavitha', 'M', 'kavitha.m@sksnursing.edu.in', '9443056789', 4, 'ASSOCIATE_PROFESSOR', 'Psychiatric Rehabilitation', 'Therapeutic Communication Lab'),
    ('NUR006', 'Jayalakshmi', 'L', 'jayalakshmi.l@sksnursing.edu.in', '9443067890', 5, 'SENIOR_LECTURER', 'Fundamental Nursing Procedures', 'Nursing Foundation Skills Lab'),
    ('NUR007', 'Padmavathi', 'A', 'padmavathi.a@sksnursing.edu.in', '9443078901', 6, 'PROFESSOR', 'Nursing Education & Management', 'Teaching Methodology Workshop'),
    ('NUR008', 'Revathi', 'G', 'revathi.g@sksnursing.edu.in', '9443089012', 0, 'ASSISTANT_PROFESSOR', 'Perioperative Nursing', 'Surgical Skills Lab'),
    ('NUR009', 'Sangeetha', 'D', 'sangeetha.d@sksnursing.edu.in', '9443090123', 1, 'LECTURER', 'School Health & Nutrition', 'Community Health Centre'),
    ('NUR010', 'Priya', 'N', 'priya.n@sksnursing.edu.in', '9443001234', 3, 'ASSISTANT_PROFESSOR', 'Antenatal & Postnatal Care', 'Labour Room Simulation'),
    ('NUR011', 'Deepa', 'V', 'deepa.v@sksnursing.edu.in', '9443012346', 2, 'LECTURER', 'Child Growth & Development', 'Paediatric Ward Practice'),
    ('NUR012', 'Lakshmi', 'B', 'lakshmi.b@sksnursing.edu.in', '9443023457', 4, 'LECTURER', 'Substance Abuse Counselling', 'De-addiction & Rehabilitation'),
]

STUDENTS = [
    # (rollNumber, firstName, lastName, email, phone, programIdx, semester, gender, dob, fatherName, motherName, parentMobile, city, district)
    ('24BSN001', 'Anitha', 'Kumari', 'anitha.k@sksnursing.edu.in', '8870012345', 0, 1, 'FEMALE', '2005-03-14', 'Kumar Shanmugam', 'Revathi Kumar', '9443112345', 'Salem', 'Salem'),
    ('24BSN002', 'Priya', 'Devi', 'priya.d@sksnursing.edu.in', '8870023456', 0, 1, 'FEMALE', '2005-07-22', 'Devendran P', 'Lakshmi Devi', '9443123456', 'Salem', 'Salem'),
    ('23BSN001', 'Kavitha', 'Rajendran', 'kavitha.r@sksnursing.edu.in', '8870034567', 0, 3, 'FEMALE', '2004-11-03', 'Rajendran Murugan', 'Selvi Rajendran', '9443134567', 'Namakkal', 'Namakkal'),
    ('23BSN002', 'Divya', 'Lakshmi', 'divya.l@sksnursing.edu.in', '8870045678', 0, 3, 'FEMALE', '2004-01-17', 'Lakshmi Narayanan S', 'Meenakshi L', '9443145678', 'Erode', 'Erode'),
    ('22BSN001', 'Sangeetha', 'Murugan', 'sangeetha.m@sksnursing.edu.in', '8870056789', 0, 5, 'FEMALE', '2003-05-28', 'Murugan Govindan', 'Padma Murugan', '9443156789', 'Dharmapuri', 'Dharmapuri'),
    ('22BSN002', 'Meena', 'Sundaram', 'meena.s@sksnursing.edu.in', '8870067890', 0, 5, 'FEMALE', '2003-09-12', 'Sundaram Pillai', 'Kamala Sundaram', '9443167890', 'Attur', 'Salem'),
    ('21BSN001', 'Ramya', 'Ganesh', 'ramya.g@sksnursing.edu.in', '8870078901', 0, 7, 'FEMALE', '2002-12-05', 'Ganesh Rajan', 'Saroja Ganesh', '9443178901', 'Mettur', 'Salem'),
    ('21BSN002', 'Swetha', 'Balan', 'swetha.b@sksnursing.edu.in', '8870089012', 0, 7, 'FEMALE', '2002-06-20', 'Balan Naidu', 'Vijaya Balan', '9443189012', 'Yercaud', 'Salem'),
    ('25MSN001', 'Deepa', 'Selvam', 'deepa.s@sksnursing.edu.in', '8870090123', 1, 1, 'FEMALE', '2000-04-09', 'Selvam Arumugam', 'Janaki Selvam', '9443190123', 'Salem', 'Salem'),
    ('25MSN002', 'Saranya', 'Ravi', 'saranya.r@sksnursing.edu.in', '8870001234', 2, 1, 'FEMALE', '2000-08-25', 'Ravi Chandran', 'Parvathi Ravi', '9443101234', 'Namakkal', 'Namakkal'),
    ('24GNM001', 'Gowri', 'Krishnan', 'gowri.k@sksnursing.edu.in', '8870011345', 5, 1, 'FEMALE', '2005-02-18', 'Krishnan Iyer', 'Saraswathi K', '9443201234', 'Salem', 'Salem'),
    ('24GNM002', 'Nithya', 'Suresh', 'nithya.s@sksnursing.edu.in', '8870021456', 5, 1, 'FEMALE', '2005-10-30', 'Suresh Pandian', 'Thenmozhi S', '9443212345', 'Omalur', 'Salem'),
]

LABS = [
    # (name, labType, deptIdx, building, roomNumber, capacity, status)
    ('Nursing Foundation Lab', 'OTHER', 5, 'Main Block', 'MB-G01', 40, 'ACTIVE'),
    ('Anatomy & Physiology Lab', 'BIOLOGY', 5, 'Main Block', 'MB-G02', 30, 'ACTIVE'),
    ('Community Health Nursing Lab', 'OTHER', 1, 'Main Block', 'MB-101', 30, 'ACTIVE'),
    ('Nutrition & Dietetics Lab', 'CHEMISTRY', 5, 'Main Block', 'MB-102', 25, 'ACTIVE'),
    ('Medical-Surgical Nursing Lab', 'OTHER', 0, 'Skills Block', 'SB-101', 35, 'ACTIVE'),
    ('Paediatric Nursing Lab', 'OTHER', 2, 'Skills Block', 'SB-102', 25, 'ACTIVE'),
    ('OBG Nursing Simulation Lab', 'OTHER', 3, 'Skills Block', 'SB-201', 25, 'ACTIVE'),
    ('Mental Health Nursing Lab', 'OTHER', 4, 'Skills Block', 'SB-202', 20, 'ACTIVE'),
    ('Computer Lab', 'COMPUTER', 6, 'Admin Block', 'AB-301', 40, 'ACTIVE'),
    ('Microbiology Lab', 'BIOLOGY', 5, 'Main Block', 'MB-103', 30, 'ACTIVE'),
]

EQUIPMENT_LIST = [
    # (name, assetCode, serialNumber, category, labIdx, manufacturer, model, status, purchasePrice, specifications)
    # Valid categories: COMPUTER, PERIPHERAL, NETWORKING, ELECTRONIC, MECHANICAL, FURNITURE, CONSUMABLE, SOFTWARE
    ('Adult Patient Simulator (Full Body Mannequin)', 'AST-NF-001', 'SN-SIM-001', 'ELECTRONIC', 0, 'Laerdal Medical', 'Nursing Anne', 'AVAILABLE', '350000.00', 'Life-size female mannequin with IV arm, wound care modules, catheterisation capability'),
    ('Hospital Bed — Fowler Type', 'AST-NF-002', 'SN-BED-001', 'FURNITURE', 0, 'Shree Hospital Equipment', 'SHE-3F200', 'AVAILABLE', '45000.00', 'Manual 3-crank Fowler bed with side rails, mattress, and IV pole'),
    ('BP Apparatus (Mercury Sphygmomanometer)', 'AST-MSN-001', 'SN-BP-001', 'ELECTRONIC', 4, 'Diamond', 'DMAM-01', 'AVAILABLE', '3500.00', 'Mercury column type with adult cuff, stethoscope compatible'),
    ('Multi-Parameter Patient Monitor', 'AST-MSN-002', 'SN-MON-001', 'ELECTRONIC', 4, 'BPL Medical Technologies', 'Ultima Prima', 'AVAILABLE', '180000.00', 'ECG, SpO2, NIBP, Temperature, Respiration — 12.1 inch colour display'),
    ('Infant Resuscitation Mannequin', 'AST-PED-001', 'SN-INF-001', 'ELECTRONIC', 5, 'Laerdal Medical', 'Resusci Baby QCPR', 'AVAILABLE', '125000.00', 'Realistic infant airway, chest compression feedback, umbilical access'),
    ('Obstetric Birthing Simulator', 'AST-OBG-001', 'SN-OBS-001', 'ELECTRONIC', 6, 'Gaumard Scientific', 'NOELLE S550', 'AVAILABLE', '450000.00', 'Full birthing simulation with maternal and fetal monitoring, multiple delivery scenarios'),
    ('Anatomical Skeleton Model', 'AST-ANA-001', 'SN-SKL-001', 'FURNITURE', 1, 'Heine Scientific', 'HS-170cm', 'AVAILABLE', '28000.00', '170cm life-size human skeleton on wheeled stand with numbered bones'),
    ('Autoclave (Vertical)', 'AST-MIC-001', 'SN-AUT-001', 'MECHANICAL', 9, 'Equitron Medica', 'EQU-7454A', 'AVAILABLE', '85000.00', '40 litre vertical autoclave, 121C at 15 psi, digital timer'),
    ('Desktop Computer (Student Workstation)', 'AST-COM-001', 'SN-PC-001', 'COMPUTER', 8, 'Dell Technologies', 'OptiPlex 3000', 'AVAILABLE', '52000.00', 'Intel i5-12500, 8GB RAM, 256GB SSD, Windows 11, 21.5 inch monitor'),
    ('Stethoscope Set (Training)', 'AST-NF-003', 'SN-STH-001', 'PERIPHERAL', 0, 'Littmann (3M)', 'Classic III', 'AVAILABLE', '7500.00', 'Dual-head adult/paediatric stethoscope for auscultation practice'),
]

INVENTORY_ITEMS = [
    # (name, itemCode, labIdx, quantity, minimumQuantity, unit, description)
    ('Disposable Gloves (Medium)', 'INV-NUR-001', 0, 500, 100, 'pairs', 'Non-sterile latex examination gloves, powder-free, for nursing procedures'),
    ('Syringes 5ml (Disposable)', 'INV-NUR-002', 4, 300, 50, 'pcs', 'Luer-lock disposable syringes for injection practice on mannequins'),
    ('Cotton Rolls (500g)', 'INV-NUR-003', 0, 50, 10, 'rolls', 'Absorbent cotton for wound dressing demonstrations'),
    ('IV Cannula 20G', 'INV-NUR-004', 4, 200, 40, 'pcs', 'Sterile IV cannulae for IV insertion training on simulation arms'),
    ('Foley Catheter 16Fr', 'INV-NUR-005', 0, 100, 20, 'pcs', 'Silicone Foley catheters for catheterisation training'),
    ('Bandage Rolls (6 inch)', 'INV-NUR-006', 4, 150, 30, 'rolls', 'Roller bandages for wound dressing and splinting practice'),
    ('Specimen Containers (Sterile)', 'INV-NUR-007', 9, 200, 50, 'pcs', 'Sterile urine and sputum specimen containers for lab collection training'),
    ('Betadine Solution (500ml)', 'INV-NUR-008', 4, 30, 5, 'bottles', 'Povidone-iodine antiseptic solution for wound care demonstrations'),
    ('Pregnancy Detection Kits', 'INV-NUR-009', 6, 50, 10, 'kits', 'HCG urine test kits for OBG lab demonstrations'),
    ('Glass Slides & Cover Slips', 'INV-NUR-010', 9, 500, 100, 'pcs', 'Microscopy slides for microbiology lab — Gram staining, AFB staining'),
]

MAINTENANCE_ENTRIES = [
    # (equipmentIdx, title, description, maintenanceType, priority, status)
    (0, 'Nursing Anne Mannequin — Annual Service', 'Annual inspection and maintenance of all joints, IV arm, and wound modules.', 'PREVENTIVE', 'MEDIUM', 'SCHEDULED'),
    (3, 'Patient Monitor Calibration', 'Scheduled calibration of ECG, SpO2, and NIBP sensors per biomedical standards.', 'PREVENTIVE', 'HIGH', 'IN_PROGRESS'),
    (5, 'Birthing Simulator Repair — Fetal Sensor', 'Fetal heart rate sensor showing intermittent readings during simulation.', 'CORRECTIVE', 'HIGH', 'REQUESTED'),
    (7, 'Autoclave Pressure Valve Replacement', 'Safety pressure release valve showing signs of wear; needs replacement.', 'CORRECTIVE', 'CRITICAL', 'IN_PROGRESS'),
    (4, 'Infant Mannequin Chest Mechanism', 'Chest compression feedback mechanism needs recalibration.', 'CORRECTIVE', 'MEDIUM', 'REQUESTED'),
    (1, 'Hospital Bed Crank Mechanism Lubrication', 'Routine lubrication of all Fowler bed crank mechanisms in Foundation Lab.', 'ROUTINE', 'LOW', 'COMPLETED'),
    (8, 'Computer Lab — Annual Windows Update', 'Deploy latest Windows updates and nursing software patches across 40 workstations.', 'PREVENTIVE', 'LOW', 'SCHEDULED'),
    (6, 'Skeleton Model Repair — Missing Hand Bones', 'Carpal bones on left hand have come loose; reattachment needed.', 'CORRECTIVE', 'LOW', 'COMPLETED'),
    (2, 'BP Apparatus Mercury Column Check', 'Verify mercury column accuracy and replace cuffs showing wear across 15 units.', 'ROUTINE', 'MEDIUM', 'IN_PROGRESS'),
    (9, 'Stethoscope Earpiece Replacement', 'Replace worn earpieces and diaphragms on 20 training stethoscopes.', 'ROUTINE', 'LOW', 'REQUESTED'),
]

EXAMINATION_DATA = [
    # (name, subjectIdx, examType, daysFromToday, duration, maxMarks)
    ('Anatomy — Internal Assessment I', 0, 'THEORY', 15, 90, 50),
    ('Physiology — Internal Assessment I', 1, 'THEORY', 17, 90, 50),
    ('Nursing Foundation — Practical Exam', 2, 'PRACTICAL', 20, 180, 100),
    ('Medical-Surgical Nursing I — Mid Semester', 6, 'THEORY', 25, 120, 75),
    ('Pharmacology — Theory Exam', 7, 'THEORY', 30, 120, 75),
    ('Community Health Nursing I — Viva', 9, 'VIVA', 10, 30, 25),
    ('Child Health Nursing — Practical', 11, 'PRACTICAL', 22, 180, 100),
    ('OBG Nursing — Practical', 13, 'PRACTICAL', 28, 180, 100),
    ('Mental Health Nursing — Theory', 12, 'THEORY', 35, 120, 75),
    ('Nursing Research — End Semester', 15, 'THEORY', 40, 120, 75),
]

AGENTS = [
    # (name, phone, email, area, locality, isActive)
    ('Tamil Nadu Nursing Education Trust', '9443301234', 'tnnet@email.com', 'Chennai', 'Tamil Nadu North', True),
    ('Salem District Health Foundation', '9443312345', 'sdhf@email.com', 'Salem', 'Salem Region', True),
    ('Southern Nursing Academy', '9443323456', 'sna@email.com', 'Madurai', 'Tamil Nadu South', True),
    ('Kongu Region Educational Services', '9443334567', 'kres@email.com', 'Erode', 'Kongu Region', True),
    ('Healthcare Career Consultants', '9443345678', 'hcc@email.com', 'Coimbatore', 'Western TN', True),
]

ENQUIRIES = [
    # (name, email, phone, programIdx, source, status, agentIdx, remarks, feeDiscussedAmount)
    # Valid statuses: ENQUIRED, INTERESTED, NOT_INTERESTED, FEES_FINALIZED, FEES_PAID, PARTIALLY_PAID, DOCUMENTS_SUBMITTED, CONVERTED, CLOSED
    ('Lakshmi Priya R', 'lakshmi.r@gmail.com', '9876501234', 0, 'WALK_IN', 'ENQUIRED', None, 'Interested in B.Sc. Nursing, completed HSC with Biology group', None),
    ('Sathya Devi M', 'sathya.m@gmail.com', '9876512345', 0, 'PHONE', 'ENQUIRED', None, 'Called to enquire about B.Sc. admission for 2025-26 batch', None),
    ('Kavitha S', 'kavitha.s@yahoo.com', '9876523456', 0, 'AGENT_REFERRAL', 'FEES_FINALIZED', 0, 'Referred by TNNET, discussed fee structure and hostel', '185000.00'),
    ('Ranjitha K', 'ranjitha.k@gmail.com', '9876534567', 5, 'WALK_IN', 'INTERESTED', None, 'Wants GNM, has completed 10+2, very keen', '120000.00'),
    ('Nithya V', 'nithya.v@gmail.com', '9876545678', 1, 'ONLINE', 'ENQUIRED', None, 'M.Sc. Medical-Surgical Nursing enquiry, has 5 years experience', None),
    ('Geetha Ram S', 'geetha.rs@gmail.com', '9876556789', 0, 'AGENT_REFERRAL', 'ENQUIRED', 1, 'Referred by Salem Health Foundation, awaiting marks sheet', None),
    ('Harini B', 'harini.b@gmail.com', '9876567890', 0, 'WALK_IN', 'FEES_FINALIZED', None, 'Visited campus with parents, toured labs and hostel', '185000.00'),
    ('Swetha P', 'swetha.p@gmail.com', '9876578901', 2, 'PHONE', 'ENQUIRED', None, 'Enquiring about M.Sc. Community Health Nursing seats for 2025-26', None),
    ('Malar K', 'malar.k@gmail.com', '9876589012', 5, 'AGENT_REFERRAL', 'INTERESTED', 2, 'GNM candidate from Madurai, Southern Nursing Academy referral', '115000.00'),
    ('Jayanthi R', 'jayanthi.r@gmail.com', '9876590123', 0, 'WALK_IN', 'NOT_INTERESTED', None, 'Visited but decided to pursue MBBS instead', None),
]


@dataclass
class BatchIds:
    departments: list[int]
    programs: list[int]
    courses: list[int]
    subjects: list[int]
    academic_years: list[int]
    semesters: list[int]
    faculty: list[int]
    students: list[int]
    labs: list[int]
    fee_structures: list[int]
    fee_payments: list[int]
    equipment: list[int]
    inventory: list[int]
    maintenance: list[int]
    examinations: list[int]
    exam_results: list[int]
    attendance: list[int]
    agents: list[int]
    enquiries: list[int]


class SeedError(RuntimeError):
    pass


def cycle(values: list[str], index: int) -> str:
    return values[index % len(values)]


def to_json_bytes(payload: dict[str, Any]) -> bytes:
    def default(value: Any) -> Any:
        if isinstance(value, date):
            return value.isoformat()
        if isinstance(value, Decimal):
            return str(value)
        raise TypeError(f'Unsupported type: {type(value)!r}')

    return json.dumps(payload, default=default).encode('utf-8')


def get_token() -> str:
    token_url = f'{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token'
    body = urllib.parse.urlencode(
        {
            'client_id': CLIENT_ID,
            'grant_type': 'password',
            'username': USERNAME,
            'password': PASSWORD,
        }
    ).encode('utf-8')
    request = urllib.request.Request(token_url, data=body, method='POST')
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)['access_token']
    except urllib.error.HTTPError as exc:
        details = exc.read().decode('utf-8', errors='replace')
        raise SeedError(f'Failed to obtain token: HTTP {exc.code} {details}') from exc


def api_request(method: str, path: str, token: str, payload: dict[str, Any] | None = None) -> Any:
    url = f'{API_URL}{path}'
    headers = {
        'Authorization': f'Bearer {token}',
        'Accept': 'application/json',
    }
    data = None
    if payload is not None:
        data = to_json_bytes(payload)
        headers['Content-Type'] = 'application/json'

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read().decode('utf-8')
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as exc:
        details = exc.read().decode('utf-8', errors='replace')
        raise SeedError(f'{method} {path} failed with HTTP {exc.code}: {details}') from exc


def create_many(token: str, path: str, payloads: list[dict[str, Any]]) -> list[dict[str, Any]]:
    created: list[dict[str, Any]] = []
    for payload in payloads:
        created.append(api_request('POST', path, token, payload))
    return created


# ---------------------------------------------------------------------------
# Enum value lists used for cycling
# ---------------------------------------------------------------------------
COMMUNITY_CATEGORIES = ['SC', 'ST', 'BC', 'MBC', 'DNC', 'OC', 'OTHERS']
BLOOD_GROUPS = ['A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE']
FEE_TYPES = ['TUITION', 'LAB_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE', 'HOSTEL_FEE', 'MISCELLANEOUS']
PAYMENT_MODES = ['CASH', 'CARD', 'UPI', 'NET_BANKING', 'CHEQUE', 'DEMAND_DRAFT', 'SCHOLARSHIP']
ATTENDANCE_STATUSES = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']
ATTENDANCE_TYPES = ['THEORY', 'LAB']
EXAM_RESULT_STATUSES = ['PENDING', 'PUBLISHED', 'WITHHELD']


def main() -> int:
    token = get_token()
    seed_tag = datetime.now().strftime('DEMO%Y%m%d%H%M%S')
    today = date.today()

    print('🏥 Seeding SKS College of Nursing, Salem — Demo Data')
    print('=' * 60)

    # 1. Departments
    departments = create_many(token, '/departments', DEPARTMENTS)
    print(f'  ✅ Created {len(departments)} departments (nursing specializations)')

    # 2. Academic Years
    academic_year_payloads = [
        {
            'name': f'{2021 + i}-{2022 + i}',
            'startDate': date(2021 + i, 6, 1),
            'endDate': date(2022 + i, 5, 31),
            'isCurrent': (2021 + i == today.year) or (i == 4),
        }
        for i in range(6)
    ]
    academic_years = create_many(token, '/academic-years', academic_year_payloads)
    print(f'  ✅ Created {len(academic_years)} academic years')

    # 3. Programs (programLevel + durationYears + departmentIds)
    program_payloads = [
        {
            'name': p[0],
            'code': p[1],
            'programLevel': p[2],
            'durationYears': p[3],
            'departmentIds': [departments[di]['id'] for di in p[4]],
        }
        for p in PROGRAMS
    ]
    programs = create_many(token, '/programs', program_payloads)
    print(f'  ✅ Created {len(programs)} programs (B.Sc., M.Sc., GNM)')

    # 4. Courses (specialization + programId)
    course_payloads = [
        {
            'name': c[0],
            'code': c[1],
            'specialization': c[2],
            'programId': programs[c[3]]['id'],
        }
        for c in COURSES
    ]
    courses = create_many(token, '/courses', course_payloads)
    print(f'  ✅ Created {len(courses)} courses')

    # 5. Semesters
    semester_names = [
        'Odd Semester 2024-25', 'Even Semester 2024-25',
        'Odd Semester 2025-26', 'Even Semester 2025-26',
        'Odd Semester 2026-27', 'Even Semester 2026-27',
    ]
    semester_payloads = [
        {
            'name': semester_names[i],
            'academicYearId': academic_years[3 + i // 2]['id'],
            'startDate': date(2024 + i // 2, 6 if i % 2 == 0 else 12, 1),
            'endDate': date(2024 + i // 2, 11, 30) if i % 2 == 0 else date(2025 + i // 2, 5, 31),
            'semesterNumber': (i % 8) + 1,
        }
        for i in range(6)
    ]
    semesters = create_many(token, '/semesters', semester_payloads)
    print(f'  ✅ Created {len(semesters)} semesters')

    # 6. Faculty
    faculty_payloads = [
        {
            'employeeCode': fm[0],
            'firstName': fm[1],
            'lastName': fm[2],
            'email': fm[3],
            'phone': fm[4],
            'departmentId': departments[fm[5]]['id'],
            'designation': fm[6],
            'specialization': fm[7],
            'labExpertise': fm[8],
            'joiningDate': today - timedelta(days=365 * (idx + 2)),
            'status': 'ACTIVE',
        }
        for idx, fm in enumerate(FACULTY_MEMBERS)
    ]
    faculty = create_many(token, '/faculty', faculty_payloads)
    print(f'  ✅ Created {len(faculty)} faculty members')

    # 7. Subjects (courseId, credits, semester)
    subject_payloads = [
        {
            'name': s[0],
            'code': s[1],
            'credits': s[2],
            'theoryCredits': s[3],
            'labCredits': s[4],
            'courseId': courses[s[5]]['id'],
            'semester': s[6],
        }
        for s in SUBJECTS
    ]
    subjects = create_many(token, '/subjects', subject_payloads)
    print(f'  ✅ Created {len(subjects)} subjects (INC syllabus)')

    # 8. Students
    # Map program index to course index (1:1 relationship in our data)
    program_to_course = {0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: 5}
    student_payloads = []
    for idx, s in enumerate(STUDENTS):
        student_payloads.append({
            'rollNumber': s[0],
            'firstName': s[1],
            'lastName': s[2],
            'email': s[3],
            'phone': s[4],
            'programId': programs[s[5]]['id'],
            'courseId': courses[program_to_course[s[5]]]['id'],
            'semester': s[6],
            'admissionDate': today - timedelta(days=365 * 2 + idx * 30),
            'labBatch': f'BATCH-{chr(65 + idx % 3)}',
            'status': 'ACTIVE',
            'dateOfBirth': s[8],
            'gender': s[7],
            'aadharNumber': f'{567800000000 + idx:012d}',
            'nationality': 'Indian',
            'religion': 'Hindu',
            'communityCategory': cycle(COMMUNITY_CATEGORIES, idx),
            'caste': '',
            'bloodGroup': cycle(BLOOD_GROUPS, idx),
            'fatherName': s[9],
            'motherName': s[10],
            'parentMobile': s[11],
            'address': {
                'postalAddress': f'{10 + idx}, Gandhi Nagar',
                'street': 'Gandhi Nagar Main Road',
                'city': s[12],
                'district': s[13],
                'state': 'Tamil Nadu',
                'pincode': f'6{36000 + idx * 11:05d}',
            },
        })
    students = create_many(token, '/students', student_payloads)
    print(f'  ✅ Created {len(students)} students')

    # 9. Labs
    lab_payloads = [
        {
            'name': lab[0],
            'labType': lab[1],
            'departmentId': departments[lab[2]]['id'],
            'building': lab[3],
            'roomNumber': lab[4],
            'capacity': lab[5],
            'status': lab[6],
        }
        for lab in LABS
    ]
    labs = create_many(token, '/labs', lab_payloads)
    print(f'  ✅ Created {len(labs)} labs (nursing simulation & skills labs)')

    # 10. Fee Structures
    fee_descriptions = [
        'Tuition fee for B.Sc. Nursing — AY 2025-26',
        'Lab & Clinical Training fee — B.Sc. Nursing',
        'Library fee — B.Sc. Nursing',
        'Examination fee — B.Sc. Nursing',
        'Hostel fee — B.Sc. Nursing',
        'Tuition fee for M.Sc. Nursing — AY 2025-26',
        'Lab & Clinical fee — M.Sc. Nursing',
        'Tuition fee for GNM — AY 2025-26',
        'Lab & Clinical fee — GNM',
        'Miscellaneous fee — All Programs',
    ]
    fee_amounts = [
        '125000.00', '25000.00', '5000.00', '5000.00', '55000.00',
        '175000.00', '35000.00', '85000.00', '18000.00', '3500.00',
    ]
    fee_program_mapping = [0, 0, 0, 0, 0, 1, 1, 5, 5, 0]
    fee_structure_payloads = [
        {
            'programId': programs[fee_program_mapping[i]]['id'],
            'academicYearId': academic_years[min(4, len(academic_years) - 1)]['id'],
            'feeType': cycle(FEE_TYPES, i),
            'amount': Decimal(fee_amounts[i]),
            'description': fee_descriptions[i],
            'isMandatory': i < 6,
            'isActive': True,
        }
        for i in range(10)
    ]
    fee_structures = create_many(token, '/fee-structures', fee_structure_payloads)
    print(f'  ✅ Created {len(fee_structures)} fee structures')

    # 11. Fee Payments
    payment_remarks = [
        'Tuition fee paid in full — B.Sc. Y1',
        'Lab fee — partial payment pending',
        'Library fee — paid via UPI',
        'Exam fee — demand draft',
        'Hostel fee — scholarship deducted',
        'Tuition fee — M.Sc. MSN paid',
        'Full year fees — parents paid',
        'Tuition fee — GNM Y1',
        'Lab fee — GNM paid by UPI',
        'First installment — B.Sc. Y3',
    ]
    fee_payment_payloads = [
        {
            'studentId': students[i % len(students)]['id'],
            'feeStructureId': fee_structures[i]['id'],
            'amountPaid': Decimal(fee_amounts[i]) if i % 3 != 1 else Decimal(fee_amounts[i]) / Decimal('2'),
            'paymentDate': today - timedelta(days=(10 - i) * 5),
            'paymentMode': cycle(PAYMENT_MODES, i),
            'status': 'PAID' if i % 3 != 1 else 'PARTIAL',
            'transactionReference': f'TXN-{today.year}-{i + 1:04d}',
            'remarks': payment_remarks[i],
        }
        for i in range(10)
    ]
    fee_payments = create_many(token, '/fee-payments', fee_payment_payloads)
    print(f'  ✅ Created {len(fee_payments)} fee payments')

    # 12. Equipment
    equipment_payloads = [
        {
            'name': eq[0],
            'assetCode': eq[1],
            'serialNumber': eq[2],
            'category': eq[3],
            'labId': labs[eq[4]]['id'],
            'manufacturer': eq[5],
            'model': eq[6],
            'status': eq[7],
            'purchaseDate': today - timedelta(days=365 * 2 + idx * 30),
            'purchasePrice': Decimal(eq[8]),
            'warrantyExpiry': today + timedelta(days=365 + idx * 60),
            'location': f'{LABS[eq[4]][0]}',
            'specifications': eq[9],
        }
        for idx, eq in enumerate(EQUIPMENT_LIST)
    ]
    equipment = create_many(token, '/equipment', equipment_payloads)
    print(f'  ✅ Created {len(equipment)} equipment items')

    # 13. Inventory
    inventory_payloads = [
        {
            'name': inv[0],
            'itemCode': inv[1],
            'labId': labs[inv[2]]['id'],
            'quantity': inv[3],
            'minimumQuantity': inv[4],
            'unit': inv[5],
            'description': inv[6],
            'lastRestocked': today - timedelta(days=idx * 7 + 5),
        }
        for idx, inv in enumerate(INVENTORY_ITEMS)
    ]
    inventory = create_many(token, '/inventory', inventory_payloads)
    print(f'  ✅ Created {len(inventory)} inventory items (nursing supplies)')

    # 14. Maintenance
    maintenance_payloads = [
        {
            'equipmentId': equipment[m[0]]['id'],
            'title': m[1],
            'description': m[2],
            'maintenanceType': m[3],
            'priority': m[4],
            'status': m[5],
            'requestedById': faculty[idx % len(faculty)]['id'],
            'requestDate': today - timedelta(days=(10 - idx) * 3),
            'scheduledDate': today + timedelta(days=idx * 5) if m[5] in ('SCHEDULED', 'REQUESTED') else None,
            'completionDate': today - timedelta(days=2) if m[5] == 'COMPLETED' else None,
            'assignedToId': faculty[(idx + 1) % len(faculty)]['id'],
            'estimatedCost': Decimal('2500.00') + Decimal(idx * 500),
            'actualCost': Decimal('2200.00') + Decimal(idx * 400) if m[5] == 'COMPLETED' else None,
            'resolutionNotes': 'Completed successfully — parts replaced and tested.' if m[5] == 'COMPLETED' else f'Awaiting action — {m[1].lower()}',
        }
        for idx, m in enumerate(MAINTENANCE_ENTRIES)
    ]
    maintenance = create_many(token, '/maintenance', maintenance_payloads)
    print(f'  ✅ Created {len(maintenance)} maintenance requests')

    # 15. Examinations (uses subjectId)
    examination_payloads = [
        {
            'name': ex[0],
            'subjectId': subjects[ex[1]]['id'],
            'examType': ex[2],
            'date': today + timedelta(days=ex[3]),
            'duration': ex[4],
            'maxMarks': ex[5],
            'semesterId': semesters[idx % len(semesters)]['id'],
        }
        for idx, ex in enumerate(EXAMINATION_DATA)
    ]
    examinations = create_many(token, '/examinations', examination_payloads)
    print(f'  ✅ Created {len(examinations)} examinations')

    # 16. Exam Results
    grade_map = {range(90, 101): 'O', range(80, 90): 'A+', range(70, 80): 'A',
                 range(60, 70): 'B+', range(50, 60): 'B', range(0, 50): 'F'}

    def compute_grade(marks: int, max_marks: int) -> str:
        pct = int(marks * 100 / max_marks) if max_marks > 0 else 0
        for r, g in grade_map.items():
            if pct in r:
                return g
        return 'B'

    exam_marks = [42, 40, 85, 58, 62, 22, 78, 90, 55, 65]
    exam_result_payloads = [
        {
            'examinationId': examinations[i]['id'],
            'studentId': students[i % len(students)]['id'],
            'marksObtained': Decimal(exam_marks[i]),
            'grade': compute_grade(exam_marks[i], EXAMINATION_DATA[i][5]),
            'status': cycle(EXAM_RESULT_STATUSES, i),
        }
        for i in range(10)
    ]
    exam_results = create_many(token, '/exam-results', exam_result_payloads)
    print(f'  ✅ Created {len(exam_results)} exam results')

    # 17. Attendance (uses subjectId)
    attendance_remarks = [
        'Attended Anatomy lecture', 'Absent — clinical posting at SKS Hospital',
        'Arrived 10 min late — bus delay', 'Excused — community health field visit',
        'Attended all sessions', 'Absent — medical leave',
        'Attended lab session', 'Excused — INC workshop',
        'Attended all sessions', 'Absent — family function',
    ]
    attendance_payloads = [
        {
            'studentId': students[i % len(students)]['id'],
            'subjectId': subjects[i % len(subjects)]['id'],
            'date': today - timedelta(days=i + 1),
            'status': cycle(ATTENDANCE_STATUSES, i),
            'type': cycle(ATTENDANCE_TYPES, i),
            'remarks': attendance_remarks[i],
        }
        for i in range(10)
    ]
    attendance = create_many(token, '/attendance', attendance_payloads)
    print(f'  ✅ Created {len(attendance)} attendance records')

    # 18. Agents (area + locality)
    agent_payloads = [
        {
            'name': a[0],
            'phone': a[1],
            'email': a[2],
            'area': a[3],
            'locality': a[4],
            'isActive': a[5],
        }
        for a in AGENTS
    ]
    agents = create_many(token, '/agents', agent_payloads)
    print(f'  ✅ Created {len(agents)} agents')

    # 19. Enquiries
    enquiry_payloads = [
        {
            'name': e[0],
            'email': e[1],
            'phone': e[2],
            'programId': programs[e[3]]['id'],
            'enquiryDate': today - timedelta(days=30 - idx * 3),
            'source': e[4],
            'status': e[5],
            'agentId': agents[e[6]]['id'] if e[6] is not None else None,
            'remarks': e[7],
            'feeDiscussedAmount': Decimal(e[8]) if e[8] is not None else None,
        }
        for idx, e in enumerate(ENQUIRIES)
    ]
    enquiries = create_many(token, '/enquiries', enquiry_payloads)
    print(f'  ✅ Created {len(enquiries)} enquiries')

    # -----------------------------------------------------------------------
    # Summary
    # -----------------------------------------------------------------------
    ids = BatchIds(
        departments=[item['id'] for item in departments],
        programs=[item['id'] for item in programs],
        courses=[item['id'] for item in courses],
        subjects=[item['id'] for item in subjects],
        academic_years=[item['id'] for item in academic_years],
        semesters=[item['id'] for item in semesters],
        faculty=[item['id'] for item in faculty],
        students=[item['id'] for item in students],
        labs=[item['id'] for item in labs],
        fee_structures=[item['id'] for item in fee_structures],
        fee_payments=[item['id'] for item in fee_payments],
        equipment=[item['id'] for item in equipment],
        inventory=[item['id'] for item in inventory],
        maintenance=[item['id'] for item in maintenance],
        examinations=[item['id'] for item in examinations],
        exam_results=[item['id'] for item in exam_results],
        attendance=[item['id'] for item in attendance],
        agents=[item['id'] for item in agents],
        enquiries=[item['id'] for item in enquiries],
    )

    print(f'\n{"=" * 60}')
    print(f'🎉 Seed complete — batch: {seed_tag}')
    print(f'   SKS College of Nursing, Salem — Demo Data Loaded')
    print(f'{"=" * 60}')

    return 0


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except SeedError as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        raise SystemExit(1)

