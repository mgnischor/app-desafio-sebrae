/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/students/StudentMonitoringViewModel.kt
    Descrição: ViewModel da tela de monitoramento de aluno, carregando snapshot de desempenho.
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
package tech.datatower.sebrae.desafio.ui.students

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import javax.inject.Inject

/**
 * ViewModel da tela de monitoramento individual de aluno.
 *
 * Carrega o [snapshot] completo de desempenho do aluno (frequência, comportamento, necessidades
 * pedagógicas/psicológicas e seguimentos com responsáveis) via observação reativa. O ID do aluno é
 * recebido do [SavedStateHandle] via rota de navegação.
 */
@HiltViewModel
class StudentMonitoringViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val studentId: Int = checkNotNull(savedStateHandle[AppRoutes.STUDENT_ID_ARG])

  /** Snapshot completo de monitoramento do aluno; `null` até os dados estarem disponíveis. */
  val snapshot: StateFlow<StudentMonitoringSnapshot?> =
      repository
          .observeStudentMonitoringSnapshot(studentId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.STUDENT_MONITORING) }
  }
}
