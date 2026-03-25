package tech.datatower.sebrae.desafio.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.data.repository.AppGraph
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
fun CalendarScreen(onBack: () -> Unit = {}) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val events by repository.observeCalendarEvents().collectAsState(initial = emptyList())
  val listState = rememberLazyListState()
  val grouped = remember(events) { events.groupBy { it.date }.entries.toList() }

  DetailScaffold(title = stringResource(R.string.calendar_title), onBack = onBack) { innerPadding, _
    ->
    Scaffold(
        floatingActionButton = {
          FloatingActionButton(
              onClick = {},
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.calendar_add_content_description),
            )
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
