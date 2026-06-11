/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/model/RealtimeNotificationRulesTest.kt
    Descrição: Testes unitários das regras de notificação em tempo real do backoffice.
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
package tech.datatower.sebrae.desafio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes unitários das regras de notificação em tempo real do backoffice. */
class RealtimeNotificationRulesTest {

  /** Garante que apenas Administrador e Coordenador recebam notificações de backoffice. */
  @Test
  fun `should allow realtime notifications only for admin and coordinator`() {
    assertTrue(RealtimeNotificationRules.canReceiveBackofficeNotifications(UserRole.ADMINISTRADOR))
    assertTrue(RealtimeNotificationRules.canReceiveBackofficeNotifications(UserRole.COORDENADOR))
    assertFalse(RealtimeNotificationRules.canReceiveBackofficeNotifications(UserRole.PROFESSOR))
  }

  /** Garante montagem correta do rascunho para evento de criação. */
  @Test
  fun `should build created draft with expected title and icon`() {
    val draft =
        RealtimeNotificationRules.buildDraft(
            action = MutationAction.Created,
            entityName = "Aluno",
            targetLabel = "Ana Lima",
        )

    assertEquals("Aluno cadastrado", draft.title)
    assertEquals("Ana Lima", draft.subtitle)
    assertEquals("person", draft.iconKey)
    assertEquals("agora", draft.timeLabel)
  }

  /** Garante montagem correta do rascunho para exclusão. */
  @Test
  fun `should build deleted draft with expected title`() {
    val draft =
        RealtimeNotificationRules.buildDraft(
            action = MutationAction.Deleted,
            entityName = "Curso",
            targetLabel = "Marketing Digital",
        )

    assertEquals("Curso excluído", draft.title)
    assertEquals("course", draft.iconKey)
  }
}
