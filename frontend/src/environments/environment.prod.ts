const runtimeEnv = typeof window !== 'undefined' ? (window as any).__env__ ?? {} : {};

export const environment = {
  production: true,
  keycloak: {
    url:      runtimeEnv.keycloakUrl  ?? 'http://localhost:8280',
    realm:    runtimeEnv.keycloakRealm    ?? 'cms',
    clientId: runtimeEnv.keycloakClientId ?? 'cms-frontend',
  },
  apiUrl: '/api/v1',
};
