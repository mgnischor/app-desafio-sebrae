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
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.model.AppUser
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

  /** Estado de carregamento e resultado do perfil. */
  sealed class ProfileState {
    data object Loading : ProfileState()

    data class Success(val data: UserProfileData) : ProfileState()

    data class Error(val message: String) : ProfileState()
  }

  private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
  val state: StateFlow<ProfileState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      dataConnectService.syncScope(ScreenDataScope.TEACHERS)
      dataConnectService.syncScope(ScreenDataScope.STUDENTS)
      dataConnectService.syncScope(ScreenDataScope.CLASSES)
    }
    observeProfile()
  }

  private fun observeProfile() {
    viewModelScope.launch {
      combine(
              repository.observeUserById(userId),
              repository.observeTeachers(),
              repository.observeStudents(),
              repository.observeClasses(),
          ) { user, teachers, students, classes ->
            if (user == null) {
              return@combine ProfileState.Error("Usuário não encontrado.")
            }

            // Vincula pelo e-mail — mesma pessoa pode ser instrutor e aluno
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
          .collect { _state.value = it }
    }
  }
}
