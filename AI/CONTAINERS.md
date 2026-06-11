# Containers Standards — Educalink (app-desafio-sebrae)

> **Scope:** The Educalink application is a native Android app distributed as an APK/AAB. It does not use Docker or Kubernetes at runtime. This file covers **CI/CD build containers** (the containerized build agents that compile, test, and package the Android app) and best practices for managing the Android build pipeline.

---

## Table of Contents

1. [Scope and Context](#1-scope-and-context)
2. [CI Build Container Requirements](#2-ci-build-container-requirements)
3. [Android Build Pipeline in CI](#3-android-build-pipeline-in-ci)
4. [Signing Key Management](#4-signing-key-management)
5. [Artifact Management](#5-artifact-management)
6. [Container Definition of Done](#6-container-definition-of-done)

---

## 1. Scope and Context

This project has **no Docker/Kubernetes runtime deployment**. The "container" context applies exclusively to CI/CD build environments:

| Concern                  | Tool / Approach                                      |
| ------------------------ | ---------------------------------------------------- |
| Build environment        | GitHub Actions runner (ubuntu-latest or custom image) |
| Android SDK              | Pinned via `actions/setup-java` + Android SDK setup action |
| Gradle cache             | Cached in CI via `actions/cache` (`.gradle/`, `~/.gradle/`) |
| Signing                  | Keystore stored as GitHub Actions encrypted secret   |
| APK/AAB output           | Uploaded as CI artifact per PR and release           |

---

## 2. CI Build Container Requirements

If a custom Docker image is used for the Android build agent:

- Base image must be pinned to a specific digest: `eclipse-temurin:17-jdk@sha256:<digest>`.
- Android SDK, build tools, and platform versions must be pinned — no `latest`.
- Images must be scanned for CVEs before use (e.g., Trivy or Snyk).
- Images must run as a non-root user.
- No secrets or credentials may be baked into the image.
- Android SDK command-line tools version must match the `compileSdk` and `targetSdk` in `build.gradle.kts`.

### 2.1 Required SDK Components (pinned in CI setup)

| Component                         | Version |
| --------------------------------- | ------- |
| Java (JDK)                        | 17 (temurin) |
| Android build tools               | Match AGP 9.1.1 requirements |
| Android platform SDK              | `targetSdk` from `build.gradle.kts` |
| Kotlin                            | 2.3.20 (via Gradle wrapper) |
| Gradle wrapper                    | Version from `gradle/wrapper/gradle-wrapper.properties` |

---

## 3. Android Build Pipeline in CI

### 3.1 Mandatory CI Steps (in order)

> **Current state (2026-05-07):** `.github/workflows/release.yml` exists and runs `assembleRelease` + version bump + GitHub Release upload on push to `release` (or via `workflow_dispatch`). It does **not** yet run tests, lint, or dependency check, and there is no `ci.yml` for PRs. The list below is the target pipeline — see `TASKS.md → Infrastructure / Build`.

1. **Checkout** source code.
2. **Set up JDK 17** (temurin) with Gradle caching enabled.
3. **Validate Gradle wrapper** checksum (`gradle/wrapper/gradle-wrapper.jar`).
4. **Run unit tests:** `./gradlew test`
5. **Run lint:** `./gradlew lint`
6. **Run OWASP Dependency Check:** `./gradlew dependencyCheckAnalyze`
7. **Build debug APK:** `./gradlew assembleDebug`
8. **Build release AAB** (on `main` branch merge or tag): `./gradlew bundleRelease`
9. **Sign release AAB** using stored keystore secret.
10. **Upload artifacts** (APK/AAB, lint report, test report).

### 3.2 Branch Policies

| Branch   | Steps Run                              |
| -------- | -------------------------------------- |
| PR       | Steps 1–7 (build debug, no release)    |
| `main`   | All steps 1–10                         |
| Tags     | All steps 1–10 with Play Store upload  |

### 3.3 Gradle Wrapper Validation

Always validate the Gradle wrapper integrity in CI before use:

```yaml
- name: Validate Gradle wrapper
  uses: gradle/wrapper-validation-action@v2
```

---

## 4. Signing Key Management

- The release keystore (`.jks` file) must **never be committed to the repository**.
- Store the keystore as a Base64-encoded GitHub Actions secret: `RELEASE_KEYSTORE_BASE64`.
- Store keystore password as: `KEYSTORE_PASSWORD`.
- Store key alias as: `KEY_ALIAS`.
- Store key password as: `KEY_PASSWORD`.
- CI script decodes and signs the AAB in an ephemeral step — the decoded keystore is never persisted after the step.

```yaml
- name: Sign release AAB
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: |
    echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 -d > release.jks
    ./gradlew bundleRelease \
      -Pandroid.injected.signing.store.file=$(pwd)/release.jks \
      -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
      -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
      -Pandroid.injected.signing.key.password=$KEY_PASSWORD
    rm -f release.jks
```

---

## 5. Artifact Management

- Debug APK: retained for 7 days in CI artifacts (for QA and testing).
- Release AAB: retained for 30 days in CI artifacts.
- Lint reports: retained for 7 days.
- Test reports (HTML + XML): retained for 7 days.
- Coverage reports (JaCoCo): retained for 7 days.
- ProGuard mapping file: stored alongside the release AAB for crash deobfuscation.

---

## 6. Container Definition of Done

A CI pipeline change is done when:

- [ ] All SDK component versions are explicitly pinned — no floating `latest` versions.
- [ ] Gradle wrapper is validated before use.
- [ ] No secrets or credentials are hardcoded in CI YAML files.
- [ ] Release signing uses encrypted secrets — keystore never committed.
- [ ] `dependencyCheckAnalyze` runs and reports are uploaded as CI artifacts.
- [ ] ProGuard mapping file is preserved alongside the release artifact.
- [ ] CI passes `./gradlew build test lint` without errors on clean checkout.
