/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/connectivity/ConnectivityObserver.kt
    Descrição: Observador reativo de estado de conectividade de rede.
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

import kotlinx.coroutines.flow.Flow

/** Contrato para observação reativa do estado de conectividade de rede. */
interface ConnectivityObserver {

  /** Estados possíveis de conectividade. */
  enum class Status {
    Available,
    Unavailable,
    Losing,
    Lost,
  }

  /** Emite mudanças de estado de conectividade como um [Flow]. */
  fun observe(): Flow<Status>

  /** Retorna o estado atual de conectividade de forma síncrona. */
  fun isConnected(): Boolean
}
