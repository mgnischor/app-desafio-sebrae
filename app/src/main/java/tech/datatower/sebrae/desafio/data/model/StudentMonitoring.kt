package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Agrega os dados de acompanhamento do aluno por domínio.
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

@Immutable
data class AttendanceRecord(
    val date: LocalDate,
    val status: AttendanceStatus,
    val minutesLate: Int = 0,
    val justification: String? = null,
)

enum class AttendanceStatus {
  Present,
  Absent,
  JustifiedAbsence,
  Late,
}

@Immutable
data class BehaviorRecord(
    val date: LocalDate,
    val participationScore: Int,
    val activityDelivery: ActivityDeliveryStatus,
    val delayMinutes: Int,
    val grade: Float?,
    val note: String,
)

enum class ActivityDeliveryStatus {
  OnTime,
  Late,
  Missing,
}

@Immutable
data class PedagogicalNeed(
    val type: PedagogicalNeedType,
    val description: String,
    val expiresAt: LocalDate?,
    val accommodations: List<String>,
)

enum class PedagogicalNeedType {
  Report,
  MedicalCertificate,
  SpecialNeed,
}

@Immutable
data class PsychologicalNeed(
    val summary: String,
    val confidentiality: ConfidentialityLevel,
    val nextStep: String,
    val reviewAt: LocalDate,
)

enum class ConfidentialityLevel {
  Restricted,
  SharedSummary,
}

@Immutable
data class ParentFollowUp(
    val date: LocalDate,
    val channel: ParentContactChannel,
    val outcome: ParentFollowUpStatus,
    val responsible: String,
    val notes: String,
)

enum class ParentContactChannel {
  Meeting,
  Phone,
  Message,
  Email,
}

enum class ParentFollowUpStatus {
  Pending,
  WaitingResponse,
  Completed,
  NoShow,
}

@Immutable
data class MonitoringAlert(
    val message: String,
    val level: MonitoringAlertLevel,
)

enum class MonitoringAlertLevel {
  Info,
  Warning,
  Critical,
}

/**
 * Regras de negócio centrais para validações e alertas da tela de acompanhamento.
 */
object StudentMonitoringRules {
  const val minimumAttendanceRate = 0.75f
  const val minimumParticipationScore = 2
  const val maximumGrade = 10f

  fun attendanceRate(records: List<AttendanceRecord>): Float {
    if (records.isEmpty()) return 1f

    val baseCount = records.count { it.status != AttendanceStatus.JustifiedAbsence }
    if (baseCount == 0) return 1f

    val presentCount = records.count {
      it.status == AttendanceStatus.Present || it.status == AttendanceStatus.Late
    }
    return presentCount.toFloat() / baseCount.toFloat()
  }

  fun averageGrade(records: List<BehaviorRecord>): Float {
    val grades = records.mapNotNull { it.grade }
    if (grades.isEmpty()) return 0f
    return grades.average().toFloat()
  }

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

    val lowParticipation = snapshot.behaviorRecords.count { it.participationScore <= minimumParticipationScore }
    if (lowParticipation >= 3) {
      alerts +=
          MonitoringAlert(
              message = "Baixa participação recorrente em sala.",
              level = MonitoringAlertLevel.Warning,
          )
    }

    val overdueParentFollowUps =
        snapshot.parentFollowUps.count {
          it.outcome == ParentFollowUpStatus.Pending || it.outcome == ParentFollowUpStatus.WaitingResponse
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

