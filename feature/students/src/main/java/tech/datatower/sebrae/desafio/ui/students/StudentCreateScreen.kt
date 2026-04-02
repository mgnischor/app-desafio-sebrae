/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/students/StudentCreateScreen.kt
    Descrição: Tela de cadastro de novo aluno com validação de dados e turma associada.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.RelationshipRules
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
import tech.datatower.sebrae.desafio.ui.components.SearchableDropdown

/**
 * Executa a rotina de student create screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta operação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCreateScreen(currentUser: AppUser?, onBack: () -> Unit) {
  val viewModel: StudentCreateViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val students by viewModel.students.collectAsState()
  val courses by viewModel.courses.collectAsState()
  val classes by viewModel.classes.collectAsState()
  val saveState by viewModel.saveState.collectAsState()
  val isSaving = saveState is StudentCreateViewModel.SaveState.Saving

  val saveErrorMessage = stringResource(R.string.student_create_error_save)
  LaunchedEffect(saveState) {
    when (val s = saveState) {
      is StudentCreateViewModel.SaveState.Success -> {
        viewModel.resetSaveState()
        onBack()
      }
      is StudentCreateViewModel.SaveState.Error -> {
        snackbarHostState.showSnackbar(s.message.ifBlank { saveErrorMessage })
        viewModel.resetSaveState()
      }
      else -> Unit
    }
  }

  var name by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var selectedCourse by remember { mutableStateOf<Course?>(null) }
  var selectedClass by remember { mutableStateOf<SchoolClass?>(null) }
  var progressPercent by rememberSaveable { mutableStateOf("0") }
  var status by rememberSaveable { mutableStateOf(StudentStatus.Active) }
  val requiredFieldsMessage = stringResource(R.string.student_create_error_required)
  val invalidProgressMessage = stringResource(R.string.student_create_error_progress)
  val deniedMessage = stringResource(R.string.permission_denied)

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

        SearchableDropdown(
            label = stringResource(R.string.student_create_course),
            items = courses,
            selected = selectedCourse,
            itemLabel = { it.title },
            onItemSelected = { picked ->
              selectedCourse = picked
              selectedClass = null // limpa turma ao trocar de curso
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Somente exibe turmas do curso selecionado
        val availableClasses =
            remember(classes, selectedCourse) {
              val courseName = selectedCourse?.title ?: return@remember emptyList<SchoolClass>()
              classes.filter { it.course.equals(courseName, ignoreCase = true) }
            }

        SearchableDropdown(
            label = stringResource(R.string.student_create_class),
            items = availableClasses,
            selected = selectedClass,
            itemLabel = { it.name },
            onItemSelected = { selectedClass = it },
            modifier = Modifier.fillMaxWidth(),
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
              if (
                  name.isBlank() ||
                      email.isBlank() ||
                      selectedCourse == null ||
                      selectedClass == null
              ) {
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
                      course = selectedCourse!!.title,
                      enrolledClass = selectedClass!!.name,
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

              viewModel.saveStudent(currentUser, candidate)
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
