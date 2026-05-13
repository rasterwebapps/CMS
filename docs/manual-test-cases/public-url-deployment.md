# Public URL Deployment Manual Test Cases

## TC-PUBURL-001: Supported entry origins keep redirects on the same origin

**Preconditions:**
- Production deployment is running on `172.16.7.209`.
- Public DNS `cms.nursing.sksh.ac.in` points to public IP `137.97.6.147` and forwards to the deployment server.
- Deployment environment has `DEPLOY_HOST=cms.nursing.sksh.ac.in`, `PUBLIC_IP=137.97.6.147`, and `LOCAL_HOST=172.16.7.209`.

**Steps:**
1. Open a private/incognito browser window.
2. Navigate to `https://cms.nursing.sksh.ac.in`.
3. Observe the browser address bar during application load and login redirection.
4. Complete Keycloak login with a valid user.
5. Confirm the application dashboard loads.
6. Repeat steps 1-5 for `https://137.97.6.147` from a public network.
7. Repeat steps 1-5 for `https://172.16.7.209` from the local LAN.

**Expected Result:**
- The browser URL remains on the same origin used to open the app during app and Keycloak redirects.
- Public domain users are not redirected to the LAN IP.
- Public IP users are not redirected to the domain or LAN IP.
- LAN users are not redirected to the public domain or public IP.
- Login completes successfully and the dashboard loads.

**Status:** NOT TESTED

## TC-PUBURL-002: Keycloak OIDC metadata exposes each requested issuer

**Preconditions:**
- Production deployment is running after the public URL fix has been deployed.

**Steps:**
1. Send a GET request to `https://cms.nursing.sksh.ac.in/realms/cms/.well-known/openid-configuration`.
2. Verify `issuer` is `https://cms.nursing.sksh.ac.in/realms/cms`.
3. Repeat for `https://137.97.6.147/realms/cms/.well-known/openid-configuration`.
4. Verify `issuer` is `https://137.97.6.147/realms/cms`.
5. Repeat for `https://172.16.7.209/realms/cms/.well-known/openid-configuration` from the local LAN.
6. Verify `issuer` is `https://172.16.7.209/realms/cms`.

**Expected Result:**
- OIDC endpoint URLs match the origin used for the request.
- No OIDC metadata response points to a different supported origin.

**Status:** NOT TESTED

## TC-PUBURL-003: Runtime frontend config allows current-origin Keycloak URL

**Preconditions:**
- Production frontend container is running after redeployment.

**Steps:**
1. Open `https://cms.nursing.sksh.ac.in/assets/env.js`.
2. Inspect `window.__env__.keycloakUrl`.
3. Repeat for `https://137.97.6.147/assets/env.js`.
4. Repeat for `https://172.16.7.209/assets/env.js` from the local LAN.

**Expected Result:**
- `keycloakUrl` is empty or unset, allowing Angular production config to use `window.location.origin`.
- No fixed origin in `env.js` forces users from one supported URL to another.

**Status:** NOT TESTED

