/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/model/RelationshipRulesTest.kt
    Descrição: Testes unitários das regras de relacionamento entre curso, turma e aluno.
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
package tech.datatower.sebrae.desafio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes unitários das regras de relacionamento entre curso, turma e aluno. */
class RelationshipRulesTest {

  /** Valida cálculo de contagem real de alunos por curso e por turma. */
  @Test
  fun `should compute real student counts by course and class`() {
    val students =
        listOf(
            Student(1, "Ana", "ana@mail.com", "Marketing", "A1", 0.1f, StudentStatus.Active),
            Student(2, "Bia", "bia@mail.com", "Marketing", "A1", 0.2f, StudentStatus.Active),
            Student(3, "Caio", "caio@mail.com", "Excel", "B3", 0.3f, StudentStatus.Active),
        )

    val byCourse = RelationshipRules.realStudentsByCourse(students)
    val byClass = RelationshipRules.realStudentsByClass(students)

    assertEquals(2, byCourse["Marketing"])
    assertEquals(1, byCourse["Excel"])
    assertEquals(2, byClass["A1"])
    assertEquals(1, byClass["B3"])
  }

  /** Garante erro quando aluno referencia curso inexistente. */
  @Test
  fun `should return error when student references missing course`() {
    val student = Student(1, "Ana", "ana@mail.com", "Inexistente", "A1", 0.1f, StudentStatus.Active)
    val courses = listOf(Course(1, "Marketing", "Negocios", "Helena", 0, 40, 0.0f, true))
    val classes =
        listOf(SchoolClass(1, "A1", "Marketing", "Helena", 0, 30, "Seg 19h", ClassStatus.Open))

    val error = RelationshipRules.validateStudentLinks(student, courses, classes)

    assertTrue(error?.contains("curso", ignoreCase = true) == true)
  }

  /** Garante relação válida quando turma pertence ao curso informado pelo aluno. */
  @Test
  fun `should accept valid course and class relationship`() {
    val student = Student(1, "Ana", "ana@mail.com", "Marketing", "A1", 0.1f, StudentStatus.Active)
    val courses = listOf(Course(1, "Marketing", "Negocios", "Helena", 0, 40, 0.0f, true))
    val classes =
        listOf(SchoolClass(1, "A1", "Marketing", "Helena", 0, 30, "Seg 19h", ClassStatus.Open))

    val error = RelationshipRules.validateStudentLinks(student, courses, classes)

    assertNull(error)
  }
}
