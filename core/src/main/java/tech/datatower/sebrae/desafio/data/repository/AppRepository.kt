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
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.AppDatabase
import tech.datatower.sebrae.desafio.data.local.AppSettingsEntity
import tech.datatower.sebrae.desafio.data.local.AppUserEntity
import tech.datatower.sebrae.desafio.data.local.AttendanceEntity
import tech.datatower.sebrae.desafio.data.local.BehaviorEntity
import tech.datatower.sebrae.desafio.data.local.CalendarEventEntity
import tech.datatower.sebrae.desafio.data.local.CertificateEntity
import tech.datatower.sebrae.desafio.data.local.CompanyEntity
import tech.datatower.sebrae.desafio.data.local.CourseEntity
import tech.datatower.sebrae.desafio.data.local.ParentFollowUpEntity
import tech.datatower.sebrae.desafio.data.local.PedagogicalNeedEntity
import tech.datatower.sebrae.desafio.data.local.PsychologicalNeedEntity
import tech.datatower.sebrae.desafio.data.local.RecentActivityEntity
import tech.datatower.sebrae.desafio.data.local.SchoolClassEntity
import tech.datatower.sebrae.desafio.data.local.StudentEntity
import tech.datatower.sebrae.desafio.data.local.TeacherEntity
import tech.datatower.sebrae.desafio.data.local.UserCompanyEntity
import tech.datatower.sebrae.desafio.data.model.AppSettings
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.Certificate
import tech.datatower.sebrae.desafio.data.model.Company
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

/** Modelo e comportamento relacionados a status distribution metric. */
data class StatusDistributionMetric(
    val status: StudentStatus,
    val count: Int,
)

/** Centraliza operações de dados relacionadas a app. */
class AppRepository(
    private val database: AppDatabase,
    private val dao: AppDao,
    private val dataSourceLabelResFlow: Flow<Int> = flowOf(R.string.stat_data_source),
) {

  // ── Company ─────────────────────────────────────────────────────────────

  fun observeCompanies(): Flow<List<Company>> =
      dao.observeCompanies().map { items -> items.map { it.toModel() } }

  fun observeActiveCompanies(): Flow<List<Company>> =
      dao.observeActiveCompanies().map { items -> items.map { it.toModel() } }

  fun observeCompanyById(companyId: Int): Flow<Company?> =
      dao.observeCompanyById(companyId).map { it?.toModel() }

  fun observeCompaniesForUser(userId: Int): Flow<List<Company>> =
      dao.observeCompaniesForUser(userId).map { items -> items.map { it.toModel() } }

  suspend fun upsertCompanyForAdmin(requester: AppUser?, company: Company) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode cadastrar empresas.")
    }
    dao.upsertCompany(company.toEntity())
  }

  suspend fun deleteCompanyForAdmin(requester: AppUser?, companyId: Int) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode remover empresas.")
    }
    database.withTransaction {
      dao.deleteUserCompaniesForCompany(companyId)
      dao.deleteCompanyById(companyId)
    }
  }

  // ── User ↔ Company ────────────────────────────────────────────────────

  fun observeUserCompanies(userId: Int): Flow<List<UserCompanyEntity>> =
      dao.observeUserCompanies(userId)

  fun observeUserIdsByCompany(companyId: Int): Flow<Set<Int>> =
      dao.observeUserIdsByCompany(companyId).map { it.toSet() }

  suspend fun grantUserCompanyAccess(requester: AppUser?, userId: Int, companyId: Int) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode conceder acesso a empresas.")
    }
    val existing = dao.getUserCompany(userId, companyId)
    if (existing == null) {
      dao.insertUserCompany(UserCompanyEntity(userId = userId, companyId = companyId))
    }
  }

  suspend fun revokeUserCompanyAccess(requester: AppUser?, userId: Int, companyId: Int) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode revogar acesso a empresas.")
    }
    dao.deleteUserCompany(userId, companyId)
  }

  // ── Courses (filtered by company) ─────────────────────────────────────

  fun observeCourses(companyId: Int): Flow<List<Course>> =
      combine(dao.observeCourses(companyId), dao.observeStudents(companyId)) {
          courseEntities,
          studentEntities ->
        val students = studentEntities.map { it.toModel() }
        val realByCourse = RelationshipRules.realStudentsByCourse(students)
        courseEntities.map { entity ->
          val model = entity.toModel()
          model.copy(totalStudents = realByCourse[model.title.trim()].orZero())
        }
      }

  fun observeAllCourses(): Flow<List<Course>> =
      combine(dao.observeAllCourses(), dao.observeAllStudents()) { courseEntities, studentEntities
        ->
        val students = studentEntities.map { it.toModel() }
        val realByCourse = RelationshipRules.realStudentsByCourse(students)
        courseEntities.map { entity ->
          val model = entity.toModel()
          model.copy(totalStudents = realByCourse[model.title.trim()].orZero())
        }
      }

  fun observeCourseById(courseId: Int): Flow<Course?> =
      dao.observeCourseById(courseId).map { it?.toModel() }

  fun observeCourseByIdWithStudents(courseId: Int, companyId: Int): Flow<Course?> =
      combine(dao.observeCourseById(courseId), dao.observeStudents(companyId)) {
          courseEntity,
          studentEntities ->
        val model = courseEntity?.toModel() ?: return@combine null
        val realByCourse =
            RelationshipRules.realStudentsByCourse(studentEntities.map { it.toModel() })
        model.copy(totalStudents = realByCourse[model.title.trim()].orZero())
      }

  // ── Classes (filtered by company) ─────────────────────────────────────

  fun observeClasses(companyId: Int): Flow<List<SchoolClass>> =
      combine(dao.observeClasses(companyId), dao.observeStudents(companyId)) {
          classEntities,
          studentEntities ->
        val students = studentEntities.map { it.toModel() }
        val realByClass = RelationshipRules.realStudentsByClass(students)
        classEntities.map { entity ->
          val model = entity.toModel()
          model.copy(studentsCount = realByClass[model.name.trim()].orZero())
        }
      }

  fun observeAllClasses(): Flow<List<SchoolClass>> =
      combine(dao.observeAllClasses(), dao.observeAllStudents()) { classEntities, studentEntities ->
        val students = studentEntities.map { it.toModel() }
        val realByClass = RelationshipRules.realStudentsByClass(students)
        classEntities.map { entity ->
          val model = entity.toModel()
          model.copy(studentsCount = realByClass[model.name.trim()].orZero())
        }
      }

  fun observeClassById(classId: Int, companyId: Int): Flow<SchoolClass?> =
      combine(dao.observeClassById(classId), dao.observeStudents(companyId)) {
          classEntity,
          studentEntities ->
        val model = classEntity?.toModel() ?: return@combine null
        val realByClass =
            RelationshipRules.realStudentsByClass(studentEntities.map { it.toModel() })
        model.copy(studentsCount = realByClass[model.name.trim()].orZero())
      }

  fun observeClassesByCourse(companyId: Int, courseName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByCourse(companyId, courseName).map { items -> items.map { it.toModel() } }

  fun observeClassesByTeacher(companyId: Int, teacherName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByTeacher(companyId, teacherName).map { items ->
        items.map { it.toModel() }
      }

  // ── Teachers (filtered by company) ────────────────────────────────────

  fun observeTeachers(companyId: Int): Flow<List<Teacher>> =
      dao.observeTeachers(companyId).map { items -> items.map { it.toModel() } }

  fun observeAllTeachers(): Flow<List<Teacher>> =
      dao.observeAllTeachers().map { items -> items.map { it.toModel() } }

  fun observeTeacherById(teacherId: Int): Flow<Teacher?> =
      dao.observeTeacherById(teacherId).map { it?.toModel() }

  // ── Students (filtered by company) ────────────────────────────────────

  fun observeStudents(companyId: Int): Flow<List<Student>> =
      dao.observeStudents(companyId).map { items -> items.map { it.toModel() } }

  fun observeAllStudents(): Flow<List<Student>> =
      dao.observeAllStudents().map { items -> items.map { it.toModel() } }

  fun observeStudentsByGuardian(guardianUserId: Int, companyId: Int): Flow<List<Student>> =
      dao.observeStudentsByGuardian(guardianUserId, companyId).map { items ->
        items.map { it.toModel() }
      }

  fun observeStudentsByClass(companyId: Int, className: String): Flow<List<Student>> =
      dao.observeStudentsByClass(companyId, className).map { items -> items.map { it.toModel() } }

  // ── Certificates (filtered by company) ────────────────────────────────

  fun observeCertificates(companyId: Int): Flow<List<Certificate>> =
      dao.observeCertificates(companyId).map { items -> items.map { it.toModel() } }

  fun observeAllCertificates(): Flow<List<Certificate>> =
      dao.observeAllCertificates().map { items -> items.map { it.toModel() } }

  // ── Calendar Events (filtered by company) ─────────────────────────────

  fun observeCalendarEvents(companyId: Int): Flow<List<CalendarEvent>> =
      dao.observeCalendarEvents(companyId).map { items -> items.map { it.toModel() } }

  // ── Recent Activities (filtered by company) ───────────────────────────

  fun observeRecentActivities(companyId: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivities(companyId).map { items -> items.map { it.toModel() } }

  fun observeRecentActivities(companyId: Int, limit: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesLimited(companyId, limit).map { items ->
        items.map { it.toModel() }
      }

  fun observeRecentActivitiesPaged(
      companyId: Int,
      limit: Int,
      offset: Int,
  ): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesPaged(companyId, limit, offset).map { items ->
        items.map { it.toModel() }
      }

  fun observeRecentActivitiesCount(companyId: Int): Flow<Int> =
      dao.observeRecentActivitiesCount(companyId)

  // ── Home Quick Stats (filtered by company) ────────────────────────────

  fun observeHomeQuickStats(companyId: Int): Flow<List<QuickStat>> =
      combine(
          dao.observeStudentsByStatusCount(companyId, StudentStatus.Active),
          dao.observePublishedCoursesCount(companyId),
          dao.observeClassesCount(companyId),
          dao.observeAverageCompletionRate(companyId),
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

  // ── Report Summary (filtered by company) ──────────────────────────────

  fun observeReportSummary(companyId: Int): Flow<ReportSummary> =
      combine(
          combine(
              dao.observeStudentsByStatusCount(companyId, StudentStatus.Active),
              dao.observePublishedCoursesCount(companyId),
          ) { activeStudents, activeCourses ->
            activeStudents to activeCourses
          },
          combine(
              dao.observeClassesCount(companyId),
              dao.observeAverageCompletionRate(companyId),
          ) { classesCount, completionRate ->
            classesCount to completionRate
          },
          combine(
              dao.observeCertificatesCount(companyId),
              dao.observeAverageTeacherRating(companyId),
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

  fun observeCourseCompletionMetrics(companyId: Int): Flow<List<CourseCompletionMetric>> =
      dao.observeCourses(companyId).map { courses ->
        courses
            .map { CourseCompletionMetric(name = it.title, rate = it.completionRate) }
            .sortedByDescending { it.rate }
      }

  fun observeMonthlyEnrollmentMetrics(companyId: Int): Flow<List<MonthlyEnrollmentMetric>> =
      dao.observeMonthlyEnrollments(companyId).map { items ->
        items.map { MonthlyEnrollmentMetric(month = it.month, count = it.count) }
      }

  fun observeStatusDistribution(companyId: Int): Flow<List<StatusDistributionMetric>> =
      combine(
          dao.observeStudentsByStatusCount(companyId, StudentStatus.Active),
          dao.observeStudentsByStatusCount(companyId, StudentStatus.Inactive),
          dao.observeStudentsByStatusCount(companyId, StudentStatus.Graduated),
      ) { active, inactive, graduated ->
        listOf(
            StatusDistributionMetric(StudentStatus.Active, active),
            StatusDistributionMetric(StudentStatus.Inactive, inactive),
            StatusDistributionMetric(StudentStatus.Graduated, graduated),
        )
      }

  // ── Student Monitoring ────────────────────────────────────────────────

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

  // ── Settings ──────────────────────────────────────────────────────────

  /** Insere um registro de frequência para o aluno informado. */
  suspend fun insertAttendanceRecord(companyId: Int, studentId: Int, record: AttendanceRecord) {
    dao.insertAttendance(
        listOf(
            AttendanceEntity(
                companyId = companyId,
                studentId = studentId,
                date = record.date,
                status = record.status,
                minutesLate = record.minutesLate,
                justification = record.justification,
            )
        )
    )
  }

  /** Insere um registro de comportamento para o aluno informado. */
  suspend fun insertBehaviorRecord(companyId: Int, studentId: Int, record: BehaviorRecord) {
    dao.insertBehaviors(
        listOf(
            BehaviorEntity(
                companyId = companyId,
                studentId = studentId,
                date = record.date,
                participationScore = record.participationScore,
                activityDelivery = record.activityDelivery,
                delayMinutes = record.delayMinutes,
                grade = record.grade,
                note = record.note,
            )
        )
    )
  }

  /** Insere uma necessidade pedagógica para o aluno informado. */
  suspend fun insertPedagogicalNeedRecord(companyId: Int, studentId: Int, need: PedagogicalNeed) {
    dao.insertPedagogicalNeeds(
        listOf(
            PedagogicalNeedEntity(
                companyId = companyId,
                studentId = studentId,
                type = need.type,
                description = need.description,
                expiresAt = need.expiresAt,
                accommodations = need.accommodations,
            )
        )
    )
  }

  /** Insere uma necessidade psicológica para o aluno informado. */
  suspend fun insertPsychologicalNeedRecord(
      companyId: Int,
      studentId: Int,
      need: PsychologicalNeed,
  ) {
    dao.insertPsychologicalNeeds(
        listOf(
            PsychologicalNeedEntity(
                companyId = companyId,
                studentId = studentId,
                summary = need.summary,
                confidentiality = need.confidentiality,
                nextStep = need.nextStep,
                reviewAt = need.reviewAt,
            )
        )
    )
  }

  /** Insere um registro de acompanhamento de pais/responsáveis para o aluno informado. */
  suspend fun insertParentFollowUpRecord(
      companyId: Int,
      studentId: Int,
      followUp: ParentFollowUp,
  ) {
    dao.insertParentFollowUps(
        listOf(
            ParentFollowUpEntity(
                companyId = companyId,
                studentId = studentId,
                date = followUp.date,
                channel = followUp.channel,
                outcome = followUp.outcome,
                responsible = followUp.responsible,
                notes = followUp.notes,
            )
        )
    )
  }

  // ── CSV Bulk Import ───────────────────────────────────────────────────

  suspend fun importStudents(entities: List<StudentEntity>) {
    dao.insertStudents(entities)
  }

  suspend fun importCourses(entities: List<CourseEntity>) {
    dao.insertCourses(entities)
  }

  suspend fun importClasses(entities: List<SchoolClassEntity>) {
    dao.insertClasses(entities)
  }

  suspend fun importTeachers(entities: List<TeacherEntity>) {
    dao.insertTeachers(entities)
  }

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

  // ── Users ─────────────────────────────────────────────────────────────

  fun observeUserById(userId: Int): Flow<AppUser?> =
      dao.observeUserById(userId).map { it?.toModel() }

  fun observeRegisteredUsersForAdmin(requester: AppUser?): Flow<List<AppUser>> {
    return if (
        requester?.role == UserRole.ADMINISTRADOR || requester?.role == UserRole.COORDENADOR
    ) {
      dao.observeUsers().map { items -> items.map { it.toModel() } }
    } else {
      flowOf(emptyList())
    }
  }

  suspend fun upsertRegisteredUserForAdmin(
      requester: AppUser?,
      user: AppUser,
      plainPassword: String,
  ) {
    if (requester?.role != UserRole.ADMINISTRADOR && requester?.role != UserRole.COORDENADOR) {
      throw SecurityException("Apenas administrador ou coordenador pode cadastrar usuarios.")
    }
    if (plainPassword.isBlank()) {
      throw IllegalArgumentException("Senha do usuario nao pode ser vazia.")
    }
    dao.upsertUser(user.toEntity(passwordHash = sha256(plainPassword)))
  }

  // ── Settings updates ──────────────────────────────────────────────────

  suspend fun updateDarkMode(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(darkMode = enabled))
  }

  suspend fun updatePushEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(pushEnabled = enabled))
  }

  suspend fun updateEmailEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(emailEnabled = enabled))
  }

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

  private fun defaultSettingsEntity() =
      AppSettingsEntity(
          id = 1,
          darkMode = false,
          pushEnabled = true,
          emailEnabled = false,
          language = "pt",
      )
}

private fun CompanyEntity.toModel() =
    Company(id = id, name = name, cnpj = cnpj, isActive = isActive)

private fun Company.toEntity() =
    CompanyEntity(id = id, name = name, cnpj = cnpj, isActive = isActive)

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
