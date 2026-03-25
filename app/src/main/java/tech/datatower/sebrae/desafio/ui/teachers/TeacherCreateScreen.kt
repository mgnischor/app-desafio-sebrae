package tech.datatower.sebrae.desafio.ui.teachers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@Composable
fun TeacherCreateScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataService = remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val teachers by repository.observeTeachers().collectAsState(initial = emptyList())

  var name by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var specialty by rememberSaveable { mutableStateOf("") }
  var activeCourses by rememberSaveable { mutableStateOf("0") }
  var totalStudents by rememberSaveable { mutableStateOf("0") }
  var rating by rememberSaveable { mutableStateOf("0.0") }

  DetailScaffold(title = "Novo instrutor", onBack = onBack) { innerPadding, _ ->
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
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("E-mail") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = specialty,
          onValueChange = { specialty = it },
          label = { Text("Especialidade") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = activeCourses,
          onValueChange = { activeCourses = it.filter(Char::isDigit) },
          label = { Text("Cursos ativos") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = totalStudents,
          onValueChange = { totalStudents = it.filter(Char::isDigit) },
          label = { Text("Total de alunos") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = rating,
          onValueChange = { rating = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("Avaliacao (0.0 a 5.0)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )

      Button(
          onClick = {
            val active = activeCourses.toIntOrNull()
            val students = totalStudents.toIntOrNull()
            val score = rating.toFloatOrNull()

            if (name.isBlank() || email.isBlank() || specialty.isBlank()) {
              scope.launch { snackbarHostState.showSnackbar("Preencha os campos obrigatorios.") }
              return@Button
            }
            if (active == null || students == null || score == null || score !in 0f..5f) {
              scope.launch { snackbarHostState.showSnackbar("Revise os campos numericos.") }
              return@Button
            }

            scope.launch {
              val nextId = (teachers.maxOfOrNull { it.id } ?: 0) + 1
              when (
                  val result =
                      dataService.upsertTeacher(
                          Teacher(
                              id = nextId,
                              name = name.trim(),
                              email = email.trim(),
                              specialty = specialty.trim(),
                              activeCourses = active,
                              totalStudents = students,
                              rating = score,
                          )
                      )) {
                is FirebaseDataConnectService.Result.Success -> onBack()
                is FirebaseDataConnectService.Result.Error ->
                    snackbarHostState.showSnackbar(result.message.ifBlank { "Falha ao salvar instrutor." })
                FirebaseDataConnectService.Result.Loading -> Unit
              }
            }
          },
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      ) {
        Text("Salvar instrutor")
      }
    }
  }
}

