/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/repository/AppGraph.kt
    Descrição: Grafo de dependências manual para acesso ao repositório em contextos sem injeção.
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
package tech.datatower.sebrae.desafio.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.GuardianStudentEntity
import tech.datatower.sebrae.desafio.data.model.UserRole
import tech.datatower.sebrae.desafio.data.remote.NoOpRemoteBootstrapper
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseRemoteBootstrapper

/**
 * Entry point Hilt para acesso às dependências singleton em contextos não-Hilt (ex: composables que
 * ainda usam AppGraph diretamente).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppGraphEntryPoint {
  fun repository(): AppRepository

  fun dataConnectService(): FirebaseDataConnectService

  fun dao(): AppDao
}

/**
 * Ponto de acesso estático às dependências gerenciadas pelo Hilt.
 *
 * Atua como bridge para composables que acessam [AppRepository] e [FirebaseDataConnectService] via
 * `LocalContext`, mantendo compatibilidade com o código existente enquanto o Hilt gerencia o ciclo
 * de vida dos singletons.
 */
object AppGraph {
  private const val TAG = "AppGraph"
  private const val FIREBASE_REMOTE_BOOTSTRAP_ENABLED = true
  private const val FIREBASE_AUTO_SEED_ALL_COLLECTIONS_FROM_LOCAL = false
  private val graphScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private fun entryPoint(context: Context): AppGraphEntryPoint =
      EntryPointAccessors.fromApplication(
          context.applicationContext,
          AppGraphEntryPoint::class.java,
      )

  /** Retorna o [AppRepository] singleton provido pelo Hilt. */
  fun repository(context: Context): AppRepository = entryPoint(context).repository()

  /** Retorna o [FirebaseDataConnectService] singleton provido pelo Hilt. */
  fun dataConnectService(context: Context): FirebaseDataConnectService =
      entryPoint(context).dataConnectService()

  /**
   * Pré-aquece os singletons pesados (Room + Firebase) antes do primeiro frame da UI e inicia as
   * rotinas de bootstrap e sincronização remota em background.
   */
  fun warmUp(context: Context) {
    graphScope.launch {
      val service = dataConnectService(context)
      val dao = entryPoint(context).dao()

      if (FIREBASE_AUTO_SEED_ALL_COLLECTIONS_FROM_LOCAL) {
        when (val seedResult = service.seedAllCollectionsFromLocalCache()) {
          is FirebaseDataConnectService.Result.Success ->
              Log.d(TAG, "Seed automatico de collections concluido: ${seedResult.data}")
          is FirebaseDataConnectService.Result.Error ->
              Log.e(TAG, "Falha no seed automatico de collections: ${seedResult.message}")
          FirebaseDataConnectService.Result.Loading -> Unit
        }
      }

      val remoteBootstrapper =
          if (FIREBASE_REMOTE_BOOTSTRAP_ENABLED) {
            FirebaseRemoteBootstrapper.create(context.applicationContext, dao)
          } else {
            NoOpRemoteBootstrapper
          }

      remoteBootstrapper.bootstrapIntoLocalCache()
      service.syncAllData()

      // Seed guardian-student links for demo RESPONSAVEL user
      seedGuardianStudentLinks(dao)
    }
  }

  /**
   * Vincula o usuário RESPONSAVEL de demonstração aos dois primeiros alunos
   * disponíveis na empresa, caso ainda não existam vínculos.
   */
  private suspend fun seedGuardianStudentLinks(dao: AppDao) {
    val guardian =
        AuthManager.users.value.firstOrNull { it.role == UserRole.RESPONSAVEL } ?: return
    val students = dao.getStudentsOnce()
    if (students.isEmpty()) return
    // Link guardian to first 2 students in the company
    val linked = students.take(2).map { s ->
      GuardianStudentEntity(guardianUserId = guardian.id, studentId = s.id)
    }
    dao.insertGuardianStudents(linked)
  }
}
