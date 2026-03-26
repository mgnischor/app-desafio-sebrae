package tech.datatower.sebrae.desafio.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import java.security.MessageDigest

/** Gerenciador de autenticação em memória. */
object AuthManager {

  private const val TAG = "AuthManager"
  private const val USERS_COLLECTION = "users"

  private val _users =
      MutableStateFlow(
          listOf(
              AppUser(
                  id = 1,
                  name = "Prof. Carlos Silva",
                  email = "professor@sebrae.edu.br",
                  role = UserRole.PROFESSOR,
              ),
              AppUser(
                  id = 2,
                  name = "Coord. Ana Santos",
                  email = "coordenador@sebrae.edu.br",
                  role = UserRole.COORDENADOR,
              ),
              AppUser(
                  id = 3,
                  name = "Admin. João Almeida",
                  email = "admin@sebrae.edu.br",
                  role = UserRole.ADMINISTRADOR,
              ),
          )
      )

  private val hashedCredentials =
      mutableMapOf(
          "professor@sebrae.edu.br" to sha256("prof123"),
          "coordenador@sebrae.edu.br" to sha256("coord123"),
          "admin@sebrae.edu.br" to sha256("admin123"),
      )

  private val _currentUser = MutableStateFlow<AppUser?>(null)

  private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

  /** Modelo e comportamento relacionados a firebase login result. */
  sealed class FirebaseLoginResult {
    /** Modelo e comportamento relacionados a success. */
    data class Success(val user: AppUser) : FirebaseLoginResult()

    /** Modelo e comportamento relacionados a error. */
    data class Error(val message: String) : FirebaseLoginResult()
  }

  /** Fluxo reativo com o usuário atualmente autenticado, ou `null` se não houver sessão ativa. */
  val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

  /** Login oficial do app via Firebase Auth e perfil em Firestore (collection `users`). */
  suspend fun loginWithFirebase(email: String, password: String): FirebaseLoginResult {
    val normalizedEmail = email.trim().lowercase()
    if (normalizedEmail.isBlank() || password.isBlank()) {
      return FirebaseLoginResult.Error("Informe e-mail e senha.")
    }

    return try {
      firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password).await()
      val authUser = firebaseAuth.currentUser
      if (authUser == null) {
        return FirebaseLoginResult.Error("Nao foi possivel iniciar sessao no Firebase.")
      }

      val profile = fetchUserProfile(authUser.uid, normalizedEmail)
      if (profile == null) {
        firebaseAuth.signOut()
        _currentUser.value = null
        FirebaseLoginResult.Error(
            "Usuario autenticado, mas sem perfil na collection users. " +
                "Cadastre o documento do usuario no Firestore."
        )
      } else {
        upsertLocalUser(profile)
        _currentUser.value = profile
        FirebaseLoginResult.Success(profile)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Falha no login Firebase", e)
      FirebaseLoginResult.Error(
          "Falha no login: ${e.message.orEmpty().ifBlank { "credenciais invalidas" }}"
      )
    }
  }

  /**
   * Tenta autenticar o usuário com as credenciais fornecidas.
   *
   * A senha informada é convertida para hash SHA-256 antes da comparação.
   *
   * @param email Endereço de e-mail informado na tela de login.
   * @param password Senha informada na tela de login.
   * @return `true` se as credenciais forem válidas e a sessão for iniciada, `false` caso contrário.
   */
  fun login(email: String, password: String): Boolean {
    val normalizedEmail = email.trim().lowercase()
    val expectedHash = hashedCredentials[normalizedEmail]
    return if (expectedHash != null && expectedHash == sha256(password)) {
      _currentUser.value = _users.value.firstOrNull { it.email == normalizedEmail }
      _currentUser.value != null
    } else {
      false
    }
  }

  /**
   * Busca dados de user profile na origem configurada.
   *
   * @param firebaseUid Valor de entrada utilizado por esta opera??o.
   * @param fallbackEmail Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `AppUser?`.
   */
  private suspend fun fetchUserProfile(firebaseUid: String, fallbackEmail: String): AppUser? {
    val byDocumentId = firestore.collection(USERS_COLLECTION).document(firebaseUid).get().await()
    if (byDocumentId.exists()) return byDocumentId.toAppUser(fallbackEmail)

    val byUid =
        firestore
            .collection(USERS_COLLECTION)
            .whereEqualTo("authUid", firebaseUid)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
    if (byUid != null) return byUid.toAppUser(fallbackEmail)

    val byEmail =
        firestore
            .collection(USERS_COLLECTION)
            .whereEqualTo("email", fallbackEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
    return byEmail?.toAppUser(fallbackEmail)
  }

  /**
   * Executa a rotina de com dentro do contexto deste componente.
   *
   * @param fallbackEmail Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `AppUser`.
   */
  private fun com.google.firebase.firestore.DocumentSnapshot.toAppUser(
      fallbackEmail: String
  ): AppUser {
    val roleRaw = getString("role").orEmpty().uppercase()
    val role = runCatching { UserRole.valueOf(roleRaw) }.getOrDefault(UserRole.PROFESSOR)
    val resolvedId =
        (get("id") as? Number)?.toInt()
            ?: (get("legacyId") as? Number)?.toInt()
            ?: id.hashCode().let { if (it == Int.MIN_VALUE) 1 else kotlin.math.abs(it) }

    return AppUser(
        id = resolvedId,
        name = getString("name").orEmpty().ifBlank { "Usuario" },
        email = getString("email").orEmpty().ifBlank { fallbackEmail },
        role = role,
    )
  }

  /**
   * Executa a rotina de upsert local user dentro do contexto deste componente.
   *
   * @param user Valor de entrada utilizado por esta opera??o.
   */
  private fun upsertLocalUser(user: AppUser) {
    val mutable = _users.value.toMutableList()
    val index = mutable.indexOfFirst { it.id == user.id || it.email == user.email }
    if (index >= 0) {
      mutable[index] = user
    } else {
      mutable.add(user)
    }
    _users.value = mutable.sortedBy { it.name }
  }

  /**
   * Registra ou atualiza um usuário no sistema.
   *
   * @param user O usuário a ser registrado ou atualizado.
   * @param plainPassword A senha em texto plano do usuário, necessária apenas para registro.
   */
  fun registerOrUpdateUser(user: AppUser, plainPassword: String) {
    val normalizedEmail = user.email.trim().lowercase()
    val normalizedUser = user.copy(email = normalizedEmail)

    val mutable = _users.value.toMutableList()
    val index = mutable.indexOfFirst { it.id == normalizedUser.id || it.email == normalizedEmail }
    if (index >= 0) {
      mutable[index] = normalizedUser
    } else {
      mutable.add(normalizedUser)
    }
    _users.value = mutable.sortedBy { it.name }
    hashedCredentials[normalizedEmail] = sha256(plainPassword)
  }

  /**
   * Remove um usuário do sistema pelo seu ID.
   *
   * @param userId O ID do usuário a ser removido.
   */
  fun removeUserById(userId: Int) {
    val target = _users.value.firstOrNull { it.id == userId } ?: return
    _users.value = _users.value.filterNot { it.id == userId }
    hashedCredentials.remove(target.email)

    if (_currentUser.value?.id == userId) {
      logout()
    }
  }

  /**
   * Altera a senha do usuário autenticado após validar a senha atual.
   *
   * @param currentPassword Senha atual informada pelo usuário logado.
   * @param newPassword Nova senha desejada.
   * @return `true` quando a senha é atualizada com sucesso.
   */
  fun changeCurrentUserPassword(currentPassword: String, newPassword: String): Boolean {
    val user = _currentUser.value ?: return false
    if (newPassword.length < 8) return false

    val expectedHash = hashedCredentials[user.email] ?: return false
    if (expectedHash != sha256(currentPassword)) return false

    hashedCredentials[user.email] = sha256(newPassword)
    return true
  }

  /** Encerra a sessão atual, retornando o estado para não autenticado. */
  fun logout() {
    runCatching { firebaseAuth.signOut() }
    _currentUser.value = null
  }

  /** Retorna o hash SHA-256 hexadecimal da string fornecida. */
  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
  }
}
