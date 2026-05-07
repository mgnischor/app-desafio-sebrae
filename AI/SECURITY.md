# Security Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern all security controls in the Educalink Android application. Security is a non-negotiable quality attribute and must be addressed from the first line of code. This document supersedes any conflicting guidance in other AI files for security-specific topics.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [OWASP Mobile Top 10 Compliance](#2-owasp-mobile-top-10-compliance)
3. [Authentication](#3-authentication)
4. [RBAC Authorization](#4-rbac-authorization)
5. [Cryptography Standards](#5-cryptography-standards)
6. [Secrets and Sensitive Data Management](#6-secrets-and-sensitive-data-management)
7. [Multi-Tenancy Security](#7-multi-tenancy-security)
8. [Data Privacy and Sensitive Records](#8-data-privacy-and-sensitive-records)
9. [Android Platform Security](#9-android-platform-security)
10. [Dependency and Supply Chain Security](#10-dependency-and-supply-chain-security)
11. [Logging and Monitoring](#11-logging-and-monitoring)
12. [Security Definition of Done](#12-security-definition-of-done)

---

## 1. General Principles

| Principle               | Mandatory Behavior                                                                                                                  |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| **Least Privilege**     | Every role receives the minimum permissions needed. `AccessPolicy.can(role, resource, action)` is the authoritative check.          |
| **Fail Securely**       | When `AccessPolicy.can()` returns `false`, the operation is rejected with `SecurityException` — never silently allowed.             |
| **Defense in Depth**    | RBAC in `AccessPolicy` + `companyId` filtering in `AppRepository` + Firebase Auth = three independent access barriers.              |
| **Zero Trust**          | Every write to `AppRepository` validates the requester's role regardless of who made the call.                                      |
| **Secure by Default**   | Firebase Auth is the only production authentication path. Local SHA-256 fallback is explicitly dev/demo only and must be disabled in release builds. |
| **Immutable Audit Trail** | Security-relevant events (login, deactivation, company access change) must produce a `RecentActivityEntity` record.               |

---

## 2. OWASP Mobile Top 10 Compliance

| #   | Risk                                       | Mitigation in Educalink                                                                                                            |
| --- | ------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------- |
| M1  | Improper Credential Usage                  | Seed credentials stored encrypted with AES-256-GCM via Android Keystore (`FirebaseSeedCredentialStore`). Never in plaintext.      |
| M2  | Inadequate Supply Chain Security           | All dependencies pinned in `libs.versions.toml`. Run `dependencyCheckAnalyze` task for CVE scanning on every PR.                  |
| M3  | Insecure Authentication / Authorization    | Firebase Auth (production). `AccessPolicy` + RBAC guards all write operations. No client-supplied roles accepted.                  |
| M4  | Insufficient Input/Output Validation       | All repository write parameters validated before Room/Firestore operations. CSV import validated in `CsvImportViewModel`.          |
| M5  | Insecure Communication                     | Firebase SDK enforces TLS for all Firestore and Auth traffic. No plain HTTP connections permitted.                                 |
| M6  | Inadequate Privacy Controls                | `PsychologicalNeedEntity` has `ConfidentialityLevel`; access filtered by `AccessPolicy`. No PII in logs.                          |
| M7  | Insufficient Binary Protections            | ProGuard/R8 enabled for release builds (`proguard-rules.pro`). Minification and obfuscation active.                                |
| M8  | Security Misconfiguration                  | `fallbackToDestructiveMigration` only in debug builds. No debug flags in release. `google-services.json` never contains private keys. |
| M9  | Insecure Data Storage                      | Room DB stored in app-private storage (not external). Android Keystore for cryptographic keys. No sensitive data in SharedPreferences without encryption. |
| M10 | Insufficient Cryptography                  | AES-256-GCM via Android Keystore for credential storage. SHA-256 for non-sensitive hashing only.                                  |

---

## 3. Authentication

### 3.1 Primary Authentication — Firebase Auth

- Firebase Auth (email + password) is the **only authentication path for production**.
- `AuthManager.loginWithFirebase()` handles Firebase Auth flow.
- On successful login, `AppUser` is resolved from the `app_users` Firestore collection / Room cache.
- Session is managed by Firebase Auth — the Firebase ID token is refreshed automatically.
- Logout clears the in-memory session and navigates to the login screen, clearing the back stack.

### 3.2 Local Fallback Authentication — DEV/DEMO ONLY

> **CRITICAL:** The SHA-256 local credential fallback in `AuthManager` is **insecure for production**. It must be:
> - Disabled via a feature flag or removed entirely before any production release.
> - Never seeded with credentials matching real user accounts.
> - Guarded with a `BuildConfig.DEBUG` check at minimum.

The seed credentials are loaded from `local.properties` (git-ignored) and injected via `BuildConfig` fields. This flow must not be present in release builds.

### 3.3 Session Security

- After logout, all in-memory state (ViewModels, StateFlows) must be cleared.
- The Navigation back stack must be cleared on logout to prevent navigation back to authenticated screens.
- Firebase Auth tokens must not be logged or stored outside the Firebase SDK.

---

## 4. RBAC Authorization

### 4.1 AccessPolicy — Single Enforcement Point

`AccessPolicy.can(role: UserRole, resource: Resource, action: Action): Boolean` is the **single, authoritative authorization check** for the entire application.

- Every repository write method that requires elevated privilege must call `AccessPolicy.can()`.
- No ad-hoc role comparisons (`if (role == ADMINISTRADOR)`) outside of `AccessPolicy`.
- `AccessPolicy` is a pure `object` — no infrastructure dependencies.
- Any change to permissions must be reflected in `AccessPolicy`, `AccessPolicyTest`, and `AI/BUSINESS.md`.

### 4.2 Role Hierarchy

```
RESPONSAVEL < PROFESSOR < ORIENTADOR_EDUCACIONAL < PSICOPEDAGOGO < COORDENADOR < ADMINISTRADOR
```

Higher roles implicitly inherit lower-role permissions within their scope.

### 4.3 Admin-Exclusive Operations

The following operations immediately throw `SecurityException` if `requester?.role != ADMINISTRADOR`:

| Operation                      | Method in AppRepository            |
| ------------------------------ | ---------------------------------- |
| Create/update company          | `upsertCompanyForAdmin`            |
| Delete company                 | `deleteCompanyForAdmin`            |
| Grant user company access      | `grantUserCompanyAccess`           |
| Revoke user company access     | `revokeUserCompanyAccess`          |
| Reset database                 | Settings — `ResetDatabase` action  |
| Clear local storage            | Settings — `ClearStorage` action   |

### 4.4 Guardian (RESPONSAVEL) Scoping

Users with role `RESPONSAVEL` can only access students linked via `GuardianStudentEntity`. `ObserveStudentsUseCase` enforces this filter — it is not optional.

---

## 5. Cryptography Standards

### 5.1 Credential Storage — Android Keystore + AES-256-GCM

`FirebaseSeedCredentialStore` encrypts seed credentials using:
- **Algorithm:** AES-256-GCM
- **Key storage:** Android Keystore (hardware-backed when available)
- **Key purpose:** `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`
- **IV:** randomly generated per encryption, stored alongside the ciphertext

```kotlin
// Mandatory pattern
val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
keyGenerator.init(
    KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
)
```

### 5.2 General Hashing

- `SHA-256` is used only for the dev-only credential fallback in `AuthManager`.
- For production password-equivalent operations, use Firebase Auth (which handles hashing server-side with bcrypt).
- Never store raw passwords or derive cryptographic keys from passwords without proper KDF.

### 5.3 Forbidden Cryptography

| Forbidden              | Reason                                          | Use Instead         |
| ---------------------- | ----------------------------------------------- | ------------------- |
| `MD5`                  | Cryptographically broken                        | SHA-256 or higher   |
| `SHA-1`                | Collision-vulnerable                            | SHA-256 or higher   |
| `AES-ECB`              | No IV, deterministic — leaks patterns           | AES-256-GCM         |
| `DES` / `3DES`         | Deprecated, weak key size                       | AES-256-GCM         |
| Hardcoded AES keys     | Key exposure in APK                             | Android Keystore    |

---

## 6. Secrets and Sensitive Data Management

### 6.1 Secret Sources

| Secret Type                   | Storage Location              | Access Mechanism        |
| ----------------------------- | ----------------------------- | ----------------------- |
| Firebase configuration        | `app/google-services.json`    | Firebase SDK (no API keys in code) |
| Seed user credentials (dev)   | `local.properties` (git-ignored) | `BuildConfig` fields injected at build time (see naming convention below) |
| AES-256-GCM encryption keys   | Android Keystore              | `FirebaseSeedCredentialStore` |
| Firebase Auth credentials     | Firebase SDK (in-memory)      | `AuthManager` |

### 6.2 Rules

- `local.properties` must be in `.gitignore` — never committed.
- `google-services.json` must not contain any private service account keys.
- **`BuildConfig` field naming convention.** Build-time secrets injected from `local.properties` must follow the `FIREBASE_*` prefix when they relate to the Firebase seed flow, and use a domain-prefixed name otherwise (e.g., a future SAML seed would use `SAML_*`). Generic `BUILD_*` is reserved for non-secret build metadata.
- **Current `local.properties` keys → BuildConfig fields:**

  | `local.properties` key       | BuildConfig field (`String`/`boolean`)         |
  | ---------------------------- | ----------------------------------------------- |
  | `firebase.seed.email`        | `FIREBASE_SEED_EMAIL` (String)                  |
  | `firebase.seed.password`     | `FIREBASE_SEED_PASSWORD` (String)               |
  | _(derived)_                  | `FIREBASE_SEED_CREDENTIALS_CONFIGURED` (boolean) — `true` only when both seed values are non-blank |
  | _(static)_                   | `FIREBASE_REMOTE_BOOTSTRAP_ENABLED` (boolean) — currently hardcoded `true` |

- If `local.properties` is absent (CI build), seed credential `BuildConfig` fields default to empty strings and `FIREBASE_SEED_CREDENTIALS_CONFIGURED` is `false` — the local fallback must not activate.
- Never hardcode API keys, tokens, or credentials in Kotlin source files, string resources, or XML files.

---

## 7. Multi-Tenancy Security

- `companyId` is always resolved from the authenticated `AppUser` — never accepted as a parameter from UI/client code.
- Every `AppDao` query that returns business data must include `:companyId` as a parameter.
- `AppRepository` must validate that the requester's `AppUser.companyId` matches the requested `companyId` on all cross-company operations.
- Firestore security rules must enforce `companyId`-based document-level access.
- No Firestore query may omit a `companyId` filter.

---

## 8. Data Privacy and Sensitive Records

### 8.1 Student PII

- Student `name`, `email`, `enrolledClass`, and monitoring data are personal data (PII).
- PII must never appear in logs, crash reports, or error messages.
- PII must not be included in `RecentActivityEntity.title/subtitle` in identifiable form.

### 8.2 Psychological Records

`PsychologicalNeedEntity` carries a `confidentiality: ConfidentialityLevel` enum (defined in `data/model/StudentMonitoring.kt`):

| Level             | Who Can Access                             |
| ----------------- | ------------------------------------------ |
| `Restricted`      | `PSICOPEDAGOGO` and above                  |
| `SharedSummary`   | `ORIENTADOR_EDUCACIONAL` and above         |

- `AccessPolicy` and `ObserveStudentsUseCase` must filter psychological records by confidentiality level before exposing them to any ViewModel.
- Psychological data must never be exported via CSV or included in reports accessible to lower-privileged roles.
- **Known gap (2026-05-07):** `ConfidentialityLevel`-based filtering is not yet enforced anywhere in the read path. See `TASKS.md`.
- A future migration can add a third level (e.g., `Confidential`) for `COORDENADOR`/`ADMINISTRADOR`-only records — when added, update this table and `BR-MON-003`.

### 8.3 Guardian Contact Records

`ParentFollowUpEntity` records communication with guardians. These records must:
- Only be visible to `ORIENTADOR_EDUCACIONAL` and above.
- Never be included in exports visible to the student or guardian themselves.

---

## 9. Android Platform Security

### 9.1 Network Security

- Only `INTERNET` and `ACCESS_NETWORK_STATE` permissions are declared.
- No `WRITE_EXTERNAL_STORAGE` or other broad permissions.
- Firebase enforces HTTPS for all connections — no `android:usesCleartextTraffic="true"` in manifest.
- A `network_security_config.xml` must explicitly deny cleartext traffic for production builds.

### 9.2 App Components

- `MainActivity` must not be exported with `intent-filter` beyond what is necessary for the launcher.
- No content providers expose app data to other apps.
- `backup_rules.xml` and `data_extraction_rules.xml` are configured to exclude sensitive Room DB files from backup.

### 9.3 ProGuard / R8

- `proguard-rules.pro` must preserve Room entity and DAO class names.
- Firebase classes must be kept per Firebase documentation.
- Hilt generated classes must be kept.
- Never disable R8 for release builds.

### 9.4 Debug vs Release

| Concern                           | Debug Build        | Release Build          |
| --------------------------------- | ------------------ | ---------------------- |
| `fallbackToDestructiveMigration`  | Allowed            | **MUST be disabled**   |
| Local SHA-256 credential fallback | Allowed            | **MUST be disabled**   |
| `android:debuggable`              | `true` (auto)      | `false` (auto)         |
| Log.d / Log.v output              | Allowed            | Stripped by ProGuard   |

---

## 10. Dependency and Supply Chain Security

- All library versions are pinned in `gradle/libs.versions.toml` — no floating versions (`+` or `latest`).
- Run `./gradlew dependencyCheckAnalyze` (OWASP Dependency Check) on every PR to detect known CVEs.
- Critical CVEs block merge.
- Firebase BOM version must be updated promptly when security patches are released.
- KSP and AGP versions must be compatible with Kotlin version — check compatibility table before upgrades.

---

## 11. Logging and Monitoring

### 11.1 What to Log

- Authentication events: login success/failure (with user ID — no email in prod logs).
- `SecurityException` events: log role, resource, and action — never the full stack trace to the user.
- Sync events: Firestore sync start/end, error counts.
- Migration events: which migration ran.

### 11.2 What Never to Log

- Student names, emails, CPF, or any PII.
- Psychological or behavioral records.
- Credentials, tokens, or `BuildConfig` secret fields.
- Raw Firestore documents with sensitive fields.

### 11.3 Log Tag

All log messages must use tag `"Educalink"`:

```kotlin
Log.i("Educalink", "User $userId logged in successfully")
Log.w("Educalink", "AccessPolicy denied: role=$role resource=$resource action=$action")
```

---

## 12. Security Definition of Done

A change is security-done when:

- [ ] All repository write methods validate the requester's role via `AccessPolicy.can()`.
- [ ] `companyId` filtering is present in all new `@Query` methods.
- [ ] No credentials, tokens, or keys are hardcoded in Kotlin, XML, or resource files.
- [ ] `local.properties` is in `.gitignore` and not committed.
- [ ] `PsychologicalNeedEntity` access is filtered by `ConfidentialityLevel` and role.
- [ ] New permissions in `AndroidManifest.xml` are justified and minimized.
- [ ] `fallbackToDestructiveMigration` and local auth fallback are disabled in release build variant.
- [ ] `dependencyCheckAnalyze` reports no critical CVEs for new/updated dependencies.
- [ ] No PII appears in log statements.
