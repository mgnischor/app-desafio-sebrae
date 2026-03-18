package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

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

enum class StudentStatus {
  Active,
  Inactive,
  Graduated,
}
