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

@HiltViewModel
class CompanyManagementViewModel
@Inject
constructor(
    private val repository: AppRepository,
) : ViewModel() {

  sealed class CompaniesState {
    data object Loading : CompaniesState()

    data class Success(val companies: List<Company>) : CompaniesState()

    data class Error(val message: String) : CompaniesState()
  }

  sealed class ActionResult {
    data object Idle : ActionResult()

    data object Success : ActionResult()

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

  private fun observeCompanies() {
    observeJob?.cancel()
    observeJob = viewModelScope.launch {
      repository.observeCompanies().collect { companies ->
        val deletedIds = _locallyDeletedIds.value
        _state.value =
            CompaniesState.Success(companies.filter { it.id !in deletedIds })
      }
    }
  }

  fun createCompany(requester: AppUser?, name: String, cnpj: String) {
    viewModelScope.launch {
      try {
        val companies =
            (_state.value as? CompaniesState.Success)?.companies ?: emptyList()
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
        _actionResult.value =
            ActionResult.Error(e.message ?: "Não foi possível remover a empresa.")
      }
    }
  }

  fun clearActionResult() {
    _actionResult.value = ActionResult.Idle
  }
}
