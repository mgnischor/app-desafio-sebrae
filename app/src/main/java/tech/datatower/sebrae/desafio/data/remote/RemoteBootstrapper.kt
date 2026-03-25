package tech.datatower.sebrae.desafio.data.remote

/**
 * Contract for bootstrapping local cache from a remote backend.
 *
 * Returns `true` when remote data was fetched and written to local storage.
 */
interface RemoteBootstrapper {
  /**
   * Executa a rotina de bootstrap into local cache dentro do contexto deste componente.
   *
   * @return Resultado produzido pela opera??o em formato `Boolean } /** Fallback bootstrapper used
   *   when no remote backend is configured. */ object NoOpRemoteBootstrapper : RemoteBootstrapper`.
   */
  suspend fun bootstrapIntoLocalCache(): Boolean
}

/** Fallback bootstrapper used when no remote backend is configured. */
object NoOpRemoteBootstrapper : RemoteBootstrapper {
  /**
   * Executa a rotina de bootstrap into local cache dentro do contexto deste componente.
   *
   * @return Resultado produzido pela opera??o em formato `Boolean`.
   */
  override suspend fun bootstrapIntoLocalCache(): Boolean = false
}
