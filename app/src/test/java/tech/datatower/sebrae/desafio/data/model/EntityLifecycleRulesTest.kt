package tech.datatower.sebrae.desafio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários das transições de ciclo de vida das entidades acadêmicas.
 */
class EntityLifecycleRulesTest {

  /**
   * Garante desativação e reativação de aluno.
   */
  @Test
  fun `student lifecycle should toggle active and inactive`() {
    val student = Student(1, "Ana", "ana@mail.com", "Marketing", "A1", 0.2f, StudentStatus.Active)

    val deactivated = EntityLifecycleRules.deactivateStudent(student)
    val reactivated = EntityLifecycleRules.reactivateStudent(deactivated)

    assertEquals(StudentStatus.Inactive, deactivated.status)
    assertEquals(StudentStatus.Active, reactivated.status)
  }

  /**
   * Garante desativação e reativação de curso.
   */
  @Test
  fun `course lifecycle should toggle publication state`() {
    val course = Course(1, "Marketing", "Negocios", "Helena", 10, 40, 0.5f, true)

    val deactivated = EntityLifecycleRules.deactivateCourse(course)
    val reactivated = EntityLifecycleRules.reactivateCourse(deactivated)

    assertFalse(deactivated.isPublished)
    assertTrue(reactivated.isPublished)
  }

  /**
   * Garante desativação e reativação de turma.
   */
  @Test
  fun `class lifecycle should toggle open and closed statuses`() {
    val schoolClass = SchoolClass(1, "A1", "Marketing", "Helena", 10, 30, "Seg 19h", ClassStatus.Open)

    val deactivated = EntityLifecycleRules.deactivateClass(schoolClass)
    val reactivated = EntityLifecycleRules.reactivateClass(deactivated)

    assertEquals(ClassStatus.Closed, deactivated.status)
    assertEquals(ClassStatus.Open, reactivated.status)
  }

  /**
   * Garante desativação e reativação de instrutor.
   */
  @Test
  fun `teacher lifecycle should toggle active flag`() {
    val teacher = Teacher(1, "Helena", "helena@mail.com", "Marketing", 2, 40, 4.7f, true)

    val deactivated = EntityLifecycleRules.deactivateTeacher(teacher)
    val reactivated = EntityLifecycleRules.reactivateTeacher(deactivated)

    assertFalse(deactivated.isActive)
    assertTrue(reactivated.isActive)
  }
}

