package tech.datatower.sebrae.desafio.data.model

data class Certificate(
    val id: Int,
    val studentName: String,
    val courseName: String,
    val issuedDate: String,
    val hours: Int,
    val code: String,
)

