package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa o usuário autenticado na sessão atual.
 *
 * @property id Identificador único do usuário.
 * @property name Nome completo exibido na interface.
 * @property email Endereço de e-mail utilizado como credencial de login.
 * @property role Nível de permissão que determina os módulos acessíveis.
 */
@Immutable
data class AppUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: UserRole,
)
