/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/UserRole.kt
    Descrição: Enumeração dos papéis de usuário disponíveis na plataforma.
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
package tech.datatower.sebrae.desafio.data.model

/**
 * Níveis de permissão dos usuários do sistema.
 *
 * Cada perfil define quais módulos e informações são acessíveis na aplicação.
 */
enum class UserRole {
  /** Acesso mínimo: visualiza apenas alunos vinculados e seus dados de acompanhamento. */
  RESPONSAVEL,

  /** Acesso restrito: Alunos, Calendário e Certificados. */
  PROFESSOR,

  /**
   * Acesso educacional: Alunos (qualquer), Calendário, Certificados e lançamento de acompanhamento.
   */
  ORIENTADOR_EDUCACIONAL,

  /**
   * Acesso psicopedagógico: Alunos (qualquer), Calendário, Certificados e acompanhamento completo.
   */
  PSICOPEDAGOGO,

  /**
   * Acesso intermediário: Alunos, Cursos, Turmas, Instrutores, Calendário, Certificados, Relatórios
   * e criação de usuários na empresa vinculada.
   */
  COORDENADOR,

  /** Acesso completo a todos os módulos, incluindo Configurações e importação CSV. */
  ADMINISTRADOR,
}
