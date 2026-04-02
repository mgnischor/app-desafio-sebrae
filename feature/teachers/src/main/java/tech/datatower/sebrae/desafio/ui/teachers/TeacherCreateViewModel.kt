/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeacherCreateViewModel.kt
    Descrição: ViewModel da tela de cadastro de professor, coordenando validação e persistência.
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

/**
 * ViewModel da tela de cadastro de professor.
 *
 * Expõe [teachers] para o seletor do formulário (preenchimento de dados de id/existência).
 * A operação de persistência é executada via [saveTeacher]; o resultado é rastreado em [saveState].
 */
@HiltViewModel
class TeacherCreateViewModel
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

  /**
   * Resultado da operação de persistência do professor.
   */
  sealed class SaveState {
    /** Formulário ainda não submetido ou resultado já consumido. */
    data object Idle : SaveState()

    /** Persistência em andamento; botão de confirmação deve ser desabilitado. */
    data object Saving : SaveState()

    /** Persistência concluída com sucesso; navegar de volta. */
    data object Success : SaveState()

    /** Persistência falhou.
     * @property message Descrição do erro para exibir ao usuário.
     */
    data class Error(val message: String) : SaveState()
  }

  private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
  /** Estado atual da operação de persistência do professor. */
  val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.TEACHERS) }
  }

  /**
   * Persiste o professor no Firestore e no banco local.
   *
   * @param requester Usuário logado (deve ter permissão para criar professores).
   * @param teacher Dados completos do professor a cadastrar ou atualizar.
   */
  fun saveTeacher(requester: AppUser?, teacher: Teacher) {
    viewModelScope.launch {
      _saveState.value = SaveState.Saving
      _saveState.value =
          when (
              val result =
                  dataConnectService.upsertTeacher(requester = requester, teacher = teacher)
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
