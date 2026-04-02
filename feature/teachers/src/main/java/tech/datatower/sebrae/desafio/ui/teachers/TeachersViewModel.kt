/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeachersViewModel.kt
    Descrição: ViewModel da tela de listagem de professores, gerenciando estado e operações CRUD.
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
package tech.datatower.sebrae.desafio.ui.teachers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

@HiltViewModel
class TeachersViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  @OptIn(ExperimentalCoroutinesApi::class)
  val teachers: StateFlow<List<Teacher>> =
      combine(AuthManager.currentUser, AuthManager.currentCompany) { user, company ->
            Pair(user, company)
          }
          .flatMapLatest { (user, company) ->
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllTeachers()
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeTeachers(cid)
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _isInitialLoading = MutableStateFlow(true)
  val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  sealed class ActionResult {
    data object Idle : ActionResult()

    data object Success : ActionResult()

    data class Error(val message: String) : ActionResult()
  }

  private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
  val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

  init {
    viewModelScope.launch {
      dataConnectService.syncScope(ScreenDataScope.TEACHERS)
      _isInitialLoading.value = false
    }
  }

  fun refresh() {
    if (_isRefreshing.value) return
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        dataConnectService.syncScope(ScreenDataScope.TEACHERS)
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  fun deactivateTeacher(currentUser: AppUser?, teacher: Teacher) {
    viewModelScope.launch {
      val result = dataConnectService.deactivateTeacher(currentUser, teacher)
      if (result is FirebaseDataConnectService.Result.Error) {
        _actionResult.value = ActionResult.Error(result.message)
      }
    }
  }

  fun reactivateTeacher(currentUser: AppUser?, teacher: Teacher) {
    viewModelScope.launch {
      val result = dataConnectService.reactivateTeacher(currentUser, teacher)
      if (result is FirebaseDataConnectService.Result.Error) {
        _actionResult.value = ActionResult.Error(result.message)
      }
    }
  }

  fun deleteTeacher(currentUser: AppUser?, teacherId: Int) {
    viewModelScope.launch {
      val result = dataConnectService.deleteTeacher(currentUser, teacherId)
      if (result is FirebaseDataConnectService.Result.Error) {
        _actionResult.value = ActionResult.Error(result.message)
      }
    }
  }

  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
