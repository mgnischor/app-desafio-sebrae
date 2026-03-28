package tech.datatower.sebrae.desafio.ui.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
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
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.RelationshipRules
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay

/**
 * Executa a rotina de student create screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCreateScreen(currentUser: AppUser?, onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val dataService = remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val students by repository.observeStudents().collectAsState(initial = emptyList())
  val courses by repository.observeCourses().collectAsState(initial = emptyList())
  val classes by repository.observeClasses().collectAsState(initial = emptyList())

  LaunchedEffect(Unit) { dataService.syncScope(ScreenDataScope.STUDENTS) }

  var name by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var course by rememberSaveable { mutableStateOf("") }
  var enrolledClass by rememberSaveable { mutableStateOf("") }
  var progressPercent by rememberSaveable { mutableStateOf("0") }
  var status by rememberSaveable { mutableStateOf(StudentStatus.Active) }
  val requiredFieldsMessage = stringResource(R.string.student_create_error_required)
  val invalidProgressMessage = stringResource(R.string.student_create_error_progress)
  val saveSuccessMessage = stringResource(R.string.student_create_success)
  val saveErrorMessage = stringResource(R.string.student_create_error_save)
  val deniedMessage = stringResource(R.string.permission_denied)
  var isSaving by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
  DetailScaffold(title = stringResource(R.string.student_create_title), onBack = onBack) {
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
          label = { Text(stringResource(R.string.student_create_name)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(stringResource(R.string.student_create_email)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = course,
          onValueChange = { course = it },
          label = { Text(stringResource(R.string.student_create_course)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = enrolledClass,
          onValueChange = { enrolledClass = it },
          label = { Text(stringResource(R.string.student_create_class)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )
      OutlinedTextField(
          value = progressPercent,
          onValueChange = { progressPercent = it.filter(Char::isDigit) },
          label = { Text(stringResource(R.string.student_create_progress)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
      )

      Text(
          text = stringResource(R.string.student_create_status),
          style = MaterialTheme.typography.labelLarge,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StudentStatus.entries.forEach { item ->
          FilterChip(
              selected = status == item,
              onClick = { status = item },
              label = {
                Text(
                    stringResource(
                        when (item) {
                          StudentStatus.Active -> R.string.status_active
                          StudentStatus.Inactive -> R.string.status_inactive
                          StudentStatus.Graduated -> R.string.status_graduated
                        }
                    )
                )
              },
          )
        }
      }

      Button(
          onClick = {
            val progress = progressPercent.toIntOrNull()
            if (name.isBlank() || email.isBlank() || course.isBlank() || enrolledClass.isBlank()) {
              scope.launch { snackbarHostState.showSnackbar(requiredFieldsMessage) }
              return@Button
            }
            if (progress == null || progress !in 0..100) {
              scope.launch { snackbarHostState.showSnackbar(invalidProgressMessage) }
              return@Button
            }

            if (
                !AccessPolicy.can(
                    currentUser?.role,
                    ProtectedResource.Students,
                    ProtectedAction.Create,
                )
            ) {
              scope.launch { snackbarHostState.showSnackbar(deniedMessage) }
              return@Button
            }

            val candidate =
                Student(
                    id = (students.maxOfOrNull { it.id } ?: 0) + 1,
                    name = name.trim(),
                    email = email.trim(),
                    course = course.trim(),
                    enrolledClass = enrolledClass.trim(),
                    progress = progress / 100f,
                    status = status,
                )

            val relationshipError =
                RelationshipRules.validateStudentLinks(
                    student = candidate,
                    existingCourses = courses,
                    existingClasses = classes,
                )
            if (relationshipError != null) {
              scope.launch { snackbarHostState.showSnackbar(relationshipError) }
              return@Button
            }

            scope.launch {
              isSaving = true
              when (
                  val result =
                      dataService.upsertStudent(
                          requester = currentUser,
                          student = candidate,
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
              isSaving = false
            }
          },
          enabled = !isSaving,
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      ) {
        Text(stringResource(R.string.student_create_save))
      }
    }
  }
  LoadingOverlay(isVisible = isSaving, message = stringResource(R.string.loading_saving))
  }
}
