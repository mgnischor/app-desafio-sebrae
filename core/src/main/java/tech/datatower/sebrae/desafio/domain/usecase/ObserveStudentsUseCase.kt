package tech.datatower.sebrae.desafio.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.model.Student
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para observar a lista de alunos de acordo com o papel do usuário autenticado.
 *
 * Regras de negócio encapsuladas:
 * - **ADMINISTRADOR** → todos os alunos de todas as empresas.
 * - **RESPONSAVEL** → apenas os alunos vinculados a este responsável na empresa ativa.
 * - **demais perfis** → alunos da empresa ativa.
 */
@Singleton
class ObserveStudentsUseCase
@Inject
constructor(
    private val repository: AppRepository,
) {
  @OptIn(ExperimentalCoroutinesApi::class)
  operator fun invoke(): Flow<List<Student>> =
      combine(AuthManager.currentUser, AuthManager.currentCompany) { user, company ->
            Pair(user, company)
          }
          .flatMapLatest { (user, company) ->
            when (user?.role) {
              UserRole.ADMINISTRADOR -> repository.observeAllStudents()
              UserRole.RESPONSAVEL -> {
                val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
                repository.observeStudentsByGuardian(user.id, cid)
              }
              else -> {
                val cid = company?.id ?: return@flatMapLatest flowOf(emptyList())
                repository.observeStudents(cid)
              }
            }
          }
}
