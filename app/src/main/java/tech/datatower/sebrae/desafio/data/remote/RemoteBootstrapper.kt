package tech.datatower.sebrae.desafio.data.remote

/**
 * Contract for bootstrapping local cache from a remote backend.
 *
 * Returns `true` when remote data was fetched and written to local storage.
 */
interface RemoteBootstrapper {
  suspend fun bootstrapIntoLocalCache(): Boolean
}

/** Fallback bootstrapper used when no remote backend is configured. */
object NoOpRemoteBootstrapper : RemoteBootstrapper {
  override suspend fun bootstrapIntoLocalCache(): Boolean = false
}

