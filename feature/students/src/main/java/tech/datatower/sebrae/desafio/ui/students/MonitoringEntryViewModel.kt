/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /feature/students/src/main/java/tech/datatower/sebrae/desafio/ui/students/MonitoringEntryViewModel.kt
    Descrição: ViewModel da tela de registro de entrada de monitoramento do aluno.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
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
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUp
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeed
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.PsychologicalNeed
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel do formulário de registro de entrada de monitoramento.
 *
 * Expõe o [snapshot] atual do aluno e disponibiliza [saveEntry] para persistir um novo registro de
 * frequência, comportamento, necessidade pedagógica/psicológica ou seguimento com responsável. O
 * tipo de entrada é selecionado via [MonitoringEntryType] (definido na tela de entrada).
 */
@HiltViewModel
class MonitoringEntryViewModel
@Inject
constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val studentId: Int = checkNotNull(savedStateHandle[AppRoutes.STUDENT_ID_ARG])

  /** Snapshot atual do aluno; `null` até os dados locais estarem disponíveis. */
  val snapshot: StateFlow<StudentMonitoringSnapshot?> =
      repository
          .observeStudentMonitoringSnapshot(studentId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  /**
   * Persiste um novo registro de monitoramento para o aluno atual.
   *
   * Determina a tabela de destino com base em [entryType] e construí o objeto de domínio com os
   * parâmetros fornecidos. A data é sempre a data de hoje.
   *
   * @param entryType Tipo de entrada a registrar (frequência, comportamento, etc.).
   * @param attendanceStatus Status de presença (usado apenas quando [entryType] = ATTENDANCE).
   * @param minutesLate Minutos de atraso (apenas ATTENDANCE).
   * @param justification Justificativa de falta (apenas ATTENDANCE, opcional).
   * @param participationScore Nota de participação de 1 a 5 (apenas BEHAVIOR).
   * @param deliveryStatus Status de entrega de atividade (apenas BEHAVIOR).
   * @param delayMinutes Minutos de atraso na entrega (apenas BEHAVIOR).
   * @param grade Nota numérica da atividade (apenas BEHAVIOR, opcional).
   * @param behaviorNote Observação textual sobre comportamento (apenas BEHAVIOR).
   * @param pedagogicalType Tipo de necessidade pedagógica (apenas PEDAGOGICAL).
   * @param pedagogicalDescription Descrição da necessidade pedagógica (apenas PEDAGOGICAL).
   * @param pedagogicalAccommodations Lista de adaptações pedagógicas (apenas PEDAGOGICAL).
   * @param psychologicalSummary Resumo da necessidade psicológica (apenas PSYCHOLOGICAL).
   * @param confidentiality Nível de confidencialidade do registro psicológico (apenas
   *   PSYCHOLOGICAL).
   * @param nextStep Próximo passo sugerido para o acompanhamento psicológico (apenas
   *   PSYCHOLOGICAL).
   * @param parentChannel Canal de contato com responsável (apenas PARENT_FOLLOWUP).
   * @param parentOutcome Resultado do contato com responsável (apenas PARENT_FOLLOWUP).
   * @param parentResponsible Profissional responsável pelo contato (apenas PARENT_FOLLOWUP).
   * @param parentNotes Observações sobre o contato com responsável (apenas PARENT_FOLLOWUP).
   * @return `true` se o registro foi persistido com sucesso; `false` em caso de erro ou empresa não
   *   selecionada.
   */
  suspend fun saveEntry(
      entryType: MonitoringEntryType,
      // Attendance
      attendanceStatus: AttendanceStatus = AttendanceStatus.Present,
      minutesLate: Int = 0,
      justification: String? = null,
      // Behavior
      participationScore: Int = 3,
      deliveryStatus: ActivityDeliveryStatus = ActivityDeliveryStatus.OnTime,
      delayMinutes: Int = 0,
      grade: Float? = null,
      behaviorNote: String = "",
      // Pedagogical
      pedagogicalType: PedagogicalNeedType = PedagogicalNeedType.Report,
      pedagogicalDescription: String = "",
      pedagogicalAccommodations: List<String> = emptyList(),
      // Psychological
      psychologicalSummary: String = "",
      confidentiality: ConfidentialityLevel = ConfidentialityLevel.Restricted,
      nextStep: String = "",
      // Parent follow-up
      parentChannel: ParentContactChannel = ParentContactChannel.Meeting,
      parentOutcome: ParentFollowUpStatus = ParentFollowUpStatus.Pending,
      parentResponsible: String = "",
      parentNotes: String = "",
  ): Boolean {
    val companyId = AuthManager.currentCompany.value?.id ?: return false
    val today = LocalDate.now()

    return try {
      when (entryType) {
        MonitoringEntryType.ATTENDANCE -> {
          repository.insertAttendanceRecord(
              companyId,
              studentId,
              AttendanceRecord(
                  date = today,
                  status = attendanceStatus,
                  minutesLate = minutesLate,
                  justification = justification,
              ),
          )
        }
        MonitoringEntryType.BEHAVIOR -> {
          repository.insertBehaviorRecord(
              companyId,
              studentId,
              BehaviorRecord(
                  date = today,
                  participationScore = participationScore,
                  activityDelivery = deliveryStatus,
                  delayMinutes = delayMinutes,
                  grade = grade,
                  note = behaviorNote,
              ),
          )
        }
        MonitoringEntryType.PEDAGOGICAL -> {
          repository.insertPedagogicalNeedRecord(
              companyId,
              studentId,
              PedagogicalNeed(
                  type = pedagogicalType,
                  description = pedagogicalDescription,
                  expiresAt = null,
                  accommodations = pedagogicalAccommodations,
              ),
          )
        }
        MonitoringEntryType.PSYCHOLOGICAL -> {
          repository.insertPsychologicalNeedRecord(
              companyId,
              studentId,
              PsychologicalNeed(
                  summary = psychologicalSummary,
                  confidentiality = confidentiality,
                  nextStep = nextStep,
                  reviewAt = today.plusMonths(1),
              ),
          )
        }
        MonitoringEntryType.PARENT_FOLLOWUP -> {
          repository.insertParentFollowUpRecord(
              companyId,
              studentId,
              ParentFollowUp(
                  date = today,
                  channel = parentChannel,
                  outcome = parentOutcome,
                  responsible = parentResponsible,
                  notes = parentNotes,
              ),
          )
        }
      }
      true
    } catch (_: Exception) {
      false
    }
  }
}
