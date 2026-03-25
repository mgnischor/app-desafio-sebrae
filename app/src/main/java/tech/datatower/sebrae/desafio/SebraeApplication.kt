package tech.datatower.sebrae.desafio

import android.app.Application
import tech.datatower.sebrae.desafio.data.repository.AppGraph

/**
 * Application entrypoint used to pre-warm app dependencies before first UI frame.
 */
class SebraeApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    AppGraph.warmUp(this)
  }
}

