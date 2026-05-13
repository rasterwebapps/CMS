const runtimeEnv = typeof window !== 'undefined' ? (window as any).__env__ ?? {} : {};
const browserOrigin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8280';

export const environment = {
  production: true,
  keycloak: {
    url:      runtimeEnv.keycloakUrl || browserOrigin,
    realm:    runtimeEnv.keycloakRealm    ?? 'cms',
    clientId: runtimeEnv.keycloakClientId ?? 'cms-frontend',
  },
  apiUrl: '/api/v1',
};
