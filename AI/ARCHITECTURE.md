# Architecture Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern the Educalink Android application. The project is a **100% Kotlin** educational management platform for SEBRAE partner institutions. All decisions follow Clean Architecture + MVVM + Repository Pattern, implemented as a Gradle multi-module Android project.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [Module Structure](#2-module-structure)
3. [Bounded Contexts](#3-bounded-contexts)
4. [Layered Architecture](#4-layered-architecture)
5. [Domain Modeling](#5-domain-modeling)
6. [Dependency Injection](#6-dependency-injection)
7. [Navigation](#7-navigation)
8. [Data Flow](#8-data-flow)
9. [Multi-Tenancy](#9-multi-tenancy)
10. [Architecture Decision Records](#10-architecture-decision-records)
11. [Architecture Definition of Done](#11-architecture-definition-of-done)

---

## 1. General Principles

| Principle                    | Mandatory Behavior                                                                                             |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **Domain Alignment**         | Modules and packages must mirror business domains (students, courses, classes, teachers, etc.), not tech layers. |
| **Modularity First**         | Feature modules must be independent Android library modules depending only on `:core`.                         |
| **SOLID by Default**         | SOLID principles are mandatory at all levels, as defined in [CODE.md](./CODE.md).                              |
| **Explicit Boundaries**      | The boundary between UI, domain, and data layers must be enforced via package structure and Hilt modules.       |
| **Dependency Inversion**     | ViewModels depend on `AppRepository` via Hilt injection; `AppRepository` depends on `AppDao` abstraction.      |
| **Evolvability**             | Architecture must support adding new feature modules without modifying `:app` beyond navigation registration.   |
| **Security as Architecture** | `AccessPolicy` and multi-tenancy (`companyId`) are architectural first-class concerns. See [SECURITY.md](./SECURITY.md). |
| **Kotlin-First**             | All code must be Kotlin 2.3.20+. No Java source files. Code generation uses KSP (not KAPT).                    |
| **Local-First**              | Room is the single source of truth for UI. Firestore syncs into Room; reads always go through Room.            |

---

## 2. Module Structure

The project is a Gradle multi-module Android build:

```
:app                        # Application entry point (SebraeApplication, MainActivity, AppNavHost)
:core                       # Shared library — data layer, domain models, rule objects, theme, reusable UI
:feature:login              # Login screen + LoginViewModel
:feature:home               # Dashboard + HomeViewModel, RecentActivitiesViewModel
:feature:students           # Student list/create/detail/monitoring + ViewModels
:feature:courses            # Course list/create/detail + ViewModels
:feature:classes            # Class (turma) list/create/detail + ViewModels
:feature:teachers           # Teacher/instructor list/create/detail + ViewModels
:feature:reports            # Aggregated reports & Vico charts + ReportsViewModel
:feature:certificates       # Certificate list + CertificatesViewModel
:feature:calendar           # Academic event calendar + CalendarViewModel
:feature:settings           # App settings + CSV import + ViewModels
:feature:users              # User management + user profile + ViewModels
:feature:companies          # Company management/detail/selector + ViewModels
```

### 2.1 Module Dependency Rules

- `:core` must not depend on any `:feature:*` or `:app` module.
- All `:feature:*` modules depend only on `:core`. Cross-feature dependencies are forbidden.
- `:app` depends on all `:feature:*` modules and `:core` for navigation wiring.
- New feature modules must be registered in `settings.gradle.kts` and added to `:app` navigation.

### 2.2 Package Naming

```
tech.datatower.sebrae.desafio          # Root package
tech.datatower.sebrae.desafio.data     # Data layer (in :core)
tech.datatower.sebrae.desafio.domain   # Domain layer (in :core)
tech.datatower.sebrae.desafio.ui       # UI (screens, ViewModels — per feature module)
```

---

## 3. Bounded Contexts

Each feature module represents a Bounded Context with its own Ubiquitous Language:

| Module            | Bounded Context        | Core Aggregates                                  |
| ----------------- | ---------------------- | ------------------------------------------------ |
| `:feature:login`  | Identity / Auth        | `AppUser`, credentials                           |
| `:feature:home`   | Dashboard              | `QuickStat`, `RecentActivity`                    |
| `:feature:students` | Student Management   | `Student`, `AttendanceRecord`, `BehaviorRecord`, `MonitoringAlert` |
| `:feature:courses` | Course Catalog        | `Course`                                         |
| `:feature:classes` | Class Management      | `SchoolClass`                                    |
| `:feature:teachers` | Instructor Registry  | `Teacher`                                        |
| `:feature:reports` | Analytics & Reports   | `MonthlyEnrollmentEntity`, derived KPIs          |
| `:feature:certificates` | Certification     | `Certificate`                                    |
| `:feature:calendar` | Academic Calendar    | `CalendarEvent`                                  |
| `:feature:settings` | Configuration        | `AppSettings`                                    |
| `:feature:users`  | User Administration    | `AppUser`, `UserCompany`                         |
| `:feature:companies` | Institution Mgmt    | `Company`, `UserCompanyEntity`                   |

### 3.1 Shared Kernel (`:core`)

`:core` acts as the Shared Kernel. It contains:
- **Data layer:** `AppDatabase`, `AppDao`, `AppRepository`, Firebase services
- **Domain models:** all `@Immutable data class` domain objects
- **Rule objects:** `EntityLifecycleRules`, `RelationshipRules`, `RealtimeNotificationRules`, `StudentMonitoringRules`
- **UseCases:** `ObserveStudentsUseCase`, `ObserveCoursesUseCase`, etc.
- **Auth:** `AuthManager`, `AccessPolicy`
- **UI shared:** theme, reusable Composable components
- **Navigation:** `AppRoutes` constants

Changes to `:core` affect all feature modules — treat it with extra caution and always run the full test suite.

---

## 4. Layered Architecture

The architecture has four layers. Dependencies flow strictly inward:

```
UI Layer (Composables + ViewModels)
    ↓ StateFlow / collectAsState
Application Layer (UseCases)
    ↓ suspend fun / Flow<T>
Data Layer (AppRepository → AppDao ↔ FirebaseDataConnectService)
    ↓ Room entities / Firestore documents
Infrastructure (AppDatabase, Firestore, Android Keystore)
```

### 4.1 Layer Responsibilities

| Layer                  | Contains                                                                                        | Must NOT contain                                 |
| ---------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| **UI**                 | `@Composable` screens, `@HiltViewModel` ViewModels, `StateFlow` exposure, navigation calls      | Business logic, database access, Firebase calls  |
| **Application**        | UseCases (`ObserveStudentsUseCase`, `SyncScreenDataUseCase`, etc.)                              | Composables, Room/Firestore implementation       |
| **Data**               | `AppRepository`, `AppDao`, `FirebaseDataConnectService`, Room entities, Firestore mappers       | UI state, ViewModel logic                        |
| **Infrastructure**     | `AppDatabase` (Room config), `FirebaseRemoteBootstrapper`, `FirebaseSeedCredentialStore`        | Domain rules, UI logic                           |

### 4.2 ViewModel Pattern

```kotlin
// Correct pattern
@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val observeStudentsUseCase: ObserveStudentsUseCase
) : ViewModel() {
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()
}

// FORBIDDEN: accessing AppDao directly from a ViewModel
// FORBIDDEN: using LiveData instead of StateFlow
```

---

## 5. Domain Modeling

All domain objects are **immutable `data class`** with `@Immutable` annotation for Compose optimization.

### 5.1 Domain Models

| Model                      | Aggregate Root | Key Fields                                                          |
| -------------------------- | -------------- | ------------------------------------------------------------------- |
| `Student`                  | Yes            | id, companyId, name, email, course, enrolledClass, progress, status |
| `Course`                   | Yes            | id, companyId, title, category, instructor, completionRate, isPublished |
| `SchoolClass`              | Yes            | id, companyId, name, course, instructor, studentsCount, maxCapacity, status |
| `Teacher`                  | Yes            | id, companyId, name, email, specialty, activeCourses, totalStudents, rating, isActive |
| `Certificate`              | Yes            | id, companyId, studentName, courseName, issuedDate, hours, code     |
| `CalendarEvent`            | Yes            | id, companyId, title, course, date, time, location, type            |
| `Company`                  | Yes            | id, name, cnpj, isActive                                            |
| `AppUser`                  | Yes            | id, name, email, role                                               |
| `StudentMonitoringSnapshot`| Composed       | Student + attendance + behavior + pedagogical + psychological data  |

### 5.2 Rule Objects (Pure Domain Services)

Rule objects are pure `object` declarations with no dependencies — they contain only domain logic:

| Object                        | Responsibility                                                        |
| ----------------------------- | --------------------------------------------------------------------- |
| `EntityLifecycleRules`        | Deactivate/reactivate Student, Course, Class, Teacher via `copy()`   |
| `RelationshipRules`           | Real student counts per course/class/teacher; validate links          |
| `RealtimeNotificationRules`   | Build recent-activity drafts; filter notification recipients by role  |
| `StudentMonitoringRules`      | Compute attendance rate; generate `MonitoringAlert` list              |

### 5.3 State Machines

All entity state transitions must go through `EntityLifecycleRules` — never mutate status fields directly:

```
Student.status:     Active ↔ Inactive → Graduated
Course.isPublished: false → true (one-way publication)
SchoolClass.status: Open → InProgress → Closed
```

---

## 6. Dependency Injection

**Hilt 2.59.2** is the mandatory DI framework. Code generation via **KSP** (never KAPT).

| Annotation                                     | Usage                                                             |
| ---------------------------------------------- | ----------------------------------------------------------------- |
| `@HiltAndroidApp`                              | `SebraeApplication` only                                          |
| `@AndroidEntryPoint`                           | `MainActivity` only                                               |
| `@HiltViewModel`                               | Every ViewModel                                                   |
| `@Module @InstallIn(SingletonComponent::class)` | `AppModule` in `:app` — provides all singletons                  |
| `@EntryPoint`                                  | `AppGraphEntryPoint` for Composable contexts without injection    |

### 6.1 What `AppModule` Provides

- `AppDatabase` (Room singleton)
- `AppDao` (from `AppDatabase.appDao()`)
- `AppRepository` (depends on `AppDao` + Firebase services)
- `FirebaseFirestore` instance
- `FirebaseSeedCredentialStore`
- `FirebaseDataConnectService`
- `ConnectivityObserver` (as `NetworkConnectivityObserver`)
- `@DataSourceLabelFlow MutableStateFlow<Int>`

---

## 7. Navigation

**Jetpack Navigation Compose** (`navigation-compose` 2.9.7) with string-route `NavHost`.

- All routes are defined as string constants in `AppRoutes` (in `:core`) — never hardcode route strings.
- `AppNavHost` in `:app` declares all routes for all feature modules.
- Start destination: `AppRoutes.LOGIN`. After login: navigate to `AppRoutes.HOME` clearing back stack.
- Logout: navigate to `AppRoutes.LOGIN` clearing `AppRoutes.HOME` from the stack.
- Arguments are passed as `navArgument` with typed `NavType` (e.g., `NavType.IntType` for IDs).
- Route pattern for detail screens: `"{entity}/{id}"` (e.g., `"students/{studentId}/monitoring"`).

```kotlin
// Correct: use AppRoutes constants
navController.navigate(AppRoutes.STUDENTS)

// FORBIDDEN: hardcoded route strings
navController.navigate("students")
```

---

## 8. Data Flow

The unidirectional data flow is:

```
Firestore (remote) ──sync──→ Room (local cache)
                                    ↓ Flow<T> (AppRepository)
                                    ↓
                              UseCase (optional transform / RBAC filter)
                                    ↓ Flow<T>
                              ViewModel (MutableStateFlow)
                                    ↓ StateFlow (collectAsState)
                              Composable Screen
```

### 8.1 Sync Strategy

- `FirebaseRemoteBootstrapper` runs at startup to seed local Room from Firestore.
- `FirebaseScreenSyncPlanner` defines per-screen minimum Firestore collections to fetch (least-privilege data pull).
- `SyncScreenDataUseCase` is called in `LaunchedEffect` on each screen entry.
- Writes go: ViewModel → `AppRepository.suspend fun` → Room (immediate) → Firestore (async push).
- `ConnectivityObserver` exposes network state; `OfflineBanner` is shown when offline.

---

## 9. Multi-Tenancy

Every data operation is scoped to a `companyId`:

- All Room entities have a `companyId` field.
- All `AppDao` queries filter by `companyId`.
- `AppRepository` receives `companyId` from the authenticated `AppUser` — never from client input.
- `UserCompanyEntity` maps users to their authorized companies.
- Cross-company data access is a security violation — `AppRepository` throws `SecurityException` when `requester.role` is insufficient.

---

## 10. Architecture Decision Records

| ADR  | Decision                                                                  | Rationale                                                       |
| ---- | ------------------------------------------------------------------------- | --------------------------------------------------------------- |
| ADR-001 | Room as local-first DB, Firestore as remote sync                       | Offline-first UX; Firestore for real-time collaboration         |
| ADR-002 | Single `AppRepository` for all UI data access                          | Simplicity at current scale; can split per-aggregate if needed  |
| ADR-003 | Single `AppDao` for all Room queries                                   | Acceptable at current entity count; split if exceeds ~25 tables |
| ADR-004 | Hilt + KSP (not KAPT)                                                  | Faster build times; KAPT deprecated in Kotlin 2.x               |
| ADR-005 | String-route Navigation Compose (not type-safe nav graph)              | Compatibility with current navigation-compose version           |
| ADR-006 | `fallbackToDestructiveMigration(true)` is a development convenience    | Target: enable only in debug variant via `BuildConfig.DEBUG`. **Known gap (2026-05-07):** currently unconditional in `AppModule.provideAppDatabase`. Must be disabled/guarded for production releases. |
| ADR-007 | SHA-256 local credential fallback in `AuthManager`                     | Dev/demo convenience only — must be guarded by `BuildConfig.DEBUG` and removed before production. **Known gap (2026-05-07):** no `BuildConfig.DEBUG` guard in place yet. |

---

## 11. Architecture Definition of Done

A feature or change is architecturally done when:

- [ ] Module dependencies flow only inward (`:feature:*` → `:core`, never sideways).
- [ ] No business logic exists in Composable screens.
- [ ] All new ViewModels use `@HiltViewModel` and expose `StateFlow`.
- [ ] New entities have `companyId` and are filtered in all `AppDao` queries.
- [ ] New routes are added to `AppRoutes` and registered in `AppNavHost`.
- [ ] New data providers are registered in `AppModule`.
- [ ] All sync scopes are registered in `FirebaseScreenSyncPlanner`.
- [ ] `AccessPolicy` is updated if new resources or actions are introduced.
