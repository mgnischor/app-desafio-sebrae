# Business Rules Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern all business logic implemented in the Educalink Android application. Every rule listed here must be implemented inside the appropriate rule object or aggregate — never in Composables, ViewModels, or infrastructure classes.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [Business Rule Classification](#2-business-rule-classification)
3. [Rule Catalog](#3-rule-catalog)
   - 3.1 [Student Rules](#31-student-rules)
   - 3.2 [Student Monitoring Rules](#32-student-monitoring-rules)
   - 3.3 [Entity Lifecycle Rules](#33-entity-lifecycle-rules)
   - 3.4 [Relationship Rules](#34-relationship-rules)
   - 3.5 [Notification Rules](#35-notification-rules)
   - 3.6 [Authorization Rules (RBAC)](#36-authorization-rules-rbac)
   - 3.7 [Sync Rules](#37-sync-rules)
   - 3.8 [Class Capacity Rules](#38-class-capacity-rules)
   - 3.9 [Certificate Rules](#39-certificate-rules)
   - 3.10 [Company Rules](#310-company-rules)
   - 3.11 [CSV Import Rules](#311-csv-import-rules)
   - 3.12 [Calendar Rules](#312-calendar-rules)
   - 3.13 [Reports Rules](#313-reports-rules)
   - 3.14 [Audit & Logging Rules](#314-audit--logging-rules)
   - 3.15 [User Administration Rules](#315-user-administration-rules)
4. [Domain Modeling Standards](#4-domain-modeling-standards)
5. [State Machines](#5-state-machines)
6. [Validation and Invariant Enforcement](#6-validation-and-invariant-enforcement)
7. [Testing Business Rules](#7-testing-business-rules)
8. [Business Rules Definition of Done](#8-business-rules-definition-of-done)

---

## 1. General Principles

| Principle                    | Mandatory Behavior                                                                                                      |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **Domain First**             | Business rules must be expressed in domain terms, not in infrastructure or framework concepts.                          |
| **Explicit Over Implicit**   | Every rule must be explicitly coded in a rule object. Hidden logic in Composables or ViewModels is a defect.            |
| **Single Source of Truth**   | Each rule has exactly one authoritative implementation — in its rule object. No duplication across layers.              |
| **Testability by Design**    | All rule objects (`EntityLifecycleRules`, `RelationshipRules`, etc.) are pure `object` with no dependencies — instantly testable. |
| **Isolation from Infrastructure** | Rule objects must not import Room, Firestore, Hilt, or Compose APIs.                                             |
| **Fail Deterministically**   | A violated business rule must throw a domain-meaningful exception or return a typed error — never a silent skip.        |
| **Auditability**             | Critical state changes (deactivation, RBAC denials) must be traceable in `RecentActivityEntity` or logs.               |

---

## 2. Business Rule Classification

| Type               | Example in Educalink                                                        |
| ------------------ | --------------------------------------------------------------------------- |
| **Constraint**     | A student's class must belong to their enrolled course (BR-REL-001)         |
| **Derivation**     | Attendance rate = (present + late) / (total − justified absences) (BR-MON-001) |
| **Invariant**      | An inactive company's data must remain scoped and not bleed to other tenants |
| **Authorization**  | Only `ADMINISTRADOR` may delete or reassign company access (BR-SEC-002)     |
| **State Transition** | `SchoolClass` can only advance: `Open → InProgress → Closed` (BR-LIF-003) |
| **Notification/Trigger** | Monitoring alert generated when attendance drops below 75% (BR-MON-002) |
| **Configuration**  | Attendance alert threshold (currently 75%) — should become externally configurable |

---

## 3. Rule Catalog

### 3.1 Student Rules

#### BR-STU-001 — Student Status Values

- **Classification:** Constraint
- **Description:** A student's `status` must be one of `Active`, `Inactive`, or `Graduated`.
- **Implementation:** `Student.status: StudentStatus` (sealed/enum in domain model).
- **Error Behavior:** Reject unknown status at import (CSV) and deserialization.

#### BR-STU-002 — Student Progress Range

- **Classification:** Constraint
- **Description:** `Student.progress` must be in range `[0, 100]`.
- **Implementation:** Validated at `AppRepository` write boundary.
- **Error Behavior:** Clamp to range or reject with `IllegalArgumentException`.

---

### 3.2 Student Monitoring Rules

**Implementation class:** `StudentMonitoringRules` (pure `object`)

#### BR-MON-001 — Attendance Rate Calculation

- **Classification:** Derivation
- **Formula:** `attendanceRate = (present + late) / (total − justifiedAbsences)`
- **Rules:**
  - `AttendanceStatus.JustifiedAbsence` is **excluded from the denominator** (not counted against the student).
  - `AttendanceStatus.Late` is counted as **present** in the numerator.
  - If `records` is empty OR all records are `JustifiedAbsence` (i.e., `baseCount == 0`), rate = `0f` and **no critical alert is generated** (see BR-MON-004).
- **Implementation:** `StudentMonitoringRules.attendanceRate(records: List<AttendanceRecord>): Float` — returns a value in `[0f, 1f]`. The current implementation returns `0f` for the empty / all-justified case; tests must assert `0f`, not `null`.

#### BR-MON-002 — Critical Attendance Alert

- **Classification:** Notification/Trigger
- **Description:** When attendance rate < **75%**, a `MonitoringAlert` with `CRITICAL` severity is generated.
- **Implementation:** `StudentMonitoringRules.buildAlerts(snapshot: StudentMonitoringSnapshot): List<MonitoringAlert>`
- **Effect:** Alert displayed in student monitoring detail screen and backoffice feed.

#### BR-MON-004 — No-Data Suppression

- **Classification:** Constraint
- **Description:** When there is no attendance data for the period (i.e., empty record list or all records are `JustifiedAbsence`), no critical alert is generated even though `attendanceRate()` returns `0f`. UI should display "Sem dados de frequência" instead of "0%".
- **Rationale:** A new student with no attendance history should not appear in the critical-attendance feed.

#### BR-MON-005 — Recurring Lateness Warning

- **Classification:** Notification/Trigger
- **Description:** When ≥ 3 `BehaviorRecord` entries in the recent window have `delayMinutes >= 10`, a `Warning`-level `MonitoringAlert` is generated.
- **Implementation:** `StudentMonitoringRules.buildAlerts` — already present in code; document here for traceability.

#### BR-MON-006 — Behavior Record Validation

- **Classification:** Constraint
- **Description:** A `BehaviorRecord` is rejected if any of the following hold:
  - `participationScore` is outside `1..5`.
  - `delayMinutes` is negative.
  - `grade` is non-null and outside `0f..StudentMonitoringRules.maximumGrade` (10f).
  - `activityDelivery == ActivityDeliveryStatus.Missing` and `note.isBlank()` (an unexcused missing delivery requires a note).
- **Implementation:** `StudentMonitoringRules.validateBehaviorRecord(record): List<String>` — returns the list of error messages; empty list means valid.

#### BR-MON-003 — Psychological Record Confidentiality

- **Classification:** Authorization
- **Description:** `PsychologicalNeedEntity` has a `confidentiality: ConfidentialityLevel` field. The enum has two levels:
  - `Restricted` — accessible only to `PSICOPEDAGOGO` and above.
  - `SharedSummary` — accessible to `ORIENTADOR_EDUCACIONAL` and above.
- **Implementation target:** Enforcement must happen in `ObserveStudentsUseCase` (and any repository read that returns `StudentMonitoringSnapshot`) by filtering records the requester's role is not authorized to see, in addition to RBAC checks via `AccessPolicy`.
- **Error Behavior:** Records the requester is not authorized to see are filtered out of the returned `Flow<...>` (silent filter). Direct write attempts (`upsertPsychologicalNeed`) for a role lacking the permission throw `SecurityException`.
- **Known gap (2026-05-07):** `ConfidentialityLevel`-based filtering is **not yet enforced** anywhere in `ObserveStudentsUseCase` or `AppRepository`. See `TASKS.md`.

---

### 3.3 Entity Lifecycle Rules

**Implementation class:** `EntityLifecycleRules` (pure `object`)

#### BR-LIF-001 — Student Deactivation

- **Classification:** State Transition
- **Description:** A student can be deactivated (→ `Inactive`) or reactivated (→ `Active`). Setting to `Graduated` is terminal.
- **Implementation:** `EntityLifecycleRules.deactivateStudent(student: Student): Student` returns `student.copy(status = StudentStatus.Inactive)`.
- **Invariant:** `Graduated` students cannot be reactivated.

#### BR-LIF-002 — Course Deactivation

- **Classification:** State Transition
- **Description:** A course can be deactivated (unpublished) via `EntityLifecycleRules.deactivateCourse`.
- **Invariant:** A deactivated course with enrolled students must notify the caller — do not silently remove students.

#### BR-LIF-003 — School Class Lifecycle

- **Classification:** State Transition
- **Transitions:** `Open → InProgress → Closed` (forward-only).
- **Implementation:** `EntityLifecycleRules` — guard against backward transitions (e.g., `Closed → Open` is invalid).

#### BR-LIF-004 — Teacher Deactivation

- **Classification:** State Transition
- **Description:** A teacher can be deactivated/reactivated.
- **Invariant:** A deactivated teacher must not appear in instructor dropdown selectors.

---

### 3.4 Relationship Rules

**Implementation class:** `RelationshipRules` (pure `object`)

#### BR-REL-001 — Student-Course-Class Consistency

- **Classification:** Constraint / Invariant
- **Description:** A student's `enrolledClass` must belong to their `course`. The class and course must belong to the same `companyId`.
- **Implementation:** `RelationshipRules.validateStudentLinks(student, course, schoolClass): Boolean`
- **Error Behavior:** Reject creation/update with `ValidationException` if constraint is violated.

#### BR-REL-002 — Real Enrollment Counts

- **Classification:** Derivation
- **Description:** `Course.studentsCount` and `SchoolClass.studentsCount` are **not stored as denormalized values** — they are recomputed from actual `Student` records.
- **Implementation:**
  - `RelationshipRules.realStudentsByCourse(students, courseId): Int`
  - `RelationshipRules.realStudentsByClass(students, classId): Int`
- **Rule:** Never persist a hardcoded count; always derive from the student list.

#### BR-REL-003 — Teacher Student Count

- **Classification:** Derivation
- **Description:** `Teacher.totalStudents` is computed from actual student records linked to courses assigned to that teacher.
- **Implementation:** `RelationshipRules.realStudentsByTeacher(students, teacherId): Int`

---

### 3.5 Notification Rules

**Implementation class:** `RealtimeNotificationRules` (pure `object`)

#### BR-NOT-001 — Backoffice Notification Recipients

- **Classification:** Authorization
- **Description:** Real-time activity feed notifications are sent only to users with role `ADMINISTRADOR` or `COORDENADOR`. Lower-privileged roles do not receive backoffice activity updates.
- **Implementation:** `RealtimeNotificationRules.shouldNotify(role: UserRole): Boolean`

#### BR-NOT-002 — Recent Activity Draft

- **Classification:** Derivation
- **Description:** A `RecentActivity` draft is built from any entity create/update/deactivation event.
- **Implementation:** `RealtimeNotificationRules.buildActivityDraft(entity, action): RecentActivityEntity`

---

### 3.6 Authorization Rules (RBAC)

**Implementation class:** `AccessPolicy` (pure `object`)

#### BR-SEC-001 — Role Hierarchy

Roles in ascending privilege order:

```
RESPONSAVEL < PROFESSOR < ORIENTADOR_EDUCACIONAL < PSICOPEDAGOGO < COORDENADOR < ADMINISTRADOR
```

#### BR-SEC-002 — Admin-Only Operations

The following operations require role `ADMINISTRADOR`:

- `upsertCompanyForAdmin` — create/update company
- `deleteCompanyForAdmin` — delete company
- `grantUserCompanyAccess` — grant user access to a company
- `revokeUserCompanyAccess` — revoke user access
- `ClearStorage`, `ResetDatabase` settings actions

**Implementation:** `AppRepository` throws `SecurityException` when `requester?.role != ADMINISTRADOR`.

#### BR-SEC-003 — RBAC Matrix

| Resource              | RESPONSAVEL | PROFESSOR | ORIENTADOR | PSICOPEDAGOGO | COORDENADOR | ADMINISTRADOR |
| --------------------- | :---------: | :-------: | :--------: | :-----------: | :---------: | :-----------: |
| Students — View       | own child   | ✓         | ✓          | ✓             | ✓           | ✓             |
| Students — Create     | ✗           | ✗         | ✓          | ✓             | ✓           | ✓             |
| Courses — View        | ✓           | ✓         | ✓          | ✓             | ✓           | ✓             |
| Teachers — Create     | ✗           | ✗         | ✗          | ✗             | ✓           | ✓             |
| Users — Manage        | ✗           | ✗         | ✗          | ✗             | ✗           | ✓             |
| Companies — Manage    | ✗           | ✗         | ✗          | ✗             | ✗           | ✓             |
| Settings — ResetDB    | ✗           | ✗         | ✗          | ✗             | ✗           | ✓             |
| StudentMonitoring     | own child   | ✓         | ✓          | ✓             | ✓           | ✓             |
| Psychological Records | ✗           | ✗         | ✗          | ✓             | ✓           | ✓             |

Full matrix is the authoritative implementation in `AccessPolicy.can(role, resource, action)`.

#### BR-SEC-004 — Guardian Access (RESPONSAVEL)

A user with role `RESPONSAVEL` may only view students linked to them via `GuardianStudentEntity`. `ObserveStudentsUseCase` filters to linked student IDs for this role.

---

### 3.7 Sync Rules

**Implementation class:** `FirebaseScreenSyncPlanner`

#### BR-SYNC-001 — Screen-Scoped Least-Privilege Sync

- **Classification:** Configuration
- **Description:** Each screen must only pull the minimum Firestore collections it needs. This minimizes bandwidth and reduces data exposure.
- **Implementation:** `FirebaseScreenSyncPlanner` maps `SyncScope` enum values to sets of Firestore collection names.
- **Rule:** Every new screen must declare its `SyncScope` in `FirebaseScreenSyncPlanner`.

#### BR-SYNC-002 — Offline Write Queue & Conflict Resolution

- **Classification:** Configuration / Invariant
- **Description:** Writes performed while `ConnectivityObserver.Status != Available` must be persisted to Room immediately and queued for Firestore push. On reconnect, queued writes are flushed in FIFO order.
- **Conflict policy:**
  - On read sync (Firestore → Room), Firestore is canonical for fields not yet locally modified.
  - On write sync (Room → Firestore), the local write wins if `local.updatedAt > remote.updatedAt`; otherwise the remote value is preserved and the conflict is recorded as a `RecentActivityEntity` of type `SYNC_CONFLICT`.
- **Implementation:** `AppRepository` write methods + a future `OfflineWriteQueueDao`.

---

### 3.8 Class Capacity Rules

**Implementation class:** `RelationshipRules`

#### BR-CLS-001 — Class Capacity Enforcement

- **Classification:** Invariant
- **Description:** A `SchoolClass` may not enroll more than `maxCapacity` students. Attempting to link a student to a class where `realStudentsByClass(students, classId) >= class.maxCapacity` must reject the operation with a domain exception.
- **Implementation:** `RelationshipRules.canEnrollStudent(students, schoolClass): Boolean` (to be added) called by `AppRepository.upsertStudent`.
- **Error Behavior:** `CapacityExceededException("Turma ${class.name} atingiu a capacidade máxima.")`.

#### BR-CLS-002 — Closed Classes Reject Enrollment

- **Classification:** State Transition / Invariant
- **Description:** A student may not be linked to a `SchoolClass` whose `status == ClassStatus.Closed`.
- **Implementation:** `RelationshipRules.validateStudentLinks` extended to also check `class.status != Closed`.
- **Error Behavior:** `ValidationException` with a domain message.

---

### 3.9 Certificate Rules

**Implementation class:** `CertificateRules` (new pure `object`, to be added to `:core`)

#### BR-CRT-001 — Certificate Code Uniqueness

- **Classification:** Invariant
- **Description:** `Certificate.code` must be non-blank and unique per `companyId`.
- **Implementation:** Validated at `AppRepository.upsertCertificate` write boundary against existing rows for the same `companyId`.
- **Error Behavior:** `ValidationException("Código de certificado duplicado.")`.

#### BR-CRT-002 — Certificate Hours Bounds

- **Classification:** Constraint
- **Description:** `Certificate.hours` must satisfy `1 <= hours <= course.durationHours` for the linked course.
- **Implementation:** `CertificateRules.validate(certificate, course): List<String>`.

#### BR-CRT-003 — Certificate Issue Eligibility

- **Classification:** Invariant
- **Description:** A certificate may only be issued for a `Student` whose `status == Graduated` (or, alternatively, `progress == 100` for non-graduated continuing-education tracks) and a `Course` that has `isPublished == true`.
- **Implementation:** `CertificateRules.canIssue(student, course): Boolean`.
- **Error Behavior:** Reject with `ValidationException` listing the reasons.

#### BR-CRT-004 — Certificate Immutability

- **Classification:** Invariant
- **Description:** Once issued, a `Certificate`'s fields are read-only. The only mutable transition is `revoke()` (admin-only), which flips a future `isRevoked` flag and produces a `RecentActivityEntity` audit record.

---

### 3.10 Company Rules

#### BR-CMP-001 — CNPJ Validation

- **Classification:** Constraint
- **Description:** `Company.cnpj` must either be the empty string (placeholder/demo entries) OR a 14-digit string passing the standard CNPJ mod-11 checksum.
- **Implementation:** `CompanyRules.validateCnpj(cnpj: String): Boolean` (new pure `object` in `:core`).
- **Error Behavior:** Reject `upsertCompanyForAdmin` with `ValidationException`.

#### BR-CMP-002 — Inactive Company Lockout

- **Classification:** Authorization
- **Description:** When `Company.isActive == false`, no user may select that company in the company switcher and any active session pinned to it must be re-routed to `COMPANY_SELECTOR`.
- **Implementation:** `AuthManager.setActiveCompany` rejects inactive companies; `FirebaseDataConnectService` filters inactive companies from the user-companies list (already present at `FirebaseDataConnectService.kt:1590`).

---

### 3.11 CSV Import Rules

**Implementation class:** `CsvImportViewModel` + per-importer `*Validator` objects

#### BR-CSV-001 — Header Schema Match

- **Classification:** Constraint
- **Description:** The first row of the CSV must match the importer's expected header exactly (case-insensitive, whitespace-trimmed). Mismatch aborts the import without writing any row.

#### BR-CSV-002 — Per-Row Validation Pipeline

- **Classification:** Constraint
- **Description:** Each row is validated against the relevant rules (e.g., students against BR-STU-001, BR-STU-002, BR-REL-001) **before** any persistence call. The validation pipeline produces a per-row report (`row index`, `errors`).

#### BR-CSV-003 — All-or-Nothing Persistence

- **Classification:** Invariant
- **Description:** If any row fails validation, **no** rows are persisted. The user is shown the validation summary and must fix the file before retrying. Partial commits are forbidden.
- **Implementation:** Use a single Room `@Transaction` for the import.

#### BR-CSV-004 — Sensitive Field Exclusion

- **Classification:** Authorization
- **Description:** CSV import never accepts psychological or behavioral monitoring fields. Only public student/course/class fields are importable.

---

### 3.12 Calendar Rules

#### BR-CAL-001 — Event Date Bounds

- **Classification:** Constraint
- **Description:** `CalendarEvent.date` must be ≥ today − 30 days (warn on past events) and ≤ today + 5 years (reject far-future events).
- **Implementation:** `CalendarEventRules.validate(event, today): List<String>` (new pure `object`).

#### BR-CAL-002 — Event Type Whitelist

- **Classification:** Constraint
- **Description:** `CalendarEvent.type` must be a valid `EventType` enum entry. Deserialization failures fall back to `EventType.Other` and emit a warning log.

---

### 3.13 Reports Rules

#### BR-RPT-001 — KPI Freshness

- **Classification:** Configuration
- **Description:** KPI panels (enrollment count, completion rate, attendance rate distribution) must reflect data ≤ 24h old. Each panel exposes a `lastUpdatedAt` timestamp; if the panel data is staler than the threshold, the panel shows a "Sincronize para atualizar" inline banner.
- **Implementation:** `ReportsViewModel` exposes a `StateFlow<KpiFreshness>` per panel.

#### BR-RPT-002 — Aggregation Tenancy

- **Classification:** Invariant
- **Description:** All aggregations are computed from records filtered by the requester's `companyId`. No aggregation may cross tenants. Admin-level cross-company reports use a separate explicit aggregator that includes tenant identity in every row.

---

### 3.14 Audit & Logging Rules

#### BR-AUD-001 — Security-Sensitive Audit Trail

- **Classification:** Invariant
- **Description:** Every login (success and failure), role change, entity deactivation, RBAC denial (`SecurityException` raised by `AppRepository`), and admin-only operation (BR-SEC-002) must produce a `RecentActivityEntity` with redacted PII (no emails, no full names — use Firebase UID and entity IDs).
- **Implementation:** `RealtimeNotificationRules.buildActivityDraft` extended with action types `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `RBAC_DENY`, `ADMIN_OP`. `AppRepository` calls it from each guarded write path.

#### BR-LOG-001 — PII Redaction in Logs

- **Classification:** Invariant
- **Description:** Log statements must never include: student names, emails, CPF, monitoring/behavioral/psychological data, raw passwords or tokens, `BuildConfig` secret fields. Logs identify users by Firebase UID.
- **Implementation:** A small helper `LogSafe.redact(value: String): String` is mandatory for any string passed to `Log.*` that may contain PII; CI lint rule should fail on direct `Log.*("$student.name")` patterns.

#### BR-PSY-001 — Psychological Data Retention

- **Classification:** Configuration
- **Description:** Records of `ConfidentialityLevel.Restricted` remain accessible while the linked `Student.status == Active`. Once the student is deactivated, the records remain readable only to `COORDENADOR`/`ADMINISTRADOR`, are excluded from all reports/exports, and the student is filtered from the `RESPONSAVEL` view (BR-SEC-004).
- **Implementation:** `ObserveStudentsUseCase` enforces both the role filter (BR-MON-003) and the status-after-deactivation filter.

#### BR-NOT-003 — Recent Activity Retention

- **Classification:** Configuration
- **Description:** The `recent_activities` feed retains at most **90 days** of events per `companyId`. A startup pruning job (`RecentActivitiesPruner`) deletes older rows. The pruning itself produces a `RecentActivityEntity` of type `RETENTION_PURGE` with the deleted row count.

---

### 3.15 User Administration Rules

#### BR-USR-001 — User Email Uniqueness

- **Classification:** Invariant
- **Description:** `AppUser.email` must be unique across the platform. Firebase Auth enforces this as the primary check; `AppRepository.upsertUser` validates the same constraint against the local cache before write.

#### BR-USR-002 — Role Assignment

- **Classification:** Authorization
- **Description:** Only `ADMINISTRADOR` can assign or change a `UserRole`. A user without an entry in `UserCompanyEntity` for at least one active company must not be allowed past the company-selector screen.
- **Implementation:** `AccessPolicy.can(role, Users, Update)` and the navigation guard in `AppNavHost`.

---

## 4. Domain Modeling Standards

- All domain models are **immutable `data class`** with `@Immutable` annotation.
- Domain models must not import Android, Room, Firestore, Hilt, or Compose types.
- ID fields use `Int` (Room auto-increment) aligned with Firestore document integer IDs.
- `companyId: Int` is mandatory on all business entities (not on `Company` itself, `AppUser`, or junction tables).
- Enums (e.g., `StudentStatus`, `EventType`, `ConfidentialityLevel`) live in the domain model package.

---

## 5. State Machines

### Student Status

```
           deactivate()          graduate()
Active ─────────────────► Inactive    Active ──────────────► Graduated
   ▲                          │                                (terminal)
   └──────── activate() ───────┘
```

### SchoolClass Status

```
Open ──── start() ──── InProgress ──── close() ──── Closed (terminal)
```

### Course Publication

```
Draft (isPublished=false) ──── publish() ──── Published (isPublished=true)
```

---

## 6. Validation and Invariant Enforcement

- Validate at `AppRepository` write boundary — never in Composable screens.
- For CSV import, validate at `CsvImportViewModel` before calling repository.
- `student.progress` ∈ [0, 100]
- `teacher.rating` ∈ [0.0, 5.0]
- `certificate.code` must be non-blank and unique per company.
- All `companyId` references must match the requester's authorized company.
- Student-Course-Class link must satisfy `BR-REL-001`.

---

## 7. Testing Business Rules

All rule objects must have corresponding unit tests in `app/src/test/`:

| Rule Object                   | Test Class                        |
| ----------------------------- | --------------------------------- |
| `StudentMonitoringRules`      | `StudentMonitoringRulesTest`      |
| `EntityLifecycleRules`        | `EntityLifecycleRulesTest`        |
| `RelationshipRules`           | `RelationshipRulesTest`           |
| `RealtimeNotificationRules`   | `RealtimeNotificationRulesTest`   |
| `AccessPolicy`                | `AccessPolicyTest`                |

Tests must cover:
- All happy-path rule applications.
- All invariant violations (with expected exception types).
- All role-boundary cases in `AccessPolicyTest` (e.g., `COORDENADOR` cannot manage companies).
- Attendance rate edge cases: all absent, all justified, all late.

---

## 8. Business Rules Definition of Done

A business rule implementation is done when:

- [ ] The rule is implemented exclusively in the appropriate rule object (not in ViewModel or Composable).
- [ ] The rule is covered by at least one unit test in `app/src/test/`.
- [ ] All state transitions go through `EntityLifecycleRules.copy()` — no direct field mutation.
- [ ] RBAC checks call `AccessPolicy.can()` — no ad-hoc role comparisons.
- [ ] Derived values (enrollment counts, attendance rate) are computed by rule objects — not stored as stale denormalized data.
- [ ] New resources or actions are registered in `AccessPolicy`.
- [ ] New sync scopes are registered in `FirebaseScreenSyncPlanner`.
