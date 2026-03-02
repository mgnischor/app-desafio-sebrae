package tech.datatower.sebrae.desafio.data.model

data class Course(
    val id: Int,
    val title: String,
    val category: String,
    val instructor: String,
    val totalStudents: Int,
    val durationHours: Int,
    val completionRate: Float,  // 0f..1f
    val isPublished: Boolean,
)

