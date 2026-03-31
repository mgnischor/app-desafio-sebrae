/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/local/AppDatabase.kt
    Descrição: Configuração do banco de dados local utilizando a biblioteca Room.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
package tech.datatower.sebrae.desafio.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.model.UserRole
import java.time.LocalDate

/** Modelo e comportamento relacionados a company entity. */
@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val cnpj: String = "",
    val isActive: Boolean = true,
)

/** Associação entre usuário e empresa (controle de acesso multi-tenant). */
@Entity(tableName = "user_companies")
data class UserCompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val companyId: Int,
)

/** Modelo e comportamento relacionados a course entity. */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val title: String,
    val category: String,
    val instructor: String,
    val totalStudents: Int,
    val durationHours: Int,
    val completionRate: Float,
    val isPublished: Boolean,
)

/** Modelo e comportamento relacionados a school class entity. */
@Entity(tableName = "school_classes")
data class SchoolClassEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val name: String,
    val course: String,
    val instructor: String,
    val studentsCount: Int,
    val maxCapacity: Int,
    val schedule: String,
    val status: ClassStatus,
)

/** Modelo e comportamento relacionados a teacher entity. */
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val name: String,
    val email: String,
    val specialty: String,
    val activeCourses: Int,
    val totalStudents: Int,
    val rating: Float,
    val isActive: Boolean,
)

/** Modelo e comportamento relacionados a student entity. */
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val name: String,
    val email: String,
    val course: String,
    val enrolledClass: String,
    val progress: Float,
    val status: StudentStatus,
)

/** Modelo e comportamento relacionados a certificate entity. */
@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val studentName: String,
    val courseName: String,
    val issuedDate: String,
    val hours: Int,
    val code: String,
)

/** Modelo e comportamento relacionados a calendar event entity. */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val title: String,
    val course: String,
    val date: String,
    val time: String,
    val location: String,
    val type: EventType,
)

/** Modelo e comportamento relacionados a recent activity entity. */
@Entity(tableName = "recent_activities")
data class RecentActivityEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val timeLabel: String,
)

/** Modelo e comportamento relacionados a monthly enrollment entity. */
@Entity(tableName = "monthly_enrollments")
data class MonthlyEnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val month: String,
    val count: Int,
)

/** Modelo e comportamento relacionados a attendance entity. */
@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val studentId: Int,
    val date: LocalDate,
    val status: AttendanceStatus,
    val minutesLate: Int,
    val justification: String?,
)

/** Modelo e comportamento relacionados a behavior entity. */
@Entity(tableName = "behavior_records")
data class BehaviorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val studentId: Int,
    val date: LocalDate,
    val participationScore: Int,
    val activityDelivery: ActivityDeliveryStatus,
    val delayMinutes: Int,
    val grade: Float?,
    val note: String,
)

/** Modelo e comportamento relacionados a pedagogical need entity. */
@Entity(tableName = "pedagogical_needs")
data class PedagogicalNeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val studentId: Int,
    val type: PedagogicalNeedType,
    val description: String,
    val expiresAt: LocalDate?,
    val accommodations: List<String>,
)

/** Modelo e comportamento relacionados a psychological need entity. */
@Entity(tableName = "psychological_needs")
data class PsychologicalNeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val studentId: Int,
    val summary: String,
    val confidentiality: ConfidentialityLevel,
    val nextStep: String,
    val reviewAt: LocalDate,
)

/** Modelo e comportamento relacionados a parent follow up entity. */
@Entity(tableName = "parent_follow_ups")
data class ParentFollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val studentId: Int,
    val date: LocalDate,
    val channel: ParentContactChannel,
    val outcome: ParentFollowUpStatus,
    val responsible: String,
    val notes: String,
)

/** Modelo e comportamento relacionados a app settings entity. */
@Entity(tableName = "settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val darkMode: Boolean,
    val pushEnabled: Boolean,
    val emailEnabled: Boolean,
    val language: String = "pt",
)

/** Modelo e comportamento relacionados a app user entity. */
@Entity(tableName = "app_users")
data class AppUserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val role: UserRole,
    val passwordHash: String,
)

/** Modelo e comportamento relacionados a app converters. */
class AppConverters {
  /**
   * Executa a rotina de string to accommodations dentro do contexto deste componente.
   *
   * @param value Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `List<String>`.
   */
  @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()

  @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

  @TypeConverter fun accommodationsToString(items: List<String>): String = items.joinToString("||")

  @TypeConverter
  fun stringToAccommodations(value: String): List<String> =
      if (value.isBlank()) emptyList() else value.split("||")
}

/** Contrato que define operações para app dao. */
@Dao
interface AppDao {

  // ── Company CRUD ──────────────────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCompanies(items: List<CompanyEntity>)

  @Upsert suspend fun upsertCompany(item: CompanyEntity)

  @Query("SELECT * FROM companies ORDER BY name") fun observeCompanies(): Flow<List<CompanyEntity>>

  @Query("SELECT * FROM companies WHERE isActive = 1 ORDER BY name")
  fun observeActiveCompanies(): Flow<List<CompanyEntity>>

  @Query("SELECT * FROM companies WHERE id = :companyId LIMIT 1")
  fun observeCompanyById(companyId: Int): Flow<CompanyEntity?>

  @Query("SELECT * FROM companies ORDER BY name") suspend fun getCompaniesOnce(): List<CompanyEntity>

  @Query("DELETE FROM companies WHERE id = :companyId") suspend fun deleteCompanyById(companyId: Int)

  @Query("DELETE FROM companies") suspend fun clearCompanies()

  // ── UserCompany CRUD ──────────────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserCompanies(items: List<UserCompanyEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserCompany(item: UserCompanyEntity)

  @Query("SELECT c.* FROM companies c INNER JOIN user_companies uc ON c.id = uc.companyId WHERE uc.userId = :userId ORDER BY c.name")
  fun observeCompaniesForUser(userId: Int): Flow<List<CompanyEntity>>

  @Query("SELECT c.* FROM companies c INNER JOIN user_companies uc ON c.id = uc.companyId WHERE uc.userId = :userId ORDER BY c.name")
  suspend fun getCompaniesForUserOnce(userId: Int): List<CompanyEntity>

  @Query("SELECT * FROM user_companies WHERE userId = :userId AND companyId = :companyId LIMIT 1")
  suspend fun getUserCompany(userId: Int, companyId: Int): UserCompanyEntity?

  @Query("SELECT * FROM user_companies WHERE userId = :userId")
  fun observeUserCompanies(userId: Int): Flow<List<UserCompanyEntity>>

  @Query("SELECT userId FROM user_companies WHERE companyId = :companyId")
  fun observeUserIdsByCompany(companyId: Int): Flow<List<Int>>

  @Query("SELECT * FROM user_companies") suspend fun getUserCompaniesOnce(): List<UserCompanyEntity>

  @Query("DELETE FROM user_companies WHERE userId = :userId AND companyId = :companyId")
  suspend fun deleteUserCompany(userId: Int, companyId: Int)

  @Query("DELETE FROM user_companies WHERE userId = :userId")
  suspend fun deleteUserCompaniesForUser(userId: Int)

  @Query("DELETE FROM user_companies WHERE companyId = :companyId")
  suspend fun deleteUserCompaniesForCompany(companyId: Int)

  @Query("DELETE FROM user_companies") suspend fun clearUserCompanies()

  // ── Courses (filtered by company) ─────────────────────────────────────────

  @Query("SELECT COUNT(*) FROM courses WHERE companyId = :companyId")
  suspend fun countCourses(companyId: Int): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourses(items: List<CourseEntity>)

  /**
   * Observa altera??es de course by id e publica atualiza??es reativas.
   *
   * @param courseId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<CourseEntity?> @Insert(onConflict`.
   */
  @Query("SELECT * FROM courses WHERE companyId = :companyId ORDER BY title")
  fun observeCourses(companyId: Int): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  fun observeCourseById(courseId: Int): Flow<CourseEntity?>

  // ── Classes (filtered by company) ─────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClasses(items: List<SchoolClassEntity>)

  @Query("SELECT * FROM school_classes WHERE companyId = :companyId ORDER BY name")
  fun observeClasses(companyId: Int): Flow<List<SchoolClassEntity>>

  @Query("SELECT * FROM school_classes WHERE id = :classId LIMIT 1")
  fun observeClassById(classId: Int): Flow<SchoolClassEntity?>

  @Query("SELECT * FROM school_classes WHERE companyId = :companyId AND course = :courseName ORDER BY name")
  fun observeClassesByCourse(companyId: Int, courseName: String): Flow<List<SchoolClassEntity>>

  @Query("SELECT * FROM school_classes WHERE companyId = :companyId AND instructor = :teacherName ORDER BY name")
  fun observeClassesByTeacher(companyId: Int, teacherName: String): Flow<List<SchoolClassEntity>>

  // ── Teachers (filtered by company) ────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTeachers(items: List<TeacherEntity>)

  @Query("SELECT * FROM teachers WHERE companyId = :companyId ORDER BY name")
  fun observeTeachers(companyId: Int): Flow<List<TeacherEntity>>

  @Query("SELECT * FROM teachers WHERE id = :teacherId LIMIT 1")
  fun observeTeacherById(teacherId: Int): Flow<TeacherEntity?>

  // ── Students (filtered by company) ────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudents(items: List<StudentEntity>)

  @Query("SELECT * FROM students WHERE companyId = :companyId ORDER BY name")
  fun observeStudents(companyId: Int): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
  fun observeStudentById(studentId: Int): Flow<StudentEntity?>

  @Query("SELECT * FROM students WHERE companyId = :companyId AND enrolledClass = :className ORDER BY name")
  fun observeStudentsByClass(companyId: Int, className: String): Flow<List<StudentEntity>>

  // ── Certificates (filtered by company) ────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCertificates(items: List<CertificateEntity>)

  @Query("SELECT * FROM certificates WHERE companyId = :companyId ORDER BY id DESC")
  fun observeCertificates(companyId: Int): Flow<List<CertificateEntity>>

  // ── Calendar Events (filtered by company) ─────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCalendarEvents(items: List<CalendarEventEntity>)

  @Query("SELECT * FROM calendar_events WHERE companyId = :companyId ORDER BY date, time")
  fun observeCalendarEvents(companyId: Int): Flow<List<CalendarEventEntity>>

  // ── Recent Activities (filtered by company) ───────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecentActivities(items: List<RecentActivityEntity>)

  @Query("SELECT * FROM recent_activities WHERE companyId = :companyId ORDER BY id DESC")
  fun observeRecentActivities(companyId: Int): Flow<List<RecentActivityEntity>>

  @Query("SELECT * FROM recent_activities WHERE companyId = :companyId ORDER BY id DESC LIMIT :limit")
  fun observeRecentActivitiesLimited(companyId: Int, limit: Int): Flow<List<RecentActivityEntity>>

  @Query("SELECT * FROM recent_activities WHERE companyId = :companyId ORDER BY id DESC LIMIT :limit OFFSET :offset")
  fun observeRecentActivitiesPaged(companyId: Int, limit: Int, offset: Int): Flow<List<RecentActivityEntity>>

  @Query("SELECT COUNT(*) FROM recent_activities WHERE companyId = :companyId")
  fun observeRecentActivitiesCount(companyId: Int): Flow<Int>

  @Query("SELECT MAX(id) FROM recent_activities") suspend fun getMaxRecentActivityId(): Int?

  // ── Monthly Enrollments (filtered by company) ─────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMonthlyEnrollments(items: List<MonthlyEnrollmentEntity>)

  @Query("SELECT * FROM monthly_enrollments WHERE companyId = :companyId ORDER BY month")
  fun observeMonthlyEnrollments(companyId: Int): Flow<List<MonthlyEnrollmentEntity>>

  // ── Attendance / Behavior / Needs (student-level, filtered by company) ────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttendance(items: List<AttendanceEntity>)

  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
  fun observeAttendance(studentId: Int): Flow<List<AttendanceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBehaviors(items: List<BehaviorEntity>)

  @Query("SELECT * FROM behavior_records WHERE studentId = :studentId ORDER BY date DESC")
  fun observeBehaviors(studentId: Int): Flow<List<BehaviorEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPedagogicalNeeds(items: List<PedagogicalNeedEntity>)

  @Query("SELECT * FROM pedagogical_needs WHERE studentId = :studentId")
  fun observePedagogicalNeeds(studentId: Int): Flow<List<PedagogicalNeedEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPsychologicalNeeds(items: List<PsychologicalNeedEntity>)

  @Query("SELECT * FROM psychological_needs WHERE studentId = :studentId")
  fun observePsychologicalNeeds(studentId: Int): Flow<List<PsychologicalNeedEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertParentFollowUps(items: List<ParentFollowUpEntity>)

  @Query("SELECT * FROM parent_follow_ups WHERE studentId = :studentId ORDER BY date DESC")
  fun observeParentFollowUps(studentId: Int): Flow<List<ParentFollowUpEntity>>

  // ── Aggregate stats (filtered by company) ─────────────────────────────────

  @Query("SELECT COUNT(*) FROM students WHERE companyId = :companyId AND status = :status")
  fun observeStudentsByStatusCount(companyId: Int, status: StudentStatus): Flow<Int>

  @Query("SELECT COUNT(*) FROM courses WHERE companyId = :companyId AND isPublished = 1")
  fun observePublishedCoursesCount(companyId: Int): Flow<Int>

  @Query("SELECT COUNT(*) FROM school_classes WHERE companyId = :companyId")
  fun observeClassesCount(companyId: Int): Flow<Int>

  @Query("SELECT COUNT(*) FROM certificates WHERE companyId = :companyId")
  fun observeCertificatesCount(companyId: Int): Flow<Int>

  @Query("SELECT AVG(completionRate) FROM courses WHERE companyId = :companyId")
  fun observeAverageCompletionRate(companyId: Int): Flow<Float?>

  @Query("SELECT AVG(rating) FROM teachers WHERE companyId = :companyId")
  fun observeAverageTeacherRating(companyId: Int): Flow<Float?>

  // ── Settings ──────────────────────────────────────────────────────────────

  @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
  fun observeSettings(): Flow<AppSettingsEntity?>

  @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
  suspend fun observeSettingsOnce(): AppSettingsEntity?

  @Upsert suspend fun upsertSettings(settings: AppSettingsEntity)

  // ── Users ─────────────────────────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(items: List<AppUserEntity>)

  @Upsert suspend fun upsertUser(item: AppUserEntity)

  @Query("SELECT * FROM app_users ORDER BY name") fun observeUsers(): Flow<List<AppUserEntity>>

  @Query("SELECT * FROM app_users WHERE id = :userId LIMIT 1")
  fun observeUserById(userId: Int): Flow<AppUserEntity?>

  @Query("SELECT * FROM app_users ORDER BY name") suspend fun getUsersOnce(): List<AppUserEntity>

  // ── One-shot reads (for Firebase sync) ────────────────────────────────────

  @Query("SELECT * FROM school_classes ORDER BY id")
  suspend fun getClassesOnce(): List<SchoolClassEntity>

  @Query("SELECT * FROM teachers ORDER BY id") suspend fun getTeachersOnce(): List<TeacherEntity>

  @Query("SELECT * FROM students ORDER BY id") suspend fun getStudentsOnce(): List<StudentEntity>

  @Query("SELECT * FROM certificates ORDER BY id")
  suspend fun getCertificatesOnce(): List<CertificateEntity>

  @Query("SELECT * FROM calendar_events ORDER BY id")
  suspend fun getCalendarEventsOnce(): List<CalendarEventEntity>

  @Query("SELECT * FROM recent_activities ORDER BY id")
  suspend fun getRecentActivitiesOnce(): List<RecentActivityEntity>

  @Query("SELECT * FROM monthly_enrollments ORDER BY id")
  suspend fun getMonthlyEnrollmentsOnce(): List<MonthlyEnrollmentEntity>

  @Query("SELECT * FROM attendance_records ORDER BY id")
  suspend fun getAttendanceOnce(): List<AttendanceEntity>

  @Query("SELECT * FROM behavior_records ORDER BY id")
  suspend fun getBehaviorsOnce(): List<BehaviorEntity>

  @Query("SELECT * FROM pedagogical_needs ORDER BY id")
  suspend fun getPedagogicalNeedsOnce(): List<PedagogicalNeedEntity>

  @Query("SELECT * FROM psychological_needs ORDER BY id")
  suspend fun getPsychologicalNeedsOnce(): List<PsychologicalNeedEntity>

  @Query("SELECT * FROM parent_follow_ups ORDER BY id")
  suspend fun getParentFollowUpsOnce(): List<ParentFollowUpEntity>

  // ── Deletes ───────────────────────────────────────────────────────────────

  @Query("DELETE FROM students WHERE id = :studentId") suspend fun deleteStudentById(studentId: Int)

  @Query("DELETE FROM courses WHERE id = :courseId") suspend fun deleteCourseById(courseId: Int)

  @Query("DELETE FROM school_classes WHERE id = :classId") suspend fun deleteClassById(classId: Int)

  @Query("DELETE FROM teachers WHERE id = :teacherId") suspend fun deleteTeacherById(teacherId: Int)

  @Query("DELETE FROM app_users WHERE id = :userId") suspend fun deleteUserById(userId: Int)

  // ── Clear all ─────────────────────────────────────────────────────────────

  @Query("DELETE FROM courses") suspend fun clearCourses()

  @Query("DELETE FROM school_classes") suspend fun clearClasses()

  @Query("DELETE FROM teachers") suspend fun clearTeachers()

  @Query("DELETE FROM students") suspend fun clearStudents()

  @Query("DELETE FROM certificates") suspend fun clearCertificates()

  @Query("DELETE FROM calendar_events") suspend fun clearCalendarEvents()

  @Query("DELETE FROM recent_activities") suspend fun clearRecentActivities()

  @Query("DELETE FROM monthly_enrollments") suspend fun clearMonthlyEnrollments()

  @Query("DELETE FROM attendance_records") suspend fun clearAttendance()

  @Query("DELETE FROM behavior_records") suspend fun clearBehaviors()

  @Query("DELETE FROM pedagogical_needs") suspend fun clearPedagogicalNeeds()

  @Query("DELETE FROM psychological_needs") suspend fun clearPsychologicalNeeds()

  @Query("DELETE FROM parent_follow_ups") suspend fun clearParentFollowUps()

  @Query("DELETE FROM app_users") suspend fun clearUsers()
}

@Database(
    entities =
        [
            CompanyEntity::class,
            UserCompanyEntity::class,
            CourseEntity::class,
            SchoolClassEntity::class,
            TeacherEntity::class,
            StudentEntity::class,
            CertificateEntity::class,
            CalendarEventEntity::class,
            RecentActivityEntity::class,
            MonthlyEnrollmentEntity::class,
            AttendanceEntity::class,
            BehaviorEntity::class,
            PedagogicalNeedEntity::class,
            PsychologicalNeedEntity::class,
            ParentFollowUpEntity::class,
            AppSettingsEntity::class,
            AppUserEntity::class,
        ],
    version = 7,
    exportSchema = false,
)
/** Modelo e comportamento relacionados a app database. */
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
  /**
   * Executa a rotina de app dao dentro do contexto deste componente.
   *
   * @return Resultado produzido pela opera??o em formato `AppDao }`.
   */
  abstract fun appDao(): AppDao
}
