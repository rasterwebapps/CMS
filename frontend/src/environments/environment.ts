const runtimeEnv = typeof window !== 'undefined' ? (window as any).__env__ ?? {} : {};

// Derive from whatever host the browser used to load the page (localhost for
// the dev machine itself, its LAN IP for anyone else) instead of hardcoding
// 'localhost' — Keycloak and the backend both run on that same machine, just
// on different ports (8281 / 8080), during local dev.
const devHost = typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const environment = {
  production: false,
  keycloak: {
    // https + 8281 (not the plain-http 8280 listener): a page served over
    // https:// gets a plain-http Keycloak redirect blocked as mixed content
    // on any origin Chrome doesn't treat as "secure" — true for a LAN IP,
    // not just non-localhost. Keycloak now also serves HTTPS on 8281 with
    // the same local-dev cert (see docker-compose.yml).
    url:      runtimeEnv.keycloakUrl      ?? `https://${devHost}:8281`,
    realm:    runtimeEnv.keycloakRealm    ?? 'cms',
    clientId: runtimeEnv.keycloakClientId ?? 'cms-frontend',
  },
  // https: ng serve runs over HTTPS (see angular.json), and a secure page
  // blocks/fails plain-http XHR calls as mixed content — the backend now
  // carries a matching local dev cert (see application-local.yml).
  apiUrl: `https://${devHost}:8080/api/v1`,
};
