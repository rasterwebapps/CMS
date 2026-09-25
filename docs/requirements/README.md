# 📐 SRS / BRD / FRD — Requirements Documentation

Formal requirements documentation for every OneCMS / College Management System module that has actually been built, organized in the order each module was delivered (Release 1 → Release 2 → Release 3).

**Basis:** These documents did not exist before 2026-09-24. They were reverse-engineered from the shipped code (entities, controllers, migrations, frontend features), `docs/BUSINESS_REQUIREMENTS.md`, and the release milestone trackers — they describe what the system actually does today, not an aspirational spec. Where a module is only partially built, its docs say so explicitly in a "Known Gaps / Not Yet Implemented" section rather than presenting deferred work as done.

**Structure:** each module has its own folder with three documents:
- **SRS.md** — System Requirements Spec (scope, actors, functional requirements, interfaces, non-functional requirements)
- **BRD.md** — Business Requirements Doc (business objective, stakeholders, business rules, workflow, success criteria)
- **FRD.md** — Functional Requirements Doc (screens, permissions, API endpoints, data model, edge cases)

See [`FINDINGS.md`](FINDINGS.md) for a consolidated punch list of real issues (security gaps, a likely functional bug, standards violations, stale docs) that surfaced incidentally while writing these — separate from the requirements docs themselves and worth triaging.

---

## Release 1

### R1-M1 — Foundation & Identity
| Module | Docs |
|---|---|
| Authentication & Identity (Keycloak, JWT, DB-driven roles/permissions) | [SRS](auth-identity/SRS.md) · [BRD](auth-identity/BRD.md) · [FRD](auth-identity/FRD.md) |
| Application Shell & Navigation | [SRS](app-shell-navigation/SRS.md) · [BRD](app-shell-navigation/BRD.md) · [FRD](app-shell-navigation/FRD.md) |

### R1-M2 — Core Academic & Lab Mapping
| Module | Docs |
|---|---|
| Speciality Management | [SRS](speciality-management/SRS.md) · [BRD](speciality-management/BRD.md) · [FRD](speciality-management/FRD.md) |
| Program & Course Management | [SRS](program-course-management/SRS.md) · [BRD](program-course-management/BRD.md) · [FRD](program-course-management/FRD.md) |
| Academic Year & Calendar | [SRS](academic-year-calendar/SRS.md) · [BRD](academic-year-calendar/BRD.md) · [FRD](academic-year-calendar/FRD.md) |
| Lab Setup & Configuration | [SRS](lab-setup-configuration/SRS.md) · [BRD](lab-setup-configuration/BRD.md) · [FRD](lab-setup-configuration/FRD.md) |
| Faculty Management | [SRS](faculty-management/SRS.md) · [BRD](faculty-management/BRD.md) · [FRD](faculty-management/FRD.md) |
| Curriculum & Lab-Curriculum Mapping | [SRS](curriculum-lab-mapping/SRS.md) · [BRD](curriculum-lab-mapping/BRD.md) · [FRD](curriculum-lab-mapping/FRD.md) |

### R1-M3 — Operational Logistics
| Module | Docs |
|---|---|
| Student Management | [SRS](student-management/SRS.md) · [BRD](student-management/BRD.md) · [FRD](student-management/FRD.md) |
| Lab Scheduling & Timetable | [SRS](lab-scheduling-timetable/SRS.md) · [BRD](lab-scheduling-timetable/BRD.md) · [FRD](lab-scheduling-timetable/FRD.md) |
| Attendance Management | [SRS](attendance-management/SRS.md) · [BRD](attendance-management/BRD.md) · [FRD](attendance-management/FRD.md) |

### R1-M4 — Finance & Asset Management
| Module | Docs |
|---|---|
| Fee Structure & Collection | [SRS](fee-structure-collection/SRS.md) · [BRD](fee-structure-collection/BRD.md) · [FRD](fee-structure-collection/FRD.md) |
| Referral & Commission Management (incl. OneBook Payment Gateway) | [SRS](referral-commission-management/SRS.md) · [BRD](referral-commission-management/BRD.md) · [FRD](referral-commission-management/FRD.md) |
| Enquiry-to-Admission Workflow | [SRS](enquiry-admission-workflow/SRS.md) · [BRD](enquiry-admission-workflow/BRD.md) · [FRD](enquiry-admission-workflow/FRD.md) |
| Equipment & Lab Inventory | [SRS](equipment-lab-inventory/SRS.md) · [BRD](equipment-lab-inventory/BRD.md) · [FRD](equipment-lab-inventory/FRD.md) |
| Maintenance & Repair | [SRS](maintenance-repair/SRS.md) · [BRD](maintenance-repair/BRD.md) · [FRD](maintenance-repair/FRD.md) |

### R1-M5 — Assessment & Reporting
| Module | Docs |
|---|---|
| Examination Management | [SRS](examination-management/SRS.md) · [BRD](examination-management/BRD.md) · [FRD](examination-management/FRD.md) |
| Lab Reports & Analytics | [SRS](lab-reports-analytics/SRS.md) · [BRD](lab-reports-analytics/BRD.md) · [FRD](lab-reports-analytics/FRD.md) |

### R1-M6 — Post-R1 Academics Additions
| Module | Docs |
|---|---|
| INC Nursing Curriculum Compliance | [SRS](inc-curriculum-compliance/SRS.md) · [BRD](inc-curriculum-compliance/BRD.md) · [FRD](inc-curriculum-compliance/FRD.md) |
| Student Promotion / Progression | [SRS](student-promotion-progression/SRS.md) · [BRD](student-promotion-progression/BRD.md) · [FRD](student-promotion-progression/FRD.md) |
| Term Lifecycle Management | [SRS](term-lifecycle-management/SRS.md) · [BRD](term-lifecycle-management/BRD.md) · [FRD](term-lifecycle-management/FRD.md) |

---

## Release 2

### R2-M3 — Library Management ✅ Complete
| Module | Docs |
|---|---|
| Library Management | [SRS](library-management/SRS.md) · [BRD](library-management/BRD.md) · [FRD](library-management/FRD.md) |

### R2-M4 — Hostel Management 🟡 Partial
| Module | Docs |
|---|---|
| Hostel Management (Campus Infra + Room Allocation shipped; Mess, Attendance/Leave, and Fee wiring not built — see each doc's Known Gaps) | [SRS](hostel-management/SRS.md) · [BRD](hostel-management/BRD.md) · [FRD](hostel-management/FRD.md) |

---

## Release 3 — Inventory Management (standalone, industry-agnostic module)

### R3-M1 — Core Data Model 🟡 Partial (~60%)
| Module | Docs |
|---|---|
| Core Data Model (Category/Uom/Product/InventoryLocation/StockLedger/CycleCount shipped; `InventoryItem` migration deferred — see Known Gaps) | [SRS](inventory-core-data-model/SRS.md) · [BRD](inventory-core-data-model/BRD.md) · [FRD](inventory-core-data-model/FRD.md) |

### R3-M2 — Procurement & Vendor Management ✅ Done
| Module | Docs |
|---|---|
| Procurement & Vendor Management | [SRS](inventory-procurement-vendor/SRS.md) · [BRD](inventory-procurement-vendor/BRD.md) · [FRD](inventory-procurement-vendor/FRD.md) |

### R3-M3 — GRN & Stock Control ✅ Done
| Module | Docs |
|---|---|
| GRN & Stock Control | [SRS](inventory-grn-stock-control/SRS.md) · [BRD](inventory-grn-stock-control/BRD.md) · [FRD](inventory-grn-stock-control/FRD.md) |

### R3-M4 — Requisition & Issue ✅ Done
| Module | Docs |
|---|---|
| Requisition & Issue | [SRS](inventory-requisition-issue/SRS.md) · [BRD](inventory-requisition-issue/BRD.md) · [FRD](inventory-requisition-issue/FRD.md) |

### R3-M5 — Asset Management ✅ Done
| Module | Docs |
|---|---|
| Asset Management | [SRS](inventory-asset-management/SRS.md) · [BRD](inventory-asset-management/BRD.md) · [FRD](inventory-asset-management/FRD.md) |

### R3-M6 — Approvals & Gate Pass ✅ Done
| Module | Docs |
|---|---|
| Approvals & Gate Pass | [SRS](inventory-approvals-gatepass/SRS.md) · [BRD](inventory-approvals-gatepass/BRD.md) · [FRD](inventory-approvals-gatepass/FRD.md) |

### R3-M7 — Reporting ✅ Done
| Module | Docs |
|---|---|
| Reporting | [SRS](inventory-reporting/SRS.md) · [BRD](inventory-reporting/BRD.md) · [FRD](inventory-reporting/FRD.md) |

Not yet covered here: **R3-M8** (`InventoryItem` migration) and **R3-M9** (Standalone deployability) — both are `⬜ Not Started` per `docs/RELEASE_3_MILESTONES.md` and have no shipped functionality to document.

---

> For the inventory module's original stakeholder-facing SRS (business-team-supplied source document) and gap analysis, see [`docs/inventory-management/`](../inventory-management/README.md). For business rules not yet mapped into a specific module above, see [`docs/BUSINESS_REQUIREMENTS.md`](../BUSINESS_REQUIREMENTS.md).
