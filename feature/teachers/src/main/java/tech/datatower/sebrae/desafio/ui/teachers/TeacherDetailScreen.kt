/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeacherDetailScreen.kt
    Descrição: Tela de detalhes de um professor, exibindo turmas vinculadas e perfil completo.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import java.util.Locale

/**
 * Tela de detalhes de um professor.
 *
 * Exibe o perfil completo do professor e suas turmas vinculadas. Usuários com permissão de
 * atualização (Administrador e Coordenador) também têm acesso a uma seção de edição inline.
 *
 * @param currentUser Usuário autenticado; controla visibilidade de campos e seções restritas.
 * @param onBack Ação de retorno para a tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDetailScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
) {
  val viewModel: TeacherDetailViewModel = hiltViewModel()
  val teacher by viewModel.teacher.collectAsState()
  val classes by viewModel.classes.collectAsState()
  val saveState by viewModel.saveState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val canUpdateTeacher =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Teachers, ProtectedAction.Update)
  val showRating =
      currentUser?.role == UserRole.ADMINISTRADOR || currentUser?.role == UserRole.COORDENADOR

  // Form state — initialised once from the loaded teacher data
  var name by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var specialty by rememberSaveable { mutableStateOf("") }
  var activeCourses by rememberSaveable { mutableStateOf("0") }
  var totalStudents by rememberSaveable { mutableStateOf("0") }
  var rating by rememberSaveable { mutableStateOf("0.0") }
  var formInitialized by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(teacher) {
    if (!formInitialized && teacher != null) {
      name = teacher!!.name
      email = teacher!!.email
      specialty = teacher!!.specialty
      activeCourses = teacher!!.activeCourses.toString()
      totalStudents = teacher!!.totalStudents.toString()
      rating = String.format(Locale.US, "%.1f", teacher!!.rating)
      formInitialized = true
    }
  }

  val saveSuccessMessage = stringResource(R.string.teacher_edit_success)
  val saveErrorMessage = stringResource(R.string.teacher_edit_error)
  val requiredFieldsMessage = stringResource(R.string.teacher_create_error_required)
  val invalidNumericMessage = stringResource(R.string.teacher_create_error_numeric)

  LaunchedEffect(saveState) {
    when (val s = saveState) {
      is TeacherDetailViewModel.SaveState.Success -> {
        snackbarHostState.showSnackbar(saveSuccessMessage)
        viewModel.clearSaveState()
      }
      is TeacherDetailViewModel.SaveState.Error -> {
        snackbarHostState.showSnackbar(s.message.ifBlank { saveErrorMessage })
        viewModel.clearSaveState()
      }
      else -> Unit
    }
  }

  DetailScaffold(title = stringResource(R.string.teacher_detail_title), onBack = onBack) {
      innerPadding,
      _ ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (teacher == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
          Text(
              stringResource(R.string.teacher_not_found),
              style = MaterialTheme.typography.bodyLarge,
          )
        }
      } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // ── Profile card ──────────────────────────────────────────────────────
          item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
              Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                    teacher!!.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(teacher!!.specialty, style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.label_email, teacher!!.email),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(
                        R.string.label_active_courses_count,
                        teacher!!.activeCourses,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(
                        R.string.label_total_students_count,
                        teacher!!.totalStudents,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showRating) {
                  Text(
                      stringResource(R.string.label_rating_value, teacher!!.rating),
                      style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }

          // ── Edit card (Coordenador / Administrador only) ──────────────────────
          if (canUpdateTeacher) {
            item {
              ElevatedCard(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors =
                      CardDefaults.elevatedCardColors(
                          MaterialTheme.colorScheme.surfaceContainerLow
                      ),
              ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                      text = stringResource(R.string.teacher_edit_section_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold,
                  )
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
                        if (
                            active == null || students == null || score == null || score !in 0f..5f
                        ) {
                          scope.launch { snackbarHostState.showSnackbar(invalidNumericMessage) }
                          return@Button
                        }
                        viewModel.updateTeacher(
                            requester = currentUser,
                            name = name,
                            email = email,
                            specialty = specialty,
                            activeCourses = active,
                            totalStudents = students,
                            rating = score,
                        )
                      },
                      modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(stringResource(R.string.teacher_edit_save))
                  }
                }
              }
            }
          }

          // ── Classes header ────────────────────────────────────────────────────
          item {
            Text(
                text = stringResource(R.string.label_taught_classes, classes.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
          }

          // ── Classes list ──────────────────────────────────────────────────────
          if (classes.isEmpty()) {
            item {
              Text(
                  stringResource(R.string.no_linked_classes),
                  style = MaterialTheme.typography.bodyMedium,
              )
            }
          } else {
            items(classes, key = { it.id }) { schoolClass ->
              ElevatedCard(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  colors =
                      CardDefaults.elevatedCardColors(
                          MaterialTheme.colorScheme.surfaceContainerLow
                      ),
              ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                      schoolClass.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Medium,
                  )
                  Text(
                      stringResource(R.string.label_course, schoolClass.course),
                      style = MaterialTheme.typography.bodySmall,
                  )
                  Text(
                      stringResource(R.string.label_schedule, schoolClass.schedule),
                      style = MaterialTheme.typography.bodySmall,
                  )
                  Text(
                      stringResource(
                          R.string.label_students_count,
                          schoolClass.studentsCount,
                          schoolClass.maxCapacity,
                      ),
                      style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }
      }

      SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}
