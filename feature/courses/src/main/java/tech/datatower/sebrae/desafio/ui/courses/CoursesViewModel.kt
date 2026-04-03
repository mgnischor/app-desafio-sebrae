/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/courses/CoursesViewModel.kt
    Descrição: ViewModel da tela de listagem de cursos, gerenciando estado e operações CRUD.
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
package tech.datatower.sebrae.desafio.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.domain.usecase.ObserveCoursesUseCase
import tech.datatower.sebrae.desafio.domain.usecase.SyncScreenDataUseCase
import javax.inject.Inject

/**
 * ViewModel da tela de listagem de cursos.
 *
 * Expõe [courses] como [StateFlow] reativo filtrado pelo papel do usuário: administradores vêem
 * todos os cursos; demais perfis apenas os da empresa corrente. Fornece [refresh],
 * [deactivateCourse], [reactivateCourse] e [deleteCourse] com feedback via [actionResult].
 */
@HiltViewModel
class CoursesViewModel
@Inject
constructor(
    private val observeCoursesUseCase: ObserveCoursesUseCase,
    private val syncScreenDataUseCase: SyncScreenDataUseCase,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  /** Lista de cursos visível para o perfil logado, atualizada reativamente pelo Room. */
  val courses: StateFlow<List<Course>> =
      observeCoursesUseCase()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _isInitialLoading = MutableStateFlow(true)
  /**
   * `true` até a primeira sincronização com o Firestore concluir; controla o indicador inicial de
   * carga.
   */
  val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  /** `true` enquanto um pull-to-refresh manual está em andamento. */
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  /** Resultado de uma operação de desativação, reativação ou exclusão de curso. */
  sealed class ActionResult {
    /** Nenhuma operação pendente ou resultado já consumido. */
    data object Idle : ActionResult()

    /** Operação concluída com sucesso. */
    data object Success : ActionResult()

    /**
     * Operação falhou.
     *
     * @property message Descrição do erro para exibir na UI.
     */
    data class Error(val message: String) : ActionResult()
  }

  private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
  val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

  init {
    viewModelScope.launch {
      syncScreenDataUseCase(ScreenDataScope.COURSES)
      _isInitialLoading.value = false
    }
  }

  /**
   * Solicita nova sincronização com o Firestore (pull-to-refresh). Ignorado se já há um refresh em
   * andamento.
   */
  fun refresh() {
    if (_isRefreshing.value) return
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        syncScreenDataUseCase(ScreenDataScope.COURSES)
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  /**
   * Desativa o curso no Firestore e no banco local. Erros são expostos via [actionResult].
   *
   * @param currentUser Usuário logado (deve ter permissão para gerir cursos).
   * @param course Curso a desativar.
   */
  fun deactivateCourse(currentUser: AppUser?, course: Course) {
    viewModelScope.launch {
      val result = dataConnectService.deactivateCourse(currentUser, course)
      if (result is FirebaseDataConnectService.Result.Error) {
        _actionResult.value = ActionResult.Error(result.message)
      }
    }
  }

  /**
   * Reativa um curso previamente desativado. Erros são expostos via [actionResult].
   *
   * @param currentUser Usuário logado (deve ter permissão para gerir cursos).
   * @param course Curso a reativar.
   */
  fun reactivateCourse(currentUser: AppUser?, course: Course) {
    viewModelScope.launch {
      val result = dataConnectService.reactivateCourse(currentUser, course)
      if (result is FirebaseDataConnectService.Result.Error) {
        _actionResult.value = ActionResult.Error(result.message)
      }
    }
  }

  /**
   * Exclui permanentemente o curso pelo ID. Erros são expostos via [actionResult].
   *
   * @param currentUser Usuário logado (deve ter permissão para excluir cursos).
   * @param courseId ID do curso a remover.
   */
  fun deleteCourse(currentUser: AppUser?, courseId: Int) {
    viewModelScope.launch {
      val result = dataConnectService.deleteCourse(currentUser, courseId)
      if (result is FirebaseDataConnectService.Result.Error) {
        _actionResult.value = ActionResult.Error(result.message)
      }
    }
  }

  /** Redefine [actionResult] para [ActionResult.Idle] após o resultado ser tratado pela UI. */
  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
