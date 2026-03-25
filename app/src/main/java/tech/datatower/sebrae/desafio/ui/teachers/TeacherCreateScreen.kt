package tech.datatower.sebrae.desafio.ui.teachers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/**
 * Executa a rotina de teacher create screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
  val requiredFieldsMessage = stringResource(R.string.teacher_create_error_required)
  val invalidNumericMessage = stringResource(R.string.teacher_create_error_numeric)
  val saveSuccessMessage = stringResource(R.string.teacher_create_success)
  val saveErrorMessage = stringResource(R.string.teacher_create_error_save)

  DetailScaffold(title = stringResource(R.string.teacher_create_title), onBack = onBack) {
      innerPadding,
      _ ->
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
          label = { Text(stringResource(R.string.teacher_create_name)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(stringResource(R.string.teacher_create_email)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = specialty,
          onValueChange = { specialty = it },
          label = { Text(stringResource(R.string.teacher_create_specialty)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = activeCourses,
          onValueChange = { activeCourses = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.teacher_create_active_courses)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = totalStudents,
          onValueChange = { totalStudents = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.teacher_create_total_students)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = rating,
          onValueChange = { rating = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(stringResource(R.string.teacher_create_rating)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )

      Button(
          onClick = {
            val active = activeCourses.toIntOrNull()
            val students = totalStudents.toIntOrNull()
            val score = rating.toFloatOrNull()

            if (name.isBlank() || email.isBlank() || specialty.isBlank()) {
              scope.launch { snackbarHostState.showSnackbar(requiredFieldsMessage) }
              return@Button
            }
            if (active == null || students == null || score == null || score !in 0f..5f) {
              scope.launch { snackbarHostState.showSnackbar(invalidNumericMessage) }
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
                      )
              ) {
                is FirebaseDataConnectService.Result.Success -> {
                  snackbarHostState.showSnackbar(saveSuccessMessage)
                  onBack()
                }
                is FirebaseDataConnectService.Result.Error ->
                    snackbarHostState.showSnackbar(result.message.ifBlank { saveErrorMessage })
                FirebaseDataConnectService.Result.Loading -> Unit
              }
            }
          },
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      ) {
        Text(stringResource(R.string.teacher_create_save))
      }
    }
  }
}
