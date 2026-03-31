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

@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  private val companyId = AuthManager.currentCompany.map { it?.id }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val events: StateFlow<List<CalendarEvent>> =
      companyId
          .filterNotNull()
          .flatMapLatest { cid -> repository.observeCalendarEvents(cid) }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val groupedEvents: StateFlow<List<Map.Entry<String, List<CalendarEvent>>>> =
      events
          .map { list -> list.groupBy { it.date }.entries.toList() }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  sealed class ActionResult {
    data object Idle : ActionResult()

    data object Success : ActionResult()

    data class Error(val message: String) : ActionResult()
  }

  private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
  val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.CALENDAR) }
  }

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

  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
