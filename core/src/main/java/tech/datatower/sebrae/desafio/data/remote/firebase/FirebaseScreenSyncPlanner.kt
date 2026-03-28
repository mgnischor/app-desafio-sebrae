/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/remote/firebase/FirebaseScreenSyncPlanner.kt
    Descrição: Planejador de sincronização de dados por tela com o Firebase.
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
package tech.datatower.sebrae.desafio.data.remote.firebase

/**
 * Define os escopos de sincronização por tela.
 *
 * Cada escopo representa um conjunto mínimo de dados remotos necessário para renderizar a tela com
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

/** Lista de tarefas de sincronização disponíveis no Firebase. */
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
 * Planeja as tarefas de sincronização necessárias para cada tela.
 *
 * A estratégia usa princípio de menor privilégio de dados: cada tela baixa somente as coleções
 * remotas realmente necessárias para sua exibição.
 */
object FirebaseScreenSyncPlanner {
  /**
   * Retorna as tarefas de sincronização para um escopo de tela.
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
