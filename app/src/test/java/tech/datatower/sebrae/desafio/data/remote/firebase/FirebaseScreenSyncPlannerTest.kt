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
