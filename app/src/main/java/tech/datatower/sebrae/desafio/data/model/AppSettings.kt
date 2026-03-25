package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val darkMode: Boolean,
    val pushEnabled: Boolean,
    val emailEnabled: Boolean,
    val language: String = "pt", // pt, en, es
)
