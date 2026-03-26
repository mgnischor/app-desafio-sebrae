package tech.datatower.sebrae.desafio.data.remote.firebase

/**
 * Define os escopos de sincronizacao por tela.
 *
 * Cada escopo representa um conjunto minimo de dados remotos necessario para renderizar a tela com
 * o Room atuando apenas como cache local.
 */
enum class ScreenDataScope {
  HOME,
  COURSES,
  COURSE_DETAIL,
  CLASSES,
  CLASS_DETAIL,
  STUDENTS,
  STUDENT_MONITORING,
  TEACHERS,
  TEACHER_DETAIL,
  CERTIFICATES,
  CALENDAR,
  REPORTS,
  SETTINGS,
  APP_STARTUP,
}

/** Lista de tarefas de sincronizacao disponiveis no Firebase. */
enum class FirebaseSyncTask {
  COURSES,
  CLASSES,
  STUDENTS,
  TEACHERS,
  CERTIFICATES,
  CALENDAR_EVENTS,
  RECENT_ACTIVITIES,
  MONTHLY_ENROLLMENTS,
  ATTENDANCE,
  BEHAVIORS,
  PEDAGOGICAL_NEEDS,
  PSYCHOLOGICAL_NEEDS,
  PARENT_FOLLOW_UPS,
  SETTINGS,
}

/**
 * Planeja as tarefas de sincronizacao necessarias para cada tela.
 *
 * A estrategia usa principio de menor privilegio de dados: cada tela baixa somente as colecoes
 * remotas realmente necessarias para sua exibicao.
 */
object FirebaseScreenSyncPlanner {
  /**
   * Retorna as tarefas de sincronizacao para um escopo de tela.
   *
   * @param scope Escopo funcional da tela atual.
   * @return Conjunto de tarefas que devem ser sincronizadas do Firebase.
   */
  fun tasksFor(scope: ScreenDataScope): Set<FirebaseSyncTask> {
    return when (scope) {
      ScreenDataScope.HOME ->
          setOf(
              FirebaseSyncTask.COURSES,
              FirebaseSyncTask.CLASSES,
              FirebaseSyncTask.STUDENTS,
              FirebaseSyncTask.RECENT_ACTIVITIES,
          )
      ScreenDataScope.COURSES,
      ScreenDataScope.COURSE_DETAIL -> setOf(FirebaseSyncTask.COURSES, FirebaseSyncTask.CLASSES)
      ScreenDataScope.CLASSES,
      ScreenDataScope.CLASS_DETAIL -> setOf(FirebaseSyncTask.CLASSES, FirebaseSyncTask.STUDENTS)
      ScreenDataScope.STUDENTS -> setOf(FirebaseSyncTask.STUDENTS, FirebaseSyncTask.CLASSES)
      ScreenDataScope.STUDENT_MONITORING ->
          setOf(
              FirebaseSyncTask.STUDENTS,
              FirebaseSyncTask.ATTENDANCE,
              FirebaseSyncTask.BEHAVIORS,
              FirebaseSyncTask.PEDAGOGICAL_NEEDS,
              FirebaseSyncTask.PSYCHOLOGICAL_NEEDS,
              FirebaseSyncTask.PARENT_FOLLOW_UPS,
          )
      ScreenDataScope.TEACHERS,
      ScreenDataScope.TEACHER_DETAIL -> setOf(FirebaseSyncTask.TEACHERS, FirebaseSyncTask.CLASSES)
      ScreenDataScope.CERTIFICATES -> setOf(FirebaseSyncTask.CERTIFICATES)
      ScreenDataScope.CALENDAR -> setOf(FirebaseSyncTask.CALENDAR_EVENTS)
      ScreenDataScope.REPORTS ->
          setOf(
              FirebaseSyncTask.COURSES,
              FirebaseSyncTask.CLASSES,
              FirebaseSyncTask.STUDENTS,
              FirebaseSyncTask.TEACHERS,
              FirebaseSyncTask.CERTIFICATES,
              FirebaseSyncTask.MONTHLY_ENROLLMENTS,
          )
      ScreenDataScope.SETTINGS -> setOf(FirebaseSyncTask.SETTINGS)
      ScreenDataScope.APP_STARTUP ->
          setOf(
              FirebaseSyncTask.COURSES,
              FirebaseSyncTask.CLASSES,
              FirebaseSyncTask.STUDENTS,
              FirebaseSyncTask.TEACHERS,
              FirebaseSyncTask.CERTIFICATES,
              FirebaseSyncTask.CALENDAR_EVENTS,
              FirebaseSyncTask.RECENT_ACTIVITIES,
              FirebaseSyncTask.MONTHLY_ENROLLMENTS,
              FirebaseSyncTask.ATTENDANCE,
              FirebaseSyncTask.BEHAVIORS,
              FirebaseSyncTask.PEDAGOGICAL_NEEDS,
              FirebaseSyncTask.PSYCHOLOGICAL_NEEDS,
              FirebaseSyncTask.PARENT_FOLLOW_UPS,
              FirebaseSyncTask.SETTINGS,
          )
    }
  }
}
