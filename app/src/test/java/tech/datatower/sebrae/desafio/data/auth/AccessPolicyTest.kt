package tech.datatower.sebrae.desafio.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.datatower.sebrae.desafio.data.model.UserRole

/** Testes unitários para as regras de RBAC centralizadas em [AccessPolicy]. */
class AccessPolicyTest {

  /** Garante que administrador possua privilégios completos em recursos críticos. */
  @Test
  fun `admin should be allowed to delete users and clear storage`() {
    assertTrue(
        AccessPolicy.can(
            role = UserRole.ADMINISTRADOR,
            resource = ProtectedResource.Users,
            action = ProtectedAction.Delete,
        )
    )
    assertTrue(
        AccessPolicy.can(
            role = UserRole.ADMINISTRADOR,
            resource = ProtectedResource.Settings,
            action = ProtectedAction.ClearStorage,
        )
    )
  }

  /**
   * Garante que coordenador tenha permissão operacional apenas em recursos acadêmicos esperados.
   */
  @Test
  fun `coordinator should update students but not users`() {
    assertTrue(
        AccessPolicy.can(
            role = UserRole.COORDENADOR,
            resource = ProtectedResource.Students,
            action = ProtectedAction.Update,
        )
    )
    assertFalse(
        AccessPolicy.can(
            role = UserRole.COORDENADOR,
            resource = ProtectedResource.Users,
            action = ProtectedAction.Update,
        )
    )
  }

  /** Garante que professor apenas visualize cursos/turmas e registre lançamentos de aluno. */
  @Test
  fun `teacher should view courses and launch student records only`() {
    assertTrue(
        AccessPolicy.can(
            role = UserRole.PROFESSOR,
            resource = ProtectedResource.Courses,
            action = ProtectedAction.View,
        )
    )
    assertTrue(
        AccessPolicy.can(
            role = UserRole.PROFESSOR,
            resource = ProtectedResource.Students,
            action = ProtectedAction.LaunchStudentRecord,
        )
    )
    assertFalse(
        AccessPolicy.can(
            role = UserRole.PROFESSOR,
            resource = ProtectedResource.Courses,
            action = ProtectedAction.Update,
        )
    )
  }
}
