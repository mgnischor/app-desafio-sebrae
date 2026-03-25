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
   * @return Resultado produzido pela opera??o em formato `Boolean`.
   */
  override suspend fun bootstrapIntoLocalCache(): Boolean {
    val localHasData = dao.countCourses() > 0
    val shouldHydrateFromRemote = !localHasData
    // Foundation step: keep the contract and lifecycle in place without forcing runtime setup.
    // The next increment should fetch Firestore collections and upsert into Room tables.
    if (shouldHydrateFromRemote) return false
    return false
  }

  companion object {
    /**
     * Cria um novo recurso para seguindo as regras de dom?nio.
     *
     * @param _context Valor de entrada utilizado por esta opera??o.
     * @param dao Valor de entrada utilizado por esta opera??o.
     * @return Resultado produzido pela opera??o em formato `RemoteBootstrapper`.
     */
    @Suppress("UNUSED_PARAMETER")
    fun create(_context: Context, dao: AppDao): RemoteBootstrapper {
      return FirebaseRemoteBootstrapper(dao = dao)
    }
  }
}
