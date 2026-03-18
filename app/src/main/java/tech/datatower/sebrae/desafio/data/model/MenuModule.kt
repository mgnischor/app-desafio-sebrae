package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define um módulo navegável da tela inicial.
 *
 * @property titleRes Recurso de string do título do módulo.
 * @property descriptionRes Recurso de string da descrição auxiliar.
 * @property icon Ícone vetorial exibido no cartão do módulo.
 * @property route Rota de navegação acionada ao clicar no módulo.
 * @property badgeCount Quantidade opcional de pendências exibida como badge.
 */
@Immutable
data class MenuModule(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int = 0,
)
