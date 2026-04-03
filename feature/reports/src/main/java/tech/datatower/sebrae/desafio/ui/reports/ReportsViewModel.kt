/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/reports/ReportsViewModel.kt
    Descrição: ViewModel da tela de relatórios, consolidando dados para exibição.
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
package tech.datatower.sebrae.desafio.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.data.repository.CourseCompletionMetric
import tech.datatower.sebrae.desafio.data.repository.MonthlyEnrollmentMetric
import tech.datatower.sebrae.desafio.data.repository.ReportSummary
import tech.datatower.sebrae.desafio.data.repository.StatusDistributionMetric
import javax.inject.Inject

/**
 * ViewModel da tela de relatórios.
 *
 * Consolida quatro [StateFlow]s reativos derivados dos dados da empresa ativa:
 * - [summary]: totais gerais (alunos, cursos, turmas, taxa de conclusão, certificados).
 * - [courseCompletion]: taxa de conclusão por curso.
 * - [monthlyEnrollments]: matrículas mensais.
 * - [statusDistribution]: distribuição de status de alunos.
 *
 * Um listener em tempo real do Firestore é iniciado no bloco `init` e cancelado automaticamente
 * quando o ViewModel é destruído.
 */
@HiltViewModel
class ReportsViewModel
@Inject
constructor(
    private val repository: AppRepository,
    dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  private val defaultSummary =
      ReportSummary(
          activeStudents = 0,
          activeCourses = 0,
          totalClasses = 0,
          completionRate = 0f,
          certificates = 0,
          averageTeacherRating = 0f,
      )

  /** Estatísticas consolidadas da empresa ativa (alunos ativos, cursos, turmas, etc.). */
  @OptIn(ExperimentalCoroutinesApi::class)
  val summary: StateFlow<ReportSummary> =
      AuthManager.currentCompany
          .map { it?.id }
          .flatMapLatest { cid ->
            if (cid == null) return@flatMapLatest flowOf(defaultSummary)
            repository.observeReportSummary(cid)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultSummary)

  /** Taxa de conclusão por curso; lista vazia se a empresa ainda não foi selecionada. */
  @OptIn(ExperimentalCoroutinesApi::class)
  val courseCompletion: StateFlow<List<CourseCompletionMetric>> =
      AuthManager.currentCompany
          .map { it?.id }
          .flatMapLatest { cid ->
            if (cid == null) return@flatMapLatest flowOf(emptyList())
            repository.observeCourseCompletionMetrics(cid)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Métricas de matrículas mensais para os últimos períodos disponíveis. */
  @OptIn(ExperimentalCoroutinesApi::class)
  val monthlyEnrollments: StateFlow<List<MonthlyEnrollmentMetric>> =
      AuthManager.currentCompany
          .map { it?.id }
          .flatMapLatest { cid ->
            if (cid == null) return@flatMapLatest flowOf(emptyList())
            repository.observeMonthlyEnrollmentMetrics(cid)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Distribuição de alunos por status (Ativo, Inativo, Concluído, etc.). */
  @OptIn(ExperimentalCoroutinesApi::class)
  val statusDistribution: StateFlow<List<StatusDistributionMetric>> =
      AuthManager.currentCompany
          .map { it?.id }
          .flatMapLatest { cid ->
            if (cid == null) return@flatMapLatest flowOf(emptyList())
            repository.observeStatusDistribution(cid)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  init {
    // Inicia listeners em tempo real do Firestore para todas as collections
    // que alimentam os relatórios. Os listeners são cancelados automaticamente
    // quando o ViewModel é destruído.
    viewModelScope.launch { dataConnectService.observeReportDataRealtime().collect {} }
  }
}
