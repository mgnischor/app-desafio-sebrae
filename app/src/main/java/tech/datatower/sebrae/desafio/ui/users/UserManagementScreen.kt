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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserManagementScreen(
    currentUser: AppUser?,
    onBack: () -> Unit,
) {
  val context = LocalContext.current
  val service = remember(context) { AppGraph.dataConnectService(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val usersResult by
      service
          .observeUsersRealtimeForAdmin(currentUser)
          .collectAsState(initial = FirebaseDataConnectService.Result.Success(emptyList()))

  val users =
      when (val result = usersResult) {
        is FirebaseDataConnectService.Result.Success -> result.data
        else -> emptyList()
      }

  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var selectedRole by remember { mutableStateOf(UserRole.PROFESSOR) }

  DetailScaffold(
      title = "Gestao de Usuarios",
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
          Text(text = "Novo usuario", style = MaterialTheme.typography.titleMedium)

          OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Nome") },
              singleLine = true,
          )
          OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("E-mail") },
              singleLine = true,
          )
          OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Senha") },
              singleLine = true,
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Perfil de acesso", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              UserRole.entries.forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    label = { Text(roleLabel(role)) },
                )
              }
            }
            Text(
                text = "Selecionado: ${roleLabel(selectedRole)}",
                style = MaterialTheme.typography.bodySmall,
            )
          }

          Button(
              onClick = {
                val normalizedEmail = email.trim().lowercase()
                if (name.isBlank() || normalizedEmail.isBlank() || password.isBlank()) {
                  scope.launch { snackbarHostState.showSnackbar("Preencha nome, e-mail e senha.") }
                  return@Button
                }

                scope.launch {
                  val nextId = (users.maxOfOrNull { it.id } ?: 0) + 1
                  val result =
                      service.upsertUserForAdmin(
                          requester = currentUser,
                          target =
                              AppUser(
                                  id = nextId,
                                  name = name.trim(),
                                  email = normalizedEmail,
                                  role = selectedRole,
                              ),
                          plainPassword = password,
                      )
                  when (result) {
                    is FirebaseDataConnectService.Result.Success -> {
                      AuthManager.registerOrUpdateUser(
                          AppUser(nextId, name.trim(), normalizedEmail, selectedRole),
                          password,
                      )
                      name = ""
                      email = ""
                      password = ""
                      selectedRole = UserRole.PROFESSOR
                      snackbarHostState.showSnackbar("Usuario cadastrado com sucesso.")
                    }
                    is FirebaseDataConnectService.Result.Error -> {
                      val details =
                          (result.message.ifBlank { result.exception.message.orEmpty() }).ifBlank {
                            "Nao foi possivel cadastrar o usuario."
                          }
                      snackbarHostState.showSnackbar(details)
                    }
                    FirebaseDataConnectService.Result.Loading -> Unit
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Salvar usuario")
          }
        }
      }

      Text(text = "Usuarios cadastrados", style = MaterialTheme.typography.titleMedium)

      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(users, key = { it.id }) { user ->
          ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(text = user.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = user.email, style = MaterialTheme.typography.bodySmall)
                Text(text = roleLabel(user.role), style = MaterialTheme.typography.labelMedium)
              }
              if (user.id != currentUser.id) {
                IconButton(
                    onClick = {
                      scope.launch {
                        val result = service.deleteUserForAdmin(currentUser, user.id)
                        when (result) {
                          is FirebaseDataConnectService.Result.Success -> {
                            AuthManager.removeUserById(user.id)
                            snackbarHostState.showSnackbar("Usuario removido com sucesso.")
                          }
                          is FirebaseDataConnectService.Result.Error -> {
                            val details =
                                (result.message.ifBlank { result.exception.message.orEmpty() })
                                    .ifBlank { "Nao foi possivel remover o usuario." }
                            snackbarHostState.showSnackbar(details)
                          }
                          FirebaseDataConnectService.Result.Loading -> Unit
                        }
                      }
                    }
                ) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                }
              }
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
