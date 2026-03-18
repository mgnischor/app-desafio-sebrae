package tech.datatower.sebrae.desafio.ui.courses

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(onBack: () -> Unit = {}) {
  val allCourses = remember { sampleCourses() }
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  val filtered by
      remember(query, allCourses) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allCourses
          } else {
            allCourses.filter {
              it.title.contains(normalizedQuery, ignoreCase = true) ||
                  it.category.contains(normalizedQuery, ignoreCase = true) ||
                  it.instructor.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  DetailScaffold(title = "Cursos", onBack = onBack) { innerPadding, _ ->
    Scaffold(
        floatingActionButton = {
          FloatingActionButton(
              onClick = {},
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = "Novo curso")
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
              placeholder = "Buscar curso, categoria ou instrutor...",
              resultCount = filtered.size,
              resultLabel = "cursos encontrados",
          )
        }

        if (filtered.isEmpty()) {
          item {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                message = "Nenhum curso encontrado.",
            )
          }
        } else {
          items(
              items = filtered,
              key = { it.id },
              contentType = { "course" },
          ) { course ->
            CourseCard(course = course)
          }
        }
      }
    }
  }
}

@Composable
private fun CourseCard(course: Course) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = course.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text = course.category,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusChip(
            label = if (course.isPublished) "Publicado" else "Rascunho",
            containerColor =
                if (course.isPublished) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor =
                if (course.isPublished) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

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
              text = "${course.totalStudents} alunos",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Outlined.Timer,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = "${course.durationHours}h",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
            text = "Instrutor: ${course.instructor}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { course.completionRate },
            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.tertiaryContainer,
            strokeCap = StrokeCap.Round,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(course.completionRate * 100).toInt()}% conclusão",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
  }
}

private fun sampleCourses() =
    listOf(
        Course(1, "Marketing Digital", "Negócios", "Profa. Helena", 320, 40, 0.82f, true),
        Course(2, "Excel para Negócios", "Tecnologia", "Prof. André", 210, 20, 0.60f, true),
        Course(3, "Empreendedorismo", "Gestão", "Profa. Carla", 180, 30, 0.91f, true),
        Course(4, "Finanças Pessoais", "Finanças", "Prof. Roberto", 95, 16, 0.45f, false),
        Course(5, "Design Gráfico", "Criatividade", "Profa. Bianca", 140, 24, 0.70f, true),
        Course(6, "Vendas e Negociação", "Negócios", "Prof. Sérgio", 260, 12, 0.55f, true),
    )

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CoursesPreview() {
  AppDesafioSEBRAETheme { CoursesScreen() }
}
