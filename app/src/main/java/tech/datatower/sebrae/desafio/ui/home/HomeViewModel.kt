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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.QuickStat
import tech.datatower.sebrae.desafio.data.model.RecentActivity
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

    val stats: StateFlow<List<QuickStat>> = repository.observeHomeQuickStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentActivities: StateFlow<List<RecentActivity>> = repository.observeRecentActivities(limit = 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncHome() {
        viewModelScope.launch {
            _isSyncing.value = true
            dataConnectService.syncScope(ScreenDataScope.HOME)
            _isSyncing.value = false
        }
    }

    fun observeRealtimeActivities(user: AppUser?) {
        viewModelScope.launch {
            dataConnectService.observeRecentActivitiesRealtimeForBackoffice(user).collect { result ->
                if (result is FirebaseDataConnectService.Result.Error) {
                    Log.w("HomeViewModel", "Falha no listener de atividades recentes: ${result.message}")
                }
            }
        }
    }
}
