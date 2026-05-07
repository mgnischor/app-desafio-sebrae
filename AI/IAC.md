# Infrastructure as Code Standards — Educalink (app-desafio-sebrae)

> **Scope:** The Educalink Android app uses **Firebase** (Auth + Firestore) as its remote backend. There is no traditional server infrastructure managed by this team. This file covers: Firebase project configuration, `google-services.json` management, `local.properties` secrets, Gradle dependency version control, ProGuard configuration, and CI/CD pipeline definition.

---

## Table of Contents

1. [Scope and Context](#1-scope-and-context)
2. [Firebase Configuration](#2-firebase-configuration)
3. [Secrets and Build Configuration](#3-secrets-and-build-configuration)
4. [Gradle Dependency Management](#4-gradle-dependency-management)
5. [ProGuard / R8 Configuration](#5-proguard--r8-configuration)
6. [CI/CD Pipeline Definition](#6-cicd-pipeline-definition)
7. [Firestore Security Rules as Code](#7-firestore-security-rules-as-code)
8. [IAC Definition of Done](#8-iac-definition-of-done)

---

## 1. Scope and Context

| Infrastructure Component      | Managed By                              | Location                     |
| ----------------------------- | --------------------------------------- | ---------------------------- |
| Firebase project               | Firebase Console + `google-services.json` | `app/google-services.json`  |
| Firestore collections          | Firebase Console + security rules       | `firestore.rules` (to be added to repo) |
| Firebase Auth providers        | Firebase Console                        | N/A (no IaC yet)             |
| Android build configuration    | `build.gradle.kts`, `libs.versions.toml` | Repo root + `gradle/`       |
| Secrets (dev credentials)      | `local.properties` (git-ignored)        | Repo root (local only)       |
| CI/CD pipeline                 | GitHub Actions YAML                     | `.github/workflows/`         |
| Release signing                | Encrypted GitHub Actions secrets        | GitHub repository settings   |

---

## 2. Firebase Configuration

### 2.1 google-services.json

- `app/google-services.json` is the Firebase project configuration file for the Android app.
- It contains the Firebase project ID, application ID, API keys for Firebase SDK initialization, and OAuth client IDs.
- **It does not contain Firebase Admin SDK private keys or service account credentials** — those must never be placed in this file.
- `google-services.json` is committed to the repository (it is not a secret — Firebase SDK API keys are restricted by Android application ID and SHA certificate fingerprint).
- If the Firebase project changes, `google-services.json` must be re-downloaded and committed.

### 2.2 Firebase Project Naming Convention

| Environment | Firebase Project ID           | Notes                         |
| ----------- | ----------------------------- | ----------------------------- |
| Development | `sebrae-desafio-dev` (example) | Used with debug build variant |
| Production  | `sebrae-desafio-prod` (example) | Used with release build variant |

Use `google-services.json` per build variant if separate Firebase projects are used for dev/prod (place in `app/src/debug/` and `app/src/release/` respectively).

### 2.3 Firebase Console Changes

- Changes to Firebase Auth providers, Firestore indexes, and security rules must be documented as a comment in the relevant configuration file or in `docs/`.
- **No manual, undocumented Firebase Console changes** — all changes must have a corresponding code or documentation update.
- Firestore composite indexes must be exported and committed as `firestore.indexes.json`.

---

## 3. Secrets and Build Configuration

### 3.1 local.properties

`local.properties` is the mechanism for injecting dev-only secrets into the build:

```properties
# local.properties — NEVER commit this file
sdk.dir=C\:\\Users\\dev\\AppData\\Local\\Android\\Sdk

# Seed credentials for dev/demo Firebase Auth fallback
firebase.seed.email=admin@example.com
firebase.seed.password=<plaintext_dev_password>
```

These values are injected as `BuildConfig` fields in `app/build.gradle.kts` (see actual code at `app/build.gradle.kts:48-59`):

```kotlin
buildConfigField("String", "FIREBASE_SEED_EMAIL", toBuildConfigString(firebaseSeedEmail))
buildConfigField("String", "FIREBASE_SEED_PASSWORD", toBuildConfigString(firebaseSeedPassword))
buildConfigField(
    "boolean",
    "FIREBASE_SEED_CREDENTIALS_CONFIGURED",
    (firebaseSeedEmail.isNotBlank() && firebaseSeedPassword.isNotBlank()).toString(),
)
```

### 3.2 Rules

- `local.properties` must be in `.gitignore` — it is already excluded by the Android Studio default `.gitignore`.
- CI builds must provide empty strings for seed credential `BuildConfig` fields when `local.properties` is absent — the existing build script already does this; `FIREBASE_SEED_CREDENTIALS_CONFIGURED` will then evaluate to `false` and the local fallback must not activate.
- Never add production credentials to `local.properties` — only dev/demo seeds.
- `BuildConfig` field naming follows the domain-prefixed convention defined in `SECURITY.md §6.2` (e.g., `FIREBASE_*` for Firebase-related seeds). A bare `BUILD_*` prefix is reserved for non-secret build metadata.

### 3.3 CI Secret Variables

| Secret Name              | Purpose                                         |
| ------------------------ | ----------------------------------------------- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded release keystore                |
| `KEYSTORE_PASSWORD`      | Keystore password                               |
| `KEY_ALIAS`              | Key alias within the keystore                   |
| `KEY_PASSWORD`           | Key password                                    |
| `FIREBASE_SERVICE_ACCOUNT` | (Future) For Firebase App Distribution deploys |

---

## 4. Gradle Dependency Management

### 4.1 libs.versions.toml — Single Source of Truth

All dependency versions are declared in `gradle/libs.versions.toml`. This file is the **single source of truth** for all library and plugin versions.

**Rules:**
- Never declare a version inline in a `build.gradle.kts` — always use `libs.*` aliases.
- No floating versions (`+`, `latest.release`, `SNAPSHOT`) except in explicitly justified dev branches.
- Version upgrades must be tested (run `./gradlew build test`) before merging.
- Firebase BOM and Compose BOM control their child library versions — do not override BOM-managed versions individually.

### 4.2 Version Update Policy

| Category              | Update Trigger                                     |
| --------------------- | -------------------------------------------------- |
| Security patches       | Immediate — within 72 hours of CVE disclosure     |
| Firebase BOM           | Monthly review                                     |
| Compose BOM            | Quarterly or with significant Material 3 updates  |
| Kotlin / KSP / AGP     | Aligned upgrades — check compatibility matrix first |
| Other libraries        | Quarterly review                                   |

### 4.3 Kotlin / KSP / AGP Compatibility

Always verify the Kotlin ↔ KSP ↔ AGP compatibility matrix before upgrading any of these three:

| Current | Kotlin | KSP    | AGP   |
| ------- | ------ | ------ | ----- |
| Active  | 2.3.20 | 2.3.6  | 9.1.1 |

---

## 5. ProGuard / R8 Configuration

- R8 is enabled for all release builds — do not disable with `minifyEnabled = false` for release.
- `proguard-rules.pro` is the project-specific rules file (`app/proguard-rules.pro`).
- Mandatory keep rules:

```proguard
# Room entities — Room generates code based on entity class names
-keep class tech.datatower.sebrae.desafio.data.local.entity.** { *; }
-keepclassmembers class tech.datatower.sebrae.desafio.data.local.entity.** { *; }

# Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class **_HiltModules { *; }
-keep class **_GeneratedInjector { *; }

# Firebase — follow Firebase Android SDK ProGuard guide
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
```

- The ProGuard mapping file (`mapping.txt`) generated per release build must be uploaded to Firebase Crashlytics (or equivalent) for crash deobfuscation.
- The mapping file must be stored as a CI artifact per release.

---

## 6. CI/CD Pipeline Definition

CI/CD pipelines are defined in `.github/workflows/` (GitHub Actions). Pipeline YAML files are infrastructure — apply the same review standards as code.

### 6.1 Required Workflows

| Workflow File              | Trigger             | Purpose                                    |
| -------------------------- | ------------------- | ------------------------------------------ |
| `ci.yml`                   | PR to `main`        | Unit tests, lint, dependency check, debug build |
| `release.yml`              | Push to `main` / tag | Release AAB build, sign, upload artifact  |

### 6.2 Pipeline Rules

- All action versions pinned to a specific SHA or semantic version tag: `actions/checkout@v4` (not `@latest`).
- Secrets are referenced by name (`${{ secrets.SECRET_NAME }}`) — never hardcoded values.
- Gradle tasks run with `--no-daemon` in CI to avoid stale daemon state.
- Cache the Gradle home directory (`~/.gradle/caches`, `~/.gradle/wrapper`) between runs.
- Fail-fast: `fail-fast: true` on matrix builds.

### 6.3 Minimal CI Workflow Structure

```yaml
name: CI
on:
  pull_request:
    branches: [main]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: gradle/wrapper-validation-action@v2
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: gradle
      - run: ./gradlew test lint dependencyCheckAnalyze assembleDebug --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: reports
          path: |
            app/build/reports/
            app/build/outputs/apk/debug/
```

---

## 7. Firestore Security Rules as Code

Firestore security rules must be version-controlled in the repository root as `firestore.rules`.

### 7.1 Mandatory Rules Structure

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Deny all by default
    match /{document=**} {
      allow read, write: if false;
    }

    // Company-scoped collections
    match /students/{studentId} {
      allow read, write: if request.auth != null
        && request.auth.uid != null
        && resource.data.companyId == getUserCompanyId(request.auth.uid);
    }

    // ... (other collections follow the same pattern)
  }
}
```

Rules must be deployed via Firebase CLI (`firebase deploy --only firestore:rules`) — never changed directly in the Firebase Console without updating `firestore.rules`.

---

## 8. IAC Definition of Done

An infrastructure change is done when:

- [ ] All dependency version changes are in `libs.versions.toml` — no inline versions.
- [ ] `local.properties` remains absent from git history (check with `git log -- local.properties`).
- [ ] `firestore.rules` is updated and committed alongside any schema or access pattern change.
- [ ] `firestore.indexes.json` is updated for any new composite query.
- [ ] CI pipeline YAML uses pinned action versions.
- [ ] ProGuard mapping file is preserved as a CI artifact for release builds.
- [ ] New `BuildConfig` fields from secrets follow the `BUILD_` prefix convention.
- [ ] `./gradlew dependencyCheckAnalyze` passes with no critical CVEs.
