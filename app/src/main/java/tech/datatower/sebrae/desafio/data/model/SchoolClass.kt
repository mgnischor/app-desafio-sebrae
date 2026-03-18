package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa uma turma vinculada a um curso, com dados operacionais para acompanhamento.
 *
 * @property id Identificador único da turma.
 * @property name Nome ou código da turma.
 * @property course Curso associado à turma.
 * @property instructor Nome do instrutor responsável.
 * @property studentsCount Quantidade atual de alunos matriculados.
 * @property maxCapacity Capacidade máxima de alunos da turma.
 * @property schedule Descrição textual do horário das aulas.
 * @property status Estado atual da turma.
 */
@Immutable
data class SchoolClass(
    val id: Int,
    val name: String,
    val course: String,
    val instructor: String,
    val studentsCount: Int,
    val maxCapacity: Int,
    val schedule: String,
    val status: ClassStatus,
)

/** Estados de execução de uma turma ao longo do período letivo. */
enum class ClassStatus {
  Open,
  InProgress,
  Closed,
}
