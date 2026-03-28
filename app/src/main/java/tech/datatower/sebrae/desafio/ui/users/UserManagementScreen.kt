/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/users/UserManagementScreen.kt
    Descrição: Tela de gerenciamento de usuários, permitindo criar e excluir contas.
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.ui.components.DetailScaffold
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Executa a rotina de user management screen dentro do contexto deste componente.
 *
 * @param currentUser Valor de entrada utilizado por esta operação.
 * @param onBack Valor de entrada utilizado por esta operação.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserManagementScreen(
    currentUser: AppUser?,
    onBack: () -> Unit,
) {
  val viewModel: UserManagementViewModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val usersState by viewModel.usersState.collectAsState()
  val actionResult by viewModel.actionResult.collectAsState()
  val users = when (val s = usersState) {
    is UserManagementViewModel.UsersState.Success -> s.users
    else -> emptyList()
  }

  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var selectedRole by remember { mutableStateOf(UserRole.PROFESSOR) }
  var pendingSaveReset by remember { mutableStateOf(false) }

  LaunchedEffect(currentUser?.id, currentUser?.role) { viewModel.observeUsers(currentUser) }

  LaunchedEffect(actionResult) {
    when (val r = actionResult) {
      is UserManagementViewModel.ActionResult.Success -> {
        if (pendingSaveReset) {
          name = ""
          email = ""
          password = ""
          selectedRole = UserRole.PROFESSOR
          pendingSaveReset = false
          snackbarHostState.showSnackbar("Usuário cadastrado com sucesso.")
        } else {
          snackbarHostState.showSnackbar("Usuário removido com sucesso.")
        }
        viewModel.clearActionResult()
      }
      is UserManagementViewModel.ActionResult.Error -> {
        snackbarHostState.showSnackbar(r.message)
        viewModel.clearActionResult()
      }
      else -> Unit
    }
  }

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

                val nextId = (users.maxOfOrNull { it.id } ?: 0) + 1
                pendingSaveReset = true
                viewModel.createUser(currentUser, nextId, name.trim(), normalizedEmail, selectedRole, password)
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
              if (user.id != currentUser?.id) {
                IconButton(
                    onClick = { viewModel.deleteUser(currentUser, user.id) }
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

/**
 * Executa a rotina de role label dentro do contexto deste componente.
 *
 * @param role Valor de entrada utilizado por esta operação.
 * @return Resultado produzido pela operação em formato `String`.
 */
private fun roleLabel(role: UserRole): String {
  return when (role) {
    UserRole.PROFESSOR -> "Professor"
    UserRole.COORDENADOR -> "Coordenador"
    UserRole.ADMINISTRADOR -> "Administrador"
  }
}

/** Executa a rotina de user management screen preview dentro do contexto deste componente. */
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
