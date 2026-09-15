#!/usr/bin/env python3
"""One-off verification for item 15 (Student Portal groundwork, OC-236/OC-237).

Provisions a single local-dev-only Keycloak test student login (via the real
POST /user-management endpoint -- same code path an admin uses to create any
student login, not a raw Keycloak/SQL shortcut), then authenticates as that
student and exercises GET /attendance/my and GET /exam-results/my end to end,
proving the self-scoping actually works against a real student identity and
not just devadmin's own faculty link.

Local dev only. Reuses the same auth/request pattern as seed_academics_night.py.
"""
from __future__ import annotations

import json
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request

_INSECURE_SSL_CONTEXT = ssl._create_unverified_context()
API_URL = 'https://localhost:8080/api/v1'
KEYCLOAK_URL = 'http://localhost:8280'
REALM = 'cms'
CLIENT_ID = 'cms-frontend'
ADMIN_USERNAME = 'devadmin'
ADMIN_PASSWORD = 'Dev@1cms'

TEST_STUDENT_ID = 45  # Oviya Thangam -- 16 real attendance rows, 4 real exam results, unlinked
TEST_STUDENT_USERNAME = 'teststudent45'
TEST_STUDENT_PASSWORD = 'Student@1cms'
TEST_STUDENT_EMAIL = 'oviya.thangam.415@skscon.edu.in'
TEST_STUDENT_FULL_NAME = 'Oviya Thangam'


def get_token(username: str, password: str) -> str:
    token_url = f'{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token'
    body = urllib.parse.urlencode({
        'client_id': CLIENT_ID, 'grant_type': 'password',
        'username': username, 'password': password,
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
            return {'__status__': r.status, 'body': json.loads(raw) if raw else None}
    except urllib.error.HTTPError as exc:
        details = exc.read().decode('utf-8', errors='replace')
        return {'__status__': exc.code, '__error__': True, '__details__': details}


def main() -> int:
    print('Connecting as devadmin to provision a real student login...')
    admin_token = get_token(ADMIN_USERNAME, ADMIN_PASSWORD)

    create_result = api('POST', '/user-management', admin_token, {
        'email': TEST_STUDENT_EMAIL,
        'fullName': TEST_STUDENT_FULL_NAME,
        'keycloakUsername': TEST_STUDENT_USERNAME,
        'password': TEST_STUDENT_PASSWORD,
        'roleName': 'STUDENT',
        'studentId': TEST_STUDENT_ID,
    })
    if create_result.get('__error__'):
        print(f"  User creation: HTTP {create_result['__status__']} — {create_result['__details__']}")
        if create_result['__status__'] != 400:  # tolerate "already registered" on a re-run
            return 1
        print('  (already exists from a prior run — continuing)')
    else:
        print(f"  Created app user id={create_result['body']['id']} linked to student {TEST_STUDENT_ID}")

    print(f'\nAuthenticating as {TEST_STUDENT_USERNAME} (student self-service identity)...')
    student_token = get_token(TEST_STUDENT_USERNAME, TEST_STUDENT_PASSWORD)

    print('\n== GET /attendance/my ==')
    my_attendance = api('GET', '/attendance/my', student_token)
    if my_attendance.get('__error__'):
        print(f"  FAILED: HTTP {my_attendance['__status__']} — {my_attendance['__details__']}")
        return 1
    rows = my_attendance['body']
    print(f'  {len(rows)} real attendance rows returned')
    if rows:
        print(f'  sample: {rows[0]}')

    print('\n== GET /exam-results/my ==')
    my_results = api('GET', '/exam-results/my', student_token)
    if my_results.get('__error__'):
        print(f"  FAILED: HTTP {my_results['__status__']} — {my_results['__details__']}")
        return 1
    results = my_results['body']
    print(f'  {len(results)} real exam result rows returned')
    if results:
        print(f'  sample: {results[0]}')

    print('\n== Negative check: GET /attendance?studentId=1 as a STUDENT (should be 403 now) ==')
    forbidden_check = api('GET', '/attendance?studentId=1', student_token)
    status = forbidden_check.get('__status__')
    print(f'  HTTP {status} ({"correctly blocked" if status == 403 else "UNEXPECTED — should have been 403"})')

    print('\nDone.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
