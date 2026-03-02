package tech.datatower.sebrae.desafio.data.model

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuModule(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int = 0,
)

