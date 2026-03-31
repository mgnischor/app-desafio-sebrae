package tech.datatower.sebrae.desafio.ui.companies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Company
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import javax.inject.Inject

@HiltViewModel
class CompanyDetailViewModel
@Inject
constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val companyId: Int = checkNotNull(savedStateHandle[AppRoutes.COMPANY_ID_ARG])

  val company: StateFlow<Company?> =
      repository
          .observeCompanyById(companyId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  /** IDs dos usuarios vinculados a esta empresa. */
  val linkedUserIds: StateFlow<Set<Int>> =
      repository
          .observeUserIdsByCompany(companyId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

  /** Lista de todos os usuarios do sistema (via AuthManager). */
  val allUsers: StateFlow<List<AppUser>> =
      AuthManager.users
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  sealed class SaveState {
    data object Idle : SaveState()

    data object Success : SaveState()

    data class Error(val message: String) : SaveState()
  }

  private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
  val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

  fun updateCompany(requester: AppUser?, name: String, cnpj: String, isActive: Boolean) {
    viewModelScope.launch {
      try {
        val updated = Company(id = companyId, name = name.trim(), cnpj = cnpj.trim(), isActive = isActive)
        repository.upsertCompanyForAdmin(requester, updated)
        _saveState.value = SaveState.Success
      } catch (e: SecurityException) {
        _saveState.value = SaveState.Error(e.message ?: "Acesso negado.")
      } catch (e: Exception) {
        _saveState.value = SaveState.Error(e.message ?: "Não foi possível salvar a empresa.")
      }
    }
  }

  fun toggleUserAccess(requester: AppUser?, userId: Int, grant: Boolean) {
    viewModelScope.launch {
      try {
        if (grant) {
          repository.grantUserCompanyAccess(requester, userId, companyId)
        } else {
          repository.revokeUserCompanyAccess(requester, userId, companyId)
        }
      } catch (e: Exception) {
        _saveState.value = SaveState.Error(e.message ?: "Erro ao alterar vinculo.")
      }
    }
  }

  fun clearSaveState() {
    _saveState.value = SaveState.Idle
  }
}
