/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/login/LoginViewModel.kt
    Descrição: ViewModel da tela de login, gerenciando o estado de autenticação e erros.
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
package tech.datatower.sebrae.desafio.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import javax.inject.Inject

/**
 * ViewModel da tela de login.
 *
 * Coordena o fluxo de autenticação via [AuthManager.loginWithFirebase] e expõe o estado resultante
 * como [StateFlow] para que a tela reaja adequadamente a cada situação.
 */
@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

  /**
   * Representa o estado atual do fluxo de autenticação.
   *
   * Use `when` de forma exaustiva para tratar cada situação na UI.
   */
  sealed class LoginState {
    /** Estado inicial; nenhuma tentativa de login foi feita ainda. */
    data object Idle : LoginState()

    /** Aguardando resposta da operação de login em andamento. */
    data object Loading : LoginState()

    /** Login falhou; contém a mensagem de erro para exibição ao usuário. */
    data class Error(val message: String) : LoginState()

    /** Login concluído com sucesso; a navegação para a tela inicial deve ser acionada. */
    data object Success : LoginState()
  }

  private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
  val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

  /**
   * Inicia a tentativa de login via Firebase Auth e atualiza [loginState] conforme o resultado.
   *
   * @param email Endereço de e-mail informado pelo usuário.
   * @param password Senha informada pelo usuário.
   */
  fun login(email: String, password: String) {
    viewModelScope.launch {
      _loginState.value = LoginState.Loading
      _loginState.value =
          when (val result = AuthManager.loginWithFirebase(email, password)) {
            is AuthManager.FirebaseLoginResult.Success -> LoginState.Success
            is AuthManager.FirebaseLoginResult.Error -> LoginState.Error(result.message)
          }
    }
  }

  /** Redefine [loginState] para [LoginState.Idle], permitindo nova tentativa de login. */
  fun resetState() {
    _loginState.value = LoginState.Idle
  }
}
