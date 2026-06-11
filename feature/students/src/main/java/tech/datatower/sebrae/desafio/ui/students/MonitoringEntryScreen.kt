/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /feature/students/src/main/java/tech/datatower/sebrae/desafio/ui/students/MonitoringEntryScreen.kt
    Descrição: Tela de registro de entrada de monitoramento individual do aluno.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.ActivityDeliveryStatus
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.AttendanceStatus
import tech.datatower.sebrae.desafio.data.model.ConfidentialityLevel
import tech.datatower.sebrae.desafio.data.model.ParentContactChannel
import tech.datatower.sebrae.desafio.data.model.ParentFollowUpStatus
import tech.datatower.sebrae.desafio.data.model.PedagogicalNeedType
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/** Tipos de registro de acompanhamento do aluno. */
enum class MonitoringEntryType(val label: String) {
  ATTENDANCE("Frequência"),
  BEHAVIOR("Comportamento"),
  PEDAGOGICAL("Pedagógico"),
  PSYCHOLOGICAL("Psicológico"),
  PARENT_FOLLOWUP("Responsáveis"),
}

/**
 * Tela de lançamento de informações de acompanhamento do aluno.
 *
 * Acessível por Orientador Educacional, Psicopedagogo, Coordenador, Administrador e Professor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringEntryScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
) {
  val viewModel: MonitoringEntryViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val snapshot by viewModel.snapshot.collectAsState()

  val canCreate =
      AccessPolicy.can(
          currentUser?.role,
          ProtectedResource.StudentMonitoring,
          ProtectedAction.Create,
      )

  var selectedType by rememberSaveable { mutableStateOf(MonitoringEntryType.ATTENDANCE.name) }
  val entryType =
      MonitoringEntryType.entries.firstOrNull { it.name == selectedType }
          ?: MonitoringEntryType.ATTENDANCE

  // Attendance fields
  var attendanceStatus by rememberSaveable { mutableStateOf(AttendanceStatus.Present.name) }
  var minutesLate by rememberSaveable { mutableStateOf("0") }
  var justification by rememberSaveable { mutableStateOf("") }

  // Behavior fields
  var participationScore by rememberSaveable { mutableIntStateOf(3) }
  var deliveryStatus by rememberSaveable { mutableStateOf(ActivityDeliveryStatus.OnTime.name) }
  var delayMinutes by rememberSaveable { mutableStateOf("0") }
  var grade by rememberSaveable { mutableStateOf("") }
  var behaviorNote by rememberSaveable { mutableStateOf("") }

  // Pedagogical fields
  var pedagogicalType by rememberSaveable { mutableStateOf(PedagogicalNeedType.Report.name) }
  var pedagogicalDescription by rememberSaveable { mutableStateOf("") }
  var pedagogicalAccommodations by rememberSaveable { mutableStateOf("") }

  // Psychological fields
  var psychologicalSummary by rememberSaveable { mutableStateOf("") }
  var confidentiality by rememberSaveable { mutableStateOf(ConfidentialityLevel.Restricted.name) }
  var nextStep by rememberSaveable { mutableStateOf("") }

  // Parent follow-up fields
  var parentChannel by rememberSaveable { mutableStateOf(ParentContactChannel.Meeting.name) }
  var parentOutcome by rememberSaveable { mutableStateOf(ParentFollowUpStatus.Pending.name) }
  var parentResponsible by rememberSaveable { mutableStateOf("") }
  var parentNotes by rememberSaveable { mutableStateOf("") }

  // General notes
  var notes by rememberSaveable { mutableStateOf("") }

  val studentName = snapshot?.studentName ?: "Aluno"

  DetailScaffold(
      title = stringResource(R.string.monitoring_entry_title),
      onBack = onBack,
  ) { innerPadding, _ ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { SnackbarHost(hostState = snackbarHostState) }

      if (!canCreate) {
        item {
          ElevatedCard(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.elevatedCardColors(),
          ) {
            Text(
                text = "Você não tem permissão para registrar acompanhamento.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
        return@LazyColumn
      }

      // Student header
      item {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = studentName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            snapshot?.enrolledClass?.let { enrolledClass ->
              Text(
                  text = "Turma: $enrolledClass",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }

      // Type selector
      item {
        Text(
            text = stringResource(R.string.monitoring_entry_select_type),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }
      item {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MonitoringEntryType.entries.forEach { type ->
            val isSelected = selectedType == type.name
            if (isSelected) {
              ElevatedButton(
                  onClick = { /* já selecionado */ },
                  modifier = Modifier.fillMaxWidth(),
                  colors =
                      ButtonDefaults.elevatedButtonColors(
                          containerColor = MaterialTheme.colorScheme.primary,
                          contentColor = MaterialTheme.colorScheme.onPrimary,
                      ),
              ) {
                Text(type.label, style = MaterialTheme.typography.labelLarge)
              }
            } else {
              OutlinedButton(
                  onClick = { selectedType = type.name },
                  modifier = Modifier.fillMaxWidth(),
              ) {
                Text(type.label, style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }

      // Form based on type
      item {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(),
        ) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            when (entryType) {
              MonitoringEntryType.ATTENDANCE -> {
                Text(
                    text = stringResource(R.string.monitoring_entry_attendance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Status", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  AttendanceStatus.entries.forEach { status ->
                    FilterChip(
                        selected = attendanceStatus == status.name,
                        onClick = { attendanceStatus = status.name },
                        label = {
                          Text(
                              when (status) {
                                AttendanceStatus.Present -> "Presente"
                                AttendanceStatus.Absent -> "Falta"
                                AttendanceStatus.JustifiedAbsence -> "Justificada"
                                AttendanceStatus.Late -> "Atraso"
                              },
                              style = MaterialTheme.typography.labelSmall,
                          )
                        },
                    )
                  }
                }
                if (attendanceStatus == AttendanceStatus.Late.name) {
                  OutlinedTextField(
                      value = minutesLate,
                      onValueChange = { minutesLate = it },
                      label = { Text("Minutos de atraso") },
                      modifier = Modifier.fillMaxWidth(),
                      singleLine = true,
                  )
                }
                if (attendanceStatus == AttendanceStatus.JustifiedAbsence.name) {
                  OutlinedTextField(
                      value = justification,
                      onValueChange = { justification = it },
                      label = { Text("Justificativa") },
                      modifier = Modifier.fillMaxWidth(),
                      minLines = 2,
                  )
                }
              }

              MonitoringEntryType.BEHAVIOR -> {
                Text(
                    text = stringResource(R.string.monitoring_entry_behavior),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Participação (1-5)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  (1..5).forEach { score ->
                    FilterChip(
                        selected = participationScore == score,
                        onClick = { participationScore = score },
                        label = { Text("$score") },
                    )
                  }
                }
                Text("Entrega de atividade", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  ActivityDeliveryStatus.entries.forEach { status ->
                    FilterChip(
                        selected = deliveryStatus == status.name,
                        onClick = { deliveryStatus = status.name },
                        label = {
                          Text(
                              when (status) {
                                ActivityDeliveryStatus.OnTime -> "No prazo"
                                ActivityDeliveryStatus.Late -> "Atrasada"
                                ActivityDeliveryStatus.Missing -> "Não entregue"
                              },
                              style = MaterialTheme.typography.labelSmall,
                          )
                        },
                    )
                  }
                }
                if (deliveryStatus == ActivityDeliveryStatus.Late.name) {
                  OutlinedTextField(
                      value = delayMinutes,
                      onValueChange = { delayMinutes = it },
                      label = { Text("Minutos de atraso") },
                      modifier = Modifier.fillMaxWidth(),
                      singleLine = true,
                  )
                }
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("Nota (0-10)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = behaviorNote,
                    onValueChange = { behaviorNote = it },
                    label = { Text(stringResource(R.string.monitoring_entry_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
              }

              MonitoringEntryType.PEDAGOGICAL -> {
                Text(
                    text = stringResource(R.string.monitoring_entry_pedagogical),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  PedagogicalNeedType.entries.forEach { type ->
                    FilterChip(
                        selected = pedagogicalType == type.name,
                        onClick = { pedagogicalType = type.name },
                        label = {
                          Text(
                              when (type) {
                                PedagogicalNeedType.Report -> "Laudo"
                                PedagogicalNeedType.MedicalCertificate -> "Atestado"
                                PedagogicalNeedType.SpecialNeed -> "Necessidade especial"
                              },
                              style = MaterialTheme.typography.labelSmall,
                          )
                        },
                    )
                  }
                }
                OutlinedTextField(
                    value = pedagogicalDescription,
                    onValueChange = { pedagogicalDescription = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = pedagogicalAccommodations,
                    onValueChange = { pedagogicalAccommodations = it },
                    label = { Text("Adaptações (separadas por vírgula)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
              }

              MonitoringEntryType.PSYCHOLOGICAL -> {
                Text(
                    text = stringResource(R.string.monitoring_entry_psychological),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = psychologicalSummary,
                    onValueChange = { psychologicalSummary = it },
                    label = { Text("Resumo") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Text("Nível de sigilo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  ConfidentialityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = confidentiality == level.name,
                        onClick = { confidentiality = level.name },
                        label = {
                          Text(
                              when (level) {
                                ConfidentialityLevel.Restricted -> "Restrito"
                                ConfidentialityLevel.SharedSummary -> "Resumo compartilhado"
                              },
                              style = MaterialTheme.typography.labelSmall,
                          )
                        },
                    )
                  }
                }
                OutlinedTextField(
                    value = nextStep,
                    onValueChange = { nextStep = it },
                    label = { Text("Próximo passo") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
              }

              MonitoringEntryType.PARENT_FOLLOWUP -> {
                Text(
                    text = stringResource(R.string.monitoring_entry_parent_followup),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Canal de contato", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  ParentContactChannel.entries.forEach { channel ->
                    FilterChip(
                        selected = parentChannel == channel.name,
                        onClick = { parentChannel = channel.name },
                        label = {
                          Text(
                              when (channel) {
                                ParentContactChannel.Meeting -> "Reunião"
                                ParentContactChannel.Phone -> "Telefone"
                                ParentContactChannel.Message -> "Mensagem"
                                ParentContactChannel.Email -> "E-mail"
                              },
                              style = MaterialTheme.typography.labelSmall,
                          )
                        },
                    )
                  }
                }
                Text("Resultado", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  ParentFollowUpStatus.entries.forEach { outcome ->
                    FilterChip(
                        selected = parentOutcome == outcome.name,
                        onClick = { parentOutcome = outcome.name },
                        label = {
                          Text(
                              when (outcome) {
                                ParentFollowUpStatus.Pending -> "Pendente"
                                ParentFollowUpStatus.WaitingResponse -> "Aguardando"
                                ParentFollowUpStatus.Completed -> "Concluído"
                                ParentFollowUpStatus.NoShow -> "Não compareceu"
                              },
                              style = MaterialTheme.typography.labelSmall,
                          )
                        },
                    )
                  }
                }
                OutlinedTextField(
                    value = parentResponsible,
                    onValueChange = { parentResponsible = it },
                    label = { Text("Responsável pelo registro") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = parentNotes,
                    onValueChange = { parentNotes = it },
                    label = { Text(stringResource(R.string.monitoring_entry_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
              }
            }
          }
        }
      }

      // Save button
      item {
        Button(
            onClick = {
              scope.launch {
                val result =
                    viewModel.saveEntry(
                        entryType = entryType,
                        // Attendance
                        attendanceStatus =
                            runCatching { AttendanceStatus.valueOf(attendanceStatus) }
                                .getOrDefault(AttendanceStatus.Present),
                        minutesLate = minutesLate.toIntOrNull() ?: 0,
                        justification = justification.ifBlank { null },
                        // Behavior
                        participationScore = participationScore,
                        deliveryStatus =
                            runCatching { ActivityDeliveryStatus.valueOf(deliveryStatus) }
                                .getOrDefault(ActivityDeliveryStatus.OnTime),
                        delayMinutes = delayMinutes.toIntOrNull() ?: 0,
                        grade = grade.toFloatOrNull(),
                        behaviorNote = behaviorNote,
                        // Pedagogical
                        pedagogicalType =
                            runCatching { PedagogicalNeedType.valueOf(pedagogicalType) }
                                .getOrDefault(PedagogicalNeedType.Report),
                        pedagogicalDescription = pedagogicalDescription,
                        pedagogicalAccommodations =
                            pedagogicalAccommodations
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() },
                        // Psychological
                        psychologicalSummary = psychologicalSummary,
                        confidentiality =
                            runCatching { ConfidentialityLevel.valueOf(confidentiality) }
                                .getOrDefault(ConfidentialityLevel.Restricted),
                        nextStep = nextStep,
                        // Parent follow-up
                        parentChannel =
                            runCatching { ParentContactChannel.valueOf(parentChannel) }
                                .getOrDefault(ParentContactChannel.Meeting),
                        parentOutcome =
                            runCatching { ParentFollowUpStatus.valueOf(parentOutcome) }
                                .getOrDefault(ParentFollowUpStatus.Pending),
                        parentResponsible = parentResponsible,
                        parentNotes = parentNotes,
                    )
                if (result) {
                  snackbarHostState.showSnackbar("Registro salvo com sucesso.")
                  onBack()
                } else {
                  snackbarHostState.showSnackbar("Não foi possível salvar o registro.")
                }
              }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(R.string.monitoring_entry_save))
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}
