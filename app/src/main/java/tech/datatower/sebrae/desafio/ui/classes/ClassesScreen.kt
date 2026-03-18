package tech.datatower.sebrae.desafio.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de acompanhamento de turmas.
 *
 * Suporta filtro por nome da turma, curso e instrutor, além de exibir status e ocupação de vagas
 * por turma.
 *
 * @param onBack Ação de navegação para retornar à tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(
    onBack: () -> Unit = {},
    onOpenClassDetail: (Int) -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val allClasses by repository.observeClasses().collectAsState(initial = emptyList())
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  val filtered by
      remember(query, allClasses) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allClasses
          } else {
            allClasses.filter {
              it.name.contains(normalizedQuery, ignoreCase = true) ||
                  it.course.contains(normalizedQuery, ignoreCase = true) ||
                  it.instructor.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  DetailScaffold(title = "Turmas", onBack = onBack) { innerPadding, _ ->
    Scaffold(
        floatingActionButton = {
          FloatingActionButton(
              onClick = {},
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = "Nova turma")
          }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { fabPadding ->
      LazyColumn(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(fabPadding),
          state = listState,
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          ListSearchHeader(
              query = query,
              onQueryChange = { query = it },
              placeholder = "Buscar turma, curso ou instrutor...",
              resultCount = filtered.size,
              resultLabel = "turmas encontradas",
          )
        }

        if (filtered.isEmpty()) {
          item { EmptyState(icon = Icons.Outlined.Group, message = "Nenhuma turma encontrada.") }
        } else {
          items(
              items = filtered,
              key = { it.id },
              contentType = { "class" },
          ) { sc ->
            ClassCard(sc = sc, onClick = { onOpenClassDetail(sc.id) })
          }
        }
      }
    }
  }
}

/**
 * Cartão com os dados principais de uma turma.
 *
 * @param sc Turma a ser representada no item da lista.
 */
@Composable
private fun ClassCard(
    sc: SchoolClass,
    onClick: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = sc.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        StatusChip(
            label =
                when (sc.status) {
                  ClassStatus.Open -> "Aberta"
                  ClassStatus.InProgress -> "Em andamento"
                  ClassStatus.Closed -> "Encerrada"
                },
            containerColor =
                when (sc.status) {
                  ClassStatus.Open -> MaterialTheme.colorScheme.tertiaryContainer
                  ClassStatus.InProgress -> MaterialTheme.colorScheme.primaryContainer
                  ClassStatus.Closed -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            contentColor =
                when (sc.status) {
                  ClassStatus.Open -> MaterialTheme.colorScheme.onTertiaryContainer
                  ClassStatus.InProgress -> MaterialTheme.colorScheme.onPrimaryContainer
                  ClassStatus.Closed -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
      }
      Text(
          text = sc.course,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Outlined.Group,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = "${sc.studentsCount}/${sc.maxCapacity} alunos",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
            text = sc.schedule,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
          text = "Instrutor: ${sc.instructor}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      val fill = sc.studentsCount.toFloat() / sc.maxCapacity.coerceAtLeast(1)
      LinearProgressIndicator(
          progress = { fill },
          modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
          color = MaterialTheme.colorScheme.secondary,
          trackColor = MaterialTheme.colorScheme.secondaryContainer,
          strokeCap = StrokeCap.Round,
      )
    }
  }
}

/**
 * Produz dados estáticos para composição e pré-visualização da tela.
 *
 * @return Lista fixa de turmas em diferentes estágios e ocupações.
 */
private fun sampleClasses() =
    listOf(
        SchoolClass(
            1,
            "Turma A1",
            "Marketing Digital",
            "Profa. Helena",
            28,
            30,
            "Seg/Qua 18h–20h",
            ClassStatus.InProgress,
        ),
        SchoolClass(
            2,
            "Turma B3",
            "Excel para Negócios",
            "Prof. André",
            22,
            25,
            "Ter/Qui 19h–21h",
            ClassStatus.InProgress,
        ),
        SchoolClass(
            3,
            "Turma C2",
            "Empreendedorismo",
            "Profa. Carla",
            18,
            20,
            "Sex 08h–12h",
            ClassStatus.Open,
        ),
        SchoolClass(
            4,
            "Turma A2",
            "Finanças Pessoais",
            "Prof. Roberto",
            10,
            20,
            "Sáb 09h–12h",
            ClassStatus.Open,
        ),
        SchoolClass(
            5,
            "Turma D1",
            "Design Gráfico",
            "Profa. Bianca",
            30,
            30,
            "Seg/Qua 14h–16h",
            ClassStatus.Closed,
        ),
    )

/** Pré-visualização da tela de turmas para validação visual. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ClassesPreview() {
  AppDesafioSEBRAETheme { ClassesScreen() }
}
