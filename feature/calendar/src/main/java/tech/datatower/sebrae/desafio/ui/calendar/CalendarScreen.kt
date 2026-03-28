/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/calendar/CalendarScreen.kt
    Descrição: Tela de calendário educacional, exibindo e permitindo a criação de eventos por data.
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
package tech.datatower.sebrae.desafio.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de calendário com eventos acadêmicos e institucionais agrupados por data.
 *
 * @param onBack Ação executada ao retornar para a tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(currentUser: AppUser? = null, onBack: () -> Unit = {}) {
  val viewModel: CalendarViewModel = hiltViewModel()
  val events by viewModel.events.collectAsState()
  val grouped by viewModel.groupedEvents.collectAsState()
  val actionResult by viewModel.actionResult.collectAsState()
  val listState = rememberLazyListState()
  val snackbarHostState = remember { SnackbarHostState() }
  var showCreateSheet by rememberSaveable { mutableStateOf(false) }
  val createErrorFallback = stringResource(R.string.calendar_create_error)
  val canCreateCalendarEvent =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Calendar, ProtectedAction.Create)

  LaunchedEffect(actionResult) {
    if (actionResult is CalendarViewModel.ActionResult.Error) {
      snackbarHostState.showSnackbar(createErrorFallback)
      viewModel.clearActionResult()
    }
  }

  DetailScaffold(title = stringResource(R.string.calendar_title), onBack = onBack) { innerPadding, _
    ->
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
          if (canCreateCalendarEvent) {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
              Icon(
                  imageVector = Icons.Outlined.Add,
                  contentDescription = stringResource(R.string.calendar_add_content_description),
              )
            }
          }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { fabPadding ->
      LazyColumn(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(fabPadding),
          state = listState,
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        grouped.forEach { (date, dayEvents) ->
          item(key = "header_$date", contentType = "header") {
            Text(
                text = date,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
          }
          items(
              items = dayEvents,
              key = { it.id },
              contentType = { "event" },
          ) { event ->
            EventCard(event)
            Spacer(modifier = Modifier.height(6.dp))
          }
        }
      }

      if (showCreateSheet) {
        CalendarEventCreateCard(
            onDismiss = { showCreateSheet = false },
            onSave = { evtTitle, evtCourse, evtDate, evtTime, evtLocation, evtType ->
              viewModel.createEvent(
                  currentUser,
                  evtTitle,
                  evtCourse,
                  evtDate,
                  evtTime,
                  evtLocation,
                  evtType,
              )
              showCreateSheet = false
            },
        )
      }
    }
  }
}

/** Formulário compacto para lançamento de reuniões, aulas e eventos no calendário. */
@Composable
private fun CalendarEventCreateCard(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, EventType) -> Unit,
) {
  var title by rememberSaveable { mutableStateOf("") }
  var course by rememberSaveable { mutableStateOf("Institutional") }
  var date by rememberSaveable { mutableStateOf("") }
  var time by rememberSaveable { mutableStateOf("") }
  var location by rememberSaveable { mutableStateOf("") }
  var type by rememberSaveable { mutableStateOf(EventType.Class) }

  ElevatedCard(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(stringResource(R.string.calendar_form_title)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = course,
          onValueChange = { course = it },
          label = { Text(stringResource(R.string.calendar_form_context)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text(stringResource(R.string.calendar_form_date)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = time,
          onValueChange = { time = it },
          label = { Text(stringResource(R.string.calendar_form_time)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text(stringResource(R.string.calendar_form_location)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )

      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = type == EventType.Class,
            onClick = { type = EventType.Class },
            label = { Text(stringResource(R.string.event_type_class)) },
        )
        FilterChip(
            selected = type == EventType.Meeting,
            onClick = { type = EventType.Meeting },
            label = { Text(stringResource(R.string.event_type_meeting)) },
        )
        FilterChip(
            selected = type == EventType.Other,
            onClick = { type = EventType.Other },
            label = { Text(stringResource(R.string.calendar_event_type_event)) },
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.calendar_form_cancel),
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { onDismiss() },
        )
        Text(
            text = stringResource(R.string.calendar_form_save),
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(
                        enabled = title.isNotBlank() && date.isNotBlank() && time.isNotBlank()
                    ) {
                      onSave(
                          title.trim(),
                          course.trim(),
                          date.trim(),
                          time.trim(),
                          location.trim(),
                          type,
                      )
                    },
            color = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
  }
}

/**
 * Cartão visual para exibição de um evento de calendário.
 *
 * @param event Evento renderizado no item da lista.
 */
@Composable
private fun EventCard(event: CalendarEvent) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      // Color dot by event type
      val dotColor: Color =
          when (event.type) {
            EventType.Class -> MaterialTheme.colorScheme.primary
            EventType.Exam -> MaterialTheme.colorScheme.error
            EventType.Meeting -> MaterialTheme.colorScheme.tertiary
            EventType.Other -> MaterialTheme.colorScheme.secondary
          }
      Box(
          modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor),
      )
      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = event.course,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
              text = event.time,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
              text = event.location,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      StatusChip(
          label =
              when (event.type) {
                EventType.Class -> stringResource(R.string.event_type_class)
                EventType.Exam -> stringResource(R.string.event_type_exam)
                EventType.Meeting -> stringResource(R.string.event_type_meeting)
                EventType.Other -> stringResource(R.string.event_type_other)
              },
          containerColor =
              when (event.type) {
                EventType.Class -> MaterialTheme.colorScheme.primaryContainer
                EventType.Exam -> MaterialTheme.colorScheme.errorContainer
                EventType.Meeting -> MaterialTheme.colorScheme.tertiaryContainer
                EventType.Other -> MaterialTheme.colorScheme.secondaryContainer
              },
          contentColor =
              when (event.type) {
                EventType.Class -> MaterialTheme.colorScheme.onPrimaryContainer
                EventType.Exam -> MaterialTheme.colorScheme.onErrorContainer
                EventType.Meeting -> MaterialTheme.colorScheme.onTertiaryContainer
                EventType.Other -> MaterialTheme.colorScheme.onSecondaryContainer
              },
      )
    }
  }
}

/** Pré-visualização da tela de calendário para desenvolvimento. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalendarPreview() {
  AppDesafioSEBRAETheme { CalendarScreen() }
}
