package tech.datatower.sebrae.desafio.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.security.MessageDigest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import tech.datatower.sebrae.desafio.BuildConfig
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole

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
) {

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
    data class Success<T>(val data: T) : Result<T>()

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
      val seedEmailFromBuild = readBuildConfigString("FIREBASE_SEED_EMAIL")
      val seedPasswordFromBuild = readBuildConfigString("FIREBASE_SEED_PASSWORD")
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
   * Valida pre-condicoes de auth para operacoes administrativas da collection users. Regras
   * esperadas no Firestore: request.auth.token.admin == true.
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
      if (isAdminClaim) {
        null
      } else {
        Result.Error(
            SecurityException("Custom claim admin=true ausente no token Firebase."),
            "Usuario autenticado sem permissao admin no Firebase. Defina custom claim admin=true e entre novamente.",
        )
      }
    } catch (e: Exception) {
      Result.Error(
          e,
          "Falha ao validar token Firebase para permissao admin: ${e.message}",
      )
    }
  }

  /**
   * Le string de BuildConfig por reflexao para evitar falha de compilacao antes do sync do Gradle.
   */
  private fun readBuildConfigString(fieldName: String): String {
    return runCatching {
          val field = BuildConfig::class.java.getField(fieldName)
          (field.get(null) as? String).orEmpty()
        }
        .getOrDefault("")
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
   * Cadastra/atualiza usuario na collection `users`. Apenas usuarios administradores podem
   * executar.
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

    return try {
      val normalizedEmail = target.email.trim().lowercase()
      val payload =
          mapOf(
              "id" to target.id,
              "name" to target.name,
              "email" to normalizedEmail,
              "role" to target.role.name,
              "passwordHash" to sha256(plainPassword),
          )
      firestore
          .collection(COLLECTION_USERS)
          .document(target.id.toString())
          .set(payload, SetOptions.merge())
          .await()
      Result.Success(true)
    } catch (e: Exception) {
      val hint =
          if (isPermissionDenied(e, e.message.orEmpty())) {
            "Verifique se o usuario Firebase autenticado possui custom claim admin=true e se o token foi atualizado (getIdToken(true) ou novo login)."
          } else {
            ""
          }
      Result.Error(e, "Falha ao cadastrar usuario no Firebase: ${e.message}. $hint")
    }
  }

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

    return try {
      firestore.collection(COLLECTION_USERS).document(userId.toString()).delete().await()
      Result.Success(true)
    } catch (e: Exception) {
      val hint =
          if (isPermissionDenied(e, e.message.orEmpty())) {
            "Verifique se o usuario Firebase autenticado possui custom claim admin=true e se o token foi atualizado (getIdToken(true) ou novo login)."
          } else {
            ""
          }
      Result.Error(e, "Falha ao remover usuario no Firebase: ${e.message}. $hint")
    }
  }

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
                  status = tech.datatower.sebrae.desafio.data.model.StudentStatus.Active,
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

  /**
   * Sincroniza todos os dados do Data Connect com cache local.
   *
   * Executa em paralelo para melhor performance.
   */
  suspend fun syncAllData(): Result<Boolean> {
    return try {
      Log.d(TAG, "Iniciando sincronização completa de dados...")

      val coursesResult = fetchCourses()
      val studentsResult = fetchStudents()
      val teachersResult = fetchTeachers()

      val allSuccess =
          coursesResult is Result.Success &&
              studentsResult is Result.Success &&
              teachersResult is Result.Success

      if (allSuccess) {
        Log.d(TAG, "Sincronização completa concluída com sucesso")
        Result.Success(true)
      } else {
        val errors = buildString {
          if (coursesResult is Result.Error) append("Cursos: ${coursesResult.message}. ")
          if (studentsResult is Result.Error) append("Estudantes: ${studentsResult.message}. ")
          if (teachersResult is Result.Error) append("Professores: ${teachersResult.message}. ")
        }
        Result.Error(Exception("Falha parcial"), errors)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Erro durante sincronização completa", e)
      Result.Error(e, "Falha na sincronização: ${e.message}")
    }
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

  // Conversões de modelo para entidade (para cache local)
  private fun Course.toEntity() =
      tech.datatower.sebrae.desafio.data.local.CourseEntity(
          id = id,
          title = title,
          category = category,
          instructor = instructor,
          totalStudents = totalStudents,
          durationHours = durationHours,
          completionRate = completionRate,
          isPublished = isPublished,
      )

  private fun Student.toEntity() =
      tech.datatower.sebrae.desafio.data.local.StudentEntity(
          id = id,
          name = name,
          email = email,
          course = course,
          enrolledClass = enrolledClass,
          progress = progress,
          status = status,
      )

  private fun Teacher.toEntity() =
      tech.datatower.sebrae.desafio.data.local.TeacherEntity(
          id = id,
          name = name,
          email = email,
          specialty = specialty,
          activeCourses = activeCourses,
          totalStudents = totalStudents,
          rating = rating,
      )

  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
  }
}
