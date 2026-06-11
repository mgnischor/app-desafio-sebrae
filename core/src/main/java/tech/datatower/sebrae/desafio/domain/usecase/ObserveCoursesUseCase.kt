/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /core/src/main/java/tech/datatower/sebrae/desafio/domain/usecase/ObserveCoursesUseCase.kt
    Descrição: UseCase para observar cursos de acordo com o papel do usuário autenticado.
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
package tech.datatower.sebrae.desafio.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.Course
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para observar a lista de cursos de acordo com o papel do usuário autenticado.
 *
 * Regras de negócio encapsuladas:
 * - **ADMINISTRADOR** → todos os cursos de todas as empresas.
 * - **demais perfis** → cursos da empresa ativa.
 */
@Singleton
class ObserveCoursesUseCase
@Inject
constructor(
    private val repository: AppRepository,
) {
  @OptIn(ExperimentalCoroutinesApi::class)
  operator fun invoke(): Flow<List<Course>> =
      combine(AuthManager.currentUser, AuthManager.currentCompany) { user, company ->
            Pair(user, company)
          }
          .flatMapLatest { (user, company) ->
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllCourses()
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeCourses(cid)
            }
          }
}
