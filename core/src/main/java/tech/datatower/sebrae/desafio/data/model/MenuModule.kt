/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/MenuModule.kt
    Descrição: Modelo de domínio representando um módulo de menu acessível ao usuário.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
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
