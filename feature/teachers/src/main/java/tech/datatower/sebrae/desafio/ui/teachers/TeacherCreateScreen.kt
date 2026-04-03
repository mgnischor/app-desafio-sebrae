/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeacherCreateScreen.kt
    Descrição: Tela de cadastro de novo professor com dados pessoais e disciplinas.
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
package tech.datatower.sebrae.desafio.ui.teachers

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
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay

/**
 * Executa a rotina de teacher create screen dentro do contexto deste componente.
 *
 * @param onBack Valor de entrada utilizado por esta opera??o.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateScreen(currentUser: AppUser?, onBack: () -> Unit) {
  val viewModel: TeacherCreateViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val teachers by viewModel.teachers.collectAsState()
  val saveState by viewModel.saveState.collectAsState()
  val isSaving = saveState is TeacherCreateViewModel.SaveState.Saving

  val saveErrorMessage = stringResource(R.string.teacher_create_error_save)
  LaunchedEffect(saveState) {
    when (val s = saveState) {
      is TeacherCreateViewModel.SaveState.Success -> {
        viewModel.resetSaveState()
        onBack()
      }
      is TeacherCreateViewModel.SaveState.Error -> {
        snackbarHostState.showSnackbar(s.message.ifBlank { saveErrorMessage })
        viewModel.resetSaveState()
      }
      else -> Unit
    }
  }

  val canShowRating =
      currentUser?.role == UserRole.ADMINISTRADOR || currentUser?.role == UserRole.COORDENADOR

  var name by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var specialty by rememberSaveable { mutableStateOf("") }
  var rating by rememberSaveable { mutableStateOf("0.0") }

  val requiredFieldsMessage = stringResource(R.string.teacher_create_error_required)
  val invalidNumericMessage = stringResource(R.string.teacher_create_error_numeric)
  val deniedMessage = stringResource(R.string.permission_denied)

  Box(modifier = Modifier.fillMaxSize()) {
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

        // Info card — cursos ativos e total de alunos são calculados automaticamente
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.teacher_create_active_courses) + ": 0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.teacher_create_total_students) + ": 0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.teacher_create_computed_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
          }
        }

        // Avaliação visível apenas para Administrador e Coordenador
        if (canShowRating) {
          OutlinedTextField(
              value = rating,
              onValueChange = { rating = it.filter { ch -> ch.isDigit() || ch == '.' } },
              label = { Text(stringResource(R.string.teacher_create_rating)) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
          )
        }

        Button(
            onClick = {
              if (name.isBlank() || email.isBlank() || specialty.isBlank()) {
                scope.launch { snackbarHostState.showSnackbar(requiredFieldsMessage) }
                return@Button
              }

              val score = if (canShowRating) rating.toFloatOrNull() else 0f
              if (canShowRating && (score == null || score !in 0f..5f)) {
                scope.launch { snackbarHostState.showSnackbar(invalidNumericMessage) }
                return@Button
              }

              if (
                  !AccessPolicy.can(
                      currentUser?.role,
                      ProtectedResource.Teachers,
                      ProtectedAction.Create,
                  )
              ) {
                scope.launch { snackbarHostState.showSnackbar(deniedMessage) }
                return@Button
              }

              val nextId = (teachers.maxOfOrNull { it.id } ?: 0) + 1
              viewModel.saveTeacher(
                  requester = currentUser,
                  teacher =
                      Teacher(
                          id = nextId,
                          name = name.trim(),
                          email = email.trim(),
                          specialty = specialty.trim(),
                          activeCourses = 0, // calculado dinamicamente pelo repositório
                          totalStudents = 0, // calculado dinamicamente pelo repositório
                          rating = score ?: 0f,
                          isActive = true,
                      ),
              )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
          Text(stringResource(R.string.teacher_create_save))
        }
      }
    }
    LoadingOverlay(isVisible = isSaving, message = stringResource(R.string.loading_saving))
  }
}
