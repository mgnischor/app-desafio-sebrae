package tech.datatower.sebrae.desafio.ui.students

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de gerenciamento de alunos.
 *
 * Permite buscar por nome, curso ou turma e apresenta os resultados em lista otimizada com
 * `LazyColumn`.
 *
 * @param onBack Ação acionada ao retornar para a tela anterior.
 * @param onOpenStudentMonitoring Ação para abrir o acompanhamento detalhado do aluno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    onBack: () -> Unit = {},
    onOpenStudentMonitoring: (Int) -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val allStudents by repository.observeStudents().collectAsState(initial = emptyList())
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  val filtered by
      remember(query, allStudents) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allStudents
          } else {
            allStudents.filter {
              it.name.contains(normalizedQuery, ignoreCase = true) ||
                  it.course.contains(normalizedQuery, ignoreCase = true) ||
                  it.enrolledClass.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  DetailScaffold(title = stringResource(R.string.students_title), onBack = onBack) { innerPadding, _ ->
    Scaffold(
        floatingActionButton = {
          FloatingActionButton(
              onClick = {},
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.students_add_content_description)
            )
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
              placeholder = stringResource(R.string.students_search_placeholder),
              resultCount = filtered.size,
              resultLabel = stringResource(R.string.students_result_label),
          )
        }

        if (filtered.isEmpty()) {
          item {
            EmptyState(
                icon = Icons.Outlined.Person,
                message = stringResource(R.string.students_empty_state)
            )
          }
        } else {
          items(
              items = filtered,
              key = { it.id },
              contentType = { "student" },
          ) { student ->
            StudentCard(
                student = student,
                onClick = { onOpenStudentMonitoring(student.id) },
            )
          }
        }
      }
    }
  }
}

/**
 * Cartão de exibição resumida de um aluno.
 *
 * @param student Dados do aluno que serão renderizados no cartão.
 * @param onClick Ação de navegação para o detalhe de acompanhamento do aluno.
 */
@Composable
private fun StudentCard(
    student: Student,
    onClick: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
      // Avatar
      Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(48.dp),
      ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = student.name.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = student.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
          )
          StatusChip(
              label =
                  when (student.status) {
                    StudentStatus.Active -> stringResource(R.string.status_active)
                    StudentStatus.Inactive -> stringResource(R.string.status_inactive)
                    StudentStatus.Graduated -> stringResource(R.string.status_graduated)
                  },
              containerColor =
                  when (student.status) {
                    StudentStatus.Active -> MaterialTheme.colorScheme.primaryContainer
                    StudentStatus.Inactive -> MaterialTheme.colorScheme.errorContainer
                    StudentStatus.Graduated -> MaterialTheme.colorScheme.tertiaryContainer
                  },
              contentColor =
                  when (student.status) {
                    StudentStatus.Active -> MaterialTheme.colorScheme.onPrimaryContainer
                    StudentStatus.Inactive -> MaterialTheme.colorScheme.onErrorContainer
                    StudentStatus.Graduated -> MaterialTheme.colorScheme.onTertiaryContainer
                  },
          )
        }
        Text(
            text = "${student.course} · ${student.enrolledClass}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          LinearProgressIndicator(
              progress = { student.progress },
              modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.primaryContainer,
              strokeCap = StrokeCap.Round,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = "${(student.progress * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

/** Pré-visualização da tela de alunos no Android Studio/IntelliJ. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudentsPreview() {
  AppDesafioSEBRAETheme { StudentsScreen() }
}
