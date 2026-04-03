/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/courses/CourseCreateScreen.kt
    Descrição: Tela de cadastro de novo curso com campos de título, carga horária e descrição.
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
package tech.datatower.sebrae.desafio.ui.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
import tech.datatower.sebrae.desafio.ui.components.SearchableDropdown

/**
 * Tela de cadastro de novo curso.
 *
 * Os campos [totalStudents] e [completionRate] são calculados automaticamente pelo repositório. A
 * [startDate] (formato AAAA-MM-DD) alimenta o cálculo dinâmico da taxa de conclusão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateScreen(currentUser: AppUser?, onBack: () -> Unit) {
  val viewModel: CourseCreateViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val courses by viewModel.courses.collectAsState()
  val teachers by viewModel.teachers.collectAsState()
  val saveState by viewModel.saveState.collectAsState()
  val isSaving = saveState is CourseCreateViewModel.SaveState.Saving

  val saveErrorMessage = stringResource(R.string.course_create_error_save)
  LaunchedEffect(saveState) {
    when (val s = saveState) {
      is CourseCreateViewModel.SaveState.Success -> {
        viewModel.resetSaveState()
        onBack()
      }
      is CourseCreateViewModel.SaveState.Error -> {
        snackbarHostState.showSnackbar(s.message.ifBlank { saveErrorMessage })
        viewModel.resetSaveState()
      }
      else -> Unit
    }
  }

  var title by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("") }
  var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
  var durationHours by rememberSaveable { mutableStateOf("") }
  var startDate by rememberSaveable { mutableStateOf("") }
  var isPublished by rememberSaveable { mutableStateOf(true) }
  val requiredFieldsMessage = stringResource(R.string.course_create_error_required)
  val invalidNumericMessage = stringResource(R.string.course_create_error_numeric)
  val deniedMessage = stringResource(R.string.permission_denied)

  Box(modifier = Modifier.fillMaxSize()) {
    DetailScaffold(title = stringResource(R.string.course_create_title), onBack = onBack) {
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
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.course_create_title_label)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.course_create_category)) },
            singleLine = true,
        )
        SearchableDropdown(
            label = stringResource(R.string.course_create_instructor),
            items = teachers,
            selected = selectedTeacher,
            itemLabel = { it.name },
            onItemSelected = { selectedTeacher = it },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = durationHours,
            onValueChange = { durationHours = it.filter(Char::isDigit) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.course_create_duration)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.course_create_start_date)) },
            singleLine = true,
            placeholder = { Text("AAAA-MM-DD") },
        )

        // Total de alunos e taxa de conclusão — calculados dinamicamente
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.course_create_students) + ": 0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.course_create_students_computed_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text = stringResource(R.string.course_create_completion) + ": 0%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.course_create_completion_computed_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
          }
        }

        Text(
            text = stringResource(R.string.course_create_published),
            style = MaterialTheme.typography.labelLarge,
        )
        FilterChip(
            selected = isPublished,
            onClick = { isPublished = true },
            label = { Text(stringResource(R.string.course_create_yes)) },
        )
        FilterChip(
            selected = !isPublished,
            onClick = { isPublished = false },
            label = { Text(stringResource(R.string.course_create_no)) },
        )

        Button(
            onClick = {
              val hours = durationHours.toIntOrNull()

              if (title.isBlank() || category.isBlank() || selectedTeacher == null) {
                scope.launch { snackbarHostState.showSnackbar(requiredFieldsMessage) }
                return@Button
              }
              if (hours == null || hours <= 0) {
                scope.launch { snackbarHostState.showSnackbar(invalidNumericMessage) }
                return@Button
              }

              if (
                  !AccessPolicy.can(
                      currentUser?.role,
                      ProtectedResource.Courses,
                      ProtectedAction.Create,
                  )
              ) {
                scope.launch { snackbarHostState.showSnackbar(deniedMessage) }
                return@Button
              }

              val nextId = (courses.maxOfOrNull { it.id } ?: 0) + 1
              viewModel.saveCourse(
                  requester = currentUser,
                  course =
                      Course(
                          id = nextId,
                          title = title.trim(),
                          category = category.trim(),
                          instructor = selectedTeacher!!.name,
                          totalStudents = 0, // calculado dinamicamente pelo repositório
                          durationHours = hours,
                          completionRate = 0f, // calculado dinamicamente a partir de startDate
                          isPublished = isPublished,
                          startDate = startDate.trim().ifBlank { null },
                      ),
              )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
          Text(stringResource(R.string.course_create_save))
        }
      }
    }
    LoadingOverlay(isVisible = isSaving, message = stringResource(R.string.loading_saving))
  }
}
