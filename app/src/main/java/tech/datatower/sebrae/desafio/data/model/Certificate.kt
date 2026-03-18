package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa um certificado emitido para conclusão de curso.
 *
 * @property id Identificador único do certificado.
 * @property studentName Nome do aluno certificado.
 * @property courseName Nome do curso concluído.
 * @property issuedDate Data de emissão no formato de exibição da interface.
 * @property hours Carga horária certificada em horas.
 * @property code Código único de validação do certificado.
 */
@Immutable
data class Certificate(
    val id: Int,
    val studentName: String,
    val courseName: String,
    val issuedDate: String,
    val hours: Int,
    val code: String,
)
