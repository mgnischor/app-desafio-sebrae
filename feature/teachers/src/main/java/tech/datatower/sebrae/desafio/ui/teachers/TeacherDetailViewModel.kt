/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeacherDetailViewModel.kt
    Descrição: ViewModel da tela de detalhes de professor, carregando dados do professor e suas turmas.
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

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import javax.inject.Inject

/**
 * ViewModel da tela de detalhes de professor.
 *
 * Carrega os dados do professor ([teacher]) e as turmas que ele ministra ([classes]) a partir do ID
 * recebido via [SavedStateHandle]. Administradores vêem todas as turmas; demais perfis apenas as da
 * empresa corrente.
 */
@HiltViewModel
class TeacherDetailViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val teacherId: Int = checkNotNull(savedStateHandle[AppRoutes.TEACHER_ID_ARG])

  /**
   * Dados completos do professor identificado pelo ID recebido na navegação; `null` enquanto
   * carrega.
   */
  val teacher: StateFlow<Teacher?> =
      repository
          .observeTeacherById(teacherId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  /**
   * Turmas ministradas por este professor, filtradas pela empresa ativa quando não-administrador.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val classes: StateFlow<List<SchoolClass>> =
      combine(teacher, AuthManager.currentUser, AuthManager.currentCompany) { t, user, company ->
            Triple(t, user, company)
          }
          .flatMapLatest { (t, user, company) ->
            val name = t?.name ?: return@flatMapLatest flowOf(emptyList())
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllClasses().map { list -> list.filter { it.instructor == name } }
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeClassesByTeacher(cid, name)
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Resultado da operação de atualização do professor. */
  sealed class SaveState {
    /** Nenhuma operação pendente ou resultado já consumido. */
    data object Idle : SaveState()

    /** Atualização concluída com sucesso. */
    data object Success : SaveState()

    /**
     * Atualização falhou.
     *
     * @property message Descrição do erro para exibir na UI.
     */
    data class Error(val message: String) : SaveState()
  }

  private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
  /** Estado atual da operação de persistência do professor. */
  val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.TEACHER_DETAIL) }
  }

  /**
   * Persiste as alterações do professor no Firestore e no banco local.
   *
   * @param requester Usuário logado (deve ter permissão para atualizar professores).
   * @param name Novo nome do professor.
   * @param email Novo e-mail do professor.
   * @param specialty Nova especialidade do professor.
   * @param activeCourses Novo total de cursos ativos.
   * @param totalStudents Novo total de alunos.
   * @param rating Nova avaliação (0f..5f).
   */
  fun updateTeacher(
      requester: AppUser?,
      name: String,
      email: String,
      specialty: String,
      activeCourses: Int,
      totalStudents: Int,
      rating: Float,
  ) {
    val current = teacher.value ?: return
    viewModelScope.launch {
      _saveState.value =
          when (
              val result =
                  dataConnectService.upsertTeacher(
                      requester = requester,
                      teacher =
                          current.copy(
                              name = name.trim(),
                              email = email.trim(),
                              specialty = specialty.trim(),
                              activeCourses = activeCourses,
                              totalStudents = totalStudents,
                              rating = rating,
                          ),
                  )
          ) {
            is FirebaseDataConnectService.Result.Success -> SaveState.Success
            is FirebaseDataConnectService.Result.Error -> SaveState.Error(result.message)
            FirebaseDataConnectService.Result.Loading -> SaveState.Idle
          }
    }
  }

  /** Redefine [saveState] para [SaveState.Idle] após o resultado ser tratado pela UI. */
  fun clearSaveState() {
    _saveState.value = SaveState.Idle
  }
}
