/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/students/StudentsScreen.kt
    Descrição: Tela de listagem de alunos, com busca, filtro e ações de gerenciamento.
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.StudentStatus
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de gerenciamento de alunos.
 *
 * Permite buscar por nome, curso ou turma e apresenta os resultados em lista otimizada com
 * `LazyColumn`.
 *
 * @param onBack Ação acionada ao retornar para a tela anterior.
 * @param onOpenStudentMonitoring Ação para abrir o acompanhamento detalhado do aluno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
    onCreateStudent: () -> Unit = {},
    onOpenStudentMonitoring: (Int) -> Unit = {},
) {
  val canCreateStudent =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Students, ProtectedAction.Create)
  val canDeactivateStudent =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Students, ProtectedAction.Deactivate)
  val canReactivateStudent =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Students, ProtectedAction.Reactivate)
  val canDeleteStudent =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Students, ProtectedAction.Delete)
  val actionErrorMessage = stringResource(R.string.action_error)

  val viewModel: StudentsViewModel = hiltViewModel()
  val allStudents by viewModel.students.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isInitialLoading by viewModel.isInitialLoading.collectAsState()
  val actionResult by viewModel.actionResult.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(actionResult) {
    if (actionResult is StudentsViewModel.ActionResult.Error) {
      snackbarHostState.showSnackbar(actionErrorMessage)
      viewModel.clearActionResult()
    }
  }

  val filtered by
      remember(query, allStudents) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allStudents
          } else {
            allStudents.filter {
              it.name.contains(normalizedQuery, ignoreCase = true) ||
                  it.course.contains(normalizedQuery, ignoreCase = true) ||
                  it.enrolledClass.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  Box(modifier = Modifier.fillMaxSize()) {
    DetailScaffold(title = stringResource(R.string.students_title), onBack = onBack) {
        innerPadding,
        _ ->
      /** Executa a rotina de refresh students dentro do contexto deste componente. */
      fun refreshStudents() = viewModel.refresh()

      Scaffold(
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          floatingActionButton = {
            if (canCreateStudent) {
              FloatingActionButton(
                  onClick = onCreateStudent,
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
              ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.students_add_content_description),
                )
              }
            }
          },
          containerColor = MaterialTheme.colorScheme.background,
      ) { fabPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::refreshStudents,
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(fabPadding),
        ) {
          LazyColumn(
              modifier = Modifier.fillMaxSize(),
              state = listState,
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            item {
              ListSearchHeader(
                  query = query,
                  onQueryChange = { query = it },
                  placeholder = stringResource(R.string.students_search_placeholder),
                  resultCount = filtered.size,
                  resultLabel = stringResource(R.string.students_result_label),
              )
            }

            if (filtered.isEmpty()) {
              item {
                EmptyState(
                    icon = Icons.Outlined.Person,
                    message = stringResource(R.string.students_empty_state),
                )
              }
            } else {
              items(
                  items = filtered,
                  key = { it.id },
                  contentType = { "student" },
              ) { student ->
                StudentCard(
                    student = student,
                    onClick = { onOpenStudentMonitoring(student.id) },
                    canDeactivate = canDeactivateStudent,
                    canReactivate = canReactivateStudent,
                    canDelete = canDeleteStudent,
                    onDeactivate = { viewModel.deactivateStudent(currentUser, student) },
                    onReactivate = { viewModel.reactivateStudent(currentUser, student) },
                    onDelete = { viewModel.deleteStudent(currentUser, student.id) },
                )
              }
            }
          }
        }
      }
    }
    LoadingOverlay(
        isVisible = isInitialLoading,
        message = stringResource(R.string.loading_syncing),
    )
  }
}

/**
 * Cartão de exibição resumida de um aluno.
 *
 * @param student Dados do aluno que serão renderizados no cartão.
 * @param onClick Ação de navegação para o detalhe de acompanhamento do aluno.
 */
@Composable
private fun StudentCard(
    student: Student,
    onClick: () -> Unit,
    canDeactivate: Boolean,
    canReactivate: Boolean,
    canDelete: Boolean,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      // Avatar
      Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(48.dp),
      ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = student.name.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = student.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
          )
          StatusChip(
              label =
                  when (student.status) {
                    StudentStatus.Active -> stringResource(R.string.status_active)
                    StudentStatus.Inactive -> stringResource(R.string.status_inactive)
                    StudentStatus.Graduated -> stringResource(R.string.status_graduated)
                  },
              containerColor =
                  when (student.status) {
                    StudentStatus.Active -> MaterialTheme.colorScheme.primaryContainer
                    StudentStatus.Inactive -> MaterialTheme.colorScheme.errorContainer
                    StudentStatus.Graduated -> MaterialTheme.colorScheme.tertiaryContainer
                  },
              contentColor =
                  when (student.status) {
                    StudentStatus.Active -> MaterialTheme.colorScheme.onPrimaryContainer
                    StudentStatus.Inactive -> MaterialTheme.colorScheme.onErrorContainer
                    StudentStatus.Graduated -> MaterialTheme.colorScheme.onTertiaryContainer
                  },
          )
        }
        Text(
            text = "${student.course} · ${student.enrolledClass}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          LinearProgressIndicator(
              progress = { student.progress },
              modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.primaryContainer,
              strokeCap = StrokeCap.Round,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = "${(student.progress * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (student.status == StudentStatus.Active && canDeactivate) {
            TextButton(onClick = onDeactivate) { Text(stringResource(R.string.action_deactivate)) }
          }
          if (student.status == StudentStatus.Inactive && canReactivate) {
            TextButton(onClick = onReactivate) { Text(stringResource(R.string.action_reactivate)) }
          }
          if (canDelete) {
            TextButton(onClick = onDelete) {
              Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(stringResource(R.string.action_delete))
            }
          }
        }
      }
    }
  }
}

/** Pré-visualização da tela de alunos no Android Studio/IntelliJ. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudentsPreview() {
  AppDesafioSEBRAETheme { StudentsScreen() }
}
