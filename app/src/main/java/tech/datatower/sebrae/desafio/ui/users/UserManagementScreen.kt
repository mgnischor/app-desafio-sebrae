package tech.datatower.sebrae.desafio.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Tela de criação/cadastro de usuários.
 *
 * Somente usuários administradores possuem acesso ao formulário.
 */
@Composable
fun UserManagementScreen(
    currentUser: AppUser?,
    onBack: () -> Unit,
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val users by
      repository.observeRegisteredUsersForAdmin(currentUser).collectAsState(initial = emptyList())

  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var selectedRole by remember { mutableStateOf(UserRole.PROFESSOR) }

  DetailScaffold(
      title = stringResource(R.string.user_management_title),
      onBack = onBack,
      actions = {},
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
              text = stringResource(R.string.user_management_access_denied),
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
        return@Column
      }

      ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.elevatedCardColors(),
      ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
              text = stringResource(R.string.user_management_new_user),
              style = MaterialTheme.typography.titleMedium,
          )

          OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text(stringResource(R.string.user_management_name)) },
              singleLine = true,
          )

          OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text(stringResource(R.string.user_management_email)) },
              singleLine = true,
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.user_management_role),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              UserRole.entries.forEach { role ->
                AssistChip(
                    onClick = { selectedRole = role },
                    label = { Text(roleLabel(role)) },
                )
              }
            }
          }

          Button(
              onClick = {
                val normalizedEmail = email.trim().lowercase()
                if (name.isBlank() || normalizedEmail.isBlank()) {
                  scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.user_management_invalid_data)
                    )
                  }
                  return@Button
                }

                scope.launch {
                  runCatching {
                        val nextId = (users.maxOfOrNull { it.id } ?: 0) + 1
                        repository.upsertRegisteredUserForAdmin(
                            requester = currentUser,
                            user =
                                AppUser(
                                    id = nextId,
                                    name = name.trim(),
                                    email = normalizedEmail,
                                    role = selectedRole,
                                ),
                        )
                      }
                      .onSuccess {
                        name = ""
                        email = ""
                        selectedRole = UserRole.PROFESSOR
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.user_management_created_success)
                        )
                      }
                      .onFailure {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.user_management_created_error)
                        )
                      }
                }
              },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.user_management_save))
          }
        }
      }

      Text(
          text = stringResource(R.string.user_management_existing_users),
          style = MaterialTheme.typography.titleMedium,
      )

      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(users, key = { it.id }) { user ->
          ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(text = user.name, style = MaterialTheme.typography.bodyLarge)
              Text(text = user.email, style = MaterialTheme.typography.bodySmall)
              Text(text = roleLabel(user.role), style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }
  }
}

private fun roleLabel(role: UserRole): String {
  return when (role) {
    UserRole.PROFESSOR -> "Professor"
    UserRole.COORDENADOR -> "Coordenador"
    UserRole.ADMINISTRADOR -> "Administrador"
  }
}

@Preview(showBackground = true)
@Composable
private fun UserManagementScreenPreview() {
  AppDesafioSEBRAETheme {
    UserManagementScreen(
        currentUser = AppUser(3, "Admin", "admin@sebrae.edu.br", UserRole.ADMINISTRADOR),
        onBack = {},
    )
  }
}

