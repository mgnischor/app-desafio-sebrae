package tech.datatower.sebrae.desafio.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.AppDatabase
import tech.datatower.sebrae.desafio.data.local.AppSettingsEntity
import tech.datatower.sebrae.desafio.data.local.AppUserEntity
import tech.datatower.sebrae.desafio.data.local.AttendanceEntity
import tech.datatower.sebrae.desafio.data.local.BehaviorEntity
import tech.datatower.sebrae.desafio.data.local.CalendarEventEntity
import tech.datatower.sebrae.desafio.data.local.CertificateEntity
import tech.datatower.sebrae.desafio.data.local.CourseEntity
import tech.datatower.sebrae.desafio.data.local.MonthlyEnrollmentEntity
import tech.datatower.sebrae.desafio.data.local.ParentFollowUpEntity
import tech.datatower.sebrae.desafio.data.local.PedagogicalNeedEntity
import tech.datatower.sebrae.desafio.data.local.PsychologicalNeedEntity
import tech.datatower.sebrae.desafio.data.local.RecentActivityEntity
import tech.datatower.sebrae.desafio.data.local.SchoolClassEntity
import tech.datatower.sebrae.desafio.data.local.StudentEntity
import tech.datatower.sebrae.desafio.data.local.TeacherEntity
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AppSettings
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.Certificate
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUp
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeed
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.PsychologicalNeed
import tech.datatower.sebrae.desafio.data.model.QuickStat
import tech.datatower.sebrae.desafio.data.model.RecentActivity
import tech.datatower.sebrae.desafio.data.model.RelationshipRules
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import java.security.MessageDigest
import java.time.LocalDate

/** Modelo e comportamento relacionados a report summary. */
data class ReportSummary(
    val activeStudents: Int,
    val activeCourses: Int,
    val totalClasses: Int,
    val completionRate: Float,
    val certificates: Int,
    val averageTeacherRating: Float,
)

/** Modelo e comportamento relacionados a course completion metric. */
data class CourseCompletionMetric(
    val name: String,
    val rate: Float,
)

/** Modelo e comportamento relacionados a monthly enrollment metric. */
data class MonthlyEnrollmentMetric(
    val month: String,
    val count: Int,
)

/** Centraliza opera??es de dados relacionadas a app. */
class AppRepository(
    private val database: AppDatabase,
    private val dao: AppDao,
    private val dataSourceLabelResFlow: Flow<Int> = flowOf(R.string.stat_data_source),
) {
  /**
   * Observa altera??es de courses e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<Course>>`.
   */
  fun observeCourses(): Flow<List<Course>> =
      combine(dao.observeCourses(), dao.observeStudents()) { courseEntities, studentEntities ->
        val students = studentEntities.map { it.toModel() }
        val realByCourse = RelationshipRules.realStudentsByCourse(students)
        courseEntities.map { entity ->
          val model = entity.toModel()
          model.copy(totalStudents = realByCourse[model.title.trim()].orZero())
        }
      }

  /**
   * Observa altera??es de course by id e publica atualiza??es reativas.
   *
   * @param courseId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<Course?>`.
   */
  fun observeCourseById(courseId: Int): Flow<Course?> =
      combine(dao.observeCourseById(courseId), dao.observeStudents()) {
          courseEntity,
          studentEntities ->
        val model = courseEntity?.toModel() ?: return@combine null
        val realByCourse =
            RelationshipRules.realStudentsByCourse(studentEntities.map { it.toModel() })
        model.copy(totalStudents = realByCourse[model.title.trim()].orZero())
      }

  /**
   * Observa altera??es de classes e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<SchoolClass>>`.
   */
  fun observeClasses(): Flow<List<SchoolClass>> =
      combine(dao.observeClasses(), dao.observeStudents()) { classEntities, studentEntities ->
        val students = studentEntities.map { it.toModel() }
        val realByClass = RelationshipRules.realStudentsByClass(students)
        classEntities.map { entity ->
          val model = entity.toModel()
          model.copy(studentsCount = realByClass[model.name.trim()].orZero())
        }
      }

  /**
   * Observa altera??es de class by id e publica atualiza??es reativas.
   *
   * @param classId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<SchoolClass?>`.
   */
  fun observeClassById(classId: Int): Flow<SchoolClass?> =
      combine(dao.observeClassById(classId), dao.observeStudents()) { classEntity, studentEntities
        ->
        val model = classEntity?.toModel() ?: return@combine null
        val realByClass =
            RelationshipRules.realStudentsByClass(studentEntities.map { it.toModel() })
        model.copy(studentsCount = realByClass[model.name.trim()].orZero())
      }

  /**
   * Observa altera??es de classes by course e publica atualiza??es reativas.
   *
   * @param courseName Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<List<SchoolClass>>`.
   */
  fun observeClassesByCourse(courseName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByCourse(courseName).map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de classes by teacher e publica atualiza??es reativas.
   *
   * @param teacherName Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<List<SchoolClass>>`.
   */
  fun observeClassesByTeacher(teacherName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByTeacher(teacherName).map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de teachers e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<Teacher>>`.
   */
  fun observeTeachers(): Flow<List<Teacher>> =
      dao.observeTeachers().map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de teacher by id e publica atualiza??es reativas.
   *
   * @param teacherId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<Teacher?>`.
   */
  fun observeTeacherById(teacherId: Int): Flow<Teacher?> =
      dao.observeTeacherById(teacherId).map { it?.toModel() }

  /**
   * Observa altera??es de students e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<Student>>`.
   */
  fun observeStudents(): Flow<List<Student>> =
      dao.observeStudents().map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de students by class e publica atualiza??es reativas.
   *
   * @param className Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<List<Student>>`.
   */
  fun observeStudentsByClass(className: String): Flow<List<Student>> =
      dao.observeStudentsByClass(className).map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de certificates e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<Certificate>>`.
   */
  fun observeCertificates(): Flow<List<Certificate>> =
      dao.observeCertificates().map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de calendar events e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<CalendarEvent>>`.
   */
  fun observeCalendarEvents(): Flow<List<CalendarEvent>> =
      dao.observeCalendarEvents().map { items -> items.map { it.toModel() } }

  /**
   * Observa altera??es de recent activities e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<RecentActivity>>`.
   */
  fun observeRecentActivities(): Flow<List<RecentActivity>> =
      dao.observeRecentActivities().map { items -> items.map { it.toModel() } }

  /** Observa atividades recentes com limite máximo de itens para telas de resumo. */
  fun observeRecentActivities(limit: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesLimited(limit).map { items -> items.map { it.toModel() } }

  /** Observa atividades recentes paginadas para telas com histórico completo. */
  fun observeRecentActivitiesPaged(limit: Int, offset: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesPaged(limit, offset).map { items -> items.map { it.toModel() } }

  /** Observa a quantidade total de atividades recentes disponível no banco local. */
  fun observeRecentActivitiesCount(): Flow<Int> = dao.observeRecentActivitiesCount()

  /**
   * Observa alterações de home quick stats e publica atualizações reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<QuickStat>>`.
   */
  fun observeHomeQuickStats(): Flow<List<QuickStat>> =
      combine(
          dao.observeStudentsByStatusCount(StudentStatus.Active),
          dao.observePublishedCoursesCount(),
          dao.observeClassesCount(),
          dao.observeAverageCompletionRate(),
          dataSourceLabelResFlow,
      ) { activeStudents, publishedCourses, classesCount, completionRate, dataSourceLabelRes ->
        listOf(
            QuickStat(
                labelRes = R.string.stat_students,
                value = activeStudents.toString(),
                trendLabelRes = dataSourceLabelRes,
            ),
            QuickStat(labelRes = R.string.stat_courses, value = publishedCourses.toString()),
            QuickStat(labelRes = R.string.stat_classes, value = classesCount.toString()),
            QuickStat(
                labelRes = R.string.stat_completion,
                value = "${((completionRate ?: 0f) * 100).toInt()}%",
                progress = completionRate ?: 0f,
            ),
        )
      }

  /**
   * Observa altera??es de report summary e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<ReportSummary>`.
   */
  fun observeReportSummary(): Flow<ReportSummary> =
      combine(
          combine(
              dao.observeStudentsByStatusCount(StudentStatus.Active),
              dao.observePublishedCoursesCount(),
          ) { activeStudents, activeCourses ->
            activeStudents to activeCourses
          },
          combine(
              dao.observeClassesCount(),
              dao.observeAverageCompletionRate(),
          ) { classesCount, completionRate ->
            classesCount to completionRate
          },
          combine(
              dao.observeCertificatesCount(),
              dao.observeAverageTeacherRating(),
          ) { certificates, rating ->
            certificates to rating
          },
      ) { studentsCourses, classesCompletion, certificatesRating ->
        ReportSummary(
            activeStudents = studentsCourses.first,
            activeCourses = studentsCourses.second,
            totalClasses = classesCompletion.first,
            completionRate = classesCompletion.second ?: 0f,
            certificates = certificatesRating.first,
            averageTeacherRating = certificatesRating.second ?: 0f,
        )
      }

  /**
   * Observa altera??es de course completion metrics e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<CourseCompletionMetric>>`.
   */
  fun observeCourseCompletionMetrics(): Flow<List<CourseCompletionMetric>> =
      dao.observeCourses().map { courses ->
        courses
            .map { CourseCompletionMetric(name = it.title, rate = it.completionRate) }
            .sortedByDescending { it.rate }
      }

  /**
   * Observa altera??es de monthly enrollment metrics e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<List<MonthlyEnrollmentMetric>>`.
   */
  fun observeMonthlyEnrollmentMetrics(): Flow<List<MonthlyEnrollmentMetric>> =
      dao.observeMonthlyEnrollments().map { items ->
        items.map { MonthlyEnrollmentMetric(month = it.month, count = it.count) }
      }

  /**
   * Observa altera??es de student monitoring snapshot e publica atualiza??es reativas.
   *
   * @param studentId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<StudentMonitoringSnapshot?>`.
   */
  fun observeStudentMonitoringSnapshot(studentId: Int): Flow<StudentMonitoringSnapshot?> =
      combine(
          dao.observeStudentById(studentId),
          combine(
              dao.observeAttendance(studentId),
              dao.observeBehaviors(studentId),
          ) { attendance, behavior ->
            attendance to behavior
          },
          combine(
              dao.observePedagogicalNeeds(studentId),
              dao.observePsychologicalNeeds(studentId),
          ) { pedagogical, psychological ->
            pedagogical to psychological
          },
          dao.observeParentFollowUps(studentId),
      ) { student, attendanceBehavior, needs, parent ->
        student?.let {
          StudentMonitoringSnapshot(
              studentId = it.id,
              studentName = it.name,
              enrolledClass = it.enrolledClass,
              attendanceRecords = attendanceBehavior.first.map { record -> record.toModel() },
              behaviorRecords = attendanceBehavior.second.map { record -> record.toModel() },
              pedagogicalNeeds = needs.first.map { need -> need.toModel() },
              psychologicalNeeds = needs.second.map { need -> need.toModel() },
              parentFollowUps = parent.map { followUp -> followUp.toModel() },
          )
        }
      }

  /**
   * Observa altera??es de settings e publica atualiza??es reativas.
   *
   * @return Resultado produzido pela opera??o em formato `Flow<AppSettings>`.
   */
  fun observeSettings(): Flow<AppSettings> =
      dao.observeSettings().map {
        if (it == null) {
          AppSettings(darkMode = false, pushEnabled = true, emailEnabled = false, language = "pt")
        } else {
          AppSettings(
              darkMode = it.darkMode,
              pushEnabled = it.pushEnabled,
              emailEnabled = it.emailEnabled,
              language = it.language,
          )
        }
      }

  /**
   * Observa altera??es de registered users for admin e publica atualiza??es reativas.
   *
   * @param requester Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `Flow<List<AppUser>>`.
   */
  fun observeRegisteredUsersForAdmin(requester: AppUser?): Flow<List<AppUser>> {
    return if (requester?.role == UserRole.ADMINISTRADOR) {
      dao.observeUsers().map { items -> items.map { it.toModel() } }
    } else {
      flowOf(emptyList())
    }
  }

  /**
   * Executa a rotina de upsert registered user for admin dentro do contexto deste componente.
   *
   * @param requester Valor de entrada utilizado por esta opera??o.
   * @param user Valor de entrada utilizado por esta opera??o.
   * @param plainPassword Valor de entrada utilizado por esta opera??o.
   */
  suspend fun upsertRegisteredUserForAdmin(
      requester: AppUser?,
      user: AppUser,
      plainPassword: String,
  ) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode cadastrar usuarios.")
    }
    if (plainPassword.isBlank()) {
      throw IllegalArgumentException("Senha do usuario nao pode ser vazia.")
    }
    dao.upsertUser(user.toEntity(passwordHash = sha256(plainPassword)))
  }

  /**
   * Executa a rotina de update dark mode dentro do contexto deste componente.
   *
   * @param enabled Valor de entrada utilizado por esta opera??o.
   */
  suspend fun updateDarkMode(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(darkMode = enabled))
  }

  /**
   * Executa a rotina de update push enabled dentro do contexto deste componente.
   *
   * @param enabled Valor de entrada utilizado por esta opera??o.
   */
  suspend fun updatePushEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(pushEnabled = enabled))
  }

  /**
   * Executa a rotina de update email enabled dentro do contexto deste componente.
   *
   * @param enabled Valor de entrada utilizado por esta opera??o.
   */
  suspend fun updateEmailEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(emailEnabled = enabled))
  }

  /**
   * Executa a rotina de update language dentro do contexto deste componente.
   *
   * @param language Valor de entrada utilizado por esta opera??o.
   */
  suspend fun updateLanguage(language: String) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(language = language))
  }

  /** Limpa dados operacionais do armazenamento local sem apagar usuários cadastrados. */
  suspend fun clearStoragePreservingUsers() {
    database.withTransaction {
      dao.clearParentFollowUps()
      dao.clearPsychologicalNeeds()
      dao.clearPedagogicalNeeds()
      dao.clearBehaviors()
      dao.clearAttendance()
      dao.clearMonthlyEnrollments()
      dao.clearRecentActivities()
      dao.clearCalendarEvents()
      dao.clearCertificates()
      dao.clearStudents()
      dao.clearTeachers()
      dao.clearClasses()
      dao.clearCourses()
      dao.upsertSettings(defaultSettingsEntity())
    }
  }

  /** Executa a rotina de seed if empty dentro do contexto deste componente. */
  suspend fun seedIfEmpty() {
    if (dao.countCourses() > 0) return

    database.withTransaction {
      dao.insertCourses(
          listOf(
              CourseEntity(
                  1,
                  "Marketing Digital",
                  "Negócios",
                  "Profa. Helena",
                  320,
                  40,
                  0.82f,
                  true,
              ),
              CourseEntity(
                  2,
                  "Excel para Negócios",
                  "Tecnologia",
                  "Prof. André",
                  210,
                  20,
                  0.60f,
                  true,
              ),
              CourseEntity(3, "Empreendedorismo", "Gestão", "Profa. Carla", 180, 30, 0.91f, true),
              CourseEntity(
                  4,
                  "Finanças Pessoais",
                  "Finanças",
                  "Prof. Roberto",
                  95,
                  16,
                  0.45f,
                  false,
              ),
              CourseEntity(
                  5,
                  "Design Gráfico",
                  "Criatividade",
                  "Profa. Bianca",
                  140,
                  24,
                  0.70f,
                  true,
              ),
              CourseEntity(
                  6,
                  "Vendas e Negociação",
                  "Negócios",
                  "Prof. Sérgio",
                  260,
                  12,
                  0.55f,
                  true,
              ),
          )
      )
      dao.insertUsers(
          listOf(
              AppUserEntity(
                  id = 1,
                  name = "Prof. Carlos Silva",
                  email = "professor@sebrae.edu.br",
                  role = UserRole.PROFESSOR,
                  passwordHash = sha256("prof123"),
              ),
              AppUserEntity(
                  id = 2,
                  name = "Coord. Ana Santos",
                  email = "coordenador@sebrae.edu.br",
                  role = UserRole.COORDENADOR,
                  passwordHash = sha256("coord123"),
              ),
              AppUserEntity(
                  id = 3,
                  name = "Admin. Joao Almeida",
                  email = "admin@sebrae.edu.br",
                  role = UserRole.ADMINISTRADOR,
                  passwordHash = sha256("admin123"),
              ),
          )
      )
      dao.insertClasses(
          listOf(
              SchoolClassEntity(
                  1,
                  "Turma A1",
                  "Marketing Digital",
                  "Profa. Helena",
                  28,
                  30,
                  "Seg/Qua 18h–20h",
                  ClassStatus.InProgress,
              ),
              SchoolClassEntity(
                  2,
                  "Turma B3",
                  "Excel para Negócios",
                  "Prof. André",
                  22,
                  25,
                  "Ter/Qui 19h–21h",
                  ClassStatus.InProgress,
              ),
              SchoolClassEntity(
                  3,
                  "Turma C2",
                  "Empreendedorismo",
                  "Profa. Carla",
                  18,
                  20,
                  "Sex 08h–12h",
                  ClassStatus.Open,
              ),
              SchoolClassEntity(
                  4,
                  "Turma A2",
                  "Finanças Pessoais",
                  "Prof. Roberto",
                  10,
                  20,
                  "Sáb 09h–12h",
                  ClassStatus.Open,
              ),
              SchoolClassEntity(
                  5,
                  "Turma D1",
                  "Design Gráfico",
                  "Profa. Bianca",
                  30,
                  30,
                  "Seg/Qua 14h–16h",
                  ClassStatus.Closed,
              ),
          )
      )
      dao.insertTeachers(
          listOf(
              TeacherEntity(
                  1,
                  "Profa. Helena Martins",
                  "helena@inst.com",
                  "Marketing & Comunicação",
                  3,
                  320,
                  4.9f,
                  true,
              ),
              TeacherEntity(
                  2,
                  "Prof. André Nunes",
                  "andre@inst.com",
                  "Tecnologia da Informação",
                  2,
                  210,
                  4.7f,
                  true,
              ),
              TeacherEntity(
                  3,
                  "Profa. Carla Faria",
                  "carla@inst.com",
                  "Empreendedorismo & Gestão",
                  2,
                  180,
                  4.8f,
                  true,
              ),
              TeacherEntity(
                  4,
                  "Prof. Roberto Lima",
                  "roberto@inst.com",
                  "Finanças & Contabilidade",
                  1,
                  95,
                  4.5f,
                  true,
              ),
              TeacherEntity(
                  5,
                  "Profa. Bianca Torres",
                  "bianca@inst.com",
                  "Design & Criatividade",
                  1,
                  140,
                  4.6f,
                  true,
              ),
              TeacherEntity(
                  6,
                  "Prof. Sérgio Campos",
                  "sergio@inst.com",
                  "Vendas & Negociação",
                  1,
                  260,
                  4.4f,
                  true,
              ),
          )
      )
      dao.insertStudents(
          listOf(
              StudentEntity(
                  1,
                  "Ana Lima",
                  "ana.lima@email.com",
                  "Marketing Digital",
                  "Turma A1",
                  0.87f,
                  StudentStatus.Active,
              ),
              StudentEntity(
                  2,
                  "Carlos Souza",
                  "carlos@email.com",
                  "Excel para Negócios",
                  "Turma B3",
                  0.42f,
                  StudentStatus.Active,
              ),
              StudentEntity(
                  3,
                  "Fernanda Costa",
                  "fernanda@email.com",
                  "Empreendedorismo",
                  "Turma C2",
                  0.95f,
                  StudentStatus.Graduated,
              ),
              StudentEntity(
                  4,
                  "Ricardo Alves",
                  "ricardo@email.com",
                  "Finanças Pessoais",
                  "Turma A2",
                  0.20f,
                  StudentStatus.Inactive,
              ),
              StudentEntity(
                  5,
                  "Mariana Pereira",
                  "mariana@email.com",
                  "Marketing Digital",
                  "Turma A1",
                  0.65f,
                  StudentStatus.Active,
              ),
              StudentEntity(
                  6,
                  "Lucas Oliveira",
                  "lucas@email.com",
                  "Excel para Negócios",
                  "Turma B3",
                  0.78f,
                  StudentStatus.Active,
              ),
              StudentEntity(
                  7,
                  "Juliana Santos",
                  "juliana@email.com",
                  "Empreendedorismo",
                  "Turma C2",
                  1.0f,
                  StudentStatus.Graduated,
              ),
              StudentEntity(
                  8,
                  "Pedro Rodrigues",
                  "pedro@email.com",
                  "Finanças Pessoais",
                  "Turma A2",
                  0.10f,
                  StudentStatus.Active,
              ),
          )
      )
      dao.insertCertificates(
          listOf(
              CertificateEntity(
                  1,
                  "Ana Lima",
                  "Marketing Digital",
                  "15/02/2026",
                  40,
                  "CERT-2026-0148",
              ),
              CertificateEntity(
                  2,
                  "Fernanda Costa",
                  "Empreendedorismo",
                  "12/02/2026",
                  30,
                  "CERT-2026-0147",
              ),
              CertificateEntity(
                  3,
                  "Juliana Santos",
                  "Empreendedorismo",
                  "10/02/2026",
                  30,
                  "CERT-2026-0146",
              ),
              CertificateEntity(
                  4,
                  "Lucas Oliveira",
                  "Excel para Negócios",
                  "05/02/2026",
                  20,
                  "CERT-2026-0145",
              ),
              CertificateEntity(
                  5,
                  "Mariana Pereira",
                  "Design Gráfico",
                  "01/02/2026",
                  24,
                  "CERT-2026-0144",
              ),
              CertificateEntity(
                  6,
                  "Pedro Rodrigues",
                  "Finanças Pessoais",
                  "28/01/2026",
                  16,
                  "CERT-2026-0143",
              ),
              CertificateEntity(
                  7,
                  "Carlos Souza",
                  "Vendas e Negociação",
                  "20/01/2026",
                  12,
                  "CERT-2026-0142",
              ),
          )
      )
      dao.insertCalendarEvents(
          listOf(
              CalendarEventEntity(
                  1,
                  "Empreendedorismo — Turma C2",
                  "Empreendedorismo",
                  "Sex, 07 Mar",
                  "08h–12h",
                  "Sala 3",
                  EventType.Class,
              ),
              CalendarEventEntity(
                  2,
                  "Reunião de Coordenação",
                  "Institucional",
                  "Sex, 07 Mar",
                  "14h–15h",
                  "Sala de Reuniões",
                  EventType.Meeting,
              ),
              CalendarEventEntity(
                  3,
                  "Marketing Digital — Turma A1",
                  "Marketing Digital",
                  "Seg, 10 Mar",
                  "18h–20h",
                  "Sala 1",
                  EventType.Class,
              ),
              CalendarEventEntity(
                  4,
                  "Excel para Negócios — Turma B3",
                  "Excel p/ Negócios",
                  "Ter, 11 Mar",
                  "19h–21h",
                  "Lab de TI",
                  EventType.Class,
              ),
              CalendarEventEntity(
                  5,
                  "Avaliação Final — Turma A1",
                  "Marketing Digital",
                  "Qua, 12 Mar",
                  "18h–20h",
                  "Sala 1",
                  EventType.Exam,
              ),
              CalendarEventEntity(
                  6,
                  "Design Gráfico — Turma D1",
                  "Design Gráfico",
                  "Qua, 12 Mar",
                  "14h–16h",
                  "Sala 2",
                  EventType.Class,
              ),
              CalendarEventEntity(
                  7,
                  "Excel para Negócios — Turma B3",
                  "Excel p/ Negócios",
                  "Qui, 13 Mar",
                  "19h–21h",
                  "Lab de TI",
                  EventType.Class,
              ),
              CalendarEventEntity(
                  8,
                  "Conselho de Instrutores",
                  "Institucional",
                  "Qui, 13 Mar",
                  "10h–11h30",
                  "Auditório",
                  EventType.Meeting,
              ),
              CalendarEventEntity(
                  9,
                  "Finanças Pessoais — Turma A2",
                  "Finanças Pessoais",
                  "Sáb, 15 Mar",
                  "09h–12h",
                  "Sala 4",
                  EventType.Class,
              ),
              CalendarEventEntity(
                  10,
                  "Cerimônia de Formatura",
                  "Institucional",
                  "Sáb, 15 Mar",
                  "16h–19h",
                  "Auditório",
                  EventType.Other,
              ),
          )
      )
      dao.insertRecentActivities(
          listOf(
              RecentActivityEntity(
                  1,
                  "Novo aluno matriculado",
                  "Carlos Souza — Turma B3",
                  "person",
                  "há 5 min",
              ),
              RecentActivityEntity(
                  2,
                  "Curso publicado",
                  "Excel para Negócios · Nível 2",
                  "course",
                  "há 1h",
              ),
              RecentActivityEntity(
                  3,
                  "Certificado emitido",
                  "Ana Lima — Marketing Digital",
                  "certificate",
                  "há 3h",
              ),
              RecentActivityEntity(
                  4,
                  "Aula agendada",
                  "Empreendedorismo · Quinta-feira",
                  "calendar",
                  "ontem",
              ),
          )
      )
      dao.insertMonthlyEnrollments(
          listOf(
              MonthlyEnrollmentEntity("Set", 85),
              MonthlyEnrollmentEntity("Out", 110),
              MonthlyEnrollmentEntity("Nov", 98),
              MonthlyEnrollmentEntity("Dez", 72),
              MonthlyEnrollmentEntity("Jan", 130),
              MonthlyEnrollmentEntity("Fev", 155),
              MonthlyEnrollmentEntity("Mar", 148),
          )
      )

      val base = LocalDate.now()
      dao.insertAttendance(
          listOf(
              AttendanceEntity(
                  studentId = 1,
                  date = base.minusDays(5),
                  status = AttendanceStatus.Present,
                  minutesLate = 0,
                  justification = null,
              ),
              AttendanceEntity(
                  studentId = 1,
                  date = base.minusDays(4),
                  status = AttendanceStatus.Late,
                  minutesLate = 12,
                  justification = null,
              ),
              AttendanceEntity(
                  studentId = 1,
                  date = base.minusDays(3),
                  status = AttendanceStatus.Absent,
                  minutesLate = 0,
                  justification = null,
              ),
              AttendanceEntity(
                  studentId = 1,
                  date = base.minusDays(2),
                  status = AttendanceStatus.JustifiedAbsence,
                  minutesLate = 0,
                  justification = "Atestado médico",
              ),
              AttendanceEntity(
                  studentId = 1,
                  date = base.minusDays(1),
                  status = AttendanceStatus.Late,
                  minutesLate = 16,
                  justification = null,
              ),
              AttendanceEntity(
                  studentId = 2,
                  date = base.minusDays(1),
                  status = AttendanceStatus.Present,
                  minutesLate = 0,
                  justification = null,
              ),
              AttendanceEntity(
                  studentId = 3,
                  date = base.minusDays(1),
                  status = AttendanceStatus.Present,
                  minutesLate = 0,
                  justification = null,
              ),
          )
      )
      dao.insertBehaviors(
          listOf(
              BehaviorEntity(
                  studentId = 1,
                  date = base.minusDays(5),
                  participationScore = 3,
                  activityDelivery = ActivityDeliveryStatus.OnTime,
                  delayMinutes = 0,
                  grade = 8.5f,
                  note = "Boa participação.",
              ),
              BehaviorEntity(
                  studentId = 1,
                  date = base.minusDays(3),
                  participationScore = 2,
                  activityDelivery = ActivityDeliveryStatus.Missing,
                  delayMinutes = 14,
                  grade = 6.0f,
                  note = "Não entregou atividade de revisão.",
              ),
              BehaviorEntity(
                  studentId = 1,
                  date = base.minusDays(1),
                  participationScore = 2,
                  activityDelivery = ActivityDeliveryStatus.Late,
                  delayMinutes = 15,
                  grade = 5.5f,
                  note = "Atrasos repetidos no início da aula.",
              ),
              BehaviorEntity(
                  studentId = 2,
                  date = base.minusDays(1),
                  participationScore = 4,
                  activityDelivery = ActivityDeliveryStatus.OnTime,
                  delayMinutes = 0,
                  grade = 7.8f,
                  note = "Evolução constante.",
              ),
          )
      )
      dao.insertPedagogicalNeeds(
          listOf(
              PedagogicalNeedEntity(
                  studentId = 1,
                  type = PedagogicalNeedType.Report,
                  description = "Laudo de dislexia com orientações de leitura assistida.",
                  expiresAt = base.plusMonths(8),
                  accommodations = listOf("Tempo extra", "Fonte ampliada"),
              ),
              PedagogicalNeedEntity(
                  studentId = 1,
                  type = PedagogicalNeedType.SpecialNeed,
                  description = "Plano adaptado para avaliações em etapas.",
                  expiresAt = null,
                  accommodations = listOf("Prova segmentada", "Apoio individual"),
              ),
          )
      )
      dao.insertPsychologicalNeeds(
          listOf(
              PsychologicalNeedEntity(
                  studentId = 1,
                  summary = "Ansiedade em avaliações presenciais.",
                  confidentiality = ConfidentialityLevel.Restricted,
                  nextStep = "Sessão de acolhimento quinzenal",
                  reviewAt = base.plusWeeks(2),
              )
          )
      )
      dao.insertParentFollowUps(
          listOf(
              ParentFollowUpEntity(
                  studentId = 1,
                  date = base.minusDays(6),
                  channel = ParentContactChannel.Phone,
                  outcome = ParentFollowUpStatus.WaitingResponse,
                  responsible = "Prof. Marta",
                  notes = "Solicitado retorno sobre rotina de estudos em casa.",
              ),
              ParentFollowUpEntity(
                  studentId = 1,
                  date = base.minusDays(2),
                  channel = ParentContactChannel.Message,
                  outcome = ParentFollowUpStatus.Pending,
                  responsible = "Coord. João",
                  notes = "Convite para reunião de alinhamento pedagógico.",
              ),
          )
      )
      dao.upsertSettings(defaultSettingsEntity())
    }
  }

  /** Executa a rotina de default settings entity dentro do contexto deste componente. */
  private fun defaultSettingsEntity() =
      AppSettingsEntity(
          id = 1,
          darkMode = false,
          pushEnabled = true,
          emailEnabled = false,
          language = "pt",
      )
}

/** Executa a rotina de course entity dentro do contexto deste componente. */
private fun CourseEntity.toModel() =
    Course(
        id = id,
        title = title,
        category = category,
        instructor = instructor,
        totalStudents = totalStudents,
        durationHours = durationHours,
        completionRate = completionRate,
        isPublished = isPublished,
    )

/** Executa a rotina de school class entity dentro do contexto deste componente. */
private fun SchoolClassEntity.toModel() =
    SchoolClass(
        id = id,
        name = name,
        course = course,
        instructor = instructor,
        studentsCount = studentsCount,
        maxCapacity = maxCapacity,
        schedule = schedule,
        status = status,
    )

/** Executa a rotina de teacher entity dentro do contexto deste componente. */
private fun TeacherEntity.toModel() =
    Teacher(
        id = id,
        name = name,
        email = email,
        specialty = specialty,
        activeCourses = activeCourses,
        totalStudents = totalStudents,
        rating = rating,
        isActive = isActive,
    )

/** Executa a rotina de student entity dentro do contexto deste componente. */
private fun StudentEntity.toModel() =
    Student(
        id = id,
        name = name,
        email = email,
        course = course,
        enrolledClass = enrolledClass,
        progress = progress,
        status = status,
    )

/** Executa a rotina de certificate entity dentro do contexto deste componente. */
private fun CertificateEntity.toModel() =
    Certificate(
        id = id,
        studentName = studentName,
        courseName = courseName,
        issuedDate = issuedDate,
        hours = hours,
        code = code,
    )

/** Executa a rotina de calendar event entity dentro do contexto deste componente. */
private fun CalendarEventEntity.toModel() =
    CalendarEvent(
        id = id,
        title = title,
        course = course,
        date = date,
        time = time,
        location = location,
        type = type,
    )

/** Executa a rotina de recent activity entity dentro do contexto deste componente. */
private fun RecentActivityEntity.toModel() =
    RecentActivity(
        id = id,
        title = title,
        subtitle = subtitle,
        icon =
            when (iconKey) {
              "person" -> Icons.Outlined.Person
              "course" -> Icons.AutoMirrored.Outlined.MenuBook
              "certificate" -> Icons.Outlined.Bookmarks
              else -> Icons.Outlined.DateRange
            },
        timeLabel = timeLabel,
    )

/** Executa a rotina de attendance entity dentro do contexto deste componente. */
private fun AttendanceEntity.toModel() =
    AttendanceRecord(
        date = date,
        status = status,
        minutesLate = minutesLate,
        justification = justification,
    )

/** Executa a rotina de behavior entity dentro do contexto deste componente. */
private fun BehaviorEntity.toModel() =
    BehaviorRecord(
        date = date,
        participationScore = participationScore,
        activityDelivery = activityDelivery,
        delayMinutes = delayMinutes,
        grade = grade,
        note = note,
    )

/** Executa a rotina de pedagogical need entity dentro do contexto deste componente. */
private fun PedagogicalNeedEntity.toModel() =
    PedagogicalNeed(
        type = type,
        description = description,
        expiresAt = expiresAt,
        accommodations = accommodations,
    )

/** Executa a rotina de psychological need entity dentro do contexto deste componente. */
private fun PsychologicalNeedEntity.toModel() =
    PsychologicalNeed(
        summary = summary,
        confidentiality = confidentiality,
        nextStep = nextStep,
        reviewAt = reviewAt,
    )

/** Executa a rotina de parent follow up entity dentro do contexto deste componente. */
private fun ParentFollowUpEntity.toModel() =
    ParentFollowUp(
        date = date,
        channel = channel,
        outcome = outcome,
        responsible = responsible,
        notes = notes,
    )

/** Executa a rotina de app user entity dentro do contexto deste componente. */
private fun AppUserEntity.toModel() = AppUser(id = id, name = name, email = email, role = role)

/**
 * Executa a rotina de app user dentro do contexto deste componente.
 *
 * @param passwordHash Valor de entrada utilizado por esta opera??o.
 */
private fun AppUser.toEntity(passwordHash: String) =
    AppUserEntity(id = id, name = name, email = email, role = role, passwordHash = passwordHash)

/**
 * Executa a rotina de sha256 dentro do contexto deste componente.
 *
 * @param input Valor de entrada utilizado por esta opera??o.
 * @return Resultado produzido pela opera??o em formato `String`.
 */
private fun sha256(input: String): String {
  val digest = MessageDigest.getInstance("SHA-256")
  return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

/** Converte valor nulo em zero para manter cálculos agregados consistentes. */
private fun Int?.orZero(): Int = this ?: 0
