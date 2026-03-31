/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/auth/AccessPolicy.kt
    Descrição: Políticas de controle de acesso baseadas em papéis de usuário.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
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

import tech.datatower.sebrae.desafio.data.model.UserRole

/** Recursos funcionais protegidos por controle de acesso baseado em perfil. */
enum class ProtectedResource {
  Students,
  Courses,
  Classes,
  Teachers,
  Users,
  Calendar,
  Settings,
  Companies,
}

/** Ações suportadas pelas regras de autorização da aplicação. */
enum class ProtectedAction {
  View,
  Create,
  Update,
  Deactivate,
  Reactivate,
  Delete,
  LaunchStudentRecord,
  ClearStorage,
  ResetDatabase,
  ChangeOwnPassword,
}

/**
 * Centraliza as decisões de autorização para reduzir lógica duplicada de permissões.
 *
 * As regras seguem princípio de privilégio mínimo e devem ser aplicadas tanto na UI quanto em
 * serviços de persistência remota/local.
 */
object AccessPolicy {

  /**
   * Verifica se um perfil possui permissão para executar uma ação em determinado recurso.
   *
   * @param role Perfil do usuário autenticado.
   * @param resource Recurso funcional alvo da ação.
   * @param action Operação pretendida.
   * @return `true` quando a operação é permitida para o perfil informado.
   */
  fun can(role: UserRole?, resource: ProtectedResource, action: ProtectedAction): Boolean {
    if (role == null) return false

    return when (role) {
      UserRole.ADMINISTRADOR -> true
      UserRole.COORDENADOR -> coordinatorRules(resource, action)
      UserRole.PROFESSOR -> professorRules(resource, action)
    }
  }

  private fun coordinatorRules(resource: ProtectedResource, action: ProtectedAction): Boolean {
    return when (resource) {
      ProtectedResource.Students,
      ProtectedResource.Courses,
      ProtectedResource.Classes,
      ProtectedResource.Teachers -> {
        action in
            setOf(
                ProtectedAction.View,
                ProtectedAction.Create,
                ProtectedAction.Update,
                ProtectedAction.Deactivate,
                ProtectedAction.Reactivate,
                ProtectedAction.LaunchStudentRecord,
            )
      }
      ProtectedResource.Calendar -> action in setOf(ProtectedAction.View, ProtectedAction.Create)
      ProtectedResource.Settings ->
          action in setOf(ProtectedAction.View, ProtectedAction.ChangeOwnPassword)
      ProtectedResource.Users -> false
      ProtectedResource.Companies -> false
    }
  }

  private fun professorRules(resource: ProtectedResource, action: ProtectedAction): Boolean {
    return when (resource) {
      ProtectedResource.Courses,
      ProtectedResource.Classes -> action == ProtectedAction.View
      ProtectedResource.Students ->
          action in setOf(ProtectedAction.View, ProtectedAction.LaunchStudentRecord)
      ProtectedResource.Calendar -> action == ProtectedAction.View
      ProtectedResource.Settings ->
          action in setOf(ProtectedAction.View, ProtectedAction.ChangeOwnPassword)
      ProtectedResource.Teachers,
      ProtectedResource.Users,
      ProtectedResource.Companies -> false
    }
  }
}
