/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/model/EntityLifecycleRulesTest.kt
    Descrição: Testes unitários das transições de ciclo de vida das entidades acadêmicas.
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes unitários das transições de ciclo de vida das entidades acadêmicas. */
class EntityLifecycleRulesTest {

  /** Garante desativação e reativação de aluno. */
  @Test
  fun `student lifecycle should toggle active and inactive`() {
    val student = Student(1, "Ana", "ana@mail.com", "Marketing", "A1", 0.2f, StudentStatus.Active)

    val deactivated = EntityLifecycleRules.deactivateStudent(student)
    val reactivated = EntityLifecycleRules.reactivateStudent(deactivated)

    assertEquals(StudentStatus.Inactive, deactivated.status)
    assertEquals(StudentStatus.Active, reactivated.status)
  }

  /** Garante desativação e reativação de curso. */
  @Test
  fun `course lifecycle should toggle publication state`() {
    val course = Course(1, "Marketing", "Negocios", "Helena", 10, 40, 0.5f, true)

    val deactivated = EntityLifecycleRules.deactivateCourse(course)
    val reactivated = EntityLifecycleRules.reactivateCourse(deactivated)

    assertFalse(deactivated.isPublished)
    assertTrue(reactivated.isPublished)
  }

  /** Garante desativação e reativação de turma. */
  @Test
  fun `class lifecycle should toggle open and closed statuses`() {
    val schoolClass =
        SchoolClass(1, "A1", "Marketing", "Helena", 10, 30, "Seg 19h", ClassStatus.Open)

    val deactivated = EntityLifecycleRules.deactivateClass(schoolClass)
    val reactivated = EntityLifecycleRules.reactivateClass(deactivated)

    assertEquals(ClassStatus.Closed, deactivated.status)
    assertEquals(ClassStatus.Open, reactivated.status)
  }

  /** Garante desativação e reativação de instrutor. */
  @Test
  fun `teacher lifecycle should toggle active flag`() {
    val teacher = Teacher(1, "Helena", "helena@mail.com", "Marketing", 2, 40, 4.7f, true)

    val deactivated = EntityLifecycleRules.deactivateTeacher(teacher)
    val reactivated = EntityLifecycleRules.reactivateTeacher(deactivated)

    assertFalse(deactivated.isActive)
    assertTrue(reactivated.isActive)
  }
}
