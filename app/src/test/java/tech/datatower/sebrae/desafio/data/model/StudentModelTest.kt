package tech.datatower.sebrae.desafio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

/**
 * Testes unitários de modelos de dados de estudantes.
 *
 * Valida invariantes de estrutura, imutabilidade e coerência de dados.
 *
 * Padrão: Arrange → Act → Assert (AAA)
 */
class StudentModelTest {

  /**
   * Valida criação de um estudante com todos os campos.
   */
  @Test
  fun `construtor deve criar estudante com todos campos preenchidos`() {
    // Arrange
    val id = 1
    val nome = "João Silva"
    val email = "joao@example.com"
    val curso = "Marketing Digital"
    val turma = "Turma A1"
    val progresso = 0.75f
    val status = StudentStatus.Active

    // Act
    val estudante =
        Student(
            id = id,
            name = nome,
            email = email,
            course = curso,
            enrolledClass = turma,
            progress = progresso,
            status = status,
        )

    // Assert
    assertEquals("ID deve ser correto", id, estudante.id)
    assertEquals("Nome deve ser correto", nome, estudante.name)
    assertEquals("Email deve ser correto", email, estudante.email)
    assertEquals("Curso deve ser correto", curso, estudante.course)
    assertEquals("Turma deve ser correta", turma, estudante.enrolledClass)
    assertEquals("Progresso deve ser correto", progresso, estudante.progress, 0.001f)
    assertEquals("Status deve ser ativo", StudentStatus.Active, estudante.status)
  }

  /**
   * Valida que o progresso fica entre 0 e 1.
   */
  @Test
  fun `progresso deve estar entre 0 e 1`() {
    // Arrange
    val estudante1 = Student(1, "Aluno 1", "email1@test.com", "Curso", "Turma", 0f, StudentStatus.Active)
    val estudante2 = Student(2, "Aluno 2", "email2@test.com", "Curso", "Turma", 1f, StudentStatus.Active)
    val estudante3 = Student(3, "Aluno 3", "email3@test.com", "Curso", "Turma", 0.5f, StudentStatus.Active)

    // Act & Assert
    assertTrue("Progresso 0 é válido", estudante1.progress >= 0)
    assertTrue("Progresso 1 é válido", estudante2.progress <= 1)
    assertTrue("Progresso 0.5 está entre 0 e 1", estudante3.progress in 0f..1f)
  }

  /**
   * Valida modelo de registro de frequência.
   */
  @Test
  fun `registro de frequência deve armazenar todos os dados`() {
    // Arrange
    val data = LocalDate.now()
    val status = AttendanceStatus.Present
    val minutoAtraso = 5

    // Act
    val registro = AttendanceRecord(data, status, minutoAtraso)

    // Assert
    assertEquals("Data deve ser correta", data, registro.date)
    assertEquals("Status deve ser presente", AttendanceStatus.Present, registro.status)
    assertEquals("Minuto de atraso deve ser 5", minutoAtraso, registro.minutesLate)
  }

  /**
   * Valida modelo de registro de comportamento.
   */
  @Test
  fun `registro de comportamento deve conter todos os campos`() {
    // Arrange
    val data = LocalDate.now()
    val participacao = 4
    val entrega = ActivityDeliveryStatus.OnTime
    val atraso = 0
    val nota = 8.5f
    val observacao = "Bom comportamento"

    // Act
    val registro =
        BehaviorRecord(
            date = data,
            participationScore = participacao,
            activityDelivery = entrega,
            delayMinutes = atraso,
            grade = nota,
            note = observacao,
        )

    // Assert
    assertEquals("Data deve ser correta", data, registro.date)
    assertEquals("Participação deve ser 4", participacao, registro.participationScore)
    assertEquals("Entrega deve ser no prazo", ActivityDeliveryStatus.OnTime, registro.activityDelivery)
    assertEquals("Atraso deve ser 0", atraso, registro.delayMinutes)
    assertEquals("Nota deve ser 8.5", nota, registro.grade, 0.001f)
    assertEquals("Observação deve ser correta", observacao, registro.note)
  }

  /**
   * Valida status de estudante.
   */
  @Test
  fun `status de estudante deve ter valores válidos`() {
    // Arrange & Act & Assert
    assertTrue("Deve ter status Ativo", StudentStatus.values().contains(StudentStatus.Active))
    assertTrue("Deve ter status Inativo", StudentStatus.values().contains(StudentStatus.Inactive))
    assertTrue("Deve ter status Formado", StudentStatus.values().contains(StudentStatus.Graduated))
  }

  /**
   * Valida imutabilidade do modelo Student (data class).
   */
  @Test
  fun `modelo Student deve ser imutável (data class)`() {
    // Arrange
    val estudante1 =
        Student(1, "João", "joao@test.com", "Curso A", "Turma 1", 0.5f, StudentStatus.Active)
    val estudante2 = estudante1.copy()

    // Act & Assert
    assertEquals("Cópia deve ter mesmos valores", estudante1, estudante2)
    assertTrue("Cópia deve ser instância diferente", estudante1 !== estudante2)
  }

  /**
   * Valida modelo de classe com status.
   */
  @Test
  fun `classe escolar deve conter status válido`() {
    // Arrange
    val turma =
        SchoolClass(
            id = 1,
            name = "Turma A1",
            course = "Marketing",
            instructor = "Prof. Helena",
            studentsCount = 28,
            maxCapacity = 30,
            schedule = "Seg/Qua 18h-20h",
            status = ClassStatus.InProgress,
        )

    // Act & Assert
    assertEquals("ID deve ser 1", 1, turma.id)
    assertEquals("Status deve ser em andamento", ClassStatus.InProgress, turma.status)
    assertTrue("Capacidade utilizada deve ser válida", turma.studentsCount <= turma.maxCapacity)
  }

  /**
   * Valida modelo de evento de calendário.
   */
  @Test
  fun `evento de calendário deve armazenar informações corretamente`() {
    // Arrange
    val evento =
        CalendarEvent(
            id = 1,
            title = "Prova de Marketing",
            course = "Marketing Digital",
            date = "2026-03-25",
            time = "18:00 - 20:00",
            location = "Sala 101",
            type = EventType.Exam,
        )

    // Act & Assert
    assertEquals("ID deve ser 1", 1, evento.id)
    assertEquals("Tipo deve ser Exam", EventType.Exam, evento.type)
    assertFalse("Título não deve estar vazio", evento.title.isEmpty())
  }
}

