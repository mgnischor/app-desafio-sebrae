package tech.datatower.sebrae.desafio.data.model

import androidx.compose.runtime.Immutable

/**
 * Representa um compromisso exibido no calendário acadêmico.
 *
 * @property id Identificador único do evento.
 * @property title Título principal do evento.
 * @property course Curso ou contexto institucional associado.
 * @property date Data textual utilizada para agrupamento e exibição.
 * @property time Faixa de horário do evento.
 * @property location Local físico ou referência de sala.
 * @property type Tipo funcional do evento para regras visuais e filtros.
 */
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

/** Classifica a natureza do compromisso exibido no calendário. */
enum class EventType {
  Class,
  Exam,
  Meeting,
  Other,
}
