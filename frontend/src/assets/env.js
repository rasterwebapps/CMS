// Runtime environment — overwritten by Docker entrypoint.sh at container startup.
// This file is the local-dev default (Angular CLI dev server, no Docker).
window.__env__ = {
  keycloakUrl: 'http://localhost:8280'
};
