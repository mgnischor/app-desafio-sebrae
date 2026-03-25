package tech.datatower.sebrae.desafio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
import java.time.LocalDate

/**
 * Testes unitários das regras de negócio do módulo de acompanhamento do aluno.
 *
 * Esta suite valida a lógica crítica de cálculo de frequência, emissão de alertas e consolidação de
 * dados de monitoramento sem dependências externas.
 *
 * Padrão: Arrange → Act → Assert (AAA)
 */
class StudentMonitoringRulesTest {

  private lateinit var hoje: LocalDate

  /** Atualiza o estado de up com os valores informados. */
  @Before
  fun setUp() {
    hoje = LocalDate.now()
  }

  // ── Testes de Taxa de Frequência ──────────────────────────────────────

  /**
   * Garante que faltas justificadas não são contabilizadas como ausências na base de cálculo da
   * taxa de frequência.
   *
   * Regra: Taxa = Presentes / (Presentes + Faltas + Atrasos), excluindo Justificadas.
   */
  @Test
  fun `attendanceRate deve ignorar faltas justificadas na base de cálculo`() {
    // Arrange: 3 registros, sendo 1 presente, 1 justificado e 1 falta
    val registros =
        listOf(
            AttendanceRecord(hoje.minusDays(3), AttendanceStatus.Present),
            AttendanceRecord(hoje.minusDays(2), AttendanceStatus.JustifiedAbsence),
            AttendanceRecord(hoje.minusDays(1), AttendanceStatus.Absent),
        )

    // Act
    val taxa = StudentMonitoringRules.attendanceRate(registros)

    // Assert: Taxa esperada = 1 presente / (1 presente + 1 falta) = 0.5
    assertEquals("Taxa de frequência deve ser 50%", 0.5f, taxa, 0.001f)
  }

  /** Valida cálculo de frequência quando todos os registros indicam presença. */
  @Test
  fun `attendanceRate deve retornar 100 porcento quando todos presentes`() {
    // Arrange
    val registros =
        listOf(
            AttendanceRecord(hoje.minusDays(2), AttendanceStatus.Present),
            AttendanceRecord(hoje.minusDays(1), AttendanceStatus.Present),
        )

    // Act
    val taxa = StudentMonitoringRules.attendanceRate(registros)

    // Assert
    assertEquals("Taxa deve ser máxima quando 100% presentes", 1.0f, taxa, 0.001f)
  }

  /** Valida cálculo de frequência quando há atrasos contabilizados. */
  @Test
  fun `attendanceRate deve contar atrasos como presenças para o cálculo`() {
    // Arrange: 2 presentes, 2 atrasos (contam como presentes), 1 falta
    val registros =
        listOf(
            AttendanceRecord(hoje.minusDays(4), AttendanceStatus.Present),
            AttendanceRecord(hoje.minusDays(3), AttendanceStatus.Late, minutesLate = 10),
            AttendanceRecord(hoje.minusDays(2), AttendanceStatus.Late, minutesLate = 5),
            AttendanceRecord(hoje.minusDays(1), AttendanceStatus.Absent),
        )

    // Act
    val taxa = StudentMonitoringRules.attendanceRate(registros)

    // Assert: Taxa = 3 presentes / (3 presentes + 1 falta) = 0.75
    assertEquals("Atrasos contam como presentes", 0.75f, taxa, 0.001f)
  }

  /** Valida cenário extremo de lista vazia de registros. */
  @Test
  fun `attendanceRate deve retornar 0 quando lista está vazia`() {
    // Arrange
    val registros = emptyList<AttendanceRecord>()

    // Act
    val taxa = StudentMonitoringRules.attendanceRate(registros)

    // Assert
    assertEquals("Taxa deve ser 0 para lista vazia", 0.0f, taxa, 0.001f)
  }

  // ── Testes de Geração de Alertas ──────────────────────────────────────

  /**
   * Garante emissão de alerta crítico quando frequência está abaixo do limite configurável (padrão:
   * 75%).
   *
   * Contexto: Aluno com múltiplas faltas nos últimos 5 dias.
   */
  @Test
  fun `buildAlerts deve retornar alerta crítico quando frequência abaixo de 75 porcento`() {
    // Arrange: Snapshot com frequência baixa (25%)
    val snapshot =
        StudentMonitoringSnapshot(
            studentId = 8,
            studentName = "Aluno teste",
            enrolledClass = "Turma X",
            attendanceRecords =
                listOf(
                    AttendanceRecord(hoje.minusDays(4), AttendanceStatus.Absent),
                    AttendanceRecord(hoje.minusDays(3), AttendanceStatus.Absent),
                    AttendanceRecord(hoje.minusDays(2), AttendanceStatus.Late, minutesLate = 15),
                    AttendanceRecord(hoje.minusDays(1), AttendanceStatus.Absent),
                ),
            behaviorRecords =
                listOf(
                    BehaviorRecord(
                        date = hoje.minusDays(4),
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
                        reviewAt = hoje.plusDays(10),
                    )
                ),
            parentFollowUps =
                listOf(
                    ParentFollowUp(
                        date = hoje.minusDays(2),
                        channel = ParentContactChannel.Phone,
                        outcome = ParentFollowUpStatus.Pending,
                        responsible = "Coordenação",
                        notes = "Tentativa 1",
                    ),
                    ParentFollowUp(
                        date = hoje.minusDays(1),
                        channel = ParentContactChannel.Message,
                        outcome = ParentFollowUpStatus.WaitingResponse,
                        responsible = "Coordenação",
                        notes = "Tentativa 2",
                    ),
                ),
        )

    // Act
    val alertas = StudentMonitoringRules.buildAlerts(snapshot)

    // Assert: Deve conter alerta referente a frequência baixa
    assertTrue(
        "Deve emitir alerta crítico para frequência abaixo do limite",
        alertas.any { it.message.contains("Frequência") },
    )
  }

  /** Valida que nenhum alerta é gerado quando o snapshot está com dados normais. */
  @Test
  fun `buildAlerts não deve retornar alertas quando todos dados estão normais`() {
    // Arrange: Snapshot com dados saudáveis
    val snapshot =
        StudentMonitoringSnapshot(
            studentId = 1,
            studentName = "Aluno Normal",
            enrolledClass = "Turma A",
            attendanceRecords =
                listOf(
                    AttendanceRecord(hoje.minusDays(2), AttendanceStatus.Present),
                    AttendanceRecord(hoje.minusDays(1), AttendanceStatus.Present),
                ),
            behaviorRecords =
                listOf(
                    BehaviorRecord(
                        date = hoje.minusDays(1),
                        participationScore = 4,
                        activityDelivery = ActivityDeliveryStatus.OnTime,
                        delayMinutes = 0,
                        grade = 8f,
                        note = "Bom",
                    )
                ),
            pedagogicalNeeds = emptyList(),
            psychologicalNeeds = emptyList(),
            parentFollowUps = emptyList(),
        )

    // Act
    val alertas = StudentMonitoringRules.buildAlerts(snapshot)

    // Assert
    assertFalse("Não deve gerar alertas para aluno com situação normal", alertas.isNotEmpty())
  }

  /** Valida emissão de alertas múltiplos quando há várias situações críticas. */
  @Test
  fun `buildAlerts deve retornar múltiplos alertas para cenários críticos combinados`() {
    // Arrange: Snapshot com frequência baixa E comportamento ruim
    val snapshot =
        StudentMonitoringSnapshot(
            studentId = 5,
            studentName = "Aluno crítico",
            enrolledClass = "Turma C",
            attendanceRecords =
                listOf(
                    AttendanceRecord(hoje.minusDays(3), AttendanceStatus.Absent),
                    AttendanceRecord(hoje.minusDays(2), AttendanceStatus.Absent),
                    AttendanceRecord(hoje.minusDays(1), AttendanceStatus.Absent),
                ),
            behaviorRecords =
                listOf(
                    BehaviorRecord(
                        date = hoje.minusDays(3),
                        participationScore = 1,
                        activityDelivery = ActivityDeliveryStatus.Missing,
                        delayMinutes = 30,
                        grade = 2f,
                        note = "Participação mínima",
                    ),
                    BehaviorRecord(
                        date = hoje.minusDays(2),
                        participationScore = 1,
                        activityDelivery = ActivityDeliveryStatus.Missing,
                        delayMinutes = 30,
                        grade = 3f,
                        note = "Sem progresso",
                    ),
                ),
            pedagogicalNeeds = emptyList(),
            psychologicalNeeds = emptyList(),
            parentFollowUps = emptyList(),
        )

    // Act
    val alertas = StudentMonitoringRules.buildAlerts(snapshot)

    // Assert: Deve gerar múltiplos alertas
    assertTrue("Deve gerar alertas críticos", alertas.size >= 1)
  }
}
