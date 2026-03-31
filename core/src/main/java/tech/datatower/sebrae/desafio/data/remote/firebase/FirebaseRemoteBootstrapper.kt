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
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.remote.RemoteBootstrapper

/**
 * Foundation adapter for future Firestore bootstrap.
 *
 * Current behavior is intentionally conservative: until Firebase is fully configured
 * (`google-services.json`, plugin and environment sync), this adapter keeps local-first mode.
 */
class FirebaseRemoteBootstrapper(
    private val dao: AppDao,
) : RemoteBootstrapper {
  /**
   * Executa a rotina de bootstrap into local cache dentro do contexto deste componente.
   *
   * @return Resultado produzido pela operação em formato `Boolean`.
   */
  override suspend fun bootstrapIntoLocalCache(): Boolean {
    val localHasData = dao.countCourses(companyId = 0) > 0
    val shouldHydrateFromRemote = !localHasData
    // Foundation step: keep the contract and lifecycle in place without forcing runtime setup.
    // The next increment should fetch Firestore collections and upsert into Room tables.
    if (shouldHydrateFromRemote) return false
    return false
  }

  companion object {
    /**
     * Cria um novo recurso seguindo as regras de domínio.
     *
     * @param _context Valor de entrada utilizado por esta operação.
     * @param dao Valor de entrada utilizado por esta operação.
     * @return Resultado produzido pela operação em formato `RemoteBootstrapper`.
     */
    @Suppress("UNUSED_PARAMETER")
    fun create(_context: Context, dao: AppDao): RemoteBootstrapper {
      return FirebaseRemoteBootstrapper(dao = dao)
    }
  }
}
