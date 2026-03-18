package tech.datatower.sebrae.desafio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
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
      AppDesafioSEBRAETheme {
        val navController = rememberNavController()
        AppNavHost(navController = navController)
      }
    }
  }
}
