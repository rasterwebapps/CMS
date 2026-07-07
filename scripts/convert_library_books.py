#!/usr/bin/env python3
"""
Convert LIBRARY BOOK.xlsx (admin format) → OneCMS import template format.
Uses only Python built-in modules — no pip install required.

Admin columns (8):  Date | Acc.no | Author(S) | TITLE OF THE BOOK | NAME OF THE PUBLISHER | YEAR & EDITION | COLLATION | SERIES
System template (19): Acc No | Entry Date | Title* | Authors* | Publisher | Year | Edition | ISBN | Collation | Series | Call No | Shelf Location | Subject Category | Source | Vendor | Bill No | Bill Date | Price | Remarks

Key remappings:
  Admin "COLLATION"   → system "Shelf Location"   (shelf codes like C1-R2, EX, EXII)
  Admin "SERIES"      → system "Call No"           (call numbers like 612 VAN/H, PHA061)
  Admin "YEAR & EDITION" → split into Year of Publication + Edition
"""

import zipfile
import xml.etree.ElementTree as ET
import io
import re

INPUT  = '/home/raster/Downloads/Telegram Desktop/LIBRARY BOOK.xlsx'
OUTPUT = '/home/raster/Downloads/LIBRARY_BOOK_import.xlsx'

NS_SHEET = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'
NS       = {'s': NS_SHEET}

# ── Helpers ───────────────────────────────────────────────────────────────────

def col_idx(ref):
    """'B3' → 1  (0-based column index, ignoring row number)"""
    letters = re.match(r'([A-Z]+)', ref.upper()).group(1)
    n = 0
    for ch in letters:
        n = n * 26 + (ord(ch) - 64)
    return n - 1

def col_letter(idx):
    """0-based index → Excel column letter(s)  0→A, 25→Z, 26→AA"""
    result = ''
    idx += 1
    while idx:
        idx, rem = divmod(idx - 1, 26)
        result = chr(65 + rem) + result
    return result

def xml_escape(s):
    return (s.replace('&', '&amp;')
             .replace('<', '&lt;')
             .replace('>', '&gt;')
             .replace('"', '&quot;')
             .replace("'", '&apos;'))

# ── Read the admin xlsx ───────────────────────────────────────────────────────

def read_xlsx(path):
    """Return list-of-lists with correct column positions (blank cells preserved)."""
    shared = []
    with zipfile.ZipFile(path) as z:
        if 'xl/sharedStrings.xml' in z.namelist():
            root = ET.fromstring(z.read('xl/sharedStrings.xml'))
            for si in root.findall('s:si', NS):
                txt = ''.join(
                    t.text or ''
                    for t in si.iter(f'{{{NS_SHEET}}}t')
                )
                shared.append(txt)

        sheet = ET.fromstring(z.read('xl/worksheets/sheet1.xml'))
        rows_out = []
        for row_el in sheet.findall('.//s:row', NS):
            cells_el = row_el.findall('s:c', NS)
            if not cells_el:
                rows_out.append([])
                continue
            max_col = max(col_idx(c.get('r', 'A1')) for c in cells_el)
            row = [''] * (max_col + 1)
            for c in cells_el:
                ci   = col_idx(c.get('r', 'A1'))
                t    = c.get('t', '')
                v_el = c.find('s:v', NS)
                if v_el is None:
                    continue
                v = v_el.text or ''
                if t == 's':
                    row[ci] = shared[int(v)]
                elif t in ('str', 'inlineStr'):
                    row[ci] = v
                else:
                    # numeric (acc.no stored as number in Excel)
                    try:
                        d = float(v)
                        row[ci] = str(int(d)) if d == int(d) else str(d)
                    except ValueError:
                        row[ci] = v
            rows_out.append(row)
    return rows_out

# ── Parse combined YEAR & EDITION field ───────────────────────────────────────

def parse_year_edition(s):
    s = s.strip().rstrip("'\"")
    if not s:
        return '', ''

    # "1990/5TH EDITION" or "1966 / 15TH edition"
    m = re.match(r'^(\d{4})\s*/\s*(.+)$', s)
    if m:
        return m.group(1), m.group(2).strip()

    # "5TH EDITION 2026"  "REPRINT 2026"  "1ST EDITION 2006"
    m = re.match(r'^(.+?)\s+(\d{4})[\'"]?\s*$', s)
    if m:
        return m.group(2), m.group(1).strip()

    # Bare year "1990"
    if re.match(r'^\d{4}$', s):
        return s, ''

    # No year found — put everything in Edition
    return '', s

# ── Build output xlsx ─────────────────────────────────────────────────────────

HEADERS = [
    "Acc No (leave blank for auto)", "Entry Date (dd-MM-yyyy)", "Title*",
    "Authors*", "Publisher", "Year of Publication", "Edition", "ISBN",
    "Collation", "Series", "Call No", "Shelf Location", "Subject Category",
    "Source (PURCHASE/DONATION/EXCHANGE)", "Vendor / Donor Name",
    "Bill No", "Bill Date (dd-MM-yyyy)", "Price (Rs)", "Remarks",
]

def build_xlsx(data_rows):
    row_xmls = []

    # Row 1: headers
    cells = ''.join(
        f'<c r="{col_letter(ci)}1" t="inlineStr"><is><t>{xml_escape(h)}</t></is></c>'
        for ci, h in enumerate(HEADERS)
    )
    row_xmls.append(f'<row r="1">{cells}</row>')

    # Data rows
    for ri, row in enumerate(data_rows, start=2):
        cells = ''.join(
            f'<c r="{col_letter(ci)}{ri}" t="inlineStr"><is><t>{xml_escape(val)}</t></is></c>'
            for ci, val in enumerate(row) if val
        )
        if cells:
            row_xmls.append(f'<row r="{ri}">{cells}</row>')

    sheet_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        '<sheetData>' + '\n'.join(row_xmls) + '</sheetData>'
        '</worksheet>'
    )

    workbook_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"'
        ' xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        '<sheets><sheet name="Books" sheetId="1" r:id="rId1"/></sheets>'
        '</workbook>'
    )

    workbook_rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1"'
        ' Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"'
        ' Target="worksheets/sheet1.xml"/>'
        '</Relationships>'
    )

    content_types = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml"  ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml"'
        ' ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        '<Override PartName="/xl/worksheets/sheet1.xml"'
        ' ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        '</Types>'
    )

    dot_rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1"'
        ' Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"'
        ' Target="xl/workbook.xml"/>'
        '</Relationships>'
    )

    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('[Content_Types].xml',        content_types)
        z.writestr('_rels/.rels',                dot_rels)
        z.writestr('xl/workbook.xml',            workbook_xml)
        z.writestr('xl/_rels/workbook.xml.rels', workbook_rels)
        z.writestr('xl/worksheets/sheet1.xml',   sheet_xml)
    return buf.getvalue()

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    print(f"Reading  {INPUT}")
    all_rows = read_xlsx(INPUT)

    # Row 0 = header, row 1 = blank, rows 2+ = data
    data_rows = [r for r in all_rows[2:] if any(v.strip() for v in r)]
    print(f"Found {len(data_rows)} data rows")

    def g(row, i):
        return row[i].strip() if len(row) > i else ''

    out_rows = []
    for r in data_rows:
        year, edition = parse_year_edition(g(r, 5))
        out_rows.append([
            g(r, 1),   # 0  Acc No              ← admin col 1 (Acc.no)
            '',        # 1  Entry Date           ← admin col 0 (Date) is blank for all
            g(r, 3),   # 2  Title*               ← admin col 3
            g(r, 2),   # 3  Authors*             ← admin col 2
            g(r, 4),   # 4  Publisher            ← admin col 4
            year,      # 5  Year of Publication  ← split from admin col 5
            edition,   # 6  Edition              ← split from admin col 5
            '',        # 7  ISBN
            '',        # 8  Collation            (physical description — not in admin file)
            '',        # 9  Series
            g(r, 7),   # 10 Call No              ← admin col 7 (mislabelled "SERIES")
            g(r, 6),   # 11 Shelf Location       ← admin col 6 (mislabelled "COLLATION")
            '',        # 12 Subject Category
            '',        # 13 Source
            '',        # 14 Vendor / Donor Name
            '',        # 15 Bill No
            '',        # 16 Bill Date
            '',        # 17 Price (Rs)
            '',        # 18 Remarks
        ])

    print("Building output xlsx …")
    xlsx_bytes = build_xlsx(out_rows)

    with open(OUTPUT, 'wb') as f:
        f.write(xlsx_bytes)

    size_kb = len(xlsx_bytes) / 1024
    print(f"Done — {OUTPUT}")
    print(f"  Rows: {len(out_rows)} books + 1 header")
    print(f"  Size: {size_kb:.1f} KB")

    # Quick sanity check: print first 3 data rows
    print("\nSample output (first 3 rows):")
    for row in out_rows[:3]:
        print(f"  AccNo={row[0]}  Title={row[2][:40]}  Year={row[5]}  Ed={row[6]}  CallNo={row[10]}  Shelf={row[11]}")

if __name__ == '__main__':
    main()
