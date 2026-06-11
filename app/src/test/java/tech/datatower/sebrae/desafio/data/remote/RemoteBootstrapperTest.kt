/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/remote/RemoteBootstrapperTest.kt
    Descrição: Testes unitários do contrato de bootstrap remoto e estratégias de fallback.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
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
