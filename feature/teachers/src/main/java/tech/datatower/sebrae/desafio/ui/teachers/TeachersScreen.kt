/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeachersScreen.kt
    Descrição: Tela de listagem de professores, com busca, filtro e ações de gerenciamento.
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
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.data.auth.AccessPolicy
import tech.datatower.sebrae.desafio.data.auth.ProtectedAction
import tech.datatower.sebrae.desafio.data.auth.ProtectedResource
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme
import java.util.Locale

/**
 * Tela de gestão de instrutores.
 *
 * Oferece busca por nome e especialidade, exibindo os registros em lista com componentes
 * performáticos do Compose.
 *
 * @param onBack Ação de retorno para a tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
    onCreateTeacher: () -> Unit = {},
    onOpenTeacherDetail: (Int) -> Unit = {},
) {
  val canCreateTeacher =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Teachers, ProtectedAction.Create)
  val canDeactivateTeacher =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Teachers, ProtectedAction.Deactivate)
  val canReactivateTeacher =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Teachers, ProtectedAction.Reactivate)
  val canDeleteTeacher =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Teachers, ProtectedAction.Delete)
  val actionErrorMessage = stringResource(R.string.action_error)

  val viewModel: TeachersViewModel = hiltViewModel()
  val allTeachers by viewModel.teachers.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isInitialLoading by viewModel.isInitialLoading.collectAsState()
  val actionResult by viewModel.actionResult.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(actionResult) {
    if (actionResult is TeachersViewModel.ActionResult.Error) {
      snackbarHostState.showSnackbar(actionErrorMessage)
      viewModel.clearActionResult()
    }
  }

  val filtered by
      remember(query, allTeachers) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allTeachers
          } else {
            allTeachers.filter {
              it.name.contains(normalizedQuery, ignoreCase = true) ||
                  it.specialty.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  Box(modifier = Modifier.fillMaxSize()) {
    DetailScaffold(title = stringResource(R.string.teachers_title), onBack = onBack) {
        innerPadding,
        _ ->
      /** Executa a rotina de refresh teachers dentro do contexto deste componente. */
      fun refreshTeachers() = viewModel.refresh()

      Scaffold(
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          floatingActionButton = {
            if (canCreateTeacher) {
              FloatingActionButton(
                  onClick = onCreateTeacher,
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
              ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.teachers_add_content_description),
                )
              }
            }
          },
          containerColor = MaterialTheme.colorScheme.background,
      ) { fabPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::refreshTeachers,
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
                  placeholder = stringResource(R.string.teachers_search_placeholder),
                  resultCount = filtered.size,
                  resultLabel = stringResource(R.string.teachers_result_label),
              )
            }

            if (filtered.isEmpty()) {
              item {
                EmptyState(
                    icon = Icons.Outlined.School,
                    message = stringResource(R.string.teachers_empty_state),
                )
              }
            } else {
              items(
                  items = filtered,
                  key = { it.id },
                  contentType = { "teacher" },
              ) { teacher ->
                TeacherCard(
                    teacher = teacher,
                    onClick = { onOpenTeacherDetail(teacher.id) },
                    canDeactivate = canDeactivateTeacher,
                    canReactivate = canReactivateTeacher,
                    canDelete = canDeleteTeacher,
                    onDeactivate = { viewModel.deactivateTeacher(currentUser, teacher) },
                    onReactivate = { viewModel.reactivateTeacher(currentUser, teacher) },
                    onDelete = { viewModel.deleteTeacher(currentUser, teacher.id) },
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
 * Cartão com informações principais de um instrutor.
 *
 * @param teacher Instrutor a ser apresentado no item de lista.
 */
@Composable
private fun TeacherCard(
    teacher: Teacher,
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
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.size(52.dp),
      ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = teacher.name.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = teacher.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = teacher.specialty,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          Text(
              text = stringResource(R.string.unit_active_courses, teacher.activeCourses),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
              text = "${teacher.totalStudents} ${stringResource(R.string.unit_students)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (teacher.isActive && canDeactivate) {
            TextButton(onClick = onDeactivate) { Text(stringResource(R.string.action_deactivate)) }
          }
          if (!teacher.isActive && canReactivate) {
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

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = String.format(Locale.getDefault(), "%.1f", teacher.rating),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
  }
}

/** Pré-visualização da tela de instrutores para desenvolvimento. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TeachersPreview() {
  AppDesafioSEBRAETheme { TeachersScreen() }
}
