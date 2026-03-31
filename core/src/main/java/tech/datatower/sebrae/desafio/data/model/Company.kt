package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa uma empresa (escola) cadastrada no sistema.
 *
 * @property id Identificador único da empresa.
 * @property name Nome de exibição da empresa/escola.
 * @property cnpj CNPJ da empresa (opcional).
 * @property isActive Indica se a empresa está ativa no sistema.
 */
@Immutable
data class Company(
    val id: Int,
    val name: String,
    val cnpj: String = "",
    val isActive: Boolean = true,
)
