package tech.datatower.sebrae.desafio.ui.teachers

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/**
 * Executa a rotina de teacher detail screen dentro do contexto deste componente.
 *
 * @param teacherId Valor de entrada utilizado por esta opera??o.
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDetailScreen(
    teacherId: Int,
    onBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataConnectService =
      remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val teacher by repository.observeTeacherById(teacherId).collectAsState(initial = null)
  val classesFlow =
      remember(teacher?.name) {
        val teacherName = teacher?.name
        if (teacherName == null) flowOf(emptyList())
        else repository.observeClassesByTeacher(teacherName)
      }
  val classes by classesFlow.collectAsState(initial = emptyList())

  LaunchedEffect(teacherId) { dataConnectService.syncScope(ScreenDataScope.TEACHER_DETAIL) }

  DetailScaffold(title = stringResource(R.string.teacher_detail_title), onBack = onBack) {
      innerPadding,
      _ ->
    if (teacher == null) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
        Text(stringResource(R.string.teacher_not_found), style = MaterialTheme.typography.bodyLarge)
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
                teacher!!.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(teacher!!.specialty, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.label_email, teacher!!.email),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.label_active_courses_count, teacher!!.activeCourses),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.label_total_students_count, teacher!!.totalStudents),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.label_rating_value, teacher!!.rating),
                style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }

      item {
        Text(
            text = stringResource(R.string.label_taught_classes, classes.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }

      if (classes.isEmpty()) {
        item {
          Text(
              stringResource(R.string.no_linked_classes),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
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
              Text(
                  stringResource(R.string.label_course, schoolClass.course),
                  style = MaterialTheme.typography.bodySmall,
              )
              Text(
                  stringResource(R.string.label_schedule, schoolClass.schedule),
                  style = MaterialTheme.typography.bodySmall,
              )
              Text(
                  stringResource(
                      R.string.label_students_count,
                      schoolClass.studentsCount,
                      schoolClass.maxCapacity,
                  ),
                  style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}
