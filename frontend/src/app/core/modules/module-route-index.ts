import { NAV_ENTRIES, isNavGroup } from '../nav/nav-config';

interface RouteModuleEntry {
  path: string;
  modules: string[];
}

/**
 * Derived (not hand-maintained) from every `NavItem.modules`/`NavGroup.modules` tag in
 * NAV_ENTRIES, sorted longest-path-first. A route not literally listed as a nav item (e.g. a
 * create/edit/detail sub-route like `/students/:id/edit`) resolves via the longest registered
 * path that is itself, or an ancestor of it — see {@link resolveModulesForUrl}. A route matching
 * no entry at all is "core" (unmapped) — always allowed, mirroring the backend's treatment of
 * permission codes with no module mapping.
 */
const ROUTE_MODULE_INDEX: RouteModuleEntry[] = buildIndex();

function buildIndex(): RouteModuleEntry[] {
  const entries: RouteModuleEntry[] = [];
  for (const entry of NAV_ENTRIES) {
    if (isNavGroup(entry)) {
      for (const item of entry.items) {
        const modules = item.modules ?? entry.modules;
        if (modules && modules.length > 0) {
          entries.push({ path: normalize(item.route), modules });
        }
      }
    } else if (entry.modules && entry.modules.length > 0) {
      entries.push({ path: normalize(entry.route), modules: entry.modules });
    }
  }
  // Longest path first, so a more specific nav route (e.g. "/students/retro-admit") is matched
  // before a shorter one it happens to be nested under (e.g. "/students").
  return entries.sort((a, b) => b.path.length - a.path.length);
}

function normalize(path: string): string {
  return path.startsWith('/') ? path : `/${path}`;
}

/**
 * Resolves which module(s) gate the given URL, walking up from the full path to each parent
 * segment until a registered nav route matches exactly or is an ancestor of it. Returns `null`
 * for a URL with no module mapping at all (core/unmapped — never gated).
 */
export function resolveModulesForUrl(url: string): string[] | null {
  const path = normalize(url.split('?')[0].split('#')[0]);

  for (const entry of ROUTE_MODULE_INDEX) {
    if (path === entry.path || path.startsWith(`${entry.path}/`)) {
      return entry.modules;
    }
  }
  return null;
}
