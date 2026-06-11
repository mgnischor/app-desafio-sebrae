# Code Quality and Performance Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern all Kotlin code in the Educalink Android application. The project uses **Kotlin 2.3.20**, **Jetpack Compose**, **Hilt 2.59.2**, **Room 2.8.4**, and **Firebase**. All code generation uses **KSP** (not KAPT). Security requirements are in [SECURITY.md](./SECURITY.md).

---

## Table of Contents

1. [General Engineering Principles](#1-general-engineering-principles)
2. [Global Quality Gates](#2-global-quality-gates)
3. [Kotlin Standards](#3-kotlin-standards)
   - 3.1 [Architecture and Design Rules](#31-architecture-and-design-rules)
   - 3.2 [Coroutines and Async Rules](#32-coroutines-and-async-rules)
   - 3.3 [Compose UI Rules](#33-compose-ui-rules)
   - 3.4 [ViewModel Rules](#34-viewmodel-rules)
   - 3.5 [Hilt DI Rules](#35-hilt-di-rules)
   - 3.6 [Room Rules](#36-room-rules)
   - 3.7 [Performance Rules](#37-performance-rules)
   - 3.8 [Quality and Maintainability Rules](#38-quality-and-maintainability-rules)
4. [Code Review and Pull Request Standards](#4-code-review-and-pull-request-standards)
5. [Definition of Done for Code Quality](#5-definition-of-done-for-code-quality)

---

## 1. General Engineering Principles

| Principle                    | Mandatory Behavior                                                                                            |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------- |
| **Clarity Over Cleverness**  | Prefer explicit Kotlin idioms over compact but obscure logic.                                                 |
| **Measure Before Optimizing**| Never optimize based on assumptions. Use Android Profiler and benchmark tools.                                |
| **Fail Fast**                | Validate inputs at `AppRepository` boundary. Throw domain-meaningful exceptions.                              |
| **Single Responsibility**    | Each ViewModel, UseCase, and Composable has one primary reason to change.                                     |
| **SOLID by Default**         | Apply SOLID as a mandatory design baseline for new code.                                                      |
| **Immutability First**       | Prefer `val`, `data class`, immutable `StateFlow`, and `@Immutable` Compose models.                          |
| **Coroutines Over Threads**  | Use Kotlin Coroutines and `Flow` for all async operations. No `Thread`, `AsyncTask`, or RxJava.               |
| **StateFlow Over LiveData**  | All ViewModels must expose `StateFlow`. `LiveData` is forbidden in new code.                                  |
| **No Business Logic in UI**  | Composable functions must contain zero business logic. All logic lives in ViewModels or rule objects.         |

### 1.1 Mandatory SOLID Application

- **SRP:** Composables render state. ViewModels transform state. UseCases encode business queries. `AppRepository` handles persistence.
- **OCP:** Add new screens by creating a new feature module + Composable + ViewModel — not by modifying existing ones.
- **LSP:** All `ConnectivityObserver` implementations must be substitutable without behavior surprises.
- **ISP:** Do not add unrelated methods to `AppRepository`; split into focused repository interfaces if needed.
- **DIP:** ViewModels inject `ObserveXxxUseCase` interfaces, not `AppRepository` directly (preferred pattern for new code).

---

## 2. Global Quality Gates

| Gate               | Requirement                                            | Policy                                |
| ------------------ | ------------------------------------------------------ | ------------------------------------- |
| Kotlin compiler    | Zero compiler errors and zero suppressed warnings      | Compile errors block merge            |
| Unit Tests         | All domain rule objects have passing tests             | Failing tests block merge             |
| Line Coverage      | ≥ 80% for `:core` domain and data layers               | Below threshold blocks merge          |
| Branch Coverage    | ≥ 70% for decision-heavy rule objects                  | Below threshold blocks merge          |
| Lint               | No `HIGH`-severity Android Lint issues                 | HIGH severity blocks merge            |
| Dependency Health  | No critical CVEs in `libs.versions.toml` dependencies  | Critical CVEs block merge             |
| Build validation   | `./gradlew build` clean with no errors                 | Build failures block merge            |

### 2.1 Coverage Targets by Layer

| Layer                      | Min Line Coverage | Min Branch Coverage |
| -------------------------- | :---------------: | :-----------------: |
| Rule objects (domain)      | 90%               | 80%                 |
| `AppRepository`            | 80%               | 70%                 |
| `AccessPolicy`             | 90%               | 85%                 |
| ViewModels                 | 70%               | 60%                 |
| Composables (UI)           | Best effort       | Best effort         |

---

## 3. Kotlin Standards

### 3.1 Architecture and Design Rules

- Enforce layer boundaries: Composables → ViewModels → UseCases → `AppRepository` → `AppDao`/Firebase.
- `AppDao` must only be accessed by `AppRepository` — never by ViewModels or UseCases directly.
- Firestore must only be accessed by `FirebaseDataConnectService` — never by `AppRepository` directly.
- Domain rule objects (`EntityLifecycleRules`, `RelationshipRules`, etc.) must be pure `object` with no dependencies.
- Domain models must not import Room, Firestore, Hilt, or Compose types.
- Avoid `object` singletons for stateful components — use Hilt `@Singleton` instead.

```kotlin
// Correct
object RelationshipRules {
    fun realStudentsByCourse(students: List<Student>, courseId: Int): Int =
        students.count { it.course == courseId }
}

// FORBIDDEN: accessing AppDao from ViewModel
@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val dao: AppDao  // ← VIOLATION
) : ViewModel()
```

### 3.2 Coroutines and Async Rules

- Use `viewModelScope.launch` for one-shot operations in ViewModels.
- Use `viewModelScope` + `stateIn(SharingStarted.WhileSubscribed(5000))` to convert `Flow` to `StateFlow`.
- Use `Dispatchers.IO` for Room and Firestore operations in `AppRepository`.
- Use `kotlinx.coroutines.test` (`runTest`, `TestCoroutineDispatcher`) for coroutine unit tests.
- Never use `runBlocking` in production code.
- Always pass and respect `CancellationException` — do not catch it silently.
- Use `callbackFlow` for wrapping Firestore real-time listeners (already used in `FirebaseDataConnectService`).

```kotlin
// Correct: StateFlow from repository Flow
val students: StateFlow<List<Student>> = observeStudentsUseCase(companyId, role)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// FORBIDDEN
val students = MutableLiveData<List<Student>>()
```

### 3.3 Compose UI Rules

- Composable functions must be **stateless** — receive state as parameters and emit events via lambdas.
- All `@Composable` screens receive a `ViewModel` via `hiltViewModel()` — not via constructor.
- Use `collectAsState()` to observe `StateFlow` in Composable screens.
- Use shared Composable components from `:core`: `DetailScaffold`, `EmptyState`, `ListSearchHeader`, `LoadingOverlay`, `OfflineBanner`, `SearchableDropdown`, `StatusChip`.
- Every screen must handle three states: **loading**, **error**, and **content** (use `LoadingOverlay` + `EmptyState`).
- `OfflineBanner` must be shown on all data screens when `ConnectivityObserver.Status` is not `Available`.
- Do not call `suspend` functions directly from Composables — use `LaunchedEffect` or ViewModel.
- Use `LaunchedEffect(Unit)` to trigger `SyncScreenDataUseCase` on screen entry.

```kotlin
// Correct
@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    // ...
    OfflineBanner(visible = isOffline)
    if (students.isEmpty()) EmptyState(...) else StudentList(students)
}

// FORBIDDEN: business logic in Composable
@Composable
fun StudentsScreen() {
    val filtered = allStudents.filter { it.isActive }  // ← VIOLATION: logic in UI
}
```

### 3.4 ViewModel Rules

- Every ViewModel is `@HiltViewModel` and annotated with `@Inject constructor`.
- Private `_state: MutableStateFlow<T>` exposed as `val state: StateFlow<T> = _state.asStateFlow()`.
- ViewModels must not hold references to `Context`, `Activity`, or `Fragment`.
- ViewModels must not contain `@Composable` calls.
- Error states must be exposed as `StateFlow<String?>` or a sealed class — not thrown as exceptions to the UI.
- Use `savedStateHandle` for arguments passed via Navigation Compose.

```kotlin
@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppRepository
) : ViewModel() {
    private val courseId = savedStateHandle.get<Int>("courseId")!!
    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course.asStateFlow()
}
```

### 3.5 Hilt DI Rules

- Use **KSP** processor for Hilt — never add `kapt` to any module's `build.gradle.kts`.
- All dependencies that need to be provided are registered in `AppModule` (`@InstallIn(SingletonComponent::class)`).
- Do not use `@Provides` for ViewModels — Hilt handles them automatically via `@HiltViewModel`.
- Use `@Qualifier` annotations (e.g., `@DataSourceLabelFlow`) for same-type bindings.
- `AppGraphEntryPoint` is only for non-injected Composable entry points — use sparingly.

### 3.6 Room Rules

- All Room entity fields follow the naming convention in [DATABASE.md](./DATABASE.md).
- All `@Query` methods use parameterized arguments — never string interpolation.
- All Room entities are annotated with `@Entity(tableName = "...")`.
- All queries that filter by company use `:companyId` as a parameter.
- New entities require a versioned `Migration` in `AppDatabase` — never rely on `fallbackToDestructiveMigration` in production builds.
- Use `@Transaction` for queries that read from multiple tables to ensure consistency.

```kotlin
// Correct
@Query("SELECT * FROM students WHERE company_id = :companyId AND is_active = 1")
fun observeActiveStudents(companyId: Int): Flow<List<StudentEntity>>

// FORBIDDEN: string interpolation
@Query("SELECT * FROM students WHERE company_id = $companyId")  // ← SQL injection risk
```

### 3.7 Performance Rules

| Area                  | Mandatory Guidance                                                                     |
| --------------------- | -------------------------------------------------------------------------------------- |
| Compose recomposition | Use `@Immutable` / `@Stable` on all domain models passed to Composables.              |
| Lazy lists            | Use `LazyColumn` / `LazyRow` for all lists with potentially unbounded item counts.     |
| Image loading         | Use `painterResource` or Coil for images — never load bitmaps on the main thread.     |
| Room queries          | Return `Flow<T>` for observed data; use `suspend fun` for one-shot writes.             |
| Firestore             | Use real-time listeners (`callbackFlow`) only for high-frequency data; one-shot for stable data. |
| Memory                | Avoid holding large lists in ViewModel longer than necessary; use `Flow` transformations. |
| Startup              | `FirebaseRemoteBootstrapper` runs at startup — keep it non-blocking with `Dispatchers.IO`. |

### 3.8 Quality and Maintainability Rules

| Topic             | Kotlin/Android Guidance                                                                        |
| ----------------- | ---------------------------------------------------------------------------------------------- |
| Null Safety       | Avoid `!!` operator. Use `?: return`, `let`, `takeIf`, `requireNotNull` with meaningful messages. |
| Function Size     | Composables and functions should not exceed ~60 lines. Extract to sub-Composables or helpers.  |
| Naming            | Domain language in all names. Use Portuguese domain terms where the domain uses them.          |
| Exceptions        | Use exceptions for exceptional flows (security violations, data corruption). Use sealed classes for expected UI errors. |
| Logging           | Use `android.util.Log` with tag `"Educalink"`. Never log student PII, credentials, or psychological data. |
| String Resources  | All user-facing strings must be in `res/values/strings.xml` (PT-BR) and `values-en/`, `values-es/`. |
| Constants         | Route constants in `AppRoutes`. UI label constants in string resources. No magic strings.       |

---

## 4. Code Review and Pull Request Standards

- Every PR must have at least one reviewer.
- PRs must not contain `TODO` comments without a linked issue.
- PRs must not lower coverage below the thresholds in [Section 2.1](#21-coverage-targets-by-layer).
- New feature modules must register routes in `AppNavHost` and dependencies in `AppModule`.
- PRs introducing new Room entities must include a migration script.
- PRs touching `AccessPolicy` must update `AccessPolicyTest` and `AI/BUSINESS.md`.
- `fallbackToDestructiveMigration(true)` must not be enabled on production build variants.

---

## 5. Definition of Done for Code Quality

A feature or change is done when:

- [ ] All Kotlin compiler warnings are resolved.
- [ ] Domain models are `@Immutable data class` with no infrastructure dependencies.
- [ ] ViewModels expose `StateFlow`, not `LiveData`.
- [ ] Composables use `collectAsState()` and are free of business logic.
- [ ] All new `@Query` methods use parameterized arguments.
- [ ] Coverage thresholds are met for the affected layers.
- [ ] `./gradlew build` completes without errors.
- [ ] All string resources are present in PT-BR (default), EN, and ES.
- [ ] Android Lint reports no `HIGH`-severity issues.
