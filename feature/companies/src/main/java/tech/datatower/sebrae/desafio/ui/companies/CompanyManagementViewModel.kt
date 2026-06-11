/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /feature/companies/src/main/java/tech/datatower/sebrae/desafio/ui/companies/CompanyManagementViewModel.kt
    Descrição: ViewModel da tela de gestão de empresas com busca e filtragem reativa.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    "Prêmio Educador Transformador" do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
package tech.datatower.sebrae.desafio.ui.companies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Company
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject

/**
 * ViewModel da tela de gestão de empresas.
 *
 * Observa a lista de empresas em tempo real e fornece operações de criação e remoção com remoção
 * otimista (a empresa some da lista imediatamente e reaparece se Firestore falhar). Todas as
 * operações requerem um requester com perfil de ADMINISTRADOR.
 */
@HiltViewModel
class CompanyManagementViewModel
@Inject
constructor(
    private val repository: AppRepository,
) : ViewModel() {

  /**
   * Estado de carregamento da lista de empresas.
   *
   * Transita de [Loading] para [Success] ou [Error] conforme o resultado da observação reativa.
   */
  sealed class CompaniesState {
    /** Lista ainda está sendo carregada do banco local. */
    data object Loading : CompaniesState()

    /**
     * Lista de empresas disponível com sucesso.
     *
     * @property companies Empresas ativas visíveis na UI.
     */
    data class Success(val companies: List<Company>) : CompaniesState()

    /**
     * Falha ao carregar; a [StateFlow] não emite mais updates.
     *
     * @property message Mensagem de erro para exibir na UI.
     */
    data class Error(val message: String) : CompaniesState()
  }

  /**
   * Resultado de uma operação de criação ou remoção de empresa.
   *
   * Deve ser limpo após exibido na UI via [clearActionResult].
   */
  sealed class ActionResult {
    /** Nenhuma operação pendente ou já tratada. */
    data object Idle : ActionResult()

    /** Operação concluída com sucesso. */
    data object Success : ActionResult()

    /**
     * Operação falhou.
     *
     * @property message Descrição do erro para exibir ao usuário.
     */
    data class Error(val message: String) : ActionResult()
  }

  private val _state = MutableStateFlow<CompaniesState>(CompaniesState.Loading)
  val state: StateFlow<CompaniesState> = _state.asStateFlow()

  private val _actionResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
  val actionResult: StateFlow<ActionResult> = _actionResult.asStateFlow()

  private val _locallyDeletedIds = MutableStateFlow<Set<Int>>(emptySet())

  private var observeJob: Job? = null

  init {
    observeCompanies()
  }

  /** Observa a lista de empresas do Room, excluindo IDs removidos. */
  private fun observeCompanies() {
    observeJob?.cancel()
    observeJob = viewModelScope.launch {
      repository.observeCompanies().collect { companies ->
        val deletedIds = _locallyDeletedIds.value
        _state.value = CompaniesState.Success(companies.filter { it.id !in deletedIds })
      }
    }
  }

  /**
   * Cria uma nova empresa com o nome e CNPJ informados.
   *
   * O ID é gerado localmente como `max(ids_atuais) + 1`. Atualiza [actionResult] com
   * [ActionResult.Success] ou [ActionResult.Error].
   *
   * @param requester Usuário que realiza a operação (deve ser ADMINISTRADOR).
   * @param name Nome da empresa.
   * @param cnpj CNPJ da empresa.
   */
  fun createCompany(requester: AppUser?, name: String, cnpj: String) {
    viewModelScope.launch {
      try {
        val companies = (_state.value as? CompaniesState.Success)?.companies ?: emptyList()
        val nextId = (companies.maxOfOrNull { it.id } ?: 0) + 1
        val company = Company(id = nextId, name = name.trim(), cnpj = cnpj.trim())
        repository.upsertCompanyForAdmin(requester, company)
        _actionResult.value = ActionResult.Success
      } catch (e: SecurityException) {
        _actionResult.value = ActionResult.Error(e.message ?: "Acesso negado.")
      } catch (e: Exception) {
        _actionResult.value =
            ActionResult.Error(e.message ?: "Não foi possível cadastrar a empresa.")
      }
    }
  }

  /**
   * Remove uma empresa com remoção otimista: some da lista imediatamente e reaparece se falhar.
   *
   * @param requester Usuário que realiza a operação (deve ser ADMINISTRADOR).
   * @param companyId ID da empresa a remover.
   */
  fun deleteCompany(requester: AppUser?, companyId: Int) {
    viewModelScope.launch {
      _locallyDeletedIds.update { it + companyId }
      try {
        repository.deleteCompanyForAdmin(requester, companyId)
        _actionResult.value = ActionResult.Success
      } catch (e: SecurityException) {
        _locallyDeletedIds.update { it - companyId }
        _actionResult.value = ActionResult.Error(e.message ?: "Acesso negado.")
      } catch (e: Exception) {
        _locallyDeletedIds.update { it - companyId }
        _actionResult.value = ActionResult.Error(e.message ?: "Não foi possível remover a empresa.")
      }
    }
  }

  /** Redefine [actionResult] para [ActionResult.Idle] após o resultado ser tratado pela UI. */
  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
