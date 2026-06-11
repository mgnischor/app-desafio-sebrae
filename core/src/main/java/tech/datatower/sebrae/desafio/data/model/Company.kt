/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /core/src/main/java/tech/datatower/sebrae/desafio/data/model/Company.kt
    Descrição: Modelo de dados representando uma empresa (escola) cadastrada no sistema.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
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
