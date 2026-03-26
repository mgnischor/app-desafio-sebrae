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
