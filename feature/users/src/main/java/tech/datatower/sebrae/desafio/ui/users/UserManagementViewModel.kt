/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/users/UserManagementViewModel.kt
    Descrição: ViewModel da tela de gerenciamento de usuários, coordenando operações CRUD.
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
package tech.datatower.sebrae.desafio.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

/**
 * ViewModel da tela de gestão de usuários.
 *
 * Observa a lista de usuários em tempo real via Firestore Realtime Listener e fornece
 * operações de criação e exclusão com remoção otimista.
 * Todas as operações requerem um requester com perfil de ADMINISTRADOR.
 */
@HiltViewModel
class UserManagementViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  /**
   * Estado da lista de usuários gerenciados, derivado do listener em tempo real.
   */
  sealed class UsersState {
    /** Lista ainda está sendo carregada. */
    data object Loading : UsersState()

    /** Lista de usuários disponível com sucesso.
     * @property users Usuários gerenciados visíveis na UI (já com exclusões otimistas aplicadas).
     */
    data class Success(val users: List<FirebaseDataConnectService.ManagedUser>) : UsersState()

    /** Falha ao carregar ou no listener em tempo real.
     * @property message Mensagem de erro para exibir na UI.
     */
    data class Error(val message: String) : UsersState()
  }

  private val _usersState = MutableStateFlow<UsersState>(UsersState.Loading)
  val usersState: StateFlow<UsersState> = _usersState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  /** `true` enquanto o refresh manual está em andamento (600 ms mínimo para UX). */
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  /** IDs excluídos localmente mas ainda presentes no Firestore (falha de permissão). */
  private val _locallyDeletedIds = MutableStateFlow<Set<Int>>(emptySet())

  private var observeJob: Job? = null

  /**
   * Resultado de uma operação de criação ou exclusão de usuário.
   */
  sealed class ActionResult {
    /** Nenhuma operação pendente ou resultado já consumido. */
    data object Idle : ActionResult()

    /** Operação concluída com sucesso. */
    data object Success : ActionResult()

    /** Operação falhou.
     * @property message Descrição do erro para exibir na UI.
     */
    data class Error(val message: String) : ActionResult()
  }

  private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
  val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

  /**
   * Inicia ou reinicia o listener em tempo real de usuários.
   *
   * Cancela o job anterior antes de iniciar um novo. Deve ser chamado uma única vez
   * após o compose inicializar (via `LaunchedEffect`).
   *
   * @param currentUser Usuário logado (deve ser ADMINISTRADOR para ver todos os usuários).
   */
  fun observeUsers(currentUser: AppUser?) {
    observeJob?.cancel()
    observeJob = viewModelScope.launch {
      combine(
              dataConnectService.observeUsersRealtimeForAdmin(currentUser),
              _locallyDeletedIds,
          ) { result, deletedIds ->
            when (result) {
              is FirebaseDataConnectService.Result.Success ->
                  UsersState.Success(
                      result.data
                          .filter { it.id !in deletedIds }
                          .distinctBy { it.email.ifBlank { it.id.toString() } }
                  )
              is FirebaseDataConnectService.Result.Error -> UsersState.Error(result.message)
              FirebaseDataConnectService.Result.Loading -> UsersState.Loading
            }
          }
          .collect { state -> _usersState.value = state }
    }
  }

  /**
   * Força um refresh da lista reiniciando o listener e exibindo o indicador por ao menos 600 ms.
   *
   * @param currentUser Usuário logado.
   */
  fun refresh(currentUser: AppUser?) {
    viewModelScope.launch {
      _isRefreshing.value = true
      observeUsers(currentUser)
      delay(600)
      _isRefreshing.value = false
    }
  }

  /**
   * Cria um novo usuário no Firestore e o registra no [AuthManager] local.
   *
   * @param requester Usuário logado que realiza a operação (deve ser ADMINISTRADOR).
   * @param nextId ID a atribuir ao novo usuário.
   * @param name Nome completo do usuário.
   * @param email E-mail do usuário (normalizado para minúsculas).
   * @param role Papel do usuário no sistema.
   * @param plainPassword Senha em texto claro; será hashada antes de persistir.
   */
  fun createUser(
      requester: AppUser?,
      nextId: Int,
      name: String,
      email: String,
      role: tech.datatower.sebrae.desafio.data.model.UserRole,
      plainPassword: String,
  ) {
    viewModelScope.launch {
      val target =
          AppUser(id = nextId, name = name.trim(), email = email.trim().lowercase(), role = role)
      val result =
          dataConnectService.upsertUserForAdmin(
              requester = requester,
              target = target,
              plainPassword = plainPassword,
          )
      when (result) {
        is FirebaseDataConnectService.Result.Success -> {
          AuthManager.registerOrUpdateUser(target, plainPassword)
          _actionResult.value = ActionResult.Success
        }
        is FirebaseDataConnectService.Result.Error -> {
          _actionResult.value =
              ActionResult.Error(
                  result.message
                      .ifBlank { result.exception.message.orEmpty() }
                      .ifBlank { "Não foi possível cadastrar o usuário." }
              )
        }
        FirebaseDataConnectService.Result.Loading -> Unit
      }
    }
  }

  /**
   * Exclui o usuário com remoção otimista; reverte localmente se o Firestore falhar.
   *
   * @param requester Usuário logado que realiza a operação (deve ser ADMINISTRADOR).
   * @param userId ID do usuário a excluir.
   */
  fun deleteUser(requester: AppUser?, userId: Int) {
    viewModelScope.launch {
      // Remove o usuário da tela imediatamente (otimista); se Firestore falhar mas Room
      // foi atualizado, o usuário não reaparece mesmo que o snapshot não mude.
      _locallyDeletedIds.update { it + userId }

      val result = dataConnectService.deleteUserForAdmin(requester, userId)
      when (result) {
        is FirebaseDataConnectService.Result.Success -> {
          AuthManager.removeUserById(userId)
          _actionResult.value = ActionResult.Success
        }
        is FirebaseDataConnectService.Result.Error -> {
          // Reverte remoção otimista — operação falhou completamente
          _locallyDeletedIds.update { it - userId }
          _actionResult.value =
              ActionResult.Error(
                  result.message
                      .ifBlank { result.exception.message.orEmpty() }
                      .ifBlank { "Não foi possível remover o usuário." }
              )
        }
        FirebaseDataConnectService.Result.Loading -> Unit
      }
    }
  }

  /** Redefine [actionResult] para [ActionResult.Idle] após o resultado ser tratado pela UI. */
  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
