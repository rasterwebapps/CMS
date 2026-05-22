const SKS_LOGO_DATA_URL = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMDAgMjAwIiB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCI+CiAgPCEtLSBPdXRlciBjaXJjbGUgYmFja2dyb3VuZCAtLT4KICA8Y2lyY2xlIGN4PSIxMDAiIGN5PSIxMDAiIHI9Ijk2IiBmaWxsPSIjMWEyYTVlIiBzdHJva2U9IiM0YTZmYTUiIHN0cm9rZS13aWR0aD0iMiIvPgoKICA8IS0tIElubmVyIGRpdmlkaW5nIGNyb3NzIGxpbmVzIC0tPgogIDxsaW5lIHgxPSIxMDAiIHkxPSIzMCIgeDI9IjEwMCIgeTI9IjE3MCIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIxLjUiIG9wYWNpdHk9IjAuNyIvPgogIDxsaW5lIHgxPSIzMCIgeTE9IjEwMCIgeDI9IjE3MCIgeTI9IjEwMCIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIxLjUiIG9wYWNpdHk9IjAuNyIvPgoKICA8IS0tIENlbnRlciBzbWFsbCBjaXJjbGUgd2l0aCBFU1RELiAxOTkzIC0tPgogIDxjaXJjbGUgY3g9IjEwMCIgY3k9IjEwMCIgcj0iMjIiIGZpbGw9IiMxYTJhNWUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS13aWR0aD0iMS41Ii8+CiAgPHRleHQgeD0iMTAwIiB5PSI5NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjciIGZvbnQtd2VpZ2h0PSJib2xkIiBmaWxsPSJ3aGl0ZSI+RVNURC48L3RleHQ+CiAgPHRleHQgeD0iMTAwIiB5PSIxMDciIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSI3IiBmb250LXdlaWdodD0iYm9sZCIgZmlsbD0id2hpdGUiPjE5OTM8L3RleHQ+CgogIDwhLS0gVG9wLWxlZnQgcXVhZHJhbnQ6IEhlYXJ0IC0tPgogIDxwYXRoIGQ9Ik0gNzIgNzIgQyA2NiA2NCA1NSA2NCA1NSA3NCBDIDU1IDgwIDYyIDg3IDcyIDk0IEMgODIgODcgODkgODAgODkgNzQgQyA4OSA2NCA3OCA2NCA3MiA3MiBaIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45Ii8+CgogIDwhLS0gVG9wLXJpZ2h0IHF1YWRyYW50OiBDYWR1Y2V1cy9tZWRpY2FsIHN0YWZmIChzaW1wbGlmaWVkKSAtLT4KICA8bGluZSB4MT0iMTI4IiB5MT0iNTUiIHgyPSIxMjgiIHkyPSI5MiIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIyLjUiLz4KICA8cGF0aCBkPSJNIDExOCA2NSBDIDExOCA1OCAxMzggNTggMTM4IDY1IEMgMTM4IDcyIDExOCA3MiAxMTggNzkgQyAxMTggODYgMTM4IDg2IDEzOCA3OSIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIxLjgiLz4KICA8Y2lyY2xlIGN4PSIxMjgiIGN5PSI1MyIgcj0iNCIgZmlsbD0id2hpdGUiIG9wYWNpdHk9IjAuOSIvPgoKICA8IS0tIEJvdHRvbS1sZWZ0IHF1YWRyYW50OiBGbGFtZSAtLT4KICA8cGF0aCBkPSJNIDcyIDE0OCBDIDYwIDEzOCA1OCAxMjUgNjUgMTE4IEMgNjMgMTI4IDcwIDEzMiA3MiAxMjUgQyA3NCAxMzIgODEgMTI4IDc5IDExOCBDIDg2IDEyNSA4NCAxMzggNzIgMTQ4IFoiIGZpbGw9IndoaXRlIiBvcGFjaXR5PSIwLjkiLz4KCiAgPCEtLSBCb3R0b20tcmlnaHQgcXVhZHJhbnQ6IEdyYWR1YXRpb24gY2FwIC0tPgogIDxwb2x5Z29uIHBvaW50cz0iMTI4LDExOCAxMDgsMTI4IDEyOCwxMzggMTQ4LDEyOCIgZmlsbD0id2hpdGUiIG9wYWNpdHk9IjAuOSIvPgogIDxyZWN0IHg9IjEyMCIgeT0iMTI4IiB3aWR0aD0iMTYiIGhlaWdodD0iMTAiIHJ4PSIyIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45Ii8+CiAgPGxpbmUgeDE9IjE0OCIgeTE9IjEyOCIgeDI9IjE0OCIgeTI9IjEzOCIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIyIi8+CiAgPGNpcmNsZSBjeD0iMTQ4IiBjeT0iMTQwIiByPSIzIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45Ii8+CgogIDwhLS0gQ2lyY3VsYXIgdGV4dDogU0tTIENPTExFR0UgT0YgTlVSU0lORyAodG9wIGFyYykgLS0+CiAgPHBhdGggaWQ9InRvcEFyYyIgZD0iTSAxOCwxMDAgQSA4Miw4MiAwIDAsMSAxODIsMTAwIiBmaWxsPSJub25lIi8+CiAgPHRleHQgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjEwIiBmb250LXdlaWdodD0iYm9sZCIgZmlsbD0id2hpdGUiIGxldHRlci1zcGFjaW5nPSIyIj4KICAgIDx0ZXh0UGF0aCBocmVmPSIjdG9wQXJjIiBzdGFydE9mZnNldD0iNSUiPlNLUyBDT0xMRUdFIE9GIE5VUlNJTkc8L3RleHRQYXRoPgogIDwvdGV4dD4KCiAgPCEtLSBDaXJjdWxhciB0ZXh0OiBWUyBFRFVDQVRJT05BTCBUUlVTVCwgU0FMRU0gKGJvdHRvbSBhcmMpIC0tPgogIDxwYXRoIGlkPSJib3R0b21BcmMiIGQ9Ik0gMTgsMTAwIEEgODIsODIgMCAwLDAgMTgyLDEwMCIgZmlsbD0ibm9uZSIvPgogIDx0ZXh0IGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSI4LjUiIGZvbnQtd2VpZ2h0PSJib2xkIiBmaWxsPSJ3aGl0ZSIgbGV0dGVyLXNwYWNpbmc9IjEiPgogICAgPHRleHRQYXRoIGhyZWY9IiNib3R0b21BcmMiIHN0YXJ0T2Zmc2V0PSI1JSI+VlMgRURVQ0FUSU9OQUwgVFJVU1QsIFNBTEVNPC90ZXh0UGF0aD4KICA8L3RleHQ+CgogIDwhLS0gWWVsbG93IHN0YXJzIC0tPgogIDxwb2x5Z29uIHBvaW50cz0iMjIsMTAwIDI1LDkyIDI4LDEwMCAyMCw5NSAzMCw5NSIgZmlsbD0iI0ZGRDcwMCIvPgogIDxwb2x5Z29uIHBvaW50cz0iMTc4LDEwMCAxODEsOTIgMTg0LDEwMCAxNzYsOTUgMTg2LDk1IiBmaWxsPSIjRkZENzAwIi8+Cjwvc3ZnPgo=';

export interface AdmissionFormData {
  admissionNumber: string;
  applicationDate: string;
  admissionDate: string;
  academicYear: string;
  programName: string;
  courseName: string | null;
  yearOfStudy: number | null;
  studentType: string | null;
  admissionQuota: string | null;

  studentName: string;
  dateOfBirth: string | null;
  gender: string | null;
  bloodGroup: string | null;
  aadharNumber: string | null;
  nationality: string | null;
  religion: string | null;
  communityCategory: string | null;
  caste: string | null;

  phone: string | null;
  email: string | null;
  postalAddress: string | null;
  street: string | null;
  city: string | null;
  district: string | null;
  state: string | null;
  pincode: string | null;

  fatherName: string | null;
  fatherPhone: string | null;
  fatherEmail: string | null;
  motherName: string | null;
  motherPhone: string | null;
  motherEmail: string | null;

  qualifications: Array<{
    qualificationType: string;
    schoolName: string | null;
    universityOrBoard: string | null;
    monthAndYearOfPassing: string | null;
    percentage: number | null;
    totalMarks: number | null;
    majorSubject: string | null;
  }>;

  documents: Array<{
    documentType: string;
    verificationStatus: string;
  }>;

  declarationPlace: string | null;
  declarationDate: string | null;
  parentConsentGiven: boolean | null;
  applicantConsentGiven: boolean | null;
}

function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = new Date(iso + (iso.includes('T') ? '' : 'T00:00:00'));
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' });
}

function fmtEnum(value: string | null | undefined): string {
  if (!value) return '';
  return value.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

function fmtDocType(code: string): string {
  const labels: Record<string, string> = {
    AADHAR_CARD: 'Aadhar Card',
    PAN_CARD: 'PAN Card',
    BIRTH_CERTIFICATE: 'Birth Certificate',
    TRANSFER_CERTIFICATE: 'Transfer Certificate (TC)',
    MIGRATION_CERTIFICATE: 'Migration Certificate',
    CONDUCT_CERTIFICATE: 'Conduct Certificate',
    COMMUNITY_CERTIFICATE: 'Community Certificate',
    INCOME_CERTIFICATE: 'Income Certificate',
    MEDICAL_CERTIFICATE: 'Medical Certificate / Fitness',
    SSLC_MARKSHEET: 'SSLC / 10th Marksheet',
    HSC_MARKSHEET: 'HSC / 12th Marksheet',
    DEGREE_CERTIFICATE: 'Degree Certificate',
    PROVISIONAL_CERTIFICATE: 'Provisional Certificate',
    PASSPORT_PHOTO: 'Passport Size Photograph',
    SIGNED_AFFIDAVIT: 'Signed Affidavit / Consent Form',
    ANTI_RAGGING_FORM: 'Anti-Ragging Form',
    COUNSELLING_LETTER: 'Counselling Allotment Letter',
    NEET_SCORECARD: 'NEET Scorecard',
    NATIVITY_CERTIFICATE: 'Nativity Certificate',
    OBC_CERTIFICATE: 'OBC Certificate',
    SC_ST_CERTIFICATE: 'SC/ST Certificate',
    DISABILITY_CERTIFICATE: 'Disability Certificate',
    BANK_PASSBOOK: 'Bank Passbook / Account Details',
    SCHOLARSHIP_LETTER: 'Scholarship Letter',
    OTHER: 'Other Document',
  };
  return labels[code] ?? fmtEnum(code);
}

function fmtVerificationStatus(status: string): string {
  const map: Record<string, string> = {
    VERIFIED: 'Verified',
    UPLOADED: 'Submitted (Pending Verification)',
    REJECTED: 'Rejected',
    NOT_UPLOADED: 'Not Submitted',
  };
  return map[status] ?? fmtEnum(status);
}

function fmtQualType(type: string): string {
  const map: Record<string, string> = {
    SSLC: '10th / SSLC',
    HSC: '12th / HSC',
    DIPLOMA: 'Diploma',
    UG: 'Under Graduate (UG)',
    PG: 'Post Graduate (PG)',
    OTHER: 'Other',
  };
  return map[type] ?? fmtEnum(type);
}

function fieldRow(label: string, value: string | null | undefined, cols = 1): string {
  const v = (value ?? '').trim();
  const span = cols > 1 ? ` style="grid-column: span ${cols};"` : '';
  return `<div class="field"${span}>
    <div class="fl">${label}</div>
    <div class="fv">${v || '&nbsp;'}</div>
  </div>`;
}

function buildAdmissionFormHtml(data: AdmissionFormData, autoPrint: boolean): string {
  const qualRows = data.qualifications.length
    ? data.qualifications.map((q, i) => `
      <tr>
        <td>${i + 1}</td>
        <td>${fmtQualType(q.qualificationType)}</td>
        <td>${q.schoolName ?? ''}</td>
        <td>${q.universityOrBoard ?? ''}</td>
        <td>${q.monthAndYearOfPassing ?? ''}</td>
        <td>${q.totalMarks != null ? String(q.totalMarks) : ''}</td>
        <td>${q.percentage != null ? q.percentage.toFixed(1) + '%' : ''}</td>
        <td>${q.majorSubject ?? ''}</td>
      </tr>`).join('')
    : `<tr><td colspan="8" style="text-align:center;color:#888;font-style:italic;padding:8px;">
        Academic qualifications are recorded separately
      </td></tr>`;

  const docRows = data.documents.length
    ? data.documents.map((d, i) => {
        const isVerified = d.verificationStatus === 'VERIFIED';
        const isRejected = d.verificationStatus === 'REJECTED';
        const tickBox = isVerified ? '&#10003;' : (isRejected ? '&#10007;' : '');
        const statusColor = isVerified ? '#166534' : (isRejected ? '#991b1b' : '#92400e');
        return `<tr>
          <td>${i + 1}</td>
          <td>${fmtDocType(d.documentType)}</td>
          <td style="text-align:center;font-weight:bold;color:${statusColor};">${tickBox}</td>
          <td style="color:${statusColor};">${fmtVerificationStatus(d.verificationStatus)}</td>
        </tr>`;
      }).join('')
    : `<tr><td colspan="4" style="text-align:center;color:#888;font-style:italic;padding:8px;">
        No documents on record
      </td></tr>`;

  const addressParts = [
    data.postalAddress, data.street, data.city,
    data.district, data.state, data.pincode,
  ].filter(Boolean).join(', ');

  const genderLabel = data.gender === 'MALE' ? 'Male'
    : data.gender === 'FEMALE' ? 'Female'
    : data.gender === 'OTHER' ? 'Other' : '';

  const studentTypeLabel = data.studentType === 'DAY_SCHOLAR' ? 'Day Scholar'
    : data.studentType === 'HOSTELER' ? 'Hosteler' : '';

  const quotaLabel = data.admissionQuota === 'MANAGEMENT' ? 'Management'
    : data.admissionQuota === 'COUNSELLING' ? 'Counselling' : '';

  const parentConsent  = data.parentConsentGiven  === true ? 'Yes' : data.parentConsentGiven  === false ? 'No' : '';
  const studentConsent = data.applicantConsentGiven === true ? 'Yes' : data.applicantConsentGiven === false ? 'No' : '';

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<title>Admission Form — ${data.studentName}</title>
<style>
  @page { size: A4 portrait; margin: 12mm 14mm; }
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { width: 210mm; background: #fff; color: #000; }
  body {
    font-family: 'Times New Roman', Times, serif;
    font-size: 11px;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  @media screen {
    html { background: #888; display: flex; justify-content: center; padding: 20px; }
    body { box-shadow: 0 2px 16px rgba(0,0,0,.4); padding: 12mm 14mm; }
  }

  /* ── HEADER ── */
  .page-header {
    display: flex; align-items: flex-start; gap: 12px;
    border-bottom: 2.5px double #1a237e; padding-bottom: 8px; margin-bottom: 6px;
  }
  .logo { width: 68px; height: 68px; flex-shrink: 0; object-fit: contain; }
  .college-info { flex: 1; }
  .college-name {
    font-size: 17px; font-weight: 900; color: #1a237e;
    text-transform: uppercase; letter-spacing: 0.4px; line-height: 1.2;
  }
  .college-sub { font-size: 9.5px; color: #333; margin-top: 3px; line-height: 1.6; }
  .form-badge {
    align-self: center;
    border: 2px solid #1a237e; padding: 5px 14px;
    font-size: 11px; font-weight: 900; letter-spacing: 3px;
    text-transform: uppercase; color: #1a237e; white-space: nowrap;
  }

  /* ── SECTIONS ── */
  .section { margin-bottom: 7px; }
  .section-hdr {
    background: #1a237e; color: #fff;
    padding: 3px 8px; font-size: 9px; font-weight: bold;
    letter-spacing: 1.5px; text-transform: uppercase;
  }
  .section-body {
    border: 1px solid #9ba8c4; border-top: none; padding: 6px 8px;
  }

  /* ── FIELD GRID ── */
  .field-grid { display: grid; gap: 5px 10px; }
  .fg-2 { grid-template-columns: 1fr 1fr; }
  .fg-3 { grid-template-columns: 1fr 1fr 1fr; }
  .fg-4 { grid-template-columns: 1fr 1fr 1fr 1fr; }
  .field { display: flex; flex-direction: column; }
  .fl {
    font-size: 8px; font-weight: bold; color: #444;
    text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 1px;
  }
  .fv {
    border-bottom: 1px solid #888; min-height: 15px;
    font-size: 11px; padding: 1px 2px; line-height: 1.3;
  }

  /* ── PERSONAL SECTION LAYOUT (fields + photo) ── */
  .personal-row { display: flex; gap: 10px; align-items: flex-start; }
  .personal-fields { flex: 1; }
  .photo-placeholder {
    width: 88px; flex-shrink: 0;
    border: 1px dashed #777;
    height: 110px;
    display: flex; flex-direction: column;
    align-items: center; justify-content: center;
    text-align: center;
    color: #888; font-size: 8.5px; line-height: 1.6;
    font-style: italic;
  }
  .photo-placeholder .ph-icon { font-size: 22px; margin-bottom: 4px; color: #ccc; }

  /* ── TABLES ── */
  table { width: 100%; border-collapse: collapse; font-size: 9.5px; }
  thead th {
    background: #dde3f0; color: #1a237e;
    font-size: 8.5px; font-weight: bold; text-transform: uppercase;
    letter-spacing: 0.5px; padding: 3px 5px;
    border: 1px solid #9ba8c4; text-align: left;
  }
  tbody td { padding: 3px 5px; border: 1px solid #c5cbd8; }
  tbody tr:nth-child(even) td { background: #f5f6fa; }

  /* ── DECLARATION ── */
  .decl-text {
    font-size: 10px; line-height: 1.6; margin-bottom: 8px;
    text-align: justify;
  }
  .consent-row { display: flex; gap: 24px; margin-bottom: 6px; }
  .consent-item { display: flex; align-items: center; gap: 6px; font-size: 10px; }
  .consent-box {
    width: 14px; height: 14px; border: 1.5px solid #444;
    display: inline-flex; align-items: center; justify-content: center;
    font-size: 10px; font-weight: bold;
  }
  .decl-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 4px; }

  /* ── SIGNATURE BLOCK ── */
  .sig-section { margin-top: 14px; }
  .sig-row { display: flex; justify-content: space-between; }
  .sig-block { text-align: center; width: 28%; }
  .sig-space { height: 32px; }
  .sig-line { border-top: 1px solid #000; padding-top: 3px; font-size: 9px; }

  /* ── WATERMARK / FOOTER ── */
  .form-footer {
    margin-top: 10px; border-top: 1px solid #9ba8c4;
    padding-top: 4px; font-size: 8px; color: #777;
    display: flex; justify-content: space-between;
  }
</style>
</head>
<body>

<!-- HEADER -->
<div class="page-header">
  <img class="logo" src="${SKS_LOGO_DATA_URL}" alt="SKS College of Nursing Logo" />
  <div class="college-info">
    <div class="college-name">SKS College Of Nursing</div>
    <div class="college-sub">
      Run By VS Educational Trust (Regn. No. 579 / 1997)<br/>
      No.31, Neikkarapatti, Salem &ndash; 636 010.<br/>
      Phone: &nbsp;&nbsp;|&nbsp;&nbsp; Email:
    </div>
  </div>
  <div class="form-badge">Admission&nbsp;Form</div>
</div>

<!-- ADMISSION DETAILS -->
<div class="section">
  <div class="section-hdr">Admission Details</div>
  <div class="section-body">
    <div class="field-grid fg-4">
      ${fieldRow('Admission Number', data.admissionNumber)}
      ${fieldRow('Application Date', fmtDate(data.applicationDate))}
      ${fieldRow('Admission Date', fmtDate(data.admissionDate))}
      ${fieldRow('Academic Year', data.academicYear)}
      ${fieldRow('Programme', data.programName)}
      ${fieldRow('Course / Specialisation', data.courseName)}
      ${fieldRow('Year of Study', data.yearOfStudy != null ? `Year ${data.yearOfStudy}` : '')}
      ${fieldRow('Admission Quota', quotaLabel || fmtEnum(data.admissionQuota))}
    </div>
    ${studentTypeLabel ? `<div class="field-grid fg-4" style="margin-top:5px;">
      ${fieldRow('Student Type', studentTypeLabel)}
    </div>` : ''}
  </div>
</div>

<!-- PERSONAL DETAILS -->
<div class="section">
  <div class="section-hdr">Personal Details</div>
  <div class="section-body">
    <div class="personal-row">
      <div class="personal-fields">
        <div class="field-grid fg-2" style="margin-bottom:5px;">
          ${fieldRow('Full Name', data.studentName, 2)}
        </div>
        <div class="field-grid fg-4">
          ${fieldRow('Date of Birth', fmtDate(data.dateOfBirth))}
          ${fieldRow('Gender', genderLabel)}
          ${fieldRow('Blood Group', data.bloodGroup)}
          ${fieldRow('Aadhar Number', data.aadharNumber)}
          ${fieldRow('Nationality', data.nationality)}
          ${fieldRow('Religion', data.religion)}
          ${fieldRow('Community Category', data.communityCategory)}
          ${fieldRow('Caste', data.caste)}
        </div>
      </div>
      <div class="photo-placeholder">
        <div class="ph-icon">&#128247;</div>
        <div>Paste Photo Here</div>
        <div style="font-size:7.5px;">(Passport Size)</div>
      </div>
    </div>
  </div>
</div>

<!-- CONTACT DETAILS -->
<div class="section">
  <div class="section-hdr">Contact Details</div>
  <div class="section-body">
    <div class="field-grid fg-2" style="margin-bottom:5px;">
      ${fieldRow('Address', addressParts || null, 2)}
    </div>
    <div class="field-grid fg-4">
      ${fieldRow('Phone', data.phone)}
      ${fieldRow('Email', data.email)}
    </div>
  </div>
</div>

<!-- PARENT / GUARDIAN DETAILS -->
<div class="section">
  <div class="section-hdr">Parent / Guardian Details</div>
  <div class="section-body">
    <div class="field-grid fg-3" style="margin-bottom:5px;">
      ${fieldRow("Father's Name", data.fatherName)}
      ${fieldRow("Father's Phone", data.fatherPhone)}
      ${fieldRow("Father's Email", data.fatherEmail)}
    </div>
    <div class="field-grid fg-3">
      ${fieldRow("Mother's Name", data.motherName)}
      ${fieldRow("Mother's Phone", data.motherPhone)}
      ${fieldRow("Mother's Email", data.motherEmail)}
    </div>
  </div>
</div>

<!-- ACADEMIC QUALIFICATIONS -->
<div class="section">
  <div class="section-hdr">Academic Qualifications</div>
  <div class="section-body" style="padding:0;">
    <table>
      <thead>
        <tr>
          <th style="width:28px;">#</th>
          <th>Examination</th>
          <th>School / College</th>
          <th>Board / University</th>
          <th>Year / Month</th>
          <th>Total Marks</th>
          <th>Percentage</th>
          <th>Subject / Stream</th>
        </tr>
      </thead>
      <tbody>${qualRows}</tbody>
    </table>
  </div>
</div>

<!-- DOCUMENT CHECKLIST -->
<div class="section">
  <div class="section-hdr">Document Checklist</div>
  <div class="section-body" style="padding:0;">
    <table>
      <thead>
        <tr>
          <th style="width:28px;">#</th>
          <th>Document</th>
          <th style="width:60px;text-align:center;">Collected</th>
          <th>Verification Status</th>
        </tr>
      </thead>
      <tbody>${docRows}</tbody>
    </table>
  </div>
</div>

<!-- DECLARATION -->
<div class="section">
  <div class="section-hdr">Declaration</div>
  <div class="section-body">
    <p class="decl-text">
      I, the undersigned, hereby declare that all the particulars furnished above are true and correct to the best of my knowledge
      and belief. I am aware that any misrepresentation or concealment of facts may result in cancellation of admission.
      I agree to abide by all the rules and regulations of <strong>SKS College Of Nursing</strong> as in force from time to time.
    </p>
    <div class="consent-row">
      <div class="consent-item">
        <div class="consent-box">${parentConsent === 'Yes' ? '&#10003;' : '&nbsp;'}</div>
        <span>Parent / Guardian consent given &nbsp;&nbsp;
          <strong>${parentConsent || '___'}</strong>
        </span>
      </div>
      <div class="consent-item">
        <div class="consent-box">${studentConsent === 'Yes' ? '&#10003;' : '&nbsp;'}</div>
        <span>Applicant consent given &nbsp;&nbsp;
          <strong>${studentConsent || '___'}</strong>
        </span>
      </div>
    </div>
    <div class="decl-meta">
      ${fieldRow('Place of Declaration', data.declarationPlace)}
      ${fieldRow('Date of Declaration', fmtDate(data.declarationDate))}
    </div>
  </div>
</div>

<!-- SIGNATURES -->
<div class="sig-section">
  <div class="sig-row">
    <div class="sig-block">
      <div class="sig-space"></div>
      <div class="sig-line">Parent / Guardian Signature</div>
    </div>
    <div class="sig-block">
      <div style="border:1.5px dashed #999;height:52px;display:flex;align-items:center;justify-content:center;font-size:9px;color:#aaa;">
        College Seal
      </div>
    </div>
    <div class="sig-block">
      <div class="sig-space"></div>
      <div class="sig-line">Signature of Principal</div>
    </div>
  </div>
</div>

<!-- FOOTER -->
<div class="form-footer">
  <span>Generated by OneCMS &ndash; SKS College of Nursing</span>
  <span>Admission No: ${data.admissionNumber} &nbsp;|&nbsp; Printed: ${new Date().toLocaleDateString('en-IN')}</span>
</div>

${autoPrint ? '<script>window.onload = function() { window.print(); };<\/script>' : ''}
</body>
</html>`;
}

export function printAdmissionForm(data: AdmissionFormData): void {
  const html = buildAdmissionFormHtml(data, true);
  const win = window.open('', '_blank', 'width=900,height=750');
  if (win) {
    win.document.write(html);
    win.document.close();
  }
}

export function downloadAdmissionForm(data: AdmissionFormData): void {
  const html = buildAdmissionFormHtml(data, false);
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  const safeName = data.studentName.replace(/[^a-z0-9]/gi, '_');
  anchor.download = `AdmissionForm_${safeName}_${data.admissionNumber}.html`;
  anchor.click();
  URL.revokeObjectURL(url);
}
