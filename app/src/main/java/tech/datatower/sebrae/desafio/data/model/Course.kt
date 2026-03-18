package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa um curso oferecido pela instituição.
 *
 * @property id Identificador único do curso.
 * @property title Título de exibição do curso.
 * @property category Categoria temática do curso.
 * @property instructor Nome do instrutor principal.
 * @property totalStudents Quantidade total de alunos matriculados.
 * @property durationHours Carga horária total em horas.
 * @property completionRate Taxa de conclusão dos alunos no intervalo de `0f..1f`.
 * @property isPublished Indica se o curso está publicado para matrícula.
 */
@Immutable
data class Course(
    val id: Int,
    val title: String,
    val category: String,
    val instructor: String,
    val totalStudents: Int,
    val durationHours: Int,
    val completionRate: Float, // 0f..1f
    val isPublished: Boolean,
)
