package tech.datatower.sebrae.desafio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import tech.datatower.sebrae.desafio.data.repository.AppGraph
import tech.datatower.sebrae.desafio.navigation.AppNavHost
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

/**
 * Activity de entrada da aplicação.
 *
 * Responsável por configurar o conteúdo Compose, aplicar o tema global e iniciar o host de
 * navegação principal.
 */
class MainActivity : ComponentActivity() {
  /**
   * Inicializa a UI declarativa da aplicação e conecta o `NavController` ao `AppNavHost`.
   *
   * @param savedInstanceState Estado previamente salvo pelo sistema Android.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
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

      AppDesafioSEBRAETheme(darkTheme = settings.darkMode) {
        val navController = rememberNavController()
        AppNavHost(navController = navController)
      }
    }
  }
}
