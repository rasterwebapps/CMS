#!/usr/bin/env python3
"""Populate the CMS backend with at least 10 records for each primary screen.

This script authenticates against Keycloak using the imported local admin user and
creates data through the secured `/api/v1` endpoints in a dependency-safe order.

All data uses realistic, meaningful names that reflect a real Indian engineering
college scenario — departments, programs, courses, faculty, students, labs,
equipment, etc. are named after actual academic domains.
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
# Meaningful reference data
# ---------------------------------------------------------------------------

DEPARTMENTS = [
    {'name': 'Computer Science and Engineering', 'code': 'CSE', 'description': 'Department of Computer Science and Engineering — covers software, algorithms, AI, and systems.', 'hodName': 'Dr. Raghavan Subramanian'},
    {'name': 'Electronics and Communication Engineering', 'code': 'ECE', 'description': 'Department of Electronics and Communication Engineering — VLSI, embedded systems, signal processing.', 'hodName': 'Dr. Meena Krishnamurthy'},
    {'name': 'Mechanical Engineering', 'code': 'MECH', 'description': 'Department of Mechanical Engineering — thermodynamics, manufacturing, robotics.', 'hodName': 'Dr. Arjun Venkatesh'},
    {'name': 'Civil Engineering', 'code': 'CIVIL', 'description': 'Department of Civil Engineering — structural analysis, geotechnical, transportation.', 'hodName': 'Dr. Kavitha Rangan'},
    {'name': 'Electrical and Electronics Engineering', 'code': 'EEE', 'description': 'Department of Electrical and Electronics Engineering — power systems, control, drives.', 'hodName': 'Dr. Senthil Kumar'},
    {'name': 'Information Technology', 'code': 'IT', 'description': 'Department of Information Technology — networks, databases, cybersecurity.', 'hodName': 'Dr. Priya Natarajan'},
    {'name': 'Biomedical Engineering', 'code': 'BME', 'description': 'Department of Biomedical Engineering — medical devices, biomechanics, bioinformatics.', 'hodName': 'Dr. Lakshmi Sundaram'},
    {'name': 'Chemical Engineering', 'code': 'CHE', 'description': 'Department of Chemical Engineering — process design, reaction engineering, polymers.', 'hodName': 'Dr. Ganesh Iyer'},
    {'name': 'Mathematics', 'code': 'MATH', 'description': 'Department of Mathematics — applied math, statistics, operations research.', 'hodName': 'Dr. Revathi Balasubramanian'},
    {'name': 'Physics', 'code': 'PHY', 'description': 'Department of Physics — optics, quantum mechanics, material science.', 'hodName': 'Dr. Mohan Ramachandran'},
]

PROGRAMS = [
    # (name, code, degreeType, durationYears, departmentIndex)
    ('B.Tech Computer Science and Engineering', 'BTCSE', 'BACHELOR', 4, 0),
    ('B.Tech Electronics and Communication', 'BTECE', 'BACHELOR', 4, 1),
    ('B.Tech Mechanical Engineering', 'BTMECH', 'BACHELOR', 4, 2),
    ('B.Tech Civil Engineering', 'BTCIVIL', 'BACHELOR', 4, 3),
    ('B.Tech Electrical and Electronics', 'BTEEE', 'BACHELOR', 4, 4),
    ('M.Tech Data Science', 'MTDS', 'MASTER', 2, 0),
    ('M.Tech VLSI Design', 'MTVLSI', 'MASTER', 2, 1),
    ('M.Tech Structural Engineering', 'MTSE', 'MASTER', 2, 3),
    ('B.Tech Information Technology', 'BTIT', 'BACHELOR', 4, 5),
    ('B.Tech Biomedical Engineering', 'BTBME', 'BACHELOR', 4, 6),
]

COURSES = [
    # (name, code, credits, theoryCredits, labCredits, programIndex, semester)
    ('Data Structures and Algorithms', 'CS201', 4, 3, 1, 0, 3),
    ('Object Oriented Programming', 'CS202', 4, 3, 1, 0, 3),
    ('Database Management Systems', 'CS301', 4, 3, 1, 0, 5),
    ('Digital Signal Processing', 'EC301', 4, 3, 1, 1, 5),
    ('Microprocessors and Microcontrollers', 'EC302', 4, 3, 1, 1, 5),
    ('Thermodynamics', 'ME201', 3, 3, 0, 2, 3),
    ('Fluid Mechanics', 'ME301', 4, 3, 1, 2, 5),
    ('Structural Analysis', 'CE301', 4, 3, 1, 3, 5),
    ('Power Systems Engineering', 'EE401', 4, 3, 1, 4, 7),
    ('Machine Learning', 'CS601', 4, 3, 1, 5, 1),
]

FACULTY_MEMBERS = [
    # (employeeCode, firstName, lastName, email, phone, deptIdx, designation, specialization, labExpertise)
    ('FAC001', 'Ramesh', 'Babu', 'ramesh.babu@college.edu', '9840012345', 0, 'PROFESSOR', 'Artificial Intelligence and Machine Learning', 'Deep Learning Frameworks'),
    ('FAC002', 'Anitha', 'Selvaraj', 'anitha.selvaraj@college.edu', '9840023456', 0, 'ASSOCIATE_PROFESSOR', 'Database Systems', 'SQL and NoSQL Lab Administration'),
    ('FAC003', 'Vijay', 'Kumar', 'vijay.kumar@college.edu', '9840034567', 1, 'PROFESSOR', 'VLSI Design', 'Cadence and Xilinx Tools'),
    ('FAC004', 'Deepa', 'Lakshmi', 'deepa.lakshmi@college.edu', '9840045678', 1, 'ASSISTANT_PROFESSOR', 'Embedded Systems', 'Microcontroller Programming Lab'),
    ('FAC005', 'Suresh', 'Pandian', 'suresh.pandian@college.edu', '9840056789', 2, 'PROFESSOR', 'Thermodynamics and Heat Transfer', 'Thermal Engineering Lab'),
    ('FAC006', 'Karthik', 'Narayanan', 'karthik.narayanan@college.edu', '9840067890', 3, 'ASSOCIATE_PROFESSOR', 'Structural Engineering', 'Concrete Testing Lab'),
    ('FAC007', 'Lavanya', 'Mohan', 'lavanya.mohan@college.edu', '9840078901', 4, 'ASSISTANT_PROFESSOR', 'Power Electronics', 'Power Systems Simulation Lab'),
    ('FAC008', 'Bharathi', 'Kannan', 'bharathi.kannan@college.edu', '9840089012', 5, 'LECTURER', 'Network Security', 'Cybersecurity Lab'),
    ('FAC009', 'Saravanan', 'Ravi', 'saravanan.ravi@college.edu', '9840090123', 6, 'ASSOCIATE_PROFESSOR', 'Medical Imaging', 'Biomedical Instrumentation Lab'),
    ('FAC010', 'Nirmala', 'Devi', 'nirmala.devi@college.edu', '9840001234', 7, 'SENIOR_LECTURER', 'Process Control', 'Chemical Process Simulation Lab'),
]

STUDENTS = [
    # (rollNumber, firstName, lastName, email, phone, programIdx, semester, gender, dob, fatherName, motherName, parentMobile, city, district)
    ('21CSE001', 'Arun', 'Prasad', 'arun.prasad@student.college.edu', '8870012345', 0, 3, 'MALE', '2003-05-14', 'Prasad Venkataraman', 'Lakshmi Prasad', '9443012345', 'Chennai', 'Chennai'),
    ('21CSE002', 'Divya', 'Rajan', 'divya.rajan@student.college.edu', '8870023456', 0, 3, 'FEMALE', '2003-08-22', 'Rajan Gopalan', 'Saroja Rajan', '9443023456', 'Coimbatore', 'Coimbatore'),
    ('21ECE001', 'Karthik', 'Sundaram', 'karthik.sundaram@student.college.edu', '8870034567', 1, 5, 'MALE', '2002-11-03', 'Sundaram Pillai', 'Meenakshi Sundaram', '9443034567', 'Madurai', 'Madurai'),
    ('21ECE002', 'Preethi', 'Murugan', 'preethi.murugan@student.college.edu', '8870045678', 1, 5, 'FEMALE', '2003-01-17', 'Murugan Shanmugam', 'Revathi Murugan', '9443045678', 'Trichy', 'Tiruchirappalli'),
    ('21MECH001', 'Venkatesh', 'Rao', 'venkatesh.rao@student.college.edu', '8870056789', 2, 3, 'MALE', '2003-03-28', 'Rao Srinivasan', 'Padma Rao', '9443056789', 'Salem', 'Salem'),
    ('21CIVIL001', 'Swetha', 'Balaji', 'swetha.balaji@student.college.edu', '8870067890', 3, 5, 'FEMALE', '2002-09-12', 'Balaji Naidu', 'Kamala Balaji', '9443067890', 'Tirunelveli', 'Tirunelveli'),
    ('21EEE001', 'Manoj', 'Krishnan', 'manoj.krishnan@student.college.edu', '8870078901', 4, 7, 'MALE', '2001-12-05', 'Krishnan Iyer', 'Saraswathi Krishnan', '9443078901', 'Vellore', 'Vellore'),
    ('22DS001', 'Sneha', 'Sharma', 'sneha.sharma@student.college.edu', '8870089012', 5, 1, 'FEMALE', '2000-06-20', 'Sharma Raghavan', 'Gayathri Sharma', '9443089012', 'Erode', 'Erode'),
    ('21IT001', 'Prakash', 'Nair', 'prakash.nair@student.college.edu', '8870090123', 8, 3, 'MALE', '2003-04-09', 'Nair Gopinath', 'Janaki Nair', '9443090123', 'Thanjavur', 'Thanjavur'),
    ('21BME001', 'Harini', 'Ganesh', 'harini.ganesh@student.college.edu', '8870001234', 9, 3, 'FEMALE', '2003-07-25', 'Ganesh Subramaniam', 'Parvathi Ganesh', '9443001234', 'Kanchipuram', 'Kanchipuram'),
]

LABS = [
    # (name, labType, deptIdx, building, roomNumber, capacity, status)
    ('Advanced Programming Lab', 'COMPUTER', 0, 'Main Block', 'MB-101', 60, 'ACTIVE'),
    ('Database Systems Lab', 'COMPUTER', 0, 'Main Block', 'MB-102', 40, 'ACTIVE'),
    ('VLSI Design Lab', 'ELECTRONICS', 1, 'ECE Block', 'EC-201', 30, 'ACTIVE'),
    ('Embedded Systems Lab', 'ELECTRONICS', 1, 'ECE Block', 'EC-202', 30, 'ACTIVE'),
    ('Thermal Engineering Lab', 'MECHANICAL', 2, 'Workshop Block', 'WS-101', 25, 'ACTIVE'),
    ('Concrete Testing Lab', 'OTHER', 3, 'Civil Block', 'CB-101', 20, 'ACTIVE'),
    ('Power Systems Lab', 'ELECTRONICS', 4, 'EEE Block', 'EE-201', 30, 'ACTIVE'),
    ('Network Security Lab', 'COMPUTER', 5, 'IT Block', 'IT-301', 40, 'ACTIVE'),
    ('Biomedical Instrumentation Lab', 'ELECTRONICS', 6, 'BME Block', 'BM-101', 25, 'UNDER_MAINTENANCE'),
    ('General Physics Lab', 'PHYSICS', 9, 'Science Block', 'SB-101', 50, 'ACTIVE'),
]

EQUIPMENT_LIST = [
    # (name, assetCode, serialNumber, category, labIdx, manufacturer, model, status, purchasePrice, specifications)
    ('Dell OptiPlex 7090 Desktop', 'AST-CSE-001', 'SN-DELL-7090-001', 'COMPUTER', 0, 'Dell Technologies', 'OptiPlex 7090', 'AVAILABLE', '85000.00', 'Intel i7-11700, 16GB RAM, 512GB SSD, Ubuntu 22.04'),
    ('HP ProDesk 400 G7', 'AST-CSE-002', 'SN-HP-400G7-001', 'COMPUTER', 1, 'HP Inc.', 'ProDesk 400 G7', 'IN_USE', '72000.00', 'Intel i5-10500, 8GB RAM, 256GB SSD, Windows 11'),
    ('Xilinx Artix-7 FPGA Board', 'AST-ECE-001', 'SN-XIL-A7-001', 'ELECTRONIC', 2, 'Xilinx (AMD)', 'Artix-7 XC7A35T', 'AVAILABLE', '45000.00', 'Artix-7 FPGA, 33K logic cells, 1.8V operation'),
    ('Arduino Mega 2560 Kit', 'AST-ECE-002', 'SN-ARD-2560-001', 'ELECTRONIC', 3, 'Arduino', 'Mega 2560 Rev3', 'AVAILABLE', '3500.00', 'ATmega2560, 54 digital I/O, 16 analog inputs'),
    ('Thermal Conductivity Apparatus', 'AST-MECH-001', 'SN-TCA-001', 'MECHANICAL', 4, 'Saraswathi Scientific', 'TCA-200', 'AVAILABLE', '125000.00', "Lee's disc method, digital temperature display, 0-200C range"),
    ('Universal Testing Machine', 'AST-CIVIL-001', 'SN-UTM-001', 'MECHANICAL', 5, 'Aimil Ltd.', 'UTM-1000kN', 'IN_USE', '650000.00', '1000 kN capacity, digital load indicator, 0.5% accuracy'),
    ('Cisco Catalyst 2960 Switch', 'AST-IT-001', 'SN-CISCO-2960-001', 'NETWORKING', 7, 'Cisco Systems', 'Catalyst 2960-24TT', 'AVAILABLE', '38000.00', '24 FastEthernet ports, 2 GbE uplinks, Layer 2 managed'),
    ('Tektronix Digital Oscilloscope', 'AST-EEE-001', 'SN-TEK-TBS1072-001', 'ELECTRONIC', 6, 'Tektronix', 'TBS 1072C', 'AVAILABLE', '55000.00', '70 MHz bandwidth, 2 channels, 1 GS/s sample rate'),
    ('Biomedical Signal Amplifier', 'AST-BME-001', 'SN-BSA-001', 'ELECTRONIC', 8, 'AD Instruments', 'PowerLab 4/26', 'UNDER_MAINTENANCE', '320000.00', '4 input channels, 16-bit resolution, LabChart software'),
    ('Spectrometer', 'AST-PHY-001', 'SN-SPEC-001', 'ELECTRONIC', 9, 'Horiba Scientific', 'iHR320', 'AVAILABLE', '185000.00', '320mm focal length, 0.06nm resolution, CCD detector'),
]

INVENTORY_ITEMS = [
    # (name, itemCode, labIdx, quantity, minimumQuantity, unit, description)
    ('Cat6 Ethernet Cables (3m)', 'INV-NET-001', 0, 100, 20, 'pcs', 'Shielded Cat6 patch cables for lab workstations'),
    ('USB Flash Drives 32GB', 'INV-USB-001', 1, 50, 10, 'pcs', 'SanDisk Ultra 32GB USB 3.0 drives for student use'),
    ('Breadboards (830 tie-points)', 'INV-ECE-001', 3, 60, 15, 'pcs', 'Solderless breadboards for embedded systems experiments'),
    ('Resistor Assortment Kit', 'INV-ECE-002', 2, 40, 10, 'kits', '1/4W carbon film resistors, 10 Ohm to 1M Ohm, 600 pcs per kit'),
    ('Lubricating Oil (1L)', 'INV-MECH-001', 4, 30, 5, 'bottles', 'SAE 40 grade lubricating oil for machine maintenance'),
    ('Cement Bags (OPC 53)', 'INV-CIVIL-001', 5, 25, 5, 'bags', 'Ordinary Portland Cement 53 grade, 50 kg per bag'),
    ('Copper Wire 1.5 sq mm', 'INV-EEE-001', 6, 80, 20, 'meters', 'Single core PVC insulated copper wire for wiring experiments'),
    ('Whiteboard Markers', 'INV-GEN-001', 7, 200, 50, 'pcs', 'Camlin Kokuyo markers, assorted colours for lab sessions'),
    ('Disposable Gloves (M)', 'INV-BME-001', 8, 500, 100, 'pairs', 'Latex examination gloves, medium size, powder-free'),
    ('Glass Prisms (60 deg)', 'INV-PHY-001', 9, 20, 5, 'pcs', 'Equilateral glass prisms for optics experiments, 25mm face'),
]

MAINTENANCE_ENTRIES = [
    # (equipmentIdx, title, description, maintenanceType, priority, status)
    (0, 'Annual Dust Cleaning — Desktop Batch 1', 'Scheduled annual internal cleaning of all CSE lab desktops to prevent overheating.', 'PREVENTIVE', 'LOW', 'SCHEDULED'),
    (1, 'Hard Disk Replacement — HP ProDesk #12', 'Student reported boot failure on workstation 12; diagnostics confirm bad sectors.', 'CORRECTIVE', 'HIGH', 'IN_PROGRESS'),
    (2, 'FPGA Board JTAG Port Repair', 'JTAG programming interface intermittent; needs connector re-soldering.', 'CORRECTIVE', 'MEDIUM', 'REQUESTED'),
    (3, 'Arduino Kit Sensor Calibration', 'Re-calibrate temperature and humidity sensors across 30 Arduino kits.', 'ROUTINE', 'LOW', 'COMPLETED'),
    (4, 'Thermal Apparatus Heater Coil Replacement', 'Heater coil burned out during high-temperature experiment; emergency replacement needed.', 'EMERGENCY', 'CRITICAL', 'IN_PROGRESS'),
    (5, 'UTM Load Cell Calibration', 'Annual calibration of the 1000 kN load cell per BIS standards.', 'PREVENTIVE', 'HIGH', 'SCHEDULED'),
    (6, 'Cisco Switch Firmware Upgrade', 'Upgrade firmware to latest stable IOS to patch known vulnerability.', 'PREVENTIVE', 'MEDIUM', 'REQUESTED'),
    (7, 'Oscilloscope Probe Replacement', 'Probes for channels 1 and 2 have degraded tips; replace with P2220 probes.', 'CORRECTIVE', 'LOW', 'COMPLETED'),
    (8, 'Biomedical Amplifier Power Supply Repair', 'Internal power supply producing voltage ripple beyond 1% threshold.', 'CORRECTIVE', 'HIGH', 'IN_PROGRESS'),
    (9, 'Spectrometer CCD Detector Servicing', 'CCD detector showing increased dark current; needs factory reconditioning.', 'CORRECTIVE', 'CRITICAL', 'REQUESTED'),
]

EXAMINATION_DATA = [
    # (name, courseIdx, examType, daysFromToday, duration, maxMarks)
    ('Data Structures — Mid Semester Theory', 0, 'THEORY', 15, 90, 50),
    ('OOP — End Semester Theory', 1, 'THEORY', 30, 180, 100),
    ('DBMS — Practical Examination', 2, 'PRACTICAL', 20, 120, 50),
    ('Digital Signal Processing — Mid Semester', 3, 'THEORY', 15, 90, 50),
    ('Microprocessors — Lab Viva', 4, 'VIVA', 10, 30, 25),
    ('Thermodynamics — End Semester Theory', 5, 'THEORY', 35, 180, 100),
    ('Fluid Mechanics — Lab Practical', 6, 'PRACTICAL', 25, 120, 50),
    ('Structural Analysis — Mid Semester', 7, 'THEORY', 18, 90, 50),
    ('Power Systems — End Semester Theory', 8, 'THEORY', 40, 180, 100),
    ('Machine Learning — Project Viva', 9, 'VIVA', 22, 60, 50),
]


@dataclass
class BatchIds:
    departments: list[int]
    programs: list[int]
    courses: list[int]
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
FEE_TYPES = ['TUITION', 'LAB_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE', 'HOSTEL_FEE', 'TRANSPORT_FEE', 'MISCELLANEOUS', 'LATE_FEE']
PAYMENT_MODES = ['CASH', 'CARD', 'UPI', 'NET_BANKING', 'CHEQUE', 'DEMAND_DRAFT', 'SCHOLARSHIP']
PAYMENT_STATUSES = ['PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED', 'REFUNDED']
ATTENDANCE_STATUSES = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']
ATTENDANCE_TYPES = ['THEORY', 'LAB']
EXAM_RESULT_STATUSES = ['PENDING', 'PUBLISHED', 'WITHHELD']


def main() -> int:
    token = get_token()
    seed_tag = datetime.now().strftime('DEMO%Y%m%d%H%M%S')
    today = date.today()

    # -----------------------------------------------------------------------
    # 1. Departments
    # -----------------------------------------------------------------------
    departments = create_many(token, '/departments', DEPARTMENTS)
    print(f'  Created {len(departments)} departments')

    # -----------------------------------------------------------------------
    # 2. Academic Years (real calendar years)
    # -----------------------------------------------------------------------
    academic_year_payloads = [
        {
            'name': f'{2020 + i}-{2021 + i}',
            'startDate': date(2020 + i, 6, 1),
            'endDate': date(2021 + i, 5, 31),
            'isCurrent': (2020 + i == today.year) or (i == 6),
        }
        for i in range(1, 11)
    ]
    academic_years = create_many(token, '/academic-years', academic_year_payloads)
    print(f'  Created {len(academic_years)} academic years')

    # -----------------------------------------------------------------------
    # 3. Programs (linked to departments)
    # -----------------------------------------------------------------------
    program_payloads = [
        {
            'name': p[0],
            'code': p[1],
            'degreeType': p[2],
            'durationYears': p[3],
            'departmentId': departments[p[4]]['id'],
        }
        for p in PROGRAMS
    ]
    programs = create_many(token, '/programs', program_payloads)
    print(f'  Created {len(programs)} programs')

    # -----------------------------------------------------------------------
    # 4. Semesters (meaningful names tied to academic years)
    # -----------------------------------------------------------------------
    semester_names = [
        'Odd Semester 2021-22', 'Even Semester 2021-22',
        'Odd Semester 2022-23', 'Even Semester 2022-23',
        'Odd Semester 2023-24', 'Even Semester 2023-24',
        'Odd Semester 2024-25', 'Even Semester 2024-25',
        'Odd Semester 2025-26', 'Even Semester 2025-26',
    ]
    semester_payloads = [
        {
            'name': semester_names[i],
            'academicYearId': academic_years[i // 2]['id'],
            'startDate': date(2021 + i // 2, 6 if i % 2 == 0 else 12, 1),
            'endDate': date(2021 + i // 2, 11, 30) if i % 2 == 0
                       else date(2022 + i // 2, 5, 31),
            'semesterNumber': (i % 8) + 1,
        }
        for i in range(10)
    ]
    semesters = create_many(token, '/semesters', semester_payloads)
    print(f'  Created {len(semesters)} semesters')

    # -----------------------------------------------------------------------
    # 5. Faculty
    # -----------------------------------------------------------------------
    _fac_keys = ['employeeCode', 'firstName', 'lastName', 'email', 'phone',
                 'deptIdx', 'designation', 'specialization', 'labExpertise']
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
    print(f'  Created {len(faculty)} faculty members')

    # -----------------------------------------------------------------------
    # 6. Courses (linked to programs)
    # -----------------------------------------------------------------------
    course_payloads = [
        {
            'name': c[0],
            'code': c[1],
            'credits': c[2],
            'theoryCredits': c[3],
            'labCredits': c[4],
            'programId': programs[c[5]]['id'],
            'semester': c[6],
        }
        for c in COURSES
    ]
    courses = create_many(token, '/courses', course_payloads)
    print(f'  Created {len(courses)} courses')

    # -----------------------------------------------------------------------
    # 7. Students
    # -----------------------------------------------------------------------
    student_payloads = []
    for idx, s in enumerate(STUDENTS):
        student_payloads.append({
            'rollNumber': s[0],
            'firstName': s[1],
            'lastName': s[2],
            'email': s[3],
            'phone': s[4],
            'programId': programs[s[5]]['id'],
            'semester': s[6],
            'admissionDate': today - timedelta(days=365 * 2 + idx * 30),
            'labBatch': f'BATCH-{chr(65 + idx % 4)}',
            'status': 'ACTIVE',
            'dateOfBirth': s[8],
            'gender': s[7],
            'aadharNumber': f'{234500000000 + idx:012d}',
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
                'pincode': f'6{30000 + idx * 11:05d}',
            },
        })
    students = create_many(token, '/students', student_payloads)
    print(f'  Created {len(students)} students')

    # -----------------------------------------------------------------------
    # 8. Labs
    # -----------------------------------------------------------------------
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
    print(f'  Created {len(labs)} labs')

    # -----------------------------------------------------------------------
    # 9. Fee Structures (realistic amounts for Indian engineering college)
    # -----------------------------------------------------------------------
    fee_descriptions = [
        'Tuition fee for B.Tech CSE — AY 2025-26',
        'Lab fee for B.Tech ECE — AY 2025-26',
        'Library fee for B.Tech MECH — AY 2025-26',
        'Examination fee for B.Tech CIVIL — AY 2025-26',
        'Hostel fee for B.Tech EEE — AY 2025-26',
        'Transport fee for M.Tech Data Science — AY 2025-26',
        'Miscellaneous fee for M.Tech VLSI — AY 2025-26',
        'Late payment penalty — M.Tech Structural — AY 2025-26',
        'Tuition fee for B.Tech IT — AY 2025-26',
        'Lab fee for B.Tech BME — AY 2025-26',
    ]
    fee_amounts = [
        '75000.00', '12000.00', '5000.00', '3500.00', '45000.00',
        '85000.00', '15000.00', '2000.00', '70000.00', '14000.00',
    ]
    fee_structure_payloads = [
        {
            'programId': programs[i]['id'],
            'academicYearId': academic_years[min(i, len(academic_years) - 1)]['id'],
            'feeType': cycle(FEE_TYPES, i),
            'amount': Decimal(fee_amounts[i]),
            'description': fee_descriptions[i],
            'isMandatory': i < 5,
            'isActive': True,
        }
        for i in range(10)
    ]
    fee_structures = create_many(token, '/fee-structures', fee_structure_payloads)
    print(f'  Created {len(fee_structures)} fee structures')

    # -----------------------------------------------------------------------
    # 10. Fee Payments
    # -----------------------------------------------------------------------
    payment_remarks = [
        'Tuition fee paid in full', 'Lab fee — partial payment', 'Library fee — paid via UPI',
        'Exam fee — cheque clearance', 'Hostel fee — demand draft', 'Transport fee — scholarship applied',
        'Miscellaneous — cash payment', 'Late fee — net banking', 'Tuition fee — card payment',
        'Lab fee — UPI payment',
    ]
    fee_payment_payloads = [
        {
            'studentId': students[i]['id'],
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
    print(f'  Created {len(fee_payments)} fee payments')

    # -----------------------------------------------------------------------
    # 11. Equipment
    # -----------------------------------------------------------------------
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
            'location': f'Rack {idx + 1}, {LABS[eq[4]][0]}',
            'specifications': eq[9],
        }
        for idx, eq in enumerate(EQUIPMENT_LIST)
    ]
    equipment = create_many(token, '/equipment', equipment_payloads)
    print(f'  Created {len(equipment)} equipment items')

    # -----------------------------------------------------------------------
    # 12. Inventory
    # -----------------------------------------------------------------------
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
    print(f'  Created {len(inventory)} inventory items')

    # -----------------------------------------------------------------------
    # 13. Maintenance Requests
    # -----------------------------------------------------------------------
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
    print(f'  Created {len(maintenance)} maintenance requests')

    # -----------------------------------------------------------------------
    # 14. Examinations
    # -----------------------------------------------------------------------
    examination_payloads = [
        {
            'name': ex[0],
            'courseId': courses[ex[1]]['id'],
            'examType': ex[2],
            'date': today + timedelta(days=ex[3]),
            'duration': ex[4],
            'maxMarks': ex[5],
            'semesterId': semesters[idx % len(semesters)]['id'],
        }
        for idx, ex in enumerate(EXAMINATION_DATA)
    ]
    examinations = create_many(token, '/examinations', examination_payloads)
    print(f'  Created {len(examinations)} examinations')

    # -----------------------------------------------------------------------
    # 15. Exam Results
    # -----------------------------------------------------------------------
    grade_map = {range(90, 101): 'O', range(80, 90): 'A+', range(70, 80): 'A',
                 range(60, 70): 'B+', range(50, 60): 'B', range(0, 50): 'F'}

    def compute_grade(marks: int, max_marks: int) -> str:
        pct = int(marks * 100 / max_marks) if max_marks > 0 else 0
        for r, g in grade_map.items():
            if pct in r:
                return g
        return 'B'

    exam_marks = [42, 85, 38, 40, 22, 78, 42, 38, 88, 45]
    exam_result_payloads = [
        {
            'examinationId': examinations[i]['id'],
            'studentId': students[i]['id'],
            'marksObtained': Decimal(exam_marks[i]),
            'grade': compute_grade(exam_marks[i], EXAMINATION_DATA[i][5]),
            'status': cycle(EXAM_RESULT_STATUSES, i),
        }
        for i in range(10)
    ]
    exam_results = create_many(token, '/exam-results', exam_result_payloads)
    print(f'  Created {len(exam_results)} exam results')

    # -----------------------------------------------------------------------
    # 16. Attendance
    # -----------------------------------------------------------------------
    attendance_remarks = [
        'Attended all sessions', 'Absent — medical leave submitted',
        'Arrived 10 minutes late', 'Excused — participated in inter-college competition',
        'Attended all sessions', 'Absent — no intimation',
        'Arrived 5 minutes late — traffic', 'Excused — college event duty',
        'Attended all sessions', 'Absent — family emergency',
    ]
    attendance_payloads = [
        {
            'studentId': students[i]['id'],
            'courseId': courses[i]['id'],
            'date': today - timedelta(days=i + 1),
            'status': cycle(ATTENDANCE_STATUSES, i),
            'type': cycle(ATTENDANCE_TYPES, i),
            'remarks': attendance_remarks[i],
        }
        for i in range(10)
    ]
    attendance = create_many(token, '/attendance', attendance_payloads)
    print(f'  Created {len(attendance)} attendance records')

    # -----------------------------------------------------------------------
    # Summary
    # -----------------------------------------------------------------------
    ids = BatchIds(
        departments=[item['id'] for item in departments],
        programs=[item['id'] for item in programs],
        courses=[item['id'] for item in courses],
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
    )

    verification = {
        'Departments': len(api_request('GET', '/departments', token)),
        'Programs': len(api_request('GET', '/programs', token)),
        'Courses': len(api_request('GET', '/courses', token)),
        'Academic Years': len(api_request('GET', '/academic-years', token)),
        'Semesters': len(api_request('GET', '/semesters', token)),
        'Faculty': len(api_request('GET', '/faculty', token)),
        'Students': len(api_request('GET', '/students', token)),
        'Attendance': sum(len(api_request('GET', f'/attendance?courseId={course_id}', token)) for course_id in ids.courses),
        'Labs': len(api_request('GET', '/labs', token)),
        'Fee Structures': len(api_request('GET', '/fee-structures', token)),
        'Fee Payments': len(api_request('GET', '/fee-payments', token)),
        'Equipment': len(api_request('GET', '/equipment', token)),
        'Inventory': len(api_request('GET', '/inventory', token)),
        'Maintenance': len(api_request('GET', '/maintenance', token)),
        'Examinations': len(api_request('GET', '/examinations', token)),
        'Exam Results': sum(
            len(api_request('GET', f'/exam-results/examination/{exam_id}', token)) for exam_id in ids.examinations
        ),
    }

    print(f'\nSeed batch: {seed_tag}')
    print('Created record IDs:')
    print(json.dumps(ids.__dict__, indent=2))
    print('\nVerified totals by screen order:')
    for screen, total in verification.items():
        print(f'  {screen}: {total}')

    return 0


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except SeedError as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        raise SystemExit(1)

