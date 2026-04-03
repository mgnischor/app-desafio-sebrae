/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/classes/ClassCreateViewModel.kt
    Descrição: ViewModel da tela de cadastro de turma, coordenando validação e persistência.
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
package tech.datatower.sebrae.desafio.ui.classes

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
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

/**
 * ViewModel da tela de cadastro de turma.
 *
 * Expõe [classes], [courses] e [teachers] para preencher os seletores do formulário. A operação de
 * persistência é executada via [saveClass]; o resultado é rastreado em [saveState].
 */
@HiltViewModel
class ClassCreateViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  @OptIn(ExperimentalCoroutinesApi::class)
  val classes: StateFlow<List<SchoolClass>> =
      combine(AuthManager.currentUser, AuthManager.currentCompany) { user, company ->
            Pair(user, company)
          }
          .flatMapLatest { (user, company) ->
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllClasses()
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeClasses(cid)
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val courses: StateFlow<List<Course>> =
      combine(AuthManager.currentUser, AuthManager.currentCompany) { user, company ->
            Pair(user, company)
          }
          .flatMapLatest { (user, company) ->
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllCourses()
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeCourses(cid)
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

  /** Resultado da operação de persistência da turma. */
  sealed class SaveState {
    /** Formulário ainda não submetido ou resultado já consumido. */
    data object Idle : SaveState()

    /** Persistência em andamento; botão de confirmação deve ser desabilitado. */
    data object Saving : SaveState()

    /** Persistência concluída com sucesso; navegar de volta. */
    data object Success : SaveState()

    /**
     * Persistência falhou.
     *
     * @property message Descrição do erro para exibir ao usuário.
     */
    data class Error(val message: String) : SaveState()
  }

  private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
  /** Estado atual da operação de persistência da turma. */
  val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.CLASSES) }
  }

  /**
   * Persiste a turma no Firestore e no banco local.
   *
   * @param requester Usuário logado (deve ter permissão para criar turmas).
   * @param schoolClass Dados completos da turma a cadastrar ou atualizar.
   */
  fun saveClass(requester: AppUser?, schoolClass: SchoolClass) {
    viewModelScope.launch {
      _saveState.value = SaveState.Saving
      _saveState.value =
          when (
              val result =
                  dataConnectService.upsertClass(requester = requester, schoolClass = schoolClass)
          ) {
            is FirebaseDataConnectService.Result.Success -> SaveState.Success
            is FirebaseDataConnectService.Result.Error -> SaveState.Error(result.message)
            FirebaseDataConnectService.Result.Loading -> SaveState.Idle
          }
    }
  }

  /** Redefine [saveState] para [SaveState.Idle]; usado ao reabrir o formulário após erro. */
  fun resetSaveState() {
    _saveState.value = SaveState.Idle
  }
}
