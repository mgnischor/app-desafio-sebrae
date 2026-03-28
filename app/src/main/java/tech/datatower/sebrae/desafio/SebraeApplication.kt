package tech.datatower.sebrae.desafio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import tech.datatower.sebrae.desafio.data.repository.AppGraph

/** Application entrypoint used to pre-warm app dependencies before first UI frame. */
@HiltAndroidApp
class SebraeApplication : Application() {
  /** Trata o evento de create no contexto da tela atual. */
  override fun onCreate() {
    super.onCreate()
    AppGraph.warmUp(this)
  }
}
