/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /core/src/main/java/tech/datatower/sebrae/desafio/domain/usecase/SyncScreenDataUseCase.kt
    Descrição: UseCase para sincronizar dados remotos do Firestore com o cache local por escopo de tela.
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
package tech.datatower.sebrae.desafio.domain.usecase

import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para sincronizar dados remotos do Firestore para o cache local de acordo com o escopo de
 * uma tela específica.
 *
 * Encapsula a chamada ao [FirebaseDataConnectService.syncScope], desacoplando os ViewModels da
 * dependência direta ao serviço de sincronização.
 */
@Singleton
class SyncScreenDataUseCase
@Inject
constructor(
    private val dataConnectService: FirebaseDataConnectService,
) {
  suspend operator fun invoke(scope: ScreenDataScope): FirebaseDataConnectService.Result<Boolean> =
      dataConnectService.syncScope(scope)
}
