package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa um aluno exibido nas telas de gestão acadêmica.
 *
 * O modelo é imutável para favorecer estabilidade de recomposição no Compose.
 *
 * @property id Identificador único do aluno.
 * @property name Nome completo do aluno.
 * @property email E-mail de contato.
 * @property course Nome do curso principal no qual o aluno está matriculado.
 * @property enrolledClass Identificação da turma atual do aluno.
 * @property progress Progresso de conclusão do curso no intervalo de `0f..1f`.
 * @property status Situação acadêmica atual do aluno.
 */
@Immutable
data class Student(
    val id: Int,
    val name: String,
    val email: String,
    val course: String,
    val enrolledClass: String,
    val progress: Float, // 0f..1f
    val status: StudentStatus,
)

/** Situações possíveis para o ciclo acadêmico do aluno. */
enum class StudentStatus {
  Active,
  Inactive,
  Graduated,
}
