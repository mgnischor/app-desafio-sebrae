package tech.datatower.sebrae.desafio.data.model

/**
 * Níveis de permissão dos usuários do sistema.
 *
 * Cada perfil define quais módulos e informações são acessíveis na aplicação.
 */
enum class UserRole {
  /** Acesso restrito: Alunos, Calendário e Certificados. */
  PROFESSOR,

  /**
   * Acesso intermediário: Alunos, Cursos, Turmas, Instrutores, Calendário, Certificados e
   * Relatórios.
   */
  COORDENADOR,

  /** Acesso completo a todos os módulos, incluindo Configurações. */
  ADMINISTRADOR,
}
