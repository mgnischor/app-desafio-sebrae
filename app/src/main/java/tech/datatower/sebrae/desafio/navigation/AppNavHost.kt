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
import tech.datatower.sebrae.desafio.ui.classes.ClassDetailScreen
import tech.datatower.sebrae.desafio.ui.classes.ClassesScreen
import tech.datatower.sebrae.desafio.ui.courses.CourseDetailScreen
import tech.datatower.sebrae.desafio.ui.courses.CoursesScreen
import tech.datatower.sebrae.desafio.ui.home.HomeScreen
import tech.datatower.sebrae.desafio.ui.login.LoginScreen
import tech.datatower.sebrae.desafio.ui.reports.ReportsScreen
import tech.datatower.sebrae.desafio.ui.settings.SettingsScreen
import tech.datatower.sebrae.desafio.ui.students.StudentMonitoringScreen
import tech.datatower.sebrae.desafio.ui.students.StudentsScreen
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
      val currentUser by AuthManager.currentUser.collectAsState()
      HomeScreen(
          user = currentUser,
          onModuleClick = { route -> navController.navigate(route) },
          onLogout = {
            AuthManager.logout()
            navController.navigate(AppRoutes.LOGIN) { popUpTo(AppRoutes.HOME) { inclusive = true } }
          },
      )
    }
    composable(AppRoutes.STUDENTS) {
      StudentsScreen(
          onBack = { navController.popBackStack() },
          onOpenStudentMonitoring = { studentId ->
            navController.navigate(AppRoutes.studentMonitoring(studentId))
          },
      )
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
          onBack = { navController.popBackStack() },
          onOpenCourseDetail = { courseId ->
            navController.navigate(AppRoutes.courseDetail(courseId))
          },
      )
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
          onBack = { navController.popBackStack() },
          onOpenClassDetail = { classId -> navController.navigate(AppRoutes.classDetail(classId)) },
      )
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
          onBack = { navController.popBackStack() },
          onOpenTeacherDetail = { teacherId ->
            navController.navigate(AppRoutes.teacherDetail(teacherId))
          },
      )
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
    composable(AppRoutes.CALENDAR) { CalendarScreen(onBack = { navController.popBackStack() }) }
    composable(AppRoutes.SETTINGS) {
      val currentUser by AuthManager.currentUser.collectAsState()
      SettingsScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
          onOpenUserManagement = { navController.navigate(AppRoutes.USERS_MANAGEMENT) },
      )
    }
    composable(AppRoutes.USERS_MANAGEMENT) {
      val currentUser by AuthManager.currentUser.collectAsState()
      UserManagementScreen(
          currentUser = currentUser,
          onBack = { navController.popBackStack() },
      )
    }
  }
}
