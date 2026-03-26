package tech.datatower.sebrae.desafio.data.model

/** Regras de ciclo de vida para operações de desativação e reativação das entidades. */
object EntityLifecycleRules {

  /** Retorna aluno desativado com status inativo. */
  fun deactivateStudent(student: Student): Student = student.copy(status = StudentStatus.Inactive)

  /** Retorna aluno reativado com status ativo. */
  fun reactivateStudent(student: Student): Student = student.copy(status = StudentStatus.Active)

  /** Retorna curso desativado (não publicado). */
  fun deactivateCourse(course: Course): Course = course.copy(isPublished = false)

  /** Retorna curso reativado (publicado). */
  fun reactivateCourse(course: Course): Course = course.copy(isPublished = true)

  /** Retorna turma desativada com status encerrado. */
  fun deactivateClass(schoolClass: SchoolClass): SchoolClass =
      schoolClass.copy(status = ClassStatus.Closed)

  /** Retorna turma reativada com status aberta. */
  fun reactivateClass(schoolClass: SchoolClass): SchoolClass =
      schoolClass.copy(status = ClassStatus.Open)

  /** Retorna instrutor desativado. */
  fun deactivateTeacher(teacher: Teacher): Teacher = teacher.copy(isActive = false)

  /** Retorna instrutor reativado. */
  fun reactivateTeacher(teacher: Teacher): Teacher = teacher.copy(isActive = true)
}
