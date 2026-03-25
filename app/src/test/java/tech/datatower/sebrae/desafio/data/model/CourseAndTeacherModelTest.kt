package tech.datatower.sebrae.desafio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Testes unitários de modelos de dados de professores e cursos.
 *
 * Valida estrutura, relacionamentos e invariantes de modelos educacionais.
 *
 * Padrão: Arrange → Act → Assert (AAA)
 */
class CourseAndTeacherModelTest {

  /** Valida criação de curso com todos os campos necessários. */
  @Test
  fun `curso deve ser criado com todos os campos preenchidos`() {
    // Arrange
    val id = 1
    val titulo = "Marketing Digital"
    val categoria = "Negócios"
    val instrutor = "Prof. Helena"
    val totalEstudantes = 320
    val cargaHoraria = 40
    val taxaConc = 0.82f
    val publicado = true

    // Act
    val curso =
        Course(
            id = id,
            title = titulo,
            category = categoria,
            instructor = instrutor,
            totalStudents = totalEstudantes,
            durationHours = cargaHoraria,
            completionRate = taxaConc,
            isPublished = publicado,
        )

    // Assert
    assertEquals("ID deve ser 1", id, curso.id)
    assertEquals("Título deve corresponder", titulo, curso.title)
    assertEquals("Categoria deve ser Negócios", categoria, curso.category)
    assertEquals("Instrutor deve corresponder", instrutor, curso.instructor)
    assertEquals("Total de estudantes deve ser 320", totalEstudantes, curso.totalStudents)
    assertEquals("Carga horária deve ser 40", cargaHoraria, curso.durationHours)
    assertEquals("Taxa de conclusão deve ser 0.82", taxaConc, curso.completionRate, 0.001f)
    assertEquals("Deve estar publicado", publicado, curso.isPublished)
  }

  /** Valida que taxa de conclusão fica entre 0 e 1. */
  @Test
  fun `taxa de conclusão deve estar entre 0 porcento e 100 porcento`() {
    // Arrange
    val cursoNaoIniciado = Course(1, "Curso 1", "Cat", "Prof", 10, 20, 0f, true)
    val cursoCompleto = Course(2, "Curso 2", "Cat", "Prof", 10, 20, 1f, true)
    val cursoMeio = Course(3, "Curso 3", "Cat", "Prof", 10, 20, 0.5f, true)

    // Act & Assert
    assertEquals("Taxa 0% é válida", 0f, cursoNaoIniciado.completionRate, 0.001f)
    assertEquals("Taxa 100% é válida", 1f, cursoCompleto.completionRate, 0.001f)
    assertEquals("Taxa 50% é válida", 0.5f, cursoMeio.completionRate, 0.001f)
  }

  /** Valida criação de professor com especialidade. */
  @Test
  fun `professor deve conter nome, email e especialidade`() {
    // Arrange
    val id = 1
    val nome = "Profa. Helena Martins"
    val email = "helena@inst.com"
    val especialidade = "Marketing & Comunicação"
    val cursosAtivos = 3
    val totalEstudantes = 320
    val avaliacao = 4.9f

    // Act
    val professor =
        Teacher(
            id = id,
            name = nome,
            email = email,
            specialty = especialidade,
            activeCourses = cursosAtivos,
            totalStudents = totalEstudantes,
            rating = avaliacao,
        )

    // Assert
    assertEquals("ID deve ser 1", id, professor.id)
    assertEquals("Nome deve corresponder", nome, professor.name)
    assertEquals("Email deve corresponder", email, professor.email)
    assertEquals("Especialidade deve corresponder", especialidade, professor.specialty)
    assertEquals("Cursos ativos deve ser 3", cursosAtivos, professor.activeCourses)
    assertEquals("Total estudantes deve ser 320", totalEstudantes, professor.totalStudents)
    assertEquals("Avaliação deve ser 4.9", avaliacao, professor.rating, 0.001f)
  }

  /** Valida que avaliação de professor fica entre 1 e 5. */
  @Test
  fun `avaliação de professor deve estar entre 1 e 5 estrelas`() {
    // Arrange
    val profRuim = Teacher(1, "Prof 1", "email@test", "Especialidade", 1, 10, 1.0f)
    val profBom = Teacher(2, "Prof 2", "email@test", "Especialidade", 2, 20, 5.0f)
    val profMedio = Teacher(3, "Prof 3", "email@test", "Especialidade", 3, 30, 3.5f)

    // Act & Assert
    assertEquals("Avaliação mínima 1.0", 1.0f, profRuim.rating, 0.001f)
    assertEquals("Avaliação máxima 5.0", 5.0f, profBom.rating, 0.001f)
    assertEquals("Avaliação intermediária 3.5", 3.5f, profMedio.rating, 0.001f)
  }

  /** Valida modelo de certificado com dados importantes. */
  @Test
  fun `certificado deve armazenar nome do aluno, curso e data`() {
    // Arrange
    val id = 1
    val nomeAluno = "Carlos Souza"
    val nomeCurso = "Marketing Digital"
    val dataEmissao = "2026-03-24"
    val horas = 40
    val codigo = "CERT-2026-001"

    // Act
    val certificado =
        Certificate(
            id = id,
            studentName = nomeAluno,
            courseName = nomeCurso,
            issuedDate = dataEmissao,
            hours = horas,
            code = codigo,
        )

    // Assert
    assertEquals("ID deve ser 1", id, certificado.id)
    assertEquals("Nome aluno deve corresponder", nomeAluno, certificado.studentName)
    assertEquals("Nome curso deve corresponder", nomeCurso, certificado.courseName)
    assertEquals("Data emissão deve corresponder", dataEmissao, certificado.issuedDate)
    assertEquals("Horas deve ser 40", horas, certificado.hours)
    assertEquals("Código deve corresponder", codigo, certificado.code)
  }

  /** Valida relação entre múltiplos professores. */
  @Test
  fun `lista de professores deve manter dados individuais corretos`() {
    // Arrange
    val professores =
        listOf(
            Teacher(1, "Prof 1", "prof1@test", "Mat", 2, 50, 4.5f),
            Teacher(2, "Prof 2", "prof2@test", "Port", 3, 75, 4.8f),
            Teacher(3, "Prof 3", "prof3@test", "Ing", 1, 30, 4.2f),
        )

    // Act & Assert
    assertEquals("Deve ter 3 professores", 3, professores.size)
    assertEquals("Primeiro professor deve ser Prof 1", "Prof 1", professores[0].name)
    assertEquals("Segundo professor deve ter 3 cursos", 3, professores[1].activeCourses)
    assertEquals("Terceiro professor deve ter avaliação 4.2", 4.2f, professores[2].rating, 0.001f)
  }

  /** Valida que certificados possuem código único. */
  @Test
  fun `certificados devem ter códigos únicos e bem formados`() {
    // Arrange
    val cert1 = Certificate(1, "Aluno 1", "Curso 1", "2026-03-20", 40, "CERT-2026-001")
    val cert2 = Certificate(2, "Aluno 2", "Curso 2", "2026-03-21", 40, "CERT-2026-002")
    val cert3 = Certificate(3, "Aluno 3", "Curso 3", "2026-03-22", 40, "CERT-2026-003")

    // Act
    val codigos = listOf(cert1.code, cert2.code, cert3.code)
    val todosUnicos = codigos.size == codigos.toSet().size

    // Assert
    assertNotNull("Certificado 1 deve ter código", cert1.code)
    assertNotNull("Certificado 2 deve ter código", cert2.code)
    assertNotNull("Certificado 3 deve ter código", cert3.code)
    assertEquals("Todos códigos devem ser únicos", true, todosUnicos)
  }
}
