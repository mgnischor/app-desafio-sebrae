/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/navigation/AppRoutes.kt
    Descrição: Definição das rotas de navegação e seus argumentos de passagem de parâmetros.
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
package tech.datatower.sebrae.desafio.navigation

/**
 * Catálogo central das rotas nomeadas usadas pelo `NavHost` da aplicação.
 *
 * Manter as rotas em um único ponto reduz risco de inconsistências e facilita refatorações de
 * navegação.
 */
object AppRoutes {
  /** Nome do argumento obrigatório com o identificador do aluno na rota de acompanhamento. */
  const val STUDENT_ID_ARG = "studentId"
  const val COURSE_ID_ARG = "courseId"
  const val CLASS_ID_ARG = "classId"
  const val TEACHER_ID_ARG = "teacherId"
  const val USER_ID_ARG = "userId"

  /** Rota da tela de login. */
  const val LOGIN = "login"

  /** Rota da tela inicial com visão geral e atalhos dos módulos. */
  const val HOME = "home"

  /** Rota da tela com histórico completo de atividades recentes. */
  const val RECENT_ACTIVITIES = "home/recent-activities"

  /** Rota da tela de gestão de alunos. */
  const val STUDENTS = "students"
  const val STUDENT_CREATE = "students/new"

  /** Rota da tela de acompanhamento individual de alunos. */
  const val STUDENT_MONITORING = "students/{$STUDENT_ID_ARG}/monitoring"

  /** Rota da tela de gestão de cursos. */
  const val COURSES = "courses"
  const val COURSE_CREATE = "courses/new"
  const val COURSE_DETAIL = "courses/{$COURSE_ID_ARG}"

  /** Rota da tela de gestão de turmas. */
  const val CLASSES = "classes"
  const val CLASS_CREATE = "classes/new"
  const val CLASS_DETAIL = "classes/{$CLASS_ID_ARG}"

  /** Rota da tela de gestão de instrutores. */
  const val TEACHERS = "teachers"
  const val TEACHER_CREATE = "teachers/new"
  const val TEACHER_DETAIL = "teachers/{$TEACHER_ID_ARG}"

  /** Rota da tela de relatórios e indicadores. */
  const val REPORTS = "reports"

  /** Rota da tela de certificados emitidos. */
  const val CERTIFICATES = "certificates"

  /** Rota da agenda e calendário de eventos acadêmicos. */
  const val CALENDAR = "calendar"

  /** Rota da tela de configurações da aplicação. */
  const val SETTINGS = "settings"

  /** Rota da tela dedicada de gestão de usuários (admin-only). */
  const val USERS_MANAGEMENT = "users-management"

  /** Rota da tela de perfil de um usuário específico. */
  const val USER_PROFILE = "users/{$USER_ID_ARG}/profile"

  /**
   * Monta a rota de acompanhamento do aluno substituindo o argumento dinâmico.
   *
   * @param studentId Identificador do aluno selecionado.
   * @return Rota final navegável para o detalhe de acompanhamento.
   */
  fun studentMonitoring(studentId: Int): String = "students/$studentId/monitoring"

  /**
   * Executa a rotina de course detail dentro do contexto deste componente.
   *
   * @param courseId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `String`.
   */
  fun courseDetail(courseId: Int): String = "courses/$courseId"

  /**
   * Executa a rotina de class detail dentro do contexto deste componente.
   *
   * @param classId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `String`.
   */
  fun classDetail(classId: Int): String = "classes/$classId"

  /**
   * Executa a rotina de teacher detail dentro do contexto deste componente.
   *
   * @param teacherId Valor de entrada utilizado por esta opera??o.
   * @return Resultado produzido pela opera??o em formato `String`.
   */
  fun teacherDetail(teacherId: Int): String = "teachers/$teacherId"

  /**
   * Monta a rota de perfil de usuário substituindo o argumento dinâmico.
   *
   * @param userId Identificador do usuário selecionado.
   * @return Rota final navegável para o perfil do usuário.
   */
  fun userProfile(userId: Int): String = "users/$userId/profile"
}
