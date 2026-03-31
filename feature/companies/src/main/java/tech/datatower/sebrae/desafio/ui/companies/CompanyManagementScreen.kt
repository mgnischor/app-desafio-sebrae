package tech.datatower.sebrae.desafio.ui.companies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompanyManagementScreen(
    currentUser: AppUser?,
    onBack: () -> Unit,
    onOpenCompanyDetail: (Int) -> Unit = {},
) {
  val viewModel: CompanyManagementViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val companiesState by viewModel.state.collectAsState()
  val actionResult by viewModel.actionResult.collectAsState()
  val companies =
      when (val s = companiesState) {
        is CompanyManagementViewModel.CompaniesState.Success -> s.companies
        else -> emptyList()
      }

  var name by remember { mutableStateOf("") }
  var cnpj by remember { mutableStateOf("") }
  var pendingSaveReset by remember { mutableStateOf(false) }

  LaunchedEffect(actionResult) {
    when (val r = actionResult) {
      is CompanyManagementViewModel.ActionResult.Success -> {
        if (pendingSaveReset) {
          name = ""
          cnpj = ""
          pendingSaveReset = false
          snackbarHostState.showSnackbar("Empresa cadastrada com sucesso.")
        } else {
          snackbarHostState.showSnackbar("Empresa removida com sucesso.")
        }
        viewModel.clearActionResult()
      }
      is CompanyManagementViewModel.ActionResult.Error -> {
        snackbarHostState.showSnackbar(r.message)
        viewModel.clearActionResult()
      }
      else -> Unit
    }
  }

  DetailScaffold(
      title = "Gestao de Empresas",
      onBack = onBack,
  ) { innerPadding, _ ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SnackbarHost(hostState = snackbarHostState)

      if (currentUser?.role != UserRole.ADMINISTRADOR) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(),
        ) {
          Text(
              text = "Apenas administradores podem acessar esta tela.",
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
        return@Column
      }

      ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(text = "Nova empresa", style = MaterialTheme.typography.titleMedium)

          OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Nome da empresa") },
              singleLine = true,
          )
          OutlinedTextField(
              value = cnpj,
              onValueChange = { cnpj = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("CNPJ") },
              singleLine = true,
          )

          Button(
              onClick = {
                if (name.isBlank()) {
                  scope.launch { snackbarHostState.showSnackbar("Preencha o nome da empresa.") }
                  return@Button
                }
                pendingSaveReset = true
                viewModel.createCompany(currentUser, name, cnpj)
              },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Salvar empresa")
          }
        }
      }

      Text(text = "Empresas cadastradas", style = MaterialTheme.typography.titleMedium)

      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(companies, key = { it.id }) { company ->
          ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = company.name, style = MaterialTheme.typography.bodyLarge)
                if (company.cnpj.isNotBlank()) {
                  Text(text = company.cnpj, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = if (company.isActive) "Ativa" else "Inativa",
                    style = MaterialTheme.typography.labelMedium,
                )
              }
              Row {
                IconButton(onClick = { onOpenCompanyDetail(company.id) }) {
                  Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { viewModel.deleteCompany(currentUser, company.id) }) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir")
                }
              }
            }
          }
        }
      }
    }
  }
}
