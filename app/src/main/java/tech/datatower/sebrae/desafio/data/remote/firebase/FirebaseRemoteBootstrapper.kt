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

  override suspend fun bootstrapIntoLocalCache(): Boolean {
    val localHasData = dao.countCourses() > 0
    val shouldHydrateFromRemote = !localHasData
    // Foundation step: keep the contract and lifecycle in place without forcing runtime setup.
    // The next increment should fetch Firestore collections and upsert into Room tables.
    if (shouldHydrateFromRemote) return false
    return false
  }

  companion object {
    @Suppress("UNUSED_PARAMETER")
    fun create(_context: Context, dao: AppDao): RemoteBootstrapper {
      return FirebaseRemoteBootstrapper(dao = dao)
    }
  }
}
