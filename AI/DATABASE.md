# Database Standards — Educalink (app-desafio-sebrae)

> **Scope:** These standards govern all data persistence in the Educalink Android application. The project uses **Room 2.8.4** as the local database (SQLite) and **Firebase Firestore** as the remote sync layer. The architecture is **local-first**: Room is the single source of truth for the UI; Firestore syncs into Room on demand.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [Mandatory Entity Traceability](#2-mandatory-entity-traceability)
3. [Room (SQLite) Standards](#3-room-sqlite-standards)
   - 3.1 [Entity Rules](#31-entity-rules)
   - 3.2 [DAO Rules](#32-dao-rules)
   - 3.3 [Database Configuration](#33-database-configuration)
   - 3.4 [Migrations](#34-migrations)
4. [Firebase Firestore Standards](#4-firebase-firestore-standards)
   - 4.1 [Sync Strategy](#41-sync-strategy)
   - 4.2 [Collection Naming](#42-collection-naming)
   - 4.3 [Security Rules](#43-security-rules)
5. [Multi-Tenancy](#5-multi-tenancy)
6. [Entity Catalog](#6-entity-catalog)
7. [Query Standards](#7-query-standards)
8. [Database Testing](#8-database-testing)
9. [Database Definition of Done](#9-database-definition-of-done)

---

## 1. General Principles

| Principle                    | Mandatory Behavior                                                                                         |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------- |
| **Local-First**              | Room is the UI's single source of truth. All reads go through Room. Firestore is a sync source only.       |
| **Schema as Code**           | Every structural change requires a versioned `Migration` in `AppDatabase`. No schema changes without migrations. |
| **Complete Traceability**    | All business entities must record `companyId` and, where meaningful, creation metadata.                    |
| **Security by Default**      | All queries filter by `companyId`. No cross-tenant data leakage is acceptable.                             |
| **Parameterized Queries**    | Every `@Query` uses named parameters (`:param`). String interpolation in SQL is a security violation.       |
| **Safe Evolution**           | Migrations must be backward-compatible. `fallbackToDestructiveMigration` must not run on production builds. |
| **Room-Only Access**         | `AppDao` is the only interface that touches Room. `AppRepository` is the only caller of `AppDao`.           |

---

## 2. Mandatory Entity Traceability

### 2.1 Required Fields on All Business Entities

| Field        | Type     | Description                                       |
| ------------ | -------- | ------------------------------------------------- |
| `id`         | `Int`    | Auto-generated primary key (Room `@PrimaryKey(autoGenerate = true)`) |
| `companyId`  | `Int`    | Multi-tenancy foreign key — mandatory on all business entities |

### 2.2 Recommended Additional Fields

Where applicable (user-created, audited entities):

| Field        | Type     | Description                                              |
| ------------ | -------- | -------------------------------------------------------- |
| `isActive`   | `Boolean`| Soft-delete / deactivation flag (use instead of physical deletion) |
| `createdAt`  | `Long`   | Unix timestamp (milliseconds UTC) of record creation     |
| `updatedAt`  | `Long`   | Unix timestamp (milliseconds UTC) of last update         |

> **Note:** The current entity set uses `isActive`/`isPublished` for soft-delete. Future entities should add `createdAt` and `updatedAt` for full auditability.

### 2.3 Physical Deletion Policy

- Business entities (students, courses, classes, teachers, certificates) must use **logical deletion** (`isActive = false`) rather than physical `DELETE`.
- Physical deletion is reserved for junction/link tables (`user_companies`, `guardian_students`) and administrative reset operations.
- The `ResetDatabase` action (admin-only) is the only permitted bulk-delete path.

---

## 3. Room (SQLite) Standards

### 3.1 Entity Rules

- Every entity class is annotated `@Entity(tableName = "snake_case_name")`.
- Primary key: `@PrimaryKey(autoGenerate = true) val id: Int = 0`.
- Column names: use `@ColumnInfo(name = "snake_case")` for multi-word fields.
- Foreign key references use `@ForeignKey` with `onDelete = CASCADE` only where cascade is business-correct.
- Every entity resides in the `data.local.entity` package in `:core`.
- Entity classes must be plain Kotlin `data class` — no business logic, no domain types (map to/from domain models in `AppRepository`).

```kotlin
// Correct
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "company_id") val companyId: Int,
    val name: String,
    val email: String,
    val course: String,
    @ColumnInfo(name = "enrolled_class") val enrolledClass: String,
    val progress: Int,
    val status: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
```

### 3.2 DAO Rules

- All DAO methods are in the single `AppDao` interface, annotated `@Dao`.
- Reactive queries return `Flow<T>` — Room automatically emits on data change.
- Write operations (`@Insert`, `@Update`, `@Delete`, `@Query` writes) are `suspend fun`.
- Every read query that is company-scoped includes `:companyId` parameter.
- Use `@Transaction` for queries spanning multiple tables.
- No raw SQL string building — only `@Query` with named parameters.

```kotlin
@Dao
interface AppDao {
    // Correct: parameterized, company-scoped, reactive
    @Query("SELECT * FROM students WHERE company_id = :companyId AND is_active = 1")
    fun observeActiveStudents(companyId: Int): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudent(student: StudentEntity)

    // FORBIDDEN: no company scope
    @Query("SELECT * FROM students")  // ← allows cross-tenant data access
    fun observeAllStudents(): Flow<List<StudentEntity>>
}
```

### 3.3 Database Configuration

- Database class: `AppDatabase` in `:core/.../data/local/AppDatabase.kt`, `@Database(entities = [...], version = N)`.
- **Current version: 10.** Source of truth is the `version = ...` argument in the `@Database` annotation; migrations define the history.
- `fallbackToDestructiveMigration(true)` **must only be present in `debug` build variants**. It must be removed or guarded for `release` builds (e.g., `if (BuildConfig.DEBUG) fallbackToDestructiveMigration(true)`).
  - **Known gap (2026-05-07):** the call in `app/.../di/AppModule.kt` (`provideAppDatabase`) is currently unconditional — see `TASKS.md`.
- Export schema: `exportSchema = true` is the target — schema files exported by Room must be committed to `app/schemas/` (path declared via `ksp { arg("room.schemaLocation", ...) }` in `app/build.gradle.kts`).
  - **Known gap (2026-05-07):** `exportSchema` is currently `false` and `app/schemas/` is not present — see `TASKS.md`.
- Database name: `"sebrae_local.db"` — do not rename.

### 3.4 Migrations

- Every schema change requires a `Migration(fromVersion, toVersion)` object declared inside `AppDatabase.companion object` and registered via `Room.databaseBuilder(...).addMigrations(...)` in `AppModule.provideAppDatabase`.
- Migrations must be named: `MIGRATION_{from}_{to}` (e.g., the existing `MIGRATION_9_10`).
- Migrations are additive only — never remove columns in a migration (use `isActive`/`isDeleted` instead).
- Migration SQL must be tested with `MigrationTestHelper` before merging.
- Schema files exported by Room must be committed to the repository alongside each migration.

```kotlin
// Existing example, in AppDatabase.kt
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE courses ADD COLUMN startDate TEXT DEFAULT NULL")
    }
}
```

---

## 4. Firebase Firestore Standards

### 4.1 Sync Strategy

- Firestore is accessed exclusively by `FirebaseDataConnectService`.
- **Remote → Local sync:** `FirebaseDataConnectService.syncScope(scope)` pulls Firestore documents and upserts into Room via `AppDao`.
- **Local → Remote push:** `AppRepository` write methods push to Firestore after writing to Room.
- `FirebaseRemoteBootstrapper` seeds Room from Firestore at app startup (runs once on first launch per session).
- `FirebaseScreenSyncPlanner` defines per-screen minimum collections — not all collections are synced for every screen.

### 4.2 Collection Naming

Firestore collection names must match Room table names exactly:

| Room Table            | Firestore Collection      |
| --------------------- | ------------------------- |
| `companies`           | `companies`               |
| `courses`             | `courses`                 |
| `school_classes`      | `school_classes`          |
| `teachers`            | `teachers`                |
| `students`            | `students`                |
| `certificates`        | `certificates`            |
| `calendar_events`     | `calendar_events`         |
| `recent_activities`   | `recent_activities`       |
| `attendance_records`  | `attendance_records`      |
| `app_users`           | `app_users`               |

### 4.3 Security Rules

- Firestore security rules must enforce `companyId`-based access: a user may only read/write documents where `companyId` matches their authorized company.
- Rules must deny unauthenticated access to all collections.
- Firestore rules must be version-controlled in the repository (e.g., `firestore.rules`).
- Firebase Auth UID must be used as the user identifier in security rules — not email or display name.

---

## 5. Multi-Tenancy

- Every query involving business data must include `companyId` as a filter parameter.
- `companyId` is resolved from the authenticated `AppUser` in `AppRepository` — it is never accepted as a client-supplied parameter from the UI.
- `UserCompanyEntity` (table `user_companies`) maps `userId → companyId` for multi-company users.
- `AppRepository` enforces that the requester's role and company match the requested data.
- No cross-company JOIN or query is permitted.

---

## 6. Entity Catalog

All current Room entities, their tables, and key fields:

| Entity Class                  | Table                  | Key Business Fields                                          |
| ----------------------------- | ---------------------- | ------------------------------------------------------------ |
| `CompanyEntity`               | `companies`            | name, cnpj, isActive                                         |
| `UserCompanyEntity`           | `user_companies`       | userId, companyId (composite PK)                             |
| `CourseEntity`                | `courses`              | companyId, title, category, instructor, completionRate, isPublished |
| `SchoolClassEntity`           | `school_classes`       | companyId, name, course, instructor, studentsCount, maxCapacity, schedule, status |
| `TeacherEntity`               | `teachers`             | companyId, name, email, specialty, activeCourses, totalStudents, rating, isActive |
| `StudentEntity`               | `students`             | companyId, name, email, course, enrolledClass, progress, status |
| `GuardianStudentEntity`       | `guardian_students`    | guardianUserId, studentId (junction)                         |
| `CertificateEntity`           | `certificates`         | companyId, studentName, courseName, issuedDate, hours, code  |
| `CalendarEventEntity`         | `calendar_events`      | companyId, title, course, date, time, location, type         |
| `RecentActivityEntity`        | `recent_activities`    | companyId, title, subtitle, iconKey, timeLabel               |
| `MonthlyEnrollmentEntity`     | `monthly_enrollments`  | companyId, month, count                                      |
| `AttendanceEntity`            | `attendance_records`   | companyId, studentId, date, status, minutesLate, justification |
| `BehaviorEntity`              | `behavior_records`     | companyId, studentId (behavioral observations)               |
| `PedagogicalNeedEntity`       | `pedagogical_needs`    | companyId, studentId                                         |
| `PsychologicalNeedEntity`     | `psychological_needs`  | companyId, studentId, confidentialityLevel                   |
| `ParentFollowUpEntity`        | `parent_followups`     | companyId, studentId (guardian contact history)              |
| `AppSettingsEntity`           | `app_settings`         | darkMode, pushEnabled, emailEnabled, language                |
| `AppUserEntity`               | `app_users`            | Firebase UID, name, email, role (cache)                      |

---

## 7. Query Standards

- All `Flow<T>`-returning queries are used for UI-observed data.
- All `suspend fun`-returning queries are used for one-shot reads and writes.
- Paging is implemented via `LIMIT` and `OFFSET` clauses — do not load unbounded lists for large datasets.
- Full-text search uses `LIKE '%' || :query || '%'` — for large datasets, consider enabling FTS5 extension.
- Aggregation queries (counts, sums) must be parameterized by `companyId`.

```kotlin
// Correct: paginated recent activities
@Query("SELECT * FROM recent_activities WHERE company_id = :companyId ORDER BY id DESC LIMIT :limit OFFSET :offset")
suspend fun getRecentActivitiesPaged(companyId: Int, limit: Int, offset: Int): List<RecentActivityEntity>
```

---

## 8. Database Testing

- Room DAO integration tests use `Room.inMemoryDatabaseBuilder` + a real `AppDatabase`.
- All `AppRepository` methods that involve Room are tested with a mocked `AppDao` using Mockito.
- Migration tests use `MigrationTestHelper` from `androidx.room:room-testing`.
- Test database must not persist between test runs — use `inMemoryDatabaseBuilder` or `allowMainThreadQueries()` only in tests.

---

## 9. Database Definition of Done

A database change is done when:

- [ ] New entity is added to `AppDatabase.entities` array with an incremented version.
- [ ] A `Migration` object is implemented and added to `AppDatabase.addMigrations(...)`.
- [ ] All `@Query` methods use `:companyId` filtering where applicable.
- [ ] No `@Query` uses string interpolation.
- [ ] `fallbackToDestructiveMigration` is not enabled in release build variants.
- [ ] Entity fields include `isActive` or equivalent soft-delete flag.
- [ ] Firestore collection name matches the Room table name.
- [ ] Schema export file is committed to the repository.
