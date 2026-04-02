/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/teachers/TeacherDetailViewModel.kt
    Descrição: ViewModel da tela de detalhes de professor, carregando dados do professor e suas turmas.
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
package tech.datatower.sebrae.desafio.ui.teachers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.SchoolClass
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.ScreenDataScope
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import tech.datatower.sebrae.desafio.navigation.AppRoutes
import javax.inject.Inject

@HiltViewModel
class TeacherDetailViewModel
@Inject
constructor(
    private val repository: AppRepository,
    private val dataConnectService: FirebaseDataConnectService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val teacherId: Int = checkNotNull(savedStateHandle[AppRoutes.TEACHER_ID_ARG])

  val teacher: StateFlow<Teacher?> =
      repository
          .observeTeacherById(teacherId)
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  @OptIn(ExperimentalCoroutinesApi::class)
  val classes: StateFlow<List<SchoolClass>> =
      combine(teacher, AuthManager.currentUser, AuthManager.currentCompany) { t, user, company ->
            Triple(t, user, company)
          }
          .flatMapLatest { (t, user, company) ->
            val name = t?.name ?: return@flatMapLatest flowOf(emptyList())
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllClasses().map { list -> list.filter { it.instructor == name } }
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeClassesByTeacher(cid, name)
            }
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  init {
    viewModelScope.launch { dataConnectService.syncScope(ScreenDataScope.TEACHER_DETAIL) }
  }
}
