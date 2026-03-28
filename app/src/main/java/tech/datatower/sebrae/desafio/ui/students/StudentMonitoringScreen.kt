/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/students/StudentMonitoringScreen.kt
    Descrição: Tela de monitoramento individual de aluno, exibindo desempenho e frequência.
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AttendanceRecord
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.MonitoringAlert
import tech.datatower.sebrae.desafio.data.model.MonitoringAlertLevel
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringRules
import tech.datatower.sebrae.desafio.data.model.StudentMonitoringSnapshot
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
    onBack: () -> Unit = {},
) {
  val viewModel: StudentMonitoringViewModel = hiltViewModel()
  val snapshot by viewModel.snapshot.collectAsState()

  if (snapshot == null) {
    DetailScaffold(title = stringResource(R.string.monitoring_title), onBack = onBack) {
        innerPadding,
        _ ->
      Column(modifier = Modifier.padding(innerPadding).padding(20.dp)) {
        Text(stringResource(R.string.monitoring_student_not_found))
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
  val tabs =
      listOf(
          stringResource(R.string.tab_attendance),
          stringResource(R.string.tab_behavior),
          stringResource(R.string.tab_pedagogical),
          stringResource(R.string.tab_psychological),
          stringResource(R.string.tab_parents),
      )

  DetailScaffold(title = stringResource(R.string.monitoring_title), onBack = onBack) {
      innerPadding,
      _ ->
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

      // Lazy render tab content to avoid rebuilding off-screen sections
      when (selectedTab) {
        0 -> item { AttendanceSection(data) }
        1 -> item { BehaviorSection(data) }
        2 -> item { PedagogicalSection(data) }
        3 -> item { PsychologicalSection(data) }
        else -> item { ParentSection(data) }
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
            label = stringResource(R.string.label_attendance_rate, (attendanceRate * 100).toInt()),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        StatusChip(
            label = stringResource(R.string.label_average_grade, averageGrade),
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
          text = stringResource(R.string.monitoring_alerts_title),
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
      title = stringResource(R.string.attendance_section_title),
      options =
          listOf(
              stringResource(R.string.attendance_rule_1),
              stringResource(R.string.attendance_rule_2),
              stringResource(R.string.attendance_rule_3),
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
                stringResource(R.string.label_justification, record.justification),
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
      title = stringResource(R.string.behavior_section_title),
      options =
          listOf(
              stringResource(R.string.behavior_rule_1),
              stringResource(R.string.behavior_rule_2),
              stringResource(R.string.behavior_rule_3),
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
                "${stringResource(R.string.label_participation, record.participationScore)} · ${stringResource(R.string.label_delivery, deliveryLabel(record.activityDelivery))} · ${stringResource(R.string.label_delay)} ${record.delayMinutes} ${stringResource(R.string.label_minutes)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text =
                stringResource(R.string.label_grade, record.grade?.toString() ?: "-") +
                    " · ${record.note}",
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
      title = stringResource(R.string.pedagogical_section_title),
      options =
          listOf(
              stringResource(R.string.pedagogical_rule_1),
              stringResource(R.string.pedagogical_rule_2),
              stringResource(R.string.pedagogical_rule_3),
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
                stringResource(
                    R.string.label_validity,
                    need.expiresAt?.toString() ?: stringResource(R.string.label_not_applicable),
                ) +
                    " · " +
                    stringResource(
                        R.string.label_accommodations,
                        need.accommodations.joinToString(),
                    ),
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
      title = stringResource(R.string.psychological_section_title),
      options =
          listOf(
              stringResource(R.string.psychological_rule_1),
              stringResource(R.string.psychological_rule_2),
              stringResource(R.string.psychological_rule_3),
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
                stringResource(
                    R.string.label_confidentiality,
                    confidentialityLabel(need.confidentiality),
                ) + " · " + stringResource(R.string.label_next_step, need.nextStep),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.label_review_at, need.reviewAt.toString()),
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
      title = stringResource(R.string.parents_section_title),
      options =
          listOf(
              stringResource(R.string.parents_rule_1),
              stringResource(R.string.parents_rule_2),
              stringResource(R.string.parents_rule_3),
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
            text = stringResource(R.string.label_responsible, followUp.responsible),
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
        Text(
            text = "${stringResource(R.string.separator_bullet)} $option",
            style = MaterialTheme.typography.bodySmall,
        )
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
@Composable
private fun attendanceStatusLabel(record: AttendanceRecord): String =
    when (record.status) {
      AttendanceStatus.Present -> stringResource(R.string.attendance_status_present)
      AttendanceStatus.Absent -> stringResource(R.string.attendance_status_absent)
      AttendanceStatus.JustifiedAbsence -> stringResource(R.string.attendance_status_justified)
      AttendanceStatus.Late -> stringResource(R.string.attendance_status_late, record.minutesLate)
    }

/**
 * Mapeia a situação de entrega de atividades para rótulo de exibição.
 *
 * @param status Situação da entrega cadastrada.
 * @return Texto amigável para a UI.
 */
@Composable
private fun deliveryLabel(status: ActivityDeliveryStatus): String =
    when (status) {
      ActivityDeliveryStatus.OnTime -> stringResource(R.string.delivery_on_time)
      ActivityDeliveryStatus.Late -> stringResource(R.string.delivery_late)
      ActivityDeliveryStatus.Missing -> stringResource(R.string.delivery_missing)
    }

/**
 * Mapeia o tipo de necessidade pedagógica para rótulo de exibição.
 *
 * @param type Tipo cadastrado da necessidade.
 * @return Texto amigável para a UI.
 */
@Composable
private fun pedagogicalTypeLabel(type: PedagogicalNeedType): String =
    when (type) {
      PedagogicalNeedType.Report -> stringResource(R.string.pedagogical_type_report)
      PedagogicalNeedType.MedicalCertificate -> stringResource(R.string.pedagogical_type_medical)
      PedagogicalNeedType.SpecialNeed -> stringResource(R.string.pedagogical_type_special)
    }

/**
 * Mapeia o nível de confidencialidade psicológica para rótulo de exibição.
 *
 * @param level Nível de confidencialidade do registro.
 * @return Texto amigável para a UI.
 */
@Composable
private fun confidentialityLabel(level: ConfidentialityLevel): String =
    when (level) {
      ConfidentialityLevel.Restricted -> stringResource(R.string.confidentiality_restricted)
      ConfidentialityLevel.SharedSummary -> stringResource(R.string.confidentiality_shared)
    }

/**
 * Mapeia o canal de contato com responsáveis para rótulo de exibição.
 *
 * @param channel Canal utilizado no contato.
 * @return Texto amigável para a UI.
 */
@Composable
private fun channelLabel(channel: ParentContactChannel): String =
    when (channel) {
      ParentContactChannel.Meeting -> stringResource(R.string.channel_meeting)
      ParentContactChannel.Phone -> stringResource(R.string.channel_phone)
      ParentContactChannel.Message -> stringResource(R.string.channel_message)
      ParentContactChannel.Email -> stringResource(R.string.channel_email)
    }

/**
 * Mapeia o status do acompanhamento familiar para rótulo de exibição.
 *
 * @param status Situação atual do contato com os responsáveis.
 * @return Texto amigável para a UI.
 */
@Composable
private fun followUpStatusLabel(status: ParentFollowUpStatus): String =
    when (status) {
      ParentFollowUpStatus.Pending -> stringResource(R.string.follow_up_pending)
      ParentFollowUpStatus.WaitingResponse -> stringResource(R.string.follow_up_waiting)
      ParentFollowUpStatus.Completed -> stringResource(R.string.follow_up_completed)
      ParentFollowUpStatus.NoShow -> stringResource(R.string.follow_up_no_show)
    }

/** Pré-visualização da tela de acompanhamento de aluno. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudentMonitoringPreview() {
  AppDesafioSEBRAETheme { StudentMonitoringScreen(studentId = 1) }
}
