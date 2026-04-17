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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Consolida os indicadores agrégados exibidos na tela de relatórios.
 *
 * @property activeStudents Quantidade de alunos com status ativo na empresa.
 * @property activeCourses Quantidade de cursos publicados na empresa.
 * @property totalClasses Quantidade total de turmas cadastradas na empresa.
 * @property completionRate Taxa média de conclusão dos cursos no intervalo de `0f..1f`.
 * @property certificates Quantidade de certificados emitidos na empresa.
 * @property averageTeacherRating Avaliação média dos instrutores no intervalo de `0f..5f`.
 */
data class ReportSummary(
    val activeStudents: Int,
    val activeCourses: Int,
    val totalClasses: Int,
    val completionRate: Float,
    val certificates: Int,
    val averageTeacherRating: Float,
)

/**
 * Métrica de taxa de conclusão por curso para o gráfico de relatórios.
 *
 * @property name Título do curso.
 * @property rate Taxa de conclusão no intervalo de `0f..1f`.
 */
data class CourseCompletionMetric(
    val name: String,
    val rate: Float,
)

/**
 * Métrica de matrículas agrupadas por mês para o gráfico de evolução.
 *
 * @property month Mês de referência no formato `"YYYY-MM"`.
 * @property count Quantidade de matrículas registradas no mês.
 */
data class MonthlyEnrollmentMetric(
    val month: String,
    val count: Int,
)

/**
 * Métrica de distribuição de alunos por status acadêmico para o gráfico de relatórios.
 *
 * @property status Situação acadêmica do grupo de alunos.
 * @property count Quantidade de alunos nesse status.
 */
data class StatusDistributionMetric(
    val status: StudentStatus,
    val count: Int,
)

/**
 * Repositório central de acesso a dados da aplicação.
 *
 * Orquestra operações de leitura e escrita no banco local Room, expondo [Flow] reativos para a
 * camada de UI via ViewModels. Cada método de consulta filtra os dados pelo `companyId` ativo,
 * garantindo o isolamento multi-tenant.
 *
 * Esta classe é provida como singleton pelo Hilt e não deve ser instanciada diretamente.
 */
class AppRepository(
    private val database: AppDatabase,
    private val dao: AppDao,
    private val dataSourceLabelResFlow: Flow<Int> = flowOf(R.string.stat_data_source),
) {

  // ── Company ─────────────────────────────────────────────────────────────

  /** Observa todas as empresas cadastradas no banco local. */
  fun observeCompanies(): Flow<List<Company>> =
      dao.observeCompanies().map { items -> items.map { it.toModel() } }

  /** Observa apenas as empresas cuja flag `isActive` esteja ativa. */
  fun observeActiveCompanies(): Flow<List<Company>> =
      dao.observeActiveCompanies().map { items -> items.map { it.toModel() } }

  /**
   * Observa a empresa identificada por [companyId].
   *
   * @param companyId Identificador da empresa.
   * @return [Flow] que emite a empresa ou `null` se não existir.
   */
  fun observeCompanyById(companyId: Int): Flow<Company?> =
      dao.observeCompanyById(companyId).map { it?.toModel() }

  /** Observa as empresas às quais o usuário [userId] tem acesso. */
  fun observeCompaniesForUser(userId: Int): Flow<List<Company>> =
      dao.observeCompaniesForUser(userId).map { items -> items.map { it.toModel() } }

  /**
   * Cria ou atualiza uma empresa. Apenas administradores podem executar esta operação.
   *
   * @param requester Usuário que solicita a operação.
   * @param company Dados da empresa a cadastrar ou atualizar.
   * @throws SecurityException Quando o [requester] não é administrador.
   */
  suspend fun upsertCompanyForAdmin(requester: AppUser?, company: Company) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode cadastrar empresas.")
    }
    dao.upsertCompany(company.toEntity())
  }

  /**
   * Remove uma empresa e seus vínculos de acesso. Apenas administradores podem executar.
   *
   * @param requester Usuário que solicita a operação.
   * @param companyId Identificador da empresa a remover.
   * @throws SecurityException Quando o [requester] não é administrador.
   */
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

  /** Observa os vínculos empresa–usuário para o [userId] informado. */
  fun observeUserCompanies(userId: Int): Flow<List<UserCompanyEntity>> =
      dao.observeUserCompanies(userId)

  /** Observa os IDs de usuários vinculados à empresa [companyId]. */
  fun observeUserIdsByCompany(companyId: Int): Flow<Set<Int>> =
      dao.observeUserIdsByCompany(companyId).map { it.toSet() }

  /**
   * Concede acesso de um usuário a uma empresa. Apenas administradores podem executar.
   *
   * @param requester Usuário que solicita a operação.
   * @param userId Identificador do usuário que receberá acesso.
   * @param companyId Identificador da empresa alvo.
   * @throws SecurityException Quando o [requester] não é administrador.
   */
  suspend fun grantUserCompanyAccess(requester: AppUser?, userId: Int, companyId: Int) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode conceder acesso a empresas.")
    }
    val existing = dao.getUserCompany(userId, companyId)
    if (existing == null) {
      dao.insertUserCompany(UserCompanyEntity(userId = userId, companyId = companyId))
    }
  }

  /**
   * Revoga o acesso de um usuário a uma empresa. Apenas administradores podem executar.
   *
   * @param requester Usuário que solicita a operação.
   * @param userId Identificador do usuário cujo acesso será revogado.
   * @param companyId Identificador da empresa alvo.
   * @throws SecurityException Quando o [requester] não é administrador.
   */
  suspend fun revokeUserCompanyAccess(requester: AppUser?, userId: Int, companyId: Int) {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      throw SecurityException("Apenas administrador pode revogar acesso a empresas.")
    }
    dao.deleteUserCompany(userId, companyId)
  }

  // ── Courses (filtered by company) ─────────────────────────────────────

  /**
   * Observa cursos da empresa [companyId], enriquecendo `totalStudents` com base nas matrículas
   * reais.
   */
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

  /** Observa todos os cursos de todas as empresas (visão de administrador). */
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

  /** Observa um curso específico pelo [courseId]. */
  fun observeCourseById(courseId: Int): Flow<Course?> =
      dao.observeCourseById(courseId).map { it?.toModel() }

  /** Observa curso por [courseId] com contagem real de alunos na empresa [companyId]. */
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

  /** Observa turmas da empresa [companyId], enriquecendo `studentsCount` com matrículas reais. */
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

  /** Observa todas as turmas de todas as empresas (visão de administrador). */
  fun observeAllClasses(): Flow<List<SchoolClass>> =
      combine(dao.observeAllClasses(), dao.observeAllStudents()) { classEntities, studentEntities ->
        val students = studentEntities.map { it.toModel() }
        val realByClass = RelationshipRules.realStudentsByClass(students)
        classEntities.map { entity ->
          val model = entity.toModel()
          model.copy(studentsCount = realByClass[model.name.trim()].orZero())
        }
      }

  /** Observa turma por [classId] com contagem real de alunos na empresa [companyId]. */
  fun observeClassById(classId: Int, companyId: Int): Flow<SchoolClass?> =
      combine(dao.observeClassById(classId), dao.observeStudents(companyId)) {
          classEntity,
          studentEntities ->
        val model = classEntity?.toModel() ?: return@combine null
        val realByClass =
            RelationshipRules.realStudentsByClass(studentEntities.map { it.toModel() })
        model.copy(studentsCount = realByClass[model.name.trim()].orZero())
      }

  /** Observa turmas vinculadas ao curso [courseName] na empresa [companyId]. */
  fun observeClassesByCourse(companyId: Int, courseName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByCourse(companyId, courseName).map { items -> items.map { it.toModel() } }

  /** Observa turmas associadas ao instrutor [teacherName] na empresa [companyId]. */
  fun observeClassesByTeacher(companyId: Int, teacherName: String): Flow<List<SchoolClass>> =
      dao.observeClassesByTeacher(companyId, teacherName).map { items ->
        items.map { it.toModel() }
      }

  // ── Teachers (filtered by company) ────────────────────────────────────

  /**
   * Observa instrutores da empresa [companyId], enriquecendo `activeCourses` e `totalStudents` com
   * dados reais calculados via [RelationshipRules].
   */
  fun observeTeachers(companyId: Int): Flow<List<Teacher>> =
      combine(
          dao.observeTeachers(companyId),
          dao.observeClasses(companyId),
          dao.observeStudents(companyId),
      ) { teacherEntities, classEntities, studentEntities ->
        val classes = classEntities.map { it.toModel() }
        val students = studentEntities.map { it.toModel() }
        val activeCoursesMap = RelationshipRules.realActiveCoursesCountByTeacher(classes)
        val studentsMap = RelationshipRules.realStudentsByTeacher(students, classes)
        teacherEntities.map { entity ->
          val model = entity.toModel()
          model.copy(
              activeCourses = activeCoursesMap[model.name.trim()] ?: 0,
              totalStudents = studentsMap[model.name.trim()] ?: 0,
          )
        }
      }

  /** Observa todos os instrutores de todas as empresas (visão de administrador). */
  fun observeAllTeachers(): Flow<List<Teacher>> =
      combine(
          dao.observeAllTeachers(),
          dao.observeAllClasses(),
          dao.observeAllStudents(),
      ) { teacherEntities, classEntities, studentEntities ->
        val classes = classEntities.map { it.toModel() }
        val students = studentEntities.map { it.toModel() }
        val activeCoursesMap = RelationshipRules.realActiveCoursesCountByTeacher(classes)
        val studentsMap = RelationshipRules.realStudentsByTeacher(students, classes)
        teacherEntities.map { entity ->
          val model = entity.toModel()
          model.copy(
              activeCourses = activeCoursesMap[model.name.trim()] ?: 0,
              totalStudents = studentsMap[model.name.trim()] ?: 0,
          )
        }
      }

  /** Observa um instrutor específico pelo [teacherId]. */
  fun observeTeacherById(teacherId: Int): Flow<Teacher?> =
      dao.observeTeacherById(teacherId).map { it?.toModel() }

  // ── Students (filtered by company) ────────────────────────────────────

  /** Observa alunos da empresa [companyId]. */
  fun observeStudents(companyId: Int): Flow<List<Student>> =
      dao.observeStudents(companyId).map { items -> items.map { it.toModel() } }

  /** Observa todos os alunos de todas as empresas (visão de administrador). */
  fun observeAllStudents(): Flow<List<Student>> =
      dao.observeAllStudents().map { items -> items.map { it.toModel() } }

  /** Observa alunos vinculados ao responsável [guardianUserId] na empresa [companyId]. */
  fun observeStudentsByGuardian(guardianUserId: Int, companyId: Int): Flow<List<Student>> =
      dao.observeStudentsByGuardian(guardianUserId, companyId).map { items ->
        items.map { it.toModel() }
      }

  /** Observa alunos matriculados na turma [className] da empresa [companyId]. */
  fun observeStudentsByClass(companyId: Int, className: String): Flow<List<Student>> =
      dao.observeStudentsByClass(companyId, className).map { items -> items.map { it.toModel() } }

  // ── Certificates (filtered by company) ────────────────────────────────

  /** Observa certificados emitidos na empresa [companyId]. */
  fun observeCertificates(companyId: Int): Flow<List<Certificate>> =
      dao.observeCertificates(companyId).map { items -> items.map { it.toModel() } }

  /** Observa todos os certificados de todas as empresas (visão de administrador). */
  fun observeAllCertificates(): Flow<List<Certificate>> =
      dao.observeAllCertificates().map { items -> items.map { it.toModel() } }

  // ── Calendar Events (filtered by company) ─────────────────────────────

  /** Observa eventos de calendário da empresa [companyId], ordenados cronologicamente. */
  fun observeCalendarEvents(companyId: Int): Flow<List<CalendarEvent>> =
      dao.observeCalendarEvents(companyId).map { items -> items.map { it.toModel() } }

  // ── Recent Activities (filtered by company) ───────────────────────────

  /** Observa todas as atividades recentes da empresa [companyId]. */
  fun observeRecentActivities(companyId: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivities(companyId).map { items -> items.map { it.toModel() } }

  /** Observa as últimas [limit] atividades recentes da empresa [companyId]. */
  fun observeRecentActivities(companyId: Int, limit: Int): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesLimited(companyId, limit).map { items ->
        items.map { it.toModel() }
      }

  /** Observa atividades recentes da empresa [companyId] com paginação [limit]/[offset]. */
  fun observeRecentActivitiesPaged(
      companyId: Int,
      limit: Int,
      offset: Int,
  ): Flow<List<RecentActivity>> =
      dao.observeRecentActivitiesPaged(companyId, limit, offset).map { items ->
        items.map { it.toModel() }
      }

  /** Observa a contagem total de atividades recentes na empresa [companyId]. */
  fun observeRecentActivitiesCount(companyId: Int): Flow<Int> =
      dao.observeRecentActivitiesCount(companyId)

  // ── Home Quick Stats (filtered by company) ────────────────────────────

  /** Observa estatísticas rápidas (dashboard) da empresa [companyId]. */
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

  /** Observa indicadores agregados de relatórios da empresa [companyId]. */
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

  /** Observa métricas de taxa de conclusão por curso na empresa [companyId]. */
  fun observeCourseCompletionMetrics(companyId: Int): Flow<List<CourseCompletionMetric>> =
      dao.observeCourses(companyId).map { courses ->
        courses
            .map { CourseCompletionMetric(name = it.title, rate = it.completionRate) }
            .sortedByDescending { it.rate }
      }

  /** Observa evolução mensal de matrículas na empresa [companyId]. */
  fun observeMonthlyEnrollmentMetrics(companyId: Int): Flow<List<MonthlyEnrollmentMetric>> =
      dao.observeMonthlyEnrollments(companyId).map { items ->
        items.map { MonthlyEnrollmentMetric(month = it.month, count = it.count) }
      }

  /** Observa distribuição de alunos por status acadêmico na empresa [companyId]. */
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

  /**
   * Observa o snapshot completo de acompanhamento do aluno [studentId], agregando frequência,
   * comportamento, necessidades pedagógicas/psicológicas e contatos familiares.
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

  /** Importa uma lista de alunos parseados de CSV para o banco local via inserção em lote. */
  suspend fun importStudents(entities: List<StudentEntity>) {
    dao.insertStudents(entities)
  }

  /** Importa uma lista de cursos parseados de CSV para o banco local via inserção em lote. */
  suspend fun importCourses(entities: List<CourseEntity>) {
    dao.insertCourses(entities)
  }

  /** Importa uma lista de turmas parseadas de CSV para o banco local via inserção em lote. */
  suspend fun importClasses(entities: List<SchoolClassEntity>) {
    dao.insertClasses(entities)
  }

  /** Importa uma lista de instrutores parseados de CSV para o banco local via inserção em lote. */
  suspend fun importTeachers(entities: List<TeacherEntity>) {
    dao.insertTeachers(entities)
  }

  /** Observa as configurações do aplicativo reativamente, fornecendo defaults quando inexistentes. */
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

  /** Observa o usuário pelo [userId]. Retorna `null` se não encontrado. */
  fun observeUserById(userId: Int): Flow<AppUser?> =
      dao.observeUserById(userId).map { it?.toModel() }

  /**
   * Observa a lista de usuários cadastrados; disponível apenas para administradores e coordenadores.
   *
   * @param requester Usuário que solicita a operação; perfis sem permissão recebem lista vazia.
   */
  fun observeRegisteredUsersForAdmin(requester: AppUser?): Flow<List<AppUser>> {
    return if (
        requester?.role == UserRole.ADMINISTRADOR || requester?.role == UserRole.COORDENADOR
    ) {
      dao.observeUsers().map { items -> items.map { it.toModel() } }
    } else {
      flowOf(emptyList())
    }
  }

  /**
   * Cria ou atualiza um usuário no sistema. Apenas administradores e coordenadores podem executar.
   *
   * @param requester Usuário que solicita a operação.
   * @param user Dados do usuário a cadastrar ou atualizar.
   * @param plainPassword Senha em texto plano (será hasheada antes de persistir).
   * @throws SecurityException Quando o [requester] não possui permissão suficiente.
   * @throws IllegalArgumentException Quando a [plainPassword] é vazia.
   */
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

  /**
   * Ativa ou desativa o modo escuro.
   *
   * @param enabled `true` para ativar o dark mode.
   */
  suspend fun updateDarkMode(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(darkMode = enabled))
  }

  /**
   * Ativa ou desativa notificações push.
   *
   * @param enabled `true` para receber notificações push.
   */
  suspend fun updatePushEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(pushEnabled = enabled))
  }

  /**
   * Ativa ou desativa notificações por e-mail.
   *
   * @param enabled `true` para receber notificações por e-mail.
   */
  suspend fun updateEmailEnabled(enabled: Boolean) {
    val current = dao.observeSettingsOnce()
    dao.upsertSettings((current ?: defaultSettingsEntity()).copy(emailEnabled = enabled))
  }

  /**
   * Atualiza o idioma preferido do aplicativo.
   *
   * @param language Código ISO 639-1 do idioma (ex.: `"pt"`, `"en"`).
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

  /**
   * Executa o reset completo da base de dados local, preservando os usuários cadastrados.
   *
   * Delega internamente para [clearStoragePreservingUsers] já que a lógica é idêntica. Caso no
   * futuro o reset precise remover dados adicionais (ex.: vínculos guardian-student), este método
   * deve ser estendido de forma independente.
   */
  suspend fun resetAllData() {
    clearStoragePreservingUsers()
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

/** Converte [CompanyEntity] para o modelo de domínio [Company]. */
private fun CompanyEntity.toModel() =
    Company(id = id, name = name, cnpj = cnpj, isActive = isActive)

/** Converte [Company] para [CompanyEntity] para persistência Room. */
private fun Company.toEntity() =
    CompanyEntity(id = id, name = name, cnpj = cnpj, isActive = isActive)

/** Converte [CourseEntity] para o modelo de domínio [Course]. */
private fun CourseEntity.toModel() =
    Course(
        id = id,
        title = title,
        category = category,
        instructor = instructor,
        totalStudents = totalStudents,
        durationHours = durationHours,
        completionRate =
            if (startDate != null) computeCompletionRate(startDate, durationHours)
            else completionRate,
        isPublished = isPublished,
        startDate = startDate,
    )

/** Converte [SchoolClassEntity] para o modelo de domínio [SchoolClass]. */
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

/** Converte [TeacherEntity] para o modelo de domínio [Teacher]. */
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

/** Converte [StudentEntity] para o modelo de domínio [Student]. */
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

/** Converte [CertificateEntity] para o modelo de domínio [Certificate]. */
private fun CertificateEntity.toModel() =
    Certificate(
        id = id,
        studentName = studentName,
        courseName = courseName,
        issuedDate = issuedDate,
        hours = hours,
        code = code,
    )

/** Converte [CalendarEventEntity] para o modelo de domínio [CalendarEvent]. */
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

/**
 * Converte [RecentActivityEntity] para o modelo de domínio [RecentActivity], mapeando a `iconKey`
 * string para o [ImageVector] correspondente.
 */
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

/** Converte [AttendanceEntity] para o modelo de domínio [AttendanceRecord]. */
private fun AttendanceEntity.toModel() =
    AttendanceRecord(
        date = date,
        status = status,
        minutesLate = minutesLate,
        justification = justification,
    )

/** Converte [BehaviorEntity] para o modelo de domínio [BehaviorRecord]. */
private fun BehaviorEntity.toModel() =
    BehaviorRecord(
        date = date,
        participationScore = participationScore,
        activityDelivery = activityDelivery,
        delayMinutes = delayMinutes,
        grade = grade,
        note = note,
    )

/** Converte [PedagogicalNeedEntity] para o modelo de domínio [PedagogicalNeed]. */
private fun PedagogicalNeedEntity.toModel() =
    PedagogicalNeed(
        type = type,
        description = description,
        expiresAt = expiresAt,
        accommodations = accommodations,
    )

/** Converte [PsychologicalNeedEntity] para o modelo de domínio [PsychologicalNeed]. */
private fun PsychologicalNeedEntity.toModel() =
    PsychologicalNeed(
        summary = summary,
        confidentiality = confidentiality,
        nextStep = nextStep,
        reviewAt = reviewAt,
    )

/** Converte [ParentFollowUpEntity] para o modelo de domínio [ParentFollowUp]. */
private fun ParentFollowUpEntity.toModel() =
    ParentFollowUp(
        date = date,
        channel = channel,
        outcome = outcome,
        responsible = responsible,
        notes = notes,
    )

/** Converte [AppUserEntity] para o modelo de domínio [AppUser]. */
private fun AppUserEntity.toModel() = AppUser(id = id, name = name, email = email, role = role)

/**
 * Converte [AppUser] para [AppUserEntity] registrando o hash SHA-256 da senha.
 *
 * @param passwordHash Hash SHA-256 da senha do usuário para armazenamento local seguro.
 */
private fun AppUser.toEntity(passwordHash: String) =
    AppUserEntity(id = id, name = name, email = email, role = role, passwordHash = passwordHash)

/**
 * Calcula o hash SHA-256 hexadecimal da string fornecida para armazenamento seguro de senhas.
 *
 * **SEGURANÇA:** SHA-256 puro (sem salt) é vulnerável a ataques de rainbow table. Em produção,
 * substituir por bcrypt, scrypt ou PBKDF2 com salt aleatório por usuário.
 *
 * @param input Valor a ser hasheado.
 * @return Hash SHA-256 em representação hexadecimal minúscula.
 */
private fun sha256(input: String): String {
  val digest = MessageDigest.getInstance("SHA-256")
  return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

/**
 * Calcula a taxa de conclusão com base na data de início e carga horária do curso.
 *
 * Conta as horas e minutos passados desde [startDate] até agora, dividindo pela carga total
 * [durationHours]. O resultado é clampeado entre `0f` e `1f`.
 */
private fun computeCompletionRate(startDate: String, durationHours: Int): Float {
  if (durationHours <= 0) return 0f
  return try {
    val start = LocalDate.parse(startDate).atStartOfDay()
    val now = LocalDateTime.now()
    val elapsedHours = ChronoUnit.HOURS.between(start, now).coerceAtLeast(0L)
    (elapsedHours.toFloat() / durationHours.toFloat()).coerceIn(0f, 1f)
  } catch (_: Exception) {
    0f
  }
}

/** Converte valor nulo em zero para manter cálculos agregados consistentes. */
private fun Int?.orZero(): Int = this ?: 0
