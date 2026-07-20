// Runtime environment — overwritten by Docker entrypoint.sh at container startup.
// This file is the local-dev default (Angular CLI dev server, no Docker).
// keycloakUrl intentionally left unset so environment.ts derives it from
// whatever host the browser used to load the page (localhost or a LAN IP).
window.__env__ = {};
