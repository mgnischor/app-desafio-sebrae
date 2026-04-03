/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/MainActivity.kt
    Descrição: Atividade principal da aplicação, responsável por configurar o tema e o host de
               navegação.
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
package tech.datatower.sebrae.desafio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import tech.datatower.sebrae.desafio.data.connectivity.ConnectivityObserver
import tech.datatower.sebrae.desafio.data.model.AppSettings
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppNavHost
import tech.datatower.sebrae.desafio.ui.components.OfflineBanner
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
  @Inject lateinit var connectivityObserver: ConnectivityObserver

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
        val connectivityStatus by
            connectivityObserver
                .observe()
                .collectAsState(initial = ConnectivityObserver.Status.Available)
        val isOffline = connectivityStatus != ConnectivityObserver.Status.Available

        Column(modifier = Modifier.fillMaxSize()) {
          OfflineBanner(isOffline = isOffline)
          AppNavHost(navController = navController)
        }
      }
    }
  }
}
