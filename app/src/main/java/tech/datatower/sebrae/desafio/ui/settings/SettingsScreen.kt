package tech.datatower.sebrae.desafio.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
 * Tela de configurações da aplicação.
 *
 * Agrupa preferências de conta, aparência, notificações e sistema.
 *
 * @param onBack Ação para retornar à tela anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: AppUser? = null,
    onBack: () -> Unit = {},
    onOpenUserManagement: () -> Unit = {},
) {
  val context = LocalContext.current
  val repository = remember(context) { AppGraph.repository(context.applicationContext) }
  val settings by
      repository
          .observeSettings()
          .collectAsState(
              initial =
                  tech.datatower.sebrae.desafio.data.model.AppSettings(
                      darkMode = false,
                      pushEnabled = true,
                      emailEnabled = false,
                      language = "pt",
                  )
          )

  var darkMode by remember { mutableStateOf(settings.darkMode) }
  var pushEnabled by remember { mutableStateOf(settings.pushEnabled) }
  var emailEnabled by remember { mutableStateOf(settings.emailEnabled) }
  var language by remember { mutableStateOf(settings.language) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(settings) {
    darkMode = settings.darkMode
    pushEnabled = settings.pushEnabled
    emailEnabled = settings.emailEnabled
    language = settings.language
  }

  DetailScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { innerPadding, _
    ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { SettingsSectionHeader(stringResource(R.string.settings_section_account)) }
      item {
        SettingsGroup {
          SettingsItem(
              icon = Icons.Outlined.PersonOutline,
              title = stringResource(R.string.settings_item_profile),
              subtitle = stringResource(R.string.settings_subtitle_profile),
          )
          SettingsItem(
              icon = Icons.Outlined.Lock,
              title = stringResource(R.string.settings_item_security),
              subtitle = stringResource(R.string.settings_subtitle_security),
          )
          if (currentUser?.role == UserRole.ADMINISTRADOR) {
            SettingsItem(
                icon = Icons.Outlined.ManageAccounts,
                title = "Usuários",
                subtitle = "Cadastro e controle de acesso",
                onClick = onOpenUserManagement,
            )
          }
        }
      }

      item { SettingsSectionHeader(stringResource(R.string.settings_section_appearance)) }
      item {
        SettingsGroup {
          SettingsToggleItem(
              icon = Icons.Outlined.DarkMode,
              title = stringResource(R.string.settings_item_dark_mode),
              subtitle = stringResource(R.string.settings_subtitle_dark_mode),
              checked = darkMode,
              onCheckedChange = {
                darkMode = it
                scope.launch { repository.updateDarkMode(it) }
              },
          )
          SettingsItem(
              icon = Icons.Outlined.ColorLens,
              title = stringResource(R.string.settings_item_color_theme),
              subtitle = stringResource(R.string.settings_subtitle_color_theme),
          )
        }
      }

      item { SettingsSectionHeader(stringResource(R.string.settings_section_notifications)) }
      item {
        SettingsGroup {
          SettingsToggleItem(
              icon = Icons.Outlined.Notifications,
              title = stringResource(R.string.settings_item_push),
              subtitle = stringResource(R.string.settings_subtitle_push),
              checked = pushEnabled,
              onCheckedChange = {
                pushEnabled = it
                scope.launch { repository.updatePushEnabled(it) }
              },
          )
          SettingsToggleItem(
              icon = Icons.Outlined.Notifications,
              title = stringResource(R.string.settings_item_email_notif),
              subtitle = stringResource(R.string.settings_subtitle_email_notif),
              checked = emailEnabled,
              onCheckedChange = {
                emailEnabled = it
                scope.launch { repository.updateEmailEnabled(it) }
              },
          )
        }
      }

      item { SettingsSectionHeader(stringResource(R.string.settings_section_system)) }
      item {
        SettingsGroup {
          LanguageSelectionItem(
              selectedLanguage = language,
              onLanguageSelected = {
                language = it
                scope.launch { repository.updateLanguage(it) }
              },
          )
          SettingsItem(
              icon = Icons.Outlined.Storage,
              title = stringResource(R.string.settings_item_storage),
              subtitle = stringResource(R.string.settings_subtitle_storage),
          )
          SettingsItem(
              icon = Icons.Outlined.Info,
              title = stringResource(R.string.settings_item_about),
              subtitle = stringResource(R.string.settings_subtitle_about),
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

/** Item para seleção de idioma com opções de português, inglês e espanhol. */
@Composable
private fun LanguageSelectionItem(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
) {
  val languages = listOf("pt", "en", "es")
  val languageNames = listOf("Português", "English", "Español")

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
    val selectedIndex = languages.indexOf(selectedLanguage).coerceAtLeast(0)
    Row(
        modifier =
            Modifier.fillMaxWidth().clickable {
              val nextIndex = (selectedIndex + 1) % languages.size
              onLanguageSelected(languages[nextIndex])
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Outlined.Language,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp),
      )
      Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
        Text(
            text = stringResource(R.string.settings_item_language),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = languageNames[selectedIndex],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/**
 * Cabeçalho textual utilizado para separar grupos de configurações.
 *
 * @param text Texto do cabeçalho da seção.
 */
@Composable
private fun SettingsSectionHeader(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 4.dp),
  )
}

/**
 * Contêiner visual de um grupo de configurações relacionadas.
 *
 * @param content Conteúdo composable dos itens do grupo.
 */
@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
  ) {
    Column { content() }
  }
}

/**
 * Item de configuração informativo sem controle de alternância.
 *
 * @param icon Ícone representativo da opção.
 * @param title Título da opção.
 * @param subtitle Descrição complementar da opção.
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(enabled = onClick != null) { onClick?.invoke() }
              .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
    )
    Column(modifier = Modifier.padding(start = 16.dp)) {
      Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/**
 * Item de configuração com alternância booleana.
 *
 * @param icon Ícone representativo da opção.
 * @param title Título da opção.
 * @param subtitle Descrição complementar da opção.
 * @param checked Estado atual do interruptor.
 * @param onCheckedChange Callback disparado ao alternar o valor.
 */
@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
    )
    Column(
        modifier = Modifier.weight(1f).padding(start = 16.dp),
    ) {
      Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
  }
}

/** Pré-visualização da tela de configurações. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsPreview() {
  AppDesafioSEBRAETheme { SettingsScreen() }
}
