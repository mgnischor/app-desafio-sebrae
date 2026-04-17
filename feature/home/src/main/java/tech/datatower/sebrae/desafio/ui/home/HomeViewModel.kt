/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/home/HomeViewModel.kt
    Descrição: ViewModel da tela inicial, provendo estatísticas e atividades em tempo real.
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
package tech.datatower.sebrae.desafio.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.QuickStat
import tech.datatower.sebrae.desafio.data.model.RecentActivity
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.domain.usecase.SyncScreenDataUseCase
import javax.inject.Inject

/**
 * ViewModel da tela inicial do painel de gestão.
 *
 * Expõe [stats] e [recentActivities] como [StateFlow]s reativos filtrados pela empresa ativa.
 * Suporta sincronização manual com o Firebase via [syncHome] e escuta de eventos em tempo real via
 * [observeRealtimeActivities].
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val syncScreenDataUseCase: SyncScreenDataUseCase,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  /**
   * Estatísticas rápidas da empresa ativa (totais de alunos, cursos, turmas, taxas de conclusão).
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val stats: StateFlow<List<QuickStat>> =
      AuthManager.currentCompany
          .map { it?.id }
          .flatMapLatest { cid ->
            if (cid == null) return@flatMapLatest flowOf(emptyList())
            repository.observeHomeQuickStats(cid)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Últimas 5 atividades recentes da empresa ativa, ordenadas por data decrescente. */
  @OptIn(ExperimentalCoroutinesApi::class)
  val recentActivities: StateFlow<List<RecentActivity>> =
      AuthManager.currentCompany
          .map { it?.id }
          .flatMapLatest { cid ->
            if (cid == null) return@flatMapLatest flowOf(emptyList())
            repository.observeRecentActivities(cid, limit = 5)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Estados possíveis da sincronização manual com o Firebase. */
  sealed class SyncState {
    data object Idle : SyncState()

    data object Loading : SyncState()

    data object Success : SyncState()

    data class Error(val message: String) : SyncState()
  }

  private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

  /** Estado atual da sincronização manual, observável pela UI. */
  val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  /**
   * `true` enquanto a sincronização com o Firebase está em andamento; oculta o botão de refresh na
   * UI.
   */
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private var realtimeJob: Job? = null

  /**
   * Dispara sincronização completa do escopo HOME com o Firebase Firestore, atualizando
   * estatísticas e atividades recentes no banco local.
   */
  fun syncHome() {
    viewModelScope.launch {
      _isSyncing.value = true
      syncScreenDataUseCase(ScreenDataScope.HOME)
      _isSyncing.value = false
    }
  }

  /**
   * Executa sincronização bidirecional completa entre Room e Firebase:
   * 1. Envia dados locais para o Firestore (push).
   * 2. Puxa todos os dados do Firestore para o cache local (pull).
   *
   * O estado da operação é exposto via [syncState].
   */
  fun syncData() {
    if (_syncState.value is SyncState.Loading) return
    viewModelScope.launch {
      _syncState.value = SyncState.Loading
      // Push: local → Firebase
      val pushResult = dataConnectService.seedAllCollectionsFromLocalCache()
      if (pushResult is FirebaseDataConnectService.Result.Error) {
        _syncState.value = SyncState.Error(pushResult.message)
        delay(4_000)
        _syncState.value = SyncState.Idle
        return@launch
      }
      // Pull: Firebase → local
      val pullResult = dataConnectService.syncScope(ScreenDataScope.APP_STARTUP)
      if (pullResult is FirebaseDataConnectService.Result.Error) {
        _syncState.value = SyncState.Error(pullResult.message)
        delay(4_000)
        _syncState.value = SyncState.Idle
        return@launch
      }
      _syncState.value = SyncState.Success
      delay(3_000)
      _syncState.value = SyncState.Idle
    }
  }

  /**
   * Inicia ou reinicia a escuta de atividades em tempo real via Firestore Realtime Listener.
   *
   * Cancela o job anterior antes de iniciar um novo. Erros são registrados como avisos no logcat.
   *
   * @param user Usuário logado; define o escopo (backoffice) das atividades visíveis.
   */
  fun observeRealtimeActivities(user: AppUser?) {
    realtimeJob?.cancel()
    realtimeJob = viewModelScope.launch {
      dataConnectService.observeRecentActivitiesRealtimeForBackoffice(user).collect { result ->
        if (result is FirebaseDataConnectService.Result.Error) {
          Log.w("HomeViewModel", "Falha no listener de atividades recentes: ${result.message}")
        }
      }
    }
  }
}
