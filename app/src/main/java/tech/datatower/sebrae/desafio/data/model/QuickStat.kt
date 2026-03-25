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
