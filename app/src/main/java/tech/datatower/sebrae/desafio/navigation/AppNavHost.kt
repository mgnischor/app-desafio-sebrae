package tech.datatower.sebrae.desafio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.datatower.sebrae.desafio.data.auth.AuthManager
import tech.datatower.sebrae.desafio.ui.calendar.CalendarScreen
import tech.datatower.sebrae.desafio.ui.certificates.CertificatesScreen
import tech.datatower.sebrae.desafio.ui.classes.ClassCreateScreen
import tech.datatower.sebrae.desafio.ui.classes.ClassDetailScreen
import tech.datatower.sebrae.desafio.ui.classes.ClassesScreen
import tech.datatower.sebrae.desafio.ui.courses.CourseCreateScreen
import tech.datatower.sebrae.desafio.ui.courses.CourseDetailScreen
import tech.datatower.sebrae.desafio.ui.courses.CoursesScreen
import tech.datatower.sebrae.desafio.ui.home.HomeScreen
import tech.datatower.sebrae.desafio.ui.home.RecentActivitiesScreen
import tech.datatower.sebrae.desafio.ui.login.LoginScreen
import tech.datatower.sebrae.desafio.ui.reports.ReportsScreen
import tech.datatower.sebrae.desafio.ui.settings.SettingsScreen
import tech.datatower.sebrae.desafio.ui.students.StudentCreateScreen
import tech.datatower.sebrae.desafio.ui.students.StudentMonitoringScreen
import tech.datatower.sebrae.desafio.ui.students.StudentsScreen
import tech.datatower.sebrae.desafio.ui.teachers.TeacherCreateScreen
import tech.datatower.sebrae.desafio.ui.teachers.TeacherDetailScreen
import tech.datatower.sebrae.desafio.ui.teachers.TeachersScreen
import tech.datatower.sebrae.desafio.ui.users.UserManagementScreen

/**
 * Declara o grafo de navegação principal da aplicação.
 *
 * Define a tela inicial (login) e mapeia cada rota de `AppRoutes` para sua respectiva tela Compose,
 * centralizando a orquestração de navegação. A rota de início é a tela de login; após autenticação
 * bem-sucedida, o usuário é direcionado para a Home sem possibilidade de voltar para o login via
 * botão "back".
 *
 * @param navController Controlador responsável por navegar entre destinos e gerenciar o back stack.
 */
@Composable
fun AppNavHost(navController: NavHostController) {
  val currentUser by AuthManager.currentUser.collectAsState()

  NavHost(
      navController = navController,
      startDestination = AppRoutes.LOGIN,
  ) {
    composable(AppRoutes.LOGIN) {
      LoginScreen(
          onLoginSuccess = {
            navController.navigate(AppRoutes.HOME) { popUpTo(AppRoutes.LOGIN) { inclusive = true } }
          }
      )
    }
    composable(AppRoutes.HOME) {
      HomeScreen(
          user = currentUser,
          onModuleClick = { route -> navController.navigate(route) },
          onOpenRecentActivities = { navController.navigate(AppRoutes.RECENT_ACTIVITIES) },
          onLogout = {
            AuthManager.logout()
            navController.navigate(AppRoutes.LOGIN) { popUpTo(AppRoutes.HOME) { inclusive = true } }
          },
      )
    }
    composable(AppRoutes.RECENT_ACTIVITIES) {
      RecentActivitiesScreen(currentUser = currentUser, onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.STUDENTS) {
      StudentsScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
          onCreateStudent = { navController.navigate(AppRoutes.STUDENT_CREATE) },
          onOpenStudentMonitoring = { studentId ->
            navController.navigate(AppRoutes.studentMonitoring(studentId))
          },
      )
    }
    composable(AppRoutes.STUDENT_CREATE) {
      StudentCreateScreen(currentUser = currentUser, onBack = { navController.popBackStack() })
    }
    composable(
        route = AppRoutes.STUDENT_MONITORING,
        arguments = listOf(navArgument(AppRoutes.STUDENT_ID_ARG) { type = NavType.IntType }),
    ) { backStackEntry ->
      val studentId =
          backStackEntry.arguments?.getInt(AppRoutes.STUDENT_ID_ARG) ?: return@composable
      StudentMonitoringScreen(studentId = studentId, onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.COURSES) {
      CoursesScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
          onCreateCourse = { navController.navigate(AppRoutes.COURSE_CREATE) },
          onOpenCourseDetail = { courseId ->
            navController.navigate(AppRoutes.courseDetail(courseId))
          },
      )
    }
    composable(AppRoutes.COURSE_CREATE) {
      CourseCreateScreen(currentUser = currentUser, onBack = { navController.popBackStack() })
    }
    composable(
        route = AppRoutes.COURSE_DETAIL,
        arguments = listOf(navArgument(AppRoutes.COURSE_ID_ARG) { type = NavType.IntType }),
    ) { backStackEntry ->
      val courseId = backStackEntry.arguments?.getInt(AppRoutes.COURSE_ID_ARG) ?: return@composable
      CourseDetailScreen(courseId = courseId, onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.CLASSES) {
      ClassesScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
          onCreateClass = { navController.navigate(AppRoutes.CLASS_CREATE) },
          onOpenClassDetail = { classId -> navController.navigate(AppRoutes.classDetail(classId)) },
      )
    }
    composable(AppRoutes.CLASS_CREATE) {
      ClassCreateScreen(currentUser = currentUser, onBack = { navController.popBackStack() })
    }
    composable(
        route = AppRoutes.CLASS_DETAIL,
        arguments = listOf(navArgument(AppRoutes.CLASS_ID_ARG) { type = NavType.IntType }),
    ) { backStackEntry ->
      val classId = backStackEntry.arguments?.getInt(AppRoutes.CLASS_ID_ARG) ?: return@composable
      ClassDetailScreen(classId = classId, onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.TEACHERS) {
      TeachersScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
          onCreateTeacher = { navController.navigate(AppRoutes.TEACHER_CREATE) },
          onOpenTeacherDetail = { teacherId ->
            navController.navigate(AppRoutes.teacherDetail(teacherId))
          },
      )
    }
    composable(AppRoutes.TEACHER_CREATE) {
      TeacherCreateScreen(currentUser = currentUser, onBack = { navController.popBackStack() })
    }
    composable(
        route = AppRoutes.TEACHER_DETAIL,
        arguments = listOf(navArgument(AppRoutes.TEACHER_ID_ARG) { type = NavType.IntType }),
    ) { backStackEntry ->
      val teacherId =
          backStackEntry.arguments?.getInt(AppRoutes.TEACHER_ID_ARG) ?: return@composable
      TeacherDetailScreen(teacherId = teacherId, onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.REPORTS) { ReportsScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.CERTIFICATES) {
      CertificatesScreen(onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.CALENDAR) {
      CalendarScreen(currentUser = currentUser, onBack = { navController.popBackStack() })
    }
    composable(AppRoutes.SETTINGS) {
      SettingsScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
          onOpenUserManagement = { navController.navigate(AppRoutes.USERS_MANAGEMENT) },
      )
    }
    composable(AppRoutes.USERS_MANAGEMENT) {
      UserManagementScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
      )
    }
  }
}
