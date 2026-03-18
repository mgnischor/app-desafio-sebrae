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

  /** Rota da tela inicial com visão geral e atalhos dos módulos. */
  const val HOME = "home"

  /** Rota da tela de gestão de alunos. */
  const val STUDENTS = "students"

  /** Rota da tela de acompanhamento individual de alunos. */
  const val STUDENT_MONITORING = "students/{$STUDENT_ID_ARG}/monitoring"

  /** Rota da tela de gestão de cursos. */
  const val COURSES = "courses"
  const val COURSE_DETAIL = "courses/{$COURSE_ID_ARG}"

  /** Rota da tela de gestão de turmas. */
  const val CLASSES = "classes"
  const val CLASS_DETAIL = "classes/{$CLASS_ID_ARG}"

  /** Rota da tela de gestão de instrutores. */
  const val TEACHERS = "teachers"
  const val TEACHER_DETAIL = "teachers/{$TEACHER_ID_ARG}"

  /** Rota da tela de relatórios e indicadores. */
  const val REPORTS = "reports"

  /** Rota da tela de certificados emitidos. */
  const val CERTIFICATES = "certificates"

  /** Rota da agenda e calendário de eventos acadêmicos. */
  const val CALENDAR = "calendar"

  /** Rota da tela de configurações da aplicação. */
  const val SETTINGS = "settings"

  /**
   * Monta a rota de acompanhamento do aluno substituindo o argumento dinâmico.
   *
   * @param studentId Identificador do aluno selecionado.
   * @return Rota final navegável para o detalhe de acompanhamento.
   */
  fun studentMonitoring(studentId: Int): String = "students/$studentId/monitoring"

  fun courseDetail(courseId: Int): String = "courses/$courseId"

  fun classDetail(classId: Int): String = "classes/$classId"

  fun teacherDetail(teacherId: Int): String = "teachers/$teacherId"
}
