/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/classes/ClassesScreen.kt
    Descrição: Tela de listagem de turmas, com busca, filtro e ações de gerenciamento.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Group
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
import tech.datatower.sebrae.desafio.data.model.ClassStatus
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.components.EmptyState
import tech.datatower.sebrae.desafio.ui.components.ListSearchHeader
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
import tech.datatower.sebrae.desafio.ui.components.StatusChip
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de acompanhamento de turmas.
 *
 * Suporta filtro por nome da turma, curso e instrutor, além de exibir status e ocupação de vagas
 * por turma.
 *
 * @param onBack Ação de navegação para retornar à tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
    onCreateClass: () -> Unit = {},
    onOpenClassDetail: (Int) -> Unit = {},
) {
  val canCreateClass =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Classes, ProtectedAction.Create)
  val canDeactivateClass =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Classes, ProtectedAction.Deactivate)
  val canReactivateClass =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Classes, ProtectedAction.Reactivate)
  val canDeleteClass =
      AccessPolicy.can(currentUser?.role, ProtectedResource.Classes, ProtectedAction.Delete)
  val actionErrorMessage = stringResource(R.string.action_error)

  val viewModel: ClassesViewModel = hiltViewModel()
  val allClasses by viewModel.classes.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isInitialLoading by viewModel.isInitialLoading.collectAsState()
  val actionResult by viewModel.actionResult.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var query by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(actionResult) {
    if (actionResult is ClassesViewModel.ActionResult.Error) {
      snackbarHostState.showSnackbar(actionErrorMessage)
      viewModel.clearActionResult()
    }
  }

  val filtered by
      remember(query, allClasses) {
        derivedStateOf {
          val normalizedQuery = query.trim()
          if (normalizedQuery.isBlank()) {
            allClasses
          } else {
            allClasses.filter {
              it.name.contains(normalizedQuery, ignoreCase = true) ||
                  it.course.contains(normalizedQuery, ignoreCase = true) ||
                  it.instructor.contains(normalizedQuery, ignoreCase = true)
            }
          }
        }
      }

  Box(modifier = Modifier.fillMaxSize()) {
    DetailScaffold(title = stringResource(R.string.classes_title), onBack = onBack) {
        innerPadding,
        _ ->
      /** Executa a rotina de refresh classes dentro do contexto deste componente. */
      fun refreshClasses() = viewModel.refresh()

      Scaffold(
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          floatingActionButton = {
            if (canCreateClass) {
              FloatingActionButton(
                  onClick = onCreateClass,
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
              ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.classes_add_content_description),
                )
              }
            }
          },
          containerColor = MaterialTheme.colorScheme.background,
      ) { fabPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::refreshClasses,
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
                  placeholder = stringResource(R.string.classes_search_placeholder),
                  resultCount = filtered.size,
                  resultLabel = stringResource(R.string.classes_result_label),
              )
            }

            if (filtered.isEmpty()) {
              item {
                EmptyState(
                    icon = Icons.Outlined.Group,
                    message = stringResource(R.string.classes_empty_state),
                )
              }
            } else {
              items(
                  items = filtered,
                  key = { it.id },
                  contentType = { "class" },
              ) { sc ->
                ClassCard(
                    sc = sc,
                    onClick = { onOpenClassDetail(sc.id) },
                    canDeactivate = canDeactivateClass,
                    canReactivate = canReactivateClass,
                    canDelete = canDeleteClass,
                    onDeactivate = { viewModel.deactivateClass(currentUser, sc) },
                    onReactivate = { viewModel.reactivateClass(currentUser, sc) },
                    onDelete = { viewModel.deleteClass(currentUser, sc.id) },
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
 * Cartão com os dados principais de uma turma.
 *
 * @param sc Turma a ser representada no item da lista.
 */
@Composable
private fun ClassCard(
    sc: SchoolClass,
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
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = sc.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        StatusChip(
            label =
                when (sc.status) {
                  ClassStatus.Open -> stringResource(R.string.class_status_open)
                  ClassStatus.InProgress -> stringResource(R.string.class_status_in_progress)
                  ClassStatus.Closed -> stringResource(R.string.class_status_closed)
                },
            containerColor =
                when (sc.status) {
                  ClassStatus.Open -> MaterialTheme.colorScheme.tertiaryContainer
                  ClassStatus.InProgress -> MaterialTheme.colorScheme.primaryContainer
                  ClassStatus.Closed -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            contentColor =
                when (sc.status) {
                  ClassStatus.Open -> MaterialTheme.colorScheme.onTertiaryContainer
                  ClassStatus.InProgress -> MaterialTheme.colorScheme.onPrimaryContainer
                  ClassStatus.Closed -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
      }
      Text(
          text = sc.course,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Outlined.Group,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text =
                  "${sc.studentsCount}/${sc.maxCapacity} ${stringResource(R.string.unit_students)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
            text = sc.schedule,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
          text = stringResource(R.string.label_instructor, sc.instructor),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      val fill = sc.studentsCount.toFloat() / sc.maxCapacity.coerceAtLeast(1)
      LinearProgressIndicator(
          progress = { fill },
          modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
          color = MaterialTheme.colorScheme.secondary,
          trackColor = MaterialTheme.colorScheme.secondaryContainer,
          strokeCap = StrokeCap.Round,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sc.status != ClassStatus.Closed && canDeactivate) {
          TextButton(onClick = onDeactivate) { Text(stringResource(R.string.action_deactivate)) }
        }
        if (sc.status == ClassStatus.Closed && canReactivate) {
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

/** Pré-visualização da tela de turmas para validação visual. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ClassesPreview() {
  AppDesafioSEBRAETheme { ClassesScreen() }
}
