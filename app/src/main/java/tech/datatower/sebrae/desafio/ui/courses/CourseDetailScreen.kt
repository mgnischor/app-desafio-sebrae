package tech.datatower.sebrae.desafio.ui.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int,
    onBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val course by repository.observeCourseById(courseId).collectAsState(initial = null)
  val classesFlow =
      remember(course?.title) {
        val name = course?.title
        if (name == null) flowOf(emptyList()) else repository.observeClassesByCourse(name)
      }
  val classes by classesFlow.collectAsState(initial = emptyList())

  DetailScaffold(title = "Detalhe do Curso", onBack = onBack) { innerPadding, _ ->
    if (course == null) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
        Text("Curso não encontrado.", style = MaterialTheme.typography.bodyLarge)
      }
      return@DetailScaffold
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
                course!!.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(course!!.category, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Instrutor principal: ${course!!.instructor}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Carga horária: ${course!!.durationHours}h",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Matrículas: ${course!!.totalStudents}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Conclusão média: ${(course!!.completionRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (course!!.isPublished) "Status: Publicado" else "Status: Rascunho",
                style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }

      item {
        Text(
            text = "Turmas vinculadas (${classes.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }

      if (classes.isEmpty()) {
        item { Text("Nenhuma turma vinculada.", style = MaterialTheme.typography.bodyMedium) }
      } else {
        items(classes, key = { it.id }) { schoolClass ->
          ElevatedCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors =
                  CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
          ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                  schoolClass.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Medium,
              )
              Text("Horário: ${schoolClass.schedule}", style = MaterialTheme.typography.bodySmall)
              Text(
                  "Instrutor: ${schoolClass.instructor}",
                  style = MaterialTheme.typography.bodySmall,
              )
              Text(
                  "Alunos: ${schoolClass.studentsCount}/${schoolClass.maxCapacity}",
                  style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}
