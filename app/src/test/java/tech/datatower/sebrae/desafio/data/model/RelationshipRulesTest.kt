package tech.datatower.sebrae.desafio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários das regras de relacionamento entre curso, turma e aluno.
 */
class RelationshipRulesTest {

  /**
   * Valida cálculo de contagem real de alunos por curso e por turma.
   */
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

  /**
   * Garante erro quando aluno referencia curso inexistente.
   */
  @Test
  fun `should return error when student references missing course`() {
    val student = Student(1, "Ana", "ana@mail.com", "Inexistente", "A1", 0.1f, StudentStatus.Active)
    val courses = listOf(Course(1, "Marketing", "Negocios", "Helena", 0, 40, 0.0f, true))
    val classes =
        listOf(
            SchoolClass(1, "A1", "Marketing", "Helena", 0, 30, "Seg 19h", ClassStatus.Open)
        )

    val error = RelationshipRules.validateStudentLinks(student, courses, classes)

    assertTrue(error?.contains("curso", ignoreCase = true) == true)
  }

  /**
   * Garante relação válida quando turma pertence ao curso informado pelo aluno.
   */
  @Test
  fun `should accept valid course and class relationship`() {
    val student = Student(1, "Ana", "ana@mail.com", "Marketing", "A1", 0.1f, StudentStatus.Active)
    val courses = listOf(Course(1, "Marketing", "Negocios", "Helena", 0, 40, 0.0f, true))
    val classes =
        listOf(
            SchoolClass(1, "A1", "Marketing", "Helena", 0, 30, "Seg 19h", ClassStatus.Open)
        )

    val error = RelationshipRules.validateStudentLinks(student, courses, classes)

    assertNull(error)
  }
}

