/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/RelationshipRules.kt
    Descrição: Regras de relacionamento entre entidades do domínio da aplicação.
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

/** Regras de relacionamento entre entidades acadêmicas para manter consistência dos dados. */
object RelationshipRules {

  /**
   * Calcula a contagem real de alunos por curso com base nas matrículas existentes.
   *
   * @param students Lista atual de alunos cadastrados.
   * @return Mapa `nomeDoCurso -> quantidadeRealDeAlunos`.
   */
  fun realStudentsByCourse(students: List<Student>): Map<String, Int> {
    return students.groupingBy { it.course.trim() }.eachCount()
  }

  /**
   * Calcula a contagem real de alunos por turma com base nas matrículas existentes.
   *
   * @param students Lista atual de alunos cadastrados.
   * @return Mapa `nomeDaTurma -> quantidadeRealDeAlunos`.
   */
  fun realStudentsByClass(students: List<Student>): Map<String, Int> {
    return students.groupingBy { it.enrolledClass.trim() }.eachCount()
  }

  /**
   * Valida se um aluno novo/atualizado referencia curso e turma já existentes.
   *
   * @param student Aluno que será persistido.
   * @param existingCourses Cursos válidos no sistema.
   * @param existingClasses Turmas válidas no sistema.
   * @return Mensagem de erro quando a relação é inválida; `null` quando está consistente.
   */
  fun validateStudentLinks(
      student: Student,
      existingCourses: List<Course>,
      existingClasses: List<SchoolClass>,
  ): String? {
    val courseExists = existingCourses.any { it.title.equals(student.course, ignoreCase = true) }
    if (!courseExists) return "O curso informado para o aluno não existe."

    val classMatch =
        existingClasses.firstOrNull { it.name.equals(student.enrolledClass, ignoreCase = true) }
            ?: return "A turma informada para o aluno não existe."

    if (!classMatch.course.equals(student.course, ignoreCase = true)) {
      return "A turma informada não pertence ao curso selecionado."
    }
    return null
  }
}
