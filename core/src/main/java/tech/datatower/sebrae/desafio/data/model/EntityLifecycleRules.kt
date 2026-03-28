/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/EntityLifecycleRules.kt
    Descrição: Regras de ciclo de vida para entidades gerenciadas pela aplicação.
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
