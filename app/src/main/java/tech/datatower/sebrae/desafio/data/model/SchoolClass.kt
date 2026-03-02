package tech.datatower.sebrae.desafio.data.model

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

enum class ClassStatus { Open, InProgress, Closed }

