package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

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

enum class ClassStatus {
  Open,
  InProgress,
  Closed,
}
