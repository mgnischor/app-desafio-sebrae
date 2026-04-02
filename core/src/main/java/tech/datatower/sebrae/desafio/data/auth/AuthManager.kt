/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/auth/AuthManager.kt
    Descrição: Gerenciador de autenticação do usuário, controlando login, logout e sessão ativa.
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
package tech.datatower.sebrae.desafio.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Company
import tech.datatower.sebrae.desafio.data.model.UserRole
import java.security.MessageDigest

/**
 * Gerencia autenticação e sessão de usuário para toda a aplicação.
 *
 * Suporta dois fluxos de login:
 * - **Firebase Auth** (principal): autentica via Firebase e busca o perfil na collection `users`
 *   do Firestore.
 * - **Local (fallback)**: valida credenciais em memória usando hashes SHA-256, útil para
 *   desenvolvimento e testes offline.
 *
 * O estado da sessão é exposto como [StateFlow] para que a UI possa reagir reativamente a
 * alterações de usuário e empresa ativa.
 */
object AuthManager {

  private const val TAG = "AuthManager"
  private const val USERS_COLLECTION = "users"
  private const val COMPANIES_COLLECTION = "companies"
  private const val USER_COMPANIES_COLLECTION = "user_companies"

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
              AppUser(
                  id = 4,
                  name = "Orientadora Beatriz Lima",
                  email = "orientador@sebrae.edu.br",
                  role = UserRole.ORIENTADOR_EDUCACIONAL,
              ),
              AppUser(
                  id = 5,
                  name = "Psicopedagoga Carla Mendes",
                  email = "psicopedagogo@sebrae.edu.br",
                  role = UserRole.PSICOPEDAGOGO,
              ),
              AppUser(
                  id = 6,
                  name = "Maria da Silva (Responsável)",
                  email = "responsavel@sebrae.edu.br",
                  role = UserRole.RESPONSAVEL,
              ),
          )
      )

  private val hashedCredentials =
      mutableMapOf(
          "professor@sebrae.edu.br" to sha256("prof123"),
          "coordenador@sebrae.edu.br" to sha256("coord123"),
          "admin@sebrae.edu.br" to sha256("admin123"),
          "orientador@sebrae.edu.br" to sha256("orient123"),
          "psicopedagogo@sebrae.edu.br" to sha256("psico123"),
          "responsavel@sebrae.edu.br" to sha256("resp123"),
      )

  private val _currentUser = MutableStateFlow<AppUser?>(null)
  private val _currentCompany = MutableStateFlow<Company?>(null)
  private val _userCompanies = MutableStateFlow<List<Company>>(emptyList())

  private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

  /**
   * Representa o resultado de uma tentativa de login via Firebase Auth.
   *
   * Use `when` para tratamento exaustivo dos casos de sucesso e falha.
   */
  sealed class FirebaseLoginResult {
    /** Login realizado com sucesso; contém o [AppUser] autenticado e com perfil carregado. */
    data class Success(val user: AppUser) : FirebaseLoginResult()

    /** Login falhou; contém a mensagem de erro localizada para exibição ao usuário. */
    data class Error(val message: String) : FirebaseLoginResult()
  }

  /** Fluxo reativo com o usuário atualmente autenticado, ou `null` se não houver sessão ativa. */
  val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

  /** Fluxo reativo com todos os usuários conhecidos pelo sistema. */
  val users: StateFlow<List<AppUser>> = _users.asStateFlow()

  /** Fluxo reativo com a empresa atualmente selecionada pelo usuário. */
  val currentCompany: StateFlow<Company?> = _currentCompany.asStateFlow()

  /** Fluxo reativo com as empresas às quais o usuário autenticado tem acesso. */
  val userCompanies: StateFlow<List<Company>> = _userCompanies.asStateFlow()

  /** Define as empresas que o usuário tem acesso e seleciona automaticamente a primeira quando nenhuma empresa está ativa. */
  fun setUserCompanies(companies: List<Company>) {
    _userCompanies.value = companies
    if (_currentCompany.value == null && companies.isNotEmpty()) {
      _currentCompany.value = companies.first()
    }
    if (_currentCompany.value != null && companies.none { it.id == _currentCompany.value?.id }) {
      _currentCompany.value = companies.firstOrNull()
    }
  }

  /**
   * Troca a empresa ativa do usuário autenticado.
   *
   * A troca só é permitida para empresas que estejam na lista [userCompanies] do usuário.
   *
   * @param company Empresa a ser ativada.
   */
  fun switchCompany(company: Company) {
    if (_userCompanies.value.any { it.id == company.id }) {
      _currentCompany.value = company
    }
  }

  /**
   * Autentica o usuário via Firebase Auth e carrega seu perfil da collection `users` no Firestore.
   *
   * O fluxo é:
   * 1. Valida e normaliza as credenciais.
   * 2. Autentica via `FirebaseAuth.signInWithEmailAndPassword`.
   * 3. Busca o documento de perfil no Firestore (por UID, por campo `authUid` ou por `email`).
   * 4. Atualiza a lista local de usuários e carrega as empresas vinculadas.
   *
   * @param email Endereço de e-mail informado na tela de login.
   * @param password Senha informada na tela de login (transmitida de forma segura ao Firebase).
   * @return [FirebaseLoginResult.Success] com o usuário autenticado, ou [FirebaseLoginResult.Error]
   *   com a mensagem de falha.
   */
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
        loadUserCompaniesFromFirestore(profile)
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
   * Busca o perfil de usuário no Firestore tentando três estratégias em cascata:
   * 1. Documento cujo ID é o próprio UID do Firebase Auth.
   * 2. Documento com o campo `authUid` igual ao UID do Firebase Auth.
   * 3. Documento com o campo `email` igual ao e-mail informado no login.
   *
   * @param firebaseUid UID do usuário autenticado no Firebase Auth.
   * @param fallbackEmail E-mail normalizado usado na terceira estratégia de busca.
   * @return [AppUser] se encontrado, ou `null` se nenhum documento corresponder.
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
   * Converte um [DocumentSnapshot] do Firestore para o modelo de domínio [AppUser].
   *
   * Resolve o `id` preferindo o campo numérico `id`, em seguida `legacyId`, e por último um hash
   * do ID do documento como fallback.
   *
   * @param fallbackEmail E-mail usado quando o campo `email` estiver ausente no documento.
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
   * Insere ou atualiza um [AppUser] na lista reativa local de usuários.
   *
   * Combina usuários por `id` ou `email` para evitar duplicatas após sincronização com o
   * Firestore.
   *
   * @param user Usuário a ser inserido ou atualizado.
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

  /**
   * Carrega as empresas do usuário a partir do Firestore após login bem-sucedido. Administradores
   * recebem todas as empresas ativas; demais papéis recebem apenas as vinculadas.
   */
  private suspend fun loadUserCompaniesFromFirestore(user: AppUser) {
    try {
      val allCompanies = firestore.collection(COMPANIES_COLLECTION).get().await()
      val companyMap =
          allCompanies.documents
              .mapNotNull { doc ->
                val id = (doc.get("id") as? Number)?.toInt() ?: return@mapNotNull null
                val name = doc.getString("name").orEmpty()
                val cnpj = doc.getString("cnpj").orEmpty()
                val isActive = doc.getBoolean("isActive") ?: true
                if (isActive) Company(id = id, name = name, cnpj = cnpj, isActive = true) else null
              }
              .associateBy { it.id }

      val companies =
          if (user.role == UserRole.ADMINISTRADOR) {
            companyMap.values.toList()
          } else {
            val userCompanyDocs =
                firestore
                    .collection(USER_COMPANIES_COLLECTION)
                    .whereEqualTo("userId", user.id)
                    .get()
                    .await()
            userCompanyDocs.documents.mapNotNull { doc ->
              val companyId = (doc.get("companyId") as? Number)?.toInt() ?: return@mapNotNull null
              companyMap[companyId]
            }
          }
      setUserCompanies(companies.sortedBy { it.name })
    } catch (e: Exception) {
      Log.w(TAG, "Falha ao carregar empresas do usuario", e)
    }
  }

  /** Encerra a sessão atual, retornando o estado para não autenticado. */
  fun logout() {
    runCatching { firebaseAuth.signOut() }
    _currentUser.value = null
    _currentCompany.value = null
    _userCompanies.value = emptyList()
  }

  /** Retorna o hash SHA-256 hexadecimal da string fornecida. */
  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
  }
}
