package tech.datatower.sebrae.desafio.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.datatower.sebrae.desafio.data.model.UserRole

/**
 * Testes unitários do gerenciador de autenticação.
 *
 * Valida lógica de login, logout, hash de senha e persistência de sessão.
 *
 * Padrão: Arrange → Act → Assert (AAA)
 */
class AuthManagerTest {
  /** Atualiza o estado de up com os valores informados. */
  @Before
  fun setUp() {
    // Limpar estado do AuthManager antes de cada teste
    AuthManager.logout()
  }

  // ── Testes de Login ───────────────────────────────────────────────────

  /**
   * Valida login bem-sucedido com credenciais válidas de professor.
   *
   * Regra: Credenciais pré-cadastradas devem autenticar com sucesso.
   */
  @Test
  fun `login com credenciais válidas deve retornar true`() {
    // Arrange
    val email = "professor@sebrae.edu.br"
    val senha = "prof123"

    // Act
    val resultado = AuthManager.login(email, senha)

    // Assert
    assertTrue("Login deve ser bem-sucedido com credenciais corretas", resultado)
  }

  /** Valida falha de login com email inválido. */
  @Test
  fun `login com email inválido deve retornar false`() {
    // Arrange
    val email = "invalido@exemplo.com"
    val senha = "prof123"

    // Act
    val resultado = AuthManager.login(email, senha)

    // Assert
    assertFalse("Login deve falhar com email não cadastrado", resultado)
  }

  /** Valida falha de login com senha incorreta. */
  @Test
  fun `login com senha incorreta deve retornar false`() {
    // Arrange
    val email = "professor@sebrae.edu.br"
    val senhaErrada = "senhaerrada123"

    // Act
    val resultado = AuthManager.login(email, senhaErrada)

    // Assert
    assertFalse("Login deve falhar com senha incorreta", resultado)
  }

  /** Valida normalização de email (minúsculas) no login. */
  @Test
  fun `login deve aceitar emails com maiúsculas`() {
    // Arrange
    val email = "PROFESSOR@SEBRAE.EDU.BR"
    val senha = "prof123"

    // Act
    val resultado = AuthManager.login(email, senha)

    // Assert
    assertTrue("Login deve aceitar emails em maiúsculas", resultado)
  }

  /** Valida login de coordenador com credenciais específicas. */
  @Test
  fun `login com credenciais de coordenador deve ser bem-sucedido`() {
    // Arrange
    val email = "coordenador@sebrae.edu.br"
    val senha = "coord123"

    // Act
    val resultado = AuthManager.login(email, senha)

    // Assert
    assertTrue("Login de coordenador deve funcionar", resultado)
  }

  /** Valida login de administrador com credenciais específicas. */
  @Test
  fun `login com credenciais de administrador deve ser bem-sucedido`() {
    // Arrange
    val email = "admin@sebrae.edu.br"
    val senha = "admin123"

    // Act
    val resultado = AuthManager.login(email, senha)

    // Assert
    assertTrue("Login de administrador deve funcionar", resultado)
  }

  // ── Testes de Sessão ──────────────────────────────────────────────────

  /** Valida que o usuário é definido após login bem-sucedido. */
  @Test
  fun `currentUser deve conter dados do usuário após login bem-sucedido`() {
    // Arrange
    val email = "professor@sebrae.edu.br"
    val senha = "prof123"

    // Act
    AuthManager.login(email, senha)
    val usuarioAtual = AuthManager.currentUser.value

    // Assert
    assertNotNull("Usuário não deve ser nulo após login", usuarioAtual)
    assertTrue("Email do usuário deve corresponder", usuarioAtual?.email == email)
    assertTrue("Role deve ser PROFESSOR", usuarioAtual?.role == UserRole.PROFESSOR)
  }

  /** Valida que currentUser permanece nulo sem login. */
  @Test
  fun `currentUser deve ser nulo quando não autenticado`() {
    // Arrange & Act
    val usuarioAtual = AuthManager.currentUser.value

    // Assert
    assertNull("Usuário deve ser nulo quando não autenticado", usuarioAtual)
  }

  /** Valida logout bem-sucedido. */
  @Test
  fun `logout deve limpar sessão do usuário`() {
    // Arrange: Login primeiro
    AuthManager.login("professor@sebrae.edu.br", "prof123")
    var usuarioAtual = AuthManager.currentUser.value
    assertNotNull("Usuário deve estar autenticado antes de logout", usuarioAtual)

    // Act
    AuthManager.logout()
    usuarioAtual = AuthManager.currentUser.value

    // Assert
    assertNull("Usuário deve ser nulo após logout", usuarioAtual)
  }

  /** Valida troca de usuário após logout e novo login. */
  @Test
  fun `login após logout deve funcionar com novo usuário`() {
    // Arrange: Login com professor
    AuthManager.login("professor@sebrae.edu.br", "prof123")
    var usuarioAtual = AuthManager.currentUser.value
    assertTrue(
        "Primeiro login deve ser bem-sucedido",
        usuarioAtual?.email == "professor@sebrae.edu.br",
    )

    // Act: Logout e login com coordenador
    AuthManager.logout()
    val resultadoNovoLogin = AuthManager.login("coordenador@sebrae.edu.br", "coord123")
    usuarioAtual = AuthManager.currentUser.value

    // Assert
    assertTrue("Novo login deve ser bem-sucedido", resultadoNovoLogin)
    assertTrue("Usuário deve mudar para coordenador", usuarioAtual?.role == UserRole.COORDENADOR)
  }

  // ── Testes de Segurança ───────────────────────────────────────────────

  /** Valida que espaços em branco no email são trimados. */
  @Test
  fun `login deve remover espaços em branco do email`() {
    // Arrange
    val email = "  professor@sebrae.edu.br  "
    val senha = "prof123"

    // Act
    val resultado = AuthManager.login(email, senha)

    // Assert
    assertTrue("Login deve funcionar com email contendo espaços", resultado)
  }

  /** Valida que tentativas repetidas de login com senha errada mantêm usuário desautenticado. */
  @Test
  fun `múltiplas tentativas de login com senha errada não devem autenticar`() {
    // Arrange
    val email = "professor@sebrae.edu.br"
    val senhaErrada = "senhaerrada"

    // Act & Assert
    repeat(3) {
      val resultado = AuthManager.login(email, senhaErrada)
      assertFalse("Tentativa $it com senha errada deve falhar", resultado)
      assertNull("Usuário deve permanecer nulo", AuthManager.currentUser.value)
    }
  }
}
