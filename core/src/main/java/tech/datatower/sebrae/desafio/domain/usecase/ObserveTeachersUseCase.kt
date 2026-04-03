package tech.datatower.sebrae.desafio.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.Teacher
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para observar a lista de professores de acordo com o papel do usuário autenticado.
 *
 * Regras de negócio encapsuladas:
 * - **ADMINISTRADOR** → todos os professores de todas as empresas.
 * - **demais perfis** → professores da empresa ativa.
 */
@Singleton
class ObserveTeachersUseCase
@Inject
constructor(
    private val repository: AppRepository,
) {
  @OptIn(ExperimentalCoroutinesApi::class)
  operator fun invoke(): Flow<List<Teacher>> =
      combine(AuthManager.currentUser, AuthManager.currentCompany) { user, company ->
            Pair(user, company)
          }
          .flatMapLatest { (user, company) ->
            if (user?.role == UserRole.ADMINISTRADOR) {
              repository.observeAllTeachers()
            } else {
              val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
              repository.observeTeachers(cid)
            }
          }
}
