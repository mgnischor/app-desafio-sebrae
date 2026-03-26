package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Agrega os dados de acompanhamento do aluno por domínio.
 *
 * @property studentId Identificador único do aluno.
 * @property studentName Nome completo exibido nos cabeçalhos das telas.
 * @property enrolledClass Turma atual vinculada ao aluno.
 * @property attendanceRecords Histórico de frequência por data/aula.
 * @property behaviorRecords Histórico de comportamento e desempenho acadêmico.
 * @property pedagogicalNeeds Necessidades pedagógicas e adaptações ativas.
 * @property psychologicalNeeds Necessidades psicológicas com nível de sigilo.
 * @property parentFollowUps Registros de contato e devolutiva dos responsáveis.
 */
@Immutable
data class StudentMonitoringSnapshot(
    val studentId: Int,
    val studentName: String,
    val enrolledClass: String,
    val attendanceRecords: List<AttendanceRecord>,
    val behaviorRecords: List<BehaviorRecord>,
    val pedagogicalNeeds: List<PedagogicalNeed>,
    val psychologicalNeeds: List<PsychologicalNeed>,
    val parentFollowUps: List<ParentFollowUp>,
)

/**
 * Registro unitário de frequência do aluno.
 *
 * @property date Data do registro.
 * @property status Situação de presença no período.
 * @property minutesLate Minutos de atraso quando aplicável.
 * @property justification Justificativa textual para faltas justificadas.
 */
@Immutable
data class AttendanceRecord(
    val date: LocalDate,
    val status: AttendanceStatus,
    val minutesLate: Int = 0,
    val justification: String? = null,
)

/** Situações de frequência aceitas para lançamento em diário. */
enum class AttendanceStatus {
  Present,
  Absent,
  JustifiedAbsence,
  Late,
}

/**
 * Registro de comportamento e desempenho observado em um período.
 *
 * @property date Data da observação.
 * @property participationScore Escala de participação no intervalo de 1 a 5.
 * @property activityDelivery Situação da entrega de atividade.
 * @property delayMinutes Minutos de atraso no período.
 * @property grade Nota acadêmica opcional no intervalo de 0 a 10.
 * @property note Observação contextual do professor.
 */
@Immutable
data class BehaviorRecord(
    val date: LocalDate,
    val participationScore: Int,
    val activityDelivery: ActivityDeliveryStatus,
    val delayMinutes: Int,
    val grade: Float?,
    val note: String,
)

/** Situações de entrega de atividade no contexto de acompanhamento. */
enum class ActivityDeliveryStatus {
  OnTime,
  Late,
  Missing,
}

/**
 * Necessidade pedagógica formalizada para o aluno.
 *
 * @property type Tipo de necessidade registrada.
 * @property description Descrição objetiva do cenário pedagógico.
 * @property expiresAt Data de validade do documento, quando existir.
 * @property accommodations Adaptações ativas orientadas pela equipe.
 */
@Immutable
data class PedagogicalNeed(
    val type: PedagogicalNeedType,
    val description: String,
    val expiresAt: LocalDate?,
    val accommodations: List<String>,
)

/** Tipos de necessidade pedagógica contemplados no cadastro. */
enum class PedagogicalNeedType {
  Report,
  MedicalCertificate,
  SpecialNeed,
}

/**
 * Registro de acompanhamento psicológico do aluno.
 *
 * @property summary Resumo clínico/pedagógico compartilhável no contexto autorizado.
 * @property confidentiality Nível de sigilo do registro.
 * @property nextStep Próxima ação planejada no acompanhamento.
 * @property reviewAt Data prevista de revisão do caso.
 */
@Immutable
data class PsychologicalNeed(
    val summary: String,
    val confidentiality: ConfidentialityLevel,
    val nextStep: String,
    val reviewAt: LocalDate,
)

/** Níveis de sigilo aplicáveis ao registro psicológico. */
enum class ConfidentialityLevel {
  Restricted,
  SharedSummary,
}

/**
 * Histórico de contato com responsáveis do aluno.
 *
 * @property date Data do contato.
 * @property channel Canal utilizado para comunicação.
 * @property outcome Resultado atual do acompanhamento com a família.
 * @property responsible Profissional que conduziu o contato.
 * @property notes Observações do atendimento.
 */
@Immutable
data class ParentFollowUp(
    val date: LocalDate,
    val channel: ParentContactChannel,
    val outcome: ParentFollowUpStatus,
    val responsible: String,
    val notes: String,
)

/** Canais oficiais de comunicação com responsáveis. */
enum class ParentContactChannel {
  Meeting,
  Phone,
  Message,
  Email,
}

/** Situações do fluxo de acompanhamento com responsáveis. */
enum class ParentFollowUpStatus {
  Pending,
  WaitingResponse,
  Completed,
  NoShow,
}

/**
 * Alerta derivado automaticamente a partir das regras de acompanhamento.
 *
 * @property message Mensagem orientativa exibida no painel do aluno.
 * @property level Severidade do alerta para priorização de ação.
 */
@Immutable
data class MonitoringAlert(
    val message: String,
    val level: MonitoringAlertLevel,
)

/** Níveis de severidade usados no painel de pendências. */
enum class MonitoringAlertLevel {
  Info,
  Warning,
  Critical,
}

/** Regras de negócio centrais para validações e alertas da tela de acompanhamento. */
object StudentMonitoringRules {
  /** Percentual mínimo de frequência esperado para evitar alerta crítico. */
  const val minimumAttendanceRate = 0.75f

  /** Pontuação mínima de participação tolerada antes de alerta recorrente. */
  const val minimumParticipationScore = 2

  /** Nota máxima permitida no lançamento acadêmico. */
  const val maximumGrade = 10f

  /**
   * Calcula a taxa de presença considerando faltas justificadas fora da base.
   *
   * @param records Lista de registros de frequência analisados.
   * @return Taxa de presença no intervalo `0f..1f`.
   */
  fun attendanceRate(records: List<AttendanceRecord>): Float {
    if (records.isEmpty()) return 0f

    val baseCount = records.count { it.status != AttendanceStatus.JustifiedAbsence }
    if (baseCount == 0) return 0f

    val presentCount = records.count {
      it.status == AttendanceStatus.Present || it.status == AttendanceStatus.Late
    }
    return presentCount.toFloat() / baseCount.toFloat()
  }

  /**
   * Calcula a média das notas disponíveis no histórico comportamental.
   *
   * @param records Lista de registros de comportamento.
   * @return Média de notas no intervalo `0f..10f`; retorna `0f` sem notas lançadas.
   */
  fun averageGrade(records: List<BehaviorRecord>): Float {
    val grades = records.mapNotNull { it.grade }
    if (grades.isEmpty()) return 0f
    return grades.average().toFloat()
  }

  /**
   * Valida consistência de um registro de comportamento antes de persistência.
   *
   * @param record Registro a ser validado.
   * @return Lista de mensagens de erro; vazia quando o registro é válido.
   */
  fun validateBehaviorRecord(record: BehaviorRecord): List<String> {
    val errors = mutableListOf<String>()

    if (record.participationScore !in 1..5) {
      errors += "Participação deve estar na escala de 1 a 5."
    }
    if (record.delayMinutes < 0) {
      errors += "Atraso não pode ser negativo."
    }
    if (record.grade != null && (record.grade < 0f || record.grade > maximumGrade)) {
      errors += "Nota deve estar no intervalo de 0 a $maximumGrade."
    }
    if (record.activityDelivery == ActivityDeliveryStatus.Missing && record.note.isBlank()) {
      errors += "Entregas não realizadas exigem observação."
    }

    return errors
  }

  /**
   * Gera alertas automáticos para apoiar priorização do acompanhamento escolar.
   *
   * @param snapshot Estado consolidado de acompanhamento do aluno.
   * @return Lista de alertas ordenada conforme as regras aplicadas.
   */
  fun buildAlerts(snapshot: StudentMonitoringSnapshot): List<MonitoringAlert> {
    val alerts = mutableListOf<MonitoringAlert>()

    val attendance = attendanceRate(snapshot.attendanceRecords)
    if (attendance < minimumAttendanceRate) {
      alerts +=
          MonitoringAlert(
              message =
                  "Frequência de ${(attendance * 100).toInt()}% abaixo do limite de ${(minimumAttendanceRate * 100).toInt()}%.",
              level = MonitoringAlertLevel.Critical,
          )
    }

    val recurringDelays = snapshot.behaviorRecords.count { it.delayMinutes >= 10 }
    if (recurringDelays >= 3) {
      alerts +=
          MonitoringAlert(
              message = "Atrasos recorrentes detectados em $recurringDelays registros recentes.",
              level = MonitoringAlertLevel.Warning,
          )
    }

    val lowParticipation =
        snapshot.behaviorRecords.count { it.participationScore <= minimumParticipationScore }
    if (lowParticipation >= 3) {
      alerts +=
          MonitoringAlert(
              message = "Baixa participação recorrente em sala.",
              level = MonitoringAlertLevel.Warning,
          )
    }

    val overdueParentFollowUps =
        snapshot.parentFollowUps.count {
          it.outcome == ParentFollowUpStatus.Pending ||
              it.outcome == ParentFollowUpStatus.WaitingResponse
        }
    if (overdueParentFollowUps >= 2) {
      alerts +=
          MonitoringAlert(
              message = "Existem $overdueParentFollowUps contatos familiares pendentes.",
              level = MonitoringAlertLevel.Info,
          )
    }

    return alerts
  }
}
