/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/QuickStat.kt
    Descrição: Modelo de domínio representando uma estatística resumida exibida na tela inicial.
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

/**
 * Representa um indicador rápido mostrado no resumo da tela inicial.
 *
 * @property labelRes Recurso de string com o rótulo da métrica.
 * @property value Valor principal formatado para exibição.
 * @property progress Percentual opcional no intervalo de `0f..1f` para barra de progresso.
 * @property trendLabel Texto opcional de tendência (ex.: variação mensal).
 * @property trendLabelRes Recurso de string opcional para tendência (preferível a trendLabel para
 *   i18n).
 */
@Immutable
data class QuickStat(
    val labelRes: Int,
    val value: String,
    val progress: Float? = null, // 0f..1f — null = não mostrar barra
    val trendLabel: String? = null,
    val trendLabelRes: Int? = null,
)
