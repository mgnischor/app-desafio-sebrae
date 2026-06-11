/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/auth/AccessPolicyTest.kt
    Descrição: Testes unitários para as regras de RBAC centralizadas em AccessPolicy.
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
