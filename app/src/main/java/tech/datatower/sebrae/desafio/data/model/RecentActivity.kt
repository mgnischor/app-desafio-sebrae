package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Item de atividade recente apresentado no dashboard.
 *
 * @property id Identificador estável para chaveamento de listas no Compose.
 * @property title Título do evento recente.
 * @property subtitle Descrição complementar do evento.
 * @property icon Ícone representativo da atividade.
 * @property timeLabel Marcador textual de tempo relativo.
 */
@Immutable
data class RecentActivity(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val timeLabel: String,
)
