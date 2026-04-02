/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/users/UserProfileViewModel.kt
    Descrição: ViewModel da tela de perfil de usuário, agregando dados de instrutor e aluno.
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
package tech.datatower.sebrae.desafio.ui.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.AppUser
import tech.datatower.sebrae.desafio.data.model.Company
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import javax.inject.Inject

/**
 * Dados agregados de um usuário: credenciais de acesso + vínculos acadêmicos.
 *
 * @property user Dados de acesso ao sistema.
 * @property teacherProfile Registro de instrutor vinculado pelo e-mail, ou `null`.
 * @property classesTaught Turmas que o instrutor ministra.
 * @property studentProfile Registro de aluno vinculado pelo e-mail, ou `null`.
 * @property classesEnrolled Turmas em que o aluno está matriculado.
 */
data class UserProfileData(
    val user: AppUser,
    val teacherProfile: Teacher? = null,
    val classesTaught: List<SchoolClass> = emptyList(),
    val studentProfile: Student? = null,
    val classesEnrolled: List<SchoolClass> = emptyList(),
)

/** ViewModel da tela de perfil de usuário. */
@HiltViewModel
class UserProfileViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
) : ViewModel() {

  private val userId: Int = checkNotNull(savedStateHandle[AppRoutes.USER_ID_ARG])

  // ── Estados do perfil ────────────────────────────────────────────────────

  /** Estado de carregamento e resultado do perfil. */
  sealed class ProfileState {
    data object Loading : ProfileState()

    data class Success(val data: UserProfileData) : ProfileState()

    data class Error(val message: String) : ProfileState()
  }

  private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
  val state: StateFlow<ProfileState> = _state.asStateFlow()

  // ── Estados de acesso a empresa ──────────────────────────────────────────

  /** Resultado de uma operação de vínculo usuário-empresa. */
  sealed class CompanyAccessResult {
    data object Idle : CompanyAccessResult()

    data object Success : CompanyAccessResult()

    data class Error(val message: String) : CompanyAccessResult()
  }

  /** Lista de todas as empresas cadastradas no sistema. */
  private val _allCompanies = MutableStateFlow<List<Company>>(emptyList())
  val allCompanies: StateFlow<List<Company>> = _allCompanies.asStateFlow()

  /** IDs das empresas às quais este usuário tem acesso. */
  private val _userCompanyIds = MutableStateFlow<Set<Int>>(emptySet())
  val userCompanyIds: StateFlow<Set<Int>> = _userCompanyIds.asStateFlow()

  /** Resultado da última operação de concessão/revogação de acesso. */
  private val _companyAccessResult = MutableStateFlow<CompanyAccessResult>(CompanyAccessResult.Idle)
  val companyAccessResult: StateFlow<CompanyAccessResult> = _companyAccessResult.asStateFlow()

  // ── Inicialização ────────────────────────────────────────────────────────

  init {
    viewModelScope.launch {
      // Sincroniza usuários para que observeUserById() encontre o registro no Room,
      // mesmo quando o usuário veio do listener em tempo real do Firestore.
      dataConnectService.fetchUsers()
      // Sincroniza empresas e vínculos usuário-empresa para exibição na UI.
      dataConnectService.syncScope(ScreenDataScope.COMPANIES)
      dataConnectService.syncScope(ScreenDataScope.TEACHERS)
      dataConnectService.syncScope(ScreenDataScope.STUDENTS)
      dataConnectService.syncScope(ScreenDataScope.CLASSES)
    }
    observeProfile()
    observeCompanyAccess()
  }

  // ── Observações reativas ─────────────────────────────────────────────────

  private fun observeProfile() {
    viewModelScope.launch {
      // Combina empresa atual + dados do usuário em uma única observação.
      // NÃO usa filterNotNull() para não bloquear quando currentCompany ainda é null
      // (ex.: login demo ou Firestore sem empresas cadastradas).
      combine(
              AuthManager.currentCompany,
              repository.observeUserById(userId),
          ) { company, user ->
            Pair(company, user)
          }
          .flatMapLatest { (company, user) ->
            if (user == null) {
              return@flatMapLatest flowOf(ProfileState.Error("Usuário não encontrado."))
            }

            val cid =
                company?.id
                    ?: // Empresa ainda não carregada — exibe dados básicos sem vínculos acadêmicos
                    return@flatMapLatest flowOf(ProfileState.Success(UserProfileData(user = user)))

            // Empresa disponível — vincula instrutor/aluno pelo e-mail
            combine(
                repository.observeTeachers(cid),
                repository.observeStudents(cid),
                repository.observeClasses(cid),
            ) { teachers, students, classes ->
              val teacher = teachers.firstOrNull { it.email.equals(user.email, ignoreCase = true) }
              val student = students.firstOrNull { it.email.equals(user.email, ignoreCase = true) }

              val classesTaught =
                  teacher?.let { t ->
                    classes.filter { it.instructor.equals(t.name, ignoreCase = true) }
                  } ?: emptyList()

              val classesEnrolled =
                  student?.let { s ->
                    classes.filter { it.name.equals(s.enrolledClass, ignoreCase = true) }
                  } ?: emptyList()

              ProfileState.Success(
                  UserProfileData(
                      user = user,
                      teacherProfile = teacher,
                      classesTaught = classesTaught,
                      studentProfile = student,
                      classesEnrolled = classesEnrolled,
                  )
              )
            }
          }
          .collect { _state.value = it }
    }
  }

  private fun observeCompanyAccess() {
    viewModelScope.launch { repository.observeCompanies().collect { _allCompanies.value = it } }
    viewModelScope.launch {
      repository.observeCompaniesForUser(userId).collect { companies ->
        _userCompanyIds.value = companies.map { it.id }.toSet()
      }
    }
  }

  // ── Ações ────────────────────────────────────────────────────────────────

  /**
   * Concede ou revoga o acesso de um usuário a uma empresa.
   *
   * @param requester Usuário logado que realiza a ação (deve ser ADMINISTRADOR).
   * @param companyId ID da empresa a conceder/revogar.
   * @param currentlyHasAccess Se `true`, revoga o acesso; se `false`, concede.
   */
  fun toggleCompanyAccess(requester: AppUser?, companyId: Int, currentlyHasAccess: Boolean) {
    viewModelScope.launch {
      val result =
          if (currentlyHasAccess) {
            dataConnectService.revokeUserCompanyAccess(requester, userId, companyId)
          } else {
            dataConnectService.grantUserCompanyAccess(requester, userId, companyId)
          }
      _companyAccessResult.value =
          when (result) {
            is FirebaseDataConnectService.Result.Success -> CompanyAccessResult.Success
            is FirebaseDataConnectService.Result.Error ->
                CompanyAccessResult.Error(
                    result.message
                        .ifBlank { result.exception.message.orEmpty() }
                        .ifBlank { "Operação falhou. Verifique suas permissões." }
                )
            FirebaseDataConnectService.Result.Loading -> CompanyAccessResult.Idle
          }
    }
  }

  /** Limpa o resultado da última operação de acesso a empresa. */
  fun clearCompanyAccessResult() {
    _companyAccessResult.value = CompanyAccessResult.Idle
  }
}
