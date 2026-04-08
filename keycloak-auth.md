# Keycloak Authentication Skill

You are an expert at configuring and integrating Keycloak authentication for the College Management System. This skill helps you work with OAuth2/OIDC authentication using Keycloak.

## CMS Keycloak Configuration

### Realm Details
- **Realm Name**: `cms`
- **Keycloak URL**: `http://localhost:8180` (development)
- **Admin Console**: `http://localhost:8180/admin`

### Client Applications

| Client ID | Type | Purpose |
|-----------|------|---------|
| `cms-frontend` | Public (SPA) | Angular frontend |
| `cms-backend` | Bearer-only | Spring Boot backend |

### User Roles

| Role | Description |
|------|-------------|
| `ROLE_ADMIN` | Full system administration |
| `ROLE_FACULTY` | Faculty member access |
| `ROLE_STUDENT` | Student access |
| `ROLE_LAB_INCHARGE` | Lab management |
| `ROLE_TECHNICIAN` | Technical support |

## Backend Integration (Spring Security)

### Security Configuration

```java
package com.cms.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())  // Stateless API
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract roles from realm_access claim
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }

            // Filter to only ROLE_ prefixed roles
            return roles.stream()
                .filter(role -> role.startsWith("ROLE_"))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        });
        return converter;
    }
}
```

### Application Configuration

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/cms
```

### Getting Current User in Controller

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    @GetMapping
    public ResponseEntity<UserProfile> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        
        // Get roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = realmAccess != null 
            ? (List<String>) realmAccess.get("roles")
            : Collections.emptyList();

        return ResponseEntity.ok(new UserProfile(userId, username, email, roles));
    }
}
```

### Method-Level Security

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    // Single role
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() { ... }

    // Multiple roles (any)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_FACULTY')")
    @GetMapping("/reports")
    public ResponseEntity<Report> getReports() { ... }

    // Custom expression
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == principal.subject")
    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) { ... }
}
```

## Frontend Integration (Angular)

### Auth Service

```typescript
// core/auth/auth.service.ts
import { Injectable, signal, computed } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

export interface UserProfile {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private keycloak: Keycloak;
  
  private readonly _authenticated = signal(false);
  private readonly _userProfile = signal<UserProfile | null>(null);
  private readonly _roles = signal<string[]>([]);

  readonly authenticated = this._authenticated.asReadonly();
  readonly userProfile = this._userProfile.asReadonly();
  readonly roles = this._roles.asReadonly();

  // Role-based computed properties
  readonly isAdmin = computed(() => this._roles().includes('ROLE_ADMIN'));
  readonly isFaculty = computed(() => this._roles().includes('ROLE_FACULTY'));
  readonly isStudent = computed(() => this._roles().includes('ROLE_STUDENT'));
  readonly isLabIncharge = computed(() => this._roles().includes('ROLE_LAB_INCHARGE'));
  readonly isTechnician = computed(() => this._roles().includes('ROLE_TECHNICIAN'));

  constructor() {
    this.keycloak = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    });
  }

  async init(): Promise<boolean> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
      });

      this._authenticated.set(authenticated);

      if (authenticated) {
        await this.loadUserProfile();
      }

      return authenticated;
    } catch (error) {
      console.error('Keycloak init failed:', error);
      return false;
    }
  }

  async login(): Promise<void> {
    await this.keycloak.login();
  }

  async logout(): Promise<void> {
    await this.keycloak.logout({ redirectUri: window.location.origin });
  }

  getToken(): string | undefined {
    return this.keycloak.token;
  }

  async updateToken(minValidity: number = 30): Promise<string> {
    await this.keycloak.updateToken(minValidity);
    return this.keycloak.token!;
  }

  hasRole(role: string): boolean {
    return this._roles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this._roles().includes(role));
  }

  private async loadUserProfile(): Promise<void> {
    const profile = await this.keycloak.loadUserProfile();
    const realmRoles = this.keycloak.realmAccess?.roles ?? [];

    this._roles.set(realmRoles.filter(r => r.startsWith('ROLE_')));
    this._userProfile.set({
      username: profile.username ?? '',
      email: profile.email ?? '',
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      roles: this._roles(),
    });
  }
}
```

### HTTP Interceptor

```typescript
// core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap, catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Only add token for API requests
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  return from(authService.updateToken()).pipe(
    switchMap(token => {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
      return next(authReq);
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.login();
      }
      return throwError(() => error);
    })
  );
};
```

### Auth Guard

```typescript
// core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.authenticated()) {
    return true;
  }

  authService.login();
  return false;
};

// Role-based guard
export const roleGuard = (requiredRoles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.authenticated()) {
      authService.login();
      return false;
    }

    if (authService.hasAnyRole(requiredRoles)) {
      return true;
    }

    router.navigate(['/unauthorized']);
    return false;
  };
};
```

### Using in Routes

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard'),
    canActivate: [authGuard],
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin'),
    canActivate: [roleGuard(['ROLE_ADMIN'])],
  },
  {
    path: 'faculty',
    loadComponent: () => import('./features/faculty/faculty'),
    canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_FACULTY'])],
  },
];
```

### Using in Templates

```html
<!-- Conditional rendering based on roles -->
@if (authService.isAdmin()) {
  <mat-nav-list>
    <a mat-list-item routerLink="/admin">Admin Dashboard</a>
    <a mat-list-item routerLink="/users">Manage Users</a>
  </mat-nav-list>
}

@if (authService.hasAnyRole(['ROLE_ADMIN', 'ROLE_FACULTY'])) {
  <button mat-button (click)="manageStudents()">Manage Students</button>
}

<!-- User profile display -->
@if (authService.userProfile(); as user) {
  <span class="user-name">{{ user.firstName }} {{ user.lastName }}</span>
  <button mat-icon-button (click)="authService.logout()">
    <mat-icon>logout</mat-icon>
  </button>
}
```

## Environment Configuration

```typescript
// environments/environment.ts
export const environment = {
  production: false,
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'cms',
    clientId: 'cms-frontend',
  },
  apiUrl: 'http://localhost:8080/api/v1',
};

// environments/environment.prod.ts
export const environment = {
  production: true,
  keycloak: {
    url: 'https://auth.example.com',
    realm: 'cms',
    clientId: 'cms-frontend',
  },
  apiUrl: 'https://api.example.com/api/v1',
};
```

## Creating New Roles

To add a new role to Keycloak:

1. Edit `infrastructure/keycloak/cms-realm.json`
2. Add to the `roles.realm` array:

```json
{
  "roles": {
    "realm": [
      { "name": "ROLE_ADMIN" },
      { "name": "ROLE_FACULTY" },
      { "name": "ROLE_STUDENT" },
      { "name": "ROLE_LAB_INCHARGE" },
      { "name": "ROLE_TECHNICIAN" },
      { "name": "ROLE_NEW_ROLE" }
    ]
  }
}
```

3. Update the `AuthService` in frontend to include new role checks
4. Use `@PreAuthorize` in backend for the new role

## Testing with Mock Authentication

### Backend Tests

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@Test
void shouldAllowAdminAccess() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
}

@Test
void shouldDenyStudentAccess() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
}
```

### Frontend Tests

```typescript
// Mock AuthService in tests
const mockAuthService = {
  authenticated: signal(true),
  isAdmin: signal(true),
  roles: signal(['ROLE_ADMIN']),
  hasRole: (role: string) => role === 'ROLE_ADMIN',
};

TestBed.configureTestingModule({
  providers: [
    { provide: AuthService, useValue: mockAuthService },
  ],
});
```
