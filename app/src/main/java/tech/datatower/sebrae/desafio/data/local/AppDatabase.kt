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

/** Modelo e comportamento relacionados a course entity. */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: Int,
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
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val timeLabel: String,
)

/** Modelo e comportamento relacionados a monthly enrollment entity. */
@Entity(tableName = "monthly_enrollments")
data class MonthlyEnrollmentEntity(
    @PrimaryKey val month: String,
    val count: Int,
)

/** Modelo e comportamento relacionados a attendance entity. */
@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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

/** Contrato que define opera??es para app dao. */
@Dao
interface AppDao {
  /**
   * Executa a rotina de insert courses dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Query("SELECT COUNT(*) FROM courses") suspend fun countCourses(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourses(items: List<CourseEntity>)

  /**
   * Observa altera??es de course by id e publica atualiza??es reativas.
   *
   * @param courseId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<CourseEntity?> @Insert(onConflict`.
   */
  @Query("SELECT * FROM courses ORDER BY title") fun observeCourses(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  fun observeCourseById(courseId: Int): Flow<CourseEntity?>

  /**
   * Executa a rotina de insert classes dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClasses(items: List<SchoolClassEntity>)

  /**
   * Observa altera??es de classes e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<SchoolClassEntity>> @Query("SELECT * FROM school_classes WHERE id`.
   */
  @Query("SELECT * FROM school_classes ORDER BY name")
  fun observeClasses(): Flow<List<SchoolClassEntity>>

  /**
   * Observa altera??es de class by id e publica atualiza??es reativas.
   *
   * @param classId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<SchoolClassEntity?> @Query("SELECT *
   *   FROM school_classes WHERE course`.
   */
  @Query("SELECT * FROM school_classes WHERE id = :classId LIMIT 1")
  fun observeClassById(classId: Int): Flow<SchoolClassEntity?>

  /**
   * Observa altera??es de classes by course e publica atualiza??es reativas.
   *
   * @param courseName Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<SchoolClassEntity>> @Query("SELECT * FROM school_classes WHERE instructor`.
   */
  @Query("SELECT * FROM school_classes WHERE course = :courseName ORDER BY name")
  fun observeClassesByCourse(courseName: String): Flow<List<SchoolClassEntity>>

  /**
   * Observa altera??es de classes by teacher e publica atualiza??es reativas.
   *
   * @param teacherName Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<SchoolClassEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM school_classes WHERE instructor = :teacherName ORDER BY name")
  fun observeClassesByTeacher(teacherName: String): Flow<List<SchoolClassEntity>>

  /**
   * Executa a rotina de insert teachers dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTeachers(items: List<TeacherEntity>)

  /**
   * Observa altera??es de teacher by id e publica atualiza??es reativas.
   *
   * @param teacherId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<TeacherEntity?> @Insert(onConflict`.
   */
  @Query("SELECT * FROM teachers ORDER BY name") fun observeTeachers(): Flow<List<TeacherEntity>>

  @Query("SELECT * FROM teachers WHERE id = :teacherId LIMIT 1")
  fun observeTeacherById(teacherId: Int): Flow<TeacherEntity?>

  /**
   * Executa a rotina de insert students dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudents(items: List<StudentEntity>)

  /**
   * Observa altera??es de student by id e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<StudentEntity?> @Query("SELECT *
   *   FROM students WHERE enrolledClass`.
   */
  @Query("SELECT * FROM students ORDER BY name") fun observeStudents(): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
  fun observeStudentById(studentId: Int): Flow<StudentEntity?>

  /**
   * Observa altera??es de students by class e publica atualiza??es reativas.
   *
   * @param className Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<StudentEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM students WHERE enrolledClass = :className ORDER BY name")
  fun observeStudentsByClass(className: String): Flow<List<StudentEntity>>

  /**
   * Executa a rotina de insert certificates dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCertificates(items: List<CertificateEntity>)

  /**
   * Observa altera??es de certificates e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<CertificateEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM certificates ORDER BY id DESC")
  fun observeCertificates(): Flow<List<CertificateEntity>>

  /**
   * Executa a rotina de insert calendar events dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCalendarEvents(items: List<CalendarEventEntity>)

  /**
   * Observa altera??es de calendar events e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<CalendarEventEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM calendar_events ORDER BY date, time")
  fun observeCalendarEvents(): Flow<List<CalendarEventEntity>>

  /**
   * Executa a rotina de insert recent activities dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecentActivities(items: List<RecentActivityEntity>)

  /**
   * Observa altera??es de recent activities e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<RecentActivityEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM recent_activities ORDER BY id DESC")
  fun observeRecentActivities(): Flow<List<RecentActivityEntity>>

  /** Observa apenas as atividades recentes mais novas para uso no painel inicial. */
  @Query("SELECT * FROM recent_activities ORDER BY id DESC LIMIT :limit")
  fun observeRecentActivitiesLimited(limit: Int): Flow<List<RecentActivityEntity>>

  /** Observa uma página específica de atividades recentes (paginação por offset). */
  @Query("SELECT * FROM recent_activities ORDER BY id DESC LIMIT :limit OFFSET :offset")
  fun observeRecentActivitiesPaged(limit: Int, offset: Int): Flow<List<RecentActivityEntity>>

  /** Observa o total de atividades recentes de forma reativa. */
  @Query("SELECT COUNT(*) FROM recent_activities") fun observeRecentActivitiesCount(): Flow<Int>

  /** Retorna o maior identificador atual de atividade recente. */
  @Query("SELECT MAX(id) FROM recent_activities") suspend fun getMaxRecentActivityId(): Int?

  /**
   * Executa a rotina de insert monthly enrollments dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMonthlyEnrollments(items: List<MonthlyEnrollmentEntity>)

  /**
   * Observa altera??es de monthly enrollments e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<MonthlyEnrollmentEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM monthly_enrollments ORDER BY rowid")
  fun observeMonthlyEnrollments(): Flow<List<MonthlyEnrollmentEntity>>

  /**
   * Executa a rotina de insert attendance dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttendance(items: List<AttendanceEntity>)

  /**
   * Observa altera??es de attendance e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<AttendanceEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
  fun observeAttendance(studentId: Int): Flow<List<AttendanceEntity>>

  /**
   * Executa a rotina de insert behaviors dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBehaviors(items: List<BehaviorEntity>)

  /**
   * Observa altera??es de behaviors e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<BehaviorEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM behavior_records WHERE studentId = :studentId ORDER BY date DESC")
  fun observeBehaviors(studentId: Int): Flow<List<BehaviorEntity>>

  /**
   * Executa a rotina de insert pedagogical needs dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPedagogicalNeeds(items: List<PedagogicalNeedEntity>)

  /**
   * Observa altera??es de pedagogical needs e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<PedagogicalNeedEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM pedagogical_needs WHERE studentId = :studentId")
  fun observePedagogicalNeeds(studentId: Int): Flow<List<PedagogicalNeedEntity>>

  /**
   * Executa a rotina de insert psychological needs dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPsychologicalNeeds(items: List<PsychologicalNeedEntity>)

  /**
   * Observa altera??es de psychological needs e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<PsychologicalNeedEntity>> @Insert(onConflict`.
   */
  @Query("SELECT * FROM psychological_needs WHERE studentId = :studentId")
  fun observePsychologicalNeeds(studentId: Int): Flow<List<PsychologicalNeedEntity>>

  /**
   * Executa a rotina de insert parent follow ups dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertParentFollowUps(items: List<ParentFollowUpEntity>)

  /**
   * Observa altera??es de parent follow ups e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `Flow<List<ParentFollowUpEntity>> @Query("SELECT COUNT(*) FROM students WHERE status`.
   */
  @Query("SELECT * FROM parent_follow_ups WHERE studentId = :studentId ORDER BY date DESC")
  fun observeParentFollowUps(studentId: Int): Flow<List<ParentFollowUpEntity>>

  /**
   * Observa altera??es de students by status count e publica atualiza??es reativas.
   *
   * @param status Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<Int> @Query("SELECT COUNT(*) FROM
   *   courses WHERE isPublished`.
   */
  @Query("SELECT COUNT(*) FROM students WHERE status = :status")
  fun observeStudentsByStatusCount(status: StudentStatus): Flow<Int>

  /**
   * Observa altera??es de published courses count e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<Int> @Query("SELECT COUNT(*) FROM
   *   school_classes") fun observeClassesCount(): Flow<Int> @Query("SELECT COUNT(*) FROM
   *   certificates") fun observeCertificatesCount(): Flow<Int> @Query("SELECT AVG(completionRate)
   *   FROM courses") fun observeAverageCompletionRate(): Flow<Float?> @Query("SELECT AVG(rating)
   *   FROM teachers") fun observeAverageTeacherRating(): Flow<Float?> @Query("SELECT * FROM
   *   settings WHERE id`.
   */
  @Query("SELECT COUNT(*) FROM courses WHERE isPublished = 1")
  fun observePublishedCoursesCount(): Flow<Int>

  /**
   * Observa altera??es de settings e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<AppSettingsEntity?> @Query("SELECT *
   *   FROM settings WHERE id`.
   */
  @Query("SELECT COUNT(*) FROM school_classes") fun observeClassesCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM certificates") fun observeCertificatesCount(): Flow<Int>

  @Query("SELECT AVG(completionRate) FROM courses") fun observeAverageCompletionRate(): Flow<Float?>

  @Query("SELECT AVG(rating) FROM teachers") fun observeAverageTeacherRating(): Flow<Float?>

  @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
  fun observeSettings(): Flow<AppSettingsEntity?>

  /**
   * Observa altera??es de settings once e publica atualiza??es reativas.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `AppSettingsEntity? @Upsert suspend fun
   *   upsertSettings(settings: AppSettingsEntity)`.
   */
  @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
  suspend fun observeSettingsOnce(): AppSettingsEntity?

  /**
   * Executa a rotina de insert users dentro do contexto deste componente.
   *
   * @param items Valor de entrada utilizado por esta opera??o.
   */
  @Upsert suspend fun upsertSettings(settings: AppSettingsEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(items: List<AppUserEntity>)

  /**
   * Obt?m dados necess?rios para classes once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `List<SchoolClassEntity> @Query("SELECT *
   *   FROM teachers ORDER BY id") suspend fun getTeachersOnce():
   *   List<TeacherEntity> @Query("SELECT * FROM students ORDER BY id") suspend fun
   *   getStudentsOnce(): List<StudentEntity> @Query("SELECT * FROM certificates ORDER BY id")`.
   */
  @Upsert suspend fun upsertUser(item: AppUserEntity)

  @Query("SELECT * FROM app_users ORDER BY name") fun observeUsers(): Flow<List<AppUserEntity>>

  @Query("SELECT * FROM app_users WHERE id = :userId LIMIT 1")
  fun observeUserById(userId: Int): Flow<AppUserEntity?>

  @Query("SELECT * FROM app_users ORDER BY name") suspend fun getUsersOnce(): List<AppUserEntity>

  @Query("SELECT * FROM school_classes ORDER BY id")
  suspend fun getClassesOnce(): List<SchoolClassEntity>

  /**
   * Obt?m dados necess?rios para certificates once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `List<CertificateEntity> @Query("SELECT *
   *   FROM calendar_events ORDER BY id")`.
   */
  @Query("SELECT * FROM teachers ORDER BY id") suspend fun getTeachersOnce(): List<TeacherEntity>

  @Query("SELECT * FROM students ORDER BY id") suspend fun getStudentsOnce(): List<StudentEntity>

  @Query("SELECT * FROM certificates ORDER BY id")
  suspend fun getCertificatesOnce(): List<CertificateEntity>

  /**
   * Obt?m dados necess?rios para calendar events once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `List<CalendarEventEntity> @Query("SELECT * FROM recent_activities ORDER BY id")`.
   */
  @Query("SELECT * FROM calendar_events ORDER BY id")
  suspend fun getCalendarEventsOnce(): List<CalendarEventEntity>

  /**
   * Obt?m dados necess?rios para recent activities once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `List<RecentActivityEntity> @Query("SELECT * FROM monthly_enrollments ORDER BY rowid")`.
   */
  @Query("SELECT * FROM recent_activities ORDER BY id")
  suspend fun getRecentActivitiesOnce(): List<RecentActivityEntity>

  /**
   * Obt?m dados necess?rios para monthly enrollments once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `List<MonthlyEnrollmentEntity> @Query("SELECT * FROM attendance_records ORDER BY id")`.
   */
  @Query("SELECT * FROM monthly_enrollments ORDER BY rowid")
  suspend fun getMonthlyEnrollmentsOnce(): List<MonthlyEnrollmentEntity>

  /**
   * Obt?m dados necess?rios para attendance once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `List<AttendanceEntity> @Query("SELECT *
   *   FROM behavior_records ORDER BY id")`.
   */
  @Query("SELECT * FROM attendance_records ORDER BY id")
  suspend fun getAttendanceOnce(): List<AttendanceEntity>

  /**
   * Obt?m dados necess?rios para behaviors once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `List<BehaviorEntity> @Query("SELECT *
   *   FROM pedagogical_needs ORDER BY id")`.
   */
  @Query("SELECT * FROM behavior_records ORDER BY id")
  suspend fun getBehaviorsOnce(): List<BehaviorEntity>

  /**
   * Obt?m dados necess?rios para pedagogical needs once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `List<PedagogicalNeedEntity> @Query("SELECT * FROM psychological_needs ORDER BY id")`.
   */
  @Query("SELECT * FROM pedagogical_needs ORDER BY id")
  suspend fun getPedagogicalNeedsOnce(): List<PedagogicalNeedEntity>

  /**
   * Obt?m dados necess?rios para psychological needs once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato
   *   `List<PsychologicalNeedEntity> @Query("SELECT * FROM parent_follow_ups ORDER BY id")`.
   */
  @Query("SELECT * FROM psychological_needs ORDER BY id")
  suspend fun getPsychologicalNeedsOnce(): List<PsychologicalNeedEntity>

  /**
   * Obt?m dados necess?rios para parent follow ups once de forma consistente.
   *
   * @param ) Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `List<ParentFollowUpEntity> } @Database(
   *   entities`.
   */
  @Query("SELECT * FROM parent_follow_ups ORDER BY id")
  suspend fun getParentFollowUpsOnce(): List<ParentFollowUpEntity>

  /** Remove um aluno específico do armazenamento local. */
  @Query("DELETE FROM students WHERE id = :studentId") suspend fun deleteStudentById(studentId: Int)

  /** Remove um curso específico do armazenamento local. */
  @Query("DELETE FROM courses WHERE id = :courseId") suspend fun deleteCourseById(courseId: Int)

  /** Remove uma turma específica do armazenamento local. */
  @Query("DELETE FROM school_classes WHERE id = :classId") suspend fun deleteClassById(classId: Int)

  /** Remove um instrutor específico do armazenamento local. */
  @Query("DELETE FROM teachers WHERE id = :teacherId") suspend fun deleteTeacherById(teacherId: Int)

  /** Limpa dados operacionais do app preservando usuários para evitar perda de acesso. */
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

  /** Remove todos os usuários cadastrados do armazenamento local (usado no reset completo). */
  @Query("DELETE FROM app_users") suspend fun clearUsers()

  /** Remove um usuário específico do armazenamento local pelo seu ID. */
  @Query("DELETE FROM app_users WHERE id = :userId") suspend fun deleteUserById(userId: Int)
}

@Database(
    entities =
        [
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
    version = 6,
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
