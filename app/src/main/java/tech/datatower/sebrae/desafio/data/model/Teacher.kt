package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa um instrutor cadastrado no sistema.
 *
 * @property id Identificador único do instrutor.
 * @property name Nome completo do instrutor.
 * @property email E-mail institucional.
 * @property specialty Área principal de atuação.
 * @property activeCourses Quantidade de cursos em que atua atualmente.
 * @property totalStudents Total de alunos atendidos em suas turmas ativas.
 * @property rating Avaliação média do instrutor no intervalo de `0f..5f`.
 */
@Immutable
data class Teacher(
    val id: Int,
    val name: String,
    val email: String,
    val specialty: String,
    val activeCourses: Int,
    val totalStudents: Int,
    val rating: Float, // 0f..5f
)
