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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit = {}) {
  val events = remember { sampleEvents() }
  val listState = rememberLazyListState()
  val grouped = remember(events) { events.groupBy { it.date }.entries.toList() }

  DetailScaffold(title = "Calendário", onBack = onBack) { innerPadding, _ ->
    Scaffold(
        floatingActionButton = {
          FloatingActionButton(
              onClick = {},
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = "Novo evento")
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
                EventType.Class -> "Aula"
                EventType.Exam -> "Prova"
                EventType.Meeting -> "Reunião"
                EventType.Other -> "Outro"
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

private fun sampleEvents() =
    listOf(
        CalendarEvent(
            1,
            "Empreendedorismo — Turma C2",
            "Empreendedorismo",
            "Sex, 07 Mar",
            "08h–12h",
            "Sala 3",
            EventType.Class,
        ),
        CalendarEvent(
            2,
            "Reunião de Coordenação",
            "Institucional",
            "Sex, 07 Mar",
            "14h–15h",
            "Sala de Reuniões",
            EventType.Meeting,
        ),
        CalendarEvent(
            3,
            "Marketing Digital — Turma A1",
            "Marketing Digital",
            "Seg, 10 Mar",
            "18h–20h",
            "Sala 1",
            EventType.Class,
        ),
        CalendarEvent(
            4,
            "Excel para Negócios — Turma B3",
            "Excel p/ Negócios",
            "Ter, 11 Mar",
            "19h–21h",
            "Lab de TI",
            EventType.Class,
        ),
        CalendarEvent(
            5,
            "Avaliação Final — Turma A1",
            "Marketing Digital",
            "Qua, 12 Mar",
            "18h–20h",
            "Sala 1",
            EventType.Exam,
        ),
        CalendarEvent(
            6,
            "Design Gráfico — Turma D1",
            "Design Gráfico",
            "Qua, 12 Mar",
            "14h–16h",
            "Sala 2",
            EventType.Class,
        ),
        CalendarEvent(
            7,
            "Excel para Negócios — Turma B3",
            "Excel p/ Negócios",
            "Qui, 13 Mar",
            "19h–21h",
            "Lab de TI",
            EventType.Class,
        ),
        CalendarEvent(
            8,
            "Conselho de Instrutores",
            "Institucional",
            "Qui, 13 Mar",
            "10h–11h30",
            "Auditório",
            EventType.Meeting,
        ),
        CalendarEvent(
            9,
            "Finanças Pessoais — Turma A2",
            "Finanças Pessoais",
            "Sáb, 15 Mar",
            "09h–12h",
            "Sala 4",
            EventType.Class,
        ),
        CalendarEvent(
            10,
            "Cerimônia de Formatura",
            "Institucional",
            "Sáb, 15 Mar",
            "16h–19h",
            "Auditório",
            EventType.Other,
        ),
    )

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalendarPreview() {
  AppDesafioSEBRAETheme { CalendarScreen() }
}
