# 📝 Changelog

All notable changes to the College Management System will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Initial project documentation structure
- Technical standards & architecture guide
- GitHub Copilot skills for Angular, Spring Boot, Flyway, and Keycloak
- Manual test case template and guidelines
- Contributing guide

### R1-M1: Foundation & Identity
- **Backend Security Configuration**
  - `SecurityConfig` with OAuth2 Resource Server, CORS, CSRF, stateless sessions
  - `JwtRoleConverter` for Keycloak realm role extraction
  - `GlobalExceptionHandler` with standardized `ErrorResponse` format
  - `ResourceNotFoundException` custom exception
  - Health check endpoint (`GET /api/v1/health`)
  - Unit tests for all security components (95% JaCoCo coverage)
- **Frontend Authentication**
  - `AuthService` — Keycloak initialization, login/logout, token management with Angular Signals
  - `AuthGuard` — route protection with Keycloak login redirect
  - `RoleGuard` — role-based route access control
  - `AuthInterceptor` — automatic Bearer token injection for API requests
  - Environment configuration for Keycloak and API URL
- **Application Shell & Navigation**
  - `AppComponent` with Material 3 sidenav layout, toolbar, and user menu
  - `DashboardComponent` with placeholder metric cards
  - Lazy-loaded route configuration with auth guard protection
  - Light/dark theme toggle
  - `@angular/animations` dependency added for Material component animations
- **Manual Test Cases**
  - `docs/manual-test-cases/security-config.md` — 6 test cases
  - `docs/manual-test-cases/authentication.md` — 7 test cases
  - `docs/manual-test-cases/app-shell.md` — 8 test cases
