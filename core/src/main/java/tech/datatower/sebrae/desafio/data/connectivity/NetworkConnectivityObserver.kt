/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/connectivity/NetworkConnectivityObserver.kt
    Descrição: Implementação do observador de conectividade baseada no ConnectivityManager do Android.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
package tech.datatower.sebrae.desafio.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observa mudanças de conectividade usando o [ConnectivityManager] do Android.
 *
 * Emite [ConnectivityObserver.Status] sempre que o estado de rede muda, começando pelo estado atual
 * no momento da assinatura.
 */
class NetworkConnectivityObserver(
    context: Context,
) : ConnectivityObserver {

  private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  override fun isConnected(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  override fun observe(): Flow<ConnectivityObserver.Status> {
    return callbackFlow {
          val request =
              NetworkRequest.Builder()
                  .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                  .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                  .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                  .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                  .build()

          val callback =
              object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                  trySend(ConnectivityObserver.Status.Available)
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                  trySend(ConnectivityObserver.Status.Losing)
                }

                override fun onLost(network: Network) {
                  trySend(ConnectivityObserver.Status.Lost)
                }

                override fun onUnavailable() {
                  trySend(ConnectivityObserver.Status.Unavailable)
                }
              }

          // Emit current state immediately
          val initialStatus =
              if (isConnected()) ConnectivityObserver.Status.Available
              else ConnectivityObserver.Status.Unavailable
          trySend(initialStatus)

          connectivityManager.registerNetworkCallback(request, callback)

          awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }
        .distinctUntilChanged()
  }
}
