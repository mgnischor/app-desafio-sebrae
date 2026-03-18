package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class CalendarEvent(
    val id: Int,
    val title: String,
    val course: String,
    val date: String,
    val time: String,
    val location: String,
    val type: EventType,
)

enum class EventType {
  Class,
  Exam,
  Meeting,
  Other,
}
