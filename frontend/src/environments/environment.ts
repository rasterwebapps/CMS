const runtimeEnv = typeof window !== 'undefined' ? (window as any).__env__ ?? {} : {};

export const environment = {
  production: false,
  keycloak: {
    url:      runtimeEnv.keycloakUrl      ?? 'http://localhost:8280',
    realm:    runtimeEnv.keycloakRealm    ?? 'cms',
    clientId: runtimeEnv.keycloakClientId ?? 'cms-frontend',
  },
  apiUrl: 'http://localhost:8080/api/v1',
};
