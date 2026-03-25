package tech.datatower.sebrae.desafio.ui.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataService = remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val courses by repository.observeCourses().collectAsState(initial = emptyList())

  var title by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("") }
  var instructor by rememberSaveable { mutableStateOf("") }
  var totalStudents by rememberSaveable { mutableStateOf("0") }
  var durationHours by rememberSaveable { mutableStateOf("0") }
  var completionRatePercent by rememberSaveable { mutableStateOf("0") }
  var isPublished by rememberSaveable { mutableStateOf(true) }

  DetailScaffold(title = "Novo curso", onBack = onBack) { innerPadding, _ ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SnackbarHost(hostState = snackbarHostState)

      OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Titulo") },
          singleLine = true,
      )
      OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Categoria") },
          singleLine = true,
      )
      OutlinedTextField(
          value = instructor,
          onValueChange = { instructor = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Instrutor") },
          singleLine = true,
      )
      OutlinedTextField(
          value = totalStudents,
          onValueChange = { totalStudents = it.filter(Char::isDigit) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Qtd. alunos") },
          singleLine = true,
      )
      OutlinedTextField(
          value = durationHours,
          onValueChange = { durationHours = it.filter(Char::isDigit) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Carga horaria (h)") },
          singleLine = true,
      )
      OutlinedTextField(
          value = completionRatePercent,
          onValueChange = { completionRatePercent = it.filter(Char::isDigit) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Taxa de conclusao (%)") },
          singleLine = true,
      )

      Text(text = "Publicado", style = MaterialTheme.typography.labelLarge)
      FilterChip(selected = isPublished, onClick = { isPublished = true }, label = { Text("Sim") })
      FilterChip(selected = !isPublished, onClick = { isPublished = false }, label = { Text("Nao") })

      Button(
          onClick = {
            val students = totalStudents.toIntOrNull()
            val hours = durationHours.toIntOrNull()
            val completion = completionRatePercent.toIntOrNull()

            if (title.isBlank() || category.isBlank() || instructor.isBlank()) {
              scope.launch { snackbarHostState.showSnackbar("Preencha titulo, categoria e instrutor.") }
              return@Button
            }
            if (students == null || hours == null || completion == null || completion !in 0..100) {
              scope.launch { snackbarHostState.showSnackbar("Revise os campos numericos.") }
              return@Button
            }

            scope.launch {
              val nextId = (courses.maxOfOrNull { it.id } ?: 0) + 1
              val result =
                  dataService.upsertCourse(
                      Course(
                          id = nextId,
                          title = title.trim(),
                          category = category.trim(),
                          instructor = instructor.trim(),
                          totalStudents = students,
                          durationHours = hours,
                          completionRate = completion / 100f,
                          isPublished = isPublished,
                      )
                  )
              if (result is tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService.Result.Success) {
                onBack()
              } else if (result is tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService.Result.Error) {
                snackbarHostState.showSnackbar(result.message.ifBlank { "Falha ao salvar curso." })
              }
            }
          },
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      ) {
        Text("Salvar curso")
      }
    }
  }
}

