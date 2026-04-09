# 🤝 Contributing to CMS

Thank you for your interest in contributing to the College Management System! This guide will help you get started.

---

## 📋 Prerequisites

- Git
- Java 21 (JDK)
- Node.js 20+ & Angular CLI
- PostgreSQL (for production profile)
- Docker & Docker Compose (for Keycloak)

---

## 🚀 Getting Started

```bash
# Clone the repository
git clone https://github.com/rasterwebapps/CMS.git
cd CMS

# Start Keycloak (Identity Provider)
docker compose up -d keycloak

# Backend — build & run (defaults to H2 in-memory database)
cd backend
./gradlew bootRun

# Frontend — install dependencies & run
cd ../frontend
npm install
ng serve
```

---

## 🔄 Contribution Workflow

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/ModuleName`)
3. **Make** your changes following the [Technical Standards](docs/TECHNICAL_STANDARDS.md)
4. **Write tests** — maintain 95% backend code coverage (enforced by JaCoCo)
5. **Create manual test cases** in `docs/manual-test-cases/` (see [template](docs/manual-test-cases/README.md))
6. **Commit** your changes (`git commit -m 'Add ModuleName module'`)
7. **Push** to your branch (`git push origin feature/ModuleName`)
8. **Open** a Pull Request

---

## 📏 Coding Standards

Please read the full [Technical Standards](docs/TECHNICAL_STANDARDS.md) before contributing. Key highlights:

### Backend (Java 21 / Spring Boot 3.x)
- Use **Java Records** for DTOs
- Use **constructor injection** for dependencies
- Apply **`@PreAuthorize`** for role-based security
- Use **`BigDecimal`** for all monetary values
- Validate inputs with **Jakarta Bean Validation** (`@Valid`)
- Keep controllers thin — business logic in services

### Frontend (Angular 21)
- Use **Standalone Components** (no NgModules)
- Use **Angular Signals** for state management
- Use **new control flow** syntax (`@if`, `@for`, `@switch`)
- Follow **folder-by-feature** structure
- Use **SCSS + Tailwind CSS** for styling
- Use **Angular Material 21** (Material 3) components

### Database
- Create **Flyway migrations** for all schema changes (PostgreSQL)
- Use `snake_case` for table and column names
- Add proper indexes and constraints

---

## 🧪 Testing Requirements

| Area | Requirement |
|------|-------------|
| Backend unit tests | Required — 95% minimum coverage |
| Backend integration tests | Required for complex logic |
| Frontend unit tests | Not required |
| Manual test cases | **Required for every completed task** |

---

## 📁 Project Structure

```
CMS/
├── README.md                        # Project overview & roadmap
├── CONTRIBUTING.md                  # This file
├── CHANGELOG.md                     # Release history
├── docs/                            # Documentation
│   ├── TECHNICAL_STANDARDS.md       # Architecture & coding standards
│   ├── skills/                      # Copilot skill templates
│   └── manual-test-cases/           # Manual test case documentation
├── .github/
│   └── copilot-instructions.md      # GitHub Copilot custom instructions
├── backend/                         # Spring Boot backend
├── frontend/                        # Angular frontend
└── infrastructure/                  # Docker, Keycloak configs
```

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
