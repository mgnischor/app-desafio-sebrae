/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/classes/ClassCreateScreen.kt
    Descrição: Tela de cadastro de nova turma com seleção de curso e professor responsável.
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
package tech.datatower.sebrae.desafio.ui.classes

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
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
import tech.datatower.sebrae.desafio.ui.components.SearchableDropdown

/**
 * Executa a rotina de class create screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassCreateScreen(currentUser: AppUser?, onBack: () -> Unit) {
  val viewModel: ClassCreateViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val classes by viewModel.classes.collectAsState()
  val courses by viewModel.courses.collectAsState()
  val teachers by viewModel.teachers.collectAsState()
  val saveState by viewModel.saveState.collectAsState()
  val isSaving = saveState is ClassCreateViewModel.SaveState.Saving

  val saveSuccessMessage = stringResource(R.string.class_create_success)
  val saveErrorMessage = stringResource(R.string.class_create_error_save)
  LaunchedEffect(saveState) {
    when (val s = saveState) {
      is ClassCreateViewModel.SaveState.Success -> {
        snackbarHostState.showSnackbar(saveSuccessMessage)
        viewModel.resetSaveState()
        onBack()
      }
      is ClassCreateViewModel.SaveState.Error -> {
        snackbarHostState.showSnackbar(s.message.ifBlank { saveErrorMessage })
        viewModel.resetSaveState()
      }
      else -> Unit
    }
  }

  var name by rememberSaveable { mutableStateOf("") }
  var selectedCourse by remember { mutableStateOf<Course?>(null) }
  var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
  var studentsCount by rememberSaveable { mutableStateOf("0") }
  var maxCapacity by rememberSaveable { mutableStateOf("0") }
  var schedule by rememberSaveable { mutableStateOf("") }
  var status by rememberSaveable { mutableStateOf(ClassStatus.Open) }
  val requiredFieldsMessage = stringResource(R.string.class_create_error_required)
  val invalidCapacityMessage = stringResource(R.string.class_create_error_capacity)
  val deniedMessage = stringResource(R.string.permission_denied)
  val relationshipMessage = stringResource(R.string.class_create_error_relationship)

  Box(modifier = Modifier.fillMaxSize()) {
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

        SearchableDropdown(
            label = stringResource(R.string.class_create_course),
            items = courses,
            selected = selectedCourse,
            itemLabel = { it.title },
            onItemSelected = { picked ->
              selectedCourse = picked
              // Limpa instrutor ao trocar o curso para evitar inconsistência
              selectedTeacher = null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Filtra instrutores que já possuem turmas no curso selecionado;
        // se não houver turmas ainda, exibe todos os instrutores cadastrados.
        val availableTeachers =
            remember(teachers, selectedCourse, classes) {
              val courseName = selectedCourse?.title ?: return@remember teachers
              val names =
                  classes
                      .filter { it.course.equals(courseName, ignoreCase = true) }
                      .map { it.instructor.trim().lowercase() }
                      .toSet()
              if (names.isEmpty()) teachers
              else teachers.filter { it.name.trim().lowercase() in names }
            }

        SearchableDropdown(
            label = stringResource(R.string.class_create_instructor),
            items = if (selectedCourse != null) availableTeachers else teachers,
            selected = selectedTeacher,
            itemLabel = { it.name },
            onItemSelected = { selectedTeacher = it },
            modifier = Modifier.fillMaxWidth(),
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
              if (
                  name.isBlank() ||
                      selectedCourse == null ||
                      selectedTeacher == null ||
                      schedule.isBlank()
              ) {
                scope.launch { snackbarHostState.showSnackbar(requiredFieldsMessage) }
                return@Button
              }
              if (enrolled == null || capacity == null || capacity <= 0 || enrolled > capacity) {
                scope.launch { snackbarHostState.showSnackbar(invalidCapacityMessage) }
                return@Button
              }

              if (
                  !AccessPolicy.can(
                      currentUser?.role,
                      ProtectedResource.Classes,
                      ProtectedAction.Create,
                  )
              ) {
                scope.launch { snackbarHostState.showSnackbar(deniedMessage) }
                return@Button
              }

              val nextId = (classes.maxOfOrNull { it.id } ?: 0) + 1
              viewModel.saveClass(
                  requester = currentUser,
                  schoolClass =
                      SchoolClass(
                          id = nextId,
                          name = name.trim(),
                          course = selectedCourse!!.title,
                          instructor = selectedTeacher!!.name,
                          studentsCount = enrolled,
                          maxCapacity = capacity,
                          schedule = schedule.trim(),
                          status = status,
                      ),
              )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
          Text(stringResource(R.string.class_create_save))
        }
      }
    }
    LoadingOverlay(isVisible = isSaving, message = stringResource(R.string.loading_saving))
  }
}
