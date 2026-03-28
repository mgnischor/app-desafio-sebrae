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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

    sealed class UsersState {
        data object Loading : UsersState()
        data class Success(val users: List<FirebaseDataConnectService.ManagedUser>) : UsersState()
        data class Error(val message: String) : UsersState()
    }

    private val _usersState = MutableStateFlow<UsersState>(UsersState.Loading)
    val usersState: StateFlow<UsersState> = _usersState.asStateFlow()

    sealed class ActionResult {
        data object Idle : ActionResult()
        data object Success : ActionResult()
        data class Error(val message: String) : ActionResult()
    }

    private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

    fun observeUsers(currentUser: AppUser?) {
        viewModelScope.launch {
            dataConnectService.observeUsersRealtimeForAdmin(currentUser).collect { result ->
                _usersState.value = when (result) {
                    is FirebaseDataConnectService.Result.Success -> UsersState.Success(result.data)
                    is FirebaseDataConnectService.Result.Error -> UsersState.Error(result.message)
                    FirebaseDataConnectService.Result.Loading -> UsersState.Loading
                }
            }
        }
    }

    fun createUser(requester: AppUser?, nextId: Int, name: String, email: String, role: tech.datatower.sebrae.desafio.data.model.UserRole, plainPassword: String) {
        viewModelScope.launch {
            val target = AppUser(id = nextId, name = name.trim(), email = email.trim().lowercase(), role = role)
            val result = dataConnectService.upsertUserForAdmin(
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
                    _actionResult.value = ActionResult.Error(
                        result.message.ifBlank { result.exception.message.orEmpty() }
                            .ifBlank { "Não foi possível cadastrar o usuário." }
                    )
                }
                FirebaseDataConnectService.Result.Loading -> Unit
            }
        }
    }

    fun deleteUser(requester: AppUser?, userId: Int) {
        viewModelScope.launch {
            val result = dataConnectService.deleteUserForAdmin(requester, userId)
            when (result) {
                is FirebaseDataConnectService.Result.Success -> {
                    AuthManager.removeUserById(userId)
                    _actionResult.value = ActionResult.Success
                }
                is FirebaseDataConnectService.Result.Error -> {
                    _actionResult.value = ActionResult.Error(
                        result.message.ifBlank { result.exception.message.orEmpty() }
                            .ifBlank { "Não foi possível remover o usuário." }
                    )
                }
                FirebaseDataConnectService.Result.Loading -> Unit
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = ActionResult.Idle
    }
}
