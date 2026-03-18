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
import java.time.LocalDate
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

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val specialty: String,
    val activeCourses: Int,
    val totalStudents: Int,
    val rating: Float,
)

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

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val id: Int,
    val studentName: String,
    val courseName: String,
    val issuedDate: String,
    val hours: Int,
    val code: String,
)

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

@Entity(tableName = "recent_activities")
data class RecentActivityEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val timeLabel: String,
)

@Entity(tableName = "monthly_enrollments")
data class MonthlyEnrollmentEntity(
    @PrimaryKey val month: String,
    val count: Int,
)

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: LocalDate,
    val status: AttendanceStatus,
    val minutesLate: Int,
    val justification: String?,
)

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

@Entity(tableName = "pedagogical_needs")
data class PedagogicalNeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val type: PedagogicalNeedType,
    val description: String,
    val expiresAt: LocalDate?,
    val accommodations: List<String>,
)

@Entity(tableName = "psychological_needs")
data class PsychologicalNeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val summary: String,
    val confidentiality: ConfidentialityLevel,
    val nextStep: String,
    val reviewAt: LocalDate,
)

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

@Entity(tableName = "settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val darkMode: Boolean,
    val pushEnabled: Boolean,
    val emailEnabled: Boolean,
)

class AppConverters {
  @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()

  @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

  @TypeConverter fun accommodationsToString(items: List<String>): String = items.joinToString("||")

  @TypeConverter
  fun stringToAccommodations(value: String): List<String> =
      if (value.isBlank()) emptyList() else value.split("||")
}

@Dao
interface AppDao {
  @Query("SELECT COUNT(*) FROM courses") suspend fun countCourses(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourses(items: List<CourseEntity>)

  @Query("SELECT * FROM courses ORDER BY title") fun observeCourses(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  fun observeCourseById(courseId: Int): Flow<CourseEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClasses(items: List<SchoolClassEntity>)

  @Query("SELECT * FROM school_classes ORDER BY name")
  fun observeClasses(): Flow<List<SchoolClassEntity>>

  @Query("SELECT * FROM school_classes WHERE id = :classId LIMIT 1")
  fun observeClassById(classId: Int): Flow<SchoolClassEntity?>

  @Query("SELECT * FROM school_classes WHERE course = :courseName ORDER BY name")
  fun observeClassesByCourse(courseName: String): Flow<List<SchoolClassEntity>>

  @Query("SELECT * FROM school_classes WHERE instructor = :teacherName ORDER BY name")
  fun observeClassesByTeacher(teacherName: String): Flow<List<SchoolClassEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTeachers(items: List<TeacherEntity>)

  @Query("SELECT * FROM teachers ORDER BY name") fun observeTeachers(): Flow<List<TeacherEntity>>

  @Query("SELECT * FROM teachers WHERE id = :teacherId LIMIT 1")
  fun observeTeacherById(teacherId: Int): Flow<TeacherEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudents(items: List<StudentEntity>)

  @Query("SELECT * FROM students ORDER BY name") fun observeStudents(): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
  fun observeStudentById(studentId: Int): Flow<StudentEntity?>

  @Query("SELECT * FROM students WHERE enrolledClass = :className ORDER BY name")
  fun observeStudentsByClass(className: String): Flow<List<StudentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCertificates(items: List<CertificateEntity>)

  @Query("SELECT * FROM certificates ORDER BY id DESC")
  fun observeCertificates(): Flow<List<CertificateEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCalendarEvents(items: List<CalendarEventEntity>)

  @Query("SELECT * FROM calendar_events ORDER BY date, time")
  fun observeCalendarEvents(): Flow<List<CalendarEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecentActivities(items: List<RecentActivityEntity>)

  @Query("SELECT * FROM recent_activities ORDER BY id")
  fun observeRecentActivities(): Flow<List<RecentActivityEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMonthlyEnrollments(items: List<MonthlyEnrollmentEntity>)

  @Query("SELECT * FROM monthly_enrollments ORDER BY rowid")
  fun observeMonthlyEnrollments(): Flow<List<MonthlyEnrollmentEntity>>

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

  @Query("SELECT COUNT(*) FROM students WHERE status = :status")
  fun observeStudentsByStatusCount(status: StudentStatus): Flow<Int>

  @Query("SELECT COUNT(*) FROM courses WHERE isPublished = 1")
  fun observePublishedCoursesCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM school_classes") fun observeClassesCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM certificates") fun observeCertificatesCount(): Flow<Int>

  @Query("SELECT AVG(completionRate) FROM courses") fun observeAverageCompletionRate(): Flow<Float?>

  @Query("SELECT AVG(rating) FROM teachers") fun observeAverageTeacherRating(): Flow<Float?>

  @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
  fun observeSettings(): Flow<AppSettingsEntity?>

  @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
  suspend fun observeSettingsOnce(): AppSettingsEntity?

  @Upsert suspend fun upsertSettings(settings: AppSettingsEntity)
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
        ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun appDao(): AppDao
}
