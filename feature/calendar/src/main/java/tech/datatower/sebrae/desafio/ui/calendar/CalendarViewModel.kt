/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/calendar/CalendarViewModel.kt
    Descrição: ViewModel da tela de calendário, gerenciando eventos e operações de criação.
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
package tech.datatower.sebrae.desafio.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.local.CalendarEventEntity
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.model.EventType
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

/**
 * ViewModel da tela de calendário de eventos.
 *
 * Expõe [events] e [groupedEvents] (agrupados por data) como [StateFlow]s reativos
 * filtrados pela empresa ativa. Suporta criação de novos eventos via [createEvent]
 * com feedback de resultado em [actionResult].
 */
@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  private val companyId =
      AuthManager.currentCompany
          .map { it?.id }
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /** Lista plana de eventos do calendário para a empresa ativa, atualizada reativamente. */
  val events: StateFlow<List<CalendarEvent>> =
      companyId
          .filterNotNull()
          .flatMapLatest { cid -> repository.observeCalendarEvents(cid) }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Eventos agrupados por data em ordem cronológica; cada entry contém uma lista de eventos do dia. */
  val groupedEvents: StateFlow<List<Map.Entry<String, List<CalendarEvent>>>> =
      events
          .map { list -> list.groupBy { it.date }.entries.toList() }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /**
   * Resultado de uma operação de criação de evento.
   */
  sealed class ActionResult {
    /** Nenhuma operação pendente ou resultado já consumido. */
    data object Idle : ActionResult()

    /** Evento criado com sucesso. */
    data object Success : ActionResult()

    /** Criação falhou.
     * @property message Descrição do erro para exibir ao usuário.
     */
    data class Error(val message: String) : ActionResult()
  }

  private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
  val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.CALENDAR) }
  }

  /**
   * Cria um novo evento de calendário para a empresa ativa.
   *
   * O ID é gerado localmente como `max(ids_atuais) + 1`.
   *
   * @param requester Usuário logado (deve ter permissão para criar eventos).
   * @param title Título do evento.
   * @param course Curso relacionado ao evento.
   * @param date Data do evento no formato `yyyy-MM-dd`.
   * @param time Horário do evento no formato `HH:mm`.
   * @param location Local do evento.
   * @param type Tipo do evento ([EventType]).
   */
  fun createEvent(
      requester: AppUser?,
      title: String,
      course: String,
      date: String,
      time: String,
      location: String,
      type: EventType,
  ) {
    viewModelScope.launch {
      val nextId = (events.value.maxOfOrNull { it.id } ?: 0) + 1
      val result =
          dataConnectService.upsertCalendarEvent(
              requester = requester,
              event =
                  CalendarEventEntity(
                      id = nextId,
                      title = title,
                      course = course,
                      date = date,
                      time = time,
                      location = location,
                      type = type,
                      companyId = companyId.value ?: 0,
                  ),
          )
      _actionResult.value =
          when (result) {
            is FirebaseDataConnectService.Result.Error -> ActionResult.Error(result.message)
            else -> ActionResult.Success
          }
    }
  }

  /** Redefine [actionResult] para [ActionResult.Idle] após o resultado ser tratado pela UI. */
  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
