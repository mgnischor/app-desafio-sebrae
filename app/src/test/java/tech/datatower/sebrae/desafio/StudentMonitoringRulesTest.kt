package tech.datatower.sebrae.desafio

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUp
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringRules
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot

class StudentMonitoringRulesTest {

  @Test
  fun attendanceRate_ignoresJustifiedAbsenceInBase() {
    val records =
        listOf(
            AttendanceRecord(LocalDate.now().minusDays(3), AttendanceStatus.Present),
            AttendanceRecord(LocalDate.now().minusDays(2), AttendanceStatus.JustifiedAbsence),
            AttendanceRecord(LocalDate.now().minusDays(1), AttendanceStatus.Absent),
        )

    val rate = StudentMonitoringRules.attendanceRate(records)

    assertEquals(0.5f, rate, 0.001f)
  }

  @Test
  fun buildAlerts_returnsCriticalWhenAttendanceBelowThreshold() {
    val today = LocalDate.now()
    val snapshot =
        StudentMonitoringSnapshot(
            studentId = 8,
            studentName = "Aluno teste",
            enrolledClass = "Turma X",
            attendanceRecords =
                listOf(
                    AttendanceRecord(today.minusDays(4), AttendanceStatus.Absent),
                    AttendanceRecord(today.minusDays(3), AttendanceStatus.Absent),
                    AttendanceRecord(today.minusDays(2), AttendanceStatus.Late, minutesLate = 15),
                    AttendanceRecord(today.minusDays(1), AttendanceStatus.Absent),
                ),
            behaviorRecords =
                listOf(
                    BehaviorRecord(
                        date = today.minusDays(4),
                        participationScore = 2,
                        activityDelivery = ActivityDeliveryStatus.Missing,
                        delayMinutes = 15,
                        grade = 5f,
                        note = "Sem entrega",
                    )
                ),
            pedagogicalNeeds = emptyList(),
            psychologicalNeeds =
                listOf(
                    tech.datatower.sebrae.desafio.data.model.PsychologicalNeed(
                        summary = "Acompanhamento",
                        confidentiality = ConfidentialityLevel.Restricted,
                        nextStep = "Revisar",
                        reviewAt = today.plusDays(10),
                    )
                ),
            parentFollowUps =
                listOf(
                    ParentFollowUp(
                        date = today.minusDays(2),
                        channel = ParentContactChannel.Phone,
                        outcome = ParentFollowUpStatus.Pending,
                        responsible = "Coordenação",
                        notes = "Tentativa 1",
                    ),
                    ParentFollowUp(
                        date = today.minusDays(1),
                        channel = ParentContactChannel.Message,
                        outcome = ParentFollowUpStatus.WaitingResponse,
                        responsible = "Coordenação",
                        notes = "Tentativa 2",
                    ),
                ),
        )

    val alerts = StudentMonitoringRules.buildAlerts(snapshot)

    assertTrue(alerts.any { it.message.contains("Frequência") })
  }
}

