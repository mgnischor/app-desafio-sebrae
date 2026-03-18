package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class QuickStat(
    val labelRes: Int,
    val value: String,
    val progress: Float? = null, // 0f..1f — null = não mostrar barra
    val trendLabel: String? = null,
)
