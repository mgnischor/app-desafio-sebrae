/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/settings/SettingsViewModel.kt
    Descrição: ViewModel da tela de configurações, responsável por persistir preferências do usuário.
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
package tech.datatower.sebrae.desafio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppSettings
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

/**
 * ViewModel da tela de configurações do aplicativo.
 *
 * Expõe [settings] como [StateFlow] reativo persistido via Room.
 * Fornece métodos para atualizar cada preferência individualmente e para
 * limpar o armazenamento local preservando os cadastros de usuários.
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  /** Preferências atuais do aplicativo, atualizadas reativamente pelo Room. */
  val settings: StateFlow<AppSettings> =
      repository
          .observeSettings()
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5_000),
              AppSettings(
                  darkMode = false,
                  pushEnabled = true,
                  emailEnabled = false,
                  language = "pt",
              ),
          )

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.SETTINGS) }
  }

  /**
   * Ativa ou desativa o tema escuro (dark mode).
   * @param enabled `true` para ativar o modo escuro.
   */
  fun updateDarkMode(enabled: Boolean) {
    viewModelScope.launch { repository.updateDarkMode(enabled) }
  }

  /**
   * Ativa ou desativa notificações push.
   * @param enabled `true` para receber notificações push.
   */
  fun updatePushEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.updatePushEnabled(enabled) }
  }

  /**
   * Ativa ou desativa notificações por e-mail.
   * @param enabled `true` para receber notificações por e-mail.
   */
  fun updateEmailEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.updateEmailEnabled(enabled) }
  }

  /**
   * Atualiza o idioma preferido do aplicativo.
   * @param language Código ISO 639-1 do idioma (ex.: `"pt"`, `"en"`).
   */
  fun updateLanguage(language: String) {
    viewModelScope.launch { repository.updateLanguage(language) }
  }

  /**
   * Limpa todas as tabelas do banco local Room, exceto a tabela de usuários.
   * Usado para redefinir os dados de demonstração sem perder os logins cadastrados.
   */
  fun clearStoragePreservingUsers() {
    viewModelScope.launch { repository.clearStoragePreservingUsers() }
  }

  /**
   * Executa reset completo da base de dados local (Room) e remota (Firebase). Acessível apenas ao
   * administrador — verificar permissão na UI antes de chamar.
   */
  fun resetAllData() {
    viewModelScope.launch {
      dataConnectService.deleteAllCollections()
      repository.resetAllData()
    }
  }
}
