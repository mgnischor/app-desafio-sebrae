# Task Checklist — Educalink (app-desafio-sebrae)

This file is the **single source of truth for task progress** during active development.

## Checkbox Legend

| Marker | Meaning     |
| ------ | ----------- |
| `[ ]`  | Not started |
| `[~]`  | In progress |
| `[x]`  | Completed   |

> **Rules:**
>
> - Mark a task `[~]` before starting it.
> - Mark a task `[x]` immediately after completing it.
> - Only one task should be `[~]` at a time unless tasks are truly parallel.
> - Do not alter task descriptions — only change the checkbox marker.
> - If a task is blocked, leave it unchecked and append an inline note: `<!-- BLOCKED: reason -->`.

---

## Active Sprint

### Architecture

- [x] Define module boundaries (`:app`, `:core`, `:feature:*`)
- [x] Implement Clean Architecture + MVVM + Repository Pattern
- [x] Implement unidirectional data flow: Firestore → Room → AppRepository → StateFlow → Composable
- [ ] Validate dependency flow: no `:feature:*` module imports another `:feature:*`
- [ ] Validate no business logic in Composable screens
- [ ] Register all sync scopes in `FirebaseScreenSyncPlanner`
- [ ] Document Context Map for all bounded contexts (feature modules)

### Business Rules

- [x] Implement `StudentMonitoringRules` (attendance rate, monitoring alerts)
- [x] Implement `EntityLifecycleRules` (activate/deactivate for all entity types)
- [x] Implement `RelationshipRules` (student-course-class consistency, real counts)
- [x] Implement `RealtimeNotificationRules` (notification drafts + role filter)
- [x] Implement `AccessPolicy` RBAC (6 roles × 9 resources × 13 actions)
- [ ] Make attendance alert threshold (75%) externally configurable
- [ ] Add `ConfidentialityLevel` enforcement to `ObserveStudentsUseCase` for psychological records (BR-MON-003)
- [ ] Validate that `Course.isPublished` transition is one-way (no unpublish after publish)
- [ ] Guard `SchoolClass.status` against backward transitions in `EntityLifecycleRules`
- [ ] Implement `CertificateRules` object covering BR-CRT-001/002/003/004
- [ ] Implement `CompanyRules.validateCnpj` (BR-CMP-001) and inactive-company lockout in `AuthManager` (BR-CMP-002)
- [ ] Extend `RelationshipRules` with `canEnrollStudent` capacity guard (BR-CLS-001) and closed-class block (BR-CLS-002)
- [ ] Implement `CalendarEventRules.validate` (BR-CAL-001/002)
- [ ] Implement offline write queue + conflict resolution policy (BR-SYNC-002)
- [ ] Implement `RecentActivitiesPruner` for 90-day retention (BR-NOT-003)
- [ ] Implement `LogSafe.redact` helper and audit `Log.*` callsites for PII (BR-LOG-001)
- [ ] Extend `RealtimeNotificationRules` with audit-event types `LOGIN_SUCCESS`/`LOGIN_FAILURE`/`RBAC_DENY`/`ADMIN_OP` (BR-AUD-001)

### Code Quality

- [x] All ViewModels use `@HiltViewModel` and expose `StateFlow`
- [x] All domain models are `@Immutable data class`
- [x] KSP used for code generation (no KAPT)
- [ ] Achieve ≥ 80% line coverage on `:core` domain and data layers
- [ ] Achieve ≥ 70% branch coverage on rule objects
- [ ] Resolve all HIGH-severity Android Lint warnings
- [ ] Replace `!!` operator usages with safe alternatives throughout codebase
- [ ] Ensure all user-facing strings are in `strings.xml` (PT-BR, EN, ES)

### Database

- [x] Room entities defined with `companyId` on all business entities
- [x] All `@Query` methods use parameterized `:companyId` filter
- [x] `MIGRATION_9_10` implemented (current schema version: 10)
- [ ] Guard `fallbackToDestructiveMigration(true)` so it only runs on debug build variant <!-- BLOCKED: currently unconditional in app/.../di/AppModule.kt:63 -->
- [ ] Enable `exportSchema = true` on `@Database` and commit `app/schemas/` to the repository <!-- BLOCKED: AppDatabase.kt:847 currently sets exportSchema = false -->
- [ ] Add `createdAt` and `updatedAt` fields to new entities going forward
- [ ] Write `MigrationTestHelper` tests for `MIGRATION_9_10`
- [ ] Version-control `firestore.rules` and `firestore.indexes.json`

### Security

- [x] Firebase Auth implemented as primary authentication
- [x] `AccessPolicy` RBAC guards all write operations in `AppRepository` (admin ops at AppRepository.kt:180/194/222/240)
- [x] AES-256-GCM via Android Keystore implemented in `FirebaseSeedCredentialStore`
- [x] `companyId` filtering on all `AppDao` queries
- [x] Seed credentials loaded from `local.properties` via `BuildConfig` (not hardcoded)
- [ ] Guard local credential fallback in `AuthManager` with `BuildConfig.DEBUG` (or disable when `FIREBASE_SEED_CREDENTIALS_CONFIGURED == false`) <!-- BLOCKED: no guard in current AuthManager -->
- [ ] Add `network_security_config.xml` to deny cleartext traffic in release builds and reference it from `AndroidManifest.xml` (`android:networkSecurityConfig`)
- [ ] Implement `ConfidentialityLevel` filtering in `ObserveStudentsUseCase` and any read path returning `StudentMonitoringSnapshot` (BR-MON-003)
- [ ] Populate `backup_rules.xml` and `data_extraction_rules.xml` to exclude `sebrae_local.db` from Android backup
- [ ] Set `MainActivity` `android:exported` rationale comment in manifest (launcher only)
- [ ] Add a release `signingConfig` and wire it to GitHub Actions secrets (currently no release signing config in `app/build.gradle.kts`)
- [ ] Run `./gradlew dependencyCheckAnalyze` and resolve any critical CVEs

### Tests

- [x] `StudentMonitoringRulesTest` — attendance rate + alert generation
- [x] `AccessPolicyTest` — RBAC matrix coverage
- [x] `AuthManagerTest` — auth flow behavior
- [x] `AppRepositoryRecentActivitiesTest` — paged activities mapping
- [x] `RelationshipRulesTest` — student counts + link validation
- [x] `EntityLifecycleRulesTest` — activate/deactivate all entities
- [x] `RealtimeNotificationRulesTest` — notification draft + role filter
- [x] `CourseAndTeacherModelTest` — model rules
- [x] `StudentModelTest` — student model validation
- [x] `FirebaseScreenSyncPlannerTest` — screen → sync task mapping
- [x] `RemoteBootstrapperTest` — NoOp bootstrapper contract
- [ ] Add `AppDaoIntegrationTest` using in-memory Room database
- [ ] Add `MigrationTest` for `MIGRATION_9_10`
- [ ] Add ViewModel unit tests for `StudentsViewModel`, `CoursesViewModel`, `LoginViewModel`
- [ ] Add `ObserveStudentsUseCaseTest` covering guardian role filter (BR-SEC-004) and `ConfidentialityLevel` filter (BR-MON-003)
- [ ] Add unit tests for new rule objects: `CertificateRules`, `CompanyRules`, `CalendarEventRules`, capacity guard in `RelationshipRules` (BR-CLS-001/002, BR-CRT-001/002/003/004, BR-CMP-001/002, BR-CAL-001/002)
- [ ] Add Compose UI test for empty state on students list screen
- [ ] Add Compose UI test verifying RBAC-hidden FAB for `RESPONSAVEL` role

### UI / UX

- [x] Shared components implemented: `DetailScaffold`, `EmptyState`, `LoadingOverlay`, `OfflineBanner`, `StatusChip`, `SearchableDropdown`, `ListSearchHeader`
- [x] `OfflineBanner` connected to `ConnectivityObserver`
- [x] Dark mode support via `AppSettingsEntity.darkMode`
- [ ] Verify `contentDescription` on all icon-only buttons across all feature screens
- [ ] Verify loading + empty + content states on all list screens
- [ ] Add confirmation dialog for student deactivation, class closure, and DB reset actions
- [ ] Verify all form validation errors display inline (not as toasts)
- [ ] Audit string resources: confirm all user-facing strings are in `strings.xml` for PT-BR, EN, ES
- [ ] Verify psychological records are hidden in UI for roles below `PSICOPEDAGOGO`

### Infrastructure / Build

- [x] `libs.versions.toml` with pinned versions for all dependencies
- [x] `local.properties` excluded from version control
- [x] ProGuard/R8 enabled for release builds
- [ ] Set up GitHub Actions `ci.yml` (lint, test, dependencyCheck, assembleDebug on PR)
- [ ] Set up GitHub Actions `release.yml` (bundleRelease, sign, upload artifact on main)
- [ ] Configure Gradle wrapper validation in CI (`gradle/wrapper-validation-action`)
- [ ] Add `firestore.rules` and `firestore.indexes.json` to repository
- [ ] Configure ProGuard mapping file upload to Firebase Crashlytics in release pipeline

---

## Backlog

<!-- Add future tasks here using: - [ ] Task description -->

- [ ] Split `AppDao` into domain-specific DAO interfaces when entity count exceeds 25
- [ ] Migrate to type-safe Navigation Compose when the library supports the current nav version
- [ ] Add CSV export feature (certificates, student progress reports)
- [ ] Implement push notifications via Firebase Cloud Messaging
- [ ] Add Firestore offline persistence configuration (`FirebaseFirestoreSettings`)
- [ ] Evaluate migrating to Firebase Data Connect when GA for Android

---
