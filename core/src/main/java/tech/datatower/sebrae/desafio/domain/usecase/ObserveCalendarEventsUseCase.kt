package tech.datatower.sebrae.desafio.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.CalendarEvent
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para observar os eventos de calendário da empresa ativa.
 *
 * Regras de negócio encapsuladas:
 * - Retorna eventos exclusivamente da empresa corrente do usuário autenticado.
 * - Emite lista vazia enquanto nenhuma empresa estiver selecionada.
 */
@Singleton
class ObserveCalendarEventsUseCase
@Inject
constructor(
    private val repository: AppRepository,
) {
  @OptIn(ExperimentalCoroutinesApi::class)
  operator fun invoke(): Flow<List<CalendarEvent>> =
      AuthManager.currentCompany
          .map { it?.id }
          .filterNotNull()
          .flatMapLatest { cid -> repository.observeCalendarEvents(cid) }
}
