package tech.datatower.sebrae.desafio.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.BehaviorRecord
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.MonitoringAlert
import tech.datatower.sebrae.desafio.data.model.MonitoringAlertLevel
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUp
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeed
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.PsychologicalNeed
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringRules
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de acompanhamento integral do aluno.
 *
 * Consolida cadastro e leitura de frequência, comportamento, necessidades pedagógicas, necessidades
 * psicológicas e acompanhamento dos pais em uma navegação por abas.
 *
 * @param studentId Identificador do aluno selecionado na listagem.
 * @param onBack Ação executada ao retornar para a tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMonitoringScreen(
    studentId: Int,
    onBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val snapshot by
      repository.observeStudentMonitoringSnapshot(studentId).collectAsState(initial = null)
  if (snapshot == null) {
    DetailScaffold(title = "Acompanhamento", onBack = onBack) { innerPadding, _ ->
      Column(modifier = Modifier.padding(innerPadding).padding(20.dp)) {
        Text("Aluno não encontrado.")
      }
    }
    return
  }

  val data = snapshot!!
  val alerts = remember(data) { StudentMonitoringRules.buildAlerts(data) }
  val attendanceRate =
      remember(data) { StudentMonitoringRules.attendanceRate(data.attendanceRecords) }
  val averageGrade = remember(data) { StudentMonitoringRules.averageGrade(data.behaviorRecords) }

  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Frequência", "Comportamentos", "Pedagógico", "Psicológico", "Pais")

  DetailScaffold(title = "Acompanhamento", onBack = onBack) { innerPadding, _ ->
    LazyColumn(
        modifier = Modifier.padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        StudentHeader(
            name = data.studentName,
            enrolledClass = data.enrolledClass,
            attendanceRate = attendanceRate,
            averageGrade = averageGrade,
        )
      }

      if (alerts.isNotEmpty()) {
        item { AlertsCard(alerts = alerts) }
      }

      item {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
          tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = { Text(title) },
            )
          }
        }
      }

      item {
        when (selectedTab) {
          0 -> AttendanceSection(data)
          1 -> BehaviorSection(data)
          2 -> PedagogicalSection(data)
          3 -> PsychologicalSection(data)
          else -> ParentSection(data)
        }
      }
    }
  }
}

/**
 * Cabeçalho resumido com identificação do aluno e indicadores principais.
 *
 * @param name Nome do aluno exibido no topo.
 * @param enrolledClass Turma vinculada ao aluno.
 * @param attendanceRate Taxa de frequência consolidada.
 * @param averageGrade Média de notas do período.
 */
@Composable
private fun StudentHeader(
    name: String,
    enrolledClass: String,
    attendanceRate: Float,
    averageGrade: Float,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
      )
      Text(
          text = enrolledClass,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip(
            label = "Frequência ${(attendanceRate * 100).toInt()}%",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        StatusChip(
            label = "Média ${"%.1f".format(averageGrade)}",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }
  }
}

/**
 * Bloco de alertas automáticos gerados pelas regras de acompanhamento.
 *
 * @param alerts Alertas ativos para o aluno no contexto atual.
 */
@Composable
private fun AlertsCard(alerts: List<MonitoringAlert>) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Alertas automáticos",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      alerts.forEach { alert ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          val color =
              when (alert.level) {
                MonitoringAlertLevel.Info -> MaterialTheme.colorScheme.secondaryContainer
                MonitoringAlertLevel.Warning -> MaterialTheme.colorScheme.tertiaryContainer
                MonitoringAlertLevel.Critical -> MaterialTheme.colorScheme.errorContainer
              }
          Spacer(
              modifier =
                  Modifier.width(6.dp)
                      .height(22.dp)
                      .background(color = color, shape = RoundedCornerShape(50))
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = alert.message, style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}

/**
 * Renderiza regras e histórico de frequência.
 *
 * @param snapshot Snapshot consolidado do acompanhamento do aluno.
 */
@Composable
private fun AttendanceSection(snapshot: StudentMonitoringSnapshot) {
  DomainSectionCard(
      title = "Cadastro de frequência",
      options =
          listOf(
              "Status por dia/aula: Presente, Falta, Falta Justificada, Atraso.",
              "Falta justificada deve registrar motivo e protocolo.",
              "Atraso registra minutos para gatilhos recorrentes.",
          ),
  )

  Spacer(modifier = Modifier.height(10.dp))

  snapshot.attendanceRecords.forEach { record ->
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(record.date.toString(), style = MaterialTheme.typography.labelMedium)
          if (!record.justification.isNullOrBlank()) {
            Text(
                "Justificativa: ${record.justification}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        StatusChip(
            label = attendanceStatusLabel(record),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

/**
 * Renderiza regras e histórico de comportamento em sala.
 *
 * @param snapshot Snapshot consolidado do acompanhamento do aluno.
 */
@Composable
private fun BehaviorSection(snapshot: StudentMonitoringSnapshot) {
  DomainSectionCard(
      title = "Cadastro de comportamento",
      options =
          listOf(
              "Participação em escala de 1 a 5.",
              "Entrega de atividade: no prazo, atrasada, não entregue.",
              "Notas válidas entre 0 e 10.",
          ),
  )

  Spacer(modifier = Modifier.height(10.dp))

  snapshot.behaviorRecords.forEach { record ->
    val validation = StudentMonitoringRules.validateBehaviorRecord(record)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = record.date.toString(), style = MaterialTheme.typography.labelMedium)
        Text(
            text =
                "Participação ${record.participationScore}/5 · Entrega ${deliveryLabel(record.activityDelivery)} · Atraso ${record.delayMinutes} min",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Nota: ${record.grade ?: "-"} · ${record.note}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (validation.isNotEmpty()) {
          Text(
              text = validation.joinToString(" | "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

/**
 * Renderiza regras e histórico de necessidades pedagógicas.
 *
 * @param snapshot Snapshot consolidado do acompanhamento do aluno.
 */
@Composable
private fun PedagogicalSection(snapshot: StudentMonitoringSnapshot) {
  DomainSectionCard(
      title = "Necessidades pedagógicas",
      options =
          listOf(
              "Tipos permitidos: laudo, atestado e necessidade especial.",
              "Documentos com validade geram alerta de vencimento.",
              "Registrar adaptações pedagógicas aplicadas ao aluno.",
          ),
  )

  Spacer(modifier = Modifier.height(10.dp))

  snapshot.pedagogicalNeeds.forEach { need ->
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = pedagogicalTypeLabel(need.type), style = MaterialTheme.typography.titleSmall)
        Text(text = need.description, style = MaterialTheme.typography.bodySmall)
        Text(
            text =
                "Validade: ${need.expiresAt?.toString() ?: "Não se aplica"} · Adaptações: ${need.accommodations.joinToString()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/**
 * Renderiza regras e histórico de necessidades psicológicas.
 *
 * @param snapshot Snapshot consolidado do acompanhamento do aluno.
 */
@Composable
private fun PsychologicalSection(snapshot: StudentMonitoringSnapshot) {
  DomainSectionCard(
      title = "Necessidades psicológicas",
      options =
          listOf(
              "Definir nível de sigilo por registro.",
              "Registrar evolução e próximo passo do acompanhamento.",
              "Revisão periódica obrigatória por responsável técnico.",
          ),
  )

  Spacer(modifier = Modifier.height(10.dp))

  snapshot.psychologicalNeeds.forEach { need ->
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = need.summary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text =
                "Sigilo: ${confidentialityLabel(need.confidentiality)} · Próximo passo: ${need.nextStep}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Revisão em ${need.reviewAt}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/**
 * Renderiza regras e histórico de contato com responsáveis.
 *
 * @param snapshot Snapshot consolidado do acompanhamento do aluno.
 */
@Composable
private fun ParentSection(snapshot: StudentMonitoringSnapshot) {
  DomainSectionCard(
      title = "Acompanhamento dos pais",
      options =
          listOf(
              "Registrar canal de contato e responsável pelo registro.",
              "Status: pendente, aguardando resposta, concluído e não compareceu.",
              "Escalonamento automático após contatos sem retorno.",
          ),
  )

  Spacer(modifier = Modifier.height(10.dp))

  snapshot.parentFollowUps.forEach { followUp ->
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${followUp.date} · ${channelLabel(followUp.channel)}",
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "Responsável: ${followUp.responsible}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = followUp.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusChip(
            label = followUpStatusLabel(followUp.outcome),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

/**
 * Cartão padrão para destacar regras de negócio e opções de cadastro por domínio.
 *
 * @param title Título da seção de regras.
 * @param options Lista de orientações operacionais exibidas ao usuário.
 */
@Composable
private fun DomainSectionCard(title: String, options: List<String>) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      options.forEach { option ->
        Text(text = "• $option", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

/**
 * Converte o status de frequência para o rótulo exibido na interface.
 *
 * @param record Registro de frequência analisado.
 * @return Texto amigável para o status da frequência.
 */
private fun attendanceStatusLabel(record: AttendanceRecord): String =
    when (record.status) {
      AttendanceStatus.Present -> "Presente"
      AttendanceStatus.Absent -> "Falta"
      AttendanceStatus.JustifiedAbsence -> "Falta justificada"
      AttendanceStatus.Late -> "Atraso ${record.minutesLate} min"
    }

/**
 * Mapeia a situação de entrega de atividades para rótulo de exibição.
 *
 * @param status Situação da entrega cadastrada.
 * @return Texto amigável para a UI.
 */
private fun deliveryLabel(status: ActivityDeliveryStatus): String =
    when (status) {
      ActivityDeliveryStatus.OnTime -> "no prazo"
      ActivityDeliveryStatus.Late -> "com atraso"
      ActivityDeliveryStatus.Missing -> "não entregue"
    }

/**
 * Mapeia o tipo de necessidade pedagógica para rótulo de exibição.
 *
 * @param type Tipo cadastrado da necessidade.
 * @return Texto amigável para a UI.
 */
private fun pedagogicalTypeLabel(type: PedagogicalNeedType): String =
    when (type) {
      PedagogicalNeedType.Report -> "Laudo"
      PedagogicalNeedType.MedicalCertificate -> "Atestado"
      PedagogicalNeedType.SpecialNeed -> "Necessidade especial"
    }

/**
 * Mapeia o nível de confidencialidade psicológica para rótulo de exibição.
 *
 * @param level Nível de confidencialidade do registro.
 * @return Texto amigável para a UI.
 */
private fun confidentialityLabel(level: ConfidentialityLevel): String =
    when (level) {
      ConfidentialityLevel.Restricted -> "Restrito"
      ConfidentialityLevel.SharedSummary -> "Resumo compartilhado"
    }

/**
 * Mapeia o canal de contato com responsáveis para rótulo de exibição.
 *
 * @param channel Canal utilizado no contato.
 * @return Texto amigável para a UI.
 */
private fun channelLabel(channel: ParentContactChannel): String =
    when (channel) {
      ParentContactChannel.Meeting -> "Reunião"
      ParentContactChannel.Phone -> "Telefone"
      ParentContactChannel.Message -> "Mensagem"
      ParentContactChannel.Email -> "E-mail"
    }

/**
 * Mapeia o status do acompanhamento familiar para rótulo de exibição.
 *
 * @param status Situação atual do contato com os responsáveis.
 * @return Texto amigável para a UI.
 */
private fun followUpStatusLabel(status: ParentFollowUpStatus): String =
    when (status) {
      ParentFollowUpStatus.Pending -> "Pendente"
      ParentFollowUpStatus.WaitingResponse -> "Aguardando resposta"
      ParentFollowUpStatus.Completed -> "Concluído"
      ParentFollowUpStatus.NoShow -> "Não compareceu"
    }

/**
 * Gera dados de exemplo para prototipação da tela de acompanhamento.
 *
 * @param studentId Identificador do aluno solicitado.
 * @return Snapshot completo com dados mockados por domínio.
 */
private fun sampleMonitoringSnapshot(studentId: Int): StudentMonitoringSnapshot {
  val base = LocalDate.now()
  return StudentMonitoringSnapshot(
      studentId = studentId,
      studentName =
          when (studentId) {
            1 -> "Ana Lima"
            2 -> "Carlos Souza"
            3 -> "Fernanda Costa"
            else -> "Aluno #$studentId"
          },
      enrolledClass = "Turma A1",
      attendanceRecords =
          listOf(
              AttendanceRecord(base.minusDays(5), AttendanceStatus.Present),
              AttendanceRecord(base.minusDays(4), AttendanceStatus.Late, minutesLate = 12),
              AttendanceRecord(base.minusDays(3), AttendanceStatus.Absent),
              AttendanceRecord(
                  base.minusDays(2),
                  AttendanceStatus.JustifiedAbsence,
                  justification = "Atestado médico",
              ),
              AttendanceRecord(base.minusDays(1), AttendanceStatus.Late, minutesLate = 16),
          ),
      behaviorRecords =
          listOf(
              BehaviorRecord(
                  date = base.minusDays(5),
                  participationScore = 3,
                  activityDelivery = ActivityDeliveryStatus.OnTime,
                  delayMinutes = 0,
                  grade = 8.5f,
                  note = "Boa participação.",
              ),
              BehaviorRecord(
                  date = base.minusDays(3),
                  participationScore = 2,
                  activityDelivery = ActivityDeliveryStatus.Missing,
                  delayMinutes = 14,
                  grade = 6.0f,
                  note = "Não entregou atividade de revisão.",
              ),
              BehaviorRecord(
                  date = base.minusDays(1),
                  participationScore = 2,
                  activityDelivery = ActivityDeliveryStatus.Late,
                  delayMinutes = 15,
                  grade = 5.5f,
                  note = "Atrasos repetidos no início da aula.",
              ),
          ),
      pedagogicalNeeds =
          listOf(
              PedagogicalNeed(
                  type = PedagogicalNeedType.Report,
                  description = "Laudo de dislexia com orientações de leitura assistida.",
                  expiresAt = base.plusMonths(8),
                  accommodations = listOf("Tempo extra", "Fonte ampliada"),
              ),
              PedagogicalNeed(
                  type = PedagogicalNeedType.SpecialNeed,
                  description = "Plano adaptado para avaliações em etapas.",
                  expiresAt = null,
                  accommodations = listOf("Prova segmentada", "Apoio individual"),
              ),
          ),
      psychologicalNeeds =
          listOf(
              PsychologicalNeed(
                  summary = "Ansiedade em avaliações presenciais.",
                  confidentiality = ConfidentialityLevel.Restricted,
                  nextStep = "Sessão de acolhimento quinzenal",
                  reviewAt = base.plusWeeks(2),
              )
          ),
      parentFollowUps =
          listOf(
              ParentFollowUp(
                  date = base.minusDays(6),
                  channel = ParentContactChannel.Phone,
                  outcome = ParentFollowUpStatus.WaitingResponse,
                  responsible = "Prof. Marta",
                  notes = "Solicitado retorno sobre rotina de estudos em casa.",
              ),
              ParentFollowUp(
                  date = base.minusDays(2),
                  channel = ParentContactChannel.Message,
                  outcome = ParentFollowUpStatus.Pending,
                  responsible = "Coord. João",
                  notes = "Convite para reunião de alinhamento pedagógico.",
              ),
          ),
  )
}

/** Pré-visualização da tela de acompanhamento de aluno. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudentMonitoringPreview() {
  AppDesafioSEBRAETheme { StudentMonitoringScreen(studentId = 1) }
}
