# Testing Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern all tests in the Educalink Android application. Tests must be deterministic, isolated, fast, and aligned with domain behavior. The test stack is **JUnit 4 + Mockito Kotlin + Kotlinx Coroutines Test + Compose UI Test**.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [Test Classification and Pyramid](#2-test-classification-and-pyramid)
3. [Test Stack](#3-test-stack)
4. [Unit Testing Standards](#4-unit-testing-standards)
   - 4.1 [Rule Object Tests (Domain)](#41-rule-object-tests-domain)
   - 4.2 [Repository Tests](#42-repository-tests)
   - 4.3 [ViewModel Tests](#43-viewmodel-tests)
   - 4.4 [Test Naming and Structure](#44-test-naming-and-structure)
5. [Integration Testing Standards](#5-integration-testing-standards)
6. [UI Testing Standards](#6-ui-testing-standards)
7. [Existing Test Catalog](#7-existing-test-catalog)
8. [Coverage and Quality Gates](#8-coverage-and-quality-gates)
9. [Test Data Management](#9-test-data-management)
10. [Flaky Test Policy](#10-flaky-test-policy)
11. [Testing Definition of Done](#11-testing-definition-of-done)

---

## 1. General Principles

| Principle                       | Mandatory Behavior                                                                                                             |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| **Determinism**                 | Tests produce the same result every run, regardless of environment or time.                                                    |
| **Isolation**                   | Tests do not share mutable state. Each test sets up its own preconditions.                                                     |
| **Speed**                       | Unit tests (JVM) run in < 50ms. No Android emulator required for domain and repository tests.                                  |
| **Readability**                 | Test names describe the scenario and expected outcome in domain language.                                                      |
| **Domain Alignment**            | Tests verify domain behavior described in [BUSINESS.md](./BUSINESS.md), not implementation details.                           |
| **AAA Structure**               | Every test follows **Arrange → Act → Assert**. No interleaving of setup and assertions.                                        |
| **No Infrastructure in Unit Tests** | Unit tests do not access Room, Firestore, or Android system services. Use Mockito for all dependencies.                    |
| **Security as a Test Concern**  | `AccessPolicy` and multi-tenancy rules must be covered by dedicated tests.                                                     |

---

## 2. Test Classification and Pyramid

| Type            | Location                   | Requires Android  | Speed    |
| --------------- | -------------------------- | :---------------: | -------- |
| **Unit**        | `app/src/test/`            | No (JVM only)     | < 50ms   |
| **Integration** | `app/src/test/` (in-memory Room) | No          | < 2s     |
| **UI (Compose)**| `app/src/androidTest/`     | Yes (emulator)    | < 30s    |
| **Instrumented**| `app/src/androidTest/`     | Yes (emulator)    | < 30s    |

Target distribution: **70% unit** / **20% integration** / **10% UI+instrumented**.

---

## 3. Test Stack

| Purpose                        | Library                          | Version  |
| ------------------------------ | -------------------------------- | -------- |
| Test runner                    | JUnit 4                          | 4.13.2   |
| Coroutines testing             | `kotlinx-coroutines-test`        | 1.10.2   |
| Mocking (Kotlin-idiomatic)     | `mockito-kotlin`                 | 6.3.0    |
| Mocking (base)                 | `mockito-core`                   | 5.23.0   |
| Compose UI test                | `compose-ui-test-junit4`         | via BOM  |
| Android instrumentation        | `androidx.test.ext:junit`        | 1.3.0    |
| UI automation                  | `espresso-core`                  | 3.7.0    |

All test dependencies are declared in `gradle/libs.versions.toml` under the `[libraries]` section.

---

## 4. Unit Testing Standards

### 4.1 Rule Object Tests (Domain)

Rule objects (`EntityLifecycleRules`, `RelationshipRules`, `StudentMonitoringRules`, `RealtimeNotificationRules`) are pure `object` — test them directly without mocking:

```kotlin
class StudentMonitoringRulesTest {

    @Test
    fun `attendance rate excludes justified absences from denominator`() {
        // Arrange
        val records = listOf(
            AttendanceRecord(status = AttendanceStatus.Present),
            AttendanceRecord(status = AttendanceStatus.JustifiedAbsence),
            AttendanceRecord(status = AttendanceStatus.Absent),
        )
        // Act
        val rate = StudentMonitoringRules.attendanceRate(records)
        // Assert
        assertEquals(0.5f, rate)  // 1 present / 2 non-justified records
    }

    @Test
    fun `late attendance counts as present in rate calculation`() { ... }

    @Test
    fun `attendance below 75 percent generates critical alert`() { ... }

    @Test
    fun `attendance rate is null when all records are justified`() { ... }
}
```

### 4.2 Repository Tests

`AppRepository` tests mock `AppDao` using Mockito and run with `runTest`:

```kotlin
class AppRepositoryRecentActivitiesTest {

    private val dao: AppDao = mock()
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        repository = AppRepository(dao, /* other mocked deps */)
    }

    @Test
    fun `getRecentActivitiesPaged maps entity to domain model correctly`() = runTest {
        // Arrange
        val entity = RecentActivityEntity(id = 1, companyId = 42, title = "Test", ...)
        whenever(dao.getRecentActivitiesPaged(42, 20, 0)).thenReturn(listOf(entity))
        // Act
        val result = repository.getRecentActivitiesPaged(companyId = 42, limit = 20, offset = 0)
        // Assert
        assertEquals("Test", result.first().title)
    }
}
```

### 4.3 ViewModel Tests

ViewModel tests use `runTest` + `TestCoroutineDispatcher` from `kotlinx-coroutines-test`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val observeStudentsUseCase: ObserveStudentsUseCase = mock()
    private lateinit var viewModel: StudentsViewModel

    @Before
    fun setUp() {
        whenever(observeStudentsUseCase(any(), any())).thenReturn(flowOf(emptyList()))
        viewModel = StudentsViewModel(observeStudentsUseCase)
    }

    @Test
    fun `students StateFlow emits empty list initially`() = runTest {
        val result = viewModel.students.first()
        assertTrue(result.isEmpty())
    }
}
```

### 4.4 Test Naming and Structure

- Test names describe the scenario: `` `given X when Y then Z` `` or `` `Y returns Z when X` ``.
- Tests follow strict AAA (Arrange, Act, Assert) with blank lines separating sections.
- One assertion per logical behavior — use `@Test` per case, not one large test.
- Test class names match the subject class: `AccessPolicyTest` for `AccessPolicy`.
- Do not use `@Ignore` without a linked issue. Flaky tests must be quarantined immediately.

---

## 5. Integration Testing Standards

Integration tests for Room use in-memory databases:

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.appDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `upsertStudent and observeActiveStudents roundtrip`() = runTest {
        // Arrange
        val student = StudentEntity(companyId = 1, name = "Ana", ...)
        // Act
        dao.upsertStudent(student)
        val result = dao.observeActiveStudents(1).first()
        // Assert
        assertEquals(1, result.size)
        assertEquals("Ana", result.first().name)
    }
}
```

Migration integration tests must use `MigrationTestHelper`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate9to10() {
        helper.createDatabase(TEST_DB, 9).close()
        helper.runMigrationsAndValidate(TEST_DB, 10, true, AppDatabase.MIGRATION_9_10)
    }
}
```

---

## 6. UI Testing Standards

- Use `ComposeTestRule` (`createComposeRule()`) for isolated Composable tests.
- Use `createAndroidComposeRule<MainActivity>()` for full-screen integration tests.
- Test all three screen states: **loading**, **empty**, and **content**.
- Test RBAC-driven UI differences: e.g., create buttons hidden for `RESPONSAVEL` role.
- Accessibility: verify `contentDescription` values are set for all icon-only buttons.

```kotlin
@get:Rule val composeTestRule = createComposeRule()

@Test
fun `empty state is shown when students list is empty`() {
    composeTestRule.setContent {
        StudentsScreen(viewModel = fakeViewModelWithEmptyList())
    }
    composeTestRule.onNodeWithText("Nenhum aluno encontrado").assertIsDisplayed()
}
```

---

## 7. Existing Test Catalog

All existing unit tests are in `app/src/test/java/tech/datatower/sebrae/desafio/`:

| Test Class                          | Subject                          | Coverage Focus                                   |
| ----------------------------------- | -------------------------------- | ------------------------------------------------ |
| `StudentMonitoringRulesTest`        | `StudentMonitoringRules`         | Attendance rate calculation, alert generation    |
| `AccessPolicyTest`                  | `AccessPolicy`                   | RBAC matrix — all roles × resources × actions    |
| `AuthManagerTest`                   | `AuthManager`                    | Auth flow behavior                               |
| `AppRepositoryRecentActivitiesTest` | `AppRepository`                  | Paged activities query mapping                   |
| `RemoteBootstrapperTest`            | `FirebaseRemoteBootstrapper`     | NoOp bootstrapper contract                       |
| `FirebaseScreenSyncPlannerTest`     | `FirebaseScreenSyncPlanner`      | Screen → sync task mapping                       |
| `RelationshipRulesTest`             | `RelationshipRules`              | Student counts, link validation                  |
| `EntityLifecycleRulesTest`          | `EntityLifecycleRules`           | Activate/deactivate all entity types             |
| `RealtimeNotificationRulesTest`     | `RealtimeNotificationRules`      | Notification draft building, role filter         |
| `CourseAndTeacherModelTest`         | `Course`, `Teacher`              | Model rules                                      |
| `StudentModelTest`                  | `Student`                        | Student model validation                         |

Instrumented test: `ExampleInstrumentedTest` in `app/src/androidTest/`.

---

## 8. Coverage and Quality Gates

| Module / Layer                  | Min Line Coverage | Min Branch Coverage |
| ------------------------------- | :---------------: | :-----------------: |
| Rule objects (domain)           | 90%               | 80%                 |
| `AppRepository`                 | 80%               | 70%                 |
| `AccessPolicy`                  | 90%               | 85%                 |
| UseCases                        | 80%               | 70%                 |
| ViewModels                      | 70%               | 60%                 |

Coverage is measured by `./gradlew test jacocoTestReport`. Coverage below threshold blocks merge.

---

## 9. Test Data Management

- Use in-memory `AppDatabase` — never a persistent test database.
- Firestore is always mocked — never call real Firebase from unit or integration tests.
- Test fixture entities: create `TestFixtures` objects in `test/` source set with pre-built entity/domain model instances.
- CSV import tests use in-memory string streams — no temp files.
- No `Thread.sleep()` in tests — use `runTest` and `advanceUntilIdle()` from coroutines-test.

---

## 10. Flaky Test Policy

- Flaky tests must be quarantined within one sprint of detection (annotated with `@Ignore("FLAKY: #issue-number")`).
- Root cause must be investigated within two sprints.
- Flaky tests must never be silently deleted — fix the root cause or document the known issue.
- Any test depending on system time must use `TestCoroutineScheduler` or a `Clock` abstraction.

---

## 11. Testing Definition of Done

A feature or change is test-done when:

- [ ] All new rule object methods have at least one unit test for happy path and one for each edge case.
- [ ] `AccessPolicyTest` is updated for any new resource or action in `AccessPolicy`.
- [ ] Repository tests cover the new `AppDao` queries with mocked DAO.
- [ ] ViewModel tests cover state transitions triggered by user actions.
- [ ] Coverage thresholds from [Section 8](#8-coverage-and-quality-gates) are maintained.
- [ ] All tests pass with `./gradlew test` (JVM) and `./gradlew connectedAndroidTest` (instrumented).
- [ ] No `@Ignore` added without a linked issue number.
