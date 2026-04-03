/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/remote/firebase/FirebaseRemoteBootstrapper.kt
    Descrição: Implementação Firebase do bootstrapper remoto para inicialização de dados.
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
package tech.datatower.sebrae.desafio.data.remote.firebase

import android.content.Context
import android.util.Log
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.remote.RemoteBootstrapper

/**
 * Implementação Firebase do bootstrapper remoto.
 *
 * Verifica se o cache local está vazio e, em caso afirmativo, hidrata todas as coleções do
 * Firestore para o Room via [FirebaseDataConnectService.syncAllData]. Retorna `true` quando a
 * hidratação inicial ocorreu com sucesso, `false` quando o cache já possuía dados ou quando a
 * sincronização remota falhou (fallback local-first).
 */
class FirebaseRemoteBootstrapper(
    private val dao: AppDao,
    private val dataConnectService: FirebaseDataConnectService,
) : RemoteBootstrapper {

  /**
   * Executa a rotina de bootstrap no cache local.
   *
   * Verifica a presença de dados locais somando o total de cursos, alunos e turmas. Se o cache
   * estiver vazio, dispara [FirebaseDataConnectService.syncAllData] para buscar todos os dados do
   * Firestore e populá-los no Room.
   *
   * @return `true` se a hidratação remota foi executada com sucesso; `false` caso o cache já
   *   tivesse dados ou a sincronização tenha falhado.
   */
  override suspend fun bootstrapIntoLocalCache(): Boolean {
    val localHasData = dao.getStudentsOnce().isNotEmpty() || dao.getClassesOnce().isNotEmpty()

    if (localHasData) {
      Log.d(TAG, "Cache local já possui dados. Bootstrap ignorado.")
      return false
    }

    Log.d(TAG, "Cache local vazio. Iniciando hydration do Firestore...")
    return when (val result = dataConnectService.syncAllData()) {
      is FirebaseDataConnectService.Result.Success -> {
        Log.d(TAG, "Hydration inicial do Firestore concluída com sucesso.")
        true
      }
      is FirebaseDataConnectService.Result.Error -> {
        Log.w(TAG, "Hydration falhou; app continuará em modo local-first. ${result.message}")
        false
      }
      FirebaseDataConnectService.Result.Loading -> false
    }
  }

  companion object {
    private const val TAG = "RemoteBootstrapper"

    /**
     * Cria uma instância de [FirebaseRemoteBootstrapper] com as dependências necessárias.
     *
     * @param _context Contexto da aplicação (reservado para uso futuro).
     * @param dao DAO local do Room.
     * @param dataConnectService Serviço de sincronização Firebase → Room.
     * @return Instância configurada de [RemoteBootstrapper].
     */
    @Suppress("UNUSED_PARAMETER")
    fun create(
        _context: Context,
        dao: AppDao,
        dataConnectService: FirebaseDataConnectService,
    ): RemoteBootstrapper {
      return FirebaseRemoteBootstrapper(dao = dao, dataConnectService = dataConnectService)
    }
  }
}
