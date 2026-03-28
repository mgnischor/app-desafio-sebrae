/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/model/CalendarEvent.kt
    Descrição: Modelo de domínio representando um evento do calendário educacional.
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
