package tech.datatower.sebrae.desafio.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.AppDatabase
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseSeedCredentialStore
import tech.datatower.sebrae.desafio.data.remote.NoOpRemoteBootstrapper
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseRemoteBootstrapper

object AppGraph {
  private const val TAG = "AppGraph"
  private const val FIREBASE_REMOTE_BOOTSTRAP_ENABLED = true
  private const val FIREBASE_AUTO_SEED_ALL_COLLECTIONS_FROM_LOCAL = true
  private val graphScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Volatile private var repository: AppRepository? = null
  @Volatile private var dataConnectService: FirebaseDataConnectService? = null
  @Volatile private var seedCredentialStore: FirebaseSeedCredentialStore? = null
  @Volatile private var appDaoRef: AppDao? = null

  fun repository(context: Context): AppRepository {
    return repository
        ?: synchronized(this) {
          repository ?: buildRepository(context.applicationContext).also { repository = it }
        }
  }

  /**
   * Fornece instância singleton do serviço Firebase Data Connect.
   *
   * Inicializa o Firestore e cria o serviço na primeira chamada.
   */
  fun dataConnectService(context: Context, dao: AppDao): FirebaseDataConnectService {
    appDaoRef = dao
    return dataConnectService
        ?: synchronized(this) {
          val secureStore =
              seedCredentialStore
                  ?: FirebaseSeedCredentialStore(context.applicationContext).also {
                    seedCredentialStore = it
                  }

          dataConnectService
              ?: FirebaseDataConnectService(
                  firestore = FirebaseFirestore.getInstance(),
                  dao = dao,
                  credentialStore = secureStore,
              )
                  .also { dataConnectService = it }
        }
  }

  fun dataConnectService(context: Context): FirebaseDataConnectService {
    val dao = appDaoRef ?: run {
      repository(context)
      appDaoRef
    }
    checkNotNull(dao) { "AppDao indisponivel para inicializar FirebaseDataConnectService." }
    return dataConnectService(context, dao)
  }

  /** Preloads heavy graph resources (Room + bootstrap flows) before first composition. */
  fun warmUp(context: Context) {
    graphScope.launch { repository(context) }
  }

  private fun buildRepository(context: Context): AppRepository {
    val database =
        Room.databaseBuilder(context, AppDatabase::class.java, "sebrae_local.db")
            .fallbackToDestructiveMigration(true)
            .build()

    val sourceLabelResFlow = MutableStateFlow(R.string.stat_data_source)
    val dao = database.appDao()
    appDaoRef = dao
    val remoteBootstrapper =
        if (FIREBASE_REMOTE_BOOTSTRAP_ENABLED) {
          FirebaseRemoteBootstrapper.create(context, dao)
        } else {
          NoOpRemoteBootstrapper
        }

    val repo =
        AppRepository(
            database = database,
            dao = dao,
            dataSourceLabelResFlow = sourceLabelResFlow,
        )

    graphScope.launch {
      repo.seedIfEmpty()

      if (FIREBASE_AUTO_SEED_ALL_COLLECTIONS_FROM_LOCAL) {
        when (
            val seedResult =
                dataConnectService(context = context, dao = dao).seedAllCollectionsFromLocalCache()) {
          is FirebaseDataConnectService.Result.Success ->
              Log.d(TAG, "Seed automatico de collections concluido: ${seedResult.data}")
          is FirebaseDataConnectService.Result.Error ->
              Log.e(TAG, "Falha no seed automatico de collections: ${seedResult.message}")
          FirebaseDataConnectService.Result.Loading -> Unit
        }
      }

      val hasRemoteData = remoteBootstrapper.bootstrapIntoLocalCache()
      if (hasRemoteData) {
        sourceLabelResFlow.value = R.string.stat_data_source
      }
    }

    return repo
  }
}
