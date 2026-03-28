/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/remote/firebase/FirebaseDataConnectService.kt
    Descrição: Serviço de integração com o Firebase Data Connect para operações de dados.
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
package tech.datatower.sebrae.desafio.data.remote.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.AppSettingsEntity
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
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.EntityLifecycleRules
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.data.model.MutationAction
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.RealtimeNotificationRules
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import java.security.MessageDigest
import java.time.LocalDate

/**
 * Serviço de integração com Firebase Data Connect.
 *
 * Fornece operações de sincronização com o Data Connect do Firebase, permitindo buscar dados
 * remotos e sincronizar com cache local (Room).
 *
 * Cada operação retorna Result<T> para melhor tratamento de erros.
 */
class FirebaseDataConnectService(
    private val firestore: FirebaseFirestore,
    private val dao: AppDao,
    private val credentialStore: FirebaseSeedCredentialStore,
    private val context: Context,
    private val seedEmail: String = "",
    private val seedPassword: String = "",
) {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Modelo e comportamento relacionados a managed user. */
  data class ManagedUser(
      val id: Int,
      val name: String,
      val email: String,
      val role: UserRole,
      val passwordHash: String,
  )

  /**
   * Resultado genérico para operações assíncronas.
   *
   * Success: operação bem-sucedida com dados Error: falha na operação com mensagem de erro Loading:
   * operação em progresso
   */
  sealed class Result<out T> {
    /** Modelo e comportamento relacionados a success. */
    data class Success<T>(val data: T) : Result<T>()

    /** Modelo e comportamento relacionados a error. */
    data class Error(val exception: Exception, val message: String = "") : Result<Nothing>()

    data object Loading : Result<Nothing>()
  }

  companion object {
    private const val TAG = "DataConnect"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_STUDENTS = "students"
    private const val COLLECTION_COURSES = "courses"
    private const val COLLECTION_CLASSES = "school_classes"
    private const val COLLECTION_TEACHERS = "teachers"
    private const val COLLECTION_CERTIFICATES = "certificates"
    private const val COLLECTION_CALENDAR_EVENTS = "calendar_events"
    private const val COLLECTION_RECENT_ACTIVITIES = "recent_activities"
    private const val COLLECTION_MONTHLY_ENROLLMENTS = "monthly_enrollments"
    private const val COLLECTION_ATTENDANCE = "attendance_records"
    private const val COLLECTION_BEHAVIORS = "behavior_records"
    private const val COLLECTION_PEDAGOGICAL_NEEDS = "pedagogical_needs"
    private const val COLLECTION_PSYCHOLOGICAL_NEEDS = "psychological_needs"
    private const val COLLECTION_PARENT_FOLLOW_UPS = "parent_follow_ups"
    private const val COLLECTION_SETTINGS = "settings"

    @Volatile private var authBootstrapDisabled = false
  }

  /**
   * Cria/popula a collection `courses` no Firestore usando os dados locais já existentes no Room.
   *
   * Fluxo:
   * 1) Verifica se `courses` remoto já possui documentos.
   * 2) Se estiver vazio, lê os cursos locais do cache.
   * 3) Envia em batch para Firestore (idempotente por document id).
   *
   * @return quantidade de documentos enviados ou 0 se a coleção já existia.
   */
  suspend fun seedCoursesCollectionFromLocalCache(): Result<Int> {
    return try {
      val authReady = ensureFirebaseSessionForFirestore()
      if (!authReady) {
        Log.w(
            TAG,
            "Seed seguira sem sessao Firebase Auth. Se as regras exigirem request.auth, a escrita sera negada.",
        )
      }

      val localCourses = dao.observeCourses().first()
      if (localCourses.isEmpty()) {
        return Result.Error(
            exception = IllegalStateException("Cache local de courses está vazio."),
            message = "Nenhum curso local encontrado para popular o Firestore.",
        )
      }

      val batch = firestore.batch()
      localCourses.forEach { entity ->
        val doc = firestore.collection(COLLECTION_COURSES).document(entity.id.toString())
        val payload =
            mapOf(
                "id" to entity.id,
                "title" to entity.title,
                "category" to entity.category,
                "instructor" to entity.instructor,
                "totalStudents" to entity.totalStudents,
                "durationHours" to entity.durationHours,
                "completionRate" to entity.completionRate,
                "isPublished" to entity.isPublished,
            )
        // Merge evita falha por sobrescrita completa e torna o seed idempotente.
        batch.set(doc, payload, SetOptions.merge())
      }

      batch.commit().await()
      Log.d(TAG, "Seed de courses concluído. Total enviado: ${localCourses.size}")
      Result.Success(localCourses.size)
    } catch (e: Exception) {
      val hint =
          when {
            e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                "Verifique Firestore Rules (write). Se usar request.auth != null, ative Anonymous em Firebase Authentication."
            e.message?.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) == true ->
                "Firebase Authentication nao esta configurado para este projeto/app. Ative o provedor Anonymous no console Firebase."
            else -> ""
          }
      Log.e(TAG, "Erro ao popular collection courses no Firestore", e)
      Result.Error(e, "Falha ao criar/popular courses: ${e.message}. $hint")
    }
  }

  /**
   * Popula todas as collections remanescentes no Firestore usando dados locais do Room. Inclui
   * `users` para cadastro de usuarios (gestao admin).
   */
  suspend fun seedAllCollectionsFromLocalCache(
      includeRestrictedCollections: Boolean = false,
  ): Result<Map<String, Int>> {
    val counters = linkedMapOf<String, Int>()
    /**
     * Executa a rotina de merge count dentro do contexto deste componente.
     *
     * @param collection Valor de entrada utilizado por esta operação.
     * @param result Valor de entrada utilizado por esta operação.
     * @return Resultado produzido pela operação em formato `Result<Map<String, Int>>?`.
     */
    fun mergeCount(collection: String, result: Result<Int>): Result<Map<String, Int>>? {
      return when (result) {
        is Result.Success -> {
          counters[collection] = result.data
          null
        }
        is Result.Error -> {
          if (isPermissionDenied(result.exception, result.message)) {
            // Em bootstrap automatico, colecoes sem permissao nao devem impedir as demais.
            Log.w(
                TAG,
                "Seed ignorado para $collection por PERMISSION_DENIED. " +
                    "Ajuste as regras do Firestore para essa colecao se quiser sincroniza-la.",
            )
            counters[collection] = 0
            null
          } else {
            Result.Error(result.exception, result.message)
          }
        }
        Result.Loading -> null
      }
    }

    if (includeRestrictedCollections) {
      val userResult =
          seedCollectionFromMaps(
              collectionName = COLLECTION_USERS,
              items =
                  dao.getUsersOnce().map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "email" to it.email,
                        "role" to it.role.name,
                        "passwordHash" to it.passwordHash,
                    )
                  },
              idSelector = { item -> (item["id"] as Number).toInt().toString() },
          )
      mergeCount(COLLECTION_USERS, userResult)?.let {
        return it
      }
    } else {
      Log.d(
          TAG,
          "Seed da collection users ignorado no bootstrap automatico (restrito a admin).",
      )
    }

    mergeCount(COLLECTION_COURSES, seedCoursesCollectionFromLocalCache())?.let {
      return it
    }

    val classesResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_CLASSES,
            items =
                dao.getClassesOnce().map {
                  mapOf(
                      "id" to it.id,
                      "name" to it.name,
                      "course" to it.course,
                      "instructor" to it.instructor,
                      "studentsCount" to it.studentsCount,
                      "maxCapacity" to it.maxCapacity,
                      "schedule" to it.schedule,
                      "status" to it.status.name,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toInt().toString() },
        )
    mergeCount(COLLECTION_CLASSES, classesResult)?.let {
      return it
    }

    val teachersResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_TEACHERS,
            items =
                dao.getTeachersOnce().map {
                  mapOf(
                      "id" to it.id,
                      "name" to it.name,
                      "email" to it.email,
                      "specialty" to it.specialty,
                      "activeCourses" to it.activeCourses,
                      "totalStudents" to it.totalStudents,
                      "rating" to it.rating,
                      "isActive" to it.isActive,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toInt().toString() },
        )
    mergeCount(COLLECTION_TEACHERS, teachersResult)?.let {
      return it
    }

    val studentsResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_STUDENTS,
            items =
                dao.getStudentsOnce().map {
                  mapOf(
                      "id" to it.id,
                      "name" to it.name,
                      "email" to it.email,
                      "course" to it.course,
                      "enrolledClass" to it.enrolledClass,
                      "progress" to it.progress,
                      "status" to it.status.name,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toInt().toString() },
        )
    mergeCount(COLLECTION_STUDENTS, studentsResult)?.let {
      return it
    }

    val certificatesResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_CERTIFICATES,
            items =
                dao.getCertificatesOnce().map {
                  mapOf(
                      "id" to it.id,
                      "studentName" to it.studentName,
                      "courseName" to it.courseName,
                      "issuedDate" to it.issuedDate,
                      "hours" to it.hours,
                      "code" to it.code,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toInt().toString() },
        )
    mergeCount(COLLECTION_CERTIFICATES, certificatesResult)?.let {
      return it
    }

    val eventsResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_CALENDAR_EVENTS,
            items =
                dao.getCalendarEventsOnce().map {
                  mapOf(
                      "id" to it.id,
                      "title" to it.title,
                      "course" to it.course,
                      "date" to it.date,
                      "time" to it.time,
                      "location" to it.location,
                      "type" to it.type.name,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toInt().toString() },
        )
    mergeCount(COLLECTION_CALENDAR_EVENTS, eventsResult)?.let {
      return it
    }

    val recentsResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_RECENT_ACTIVITIES,
            items =
                dao.getRecentActivitiesOnce().map {
                  mapOf(
                      "id" to it.id,
                      "title" to it.title,
                      "subtitle" to it.subtitle,
                      "iconKey" to it.iconKey,
                      "timeLabel" to it.timeLabel,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toInt().toString() },
        )
    mergeCount(COLLECTION_RECENT_ACTIVITIES, recentsResult)?.let {
      return it
    }

    val monthlyResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_MONTHLY_ENROLLMENTS,
            items =
                dao.getMonthlyEnrollmentsOnce().map {
                  mapOf("month" to it.month, "count" to it.count)
                },
            idSelector = { item -> item["month"].toString() },
        )
    mergeCount(COLLECTION_MONTHLY_ENROLLMENTS, monthlyResult)?.let {
      return it
    }

    val attendanceResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_ATTENDANCE,
            items =
                dao.getAttendanceOnce().map {
                  mapOf(
                      "id" to it.id,
                      "studentId" to it.studentId,
                      "date" to it.date.toString(),
                      "status" to it.status.name,
                      "minutesLate" to it.minutesLate,
                      "justification" to it.justification,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toLong().toString() },
        )
    mergeCount(COLLECTION_ATTENDANCE, attendanceResult)?.let {
      return it
    }

    val behaviorResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_BEHAVIORS,
            items =
                dao.getBehaviorsOnce().map {
                  mapOf(
                      "id" to it.id,
                      "studentId" to it.studentId,
                      "date" to it.date.toString(),
                      "participationScore" to it.participationScore,
                      "activityDelivery" to it.activityDelivery.name,
                      "delayMinutes" to it.delayMinutes,
                      "grade" to it.grade,
                      "note" to it.note,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toLong().toString() },
        )
    mergeCount(COLLECTION_BEHAVIORS, behaviorResult)?.let {
      return it
    }

    val pedagogicalResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_PEDAGOGICAL_NEEDS,
            items =
                dao.getPedagogicalNeedsOnce().map {
                  mapOf(
                      "id" to it.id,
                      "studentId" to it.studentId,
                      "type" to it.type.name,
                      "description" to it.description,
                      "expiresAt" to it.expiresAt?.toString(),
                      "accommodations" to it.accommodations,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toLong().toString() },
        )
    mergeCount(COLLECTION_PEDAGOGICAL_NEEDS, pedagogicalResult)?.let {
      return it
    }

    val psychologicalResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_PSYCHOLOGICAL_NEEDS,
            items =
                dao.getPsychologicalNeedsOnce().map {
                  mapOf(
                      "id" to it.id,
                      "studentId" to it.studentId,
                      "summary" to it.summary,
                      "confidentiality" to it.confidentiality.name,
                      "nextStep" to it.nextStep,
                      "reviewAt" to it.reviewAt.toString(),
                  )
                },
            idSelector = { item -> (item["id"] as Number).toLong().toString() },
        )
    mergeCount(COLLECTION_PSYCHOLOGICAL_NEEDS, psychologicalResult)?.let {
      return it
    }

    val parentResult =
        seedCollectionFromMaps(
            collectionName = COLLECTION_PARENT_FOLLOW_UPS,
            items =
                dao.getParentFollowUpsOnce().map {
                  mapOf(
                      "id" to it.id,
                      "studentId" to it.studentId,
                      "date" to it.date.toString(),
                      "channel" to it.channel.name,
                      "outcome" to it.outcome.name,
                      "responsible" to it.responsible,
                      "notes" to it.notes,
                  )
                },
            idSelector = { item -> (item["id"] as Number).toLong().toString() },
        )
    mergeCount(COLLECTION_PARENT_FOLLOW_UPS, parentResult)?.let {
      return it
    }

    val settingsEntity = dao.observeSettingsOnce()
    if (settingsEntity != null) {
      val settingsResult =
          seedCollectionFromMaps(
              collectionName = COLLECTION_SETTINGS,
              items =
                  listOf(
                      mapOf(
                          "id" to settingsEntity.id,
                          "darkMode" to settingsEntity.darkMode,
                          "pushEnabled" to settingsEntity.pushEnabled,
                          "emailEnabled" to settingsEntity.emailEnabled,
                          "language" to settingsEntity.language,
                      )
                  ),
              idSelector = { "settings" },
          )
      mergeCount(COLLECTION_SETTINGS, settingsResult)?.let {
        return it
      }
    }

    return Result.Success(counters)
  }

  /**
   * Executa a rotina de seed collection from maps dentro do contexto deste componente.
   *
   * @param collectionName Valor de entrada utilizado por esta operação.
   * @param items Valor de entrada utilizado por esta operação.
   * @param idSelector Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Result<Int>`.
   */
  private suspend fun seedCollectionFromMaps(
      collectionName: String,
      items: List<Map<String, Any?>>,
      idSelector: (Map<String, Any?>) -> String,
  ): Result<Int> {
    return try {
      if (items.isEmpty()) return Result.Success(0)

      val batch = firestore.batch()
      items.forEach { payload ->
        val doc = firestore.collection(collectionName).document(idSelector(payload))
        batch.set(doc, payload, SetOptions.merge())
      }
      batch.commit().await()
      Log.d(TAG, "Seed da collection $collectionName concluido. Total enviado: ${items.size}")
      Result.Success(items.size)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao popular collection $collectionName no Firestore", e)
      Result.Error(e, "Falha ao criar/popular $collectionName: ${e.message}")
    }
  }

  /**
   * Verifica se permission denied est? em condi??o v?lida para o fluxo atual.
   *
   * @param exception Valor de entrada utilizado por esta operação.
   * @param message Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Boolean`.
   */
  private fun isPermissionDenied(exception: Exception, message: String): Boolean {
    val raw = (exception.message.orEmpty() + " " + message).uppercase()
    return raw.contains("PERMISSION_DENIED")
  }

  /** Garante uma sessao Firebase Auth para satisfazer regras que exigem request.auth != null. */
  private suspend fun ensureFirebaseSessionForFirestore(): Boolean {
    if (authBootstrapDisabled) return false

    val auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) return true

    // Migra credenciais para storage criptografado apenas se ainda nao existirem.
    if (!credentialStore.hasCredentials()) {
      val seedEmailFromBuild = seedEmail
      val seedPasswordFromBuild = seedPassword
      credentialStore.saveCredentials(seedEmailFromBuild, seedPasswordFromBuild)
    }

    val seedEmail = credentialStore.getEmail()
    val seedPassword = credentialStore.getPassword()
    val seedCredentialsConfigured = credentialStore.hasCredentials()

    var emailAuthError: Exception? = null
    if (seedCredentialsConfigured) {
      try {
        // 1) Tenta credenciais fornecidas localmente (nao versionadas).
        auth.signInWithEmailAndPassword(seedEmail, seedPassword).await()
        Log.d(TAG, "Sessao Firebase criada com usuario de seed.")
        return true
      } catch (e: Exception) {
        emailAuthError = e
      }
    } else {
      Log.d(
          TAG,
          "Credenciais de seed nao configuradas em local.properties. Tentando fallback anonimo.",
      )
    }

    return try {
      // 2) Fallback anonimo para projetos que nao usam email/senha
      auth.signInAnonymously().await()
      Log.d(TAG, "Fallback para sessao anonima aplicado.")
      true
    } catch (anonError: Exception) {
      if (
          anonError.message?.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) == true ||
              emailAuthError?.message?.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ==
                  true
      ) {
        authBootstrapDisabled = true
        Log.w(
            TAG,
            "Firebase Auth sem providers configurados. Ative Email/Senha ou Anonymous em Authentication. " +
                "Enquanto isso, o bootstrap de auth foi desativado para evitar erro repetido.",
        )
      } else {
        Log.w(TAG, "Falha ao autenticar no Firebase Auth para seed remoto.")
      }
      false
    }
  }

  /**
   * Valida pré-condições de auth para operações administrativas da collection users.
   *
   * Garante que existe uma sessão Firebase ativa. A verificação da custom claim `admin=true` é
   * feita em modo informativo: se a claim estiver ausente, um aviso é registrado no log mas a
   * operação prossegue. A segurança de escrita é imposta pelas regras do Firestore; o papel
   * `ADMINISTRADOR` local já foi verificado pelo chamador antes desta função.
   */
  private suspend fun ensureFirebaseAdminClaimForUserManagement(): Result.Error? {
    val authReady = ensureFirebaseSessionForFirestore()
    if (!authReady) {
      return Result.Error(
          SecurityException("Sessao Firebase indisponivel para operacao administrativa."),
          "Autenticacao Firebase obrigatoria para gerenciar usuarios. Faca login novamente e tente de novo.",
      )
    }

    val firebaseUser =
        FirebaseAuth.getInstance().currentUser
            ?: return Result.Error(
                SecurityException("Nenhum usuario Firebase autenticado."),
                "Sessao Firebase ausente. Faca login novamente para continuar.",
            )

    return try {
      // Forca refresh do token para carregar claims atualizadas.
      val tokenResult = firebaseUser.getIdToken(true).await()
      val isAdminClaim = tokenResult.claims["admin"] == true
      if (!isAdminClaim) {
        // Claim ausente: registra aviso e permite prosseguir com base no papel local.
        // Se as regras do Firestore exigirem admin=true, a operação falhará com
        // PERMISSION_DENIED — que é tratado no catch do chamador com uma mensagem clara.
        Log.w(
            TAG,
            "Custom claim admin=true ausente no token Firebase. " +
                "Prosseguindo com base no papel local ADMINISTRADOR. " +
                "Para restringir no servidor, configure a claim via Firebase Admin SDK.",
        )
      }
      null // Permite a operação prosseguir
    } catch (e: Exception) {
      // Falha ao verificar claims: registra aviso mas não bloqueia.
      Log.w(TAG, "Nao foi possivel verificar claims do token Firebase: ${e.message}. Prosseguindo.")
      null
    }
  }

  /**
   * Busca lista de cursos do Data Connect.
   *
   * Operação de Query do Data Connect:
   * - Conecta ao Firestore
   * - Busca documentos da coleção "courses"
   * - Converte para modelo Course
   * - Sincroniza com cache local
   *
   * @return Result<List<Course>> sucesso ou erro
   */
  suspend fun fetchCourses(): Result<List<Course>> {
    return try {
      Log.d(TAG, "Iniciando busca de cursos do Data Connect...")

      val snapshot = firestore.collection(COLLECTION_COURSES).get().await()
      val courses =
          snapshot.documents.mapNotNull { doc ->
            try {
              Course(
                  id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
                  title = doc.getString("title") ?: "",
                  category = doc.getString("category") ?: "",
                  instructor = doc.getString("instructor") ?: "",
                  totalStudents = (doc.get("totalStudents") as? Number)?.toInt() ?: 0,
                  durationHours = (doc.get("durationHours") as? Number)?.toInt() ?: 0,
                  completionRate = (doc.get("completionRate") as? Number)?.toFloat() ?: 0f,
                  isPublished = doc.getBoolean("isPublished") ?: false,
              )
            } catch (e: Exception) {
              Log.w(TAG, "Erro ao converter curso: ${doc.id}", e)
              null
            }
          }

      // Sincronizar com cache local
      if (courses.isNotEmpty()) {
        dao.insertCourses(courses.map { it.toEntity() })
        Log.d(TAG, "Sincronizados ${courses.size} cursos para cache local")
      }

      Result.Success(courses)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar cursos do Data Connect", e)
      Result.Error(e, "Falha ao buscar cursos: ${e.message}")
    }
  }

  /**
   * Cadastra/atualiza usuário na collection `users` do Firestore e no cache Room local.
   *
   * Estratégia de fallback: se a escrita remota falhar por falta de permissão (ex.: regras do
   * Firestore exigindo custom claim `admin=true`), o usuário é salvo apenas localmente. A tela de
   * gestão refletirá o novo usuário imediatamente.
   */
  suspend fun upsertUserForAdmin(
      requester: AppUser?,
      target: AppUser,
      plainPassword: String,
  ): Result<Boolean> {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      return Result.Error(
          SecurityException("Apenas administrador pode cadastrar usuarios."),
          "Acesso negado para cadastro de usuarios.",
      )
    }
    if (plainPassword.isBlank()) {
      return Result.Error(
          IllegalArgumentException("Senha nao pode ser vazia."),
          "Informe uma senha valida.",
      )
    }

    ensureFirebaseAdminClaimForUserManagement()?.let {
      return it
    }

    val normalizedEmail = target.email.trim().lowercase()
    val passwordHash = sha256(plainPassword)

    // ── Cria conta no Firebase Auth (sem deslogar o admin atual) ────────────
    val authUid = createFirebaseAuthUser(normalizedEmail, plainPassword)

    // ── Tenta escrita remota ─────────────────────────────────────────────────
    val firestoreSuccess =
        try {
          val action = resolveMutationAction(COLLECTION_USERS, target.id.toString())
          val payload =
              buildMap<String, Any?> {
                put("id", target.id)
                put("name", target.name)
                put("email", normalizedEmail)
                put("role", target.role.name)
                put("passwordHash", passwordHash)
                if (authUid != null) put("authUid", authUid)
              }
          firestore
              .collection(COLLECTION_USERS)
              .document(target.id.toString())
              .set(payload, SetOptions.merge())
              .await()
          registerRecentActivity(
              requester = requester,
              action = action,
              entityName = "Usuário",
              targetLabel = target.name,
          )
          Log.d(TAG, "Usuario ${target.name} salvo no Firestore.")
          true
        } catch (e: Exception) {
          if (isPermissionDenied(e, e.message.orEmpty())) {
            Log.w(
                TAG,
                "PERMISSION_DENIED ao salvar usuario ${target.name} no Firestore. " +
                    "Salvando apenas localmente. Configure a custom claim admin=true " +
                    "via Firebase Admin SDK para habilitar persistência remota.",
            )
          } else {
            Log.w(
                TAG,
                "Falha ao salvar usuario ${target.name} no Firestore: ${e.message}. Salvando localmente.",
            )
          }
          false
        }

    // ── Salvamento local (sempre executado) ──────────────────────────────────
    try {
      dao.upsertUser(
          tech.datatower.sebrae.desafio.data.local.AppUserEntity(
              id = target.id,
              name = target.name,
              email = normalizedEmail,
              role = target.role,
              passwordHash = passwordHash,
          )
      )
    } catch (e: Exception) {
      if (!firestoreSuccess) {
        // Ambos falharam — retorna erro
        return Result.Error(e, "Falha ao salvar usuario localmente: ${e.message}")
      }
      Log.w(TAG, "Falha ao salvar usuario ${target.name} no cache local: ${e.message}")
    }

    return Result.Success(true)
  }

  /**
   * Remove um usuário da collection `users` no Firestore e do cache Room local.
   *
   * Estratégia de fallback: se a exclusão remota falhar por falta de permissão no Firestore (ex.:
   * custom claim `admin=true` não configurada nas regras), o usuário é removido apenas do
   * armazenamento local. A tela de gestão refletirá a remoção imediatamente.
   */
  suspend fun deleteUserForAdmin(requester: AppUser?, userId: Int): Result<Boolean> {
    if (requester?.role != UserRole.ADMINISTRADOR) {
      return Result.Error(
          SecurityException("Apenas administrador pode remover usuarios."),
          "Acesso negado para remocao de usuarios.",
      )
    }

    ensureFirebaseAdminClaimForUserManagement()?.let {
      return it
    }

    // ── Tenta exclusão remota ────────────────────────────────────────────────
    val firestoreSuccess =
        try {
          firestore.collection(COLLECTION_USERS).document(userId.toString()).delete().await()
          Log.d(TAG, "Usuario $userId removido do Firestore.")
          true
        } catch (e: Exception) {
          if (isPermissionDenied(e, e.message.orEmpty())) {
            Log.w(
                TAG,
                "PERMISSION_DENIED ao remover usuario $userId do Firestore. " +
                    "Verifique as regras de seguranca do Firestore ou configure a custom claim " +
                    "admin=true via Firebase Admin SDK. Removendo apenas localmente.",
            )
          } else {
            Log.w(
                TAG,
                "Falha ao remover usuario $userId do Firestore: ${e.message}. Removendo localmente.",
            )
          }
          false
        }

    // ── Exclusão local (sempre executada) ────────────────────────────────────
    try {
      dao.deleteUserById(userId)
    } catch (e: Exception) {
      Log.w(TAG, "Falha ao remover usuario $userId do cache local: ${e.message}")
    }

    // ── Atividade recente (apenas se Firestore sincronizou) ──────────────────
    if (firestoreSuccess) {
      registerRecentActivity(
          requester = requester,
          action = MutationAction.Deleted,
          entityName = "Usuário",
          targetLabel = "ID #$userId",
      )
    }

    return Result.Success(true)
  }

  /**
   * Observa altera??es de users realtime for admin e publica atualiza??es reativas.
   *
   * @param requester Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Flow<Result<List<ManagedUser>>>`.
   */
  fun observeUsersRealtimeForAdmin(requester: AppUser?): Flow<Result<List<ManagedUser>>> =
      callbackFlow {
        if (requester?.role != UserRole.ADMINISTRADOR) {
          trySend(
              Result.Error(
                  SecurityException("Apenas administrador pode observar usuarios."),
                  "Acesso negado para leitura de usuarios.",
              )
          )
          close()
          return@callbackFlow
        }

        val registration: ListenerRegistration =
            firestore.collection(COLLECTION_USERS).addSnapshotListener { snapshot, error ->
              if (error != null) {
                trySend(
                    Result.Error(
                        error,
                        "Falha ao observar usuarios em tempo real: ${error.message}",
                    )
                )
                return@addSnapshotListener
              }

              val users =
                  snapshot
                      ?.documents
                      ?.mapNotNull { doc ->
                        val id =
                            (doc.get("id") as? Number)?.toInt()
                                ?: (doc.get("legacyId") as? Number)?.toInt()
                                ?: doc.id.hashCode().let {
                                  if (it == Int.MIN_VALUE) 1 else kotlin.math.abs(it)
                                }
                        val roleName = doc.getString("role").orEmpty()
                        val role =
                            runCatching { UserRole.valueOf(roleName.uppercase()) }
                                .getOrDefault(UserRole.PROFESSOR)
                        ManagedUser(
                            id = id,
                            name = doc.getString("name").orEmpty().ifBlank { "Usuario" },
                            email = doc.getString("email").orEmpty(),
                            role = role,
                            passwordHash = doc.getString("passwordHash").orEmpty(),
                        )
                      }
                      ?.sortedBy { it.name }
                      .orEmpty()

              trySend(Result.Success(users))
            }

        awaitClose { registration.remove() }
      }

  /** Observa atividades recentes em tempo real para perfis de backoffice. */
  fun observeRecentActivitiesRealtimeForBackoffice(
      requester: AppUser?,
  ): Flow<Result<List<RecentActivityEntity>>> = callbackFlow {
    if (!RealtimeNotificationRules.canReceiveBackofficeNotifications(requester?.role)) {
      trySend(Result.Success(emptyList()))
      close()
      return@callbackFlow
    }

    val registration: ListenerRegistration =
        firestore
            .collection(COLLECTION_RECENT_ACTIVITIES)
            .orderBy("id", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
              if (error != null) {
                trySend(
                    Result.Error(error, "Falha ao observar atividades recentes: ${error.message}")
                )
                return@addSnapshotListener
              }

              val entities =
                  snapshot
                      ?.documents
                      ?.mapNotNull { doc ->
                        val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
                        RecentActivityEntity(
                            id = id,
                            title = doc.getString("title").orEmpty(),
                            subtitle = doc.getString("subtitle").orEmpty(),
                            iconKey = doc.getString("iconKey").orEmpty().ifBlank { "calendar" },
                            timeLabel = doc.getString("timeLabel").orEmpty().ifBlank { "agora" },
                        )
                      }
                      ?.sortedByDescending { it.id }
                      .orEmpty()

              if (entities.isNotEmpty()) {
                // Atualiza cache local para renderização reativa da Home.
                serviceScope.launch { dao.insertRecentActivities(entities) }
              }
              trySend(Result.Success(entities))
            }

    awaitClose { registration.remove() }
  }

  /** Busca lista de estudantes do Data Connect. */
  suspend fun fetchStudents(): Result<List<Student>> {
    return try {
      Log.d(TAG, "Iniciando busca de estudantes do Data Connect...")

      val snapshot = firestore.collection(COLLECTION_STUDENTS).get().await()
      val students =
          snapshot.documents.mapNotNull { doc ->
            try {
              Student(
                  id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
                  name = doc.getString("name") ?: "",
                  email = doc.getString("email") ?: "",
                  course = doc.getString("course") ?: "",
                  enrolledClass = doc.getString("enrolledClass") ?: "",
                  progress = (doc.get("progress") as? Number)?.toFloat() ?: 0f,
                  status =
                      runCatching {
                            StudentStatus.valueOf(
                                doc.getString("status").orEmpty().ifBlank { "Active" }
                            )
                          }
                          .getOrDefault(StudentStatus.Active),
              )
            } catch (e: Exception) {
              Log.w(TAG, "Erro ao converter estudante: ${doc.id}", e)
              null
            }
          }

      // Sincronizar com cache local
      if (students.isNotEmpty()) {
        dao.insertStudents(students.map { it.toEntity() })
        Log.d(TAG, "Sincronizados ${students.size} estudantes para cache local")
      }

      Result.Success(students)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar estudantes do Data Connect", e)
      Result.Error(e, "Falha ao buscar estudantes: ${e.message}")
    }
  }

  /** Busca lista de turmas do Data Connect. */
  suspend fun fetchClasses(): Result<List<SchoolClass>> {
    return try {
      Log.d(TAG, "Iniciando busca de turmas do Data Connect...")

      val snapshot = firestore.collection(COLLECTION_CLASSES).get().await()
      val classes =
          snapshot.documents.mapNotNull { doc ->
            try {
              SchoolClass(
                  id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
                  name = doc.getString("name") ?: "",
                  course = doc.getString("course") ?: "",
                  instructor = doc.getString("instructor") ?: "",
                  studentsCount = (doc.get("studentsCount") as? Number)?.toInt() ?: 0,
                  maxCapacity = (doc.get("maxCapacity") as? Number)?.toInt() ?: 0,
                  schedule = doc.getString("schedule") ?: "",
                  status =
                      runCatching {
                            ClassStatus.valueOf(
                                doc.getString("status").orEmpty().ifBlank { "Open" }
                            )
                          }
                          .getOrDefault(ClassStatus.Open),
              )
            } catch (e: Exception) {
              Log.w(TAG, "Erro ao converter turma: ${doc.id}", e)
              null
            }
          }

      if (classes.isNotEmpty()) {
        dao.insertClasses(classes.map { it.toEntity() })
        Log.d(TAG, "Sincronizadas ${classes.size} turmas para cache local")
      }

      Result.Success(classes)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar turmas do Data Connect", e)
      Result.Error(e, "Falha ao buscar turmas: ${e.message}")
    }
  }

  /** Busca lista de professores do Data Connect. */
  suspend fun fetchTeachers(): Result<List<Teacher>> {
    return try {
      Log.d(TAG, "Iniciando busca de professores do Data Connect...")

      val snapshot = firestore.collection(COLLECTION_TEACHERS).get().await()
      val teachers =
          snapshot.documents.mapNotNull { doc ->
            try {
              Teacher(
                  id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null,
                  name = doc.getString("name") ?: "",
                  email = doc.getString("email") ?: "",
                  specialty = doc.getString("specialty") ?: "",
                  activeCourses = (doc.get("activeCourses") as? Number)?.toInt() ?: 0,
                  totalStudents = (doc.get("totalStudents") as? Number)?.toInt() ?: 0,
                  rating = (doc.get("rating") as? Number)?.toFloat() ?: 0f,
                  isActive = doc.getBoolean("isActive") ?: true,
              )
            } catch (e: Exception) {
              Log.w(TAG, "Erro ao converter professor: ${doc.id}", e)
              null
            }
          }

      // Sincronizar com cache local
      if (teachers.isNotEmpty()) {
        dao.insertTeachers(teachers.map { it.toEntity() })
        Log.d(TAG, "Sincronizados ${teachers.size} professores para cache local")
      }

      Result.Success(teachers)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao buscar professores do Data Connect", e)
      Result.Error(e, "Falha ao buscar professores: ${e.message}")
    }
  }

  /** Busca lista de certificados e atualiza o cache local. */
  suspend fun fetchCertificates(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_CERTIFICATES).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
            CertificateEntity(
                id = id,
                studentName = doc.getString("studentName").orEmpty(),
                courseName = doc.getString("courseName").orEmpty(),
                issuedDate = doc.getString("issuedDate").orEmpty(),
                hours = (doc.get("hours") as? Number)?.toInt() ?: 0,
                code = doc.getString("code").orEmpty(),
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertCertificates(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar certificados: ${e.message}")
    }
  }

  /** Busca eventos de calendario e atualiza o cache local. */
  suspend fun fetchCalendarEvents(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_CALENDAR_EVENTS).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
            val typeName = doc.getString("type").orEmpty().ifBlank { EventType.Other.name }
            val type = runCatching { EventType.valueOf(typeName) }.getOrDefault(EventType.Other)
            CalendarEventEntity(
                id = id,
                title = doc.getString("title").orEmpty(),
                course = doc.getString("course").orEmpty(),
                date = doc.getString("date").orEmpty(),
                time = doc.getString("time").orEmpty(),
                location = doc.getString("location").orEmpty(),
                type = type,
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertCalendarEvents(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar eventos de calendario: ${e.message}")
    }
  }

  /** Busca atividades recentes e atualiza o cache local. */
  suspend fun fetchRecentActivities(): Result<Int> {
    return try {
      val snapshot =
          firestore
              .collection(COLLECTION_RECENT_ACTIVITIES)
              .orderBy("id", Query.Direction.DESCENDING)
              .limit(200)
              .get()
              .await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
            RecentActivityEntity(
                id = id,
                title = doc.getString("title").orEmpty(),
                subtitle = doc.getString("subtitle").orEmpty(),
                iconKey = doc.getString("iconKey").orEmpty().ifBlank { "person" },
                timeLabel = doc.getString("timeLabel").orEmpty(),
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertRecentActivities(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar atividades recentes: ${e.message}")
    }
  }

  /** Busca metricas mensais e atualiza o cache local. */
  suspend fun fetchMonthlyEnrollments(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_MONTHLY_ENROLLMENTS).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val month = doc.getString("month").orEmpty()
            if (month.isBlank()) return@mapNotNull null
            MonthlyEnrollmentEntity(
                month = month,
                count = (doc.get("count") as? Number)?.toInt() ?: 0,
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertMonthlyEnrollments(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar matriculas mensais: ${e.message}")
    }
  }

  /** Busca frequencias e atualiza o cache local. */
  suspend fun fetchAttendanceRecords(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_ATTENDANCE).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val studentId = (doc.get("studentId") as? Number)?.toInt() ?: return@mapNotNull null
            val date =
                runCatching { LocalDate.parse(doc.getString("date").orEmpty()) }.getOrNull()
                    ?: return@mapNotNull null
            val statusName =
                doc.getString("status").orEmpty().ifBlank { AttendanceStatus.Present.name }
            val status =
                runCatching { AttendanceStatus.valueOf(statusName) }
                    .getOrDefault(AttendanceStatus.Present)
            AttendanceEntity(
                id = (doc.get("id") as? Number)?.toInt() ?: 0,
                studentId = studentId,
                date = date,
                status = status,
                minutesLate = (doc.get("minutesLate") as? Number)?.toInt() ?: 0,
                justification = doc.getString("justification"),
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertAttendance(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar frequencias: ${e.message}")
    }
  }

  /** Busca registros comportamentais e atualiza o cache local. */
  suspend fun fetchBehaviorRecords(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_BEHAVIORS).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val studentId = (doc.get("studentId") as? Number)?.toInt() ?: return@mapNotNull null
            val date =
                runCatching { LocalDate.parse(doc.getString("date").orEmpty()) }.getOrNull()
                    ?: return@mapNotNull null
            val deliveryName =
                doc.getString("activityDelivery").orEmpty().ifBlank {
                  ActivityDeliveryStatus.OnTime.name
                }
            val delivery =
                runCatching { ActivityDeliveryStatus.valueOf(deliveryName) }
                    .getOrDefault(ActivityDeliveryStatus.OnTime)
            BehaviorEntity(
                id = (doc.get("id") as? Number)?.toInt() ?: 0,
                studentId = studentId,
                date = date,
                participationScore = (doc.get("participationScore") as? Number)?.toInt() ?: 0,
                activityDelivery = delivery,
                delayMinutes = (doc.get("delayMinutes") as? Number)?.toInt() ?: 0,
                grade = (doc.get("grade") as? Number)?.toFloat(),
                note = doc.getString("note").orEmpty(),
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertBehaviors(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar comportamento: ${e.message}")
    }
  }

  /** Busca necessidades pedagogicas e atualiza o cache local. */
  suspend fun fetchPedagogicalNeeds(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_PEDAGOGICAL_NEEDS).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val studentId = (doc.get("studentId") as? Number)?.toInt() ?: return@mapNotNull null
            val typeName =
                doc.getString("type").orEmpty().ifBlank { PedagogicalNeedType.Report.name }
            val type =
                runCatching { PedagogicalNeedType.valueOf(typeName) }
                    .getOrDefault(PedagogicalNeedType.Report)
            val expiresAt =
                runCatching { LocalDate.parse(doc.getString("expiresAt").orEmpty()) }.getOrNull()
            val accommodations =
                (doc.get("accommodations") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            PedagogicalNeedEntity(
                id = (doc.get("id") as? Number)?.toInt() ?: 0,
                studentId = studentId,
                type = type,
                description = doc.getString("description").orEmpty(),
                expiresAt = expiresAt,
                accommodations = accommodations,
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertPedagogicalNeeds(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar necessidades pedagogicas: ${e.message}")
    }
  }

  /** Busca necessidades psicologicas e atualiza o cache local. */
  suspend fun fetchPsychologicalNeeds(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_PSYCHOLOGICAL_NEEDS).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val studentId = (doc.get("studentId") as? Number)?.toInt() ?: return@mapNotNull null
            val reviewAt =
                runCatching { LocalDate.parse(doc.getString("reviewAt").orEmpty()) }.getOrNull()
                    ?: return@mapNotNull null
            val confidentialityName =
                doc.getString("confidentiality").orEmpty().ifBlank {
                  ConfidentialityLevel.Restricted.name
                }
            val confidentiality =
                runCatching { ConfidentialityLevel.valueOf(confidentialityName) }
                    .getOrDefault(ConfidentialityLevel.Restricted)
            PsychologicalNeedEntity(
                id = (doc.get("id") as? Number)?.toInt() ?: 0,
                studentId = studentId,
                summary = doc.getString("summary").orEmpty(),
                confidentiality = confidentiality,
                nextStep = doc.getString("nextStep").orEmpty(),
                reviewAt = reviewAt,
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertPsychologicalNeeds(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar necessidades psicologicas: ${e.message}")
    }
  }

  /** Busca acompanhamentos de responsaveis e atualiza o cache local. */
  suspend fun fetchParentFollowUps(): Result<Int> {
    return try {
      val snapshot = firestore.collection(COLLECTION_PARENT_FOLLOW_UPS).get().await()
      val entities =
          snapshot.documents.mapNotNull { doc ->
            val studentId = (doc.get("studentId") as? Number)?.toInt() ?: return@mapNotNull null
            val date =
                runCatching { LocalDate.parse(doc.getString("date").orEmpty()) }.getOrNull()
                    ?: return@mapNotNull null
            val channelName =
                doc.getString("channel").orEmpty().ifBlank { ParentContactChannel.Meeting.name }
            val channel =
                runCatching { ParentContactChannel.valueOf(channelName) }
                    .getOrDefault(ParentContactChannel.Meeting)
            val outcomeName =
                doc.getString("outcome").orEmpty().ifBlank { ParentFollowUpStatus.Pending.name }
            val outcome =
                runCatching { ParentFollowUpStatus.valueOf(outcomeName) }
                    .getOrDefault(ParentFollowUpStatus.Pending)
            ParentFollowUpEntity(
                id = (doc.get("id") as? Number)?.toInt() ?: 0,
                studentId = studentId,
                date = date,
                channel = channel,
                outcome = outcome,
                responsible = doc.getString("responsible").orEmpty(),
                notes = doc.getString("notes").orEmpty(),
            )
          }
      if (entities.isNotEmpty()) {
        dao.insertParentFollowUps(entities)
      }
      Result.Success(entities.size)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar acompanhamentos de responsaveis: ${e.message}")
    }
  }

  /** Busca configuracoes remotas e atualiza o cache local. */
  suspend fun fetchSettings(): Result<Boolean> {
    return try {
      val snapshot = firestore.collection(COLLECTION_SETTINGS).limit(1).get().await()
      val document = snapshot.documents.firstOrNull()
      if (document != null) {
        dao.upsertSettings(
            AppSettingsEntity(
                id = 1,
                darkMode = document.getBoolean("darkMode") ?: false,
                pushEnabled = document.getBoolean("pushEnabled") ?: true,
                emailEnabled = document.getBoolean("emailEnabled") ?: false,
                language = document.getString("language").orEmpty().ifBlank { "pt" },
            )
        )
      }
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao buscar configuracoes: ${e.message}")
    }
  }

  /**
   * Sincroniza um escopo de tela especifico.
   *
   * @param scope Escopo funcional da tela que requisitou dados.
   * @return `Success(true)` quando todas as tarefas do escopo concluem sem erro.
   */
  suspend fun syncScope(scope: ScreenDataScope): Result<Boolean> {
    val tasks = FirebaseScreenSyncPlanner.tasksFor(scope)
    tasks.forEach { task ->
      when (val result = syncTask(task)) {
        is Result.Success -> Unit
        is Result.Error -> return result
        Result.Loading -> Unit
      }
    }
    return Result.Success(true)
  }

  /** Executa uma tarefa individual de sincronizacao Firebase para o cache local. */
  private suspend fun syncTask(task: FirebaseSyncTask): Result<Boolean> {
    return when (task) {
      FirebaseSyncTask.COURSES -> fetchCourses().toBooleanResult()
      FirebaseSyncTask.CLASSES -> fetchClasses().toBooleanResult()
      FirebaseSyncTask.STUDENTS -> fetchStudents().toBooleanResult()
      FirebaseSyncTask.TEACHERS -> fetchTeachers().toBooleanResult()
      FirebaseSyncTask.CERTIFICATES -> fetchCertificates().toBooleanResult()
      FirebaseSyncTask.CALENDAR_EVENTS -> fetchCalendarEvents().toBooleanResult()
      FirebaseSyncTask.RECENT_ACTIVITIES -> fetchRecentActivities().toBooleanResult()
      FirebaseSyncTask.MONTHLY_ENROLLMENTS -> fetchMonthlyEnrollments().toBooleanResult()
      FirebaseSyncTask.ATTENDANCE -> fetchAttendanceRecords().toBooleanResult()
      FirebaseSyncTask.BEHAVIORS -> fetchBehaviorRecords().toBooleanResult()
      FirebaseSyncTask.PEDAGOGICAL_NEEDS -> fetchPedagogicalNeeds().toBooleanResult()
      FirebaseSyncTask.PSYCHOLOGICAL_NEEDS -> fetchPsychologicalNeeds().toBooleanResult()
      FirebaseSyncTask.PARENT_FOLLOW_UPS -> fetchParentFollowUps().toBooleanResult()
      FirebaseSyncTask.SETTINGS -> fetchSettings()
    }
  }

  /** Converte um resultado tipado em sucesso booleano para pipeline de sincronizacao. */
  private fun <T> Result<T>.toBooleanResult(): Result<Boolean> {
    return when (this) {
      is Result.Success -> Result.Success(true)
      is Result.Error -> this
      Result.Loading -> Result.Loading
    }
  }

  /**
   * Sincroniza todos os dados do Data Connect com cache local.
   *
   * Executa em paralelo para melhor performance.
   */
  suspend fun syncAllData(): Result<Boolean> {
    return syncScope(ScreenDataScope.APP_STARTUP)
  }

  /** Verifica conexão com Firebase. */
  suspend fun checkConnection(): Result<Boolean> {
    return try {
      firestore.collection("_metadata").limit(1).get().await()
      Log.d(TAG, "Conexão com Firebase validada")
      Result.Success(true)
    } catch (e: Exception) {
      Log.w(TAG, "Falha na conexão com Firebase", e)
      Result.Error(e, "Sem conexão com Firebase: ${e.message}")
    }
  }

  /**
   * Exclui todos os documentos de todas as collections no Firestore. Usado no reset completo do
   * banco de dados (somente administrador).
   */
  suspend fun deleteAllCollections(): Result<Boolean> {
    val allCollections =
        listOf(
            COLLECTION_STUDENTS,
            COLLECTION_COURSES,
            COLLECTION_CLASSES,
            COLLECTION_TEACHERS,
            COLLECTION_CERTIFICATES,
            COLLECTION_CALENDAR_EVENTS,
            COLLECTION_RECENT_ACTIVITIES,
            COLLECTION_MONTHLY_ENROLLMENTS,
            COLLECTION_ATTENDANCE,
            COLLECTION_BEHAVIORS,
            COLLECTION_PEDAGOGICAL_NEEDS,
            COLLECTION_PSYCHOLOGICAL_NEEDS,
            COLLECTION_PARENT_FOLLOW_UPS,
            COLLECTION_SETTINGS,
        )
    return try {
      for (collectionName in allCollections) {
        deleteCollection(collectionName)
      }
      Log.d(TAG, "Reset completo do Firestore concluído.")
      Result.Success(true)
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao realizar reset do Firestore", e)
      Result.Error(e, "Falha ao resetar Firestore: ${e.message}")
    }
  }

  /** Deleta todos os documentos de uma collection em lotes de até 500. */
  private suspend fun deleteCollection(collectionName: String) {
    val batchSize = 400L
    var deleted: Int
    do {
      val snapshot = firestore.collection(collectionName).limit(batchSize).get().await()
      deleted = snapshot.size()
      if (deleted > 0) {
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Log.d(TAG, "Deletados $deleted documentos de $collectionName")
      }
    } while (deleted >= batchSize)
  }

  /**
   * Inicia listeners em tempo real do Firestore para todas as collections que alimentam a tela de
   * relatórios. Cada mudança no Firestore atualiza o cache Room, que por sua vez dispara os flows
   * reativos da UI.
   *
   * O flow emite [Unit] a cada atualização recebida e cancela todos os listeners quando é cancelado
   * (ex: ViewModel destruído).
   */
  fun observeReportDataRealtime(): Flow<Unit> = callbackFlow {
    val listeners = mutableListOf<ListenerRegistration>()

    listeners +=
        firestore.collection(COLLECTION_STUDENTS).addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.w(TAG, "Snapshot students error", error)
            return@addSnapshotListener
          }
          val entities =
              snapshot
                  ?.documents
                  ?.mapNotNull { doc ->
                    val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
                    val statusName =
                        doc.getString("status").orEmpty().ifBlank { StudentStatus.Active.name }
                    StudentEntity(
                        id = id,
                        name = doc.getString("name").orEmpty(),
                        email = doc.getString("email").orEmpty(),
                        course = doc.getString("course").orEmpty(),
                        enrolledClass = doc.getString("enrolledClass").orEmpty(),
                        progress = (doc.get("progress") as? Number)?.toFloat() ?: 0f,
                        status =
                            runCatching { StudentStatus.valueOf(statusName) }
                                .getOrDefault(StudentStatus.Active),
                    )
                  }
                  .orEmpty()
          serviceScope.launch {
            dao.clearStudents()
            if (entities.isNotEmpty()) dao.insertStudents(entities)
          }
          trySend(Unit)
        }

    listeners +=
        firestore.collection(COLLECTION_COURSES).addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.w(TAG, "Snapshot courses error", error)
            return@addSnapshotListener
          }
          val entities =
              snapshot
                  ?.documents
                  ?.mapNotNull { doc ->
                    val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
                    CourseEntity(
                        id = id,
                        title = doc.getString("title").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        instructor = doc.getString("instructor").orEmpty(),
                        totalStudents = (doc.get("totalStudents") as? Number)?.toInt() ?: 0,
                        durationHours = (doc.get("durationHours") as? Number)?.toInt() ?: 0,
                        completionRate = (doc.get("completionRate") as? Number)?.toFloat() ?: 0f,
                        isPublished = doc.getBoolean("isPublished") ?: false,
                    )
                  }
                  .orEmpty()
          serviceScope.launch {
            dao.clearCourses()
            if (entities.isNotEmpty()) dao.insertCourses(entities)
          }
          trySend(Unit)
        }

    listeners +=
        firestore.collection(COLLECTION_CLASSES).addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.w(TAG, "Snapshot classes error", error)
            return@addSnapshotListener
          }
          val entities =
              snapshot
                  ?.documents
                  ?.mapNotNull { doc ->
                    val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
                    val statusName =
                        doc.getString("status").orEmpty().ifBlank { ClassStatus.Open.name }
                    SchoolClassEntity(
                        id = id,
                        name = doc.getString("name").orEmpty(),
                        course = doc.getString("course").orEmpty(),
                        instructor = doc.getString("instructor").orEmpty(),
                        studentsCount = (doc.get("studentsCount") as? Number)?.toInt() ?: 0,
                        maxCapacity = (doc.get("maxCapacity") as? Number)?.toInt() ?: 0,
                        schedule = doc.getString("schedule").orEmpty(),
                        status =
                            runCatching { ClassStatus.valueOf(statusName) }
                                .getOrDefault(ClassStatus.Open),
                    )
                  }
                  .orEmpty()
          serviceScope.launch {
            dao.clearClasses()
            if (entities.isNotEmpty()) dao.insertClasses(entities)
          }
          trySend(Unit)
        }

    listeners +=
        firestore.collection(COLLECTION_CERTIFICATES).addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.w(TAG, "Snapshot certificates error", error)
            return@addSnapshotListener
          }
          val entities =
              snapshot
                  ?.documents
                  ?.mapNotNull { doc ->
                    val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
                    CertificateEntity(
                        id = id,
                        studentName = doc.getString("studentName").orEmpty(),
                        courseName = doc.getString("courseName").orEmpty(),
                        issuedDate = doc.getString("issuedDate").orEmpty(),
                        hours = (doc.get("hours") as? Number)?.toInt() ?: 0,
                        code = doc.getString("code").orEmpty(),
                    )
                  }
                  .orEmpty()
          serviceScope.launch {
            dao.clearCertificates()
            if (entities.isNotEmpty()) dao.insertCertificates(entities)
          }
          trySend(Unit)
        }

    listeners +=
        firestore.collection(COLLECTION_MONTHLY_ENROLLMENTS).addSnapshotListener { snapshot, error
          ->
          if (error != null) {
            Log.w(TAG, "Snapshot monthly error", error)
            return@addSnapshotListener
          }
          val entities =
              snapshot
                  ?.documents
                  ?.mapNotNull { doc ->
                    val month = doc.getString("month").orEmpty()
                    if (month.isBlank()) return@mapNotNull null
                    MonthlyEnrollmentEntity(
                        month = month,
                        count = (doc.get("count") as? Number)?.toInt() ?: 0,
                    )
                  }
                  .orEmpty()
          serviceScope.launch {
            dao.clearMonthlyEnrollments()
            if (entities.isNotEmpty()) dao.insertMonthlyEnrollments(entities)
          }
          trySend(Unit)
        }

    awaitClose { listeners.forEach { it.remove() } }
  }

  /**
   * Executa a rotina de upsert course dentro do contexto deste componente.
   *
   * @param course Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Result<Boolean>`.
   */
  suspend fun upsertCourse(course: Course): Result<Boolean> {
    return upsertCourse(requester = null, course = course)
  }

  /** Salva curso com validação explícita de autorização por perfil. */
  suspend fun upsertCourse(requester: AppUser?, course: Course): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Courses,
            action = ProtectedAction.Update,
            defaultMessage = "Acesso negado para alterar cursos.",
        )
    if (permissionError != null) return permissionError

    return try {
      val action = resolveMutationAction(COLLECTION_COURSES, course.id.toString())
      val payload =
          mapOf(
              "id" to course.id,
              "title" to course.title,
              "category" to course.category,
              "instructor" to course.instructor,
              "totalStudents" to course.totalStudents,
              "durationHours" to course.durationHours,
              "completionRate" to course.completionRate,
              "isPublished" to course.isPublished,
          )
      firestore.collection(COLLECTION_COURSES).document(course.id.toString()).set(payload).await()
      dao.insertCourses(listOf(course.toEntity()))
      registerRecentActivity(
          requester = requester,
          action = action,
          entityName = "Curso",
          targetLabel = course.title,
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao salvar curso: ${e.message}")
    }
  }

  /**
   * Executa a rotina de upsert class dentro do contexto deste componente.
   *
   * @param schoolClass Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Result<Boolean>`.
   */
  suspend fun upsertClass(schoolClass: SchoolClass): Result<Boolean> {
    return upsertClass(requester = null, schoolClass = schoolClass)
  }

  /** Salva turma com validação explícita de autorização por perfil. */
  suspend fun upsertClass(requester: AppUser?, schoolClass: SchoolClass): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Classes,
            action = ProtectedAction.Update,
            defaultMessage = "Acesso negado para alterar turmas.",
        )
    if (permissionError != null) return permissionError

    return try {
      val action = resolveMutationAction(COLLECTION_CLASSES, schoolClass.id.toString())
      val payload =
          mapOf(
              "id" to schoolClass.id,
              "name" to schoolClass.name,
              "course" to schoolClass.course,
              "instructor" to schoolClass.instructor,
              "studentsCount" to schoolClass.studentsCount,
              "maxCapacity" to schoolClass.maxCapacity,
              "schedule" to schoolClass.schedule,
              "status" to schoolClass.status.name,
          )
      firestore
          .collection(COLLECTION_CLASSES)
          .document(schoolClass.id.toString())
          .set(payload)
          .await()
      dao.insertClasses(listOf(schoolClass.toEntity()))
      registerRecentActivity(
          requester = requester,
          action = action,
          entityName = "Turma",
          targetLabel = schoolClass.name,
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao salvar turma: ${e.message}")
    }
  }

  /**
   * Executa a rotina de upsert teacher dentro do contexto deste componente.
   *
   * @param teacher Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Result<Boolean>`.
   */
  suspend fun upsertTeacher(teacher: Teacher): Result<Boolean> {
    return upsertTeacher(requester = null, teacher = teacher)
  }

  /** Salva instrutor com validação explícita de autorização por perfil. */
  suspend fun upsertTeacher(requester: AppUser?, teacher: Teacher): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Teachers,
            action = ProtectedAction.Update,
            defaultMessage = "Acesso negado para alterar instrutores.",
        )
    if (permissionError != null) return permissionError

    return try {
      val action = resolveMutationAction(COLLECTION_TEACHERS, teacher.id.toString())
      val payload =
          mapOf(
              "id" to teacher.id,
              "name" to teacher.name,
              "email" to teacher.email,
              "specialty" to teacher.specialty,
              "activeCourses" to teacher.activeCourses,
              "totalStudents" to teacher.totalStudents,
              "rating" to teacher.rating,
              "isActive" to teacher.isActive,
          )
      firestore.collection(COLLECTION_TEACHERS).document(teacher.id.toString()).set(payload).await()
      dao.insertTeachers(listOf(teacher.toEntity()))
      registerRecentActivity(
          requester = requester,
          action = action,
          entityName = "Instrutor",
          targetLabel = teacher.name,
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao salvar instrutor: ${e.message}")
    }
  }

  /**
   * Executa a rotina de upsert student dentro do contexto deste componente.
   *
   * @param student Valor de entrada utilizado por esta operação.
   * @return Resultado produzido pela operação em formato `Result<Boolean>`.
   */
  suspend fun upsertStudent(student: Student): Result<Boolean> {
    return upsertStudent(requester = null, student = student)
  }

  /** Salva aluno com validação explícita de autorização por perfil. */
  suspend fun upsertStudent(requester: AppUser?, student: Student): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Students,
            action = ProtectedAction.Update,
            defaultMessage = "Acesso negado para alterar alunos.",
        )
    if (permissionError != null) return permissionError

    return try {
      val action = resolveMutationAction(COLLECTION_STUDENTS, student.id.toString())
      val payload =
          mapOf(
              "id" to student.id,
              "name" to student.name,
              "email" to student.email,
              "course" to student.course,
              "enrolledClass" to student.enrolledClass,
              "progress" to student.progress,
              "status" to student.status.name,
          )
      firestore.collection(COLLECTION_STUDENTS).document(student.id.toString()).set(payload).await()
      dao.insertStudents(listOf(student.toEntity()))
      registerRecentActivity(
          requester = requester,
          action = action,
          entityName = "Aluno",
          targetLabel = student.name,
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao salvar aluno: ${e.message}")
    }
  }

  /** Registra evento de calendário para aulas, reuniões e eventos institucionais. */
  suspend fun upsertCalendarEvent(
      requester: AppUser?,
      event: CalendarEventEntity,
  ): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Calendar,
            action = ProtectedAction.Create,
            defaultMessage = "Acesso negado para cadastrar eventos no calendário.",
        )
    if (permissionError != null) return permissionError

    return try {
      val action = resolveMutationAction(COLLECTION_CALENDAR_EVENTS, event.id.toString())
      val payload =
          mapOf(
              "id" to event.id,
              "title" to event.title,
              "course" to event.course,
              "date" to event.date,
              "time" to event.time,
              "location" to event.location,
              "type" to event.type.name,
          )
      firestore
          .collection(COLLECTION_CALENDAR_EVENTS)
          .document(event.id.toString())
          .set(payload)
          .await()
      dao.insertCalendarEvents(listOf(event))
      registerRecentActivity(
          requester = requester,
          action = action,
          entityName = "Evento",
          targetLabel = event.title,
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao salvar evento no calendário: ${e.message}")
    }
  }

  /** Desativa aluno com regra de transição de ciclo de vida. */
  suspend fun deactivateStudent(requester: AppUser?, student: Student): Result<Boolean> {
    return upsertStudent(requester, EntityLifecycleRules.deactivateStudent(student))
  }

  /** Reativa aluno com regra de transição de ciclo de vida. */
  suspend fun reactivateStudent(requester: AppUser?, student: Student): Result<Boolean> {
    return upsertStudent(requester, EntityLifecycleRules.reactivateStudent(student))
  }

  /** Desativa curso com regra de transição de ciclo de vida. */
  suspend fun deactivateCourse(requester: AppUser?, course: Course): Result<Boolean> {
    return upsertCourse(requester, EntityLifecycleRules.deactivateCourse(course))
  }

  /** Reativa curso com regra de transição de ciclo de vida. */
  suspend fun reactivateCourse(requester: AppUser?, course: Course): Result<Boolean> {
    return upsertCourse(requester, EntityLifecycleRules.reactivateCourse(course))
  }

  /** Desativa turma com regra de transição de ciclo de vida. */
  suspend fun deactivateClass(requester: AppUser?, schoolClass: SchoolClass): Result<Boolean> {
    return upsertClass(requester, EntityLifecycleRules.deactivateClass(schoolClass))
  }

  /** Reativa turma com regra de transição de ciclo de vida. */
  suspend fun reactivateClass(requester: AppUser?, schoolClass: SchoolClass): Result<Boolean> {
    return upsertClass(requester, EntityLifecycleRules.reactivateClass(schoolClass))
  }

  /** Desativa instrutor com regra de transição de ciclo de vida. */
  suspend fun deactivateTeacher(requester: AppUser?, teacher: Teacher): Result<Boolean> {
    return upsertTeacher(requester, EntityLifecycleRules.deactivateTeacher(teacher))
  }

  /** Reativa instrutor com regra de transição de ciclo de vida. */
  suspend fun reactivateTeacher(requester: AppUser?, teacher: Teacher): Result<Boolean> {
    return upsertTeacher(requester, EntityLifecycleRules.reactivateTeacher(teacher))
  }

  /** Exclui aluno de forma definitiva. */
  suspend fun deleteStudent(requester: AppUser?, studentId: Int): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Students,
            action = ProtectedAction.Delete,
            defaultMessage = "Acesso negado para excluir alunos.",
        )
    if (permissionError != null) return permissionError

    return try {
      firestore.collection(COLLECTION_STUDENTS).document(studentId.toString()).delete().await()
      dao.deleteStudentById(studentId)
      registerRecentActivity(
          requester = requester,
          action = MutationAction.Deleted,
          entityName = "Aluno",
          targetLabel = "ID #$studentId",
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao excluir aluno: ${e.message}")
    }
  }

  /** Exclui curso de forma definitiva. */
  suspend fun deleteCourse(requester: AppUser?, courseId: Int): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Courses,
            action = ProtectedAction.Delete,
            defaultMessage = "Acesso negado para excluir cursos.",
        )
    if (permissionError != null) return permissionError

    return try {
      firestore.collection(COLLECTION_COURSES).document(courseId.toString()).delete().await()
      dao.deleteCourseById(courseId)
      registerRecentActivity(
          requester = requester,
          action = MutationAction.Deleted,
          entityName = "Curso",
          targetLabel = "ID #$courseId",
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao excluir curso: ${e.message}")
    }
  }

  /** Exclui turma de forma definitiva. */
  suspend fun deleteClass(requester: AppUser?, classId: Int): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Classes,
            action = ProtectedAction.Delete,
            defaultMessage = "Acesso negado para excluir turmas.",
        )
    if (permissionError != null) return permissionError

    return try {
      firestore.collection(COLLECTION_CLASSES).document(classId.toString()).delete().await()
      dao.deleteClassById(classId)
      registerRecentActivity(
          requester = requester,
          action = MutationAction.Deleted,
          entityName = "Turma",
          targetLabel = "ID #$classId",
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao excluir turma: ${e.message}")
    }
  }

  /** Exclui instrutor de forma definitiva. */
  suspend fun deleteTeacher(requester: AppUser?, teacherId: Int): Result<Boolean> {
    val permissionError =
        denyIfForbidden(
            requester = requester,
            resource = ProtectedResource.Teachers,
            action = ProtectedAction.Delete,
            defaultMessage = "Acesso negado para excluir instrutores.",
        )
    if (permissionError != null) return permissionError

    return try {
      firestore.collection(COLLECTION_TEACHERS).document(teacherId.toString()).delete().await()
      dao.deleteTeacherById(teacherId)
      registerRecentActivity(
          requester = requester,
          action = MutationAction.Deleted,
          entityName = "Instrutor",
          targetLabel = "ID #$teacherId",
      )
      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e, "Falha ao excluir instrutor: ${e.message}")
    }
  }

  /** Retorna erro padronizado quando a ação não é permitida para o perfil informado. */
  private fun denyIfForbidden(
      requester: AppUser?,
      resource: ProtectedResource,
      action: ProtectedAction,
      defaultMessage: String,
  ): Result.Error? {
    if (requester == null) return null
    return if (!AccessPolicy.can(requester.role, resource, action)) {
      Result.Error(SecurityException(defaultMessage), defaultMessage)
    } else {
      null
    }
  }

  /** Resolve se uma escrita corresponde a criação ou atualização de documento. */
  private suspend fun resolveMutationAction(
      collectionName: String,
      documentId: String,
  ): MutationAction {
    val exists = firestore.collection(collectionName).document(documentId).get().await().exists()
    return if (exists) MutationAction.Updated else MutationAction.Created
  }

  /** Registra atividade recente no Firestore e no cache local para consumo em tempo real. */
  private suspend fun registerRecentActivity(
      requester: AppUser?,
      action: MutationAction,
      entityName: String,
      targetLabel: String,
  ) {
    if (!RealtimeNotificationRules.canReceiveBackofficeNotifications(requester?.role)) return

    val draft = RealtimeNotificationRules.buildDraft(action, entityName, targetLabel)
    val nextId = (dao.getMaxRecentActivityId() ?: 0) + 1
    val entity =
        RecentActivityEntity(
            id = nextId,
            title = draft.title,
            subtitle = draft.subtitle,
            iconKey = draft.iconKey,
            timeLabel = draft.timeLabel,
        )

    val payload =
        mapOf(
            "id" to entity.id,
            "title" to entity.title,
            "subtitle" to entity.subtitle,
            "iconKey" to entity.iconKey,
            "timeLabel" to entity.timeLabel,
        )

    firestore
        .collection(COLLECTION_RECENT_ACTIVITIES)
        .document(entity.id.toString())
        .set(payload, SetOptions.merge())
        .await()
    dao.insertRecentActivities(listOf(entity))
  }

  // Conversões de modelo para entidade (para cache local)
  /** Executa a rotina de course dentro do contexto deste componente. */
  private fun Course.toEntity() =
      CourseEntity(
          id = id,
          title = title,
          category = category,
          instructor = instructor,
          totalStudents = totalStudents,
          durationHours = durationHours,
          completionRate = completionRate,
          isPublished = isPublished,
      )

  /** Executa a rotina de student dentro do contexto deste componente. */
  private fun Student.toEntity() =
      StudentEntity(
          id = id,
          name = name,
          email = email,
          course = course,
          enrolledClass = enrolledClass,
          progress = progress,
          status = status,
      )

  /** Executa a rotina de teacher dentro do contexto deste componente. */
  private fun Teacher.toEntity() =
      tech.datatower.sebrae.desafio.data.local.TeacherEntity(
          id = id,
          name = name,
          email = email,
          specialty = specialty,
          activeCourses = activeCourses,
          totalStudents = totalStudents,
          rating = rating,
          isActive = isActive,
      )

  /** Executa a rotina de school class dentro do contexto deste componente. */
  private fun SchoolClass.toEntity() =
      SchoolClassEntity(
          id = id,
          name = name,
          course = course,
          instructor = instructor,
          studentsCount = studentsCount,
          maxCapacity = maxCapacity,
          schedule = schedule,
          status = status,
      )

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

  /**
   * Cria uma conta no Firebase Authentication sem afetar a sessão do admin atual.
   *
   * Usa uma instância secundária do FirebaseApp para que o `signIn` implícito do
   * `createUserWithEmailAndPassword` não faça logout do administrador logado.
   *
   * @return UID do novo usuário Firebase, ou `null` se a conta já existir ou falhar.
   */
  private suspend fun createFirebaseAuthUser(email: String, password: String): String? {
    val tempAppName = "temp_create_user_${System.currentTimeMillis()}"
    return try {
      val mainOptions = FirebaseApp.getInstance().options
      val tempApp = FirebaseApp.initializeApp(context, mainOptions, tempAppName)
      try {
        val tempAuth = FirebaseAuth.getInstance(tempApp)
        val result = tempAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid
        tempAuth.signOut()
        Log.d(TAG, "Conta Firebase Auth criada para $email (uid=$uid)")
        uid
      } finally {
        tempApp.delete()
      }
    } catch (e: FirebaseAuthUserCollisionException) {
      // Conta já existe — login funcionará normalmente
      Log.d(TAG, "Conta Firebase Auth ja existe para $email")
      null
    } catch (e: Exception) {
      Log.w(TAG, "Nao foi possivel criar conta Firebase Auth para $email: ${e.message}")
      null
    }
  }
}
