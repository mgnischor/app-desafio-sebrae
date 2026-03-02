package tech.datatower.sebrae.desafio.data.model

import androidx.compose.ui.graphics.vector.ImageVector

data class RecentActivity(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val timeLabel: String,
)

