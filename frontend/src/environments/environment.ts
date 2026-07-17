const runtimeEnv = typeof window !== 'undefined' ? (window as any).__env__ ?? {} : {};

// Derive from whatever host the browser used to load the page (localhost for
// the dev machine itself, its LAN IP for anyone else) instead of hardcoding
// 'localhost' — Keycloak and the backend both run on that same machine, just
// on different ports (8280 / 8080), during local dev.
const devHost = typeof window !== 'undefined' ? window.location.hostname : 'localhost';

export const environment = {
  production: false,
  keycloak: {
    url:      runtimeEnv.keycloakUrl      ?? `http://${devHost}:8280`,
    realm:    runtimeEnv.keycloakRealm    ?? 'cms',
    clientId: runtimeEnv.keycloakClientId ?? 'cms-frontend',
  },
  // https: ng serve runs over HTTPS (see angular.json), and a secure page
  // blocks/fails plain-http XHR calls as mixed content — the backend now
  // carries a matching local dev cert (see application-local.yml).
  apiUrl: `https://${devHost}:8080/api/v1`,
};
