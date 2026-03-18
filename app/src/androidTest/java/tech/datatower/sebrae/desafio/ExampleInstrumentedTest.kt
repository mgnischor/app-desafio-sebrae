package tech.datatower.sebrae.desafio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exemplo de teste instrumentado executado em dispositivo ou emulador Android.
 *
 * Valida a obtenção do contexto de aplicação do app em execução.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  /** Verifica se o nome de pacote retornado pelo contexto corresponde ao aplicativo. */
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("tech.datatower.sebrae.desafio", appContext.packageName)
  }
}
