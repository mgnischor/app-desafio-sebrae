package tech.datatower.sebrae.desafio.ui.companies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompanyDetailScreen(onBack: () -> Unit) {
  val viewModel: CompanyDetailViewModel = hiltViewModel()
  val snackbarHostState = remember { SnackbarHostState() }

  val company by viewModel.company.collectAsState()
  val saveState by viewModel.saveState.collectAsState()
  val currentUser by AuthManager.currentUser.collectAsState()
  val linkedUserIds by viewModel.linkedUserIds.collectAsState()
  val allUsers by viewModel.allUsers.collectAsState()

  var name by remember { mutableStateOf("") }
  var cnpj by remember { mutableStateOf("") }
  var isActive by remember { mutableStateOf(true) }
  var initialized by remember { mutableStateOf(false) }

  LaunchedEffect(company) {
    if (!initialized && company != null) {
      name = company!!.name
      cnpj = company!!.cnpj
      isActive = company!!.isActive
      initialized = true
    }
  }

  LaunchedEffect(saveState) {
    when (val s = saveState) {
      is CompanyDetailViewModel.SaveState.Success -> {
        snackbarHostState.showSnackbar("Empresa atualizada com sucesso.")
        viewModel.clearSaveState()
      }
      is CompanyDetailViewModel.SaveState.Error -> {
        snackbarHostState.showSnackbar(s.message)
        viewModel.clearSaveState()
      }
      else -> Unit
    }
  }

  DetailScaffold(
      title = "Detalhes da Empresa",
      onBack = onBack,
  ) { innerPadding, _ ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SnackbarHost(hostState = snackbarHostState)

      if (company == null) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        return@Column
      }

      // ── Editar empresa ──────────────────────────────────────
      ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(text = "Editar empresa", style = MaterialTheme.typography.titleMedium)

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

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = "Empresa ativa", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = isActive, onCheckedChange = { isActive = it })
          }

          Button(
              onClick = { viewModel.updateCompany(currentUser, name, cnpj, isActive) },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Salvar alteracoes")
          }
        }
      }

      // ── Usuarios vinculados ─────────────────────────────────
      ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
              text = "Usuarios vinculados",
              style = MaterialTheme.typography.titleMedium,
          )
          HorizontalDivider()

          if (allUsers.isEmpty()) {
            Text(
                text = "Nenhum usuario encontrado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            allUsers.forEach { user ->
              val isLinked = user.id in linkedUserIds
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                    checked = isLinked,
                    onCheckedChange = { checked ->
                      viewModel.toggleUserAccess(currentUser, user.id, checked)
                    },
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = user.name,
                      style = MaterialTheme.typography.bodyMedium,
                  )
                  Text(
                      text = "${user.email} • ${user.role.name}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(24.dp))
    }
  }
}
