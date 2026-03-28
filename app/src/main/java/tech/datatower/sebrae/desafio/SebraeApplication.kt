/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/SebraeApplication.kt
    Descrição: Classe Application da aplicação, responsável por inicializar o Hilt e o
               bootstrapper remoto.
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

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import tech.datatower.sebrae.desafio.data.repository.AppGraph

/** Ponto de entrada da aplicação, responsável por inicializar o Hilt e o bootstrapper remoto. */
@HiltAndroidApp
class SebraeApplication : Application() {
  /** Trata o evento de create no contexto da aplicação. */
  override fun onCreate() {
    super.onCreate()
    AppGraph.warmUp(this)
  }
}
