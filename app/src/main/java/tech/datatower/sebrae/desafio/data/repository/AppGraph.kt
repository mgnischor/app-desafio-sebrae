package tech.datatower.sebrae.desafio.data.repository

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.local.AppDatabase

object AppGraph {
  @Volatile private var repository: AppRepository? = null

  fun repository(context: Context): AppRepository {
    return repository
        ?: synchronized(this) {
          repository ?: buildRepository(context.applicationContext).also { repository = it }
        }
  }

  private fun buildRepository(context: Context): AppRepository {
    val database =
        Room.databaseBuilder(context, AppDatabase::class.java, "sebrae_local.db")
            .fallbackToDestructiveMigration()
            .build()

    val repo = AppRepository(database = database, dao = database.appDao())
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repo.seedIfEmpty() }
    return repo
  }
}
