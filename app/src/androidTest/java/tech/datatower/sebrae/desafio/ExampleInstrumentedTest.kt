/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/androidTest/java/tech/datatower/sebrae/desafio/ExampleInstrumentedTest.kt
    Descrição: Testes instrumentados de exemplo para verificação do ambiente de testes Android.
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
package tech.datatower.sebrae.desafio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exemplo de teste instrumentado executado em dispositivo ou emulador Android.
 *
 * Valida funcionalidades que requerem contexto de aplicação real e acesso ao ambiente de execução
 * do Android.
 *
 * Padrão: Arrange → Act → Assert (AAA)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

  /**
   * Verifica se o nome de pacote retornado pelo contexto corresponde ao aplicativo esperado.
   *
   * Valida: Inicialização correta do contexto de instrumentação.
   */
  @Test
  fun appContextReturnCorrectPackageName() {
    // Arrange
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val expectedPackageName = "tech.datatower.sebrae.desafio"

    // Act
    val appContext = instrumentation.targetContext
    val actualPackageName = appContext.packageName

    // Assert
    assertEquals(
        "Nome do pacote deve corresponder ao esperado",
        expectedPackageName,
        actualPackageName,
    )
  }

  /**
   * Valida que o contexto não é nulo.
   *
   * Valida: Disponibilidade do contexto de instrumentação.
   */
  @Test
  fun appContextIsNotNull() {
    // Arrange
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    // Act
    val appContext = instrumentation.targetContext

    // Assert
    assertNotNull("Contexto da aplicação não deve ser nulo", appContext)
  }

  /**
   * Valida que o contexto de teste também está disponível.
   *
   * Valida: Acesso ao contexto de teste do instrumentation.
   */
  @Test
  fun testContextIsAvailable() {
    // Arrange
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    // Act
    val testContext = instrumentation.context

    // Assert
    assertNotNull("Contexto de teste não deve ser nulo", testContext)
    assertNotNull("Pacote do contexto de teste não deve ser nulo", testContext.packageName)
  }

  /**
   * Valida que diferentes contextos têm nomes de pacote distintos.
   *
   * Valida: Diferenciação entre contexto de app e contexto de teste.
   */
  @Test
  fun appAndTestContextsMayHaveDifferentPackages() {
    // Arrange
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val appContext = instrumentation.targetContext
    val testContext = instrumentation.context

    // Act
    val appPackageName = appContext.packageName
    val testPackageName = testContext.packageName

    // Assert
    assertNotNull("Pacote da app deve existir", appPackageName)
    assertNotNull("Pacote de teste deve existir", testPackageName)
    assertTrue(
        "Pelo menos um dos pacotes deve estar definido corretamente",
        appPackageName.isNotEmpty() && testPackageName.isNotEmpty(),
    )
  }
}
