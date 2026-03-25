package tech.datatower.sebrae.desafio.ui.classes

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: Int,
    onBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val schoolClass by repository.observeClassById(classId).collectAsState(initial = null)
  val studentsFlow =
      remember(schoolClass?.name) {
        val className = schoolClass?.name
        if (className == null) flowOf(emptyList()) else repository.observeStudentsByClass(className)
      }
  val students by studentsFlow.collectAsState(initial = emptyList())

  DetailScaffold(title = stringResource(R.string.class_detail_title), onBack = onBack) { innerPadding, _ ->
    if (schoolClass == null) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
        Text(stringResource(R.string.class_not_found), style = MaterialTheme.typography.bodyLarge)
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
                schoolClass!!.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.label_course, schoolClass!!.course), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.label_instructor, schoolClass!!.instructor),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.label_schedule, schoolClass!!.schedule), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.label_occupation, schoolClass!!.studentsCount, schoolClass!!.maxCapacity),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.status_label, classStatusLabel(schoolClass!!.status)), style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      item {
        Text(
            text = stringResource(R.string.label_class_students, students.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }

      if (students.isEmpty()) {
        item { Text(stringResource(R.string.no_class_students), style = MaterialTheme.typography.bodyMedium) }
      } else {
        items(students, key = { it.id }) { student ->
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
                  student.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Medium,
              )
              Text(student.email, style = MaterialTheme.typography.bodySmall)
              Text(
                  stringResource(R.string.label_progress, (student.progress * 100).toInt()),
                  style = MaterialTheme.typography.bodySmall,
              )
              Text(stringResource(R.string.status_label, studentStatusLabel(student.status)), style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun classStatusLabel(status: ClassStatus): String =
    when (status) {
      ClassStatus.Open -> stringResource(R.string.class_status_open)
      ClassStatus.InProgress -> stringResource(R.string.class_status_in_progress)
      ClassStatus.Closed -> stringResource(R.string.class_status_closed)
    }

@Composable
private fun studentStatusLabel(status: StudentStatus): String =
    when (status) {
      StudentStatus.Active -> stringResource(R.string.status_active)
      StudentStatus.Inactive -> stringResource(R.string.status_inactive)
      StudentStatus.Graduated -> stringResource(R.string.status_graduated)
    }

