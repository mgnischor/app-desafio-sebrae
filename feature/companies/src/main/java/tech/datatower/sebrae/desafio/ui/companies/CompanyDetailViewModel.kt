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

/**
 * ViewModel da tela de detalhes de empresa.
 *
 * Expõe os dados da empresa ([company]), os usuários vinculados ([linkedUserIds]) e a lista global
 * de usuários ([allUsers]) para gestão de acesso. Suporta atualização dos dados da empresa e
 * concessão/revogação de acesso de usuários.
 */
@HiltViewModel
class CompanyDetailViewModel
@Inject
constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val companyId: Int = checkNotNull(savedStateHandle[AppRoutes.COMPANY_ID_ARG])

  /** Dados completos da empresa cujo ID foi recebido via [SavedStateHandle]. */
  val company: StateFlow<Company?> =
      repository
          .observeCompanyById(companyId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  /** IDs dos usuários vinculados a esta empresa; atualizado reativamente pelo Room. */
  val linkedUserIds: StateFlow<Set<Int>> =
      repository
          .observeUserIdsByCompany(companyId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

  /** Lista de todos os usuários cadastrados no sistema, exposta via [AuthManager.users]. */
  val allUsers: StateFlow<List<AppUser>> =
      AuthManager.users.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Resultado de uma operação de atualização ou vínculo de acesso. */
  sealed class SaveState {
    /** Nenhuma operação pendente. */
    data object Idle : SaveState()

    /** Operação concluída com sucesso. */
    data object Success : SaveState()

    /**
     * Operação falhou.
     *
     * @property message Descrição do erro para exibir ao usuário.
     */
    data class Error(val message: String) : SaveState()
  }

  private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
  val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

  /**
   * Atualiza os dados cadastrais da empresa no Room e no Firestore.
   *
   * @param requester Usuário que realiza a operação (deve ser ADMINISTRADOR).
   * @param name Novo nome da empresa.
   * @param cnpj Novo CNPJ da empresa.
   * @param isActive Indica se a empresa deve permanecer ativa.
   */
  fun updateCompany(requester: AppUser?, name: String, cnpj: String, isActive: Boolean) {
    viewModelScope.launch {
      try {
        val updated =
            Company(id = companyId, name = name.trim(), cnpj = cnpj.trim(), isActive = isActive)
        repository.upsertCompanyForAdmin(requester, updated)
        _saveState.value = SaveState.Success
      } catch (e: SecurityException) {
        _saveState.value = SaveState.Error(e.message ?: "Acesso negado.")
      } catch (e: Exception) {
        _saveState.value = SaveState.Error(e.message ?: "Não foi possível salvar a empresa.")
      }
    }
  }

  /**
   * Concede ou revoga o acesso de um usuário à empresa.
   *
   * @param requester Usuário que realiza a operação (deve ser ADMINISTRADOR).
   * @param userId ID do usuário a vincular ou desvincular.
   * @param grant `true` para conceder acesso; `false` para revogá-lo.
   */
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

  /** Redefine [saveState] para [SaveState.Idle] após o resultado ser tratado pela UI. */
  fun clearSaveState() {
    _saveState.value = SaveState.Idle
  }
}
