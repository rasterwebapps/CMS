#!/usr/bin/env python3
"""One-off top-up seeding for the 2026-09-15/16 Academics overnight autonomous run.

Reuses the same auth/request pattern as seed_demo_data.py (password grant against the
local Keycloak realm, then real POSTs through the secured /api/v1 backend — never raw
SQL) to push real record counts past 50 for screens the DB audit found short:
Faculty Absence Marking, Escort Duties (rotation pools), Student Promotion.

Local dev only. Credentials are the checked-in local-dev Keycloak realm import's
devadmin account (infrastructure/keycloak/cms-realm.json) — not a production secret.
"""
from __future__ import annotations

import json
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import date, timedelta

_INSECURE_SSL_CONTEXT = ssl._create_unverified_context()
API_URL = 'https://localhost:8080/api/v1'
KEYCLOAK_URL = 'http://localhost:8280'
REALM = 'cms'
CLIENT_ID = 'cms-frontend'
USERNAME = 'devadmin'
PASSWORD = 'Dev@1cms'


def get_token() -> str:
    token_url = f'{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token'
    body = urllib.parse.urlencode({
        'client_id': CLIENT_ID, 'grant_type': 'password',
        'username': USERNAME, 'password': PASSWORD,
    }).encode('utf-8')
    with urllib.request.urlopen(urllib.request.Request(token_url, data=body, method='POST'), timeout=30) as r:
        return json.load(r)['access_token']


def api(method: str, path: str, token: str, payload=None):
    url = f'{API_URL}{path}'
    headers = {'Authorization': f'Bearer {token}', 'Accept': 'application/json'}
    data = None
    if payload is not None:
        data = json.dumps(payload).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30, context=_INSECURE_SSL_CONTEXT) as r:
            raw = r.read().decode('utf-8')
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as exc:
        details = exc.read().decode('utf-8', errors='replace')
        return {'__error__': exc.code, '__details__': details}


def seed_faculty_absences(token: str, target: int = 50):
    print('\n== Faculty Absence Marking ==')
    faculties = api('GET', '/faculty', token) or []
    if isinstance(faculties, dict) and 'content' in faculties:
        faculties = faculties['content']
    if not faculties:
        print('  no faculties found via GET /faculties, skipping'); return
    reasons = ['Medical leave', 'Family function', 'Conference attendance', 'Personal emergency',
               'Medical appointment', 'Bereavement leave', 'University duty', 'Sick leave']
    created = 0
    today = date.today()
    for i in range(target):
        fac = faculties[i % len(faculties)]
        fac_id = fac.get('id')
        absence_date = (today + timedelta(days=(i - target // 2))).isoformat()
        resp = api('POST', '/faculty-absences', token, {
            'facultyId': fac_id, 'absenceDate': absence_date, 'reason': reasons[i % len(reasons)],
        })
        if isinstance(resp, dict) and '__error__' in resp:
            if 'already' not in resp['__details__'].lower():
                print(f'  [{i}] faculty {fac_id} {absence_date}: HTTP {resp["__error__"]} {resp["__details__"][:140]}')
            continue
        created += 1
    print(f'  created {created} new faculty_absences rows')


def seed_escort_pools(token: str, max_pools: int = 30):
    print('\n== Escort Duties (rotation pools) ==')
    # Batches aren't listable generically without a term/cohort context in this API;
    # walk cohorts -> their batches via the capacity-plan read endpoints is out of scope
    # for a quick top-up, so instead probe a contiguous range of batch ids (280 exist per
    # the DB audit) and use the real eligibility endpoint to decide if a pool is viable.
    created = 0
    tried = 0
    today = date.today().isoformat()
    for batch_id in range(1, 281):
        if created >= max_pools:
            break
        candidates = api('GET', f'/escort-rotation/batches/{batch_id}/candidates', token)
        tried += 1
        if not isinstance(candidates, list) or len(candidates) < 2:
            continue
        faculty_ids = [c['facultyId'] for c in candidates[:min(4, len(candidates))]]
        resp = api('POST', '/escort-rotation/pools', token, {
            'batchId': batch_id, 'anchorOccurrenceDate': today, 'facultyIds': faculty_ids,
        })
        if isinstance(resp, dict) and '__error__' in resp:
            if 'already has' not in resp['__details__']:
                print(f'  batch {batch_id}: HTTP {resp["__error__"]} {resp["__details__"][:140]}')
            continue
        created += 1
    print(f'  probed {tried} batches, created {created} rotation pools (each = {len(faculty_ids) if created else 0}+ escort_rotation_assignments rows)')


def seed_student_promotions(token: str):
    print('\n== Student Promotion ==')
    cohorts = api('GET', '/cohorts', token) or []
    if isinstance(cohorts, dict) and 'content' in cohorts:
        cohorts = cohorts['content']
    total_decisions = 0
    for cohort in cohorts:
        cohort_id = cohort.get('id')
        terms = api('GET', f'/student-promotions/active-terms?cohortId={cohort_id}', token)
        if not isinstance(terms, list) or len(terms) < 1:
            continue
        for from_term in terms:
            suggestion = api('GET', f'/student-promotions/suggested-next-term?fromTermInstanceId={from_term["termInstanceId"]}', token)
            if not isinstance(suggestion, dict) or 'termInstanceId' not in suggestion:
                continue
            preview = api('POST', '/student-promotions/preview', token, {
                'cohortId': cohort_id,
                'fromTermInstanceId': from_term['termInstanceId'],
                'toTermInstanceId': suggestion['termInstanceId'],
            })
            if not isinstance(preview, dict) or '__error__' in preview or not preview.get('students'):
                if isinstance(preview, dict) and '__error__' in preview:
                    print(f'  cohort {cohort_id} term {from_term["termInstanceId"]}: preview HTTP {preview["__error__"]} {preview["__details__"][:140]}')
                continue
            decisions = [
                {'studentId': s['studentId'], 'outcome': s['recommendedOutcome'], 'remarks': 'Autonomous overnight seed run — recommended outcome applied as-is'}
                for s in preview['students'] if s.get('recommendedOutcome')
            ]
            if not decisions:
                continue
            execute = api('POST', '/student-promotions/execute', token, {
                'cohortId': cohort_id,
                'fromTermInstanceId': from_term['termInstanceId'],
                'toTermInstanceId': suggestion['termInstanceId'],
                'decisions': decisions,
                'generateCourseRegistrations': False,
                'generateFeeDemands': False,
            })
            if isinstance(execute, dict) and '__error__' in execute:
                print(f'  cohort {cohort_id} {from_term["termInstanceId"]}->{suggestion["termInstanceId"]}: execute HTTP {execute["__error__"]} {execute["__details__"][:200]}')
                continue
            n = len(decisions)
            total_decisions += n
            print(f'  cohort {cohort_id}: {from_term["termLabel"]} -> {suggestion["termLabel"]}: {n} decisions executed')
    print(f'  total student_promotion_decisions created: {total_decisions}')


def seed_staff_swaps(token: str, class_schedule_ids_by_day: dict, target: int = 50):
    print('\n== Swap Staff Sessions ==')
    from datetime import datetime
    weekday_index = {'MONDAY': 0, 'TUESDAY': 1, 'WEDNESDAY': 2, 'THURSDAY': 3, 'FRIDAY': 4, 'SATURDAY': 5}
    term_start = date(2026, 10, 1)  # term_instances.id=1 start_date, confirmed via DB read
    applied = 0
    used_targets = set()
    for cs_id, day in class_schedule_ids_by_day:
        if applied >= target:
            break
        wd = weekday_index.get(day)
        if wd is None:
            continue
        delta = (wd - term_start.weekday()) % 7
        occurrence_date = (term_start + timedelta(days=delta + 7 * (applied % 4))).isoformat()  # spread across a few weeks
        candidates = api('GET', f'/timetables/staff-swap/sessions/{cs_id}/candidates?date={occurrence_date}', token)
        if not isinstance(candidates, list) or not candidates:
            continue
        pick = next((c for c in candidates if c['classScheduleId'] not in used_targets), None)
        if pick is None:
            continue
        resp = api('POST', f'/timetables/staff-swap/sessions/{cs_id}/apply', token, {
            'targetClassScheduleId': pick['classScheduleId'], 'date': occurrence_date,
        })
        if isinstance(resp, dict) and '__error__' in resp:
            print(f'  cs {cs_id} <-> {pick["classScheduleId"]} on {occurrence_date}: HTTP {resp["__error__"]} {resp["__details__"][:140]}')
            continue
        used_targets.add(cs_id)
        used_targets.add(pick['classScheduleId'])
        applied += 1
    print(f'  applied {applied} real faculty swaps (each writes real session_occurrences exception rows for both sides)')


def main() -> int:
    print('Academics overnight top-up seeding — connecting as devadmin')
    token = get_token()
    seed_faculty_absences(token)
    seed_escort_pools(token)
    seed_student_promotions(token)
    return 0


if __name__ == '__main__':
    sys.exit(main())
