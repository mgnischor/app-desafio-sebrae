package tech.datatower.sebrae.desafio.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Tela de login da aplicação.
 *
 * Apresenta campos de e-mail e senha, valida as credenciais contra os usuários pré-cadastrados e
 * direciona para a tela inicial em caso de sucesso.
 *
 * @param onLoginSuccess Callback chamado após autenticação bem-sucedida.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var passwordVisible by rememberSaveable { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val passwordFocusRequester = remember { FocusRequester() }

  val errorMessage = stringResource(R.string.login_error_invalid_credentials)

  fun attemptLogin() {
    isLoading = true
    val success = AuthManager.login(email, password)
    isLoading = false
    if (success) {
      onLoginSuccess()
    } else {
      scope.launch { snackbarHostState.showSnackbar(errorMessage) }
    }
  }

  Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background,
                            )
                    )
                )
                .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .verticalScroll(rememberScrollState())
                  .padding(horizontal = 24.dp, vertical = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // ── Logo / header ──────────────────────────────────────────
        LogoSection()

        Spacer(modifier = Modifier.height(40.dp))

        // ── Login form ────────────────────────────────────────────
        LoginFormCard(
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
            isLoading = isLoading,
            passwordFocusRequester = passwordFocusRequester,
            onLoginClick = ::attemptLogin,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Credential hints ──────────────────────────────────────
        CredentialHints()
      }
    }
  }
}

// ── Logo section ──────────────────────────────────────────────────────────────

@Composable
private fun LogoSection() {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
        modifier =
            Modifier.size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
      Text(
          text = "S",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.Bold,
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.home_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

// ── Login form card ───────────────────────────────────────────────────────────

@Composable
private fun LoginFormCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean,
    passwordFocusRequester: FocusRequester,
    onLoginClick: () -> Unit,
) {
  Card(
      shape = RoundedCornerShape(24.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Text(
          text = stringResource(R.string.login_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = stringResource(R.string.login_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(24.dp))

      // E-mail field
      OutlinedTextField(
          value = email,
          onValueChange = onEmailChange,
          label = { Text(stringResource(R.string.login_label_email)) },
          leadingIcon = {
            Icon(imageVector = Icons.Outlined.MailOutline, contentDescription = null)
          },
          singleLine = true,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next,
              ),
          keyboardActions =
              KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Password field
      OutlinedTextField(
          value = password,
          onValueChange = onPasswordChange,
          label = { Text(stringResource(R.string.login_label_password)) },
          leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = onPasswordVisibilityToggle) {
              Icon(
                  imageVector =
                      if (passwordVisible) Icons.Outlined.VisibilityOff
                      else Icons.Outlined.Visibility,
                  contentDescription =
                      if (passwordVisible) stringResource(R.string.login_hide_password)
                      else stringResource(R.string.login_show_password),
              )
            }
          },
          visualTransformation =
              if (passwordVisible) VisualTransformation.None
              else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = ImeAction.Done,
              ),
          keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
          onClick = onLoginClick,
          enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
          shape = RoundedCornerShape(14.dp),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          modifier = Modifier.fillMaxWidth().height(52.dp),
      ) {
        Text(
            text =
                if (isLoading) stringResource(R.string.login_loading)
                else stringResource(R.string.login_button),
            style = MaterialTheme.typography.titleSmall,
        )
      }
    }
  }
}

// ── Credential hints ──────────────────────────────────────────────────────────

/** Exibe as credenciais de acesso pré-cadastradas como referência para o usuário. */
@Composable
private fun CredentialHints() {
  Card(
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
          ),
      modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
          text = stringResource(R.string.login_hint_title),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Spacer(modifier = Modifier.height(10.dp))
      CredentialRow(
          role = stringResource(R.string.role_professor),
          email = "professor@sebrae.edu.br",
      )
      Spacer(modifier = Modifier.height(6.dp))
      CredentialRow(
          role = stringResource(R.string.role_coordenador),
          email = "coordenador@sebrae.edu.br",
      )
      Spacer(modifier = Modifier.height(6.dp))
      CredentialRow(
          role = stringResource(R.string.role_administrador),
          email = "admin@sebrae.edu.br",
      )
    }
  }
}

@Composable
private fun CredentialRow(role: String, email: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = role,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Text(
          text = email,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
    }
  }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
  AppDesafioSEBRAETheme { LoginScreen(onLoginSuccess = {}) }
}
