package tech.datatower.sebrae.desafio.ui.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

/**
 * Executa a rotina de class create screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassCreateScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataService = remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val classes by repository.observeClasses().collectAsState(initial = emptyList())

  var name by rememberSaveable { mutableStateOf("") }
  var course by rememberSaveable { mutableStateOf("") }
  var instructor by rememberSaveable { mutableStateOf("") }
  var studentsCount by rememberSaveable { mutableStateOf("0") }
  var maxCapacity by rememberSaveable { mutableStateOf("0") }
  var schedule by rememberSaveable { mutableStateOf("") }
  var status by rememberSaveable { mutableStateOf(ClassStatus.Open) }
  val requiredFieldsMessage = stringResource(R.string.class_create_error_required)
  val invalidCapacityMessage = stringResource(R.string.class_create_error_capacity)
  val saveSuccessMessage = stringResource(R.string.class_create_success)
  val saveErrorMessage = stringResource(R.string.class_create_error_save)

  DetailScaffold(title = stringResource(R.string.class_create_title), onBack = onBack) {
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
          label = { Text(stringResource(R.string.class_create_name)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = course,
          onValueChange = { course = it },
          label = { Text(stringResource(R.string.class_create_course)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = instructor,
          onValueChange = { instructor = it },
          label = { Text(stringResource(R.string.class_create_instructor)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = studentsCount,
          onValueChange = { studentsCount = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.class_create_students_count)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = maxCapacity,
          onValueChange = { maxCapacity = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.class_create_capacity)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = schedule,
          onValueChange = { schedule = it },
          label = { Text(stringResource(R.string.class_create_schedule)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )

      Text(
          text = stringResource(R.string.class_create_status),
          style = MaterialTheme.typography.labelLarge,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClassStatus.entries.forEach { item ->
          FilterChip(
              selected = status == item,
              onClick = { status = item },
              label = {
                Text(
                    stringResource(
                        when (item) {
                          ClassStatus.Open -> R.string.class_status_open
                          ClassStatus.InProgress -> R.string.class_status_in_progress
                          ClassStatus.Closed -> R.string.class_status_closed
                        }
                    )
                )
              },
          )
        }
      }

      Button(
          onClick = {
            val enrolled = studentsCount.toIntOrNull()
            val capacity = maxCapacity.toIntOrNull()
            if (name.isBlank() || course.isBlank() || instructor.isBlank() || schedule.isBlank()) {
              scope.launch { snackbarHostState.showSnackbar(requiredFieldsMessage) }
              return@Button
            }
            if (enrolled == null || capacity == null || capacity <= 0 || enrolled > capacity) {
              scope.launch { snackbarHostState.showSnackbar(invalidCapacityMessage) }
              return@Button
            }

            scope.launch {
              val nextId = (classes.maxOfOrNull { it.id } ?: 0) + 1
              when (
                  val result =
                      dataService.upsertClass(
                          SchoolClass(
                              id = nextId,
                              name = name.trim(),
                              course = course.trim(),
                              instructor = instructor.trim(),
                              studentsCount = enrolled,
                              maxCapacity = capacity,
                              schedule = schedule.trim(),
                              status = status,
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
        Text(stringResource(R.string.class_create_save))
      }
    }
  }
}
