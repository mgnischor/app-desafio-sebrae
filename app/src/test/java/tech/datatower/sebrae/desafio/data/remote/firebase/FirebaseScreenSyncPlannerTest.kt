/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/test/java/tech/datatower/sebrae/desafio/data/remote/firebase/FirebaseScreenSyncPlannerTest.kt
    Descrição: Testes unitários do planejamento de sincronização remota por tela.
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
package tech.datatower.sebrae.desafio.data.remote.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitarios do planejamento de sincronizacao por tela.
 *
 * Garante que cada escopo mapeia apenas as colecoes remotas necessarias, reduzindo trafego e
 * limitando exposicao de dados desnecessarios.
 */
class FirebaseScreenSyncPlannerTest {
  /** Valida o escopo de acompanhamento do aluno com todas as dependencias criticas. */
  @Test
  fun studentMonitoringScopeIncludesAllMonitoringCollections() {
    val tasks = FirebaseScreenSyncPlanner.tasksFor(ScreenDataScope.STUDENT_MONITORING)

    assertTrue(tasks.contains(FirebaseSyncTask.STUDENTS))
    assertTrue(tasks.contains(FirebaseSyncTask.ATTENDANCE))
    assertTrue(tasks.contains(FirebaseSyncTask.BEHAVIORS))
    assertTrue(tasks.contains(FirebaseSyncTask.PEDAGOGICAL_NEEDS))
    assertTrue(tasks.contains(FirebaseSyncTask.PSYCHOLOGICAL_NEEDS))
    assertTrue(tasks.contains(FirebaseSyncTask.PARENT_FOLLOW_UPS))
    assertEquals(6, tasks.size)
  }

  /** Valida escopo minimo da tela de certificados para performance e menor superficie de dados. */
  @Test
  fun certificatesScopeIsMinimal() {
    val tasks = FirebaseScreenSyncPlanner.tasksFor(ScreenDataScope.CERTIFICATES)

    assertEquals(setOf(FirebaseSyncTask.CERTIFICATES), tasks)
  }

  /** Garante que o escopo de inicializacao cobre todos os dados necessarios para cache local. */
  @Test
  fun appStartupScopeContainsAllCollections() {
    val tasks = FirebaseScreenSyncPlanner.tasksFor(ScreenDataScope.APP_STARTUP)

    assertTrue(tasks.contains(FirebaseSyncTask.COURSES))
    assertTrue(tasks.contains(FirebaseSyncTask.CLASSES))
    assertTrue(tasks.contains(FirebaseSyncTask.STUDENTS))
    assertTrue(tasks.contains(FirebaseSyncTask.TEACHERS))
    assertTrue(tasks.contains(FirebaseSyncTask.CERTIFICATES))
    assertTrue(tasks.contains(FirebaseSyncTask.CALENDAR_EVENTS))
    assertTrue(tasks.contains(FirebaseSyncTask.RECENT_ACTIVITIES))
    assertTrue(tasks.contains(FirebaseSyncTask.MONTHLY_ENROLLMENTS))
    assertTrue(tasks.contains(FirebaseSyncTask.ATTENDANCE))
    assertTrue(tasks.contains(FirebaseSyncTask.BEHAVIORS))
    assertTrue(tasks.contains(FirebaseSyncTask.PEDAGOGICAL_NEEDS))
    assertTrue(tasks.contains(FirebaseSyncTask.PSYCHOLOGICAL_NEEDS))
    assertTrue(tasks.contains(FirebaseSyncTask.PARENT_FOLLOW_UPS))
    assertTrue(tasks.contains(FirebaseSyncTask.SETTINGS))
    assertEquals(14, tasks.size)
  }
}
