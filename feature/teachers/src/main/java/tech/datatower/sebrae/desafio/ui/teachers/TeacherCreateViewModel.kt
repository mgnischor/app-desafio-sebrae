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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

@HiltViewModel
class TeacherCreateViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  private val companyId = AuthManager.currentCompany.map { it?.id }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val teachers: StateFlow<List<Teacher>> =
      companyId
          .filterNotNull()
          .flatMapLatest { cid -> repository.observeTeachers(cid) }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  sealed class SaveState {
    data object Idle : SaveState()

    data object Saving : SaveState()

    data object Success : SaveState()

    data class Error(val message: String) : SaveState()
  }

  private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
  val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.TEACHERS) }
  }

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

  fun resetSaveState() {
    _saveState.value = SaveState.Idle
  }
}
