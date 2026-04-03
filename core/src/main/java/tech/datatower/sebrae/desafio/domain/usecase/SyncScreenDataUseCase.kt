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
