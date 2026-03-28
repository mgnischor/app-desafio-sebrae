/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/repository/AppRepository.kt
    Descrição: Repositório principal de acesso a dados locais (Room) e remotos (Firebase).
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
import tech.datatower.sebrae.desafio.data.local.ParentFollowUpEntity
import tech.datatower.sebrae.desafio.data.local.PedagogicalNeedEntity
import tech.datatower.sebrae.desafio.data.local.PsychologicalNeedEntity
import tech.datatower.sebrae.desafio.data.local.RecentActivityEntity
import tech.datatower.sebrae.desafio.data.local.SchoolClassEntity
import tech.datatower.sebrae.desafio.data.local.StudentEntity
import tech.datatower.sebrae.desafio.data.local.TeacherEntity
import tech.datatower.sebrae.desafio.data.model.AppSettings
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.Certificate
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.ParentFollowUp
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeed
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

/** Centraliza operações de dados relacionadas a app. */
class AppRepository(
    private val database: AppDatabase,
    private val dao: AppDao,
    private val dataSourceLabelResFlow: Flow<Int> = flowOf(R.string.stat_data_source),
) {
  /**
   * Observa alterações de courses e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<Course>>`.
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
   * Observa alterações de course by id e publica atualizações reativas.
   *
   * @param courseId Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<Course?>`.
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
   * Observa alterações de classes e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<SchoolClass>>`.
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
   * Observa alterações de class by id e publica atualizações reativas.
   *
   * @param classId Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<SchoolClass?>`.
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
   * Observa alterações de classes by course e publica atualizações reativas.
   *
   * @param courseName Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<List<SchoolClass>>`.
   */
  fun observeClassesByCourse(courseName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByCourse(courseName).map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de classes by teacher e publica atualizações reativas.
   *
   * @param teacherName Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<List<SchoolClass>>`.
   */
  fun observeClassesByTeacher(teacherName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByTeacher(teacherName).map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de teachers e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<Teacher>>`.
   */
  fun observeTeachers(): Flow<List<Teacher>> =
      dao.observeTeachers().map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de teacher by id e publica atualizações reativas.
   *
   * @param teacherId Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<Teacher?>`.
   */
  fun observeTeacherById(teacherId: Int): Flow<Teacher?> =
      dao.observeTeacherById(teacherId).map { it?.toModel() }

  /**
   * Observa alterações de students e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<Student>>`.
   */
  fun observeStudents(): Flow<List<Student>> =
      dao.observeStudents().map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de students by class e publica atualizações reativas.
   *
   * @param className Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<List<Student>>`.
   */
  fun observeStudentsByClass(className: String): Flow<List<Student>> =
      dao.observeStudentsByClass(className).map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de certificates e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<Certificate>>`.
   */
  fun observeCertificates(): Flow<List<Certificate>> =
      dao.observeCertificates().map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de calendar events e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<CalendarEvent>>`.
   */
  fun observeCalendarEvents(): Flow<List<CalendarEvent>> =
      dao.observeCalendarEvents().map { items -> items.map { it.toModel() } }

  /**
   * Observa alterações de recent activities e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<RecentActivity>>`.
   */
  fun observeRecentActivities(): Flow<List<RecentActivity>> =
      dao.observeRecentActivities().map { items -> items.map { it.toModel() } }

  /** Observa atividades recentes com limite máximo de itens para telas de resumo. */
  fun observeRecentActivities(limit: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesLimited(limit).map { items -> items.map { it.toModel() } }

  /** Observa uma página de atividades recentes mapeada para o modelo de domínio. */
  fun observeRecentActivitiesPaged(limit: Int, offset: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesPaged(limit, offset).map { items -> items.map { it.toModel() } }

  /** Observa o total de atividades recentes de forma reativa. */
  fun observeRecentActivitiesCount(): Flow<Int> = dao.observeRecentActivitiesCount()

  /**
   * Observa alterações de home quick stats e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<QuickStat>>`.
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
   * Observa alterações de report summary e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<ReportSummary>`.
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
   * Observa alterações de course completion metrics e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<CourseCompletionMetric>>`.
   */
  fun observeCourseCompletionMetrics(): Flow<List<CourseCompletionMetric>> =
      dao.observeCourses().map { courses ->
        courses
            .map { CourseCompletionMetric(name = it.title, rate = it.completionRate) }
            .sortedByDescending { it.rate }
      }

  /**
   * Observa alterações de monthly enrollment metrics e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<List<MonthlyEnrollmentMetric>>`.
   */
  fun observeMonthlyEnrollmentMetrics(): Flow<List<MonthlyEnrollmentMetric>> =
      dao.observeMonthlyEnrollments().map { items ->
        items.map { MonthlyEnrollmentMetric(month = it.month, count = it.count) }
      }

  /**
   * Observa alterações de student monitoring snapshot e publica atualizações reativas.
   *
   * @param studentId Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<StudentMonitoringSnapshot?>`.
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
   * Observa alterações de settings e publica atualizações reativas.
   *
   * @return Resultado produzido pela operação em formato `Flow<AppSettings>`.
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
   * Observa alterações de registered users for admin e publica atualizações reativas.
   *
   * @param requester Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<List<AppUser>>`.
   */
  /**
   * Observa alterações de um usuário específico por ID e publica atualizações reativas.
   *
   * @param userId Identificador do usuário desejado.
   * @return Resultado produzido pela operação em formato `Flow<AppUser?>`.
   */
  fun observeUserById(userId: Int): Flow<AppUser?> =
      dao.observeUserById(userId).map { it?.toModel() }

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
   * @param requester Valor de entrada utilizado por esta operação.
   * @param user Valor de entrada utilizado por esta operação.
   * @param plainPassword Valor de entrada utilizado por esta operação.
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
   * @param enabled Valor de entrada utilizado por esta operação.
   */
  suspend fun updateDarkMode(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(darkMode = enabled))
  }

  /**
   * Executa a rotina de update push enabled dentro do contexto deste componente.
   *
   * @param enabled Valor de entrada utilizado por esta operação.
   */
  suspend fun updatePushEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(pushEnabled = enabled))
  }

  /**
   * Executa a rotina de update email enabled dentro do contexto deste componente.
   *
   * @param enabled Valor de entrada utilizado por esta operação.
   */
  suspend fun updateEmailEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(emailEnabled = enabled))
  }

  /**
   * Executa a rotina de update language dentro do contexto deste componente.
   *
   * @param language Valor de entrada utilizado por esta operação.
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

  /** Executa o reset completo da base de dados local, preservando os usuários cadastrados. */
  suspend fun resetAllData() {
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
 * @param passwordHash Valor de entrada utilizado por esta operação.
 */
private fun AppUser.toEntity(passwordHash: String) =
    AppUserEntity(id = id, name = name, email = email, role = role, passwordHash = passwordHash)

/**
 * Executa a rotina de sha256 dentro do contexto deste componente.
 *
 * @param input Valor de entrada utilizado por esta operação.
 * @return Resultado produzido pela operação em formato `String`.
 */
private fun sha256(input: String): String {
  val digest = MessageDigest.getInstance("SHA-256")
  return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

/** Converte valor nulo em zero para manter cálculos agregados consistentes. */
private fun Int?.orZero(): Int = this ?: 0
