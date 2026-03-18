package tech.datatower.sebrae.desafio.data.auth

import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole

/**
 * Gerenciador de autenticação em memória.
 *
 * Mantém a sessão do usuário durante a execução da aplicação e valida credenciais contra a lista
 * de usuários pré-cadastrados. As senhas são comparadas via hash SHA-256 para evitar armazenamento
 * em texto simples. A sessão é encerrada quando o aplicativo é fechado.
 */
object AuthManager {

  /** Usuários pré-cadastrados com seus respectivos níveis de permissão. */
  private val preDefinedUsers =
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

  /**
   * Mapeamento de e-mail para hash SHA-256 da senha dos usuários pré-cadastrados.
   *
   * Senhas: professor→prof123, coordenador→coord123, admin→admin123.
   */
  private val hashedCredentials =
      mapOf(
          "professor@sebrae.edu.br" to sha256("prof123"),
          "coordenador@sebrae.edu.br" to sha256("coord123"),
          "admin@sebrae.edu.br" to sha256("admin123"),
      )

  private val _currentUser = MutableStateFlow<AppUser?>(null)

  /** Fluxo reativo com o usuário atualmente autenticado, ou `null` se não houver sessão ativa. */
  val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

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
      _currentUser.value = preDefinedUsers.first { it.email == normalizedEmail }
      true
    } else {
      false
    }
  }

  /** Encerra a sessão atual, retornando o estado para não autenticado. */
  fun logout() {
    _currentUser.value = null
  }

  /** Retorna o hash SHA-256 hexadecimal da string fornecida. */
  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
  }
}
