package tech.datatower.sebrae.desafio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import tech.datatower.sebrae.desafio.data.model.AppSettings
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppNavHost
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme
import javax.inject.Inject

/**
 * Activity de entrada da aplicação.
 *
 * Responsável por configurar o conteúdo Compose, aplicar o tema global e iniciar o host de
 * navegação principal.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var repository: AppRepository

  /**
   * Inicializa a UI declarativa da aplicação e conecta o `NavController` ao `AppNavHost`.
   *
   * @param savedInstanceState Estado previamente salvo pelo sistema Android.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settings by
          repository
              .observeSettings()
              .collectAsState(
                  initial =
                      AppSettings(
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
