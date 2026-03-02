package tech.datatower.sebrae.desafio.data.model

data class Teacher(
    val id: Int,
    val name: String,
    val email: String,
    val specialty: String,
    val activeCourses: Int,
    val totalStudents: Int,
    val rating: Float,          // 0f..5f
)

