/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/login/LoginScreen.kt
    Descrição: Tela de autenticação do usuário, com campos de e-mail e senha.
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
package tech.datatower.sebrae.desafio.ui.login

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.datatower.sebrae.desafio.core.R
import tech.datatower.sebrae.desafio.ui.components.LoadingOverlay
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

  val viewModel: LoginViewModel = hiltViewModel()
  val loginState by viewModel.loginState.collectAsState()
  val isLoading = loginState is LoginViewModel.LoginState.Loading

  val snackbarHostState = remember { SnackbarHostState() }
  val passwordFocusRequester = remember { FocusRequester() }

  val errorMessage = stringResource(R.string.login_error_invalid_credentials)
  LaunchedEffect(loginState) {
    when (val state = loginState) {
      is LoginViewModel.LoginState.Success -> {
        viewModel.resetState()
        onLoginSuccess()
      }
      is LoginViewModel.LoginState.Error -> {
        snackbarHostState.showSnackbar(state.message.ifBlank { errorMessage })
        viewModel.resetState()
      }
      else -> Unit
    }
  }
  /** Executa a rotina de attempt login dentro do contexto deste componente. */
  fun attemptLogin() {
    viewModel.login(email, password)
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

        Spacer(modifier = Modifier.height(24.dp))
      }

      // Overlay de carregamento cobre toda a tela durante o login
      LoadingOverlay(
          isVisible = isLoading,
          message = stringResource(R.string.login_loading),
      )
    }
  }
}

// ── Logo section ──────────────────────────────────────────────────────────────
/** Seção de logo do aplicativo, usando o ícone do launcher. */
@Composable
private fun LogoSection() {
  val context = LocalContext.current
  val density = LocalDensity.current
  // Obtém o ícone do launcher via PackageManager — funciona com adaptive icons (API 26+)
  // sem depender de painterResource, que não suporta drawables adaptativos.
  val iconBitmap =
      remember(context, density) {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val sizePx = with(density) { 80.dp.roundToPx() }
        val bmp =
            android.graphics.Bitmap.createBitmap(
                sizePx,
                sizePx,
                android.graphics.Bitmap.Config.ARGB_8888,
            )
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        bmp.asImageBitmap()
      }
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Image(
        bitmap = iconBitmap,
        contentDescription = null,
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)),
    )
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
/**
 * Executa a rotina de login form card dentro do contexto deste componente.
 *
 * @param email Valor de entrada utilizado por esta opera??o.
 * @param onEmailChange Valor de entrada utilizado por esta opera??o.
 */
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
          keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
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
              if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

/**
 * Executa a rotina de credential row dentro do contexto deste componente.
 *
 * @param role Valor de entrada utilizado por esta opera??o.
 * @param email Valor de entrada utilizado por esta opera??o.
 */
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
/** Executa a rotina de login screen preview dentro do contexto deste componente. */
@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
  AppDesafioSEBRAETheme { LoginScreen(onLoginSuccess = {}) }
}
