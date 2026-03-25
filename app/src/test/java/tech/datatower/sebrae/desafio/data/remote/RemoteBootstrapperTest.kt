package tech.datatower.sebrae.desafio.data.remote

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Testes unitários do contrato de bootstrap remoto.
 *
 * Valida comportamento padrão e fallback de estratégias de carregamento remoto.
 *
 * Padrão: Arrange → Act → Assert (AAA)
 */
class RemoteBootstrapperTest {

  /**
   * Valida que o bootstrapper no-op sempre retorna false.
   *
   * Regra: Fallback seguro para quando nenhuma fonte remota está disponível.
   */
  @Test
  fun `NoOpRemoteBootstrapper deve sempre retornar false`() {
    // Arrange
    val bootstrapper: RemoteBootstrapper = NoOpRemoteBootstrapper

    // Act
    val resultado = runBlocking { bootstrapper.bootstrapIntoLocalCache() }

    // Assert
    assertFalse("NoOp bootstrapper deve sempre retornar false", resultado)
  }

  companion object {
    /**
     * Executa a rotina de run blocking dentro do contexto deste componente.
     *
     * @param block Valor de entrada utilizado por esta operação.
     * @return Resultado produzido pela operação em formato `Boolean`.
     */
    private fun runBlocking(block: suspend () -> Boolean): Boolean {
      return kotlinx.coroutines.runBlocking { block() }
    }
  }
}
