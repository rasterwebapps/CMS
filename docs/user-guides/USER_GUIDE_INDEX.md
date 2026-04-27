# SKS College of Nursing
# CMS User Guide Index

**Version 2.0 | April 27, 2026**

---

## Quick Navigation — Find Your Guide

| Your Role | Read This Guide | Time |
|-----------|----------------|------|
| College Administrator | [COLLEGE_ADMIN_USER_GUIDE.md](./COLLEGE_ADMIN_USER_GUIDE.md) | ~1.5 hours |
| Front Office Staff | [FRONT_OFFICE_USER_GUIDE.md](./FRONT_OFFICE_USER_GUIDE.md) | ~1 hour |
| Cashier / Finance | [CASHIER_USER_GUIDE.md](./CASHIER_USER_GUIDE.md) | ~45 min |

---

## SKS College of Nursing — Quick Overview

**SKS College of Nursing** is an INC-approved nursing institute offering:

| Program | Code | Duration | Seats |
|---------|------|----------|-------|
| Bachelor of Science in Nursing | BSC-NUR | 4 Years | 60 |
| Master of Science in Nursing | MSC-NUR | 2 Years | 30 |
| General Nursing & Midwifery | GNM | 3½ Years | 60 |
| Post Basic B.Sc Nursing | PBB-NUR | 2 Years | 30 |
| Diploma in OT Technician | DOTT | 2 Years | 30 |
| Diploma in Medical Lab Technology | DMLT | 2 Years | 30 |

---

## Complete Admission Workflow — All Roles

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║         SKS COLLEGE OF NURSING — ADMISSION WORKFLOW OVERVIEW                 ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║   PROSPECTIVE STUDENT                                                         ║
║         │ Phone / Walk-in / Social media                                      ║
║         ▼                                                                     ║
║   ┌──────────────────────────────────┐                                        ║
║   │ STEP 1: ENQUIRY CREATION         │ ← Front Office                        ║
║   │ Status: ENQUIRED                 │                                        ║
║   └─────────────────┬────────────────┘                                        ║
║                     │ Student confirms                                        ║
║                     ▼                                                         ║
║   ┌──────────────────────────────────┐                                        ║
║   │ STEP 2: CONFIRM INTEREST         │ ← Front Office                        ║
║   │ Status: INTERESTED               │                                        ║
║   └─────────────────┬────────────────┘                                        ║
║                     │                                                         ║
║                     ▼                                                         ║
║   ┌──────────────────────────────────┐                                        ║
║   │ STEP 3: FEE FINALIZATION         │ ← College Admin ONLY                  ║
║   │ Status: FEES_FINALIZED           │                                        ║
║   └─────────────────┬────────────────┘                                        ║
║                     │ Student pays                                            ║
║                     ▼                                                         ║
║   ┌──────────────────────────────────┐                                        ║
║   │ STEP 4: PAYMENT COLLECTION       │ ← Cashier / Front Office / Admin      ║
║   │ Status: FEES_PAID                │                                        ║
║   │       / PARTIALLY_PAID           │                                        ║
║   └─────────────────┬────────────────┘                                        ║
║                     │                                                         ║
║                     ▼                                                         ║
║   ┌──────────────────────────────────┐                                        ║
║   │ STEP 5: DOCUMENT COLLECTION      │ ← Front Office / Admin                ║
║   │ 7 mandatory documents:           │                                        ║
║   │ 10th, 12th, TC, Aadhar, Photo,   │                                        ║
║   │ Medical Cert, Blood Group        │                                        ║
║   │ Status: DOCUMENTS_SUBMITTED      │                                        ║
║   └─────────────────┬────────────────┘                                        ║
║                     │                                                         ║
║                     ▼                                                         ║
║   ┌──────────────────────────────────┐                                        ║
║   │ STEP 6: ADMISSION CREATION       │ ← Front Office / Admin                ║
║   │ Student ID: SKS-NUR-2026-XXX     │                                        ║
║   │ Status: ADMITTED ✅              │                                        ║
║   └──────────────────────────────────┘                                        ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

---

## Who Does What — Role Responsibility Matrix

| Task | Front Office | Cashier | College Admin |
|------|:---:|:---:|:---:|
| Create new enquiry | ✅ | ❌ | ✅ |
| View enquiries | ✅ | ✅ (read) | ✅ |
| Update enquiry status | ✅ | ❌ | ✅ |
| Finalize student fees | ❌ | ❌ | ✅ only |
| Collect payments | ✅ | ✅ | ✅ |
| Upload & verify documents | ✅ | ❌ | ✅ |
| Submit documents | ✅ | ❌ | ✅ |
| Create admissions | ✅ | ❌ | ✅ |
| Configure programs/fees | ❌ | ❌ | ✅ only |
| View reports | ✅ | ✅ | ✅ |
| Access finance config | ❌ | ❌ | ✅ only |

---

## Enquiry Status Reference

| Status | Meaning | Who Acts |
|--------|---------|---------|
| `ENQUIRED` | Student expressed interest | Front Office: evaluate, guide |
| `INTERESTED` | Student confirmed interest | College Admin: finalize fees |
| `FEES_FINALIZED` | Fee amount set | Cashier/Front Office: collect payment |
| `FEES_PAID` | Fully paid | Front Office: collect documents |
| `PARTIALLY_PAID` | Partially paid | Cashier: collect more; Front Office: collect docs |
| `DOCUMENTS_SUBMITTED` | All 7 docs uploaded | Front Office/Admin: create admission |
| `ADMITTED` | ✅ Student admitted | Everyone: celebrate! |

---

## Document Checklist — All 7 Must Be Collected

For every nursing admission, these documents are **mandatory per INC**:

```
[ ] 1. 10th Grade Marksheet (min 45% aggregate)
[ ] 2. 12th Grade Marksheet (Science — Bio+Chem+Physics)
[ ] 3. Transfer Certificate (from last institution)
[ ] 4. Aadhar Card (government ID)
[ ] 5. Passport Photo (white background, recent)
[ ] 6. Medical Fitness Certificate (within 6 months)
[ ] 7. Blood Group Report (from accredited lab)
```

---

## Payment Methods Accepted at SKS

| Method | Description | Reference Needed |
|--------|-------------|-----------------|
| CASH | In-person cash at counter | None |
| CHEQUE | Crossed cheque | Cheque number |
| NEFT / RTGS | Bank transfer | UTR number |
| UPI | Google Pay / PhonePe / BHIM | Transaction ID |
| CARD | Credit / Debit card | Auth code from terminal |
| DD | Demand Draft | DD number |

---

## Nursing Programs — Eligibility Quick Reference

| Program | Min Qualification | Min % | Stream |
|---------|-----------------|-------|--------|
| B.Sc Nursing | Class 12 | 45% (Gen) / 40% (SC/ST) | Science: Bio+Chem+Physics |
| M.Sc Nursing | B.Sc Nursing + 1 yr | 55% | N/A |
| GNM | Class 12 | 40% | Any |
| Post Basic B.Sc | GNM + 1 yr | — | N/A |
| DOTT / DMLT | Class 12 | 40% | Science preferred |

---

## SKS Academic Calendar (INC) — 2026–27

| Event | Date |
|-------|------|
| New Batch Admission Opens | May 1, 2026 |
| Last Date for Applications | July 31, 2026 |
| Orientation & Induction | August 1–7, 2026 |
| Semester 1 Begins | August 8, 2026 |
| Semester 1 Ends | December 12, 2026 |
| Semester 1 Exams | December 15–28, 2026 |
| Semester 2 Begins | January 5, 2027 |
| Semester 2 Ends | May 15, 2027 |
| Semester 2 Exams | May 18–30, 2027 |
| Annual Clinical Placement | Feb–Mar 2027 (field rotation) |

---

## Fee Summary — 2026–27

| Program | Year 1 | Year 2 | Year 3 | Year 4 |
|---------|--------|--------|--------|--------|
| B.Sc Nursing | ₹1,03,000 | ₹1,00,000 | ₹1,05,000 | ₹1,00,000 |
| M.Sc Nursing | ₹1,25,000 | ₹1,35,000 | — | — |
| GNM | ₹70,000 | ₹67,000 | ₹69,000 | — |

> Hostel fee: ₹36,000/year (optional). Uniform/kit fee: ₹8,000 in Year 1 only.

---

## FAQs

### General

**Q: How do I log in?**
A: Open the CMS URL, enter your college email and Keycloak password. Redirected to your role-based dashboard automatically.

**Q: What browser should I use?**
A: Chrome (recommended), Firefox, or Edge. Use the latest version.

**Q: My session expired — what do I do?**
A: Refresh the page (F5). You will be redirected to Keycloak to log in again.

### Role-Specific

**Q (Front Office): Can I finalize fees?**
A: No. Only the College Admin can finalize fees. You will see the "Finalize Fee" button is not available to you.

**Q (Cashier): Can I see the student documents?**
A: You can view the enquiry but cannot upload or verify documents. Contact Front Office if docs need attention.

**Q (Cashier): Can I create the admission?**
A: No. Admission creation is done by Front Office or College Admin after documents are submitted.

**Q (College Admin): Can I collect payments?**
A: Yes. You can click "Collect Payment" from the enquiry list shortcut, or through Finance → Fee Payments.

### Workflow

**Q: Student wants to pay in two instalments. Is that possible?**
A: Yes. Record each payment separately. Status becomes `PARTIALLY_PAID` after first instalment. Full document submission and admission can still proceed.

**Q: Student lost their receipt. Can we reprint?**
A: Yes. Find the enquiry → Payments tab → Click on the receipt → Print.

**Q: A student paid by NEFT but didn't give UTR number. How do I record?**
A: Check college bank account to verify transfer. Once confirmed, record payment with the UTR from bank statement/portal and note "UTR from bank verification" in remarks.

---

## Support Contacts

| Issue | Contact |
|-------|---------|
| Application login / access | System Administrator |
| Keycloak role issues | IT Department |
| Payment discrepancy | Finance Manager |
| Document requirements | Principal / INC guidelines |
| Program eligibility | Academic Coordinator |
| General system help | Your Department Supervisor |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Apr 27, 2026 | Initial guides |
| 2.0 | Apr 27, 2026 | Updated with SKS nursing-specific content, flow diagrams, PDF export support |

---

**SKS College of Nursing | CMS User Guides**
**Version 2.0 | April 27, 2026**

