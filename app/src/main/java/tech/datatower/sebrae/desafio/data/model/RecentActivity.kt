package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Item de atividade recente apresentado no dashboard.
 *
 * @property title Título do evento recente.
 * @property subtitle Descrição complementar do evento.
 * @property icon Ícone representativo da atividade.
 * @property timeLabel Marcador textual de tempo relativo.
 */
@Immutable
data class RecentActivity(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val timeLabel: String,
)
