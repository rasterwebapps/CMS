#!/usr/bin/env python3
"""One-off verification for item 16 (Parent Portal, first slice: OC-243).

Mirrors scripts/verify_student_portal_self_service.py exactly, but for a guardian with
TWO real wards (to actually exercise the multi-ward case the design doc flagged):

1. As devadmin, create a Guardian record (POST /guardians).
2. Link it to two real students who already have real attendance/exam-result data
   (POST /guardians/{id}/wards/{studentId}).
3. Provision a real Keycloak PARENT login for that guardian (POST /user-management,
   same code path any admin uses -- not a raw Keycloak/SQL shortcut).
4. Authenticate as the parent and exercise GET /guardian/wards, GET /attendance/my-wards,
   and GET /exam-results/my-wards end to end against real data for both wards.
5. Negative check: a third, unlinked student's id must be rejected with 403, proving
   GuardianService#assertIsMyWard actually enforces ward ownership and not just presence
   of *a* MY_WARD_* permission.

Local dev only.
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

WARD_A_ID = 45  # Oviya Thangam -- 16 real attendance rows, 4 real exam results
WARD_B_ID = 46  # Pavithra Umapathy -- 16 real attendance rows, 4 real exam results
UNLINKED_STUDENT_ID = 47  # Radha Venkatesh -- has real data too, but never linked to this guardian

GUARDIAN_EMAIL = 'test.guardian.parentportal@skscon.edu.in'
GUARDIAN_FIRST = 'Test'
GUARDIAN_LAST = 'Guardian'
GUARDIAN_USERNAME = 'testguardian45'
GUARDIAN_PASSWORD = 'Guardian@1cms'


def clear_temporary_password(username: str) -> None:
    """Admin-created Keycloak accounts get a temporary password by design (must-change-on-
    first-login), which a password-grant token request rejects with a plain 400. Clears that
    flag via the master-realm Keycloak Admin REST API using the checked-in local-dev bootstrap
    credential (docker-compose.yml) -- purely a scripted-verification affordance; a real
    admin-created guardian would clear this themselves on first login. Local dev only."""
    master_token_url = f'{KEYCLOAK_URL}/realms/master/protocol/openid-connect/token'
    body = urllib.parse.urlencode({
        'client_id': 'admin-cli', 'grant_type': 'password',
        'username': 'admin', 'password': 'admin',
    }).encode('utf-8')
    with urllib.request.urlopen(urllib.request.Request(master_token_url, data=body, method='POST'), timeout=30) as r:
        admin_token = json.load(r)['access_token']

    def kc_admin(method: str, path: str, payload=None):
        req = urllib.request.Request(
            f'{KEYCLOAK_URL}/admin/realms/{REALM}{path}',
            data=json.dumps(payload).encode('utf-8') if payload is not None else None,
            headers={'Authorization': f'Bearer {admin_token}', 'Content-Type': 'application/json'},
            method=method)
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode('utf-8')
            return json.loads(raw) if raw else None

    users = kc_admin('GET', f'/users?username={username}&exact=true')
    kc_user_id = users[0]['id']
    kc_admin('PUT', f'/users/{kc_user_id}', {'requiredActions': []})
    kc_admin('PUT', f'/users/{kc_user_id}/reset-password', {
        'type': 'password', 'value': GUARDIAN_PASSWORD, 'temporary': False,
    })


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
    print('Connecting as devadmin...')
    admin_token = get_token(ADMIN_USERNAME, ADMIN_PASSWORD)

    print('\n== POST /guardians (create Guardian record) ==')
    guardian_result = api('POST', '/guardians', admin_token, {
        'firstName': GUARDIAN_FIRST, 'lastName': GUARDIAN_LAST,
        'email': GUARDIAN_EMAIL, 'phone': '9999999999', 'relationshipHint': 'Mother',
    })
    if guardian_result.get('__error__'):
        print(f"  HTTP {guardian_result['__status__']} — {guardian_result['__details__']}")
        # Tolerate "already registered" on a re-run: look it up instead.
        if guardian_result['__status__'] != 400:
            return 1
        all_guardians = api('GET', '/guardians', admin_token)['body']
        guardian_id = next(g['id'] for g in all_guardians if g['email'] == GUARDIAN_EMAIL)
        print(f'  (already exists from a prior run, id={guardian_id} — continuing)')
    else:
        guardian_id = guardian_result['body']['id']
        print(f'  Created guardian id={guardian_id}')

    print(f'\n== Linking guardian {guardian_id} to two real wards ==')
    for student_id, is_primary in [(WARD_A_ID, True), (WARD_B_ID, False)]:
        link_result = api('POST', f'/guardians/{guardian_id}/wards/{student_id}?isPrimary={str(is_primary).lower()}', admin_token)
        status = link_result.get('__status__')
        ok = status == 201 or (link_result.get('__error__') and status == 400)  # 400 = already linked, tolerate
        print(f'  ward {student_id} (primary={is_primary}): HTTP {status} ({"ok" if ok else "UNEXPECTED"})')
        if not ok:
            return 1

    print('\n== POST /user-management (provision the guardian\'s real login) ==')
    create_login = api('POST', '/user-management', admin_token, {
        'email': GUARDIAN_EMAIL, 'fullName': f'{GUARDIAN_FIRST} {GUARDIAN_LAST}',
        'keycloakUsername': GUARDIAN_USERNAME, 'password': GUARDIAN_PASSWORD,
        'roleName': 'PARENT', 'guardianId': guardian_id,
    })
    if create_login.get('__error__'):
        print(f"  HTTP {create_login['__status__']} — {create_login['__details__']}")
        if create_login['__status__'] != 400:
            return 1
        print('  (already exists from a prior run — continuing)')
    else:
        print(f"  Created app user id={create_login['body']['id']} linked to guardian {guardian_id}")

    print(f'\nClearing the temporary-password flag on {GUARDIAN_USERNAME} (scripted-verification affordance only)...')
    clear_temporary_password(GUARDIAN_USERNAME)

    print(f'\nAuthenticating as {GUARDIAN_USERNAME} (parent self-service identity)...')
    parent_token = get_token(GUARDIAN_USERNAME, GUARDIAN_PASSWORD)

    print('\n== GET /guardian/wards ==')
    wards = api('GET', '/guardian/wards', parent_token)
    if wards.get('__error__'):
        print(f"  FAILED: HTTP {wards['__status__']} — {wards['__details__']}")
        return 1
    ward_list = wards['body']
    print(f'  {len(ward_list)} ward(s) returned: {ward_list}')
    if len(ward_list) != 2:
        print('  UNEXPECTED: expected exactly 2 wards')
        return 1

    for student_id, label in [(WARD_A_ID, 'Ward A'), (WARD_B_ID, 'Ward B')]:
        print(f'\n== GET /attendance/my-wards?studentId={student_id} ({label}) ==')
        att = api('GET', f'/attendance/my-wards?studentId={student_id}', parent_token)
        if att.get('__error__'):
            print(f"  FAILED: HTTP {att['__status__']} — {att['__details__']}")
            return 1
        print(f"  {len(att['body'])} real attendance rows returned")

        print(f'== GET /exam-results/my-wards?studentId={student_id} ({label}) ==')
        results = api('GET', f'/exam-results/my-wards?studentId={student_id}', parent_token)
        if results.get('__error__'):
            print(f"  FAILED: HTTP {results['__status__']} — {results['__details__']}")
            return 1
        print(f"  {len(results['body'])} real exam result rows returned")

    print(f'\n== Negative check: GET /attendance/my-wards?studentId={UNLINKED_STUDENT_ID} (not a ward, should be 403) ==')
    forbidden = api('GET', f'/attendance/my-wards?studentId={UNLINKED_STUDENT_ID}', parent_token)
    status = forbidden.get('__status__')
    print(f'  HTTP {status} ({"correctly blocked" if status == 403 else "UNEXPECTED — should have been 403"})')
    if status != 403:
        return 1

    print('\nDone.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
