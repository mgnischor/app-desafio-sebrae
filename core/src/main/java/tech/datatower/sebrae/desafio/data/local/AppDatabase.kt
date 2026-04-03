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

import androidx.room.ColumnInfo
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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/**
 * Entidade Room que representa uma empresa (escola) no banco de dados local.
 *
 * @property id Identificador único da empresa.
 * @property name Nome de exibição da empresa.
 * @property cnpj CNPJ da empresa; string vazia quando não informado.
 * @property isActive Indica se a empresa está ativa no sistema.
 */
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

/**
 * Entidade Room que representa um curso no banco de dados local.
 *
 * @property id Identificador único do curso.
 * @property companyId Identificador da empresa dona do registro.
 * @property title Título de exibição do curso.
 * @property category Categoria temática do curso.
 * @property instructor Nome do instrutor principal.
 * @property totalStudents Quantidade total de alunos matriculados (calculado pelo repositório).
 * @property durationHours Carga horária total em horas.
 * @property completionRate Taxa de conclusão no intervalo de `0f..1f`.
 * @property isPublished Indica se o curso está publicado para matrícula.
 * @property startDate Data de início do curso no formato ISO-8601, ou `null` se não definida.
 */
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
    @ColumnInfo(defaultValue = "NULL") val startDate: String? = null,
)

/**
 * Entidade Room que representa uma turma no banco de dados local.
 *
 * @property id Identificador único da turma.
 * @property companyId Identificador da empresa dona do registro.
 * @property name Nome ou código da turma.
 * @property course Curso ao qual a turma está vinculada.
 * @property instructor Nome do instrutor responsável.
 * @property studentsCount Quantidade atual de alunos matriculados (calculado pelo repositório).
 * @property maxCapacity Capacidade máxima de alunos.
 * @property schedule Descrição textual do horário das aulas.
 * @property status Estado atual da turma.
 */
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

/**
 * Entidade Room que representa um instrutor no banco de dados local.
 *
 * @property id Identificador único do instrutor.
 * @property companyId Identificador da empresa dona do registro.
 * @property name Nome completo do instrutor.
 * @property email E-mail institucional.
 * @property specialty Área principal de atuação.
 * @property activeCourses Quantidade de cursos em que atua atualmente.
 * @property totalStudents Total de alunos atendidos nas turmas ativas.
 * @property rating Avaliação média no intervalo de `0f..5f`.
 * @property isActive Indica se o instrutor está ativo.
 */
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

/**
 * Entidade Room que representa um aluno no banco de dados local.
 *
 * @property id Identificador único do aluno.
 * @property companyId Identificador da empresa dona do registro.
 * @property name Nome completo do aluno.
 * @property email E-mail de contato.
 * @property course Nome do curso principal.
 * @property enrolledClass Identificação da turma atual.
 * @property progress Progresso de conclusão no intervalo de `0f..1f`.
 * @property status Situação acadêmica atual.
 */
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

/** Vínculo entre responsável (guardian) e aluno — permite filtrar alunos por responsável. */
@Entity(tableName = "guardian_students")
data class GuardianStudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val guardianUserId: Int,
    val studentId: Int,
)

/**
 * Entidade Room que representa um certificado emitido no banco de dados local.
 *
 * @property id Identificador único do certificado.
 * @property companyId Identificador da empresa dona do registro.
 * @property studentName Nome do aluno certificado.
 * @property courseName Nome do curso concluído.
 * @property issuedDate Data de emissão no formato de exibição.
 * @property hours Carga horária certificada em horas.
 * @property code Código único de validação do certificado.
 */
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

/**
 * Entidade Room que representa um evento do calendário no banco de dados local.
 *
 * @property id Identificador único do evento.
 * @property companyId Identificador da empresa dona do registro.
 * @property title Título principal do evento.
 * @property course Curso ou contexto institucional associado.
 * @property date Data textual usada para agrupamento e exibição.
 * @property time Faixa de horário do evento.
 * @property location Local físico ou referência de sala.
 * @property type Tipo funcional do evento para regras visuais e filtros.
 */
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

/**
 * Entidade Room que representa uma atividade recente no banco de dados local.
 *
 * Usada para popular o feed de atividades recentes no dashboard.
 *
 * @property id Identificador estável para chaveamento de listas no Compose.
 * @property companyId Identificador da empresa dona do registro.
 * @property title Título do evento recente.
 * @property subtitle Descrição complementar do evento.
 * @property iconKey Chave string que mapeia o ícone vetorial correspondente.
 * @property timeLabel Marcador textual de tempo relativo.
 */
@Entity(tableName = "recent_activities")
data class RecentActivityEntity(
    @PrimaryKey val id: Int,
    val companyId: Int,
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val timeLabel: String,
)

/**
 * Entidade Room que armazena a contagem de matrículas agrupadas por mês.
 *
 * Usada para gerar o gráfico de evolução de matrículas na tela de relatórios.
 *
 * @property id Identificador gerado automaticamente.
 * @property companyId Identificador da empresa dona do registro.
 * @property month Mês de referência no formato `"YYYY-MM"`.
 * @property count Quantidade de matrículas registradas no mês.
 */
@Entity(tableName = "monthly_enrollments")
data class MonthlyEnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val month: String,
    val count: Int,
)

/**
 * Entidade Room que representa um registro unitário de frequência de aluno.
 *
 * @property id Identificador gerado automaticamente.
 * @property companyId Identificador da empresa dona do registro.
 * @property studentId Identificador do aluno ao qual o registro pertence.
 * @property date Data do registro.
 * @property status Situação de presença no período.
 * @property minutesLate Minutos de atraso; zero quando não aplicável.
 * @property justification Justificativa textual para faltas justificadas.
 */
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

/**
 * Entidade Room que representa um registro de comportamento e desempenho acadêmico de um aluno.
 *
 * @property id Identificador gerado automaticamente.
 * @property companyId Identificador da empresa dona do registro.
 * @property studentId Identificador do aluno ao qual o registro pertence.
 * @property date Data da observação.
 * @property participationScore Escala de participação no intervalo de 1 a 5.
 * @property activityDelivery Situação da entrega de atividade.
 * @property delayMinutes Minutos de atraso no período.
 * @property grade Nota acadêmica opcional no intervalo de 0 a 10.
 * @property note Observação contextual do professor.
 */
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

/**
 * Entidade Room que representa uma necessidade pedagógica formalizada para um aluno.
 *
 * @property id Identificador gerado automaticamente.
 * @property companyId Identificador da empresa dona do registro.
 * @property studentId Identificador do aluno ao qual a necessidade pertence.
 * @property type Tipo de necessidade registrada.
 * @property description Descrição objetiva do cenário pedagógico.
 * @property expiresAt Data de validade do documento, quando existir.
 * @property accommodations Adaptações ativas orientadas pela equipe.
 */
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

/**
 * Entidade Room que representa um registro de acompanhamento psicológico de um aluno.
 *
 * @property id Identificador gerado automaticamente.
 * @property companyId Identificador da empresa dona do registro.
 * @property studentId Identificador do aluno ao qual o registro pertence.
 * @property summary Resumo clínico/pedagógico compartilhável no contexto autorizado.
 * @property confidentiality Nível de sigilo do registro.
 * @property nextStep Próxima ação planejada no acompanhamento.
 * @property reviewAt Data prevista de revisão do caso.
 */
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

/**
 * Entidade Room que representa um histórico de contato com responsáveis de um aluno.
 *
 * @property id Identificador gerado automaticamente.
 * @property companyId Identificador da empresa dona do registro.
 * @property studentId Identificador do aluno ao qual o contato se refere.
 * @property date Data do contato.
 * @property channel Canal utilizado para comunicação.
 * @property outcome Resultado atual do acompanhamento com a família.
 * @property responsible Profissional que conduziu o contato.
 * @property notes Observações do atendimento.
 */
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

/**
 * Entidade Room que armazena as preferências persistidas do usuário na tabela `settings`.
 *
 * @property id Sempre igual a `1`; tabela compatível com um único registro.
 * @property darkMode Indica se o tema escuro está habilitado.
 * @property pushEnabled Indica se as notificações push estão ativadas.
 * @property emailEnabled Indica se o envio de notificações por e-mail está ativado.
 * @property language Código do idioma da interface.
 */
@Entity(tableName = "settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val darkMode: Boolean,
    val pushEnabled: Boolean,
    val emailEnabled: Boolean,
    val language: String = "pt",
)

/**
 * Entidade Room que representa um usuário cadastrado na tabela `app_users`.
 *
 * Armazena dados de perfil e o hash da senha para autenticação local (fallback offline).
 *
 * @property id Identificador único do usuário.
 * @property name Nome completo exibido na interface.
 * @property email Endereço de e-mail utilizado como credencial de login.
 * @property role Nível de permissão que determina os módulos acessíveis.
 * @property passwordHash Hash SHA-256 da senha utilizado na autenticação local.
 */
@Entity(tableName = "app_users")
data class AppUserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val role: UserRole,
    val passwordHash: String,
)

/**
 * Conversores de tipo registrados no Room para serializar valores complexos em colunas SQLite.
 *
 * Suporta conversão bidirecional de [LocalDate] e de listas de string (usada em `accommodations`).
 */
class AppConverters {
  /**
   * Serializa um [LocalDate] no formato ISO-8601 (`"YYYY-MM-DD"`), ou `null` se o valor for nulo.
   */
  @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()

  /** Desserializa uma string ISO-8601 para [LocalDate], ou `null` se o valor for nulo. */
  @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

  /** Serializa uma lista de strings em um único valor delimitado por `"||"`. */
  @TypeConverter fun accommodationsToString(items: List<String>): String = items.joinToString("||")

  /** Desserializa o valor delimitado por `"||"` de volta para lista de strings. */
  @TypeConverter
  fun stringToAccommodations(value: String): List<String> =
      if (value.isBlank()) emptyList() else value.split("||")
}

/**
 * Interface DAO (Data Access Object) do Room com todas as operações de persistência local.
 *
 * Organizada por domínio (empresa, turma, curso, aluno, etc.) e inclui operações de inserção,
 * consulta reativa por [Flow], consultas pontuais suspensas e exclusões em lote.
 */
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

  @Query("SELECT * FROM companies ORDER BY name")
  suspend fun getCompaniesOnce(): List<CompanyEntity>

  @Query("DELETE FROM companies WHERE id = :companyId")
  suspend fun deleteCompanyById(companyId: Int)

  @Query("DELETE FROM companies") suspend fun clearCompanies()

  // ── UserCompany CRUD ──────────────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserCompanies(items: List<UserCompanyEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserCompany(item: UserCompanyEntity)

  @Query(
      "SELECT c.* FROM companies c INNER JOIN user_companies uc ON c.id = uc.companyId WHERE uc.userId = :userId ORDER BY c.name"
  )
  fun observeCompaniesForUser(userId: Int): Flow<List<CompanyEntity>>

  @Query(
      "SELECT c.* FROM companies c INNER JOIN user_companies uc ON c.id = uc.companyId WHERE uc.userId = :userId ORDER BY c.name"
  )
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

  /** Observa em tempo real todos os cursos da empresa informada, ordenados por título. */
  @Query("SELECT * FROM courses WHERE companyId = :companyId ORDER BY title")
  fun observeCourses(companyId: Int): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses ORDER BY title") fun observeAllCourses(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  fun observeCourseById(courseId: Int): Flow<CourseEntity?>

  // ── Classes (filtered by company) ─────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClasses(items: List<SchoolClassEntity>)

  @Query("SELECT * FROM school_classes WHERE companyId = :companyId ORDER BY name")
  fun observeClasses(companyId: Int): Flow<List<SchoolClassEntity>>

  @Query("SELECT * FROM school_classes ORDER BY name")
  fun observeAllClasses(): Flow<List<SchoolClassEntity>>

  @Query("SELECT * FROM school_classes WHERE id = :classId LIMIT 1")
  fun observeClassById(classId: Int): Flow<SchoolClassEntity?>

  @Query(
      "SELECT * FROM school_classes WHERE companyId = :companyId AND course = :courseName ORDER BY name"
  )
  fun observeClassesByCourse(companyId: Int, courseName: String): Flow<List<SchoolClassEntity>>

  @Query(
      "SELECT * FROM school_classes WHERE companyId = :companyId AND instructor = :teacherName ORDER BY name"
  )
  fun observeClassesByTeacher(companyId: Int, teacherName: String): Flow<List<SchoolClassEntity>>

  // ── Teachers (filtered by company) ────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTeachers(items: List<TeacherEntity>)

  @Query("SELECT * FROM teachers WHERE companyId = :companyId ORDER BY name")
  fun observeTeachers(companyId: Int): Flow<List<TeacherEntity>>

  @Query("SELECT * FROM teachers ORDER BY name") fun observeAllTeachers(): Flow<List<TeacherEntity>>

  @Query("SELECT * FROM teachers WHERE id = :teacherId LIMIT 1")
  fun observeTeacherById(teacherId: Int): Flow<TeacherEntity?>

  // ── Students (filtered by company) ────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudents(items: List<StudentEntity>)

  @Query("SELECT * FROM students WHERE companyId = :companyId ORDER BY name")
  fun observeStudents(companyId: Int): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students ORDER BY name") fun observeAllStudents(): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
  fun observeStudentById(studentId: Int): Flow<StudentEntity?>

  @Query(
      "SELECT * FROM students WHERE companyId = :companyId AND enrolledClass = :className ORDER BY name"
  )
  fun observeStudentsByClass(companyId: Int, className: String): Flow<List<StudentEntity>>

  // ── Guardian-Student links ────────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGuardianStudents(items: List<GuardianStudentEntity>)

  @Query(
      "SELECT s.* FROM students s INNER JOIN guardian_students gs ON s.id = gs.studentId WHERE gs.guardianUserId = :guardianUserId AND s.companyId = :companyId ORDER BY s.name"
  )
  fun observeStudentsByGuardian(guardianUserId: Int, companyId: Int): Flow<List<StudentEntity>>

  @Query("DELETE FROM guardian_students") suspend fun clearGuardianStudents()

  // ── Certificates (filtered by company) ────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCertificates(items: List<CertificateEntity>)

  @Query("SELECT * FROM certificates WHERE companyId = :companyId ORDER BY id DESC")
  fun observeCertificates(companyId: Int): Flow<List<CertificateEntity>>

  @Query("SELECT * FROM certificates ORDER BY id DESC")
  fun observeAllCertificates(): Flow<List<CertificateEntity>>

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

  @Query(
      "SELECT * FROM recent_activities WHERE companyId = :companyId ORDER BY id DESC LIMIT :limit"
  )
  fun observeRecentActivitiesLimited(companyId: Int, limit: Int): Flow<List<RecentActivityEntity>>

  @Query(
      "SELECT * FROM recent_activities WHERE companyId = :companyId ORDER BY id DESC LIMIT :limit OFFSET :offset"
  )
  fun observeRecentActivitiesPaged(
      companyId: Int,
      limit: Int,
      offset: Int,
  ): Flow<List<RecentActivityEntity>>

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
            GuardianStudentEntity::class,
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
    version = 10,
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

  companion object {
    /** Migração da versão 9 para 10: adiciona coluna `startDate` à tabela `courses`. */
    val MIGRATION_9_10 =
        object : Migration(9, 10) {
          override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE courses ADD COLUMN startDate TEXT DEFAULT NULL")
          }
        }
  }
}
