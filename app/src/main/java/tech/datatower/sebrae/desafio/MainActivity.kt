package tech.datatower.sebrae.desafio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import tech.datatower.sebrae.desafio.navigation.AppNavHost
import tech.datatower.sebrae.desafio.ui.theme.AppDesafioSEBRAETheme

class MainActivity : ComponentActivity() {
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
